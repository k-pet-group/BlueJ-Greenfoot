/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugmgr.objectbench;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.BlueJEvent;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ResultWatcher;
import bluej.extensions.BObject;
import bluej.extensions.ExtensionBridge;
import bluej.extmgr.MenuManager;
import bluej.extmgr.ObjectExtensionMenu;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.Utility;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;

/**
 * A wrapper around a Java object that handles calling methods, inspecting, etc.
 *
 * <p>The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 */
public class ObjectWrapper extends JComponent implements Accessible, FocusListener, InvokeListener, KeyListener, NamedValue
{
    // Strings
    static String methodException = Config.getString("debugger.objectwrapper.methodException");
    static String invocationException = Config.getString("debugger.objectwrapper.invocationException");
    static String inspect = Config.getString("debugger.objectwrapper.inspect");
    static String remove = Config.getString("debugger.objectwrapper.remove");
    static String redefinedIn = Config.getString("debugger.objectwrapper.redefined");
    static String inheritedFrom = Config.getString("debugger.objectwrapper.inherited");

    // Colors
    static final Color envOpColour = Config.ENV_COLOUR;
    static final Color textColour = Color.white;
    
    // Images
    private static final Image objectImage = Config.getImageAsIcon("image.bench.object").getImage();
    private static final Image selectedObjectImage = Config.getImageAsIcon("image.bench.object-selected").getImage();
    
    // Strokes
    static final Stroke selectedStroke = new BasicStroke(2.0f);
    static final Stroke normalStroke = new BasicStroke(1.0f);

    // vertical offset between instance and class name
    public static final int WORD_GAP = 20;
    public static final int SHADOW_SIZE = 5;
    
    protected static final int HGAP = 5;    // horiz. gap between objects (left of each object)
    protected static final int VGAP = 6;    // vert. gap between objects (above and below of each object)
    public static final int WIDTH = objectImage.getWidth(null);    // width including gap
    public static final int HEIGHT = objectImage.getHeight(null);   // height including gap

    private static int itemHeight = 19;   // wild guess until we find out
    private static boolean itemHeightKnown = false;
    private static int itemsOnScreen;

    /** The Java object that this wraps */
    protected DebuggerObject obj;
    protected GenTypeClass iType;

    /** Fully qualified type this object represents, including type parameters */
    private String className;
    private String instanceName;
    protected String displayClassName;
    protected JPopupMenu menu;

    // back references to the containers that we live in
    private Package pkg;
    private PkgMgrFrame pmf;
    private ObjectBench ob;

    private boolean isSelected = false;
    
    /**
     * Get an object wrapper for a user object. 
     * 
     * @param pmf   The package manager frame
     * @param ob    The object bench
     * @param obj   The object to wrap
     * @param iType   The static type of the object, used as a fallback if
     *                the runtime type is inaccessible
     * @param instanceName  The name for the object reference
     * @return
     */
    static public ObjectWrapper getWrapper(PkgMgrFrame pmf, ObjectBench ob,
                                            DebuggerObject obj,
                                            GenTypeClass iType,
                                            String instanceName)
    {
        if (obj.isArray()) {
            return new ArrayWrapper(pmf, ob, obj, instanceName);
        }
        else {
            return new ObjectWrapper(pmf, ob, obj, iType, instanceName);
        }
    }

    protected ObjectWrapper(PkgMgrFrame pmf, ObjectBench ob, DebuggerObject obj, GenTypeClass iType, String instanceName)
    {
        // first one we construct will give us more info about the size of the screen
        if(!itemHeightKnown) {
            itemsOnScreen = (int)Config.screenBounds.getHeight() / itemHeight;
        }

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.ob = ob;
        this.obj = obj;
        this.iType = iType;
        this.setName(instanceName);
        if (obj.isNullObject()) {
            className = "";
            displayClassName = "";
        }
        else {
            GenTypeClass objType = obj.getGenType();
            className = objType.toString();
            displayClassName = objType.toString(true);
        }

        createMenu(findIType());
                
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setSize(WIDTH, HEIGHT);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addFocusListener(this);
        addKeyListener(this);
    }

    public Package getPackage()
    {
        return pkg;
    }

    /**
     * Get the PkgMgrFrame which is housing this object wrapper.
     */
    public PkgMgrFrame getFrame()
    {
        return pmf;
    }
    
    public String getClassName()
    {
        return obj.getClassName();
    }
    
    public String getTypeName()
    {
        return className;
    }

