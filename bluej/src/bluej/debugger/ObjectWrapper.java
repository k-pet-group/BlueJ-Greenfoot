package bluej.debugger;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
// import bluej.tester.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A wrapper around a Java object that handles calling methods, inspecting,
 * etc. The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 * @version $Id: ObjectWrapper.java 1458 2002-10-23 12:06:40Z jckm $
 */
public class ObjectWrapper extends JComponent
    implements ActionListener
{
    static final Color shadow = Config.getItemColour("colour.wrapper.shadow");
    static final Color bg = Config.getItemColour("colour.wrapper.bg");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");
    static final Color textColour = Color.white;

    public static final int WIDTH = 100;
    public static final int HEIGHT = 70;
    // vertical offset between instance and class name
    public static final int WORD_GAP = 25;


    private static int itemHeight = 19;   // wild guess until we find out
    private static boolean itemHeightKnown = false;
    private static int itemsOnScreen;

    /** The Java object that this wraps **/
    private DebuggerObject obj;
    protected String className;
    protected String instanceName;
    private String displayClassName;
    private JPopupMenu menu;
    private Method[] methods;
    private Package pkg;
    private PkgMgrFrame pmf;

    private Hashtable methodsUsed;
    private Hashtable actions;

    public ObjectWrapper(PkgMgrFrame pmf, DebuggerObject obj, String instanceName)
    {
        if(pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.obj = obj;
        this.instanceName = instanceName;

        if(!itemHeightKnown)
            itemsOnScreen = (int)Config.screenBounds.getHeight() / itemHeight;

        className = obj.getClassName();

        if (!obj.isArray())
            createMenu(className);

        int dot_index = className.lastIndexOf('.');
        if(dot_index >= 0)
            displayClassName = className.substring(dot_index + 1);
        else
            displayClassName = className;

        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        setSize(WIDTH, HEIGHT);
    }

    public Package getPackage()
    {
        return pkg;
    }

    public PkgMgrFrame getFrame()
    {
        return pmf;
    }
    
    public String getClassName()
    {
        return className;
    }

    /**
     * Creates the popup menu structure by parsing the object's
     * class inheritance hierarchy.
     *
     * @param className   class name of the object for which the menu is to be built
     */
    private void createMenu(String className)
    {
        Class cl = pkg.loadClass(className);

        List classes = getClassHierarchy(cl);

        menu = new JPopupMenu(instanceName + " operations");

        if (cl != null) {
            View view = View.getView(cl);
            actions = new Hashtable();
            methodsUsed = new Hashtable();

            // define a view filter
            ViewFilter filter =
                new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PROTECTED);

            menu.addSeparator();

            // get declared methods for the class
            MethodView[] declaredMethods = view.getDeclaredMethods();

            // create method entries for locally declared methods
            int itemLimit = itemsOnScreen - 8 - classes.size();
            createMenuItems(menu, declaredMethods, filter, 0,
                            declaredMethods.length, itemLimit);

            // create submenus for superclasses
            for(int i = 1; i < classes.size(); i++ ) {
                Class currentClass = (Class)classes.get(i);
                view = View.getView(currentClass);
                declaredMethods = view.getDeclaredMethods();
                JMenu subMenu = new JMenu(inheritedFrom + " "
                               + JavaNames.stripPrefix(currentClass.getName()));
                subMenu.setFont(PrefMgr.getStandoutMenuFont());
                createMenuItems(subMenu, declaredMethods, filter, 0,
                                declaredMethods.length, (itemsOnScreen / 2));
                menu.insert(subMenu, 0);
            }

            menu.addSeparator();
        }

        // add inspect, serializable and remove options
        JMenuItem item;
        menu.add(item = new JMenuItem(inspect));
        item.addActionListener(this);
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        // serializable support - not yet enabled 12/01/2000 ajp
        /*      if (Serializable.class.isAssignableFrom(cl))
                {
                menu.add(item = new JMenuItem(serializable));
                item.addActionListener(this);
                item.setFont(PrefMgr.getStandoutMenuFont());
                item.setForeground(envOpColour);
                } */

	/* menu.add(item = new JMenuItem("make test"));
	item.addActionListener(this);
	item.setFont(PrefMgr.getStandoutMenuFont());
	item.setForeground(envOpColour);*/

        menu.add(item = new JMenuItem(remove));
        item.addActionListener(this);
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        add(menu);
    }


    /**
     * creates the individual menu items for an object's popup menu.
     * The method checks for previously defined methods with the same signature
     * and appends information referring to this.
     *
     * @param menu      the menu that the items are to be created for
     * @param methods   the methods for which menu items should be created
     * @param filter    the filter which decides on which methods should be shown
     * @param first     the index of the methods array which represents the
     *                  starting point of the menu items
     * @param last      the index of the methods array which represents the end
     *                  point of the menu items
     * @param sizeLimit the limit to which the menu should grow before openeing
     *                  submenus
     */
    private void createMenuItems(JComponent menu, MethodView[] methods,
                                 ViewFilter filter, int first, int last,
                                 int sizeLimit)
    {
        JMenuItem item;
        String methodSignature;

        Arrays.sort(methods);
        for(int i = first; i < last; i++) {
            try {
                MethodView m = methods[i];
                if(!filter.accept(m))
                    continue;

                // check if method signature has already been added to a menu
                if(methodsUsed.containsKey(m.getSignature())) {
                    methodSignature = ( m.getSignature()
                             + "   [ " + redefinedIn + " "
                             + JavaNames.stripPrefix(
                                   ((String)methodsUsed.get(m.getSignature())))
                             + " ]");
                }
                else {
                    methodSignature = m.getSignature();
                    methodsUsed.put(m.getSignature(), m.getClassName());
                }
                item = new JMenuItem(methodSignature);
                item.addActionListener(this);
                item.setFont(PrefMgr.getPopupMenuFont());
                actions.put(item, m);

                // check whether it's time for a submenu

                int itemCount;
                if(menu instanceof JMenu)
                    itemCount =((JMenu)menu).getMenuComponentCount();
                else
                    itemCount = menu.getComponentCount();
                if(itemCount >= sizeLimit) {
                    JMenu subMenu = new JMenu("more methods");
                    subMenu.setFont(PrefMgr.getStandoutMenuFont());
                    subMenu.setForeground(envOpColour);
                    menu.add(subMenu);
                    menu = subMenu;
                    sizeLimit = itemsOnScreen / 2;
                }
                menu.add(item);
            } catch(Exception e) {
                Debug.reportError(methodException + e);
                e.printStackTrace();
            }
        }
    }


    /**
     * Creates a List containing all classes in an inheritance hierarchy
     * working back to Object
     *
     * @param   derivedClass    the class whose hierarchy is mapped (including self)
     * @return                  the List containng the classes in the inheritance hierarchy
     */
    public List getClassHierarchy(Class derivedClass)
    {
        Class currentClass = derivedClass;
        List classVector = new ArrayList();
        while(currentClass != null) {
            classVector.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        return classVector;
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    public String getName()
    {
        return instanceName;
    }

    public void setName(String newName)
    {
        instanceName = newName;
    }

    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        drawUMLStyle(g2);
    }
    
    public DebuggerObject getObject()
    {
        return obj;
    }

    /**
     * draw a UML style object instance
     */
    private void drawUMLStyle(Graphics2D g)
    {
        g.setFont(PrefMgr.getStandardFont());
        FontMetrics fm = g.getFontMetrics();

        g.setColor(shadow);
        g.fillRoundRect(10, 10, WIDTH - 10, HEIGHT - 15, 8, 8);
        g.setColor(bg);
        g.fillRoundRect(5, 5, WIDTH - 10, HEIGHT - 15, 8, 8);
        g.setColor(Color.black);
        g.drawRoundRect(5, 5, WIDTH - 10, HEIGHT - 15, 8, 8);

        g.setColor(textColour);

        int maxWidth = WIDTH - 20;

        // draw instance name
        String objectName = instanceName + ":";
        int h = fm.getAscent() + 4;
        int w = fm.stringWidth(objectName);
        if(w > maxWidth)
            w = maxWidth;

        Utility.drawCentredText(g, instanceName + ":", 10, 10,  WIDTH - 20, h);
        int lineX = (int)(WIDTH - w)/2;
        int lineY = h + 12;

        g.drawLine(lineX, lineY, lineX + w, lineY);

        // draw class name
        w = fm.stringWidth(displayClassName);
        if(w > maxWidth)
            w = maxWidth;

        Utility.drawCentredText(g, displayClassName, 10, 35,  WIDTH - 20, h);
        lineX = (int)(WIDTH - w)/2;
        lineY += WORD_GAP;
        g.drawLine(lineX, lineY, lineX + w, lineY);

    }

    /**
     * Process a mouse click into this object. If it was a popup event, show the object's
     * menu. If it was a double click, inspect the object. If it was a normal mouse click,
     * insert it into a parameter field (if any).
     */
    protected void processMouseEvent(MouseEvent evt)
    {
        int menuOffset;
        super.processMouseEvent(evt);

        if(evt.isPopupTrigger()) {
            if(menu == null)
                return;

            if(!itemHeightKnown) {
                int height = ((JComponent)menu.getComponent(0)).getHeight();

                // first time, before it's shown, we won't get the real height
                if(height > 1) {
                    itemHeight = height;
                    itemsOnScreen = (int)Config.screenBounds.getHeight() /
                                         itemHeight;
                    itemHeightKnown = true;
                }
            }
            // try tp position menu so that the pointer is near the method items
            menuOffset = (menu.getComponentCount() - 4) * itemHeight;
            menu.show(this, evt.getX() + 1, evt.getY() - menuOffset);
        }
        else if(evt.getID() == MouseEvent.MOUSE_CLICKED) {
            if(evt.getClickCount() > 1)  // double click
                inspectObject();
            else {
                ObjectBench bench = (ObjectBench)getParent();
                bench.fireObjectEvent(this);
            }

        }
    }

    public void actionPerformed(ActionEvent e)
    {
        MethodView method = (MethodView)actions.get(e.getSource());
        if(method != null)
            executeMethod(method);			// user method
        else {
            String cmd = e.getActionCommand();
            if(inspect.equals(cmd)) {			// inspect
                // load the object into runtime scope
                inspectObject();
            }
            else if(remove.equals(cmd))	{		// remove
                ObjectBench bench = (ObjectBench)getParent();
                bench.remove(this, pkg.getId());
            }

/*		else if ("make test".equals(cmd)) {

			CallRecord cr = CallRecord.getCallRecord(getName());

			if (cr == null)
				System.out.println("object was constructed with a get");
			else
				System.out.println(cr.dump(1, "wow", false));
		} */
            // serializable support - not yet enabled 12/01/2000 ajp
            /*            else if(serializable.equals(cmd)) {

                          Debugger.debugger.serializeObject(pkg.getId(),
                          instanceName, "test.obj");
            */
            /*                DebuggerObject debObj =
                              Debugger.debugger.deserializeObject(pkg.getRemoteClassLoader().getId(),
                              pkg.getId(),
                              "unserial_1",
                              "test.obj");

                              ObjectWrapper wrapper = new ObjectWrapper(debObj,
                              "unserial_1",
                              pkg);

                              pkg.getFrame().getObjectBench().add(wrapper);  // might change name
                              }
            */
        }
    }

    /**
     * Open this object for inspection.
     */
    private void inspectObject()
    {
        ObjectViewer viewer =
      	    ObjectViewer.getViewer(true, obj, instanceName, pkg, true, pmf);
    }

    /**
     * Execute an interactive method call. If the method has results,
     * create a watcher to watch out for the result coming back, do the
     * actual invocation, and update open object viewers after the call.
     */
    private void executeMethod(MethodView method)
    {
        ResultWatcher watcher = null;

        pkg.forgetLastSource();
        if(!method.isVoid()) {
            watcher = new ResultWatcher() {
                    public void putResult(DebuggerObject result, String name)
                    {
                        ObjectViewer viewer =
                            ObjectViewer.getViewer(false, result, name,
                                                   pkg, true, pmf);
                        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL,
                                              viewer.getResult());
                    }
                    public void putError (String message) {}
                };
        }

        Invoker invoker = new Invoker(pmf, method, instanceName, watcher);
        invoker.invokeInteractive();
    }

    // Internationalisation
    static String methodException =
        Config.getString("debugger.objectwrapper.methodException");
    static String invocationException =
        Config.getString("debugger.objectwrapper.invocationException");

    static String inspect =
        Config.getString("debugger.objectwrapper.inspect");
    static String remove =
        Config.getString("debugger.objectwrapper.remove");

    static String redefinedIn =
        Config.getString("debugger.objectwrapper.redefined");

    static String inheritedFrom =
        Config.getString("debugger.objectwrapper.inherited");

    static String serializable =
        Config.getString("debugger.objectwrapper.serializable");
}
