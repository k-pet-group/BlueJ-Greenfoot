package bluej.debugmgr.objectbench;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.*;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.*;
import bluej.debugmgr.inspector.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.*;
import bluej.utility.*;
import bluej.views.*;
import bluej.extmgr.*;

/**
 * A wrapper around a Java object that handles calling methods, inspecting, etc.
 *
 * The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 * @version $Id: ObjectWrapper.java 2544 2004-05-24 08:56:02Z polle $
 */
public class ObjectWrapper extends JComponent
{
    // Strings
    static String methodException = Config.getString("debugger.objectwrapper.methodException");
    static String invocationException = Config.getString("debugger.objectwrapper.invocationException");
    static String inspect = Config.getString("debugger.objectwrapper.inspect");
    static String remove = Config.getString("debugger.objectwrapper.remove");
    static String redefinedIn = Config.getString("debugger.objectwrapper.redefined");
    static String inheritedFrom = Config.getString("debugger.objectwrapper.inherited");

    // Colors
    static final Color shadow = Config.getItemColour("colour.wrapper.shadow");
    static final Color bg = Config.getItemColour("colour.wrapper.bg");
    static final Color envOpColour = Config.getItemColour("colour.menu.environOp");
    static final Color textColour = Color.white;
    
    public static final int GAP = 5;    // gap between objects (left of each object)
    public static final int WIDTH = 90;
    public static final int HEIGHT = 60;

    // vertical offset between instance and class name
    public static int WORD_GAP = 20;
    public static int SHADOW_SIZE = 5;

    private static int itemHeight = 19;   // wild guess until we find out
    private static boolean itemHeightKnown = false;
    private static int itemsOnScreen;

    // The Java object that this wraps
    protected DebuggerObject obj;

    //TODO: fix these access permissions
    public String className;
    public String instanceName;
    protected String displayClassName;
    protected JPopupMenu menu;

    // back references to the containers that we live in
    private Package pkg;
    private PkgMgrFrame pmf;
    private ObjectBench ob;

    private Method[] methods;
    private Hashtable methodsUsed;
    private Hashtable actions;

    static public ObjectWrapper getWrapper(PkgMgrFrame pmf, ObjectBench ob,
                                            DebuggerObject obj, String instanceName)
    {
        if(pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        if (obj.isArray())
            return new ArrayWrapper(pmf, ob, obj, instanceName);
        else
            return new ObjectWrapper(pmf, ob, obj, instanceName);
    }

    public ObjectWrapper(PkgMgrFrame pmf, ObjectBench ob, DebuggerObject obj, String instanceName)
    {
        if(pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        // first one we construct will give us more info about the size of the screen
        if(!itemHeightKnown)
            itemsOnScreen = (int)Config.screenBounds.getHeight() / itemHeight;

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.ob = ob;
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
        
        setMinimumSize(new Dimension(WIDTH+GAP, HEIGHT));
        setSize(WIDTH + GAP, HEIGHT);
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
     * Open this object for inspection.
     */
    public void prepareRemove()
    {
        Inspector.removeInstance(obj);
    }

    /**
     * Creates the popup menu structure by parsing the object's
     * class inheritance hierarchy.
     *
     * @param className   class name of the object for which the menu is to be built
     */
    protected void createMenu(String className)
    {
        // System.out.println ("Bobject createMenu");    Damiano, marker for dynamic menu
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

        // add inspect and remove options
        JMenuItem item;
        menu.add(item = new JMenuItem(inspect));
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) { inspectObject(); }
            });
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);
  
