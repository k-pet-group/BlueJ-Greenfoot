/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016,2017,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target.role;

import bluej.Config;
import bluej.debugmgr.ConstructAction;
import bluej.debugmgr.objectbench.InvokeAction;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.actions.ClassTargetOperation;
import bluej.prefmgr.PrefMgr;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import bluej.views.ViewFilter.StaticOrInstance;
import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * A class role in a class target, providing behaviour specific to particular
 * class types
 * 
 * @author Bruce Quig
 */
public abstract class ClassRole
{
    public final static String CLASS_ROLE_NAME = null;

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return CLASS_ROLE_NAME;
    }

    /**
     * save details about the class target variant this role represents.
     * 
     * @param props
     *            the properties object associated with this target and role
     * @param modifiers
     *            modifiers for
     * @param prefix
     *            prefix to identifiy this role's target
     */
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, int modifiers, String prefix)
    {
    }

    /**
     * load existing information about this class role
     * 
     * @param props
     *            the properties object to read
     * @param prefix
     *            an internal name used for this target to identify its
     *            properties in a properties file used by multiple targets.
     */
    public void load(Properties props, String prefix)
        throws NumberFormatException
    {

    }

    /**
     * Get the "stereotype label" for this class role. This will be displayed
     * on classes in the UML diagram along with the class name. It may return
     * null if there is no stereotype label.
     */
    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return null;
    }

    /**
     * Generates a source code skeleton for this class.
     * 
     * @param template
     *            the name of the particular class template (just the base name
     *            without path and suffix)
     * @param pkg
     *            the package that the class target resides in
     * @param name
     *            the name of the class
     * @param sourceFile
     *            the name of the source file to be generated
     */
    public boolean generateSkeleton(String template, Package pkg, String name, String sourceFile)
    {
        Hashtable<String,String> translations = new Hashtable<>();
        translations.put("CLASSNAME", name);

        if (pkg.isUnnamedPackage()) {
            translations.put("PKGLINE", "");
        }
        else {
            translations.put("PKGLINE", "package " + pkg.getQualifiedName() + ";" + Config.nl + Config.nl);
        }
        // For Stride, we just put the package name:
        translations.put("PKGNAME", pkg.getQualifiedName());

        try {
            // Check for existing file. Normally this won't happen (the check for duplicate
            // target occurs prior to this) but on Windows filenames are case insensitive.
            File dest = new File(sourceFile);
            if (dest.exists()) {
                pkg.showError("duplicate-name");
                return false;
            }
            BlueJFileReader.translateFile(Config.getClassTemplateFile(template),
                    new File(sourceFile), translations,
                    Charset.forName("UTF-8"), pkg.getProject().getProjectCharset());
            return true;
        }
        catch (IOException e) {
            pkg.showError("skeleton-error");
            Debug.reportError("The default skeleton for the class could not be generated");
            Debug.reportError("Exception: " + e);
            return false;
        }
    }


    /**
     * Adds role specific items at the top of the popup menu for this class
     * target.
     * 
     * @param menu
     *            the menu object to add to
     * @param ct
     *            ClassTarget object associated with this class role
     * @param state
     *            the state of the ClassTarget
     * 
     * @return true if any menu items have been added
     */
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getRoleOperationsBegin(ClassTarget ct, Class<?> cl, State state)
    {
        return List.of();
    }

    /**
     * Adds role specific items at the bottom of the popup menu for this class
     * target.
     * 
     * @param menu
     *            the menu object to add to
     * @param ct
     *            ClassTarget object associated with this class role
     * @param state
     *            the state of the ClassTarget
     * 
     * @return true if any menu items have been added
     */
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getRoleOperationsEnd(ClassTarget ct, State state)
    {
        return List.of();
    }

    /**
     * Creates a class menu containing the constructors.
     * 
     * @param menu
     *            the popup menu to add the class menu items to
     * @param cl
     *            Class object associated with this class target
     * @return true if any menu items were added to the menu
     */
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getClassConstructorOperations(ClassTarget ct, Class<?> cl)
    {
        View view = View.getView(cl);

        if (!java.lang.reflect.Modifier.isAbstract(cl.getModifiers())) {
            ViewFilter filter = new ViewFilter(StaticOrInstance.INSTANCE, ct.getPackage().getQualifiedName());
            ConstructorView[] constructors = view.getConstructors();

            return createMenuItems(constructors, filter, 0, constructors.length, "new ", ct);
        }

        return List.of();
    }

    /**
     * If the given class has any static methods visible from the class target's package,
     * add actions to invoke them to the menu.
     * 
     * @param menu The menu to add any static methods to.
     * @param ct The class target for this ClassRole
     * @param cl The class for this ClassRole
     * @return true if any menu items were added to the menu
     */
    @OnThread(Tag.FXPlatform)
    public List<ClassTargetOperation> getClassStaticOperations(ClassTarget ct, Class<?> cl)
    {
        View view = View.getView(cl);

        ViewFilter filter = new ViewFilter(StaticOrInstance.STATIC, ct.getPackage().getQualifiedName());
        MethodView[] allMethods = view.getAllMethods();
        return createMenuItems(allMethods, filter, 0, allMethods.length, "", ct);
    }

    /**
     * Create the menu items for the given members (constructors or methods).
     * @return  true if any items were created
     */
    @OnThread(Tag.FXPlatform)
    private static List<ClassTargetOperation> createMenuItems(CallableView[] members, ViewFilter filter,
                                           int first, int last, String prefix, InvokeListener il)
    {
        // If we have a lot of items, we should create a submenu to fold some items in
        // 28 is a wild guess for now. It was 19 but with higher resolution screens, it became insufficient.
        int itemHeight = 28;
        int itemsOnScreen = (int)Config.screenBounds.getHeight() / itemHeight;
        int sizeLimit = itemsOnScreen / 2;
        
        ArrayList<ClassTargetOperation> ops = new ArrayList<>();

        for (int i = first; i < last; i++) {
            try {
                CallableView m = members[last - i - 1];
                if (!filter.test(m))
                    continue;
                // Debug.message("createSubMenu - creating MenuItem");

                if (m instanceof MethodView)
                {
                    ops.add(new ExecuteMethodAction(prefix + m.getLongDesc(), il, (MethodView)m));
                }
                else if (m instanceof ConstructorView)
                {
                    ops.add(new CallConstructorAction(prefix + m.getLongDesc(), il, (ConstructorView) m));
                }
            }
            catch (Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return ops;
    }

    public void run(PkgMgrFrame pmf, ClassTarget ct, String param)
    {}
    
    /**
     * Get all the files belonging to a class target - source, class, ctxt, docs
     * @param ct  The class target
     * @return  A list of File objects
     */
    public List<File> getAllFiles(ClassTarget ct)
    {
        // .frame (if available), .java, .class, .ctxt, and doc (.html)
        List<File> rlist = new ArrayList<>();
        
        rlist.add(ct.getClassFile());
        rlist.addAll(Utility.mapList(ct.getAllSourceFilesJavaLast(), sf -> sf.file));
        rlist.add(ct.getContextFile());
        rlist.add(ct.getDocumentationFile());
        
        File [] innerClasses = ct.getInnerClassFiles();
        Collections.addAll(rlist, innerClasses);
        
        return rlist;
    }

    /**
     * True if this can be converted to Stride (assuming Java source is available;
     * this method does not need to check for that).  Returns false for unsupported
     * class types, like enums or unit tests.
     */
    @OnThread(Tag.Any)
    public abstract boolean canConvertToStride();

    private static class ExecuteMethodAction extends ClassTargetOperation
    {
        private final InvokeListener il;
        private final MethodView m;

        public ExecuteMethodAction(String label, InvokeListener il, MethodView m)
        {
            super(label, Combine.ONE, null, label, MenuItemOrder.RUN_METHOD);
            this.il = il;
            this.m = m;
        }

        @Override
        protected void execute(ClassTarget target)
        {
            il.executeMethod(m);
        }
    }

    private static class CallConstructorAction extends ClassTargetOperation
    {
        private final InvokeListener il;
        private final ConstructorView c;

        public CallConstructorAction(String label, InvokeListener il, ConstructorView c)
        {
            super(label, Combine.ONE, null, label, MenuItemOrder.RUN_CONSTRUCTOR);
            this.il = il;
            this.c = c;
        }

        @Override
        protected void execute(ClassTarget target)
        {
            il.callConstructor(c);
        }
    }
}
