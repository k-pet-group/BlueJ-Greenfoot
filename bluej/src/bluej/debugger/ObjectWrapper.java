package bluej.debugger;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;

import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A wrapper around a Java object that handles calling methods, inspecting,
 * etc. The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 * @version $Id: ObjectWrapper.java 517 2000-05-25 07:58:59Z ajp $
 */
public class ObjectWrapper extends JComponent
    implements ActionListener
{
    static final Color shadow = Config.getItemColour("colour.wrapper.shadow");
    static final Color bg = Config.getItemColour("colour.wrapper.bg");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

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

    public static final int WIDTH = 100;
    public static final int HEIGHT = 70;

    public ObjectWrapper(PkgMgrFrame pmf, DebuggerObject obj, String instanceName)
    {
        if(pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.obj = obj;
        this.instanceName = instanceName;

        className = obj.getClassName();
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

    /**
     * Creates the popup menu structure by parsing the object's
     * class inheritance hierarchy.
     *
     * @param   className   class name of the object for which the menu is to be built
     */
    private void createMenu(String className)
    {
        Class cl = pkg.loadClass(className);

        Vector classes = getClassHierarchy(cl);

        menu = new JPopupMenu(instanceName + " operations");

        if (cl != null) {
            View view = View.getView(cl);
            actions = new Hashtable();
            methodsUsed = new Hashtable();

            // define a view filter
            ViewFilter filter= new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PROTECTED);

            menu.addSeparator();

            // get declared methods for the class
            MethodView[] declaredMethods = view.getDeclaredMethods();

            createMenuItems(menu, declaredMethods, filter, 0,
                            declaredMethods.length);

            for(int i = 1; i < classes.size(); i++ ) {
                Class currentClass = (Class)classes.elementAt(i);
                view = View.getView(currentClass);
                declaredMethods = view.getDeclaredMethods();
                JMenu subMenu =  new JMenu(inheritedFrom + " "
                                + JavaNames.stripPrefix(currentClass.getName()));
                subMenu.setFont(PrefMgr.getStandoutMenuFont());
                createMenuItems(subMenu, declaredMethods, filter, 0, declaredMethods.length);
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

        menu.add(item = new JMenuItem(remove));
        item.addActionListener(this);
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        add(menu);
    }




    /**
     * creates the individual menu items for an object's popup menu.
     * The method checks for previously defined methods with the same signature and
     * appends information referring to this.
     *
     * @param menu      the menu that the items are to be created for
     * @param methods   the methods for which menu items should be created
     * @param filter    the filter which decides on which methods should be shown
     * @param first     the index of the methods array which represents the starting point of the menu items
     * @param last      the index of the methods array which represents the end point of the menu items
     */
    private void createMenuItems(JComponent menu, MethodView[] methods,
				 ViewFilter filter, int first, int last)
    {
        JMenuItem item;
        String methodSignature;

	for(int i = first; i < last; i++) {
	    try {
		MethodView m = methods[i];
		if(!filter.accept(m))
		    continue;

		// check if method signature has already been added to a menu
		if(methodsUsed.containsKey(m.getShortDesc())) {
		    methodSignature = ( m.getShortDesc()
					+ "   [ " + redefinedIn + " "
					+ JavaNames.stripPrefix(((String)methodsUsed.get(m.getShortDesc())))
			+ " ]");
		}
		else {
		    methodSignature =  m.getShortDesc();
		    methodsUsed.put(m.getShortDesc(), m.getClassName());
		}
		item = new JMenuItem(methodSignature);
		item.addActionListener(this);
		item.setFont(PrefMgr.getStandardMenuFont());
		actions.put(item, m);
		menu.add(item);
	    } catch(Exception e) {
		Debug.reportError(methodException + e);
		e.printStackTrace();
	    }
	}
    }


    /**
     ** creates a Vector containing all classes in an inheritance hierarchy
     ** working back to Object
     **
     ** @param derivedClass the class whose hierarchy is mapped (including self)
     ** @return the Vector containng the classes in the inheritance hierarchy
     **/
    public Vector getClassHierarchy(Class derivedClass)
    {
	Class currentClass = derivedClass;
	Vector classVector = new Vector();
	while(currentClass != null) {
	    classVector.addElement(currentClass);
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
        g.setFont(PrefMgr.getStandardFont());
        FontMetrics fm = g.getFontMetrics();

        g.setColor(shadow);
        g.fillOval(10, 5, WIDTH - 10, HEIGHT - 5);
        g.setColor(bg);
        g.fillOval(10, 5, WIDTH - 15, HEIGHT - 10);
        g.setColor(Color.black);
        g.drawOval(10, 5, WIDTH - 15, HEIGHT - 10);

        g.setColor(Color.white);
        Utility.drawCentredText(g, displayClassName, 10, 5, WIDTH - 15, HEIGHT - 5);

        int w = fm.stringWidth(instanceName) + 10;
        int h = fm.getAscent() + 4;
        g.fillRect(0, 10, w, h);

        g.setColor(Color.black);
        g.drawRect(0, 10, w, h);
        Utility.drawCentredText(g, instanceName, 0, 10, w, h);
    }

    protected void processMouseEvent(MouseEvent evt)
    {
        int menuOffset;
        super.processMouseEvent(evt);

//XXX        pkg.getFrame().clearStatus();

        if(isPopupEvent(evt)) {
            int itemHeight = ((JComponent)menu.getComponent(0)).getHeight();

            if (itemHeight <= 1)     // not yet shown - don't know real height
            // take a wild guess here

            // lifted higher to avoid mouse events on underlying objects - temporary
            menuOffset = (menu.getComponentCount() - 1) * 19;
            // lifted higher to avoid mouse events on underlying objects
            //menuOffset = (menu.getComponentCount() - 4) * 19;
            else
            // from the second time: do it properly
            menuOffset = (menu.getComponentCount() - 1) * itemHeight;
            //menuOffset = (menu.getComponentCount() - 4) * itemHeight;

            menu.show(this, evt.getX(), evt.getY() - menuOffset);
	}
	else if(evt.getID() == MouseEvent.MOUSE_CLICKED) {
	    if(evt.getClickCount() > 1)
		inspectObject();
	    else {
		ObjectBench bench = (ObjectBench)getParent();
		bench.objectSelected(this);
	    }

	}
    }

    private boolean isPopupEvent(MouseEvent evt)
    {
	return evt.isPopupTrigger()
	    || ((evt.getID() == MouseEvent.MOUSE_PRESSED) && evt.isControlDown());
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
//XXX	ObjectViewer viewer =
//	    ObjectViewer.getViewer(true, obj, instanceName, pkg, true,
//				   pkg.getFrame());
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
                    ObjectViewer viewer = ObjectViewer.getViewer(false, result,
                                        						 name,
                                        						 pkg, true,
                                        						 pmf);
                }
            };
        }

        Invoker invoker = new Invoker(pmf, method, instanceName, watcher);
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
