package bluej.pkgmgr.target.role;

import java.awt.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import bluej.Config;
import bluej.debugmgr.ConstructAction;
import bluej.debugmgr.objectbench.InvokeAction;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;
import bluej.utility.*;
import bluej.views.*;

/**
 * A class role in a class target, providing behaviour specific to particular
 * class types
 * 
 * @author Bruce Quig
 * @version $Id: ClassRole.java 3630 2005-10-03 00:50:38Z davmac $
 */
public abstract class ClassRole
{
    public final static String CLASS_ROLE_NAME = null;

    private final Color defaultbg = Config.getItemColour("colour.class.bg.default");
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

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
     */
    public Color getBackgroundColour()
    {
        return defaultbg;
    }

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
        Hashtable translations = new Hashtable();
        translations.put("CLASSNAME", name);

        if (pkg.isUnnamedPackage())
            translations.put("PKGLINE", "");
        else
            translations.put("PKGLINE", "package " + pkg.getQualifiedName() + ";" + Config.nl + Config.nl);

        try {
            // Check for existing file. Normally this won't happen (the check for duplicate
            // target occurs prior to this) but on Windows filenames are case insensitive.
            File dest = new File(sourceFile);
            if (dest.exists()) {
                pkg.showError("duplicate-name");
                return false;
            }
            BlueJFileReader.translateFile(Config.getClassTemplateFile(template), new File(sourceFile), translations);
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
     * Adds a single item to this roles popup menu.
     * 
     * This method is used by ClassTarget to add some standard menus as well as
     * by the roles to add menus. It should be overridden with caution.
     * 
     * @param menu
     *            the popup menu the item is to be added to
     * @param action
     *            the action to be registered with this menu item
     * @param itemString
     *            the String to be displayed on menu item
     * @param enabled
     *            boolean value representing whether item should be enabled
     *  
     */
    public void addMenuItem(JPopupMenu menu, Action action, boolean enabled)
    {
        JMenuItem item;

        item = new JMenuItem();
        item.setAction(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        item.setEnabled(enabled);

        menu.add(item);
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
    public boolean createRoleMenu(JPopupMenu menu, ClassTarget ct, Class cl, int state)
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
    public boolean createRoleMenuEnd(JPopupMenu menu, ClassTarget ct, int state)
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
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class cl)
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

    public boolean createClassStaticMenu(JPopupMenu menu, ClassTarget ct, Class cl)
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

    /**
     * Removes applicable files (.class, .java and .ctxt) prior to this
     * ClassRole being removed from a Package.
     *  
     */
    public void prepareFilesForRemoval(ClassTarget ct, String sourceFile, String classFile, String contextFile)
    {
        File sourceFileName = new File(sourceFile);
        if (sourceFileName.exists()) {
            sourceFileName.delete();
        }
        File classFileName = new File(classFile);
        if (classFileName.exists())
            classFileName.delete();

        File contextFileName = new File(contextFile);
        if (contextFileName.exists())
            contextFileName.delete();
    }

//    /**
//     * Draw role specific elements of this class.
//     */
//    public void draw(Graphics2D g, ClassTarget ct, int x, int y, int width, int height)
//    {}  // currently unused

    public void run(PkgMgrFrame pmf, ClassTarget ct, String param)
    {}
}