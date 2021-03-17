/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import bluej.utility.javafx.ResizableRectangle;
import bluej.views.ViewFilter.StaticOrInstance;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.When;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.inspector.ObjectBackground;
import bluej.extensions2.BObject;
import bluej.extensions2.ExtensionBridge;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.ExtensionsMenuManager;
import bluej.extmgr.ObjectExtensionMenu;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A wrapper around a Java object that handles calling methods, inspecting, etc.
 *
 * <p>The wrapper is represented by the red oval that is visible on the
 * object bench.
 *
 * @author  Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class ObjectWrapper extends StackPane implements InvokeListener, NamedValue
{
    // Strings
    @OnThread(Tag.Any)
    static final String methodException = Config.getString("debugger.objectwrapper.methodException");
    @OnThread(Tag.Any)
    static final String invocationException = Config.getString("debugger.objectwrapper.invocationException");
    @OnThread(Tag.Any)
    static final String inspect = Config.getString("debugger.objectwrapper.inspect");
    @OnThread(Tag.Any)
    static final String remove = Config.getString("debugger.objectwrapper.remove");
    @OnThread(Tag.Any)
    static final String redefinedIn = Config.getString("debugger.objectwrapper.redefined");
    @OnThread(Tag.Any)
    static final String inheritedFrom = Config.getString("debugger.objectwrapper.inherited");

    @OnThread(Tag.Any)
    static final Color envOpColour = new Color(152,32,32);
    
    public static final int WIDTH = 100;    // width including gap
    public static final int HEIGHT = 70;   // height including gap
    public static final double CORNER_SIZE = 36;
    public static final double FOCUSED_BORDER = 3.0;
    public static final double UNFOCUSED_BORDER = 1.0;
    public static final double SHADOW_RADIUS = 3.0;

    // wild guess until we find out.
    // It was 19 but with higher resolution screens, it became insufficient.
    private static int itemHeight = 28;
    private static boolean itemHeightKnown = false;
    @OnThread(Tag.Any)
    private static int itemsOnScreen;

    /** The Java object that this wraps */
    @OnThread(Tag.Any)
    protected final DebuggerObject obj;
    private final String objClassName; // Cache of obj.getClassName for thread-safety
    @OnThread(Tag.Any)
    // final by the end of the constructor:
    protected GenTypeClass iType;

    /** Fully qualified type this object represents, including type parameters */
    private final String className;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private String objInstanceName;
    protected String displayClassName;
    protected ContextMenu menu;
    
    protected final Rectangle highlight = new ResizableRectangle();
            

    // back references to the containers that we live in
    private final Package pkg;
    private final PkgMgrFrame pmf;
    private final ObjectBench ob;

    private static final String MENU_STYLE_INBUILT = "object-action-inbuilt";
    
    /**
     * Get an object wrapper for a user object. 
     * 
     * @param pmf   The package manager frame
     * @param ob    The object bench
     * @param obj   The object to wrap
     * @param iType   The static type of the object, used as a fallback if
     *                the runtime type is inaccessible
     * @param instanceName  The name for the object reference
     * @return A new object wrapper for the user's object
     */
    @OnThread(Tag.FXPlatform)
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

    @OnThread(Tag.FXPlatform)
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
        this.objClassName = obj.getClassName();
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

        Class<?> cl = findIType();
        ExtensionsManager extMgr = ExtensionsManager.getInstance();

        createMenu(extMgr, cl);
        JavaFXUtil.listenForContextMenu(this, (x, y) -> {
            menu.show(this, x, y);
            return true;
        }, KeyCode.SPACE, KeyCode.ENTER);

        setMinWidth(WIDTH);
        setMinHeight(HEIGHT);
        setMaxWidth(WIDTH);
        setMaxHeight(HEIGHT);
        setCursor(Cursor.HAND);

        setFocusTraversable(true);

        setOnMouseClicked(this::clicked);

        JavaFXUtil.addFocusListener(this, focused -> {
            if (focused)
                ob.objectGotFocus(this);
            else if (ob.getSelectedObject() == this)
                ob.setSelectedObject(null);
        });

        JavaFXUtil.addStyleClass(this, "object-wrapper");

        Label label = new Label(getName() + ":\n" + displayClassName);
        setAccessibleText(label.getText() + " Object");
        setAccessibleRole(AccessibleRole.NODE);
        JavaFXUtil.addStyleClass(label, "object-wrapper-text");
        createComponent(label);
        highlight.setMouseTransparent(true);
        highlight.setVisible(false);
        highlight.getStyleClass().add("object-debug-highlight");
    }

    protected void createComponent(Label label)
    {
        getChildren().addAll(new ObjectBackground(CORNER_SIZE, 
            new When(focusedProperty()).then(FOCUSED_BORDER).otherwise(UNFOCUSED_BORDER)), 
            label, highlight);
        setBackground(null);
        setEffect(new DropShadow(SHADOW_RADIUS, SHADOW_RADIUS/2.0, SHADOW_RADIUS/2.0, javafx.scene.paint.Color.GRAY));
    }

    @OnThread(Tag.Any)
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
    
    @OnThread(Tag.Any)
    public String getClassName()
    {
        return objClassName;
    }
    
    @OnThread(Tag.Any)
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
    @OnThread(Tag.FXPlatform)
    public JavaType getGenType()
    {
        return iType;
    }
    
    // --------- NamedValue interface --------------
    
    @Override
    @OnThread(Tag.Any)
    public boolean isFinal()
    {
        return true;
    }
    
    @Override
    @OnThread(Tag.Any)
    public boolean isInitialized()
    {
        return true;
    }
    
    // ----------------------------------------------
    
    @OnThread(value = Tag.Any, requireSynchronized = true)
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
        Project proj = pkg.getProject();
        proj.removeInspectorInstance(obj);
    }

    /**
     * Check whether the given class is accessible (from this wrapper's package)
     * 
     * @param cl  The class to check for accessibility
     * @return    True if the class is accessible, false otherwise
     */
    @OnThread(Tag.FXPlatform)
    private boolean classIsAccessible(Class<?> cl)
    {
        int clMods = cl.getModifiers();
        String classPackage = JavaNames.getPrefix(cl.getName());
        return !(Modifier.isProtected(clMods) && !pkg.getQualifiedName().equals(classPackage)
                || Modifier.isPrivate(clMods));
    }
    
    private Class<?> unwrapArrays(String className)
    {
        String memberType = className.substring(0, className.length() - 2);
        if (memberType.endsWith("[]"))
        {
            // Nested array:
            return Array.newInstance(unwrapArrays(memberType), 0).getClass();
        }
        switch (memberType)
        {
            case "boolean":
                return boolean[].class;
            case "byte":
                return byte[].class;
            case "short":
                return short[].class;
            case "int":
                return int[].class;
            case "long":
                return long[].class;
            case "float":
                return float[].class;
            case "double":
                return double[].class;
            case "char":
                return char[].class;
        }
        return Array.newInstance(pkg.loadClass(memberType), 0).getClass();
    }
    
    /**
     * Determine an appropriate type to use for this object in shell files.
     * The type must be accessible in the current package.
     * 
     * iType will be set to the chosen type.
     * 
     * @return  The class of the chosen type.
     */
    @OnThread(Tag.FXPlatform)
    private Class<?> findIType()
    {
        String className = obj.getClassName();
        Class<?> cl = pkg.loadClass(className);
        if (cl == null && obj.isArray() && className.endsWith("[]"))
        {
            cl = unwrapArrays(className);
        }
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
    protected void createMenu(ExtensionsManager extMgr, Class<?> cl)
    {
        menu = new ContextMenu();

        // add the menu items to call the methods
        createMethodMenuItems(menu.getItems(), cl, iType, this, pkg.getQualifiedName(), true);

        // add inspect and remove options
        MenuItem item;
        menu.getItems().add(item = new MenuItem(inspect));
        JavaFXUtil.addStyleClass(item, MENU_STYLE_INBUILT);
        item.setOnAction(e -> inspectObject());
  
        menu.getItems().add(item = new MenuItem(remove));
        JavaFXUtil.addStyleClass(item, MENU_STYLE_INBUILT);
        item.setOnAction(e -> removeObject());

        ExtensionsMenuManager menuManager = new ExtensionsMenuManager(menu, extMgr, new ObjectExtensionMenu(this));
        menuManager.addExtensionMenu(pkg.getProject());
    }
    
    /**
     * Creates the menu items for all the methods in the class, which is a raw
     * class type.
     * 
     * @param menu  The menu to add the menu items to
     * @param cl    The class whose methods to add
     * @param il    The invoke listener to notify when a method is called
     * @param currentPackageName Name of the package that this object will be
     *            shown from (used to determine wheter to show package protected
     *            methods)
     * @param showObjectMethods Whether to show the submenu with methods from java.lang.Object
     */
    public static void createMethodMenuItems(ObservableList<MenuItem> menu, Class<?> cl, InvokeListener il,
                                             String currentPackageName, boolean showObjectMethods)
    {
        GenTypeClass gt = new GenTypeClass(new JavaReflective(cl));
        createMethodMenuItems(menu, cl, gt, il, currentPackageName, showObjectMethods);
    }

    /**
     * Creates the menu items for all the methods in the class
     * 
     * @param menu  The menu to add the menu items to
     * @param cl    The class whose methods to add
     * @param gtype  The generic type of the class
     * @param il    The invoke listener to notify when a method is called
     * @param currentPackageName Name of the package that this object will be
     *            shown from (used to determine wheter to show package protected
     *            methods)
     * @param showObjectMethods Whether to show the submenu for methods inherited from java.lang.Object
     */
    public static void createMethodMenuItems(ObservableList<MenuItem> menu, Class<?> cl, GenTypeClass gtype, InvokeListener il,
                                             String currentPackageName, boolean showObjectMethods)
    {
        if (cl != null) {
            View view = View.getView(cl);
            Hashtable<String, String> methodsUsed = new Hashtable<>();
            List<Class<?>> classes = getClassHierarchy(cl);

            // define a view filter
            ViewFilter filter = new ViewFilter(StaticOrInstance.INSTANCE, currentPackageName);

            menu.add(new SeparatorMenuItem());

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
          
            createMenuItems(menu, declaredMethods, il, filter, itemLimit, curType.getMap(), methodsUsed);

            // create submenus for superclasses
            for(int i = 1; i < classes.size(); i++ ) {
                Class<?> currentClass = classes.get(i);
                view = View.getView(currentClass);
                
                // Determine visibility of package private / protected members
                filter = new ViewFilter(StaticOrInstance.INSTANCE, currentPackageName);

                // map generic type paramaters to the current superclass
                curType = curType.mapToSuper(currentClass.getName());
                
                if (!"java.lang.Object".equals(currentClass.getName()) || showObjectMethods) { 
                    declaredMethods = view.getDeclaredMethods();
                    Menu subMenu = new Menu(inheritedFrom + " "
                                   + JavaNames.stripPrefix(currentClass.getName()));
                    createMenuItems(subMenu.getItems(), declaredMethods, il, filter, (itemsOnScreen / 2), curType.getMap(), methodsUsed);
                    menu.add(0, subMenu);
                }
            }
            // Create submenus for interfaces which have default methods:
            for (Class<?> iface : getInterfacesWithDefaultMethods(cl))
            {
                view = View.getView(iface);
                declaredMethods = view.getDeclaredMethods();
                Menu subMenu = new Menu(inheritedFrom + " "
                        + JavaNames.stripPrefix(iface.getName()));
                createMenuItems(subMenu.getItems(), declaredMethods, il, filter, (itemsOnScreen / 2), curType.getMap(), methodsUsed);
                menu.add(0, subMenu);
            }

            menu.add(new SeparatorMenuItem());
        }
    }
    /**
     * creates the individual menu items for an object's popup menu.
     * The method checks for previously defined methods with the same signature
     * and appends information referring to this.
     *
     * @param menu          the menu that the items are to be created for
     * @param methods       the methods for which menu items should be created
     * @param il            the listener to be notified when a method should be called interactively
     * @param filter        the filter which decides on which methods should be shown
     * @param sizeLimit     the limit to which the menu should grow before openeing submenus
     * @param genericParams the mapping of generic type parameter names to their corresponding
     *                      types in the object instance (a map of String -> GenType).
     * @param methodsUsed   the table to store the methods that already been ddealt
     */
    private static void createMenuItems(List<MenuItem> menu, MethodView[] methods, InvokeListener il, ViewFilter filter,
                                        int sizeLimit, Map<String, GenTypeParameter> genericParams, Hashtable<String, String> methodsUsed)
    {
        MenuItem item;
        boolean menuEmpty = true;

        Arrays.sort(methods);
        for (MethodView method : methods) {
            try {
                if (!filter.test(method))
                    continue;

                menuEmpty = false;
                String methodSignature = method.getCallSignature();   // uses types for params
                String methodDescription = method.getLongDesc(genericParams); // uses names for params

                // check if method signature has already been added to a menu
                if (methodsUsed.containsKey(methodSignature)) {
                    methodDescription = methodDescription
                             + "   [ " + redefinedIn + " "
                             + JavaNames.stripPrefix(
                                   methodsUsed.get(methodSignature))
                             + " ]";
                }
                else {
                    methodsUsed.put(methodSignature, method.getClassName());
                }

                item = new MenuItem(methodDescription);
                item.setOnAction(e -> il.executeMethod(method));

                // check whether it's time for a submenu
                int itemCount = menu.size();
                if (itemCount >= sizeLimit) {
                    Menu subMenu = new Menu(Config.getString("debugger.objectwrapper.moreMethods"));
                    menu.add(subMenu);
                    menu = subMenu.getItems();
                    sizeLimit = itemsOnScreen / 2;
                }

                menu.add(item);
            } catch (Exception e) {
                Debug.reportError(methodException + e);
                e.printStackTrace();
            }
        }
        
        // If there are no accessible methods, insert a message which says so.
        if (menuEmpty) {
            MenuItem mi = new MenuItem(Config.getString("debugger.objectwrapper.noMethods"));
            mi.setDisable(true);
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
    @OnThread(Tag.Any)
    private static List<Class<?>> getClassHierarchy(Class<?> derivedClass)
    {
        Class<?> currentClass = derivedClass;
        List<Class<?>> classVector = new ArrayList<>();
        while(currentClass != null) {
            classVector.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }
        return classVector;
    }

    /**
     * Gets a list containing interfaces implemented by the given class, anywhere
     * in its parent hierarchy, which have default methods (regardless of whether
     * the methods are overridden elsewhere in the hierarchy).
     *
     * So if you have interface I with default method getI, and interface J with no
     * default methods, and: class A implements I, class B extends A implements J,
     * and class C extends B, then calling getInterfacesWithDefaultMethods for
     * A, B or C will return a singleton list with I in it.
     *
     * @param cls The class whose implements interfaces are to be searched.
     * @return The list containing the implemented interfaces which have default methods.
     */
    @OnThread(Tag.Any)
    private static List<Class<?>> getInterfacesWithDefaultMethods(Class<?> cls)
    {
        return getClassHierarchy(cls).stream()
                .flatMap(c -> Arrays.stream(c.getInterfaces()))
                .filter(i -> Arrays.stream(i.getDeclaredMethods()).anyMatch(m -> m.isDefault()))
                .collect(Collectors.toList());
    }
    
    @Override
    @OnThread(value = Tag.Any, ignoreParent = true)
    public synchronized String getName()
    {
        return objInstanceName;
    }
    
    @OnThread(Tag.Any)
    public synchronized void setName(String newName)
    {
        objInstanceName = newName;
    }

    @OnThread(Tag.Any)
    public DebuggerObject getObject()
    {
        return obj;
    }

    /**
     * Process a mouse click into this object. If it was a popup event, show the object's
     * menu. If it was a double click, inspect the object. If it was a normal mouse click,
     * insert it into a parameter field (if any).
     */
    private void clicked(MouseEvent evt)
    {
        // Don't process popup here, done elsewhere
        if (!evt.isPopupTrigger() && evt.getButton() == MouseButton.PRIMARY) {
            if (evt.getClickCount() > 1) // double click
                inspectObject();
            else { //single click
                ob.fireObjectSelectedEvent(this);
            }
        }
        //manage focus
        requestFocus();
    }

    /**
     * Open this object for inspection.
     */
    @OnThread(Tag.FXPlatform)
    protected void inspectObject()
    {
        InvokerRecord ir = new ObjectInspectInvokerRecord(getName());
        pkg.getProject().getInspectorInstance(obj, getName(), pkg, ir, pmf.getWindow(), this);  // shows the inspector
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
    @OnThread(Tag.FXPlatform)
    public void executeMethod(final MethodView method)
    {
        ResultWatcher watcher = null;

        pkg.forgetLastSource();

        String instanceName = getName();
        watcher = new BluejResultWatcher(obj, instanceName, pkg, pmf, method) {

            @Override
            protected void addInteraction(InvokerRecord ir)
            {
                ob.addInteraction(ir);
            }
        };

        if (pmf.checkDebuggerState()) {
            Invoker invoker = new Invoker(pmf, method, instanceName, obj, watcher);
            invoker.invokeInteractive();
        }
    }

    @Override
    public void callConstructor(ConstructorView cv)
    {
        // do nothing (satisfy the InvokeListener interface)
    }

    /**
     * @param isSelected The isSelected to set.
     */
    public void setSelected(boolean isSelected) 
    {
        if(isSelected) {
            pmf.setStatus(getName() + " : " + displayClassName);
        }
    }

    public void showMenu()
    {
        menu.show(this, Side.LEFT, 5, 5);
    }

    public void animateIn(Optional<Point2D> animateFromScenePoint)
    {
        // Set scale now; otherwise you can briefly see a flash of
        // full size on screen before it scales back down with animation start:
        setScaleX(0.2);
        setScaleY(0.2);
        // Don't start the animations until we are in our right position:
        JavaFXUtil.listenOnce(layoutYProperty(), layoutY -> {
            setVisible(true);
            ScaleTransition scale = new ScaleTransition(Duration.millis(300), this);
            scale.setFromX(0.2);
            scale.setFromY(0.2);
            scale.setToX(1.0);
            scale.setToY(1.0);
            if (animateFromScenePoint.isPresent())
            {
                TranslateTransition move = new TranslateTransition(Duration.millis(300), this);
                Point2D local = sceneToLocal(animateFromScenePoint.get());
                move.setFromX(local.getX());
                move.setFromY(local.getY());
                move.setToX(0.0);
                move.setToY(0.0);

                // If we are moving a long way, increase the animation time:
                if (Math.hypot(local.getX(), local.getY()) >= 300.0)
                {
                    scale.setDuration(Duration.millis(600.0));
                    move.setDuration(Duration.millis(600.0));
                }

                new ParallelTransition(scale, move).play();
            }
            else
            {
                scale.play();
            }
        });

    }
    
    public void animateOut(FXPlatformRunnable after)
    {
        ScaleTransition t = new ScaleTransition(Duration.millis(300), this);
        t.setToX(0.0);
        t.setToY(0.0);
        t.setOnFinished(e -> {
            if (after != null)
                after.run();
        });
        t.play();
    }

    /**
     * Sets the highlight (for current object while debugging) on or off
     * @param highlightOn True to highlight this object, false to turn it off
     */
    public void setHighlight(boolean highlightOn)
    {
        highlight.setVisible(highlightOn);
    }

    /*
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
    */

}
