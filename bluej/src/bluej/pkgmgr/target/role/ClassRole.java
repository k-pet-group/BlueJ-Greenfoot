/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2016  Michael Kolling and John Rosenberg
 
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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import bluej.pkgmgr.target.DependentTarget.State;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import bluej.Config;
import bluej.debugmgr.ConstructAction;
import bluej.debugmgr.objectbench.InvokeAction;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;
import bluej.utility.BlueJFileReader;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class role in a class target, providing behaviour specific to particular
 * class types
 * 
 * @author Bruce Quig
 */
public abstract class ClassRole
{
    public final static String CLASS_ROLE_NAME = null;

    private final Color defaultbg = Config.getOptionalItemColour("colour.class.bg.default");
    protected final Color envOpColour = Config.ENV_COLOUR;

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
     * Return the default background colour for targets that don't want to
     * define their own colour.
     * @param width Width of total area to paint
     * @param height Height of total area to paint
     */
    public Paint getBackgroundPaint(int width, int height)
    {
        if (defaultbg != null) {
            return defaultbg;
        } else {
            Paint result;
            if (!Config.isRaspberryPi()){
                result = new GradientPaint(
                    0, 0, new Color(246,221,192),
                    0, height, new Color(245,204,155)); 
            }else{
                //return the average colour.
                result = new Color(246, 233, 174);
            }
            return result;
        }
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
        Hashtable<String,String> translations = new Hashtable<String,String>();
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
    public boolean createRoleMenu(ObservableList<MenuItem> menu, ClassTarget ct, Class<?> cl, State state)
    {
        return false;
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
    public boolean createRoleMenuEnd(ObservableList<MenuItem> menu, ClassTarget ct, State state)
    {
        return false;
    }

    /**
     * Creates a class menu containing the constructors.
     * 
     * @param menu
     *            the popup menu to add the class menu items to
     * @param cl
     *            Class object associated with this class target
     */
    @OnThread(Tag.FXPlatform)
    public boolean createClassConstructorMenu(ObservableList<MenuItem> menu, ClassTarget ct, Class<?> cl)
    {
        ViewFilter filter;
        View view = View.getView(cl);

        if (!java.lang.reflect.Modifier.isAbstract(cl.getModifiers())) {
            filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PACKAGE);
            ConstructorView[] constructors = view.getConstructors();

            if (createMenuItems(menu, constructors, filter, 0, constructors.length, "new ", ct))
                return true;
        }

        return false;
    }

    @OnThread(Tag.FXPlatform)
    public boolean createClassStaticMenu(ObservableList<MenuItem> menu, ClassTarget ct, boolean hasSource, Class<?> cl)
    {
        ViewFilter filter;
        View view = View.getView(cl);

        filter = new ViewFilter(ViewFilter.STATIC | ViewFilter.PACKAGE);
        MethodView[] allMethods = view.getAllMethods();
        if (createMenuItems(menu, allMethods, filter, 0, allMethods.length, "", ct))
            return true;

        return false;
    }

    /**
     * Create the menu items for the given members (constructors or methods).
     * @return  true if any items were created
     */
    @OnThread(Tag.FXPlatform)
    private static boolean createMenuItems(ObservableList<MenuItem> menu, CallableView[] members, ViewFilter filter, int first, int last,
            String prefix, InvokeListener il)
    {
        // Debug.message("Inside ClassTarget.createMenuItems\n first = " + first
        // + " last = " + last);
        boolean hasEntries = false;
        JMenuItem item;

        for (int i = first; i < last; i++) {
            try {
                CallableView m = members[last - i - 1];
                if (!filter.accept(m))
                    continue;
                // Debug.message("createSubMenu - creating MenuItem");

                Action callAction = null;
                if (m instanceof MethodView)
                {
                    MenuItem menuItem = new MenuItem(prefix + m.getLongDesc());
                    menu.add(menuItem);
                    menuItem.setOnAction(e -> SwingUtilities.invokeLater(() ->
                    {
                        new InvokeAction((MethodView)m, il, prefix + m.getLongDesc()).actionPerformed(null);
                    }));
                    hasEntries = true;
                }
                else if (m instanceof ConstructorView)
                {
                    MenuItem menuItem = new MenuItem(prefix + m.getLongDesc());
                    menu.add(menuItem);
                    menuItem.setOnAction(e -> SwingUtilities.invokeLater(() -> {
                        new ConstructAction((ConstructorView) m, il, prefix + m.getLongDesc()).actionPerformed(null);
                    }));
                    hasEntries = true;
                }
            }
            catch (Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return hasEntries;
    }
    // Swing version, for Greenfoot:
    public static boolean createMenuItems(JPopupMenu menu, CallableView[] members, ViewFilter filter, int first, int last,
                                          String prefix, InvokeListener il)
    {
        // Debug.message("Inside ClassTarget.createMenuItems\n first = " + first
        // + " last = " + last);
        boolean hasEntries = false;
        JMenuItem item;

        for (int i = first; i < last; i++) {
            try {
                CallableView m = members[last - i - 1];
                if (!filter.accept(m))
                    continue;
                // Debug.message("createSubMenu - creating MenuItem");

                Action callAction = null;
                if (m instanceof MethodView)
                    callAction = new InvokeAction((MethodView) m, il, prefix + m.getLongDesc());
                else if (m instanceof ConstructorView)
                    callAction = new ConstructAction((ConstructorView) m, il, prefix + m.getLongDesc());

                if (callAction != null) {
                    item = menu.add(callAction);
                    item.setFont(PrefMgr.getPopupMenuFont());
                    hasEntries = true;
                }
            }
            catch (Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return hasEntries;
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
        List<File> rlist = new ArrayList<File>();
        
        rlist.add(ct.getClassFile());
        rlist.addAll(Utility.mapList(ct.getAllSourceFilesJavaLast(), sf -> sf.file));
        rlist.add(ct.getContextFile());
        rlist.add(ct.getDocumentationFile());
        
        File [] innerClasses = ct.getInnerClassFiles();
        for (int i = 0; i < innerClasses.length; i++) {
            rlist.add(innerClasses[i]);
        }
        
        return rlist;
    }

    /**
     * True if this can be converted to Stride (assuming Java source is available;
     * this method does not need to check for that).  Returns false for unsupported
     * class types, like enums or unit tests.
     */
    @OnThread(Tag.Any)
    public abstract boolean canConvertToStride();
}