    /**
     * Return the invocation type for this object. The invocation type is the
     * type which should be written in the shell file. It is not necessarily the
     * same as the actual (dynamic) type of the object.
     */
    @Override
    public JavaType getGenType()
    {
        return iType;
    }
    
    // --------- NamedValue interface --------------
    
    @Override
    public boolean isFinal()
    {
        return true;
    }
    
    @Override
    public boolean isInitialized()
    {
        return true;
    }
    
    // ----------------------------------------------
    
    private BObject singleBObject;  // Every ObjectWrapper has none or one BObject
    
    /**
     * Return the extensions BObject associated with this ObjectWrapper.
     * There should be only one BObject object associated with each Package.
     * @return the BPackage associated with this Package.
     */
    public synchronized final BObject getBObject ()
    {
        if ( singleBObject == null )
          singleBObject = ExtensionBridge.newBObject(this);
          
        return singleBObject;
    }
    
    /**
     * Perform any necessary cleanup before removal from the object bench.
     */
    public void prepareRemove()
    {
        pkg.getProject().removeInspectorInstance(obj);
    }

    /**
     * Check whether the given class is accessible (from this wrapper's package)
     * 
     * @param cl  The class to check for accessibility
     * @return    True if the class is accessible, false otherwise
     */
    private boolean classIsAccessible(Class<?> cl)
    {
        int clMods = cl.getModifiers();
        String classPackage = JavaNames.getPrefix(cl.getName());
        if (Modifier.isProtected(clMods) && ! pkg.getQualifiedName().equals(classPackage)
                || Modifier.isPrivate(clMods)) {
            return false;
        }
        else {
            return true;
        }
    }
    
    /**
     * Determine an appropriate type to use for this object in shell files.
     * The type must be accessible in the current package.
     * 
     * iType will be set to the chosen type.
     * 
     * @return  The class of the chosen type.
     */
    private Class<?> findIType()
    {
        String className = obj.getClassName();
        Class<?> cl = pkg.loadClass(className);
        
        // If the class is inaccessible, use the invocation type.
        if (cl != null) {
            if (! classIsAccessible(cl)) {
                cl = pkg.loadClass(iType.classloaderName());
                while (cl != null && ! classIsAccessible(cl)) {
                    cl = cl.getSuperclass();
                    if (cl != null) {
                        iType = iType.mapToSuper(cl.getName());
                    }
                    else {
                        JavaReflective objectReflective = new JavaReflective(Object.class);
                        iType = new GenTypeClass(objectReflective);
                    }
                }
            }
            else {
                // If the class type *is* accessible, on the other hand,
                // use it as the invocation type.
                iType = obj.getGenType();
            }
        }

        return cl;
    }
    