        menu.add(item = new JMenuItem(remove));
        item.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) { removeObject(); }
            });
        item.setFont(PrefMgr.getStandoutMenuFont());
        item.setForeground(envOpColour);

        MenuManager menuManager = new MenuManager (menu); 
        menuManager.setAttachedObject(this);
        menuManager.addExtensionMenu(pkg.getProject());

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

        Arrays.sort(methods);
        for(int i = first; i < last; i++) {
            try {
                MethodView m = methods[i];
                if(!filter.accept(m))
                    continue;

                String methodSignature = m.getSignature();   // uses types for params
                String methodDescription = m.getShortDesc(); // uses names for params
                // check if method signature has already been added to a menu
                if(methodsUsed.containsKey(methodSignature)) {
                    methodDescription = methodDescription
                             + "   [ " + redefinedIn + " "
                             + JavaNames.stripPrefix(
                                   ((String)methodsUsed.get(methodSignature)))
                             + " ]";
                }
                else {
                    methodsUsed.put(methodSignature, m.getClassName());
                }
                item = new JMenuItem(methodDescription);
                item.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) { invokeMethod(e.getSource()); }
                    });
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
        return new Dimension(WIDTH+GAP, HEIGHT);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(WIDTH+GAP, HEIGHT);
    }

    public Dimension getMaximumSize()
    {
        return new Dimension(WIDTH+GAP, HEIGHT);
    }

    public String getName()
    {
        return instanceName;
    }

    public void setName(String newName)
    {
        instanceName = newName;
    }

    public DebuggerObject getObject()
    {
        return obj;
    }


    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);            //paint background

        Graphics2D g2 = (Graphics2D)g;
        drawUMLStyle(g2);
    }
    
    protected void drawUMLObjectShape(Graphics2D g, int x, int y, int w, int h, int shad, int corner)
    {
        g.setColor(shadow);
        g.fillRoundRect(x+shad,y+shad,w-shad,h-shad,corner,corner);
        g.setColor(bg);
        g.fillRoundRect(x,y,w-shad,h-shad,corner,corner);
        g.setColor(Color.black);
        g.drawRoundRect(x,y,w-shad, h-shad,corner,corner);
    }

    protected void drawUMLObjectText(Graphics2D g, int x, int y, int w, int h, int shad, String a, String b)
    {
        g.setColor(textColour);
        g.setFont(PrefMgr.getStandardFont());

        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getAscent() + 5;

        int maxWidth = w - shad - 4;    // our uml object will be (w-shad) pixels wide
                                        // we leave 2 pixels of space either side of shape

        // draw top string (normally instance name)
        int aWidth = fm.stringWidth(a);
        if(aWidth > maxWidth)
            aWidth = maxWidth;

        Utility.drawCentredText(g, a, x+2, y+5, maxWidth, fontHeight);

        int lineX = x + 2 + ((int)(maxWidth - aWidth)/2);
        int lineY = y + 5 + fontHeight;

        g.drawLine(lineX, lineY, lineX + aWidth, lineY);

        // draw bottom string (normally class name)
        int bWidth = fm.stringWidth(b);
        if(bWidth > maxWidth)
            bWidth = maxWidth;

        Utility.drawCentredText(g, b, x+2, y+25, maxWidth, fontHeight);
        lineX = x + 2 + ((int)(maxWidth - bWidth)/2);
        lineY = y + 25 + fontHeight;
        g.drawLine(lineX, lineY, lineX + bWidth, lineY);
    }

    /**
     * draw a UML style object instance
     */
    protected void drawUMLStyle(Graphics2D g)
    {
        drawUMLObjectShape(g, GAP, 0, WIDTH, HEIGHT, SHADOW_SIZE, 8);

        drawUMLObjectText(g, GAP, 0, WIDTH, HEIGHT, SHADOW_SIZE,
                            instanceName + ":", displayClassName);
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
            int offsetFactor = 4;
            int menuCount = menu.getComponentCount();
            // typically there are a minimum of 4 menu items for most objects
            // arrays however do not (at present) so calculation is adjusted to compensate 
            if( menuCount < 4)
                offsetFactor = menuCount;
            menuOffset = (menu.getComponentCount() - offsetFactor) * itemHeight;
         
            menu.show(this, evt.getX() + 1, evt.getY() - menuOffset);
        }
        else if(evt.getID() == MouseEvent.MOUSE_CLICKED) {
            if(evt.getClickCount() > 1)  // double click
                inspectObject();
            else {
                ob.fireObjectEvent(this);
            }

        }
    }

    // --- popup menu actions ---
    
    /**
     * Invoke a method on this object.
     */
    protected void invokeMethod(Object eventSource)
    {
        MethodView method = (MethodView)actions.get(eventSource);
        if(method != null)
            executeMethod(method);			// user method
    }

    /**
     * Open this object for inspection.
     */
    protected void inspectObject()
    {
        InvokerRecord ir = new ObjectInspectInvokerRecord(getClassName(), getName());
        
        ObjectInspector viewer =
      	    ObjectInspector.getInstance(obj, instanceName, pkg, ir, pmf);
    }

    protected void removeObject()
    {
        ob.remove(this, pkg.getId());
    }
    
    /**
     * Execute an interactive method call. If the method has results,
     * create a watcher to watch out for the result coming back, do the
     * actual invocation, and update open object viewers after the call.
     */
    private void executeMethod(final MethodView method)
    {
        ResultWatcher watcher = null;

        pkg.forgetLastSource();

        watcher = new ResultWatcher() {
            private ExpressionInformation expressionInformation = new ExpressionInformation(method,getName());
            
            public void putResult(DebuggerObject result, String name, InvokerRecord ir)
            {
                ob.addInteraction(ir);
                
                // a void result returns a name of null
                if (name == null)
                    return;
                                    
                ResultInspector viewer =
                    ResultInspector.getInstance(result, name, pkg,
                                           ir, expressionInformation, pmf);
                BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL,
                                      viewer.getResult());
            }
            public void putError(String msg) { }
            public ExpressionInformation getExpressionInformation() {
                return expressionInformation;
            }              
        };

        Invoker invoker = new Invoker(pmf, method, instanceName, watcher);
        invoker.invokeInteractive();
    }
}