    /**
     * Creates the popup menu structure by parsing the object's
     * class inheritance hierarchy.
     */
    protected void createMenu(Class<?> cl)
    {
        menu = new JPopupMenu(getName() + " operations");

        // add the menu items to call the methods
        createMethodMenuItems(menu, cl, iType, this, obj, pkg.getQualifiedName(), true);

        // add inspect and remove options
        JMenuItem item;
        menu.add(item = new JMenuItem(inspect));
        item.addActionListener(
            new ActionListener() {
                @Override
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
        menuManager.setMenuGenerator(new ObjectExtensionMenu(this));
        menuManager.addExtensionMenu(pkg.getProject());

        add(menu);
    }
    
    /**
     * Creates the menu items for all the methods in the class, which is a raw
     * class type.
     * 
     * @param menu  The menu to add the menu items to
     * @param cl    The class whose methods to add
     * @param il    The invoke listener to notify when a method is called
     * @param obj   The object to apply the methods to
     * @param currentPackageName Name of the package that this object will be
     *            shown from (used to determine wheter to show package protected
     *            methods)
     * @param showObjectMethods Whether to show the submenu with methods from java.lang.Object
     */
    public static void createMethodMenuItems(JPopupMenu menu, Class<?> cl, InvokeListener il, DebuggerObject obj,
            String currentPackageName, boolean showObjectMethods)
    {
        GenTypeClass gt = new GenTypeClass(new JavaReflective(cl));
        createMethodMenuItems(menu, cl, gt, il, obj, currentPackageName, showObjectMethods);
    }
    
    /**
     * Creates the menu items for all the methods in the class
     * 
     * @param menu  The menu to add the menu items to
     * @param cl    The class whose methods to add
     * @param gtype  The generic type of the class
     * @param il    The invoke listener to notify when a method is called
     * @param obj   The object to apply the methods to
     * @param currentPackageName Name of the package that this object will be
     *            shown from (used to determine wheter to show package protected
     *            methods)
     * @param showObjectMethods Whether to show the submenu for methods inherited from java.lang.Object
     */
    public static void createMethodMenuItems(JPopupMenu menu, Class<?> cl, GenTypeClass gtype, InvokeListener il, DebuggerObject obj,
            String currentPackageName, boolean showObjectMethods)
    {
        if (cl != null) {
            View view = View.getView(cl);
            Hashtable<JMenuItem, MethodView> actions = new Hashtable<JMenuItem, MethodView>();
            Hashtable<String, String> methodsUsed = new Hashtable<String, String>();
            List<Class<?>> classes = getClassHierarchy(cl);

            // define two view filters for different package visibility
            ViewFilter samePackageFilter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
            ViewFilter otherPackageFilter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PUBLIC);
            
            // define a view filter
            ViewFilter filter;
            if (currentPackageName != null && currentPackageName.equals(view.getPackageName()))
                filter = samePackageFilter;
            else
                filter = otherPackageFilter;

            menu.addSeparator();

            // get declared methods for the class
            MethodView[] declaredMethods = view.getDeclaredMethods();
            
            // create method entries for locally declared methods
            GenTypeClass curType = gtype;
            if (curType == null) {
                curType = new GenTypeClass(new JavaReflective(cl));
            }
            
            // HACK to make it work in greenfoot.
            if(itemsOnScreen <= 0 ) {
                itemsOnScreen = 30; 
            }

            int itemLimit = itemsOnScreen - 8 - classes.size();
          
            createMenuItems(menu, declaredMethods, il, filter, itemLimit, curType.getMap(), actions, methodsUsed);

            // create submenus for superclasses
            for(int i = 1; i < classes.size(); i++ ) {
                Class<?> currentClass = classes.get(i);
                view = View.getView(currentClass);
                
                // Determine visibility of package private / protected members
                if (currentPackageName != null && currentPackageName.equals(view.getPackageName()))
                    filter = samePackageFilter;
                else
                    filter = otherPackageFilter;
                
                // map generic type paramaters to the current superclass
                curType = curType.mapToSuper(currentClass.getName());
                
                if (!"java.lang.Object".equals(currentClass.getName()) || showObjectMethods) { 
                    declaredMethods = view.getDeclaredMethods();
                    JMenu subMenu = new JMenu(inheritedFrom + " "
                                   + JavaNames.stripPrefix(currentClass.getName()));
                    subMenu.setFont(PrefMgr.getStandoutMenuFont());
                    createMenuItems(subMenu, declaredMethods, il, filter, (itemsOnScreen / 2), curType.getMap(), actions, methodsUsed);
                    menu.insert(subMenu, 0);
                }
            }

            menu.addSeparator();
        }
    }
    
    /**
     * creates the individual menu items for an object's popup menu.
     * The method checks for previously defined methods with the same signature
     * and appends information referring to this.
     *
     * @param menu      the menu that the items are to be created for
     * @param methods   the methods for which menu items should be created
     * @param il the listener to be notified when a method should be called
     *            interactively
     * @param filter    the filter which decides on which methods should be shown
     * @param sizeLimit the limit to which the menu should grow before openeing
     *                  submenus
     * @param genericParams the mapping of generic type parameter names to their
     *            corresponding types in the object instance (a map of String ->
     *            GenType).
     * @param methodsUsed 
     * @param actions 
     */
    private static void createMenuItems(JComponent menu, MethodView[] methods, InvokeListener il, ViewFilter filter,
            int sizeLimit, Map<String,GenTypeParameter> genericParams, Hashtable<JMenuItem, MethodView> actions, Hashtable<String, String> methodsUsed)
    {
        JMenuItem item;
        boolean menuEmpty = true;

        Arrays.sort(methods);
        for(int i = 0; i < methods.length; i++) {
            try {
                MethodView m = methods[i];
                if(!filter.accept(m))
                    continue;

                menuEmpty = false;
                String methodSignature = m.getCallSignature();   // uses types for params
                String methodDescription = m.getLongDesc(genericParams); // uses names for params

                // check if method signature has already been added to a menu
                if(methodsUsed.containsKey(methodSignature)) {
                    methodDescription = methodDescription
                             + "   [ " + redefinedIn + " "
                             + JavaNames.stripPrefix(
                                   methodsUsed.get(methodSignature))
                             + " ]";
                }
                else {
                    methodsUsed.put(methodSignature, m.getClassName());
                }

                Action a = new InvokeAction(m, il, methodDescription);
                item = new JMenuItem(a);
               
                item.setFont(PrefMgr.getPopupMenuFont());
                actions.put(item, m);

                // check whether it's time for a submenu

                int itemCount;
                if(menu instanceof JMenu)
                    itemCount =((JMenu)menu).getMenuComponentCount();
                else
                    itemCount = menu.getComponentCount();
                if(itemCount >= sizeLimit) {
                    JMenu subMenu = new JMenu(Config.getString("debugger.objectwrapper.moreMethods"));
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
        
        // If there are no accessible methods, insert a message which says so.
        if (menuEmpty) {
            JMenuItem mi = new JMenuItem(Config.getString("debugger.objectwrapper.noMethods"));
            mi.setFont(PrefMgr.getStandoutMenuFont());
            mi.setForeground(envOpColour);
            mi.setEnabled(false);
            menu.add(mi);
        }
    }

    /**
     * Creates a List containing all classes in an inheritance hierarchy
     * working back to Object
     *
     * @param   derivedClass    the class whose hierarchy is mapped (including self)
     * @return                  the List containng the classes in the inheritance hierarchy
     */
    public static List<Class<?>> getClassHierarchy(Class<?> derivedClass)
    {
        Class<?> currentClass = derivedClass;
        List<Class<?>> classVector = new ArrayList<Class<?>>();
        while(currentClass != null) {
            classVector.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        return classVector;
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Override
    public String getName()
    {
        return instanceName;
    }

    @Override
    public void setName(String newName)
    {
        instanceName = newName;
    }

    public DebuggerObject getObject()
    {
        return obj;
    }


    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);            //paint background

        Graphics2D g2 = (Graphics2D)g;
        drawUMLStyle(g2);
    }
    
    /**
     * Draw the body of an object wrapper (everything, except the text).
     */
    protected void drawUMLObjectShape(Graphics2D g, int x, int y, int w, int h, int shad, int corner)
    {
        g.drawImage(isSelected ? selectedObjectImage : objectImage, x, y, null);
    }

    /**
     * Draw the text onto an object wrapper (objectname: classname).
     */
    protected void drawUMLObjectText(Graphics2D g, int x, int y, int w, int shad, 
            String objName, String className)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(textColour);
        g.setFont(PrefMgr.getTargetFont());

        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getAscent() + 5;

        int maxWidth = w - shad - 4;    // our uml object will be (w-shad) pixels wide
                                        // we leave 2 pixels of space either side of shape

        // 54 is a hard hack; the height of the red part of the object image:
        int totalGap = 54 - (fontHeight + fm.getAscent()); 
        int start = totalGap / 2;
        
        // draw top string (normally instance name)
        int aWidth = fm.stringWidth(objName);
        if(aWidth > maxWidth) {
            aWidth = maxWidth;
        }

        Utility.drawCentredText(g, objName, x+2, y+start, maxWidth, fontHeight);

        // draw bottom string (normally class name)
        int bWidth = fm.stringWidth(className);
        if(bWidth > maxWidth)
            bWidth = maxWidth;

        Utility.drawCentredText(g, className, x+2, y+start+fontHeight, maxWidth, fontHeight);
    }

    /**
     * draw a UML style object instance
     */
    protected void drawUMLStyle(Graphics2D g)
    {
        drawUMLObjectShape(g, HGAP, (VGAP / 2), WIDTH-HGAP, HEIGHT-VGAP, SHADOW_SIZE, 8);
        drawUMLObjectText(g, HGAP, (VGAP / 2), WIDTH-HGAP, SHADOW_SIZE,
                            getName() + ":", displayClassName);
    }

    /**
     * Process a mouse click into this object. If it was a popup event, show the object's
     * menu. If it was a double click, inspect the object. If it was a normal mouse click,
     * insert it into a parameter field (if any).
     */
    @Override
    protected void processMouseEvent(MouseEvent evt)
    {
        super.processMouseEvent(evt);
        if(evt.isPopupTrigger()) {
            showMenu(evt.getX(), evt. getY());
        }
        else if(evt.getID() == MouseEvent.MOUSE_CLICKED) {
            if (evt.getClickCount() > 1) // double click
                inspectObject();
            else { //single click
                ob.fireObjectEvent(this);
            }
        }
        //manage focus
        if (evt.getID() == MouseEvent.MOUSE_CLICKED || evt.isPopupTrigger()) {
            requestFocusInWindow();
        }
    }

    // --- popup menu actions ---
    
    private int calcOffset()
    {
        int menuOffset;
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
        return menuOffset;
    }

    public void showMenu(int x, int y)
    {
        int menuOffset;

        if(menu == null) {
            return;
        }

        menuOffset = calcOffset();
        menu.show(this, x + 1, y - menuOffset);
    }

    public void showMenu()
    {
        showMenu(WIDTH/2, HEIGHT/2);
    }

    /**
     * Open this object for inspection.
     */
    protected void inspectObject()
    {
        InvokerRecord ir = new ObjectInspectInvokerRecord(getName());
        pkg.getProject().getInspectorInstance(obj, getName(), pkg, ir, pmf);  // shows the inspector
    }

    protected void removeObject()
    {
        ob.removeObject(this, pkg.getId());
    }
    
    /**
     * Execute an interactive method call. If the method has results,
     * create a watcher to watch out for the result coming back, do the
     * actual invocation, and update open object viewers after the call.
     */
    @Override
    public void executeMethod(final MethodView method)
    {
        ResultWatcher watcher = null;

        pkg.forgetLastSource();

        watcher = new ResultWatcher() {
            private ExpressionInformation expressionInformation = new ExpressionInformation(method,getName(),obj.getGenType());
            
            @Override
            public void beginCompile()
            {
                pmf.setWaitCursor(true);
            }
            
            @Override
            public void beginExecution(InvokerRecord ir)
            {
                BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
                pmf.setWaitCursor(false);
            }
            
            @Override
            public void putResult(DebuggerObject result, String name, InvokerRecord ir)
            {
                ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), instanceName);
                executionEvent.setMethodName(method.getName());
                executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
                executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                executionEvent.setResultObject(result);
                BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                
                pkg.getProject().updateInspectors();
                expressionInformation.setArgumentValues(ir.getArgumentValues());
                ob.addInteraction(ir);
                
                // a void result returns a name of null
                if (result != null && ! result.isNullObject()) {
                    pkg.getProject().getResultInspectorInstance(result, name, pkg,
                            ir, expressionInformation, pmf);
                }
            }
            
            @Override
            public void putError(String msg, InvokerRecord ir)
            {
                pmf.setWaitCursor(false);
            }
            
            @Override
            public void putException(ExceptionDescription exception, InvokerRecord ir)
            {
                ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), instanceName);
                executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
                executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                executionEvent.setException(exception);
                BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                
                pkg.getProject().updateInspectors();
                pkg.exceptionMessage(exception);
            }
            
            @Override
            public void putVMTerminated(InvokerRecord ir)
            {
                ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), instanceName);
                executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
                executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
            }
        };

        if (pmf.checkDebuggerState()) {
            Invoker invoker = new Invoker(pmf, method, this, watcher);
            invoker.invokeInteractive();
        }
    }
    
    public void callConstructor(ConstructorView cv)
    {
        // do nothing (satisfy the InvokeListener interface)
    }

    /**
     * @param isSelected The isSelected to set.
     */
    public void setSelected(boolean isSelected) 
    {
        this.isSelected = isSelected;
        if(isSelected) {
            pmf.setStatus(getName() + " : " + displayClassName);
            scrollRectToVisible(new Rectangle(0, 0, WIDTH, HEIGHT));
        }
        repaint();
    }
    
    @Override
    public AccessibleContext getAccessibleContext()
    {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {

                @Override
                public String getAccessibleName() {
                    return getName() + ": " + displayClassName;
                }

                // If we leave the default role, NVDA ignores this component.
                // List item works, and seemed like an okay fit
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.LIST_ITEM;
                }                
                
            };
        }
        return accessibleContext;
    }

    @Override
    public void keyPressed(KeyEvent arg0)
    {
        ob.keyPressed(arg0);
    }

    @Override
    public void keyReleased(KeyEvent arg0)
    {
        ob.keyReleased(arg0);
    }

    @Override
    public void keyTyped(KeyEvent arg0)
    {
        ob.keyTyped(arg0);
    }

    @Override
    public void focusGained(FocusEvent arg0)
    {
        ob.objectGotFocus(this);
    }

    @Override
    public void focusLost(FocusEvent arg0)
    {
        if (ob.getSelectedObject() == this)
        {
            ob.setSelectedObject(null);
        }
    }
}
