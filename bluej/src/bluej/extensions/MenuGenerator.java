/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;

import javax.swing.JMenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ's menus should register an
 * instance of MenuGenerator with the BlueJ proxy object.  
 * <p>
 * A MenuGenerator provides a set of functions which can be called back
 * by BlueJ to request the actual menu items which will be displayed, and
 * to indicate that a particular menu item is about to be displayed, so
 * that an extension can (e.g.) enable or disable appropriate items.
 * <p>
 * Note that the JMenuItem which is returned by the extension can itself
 * be a JMenu, allowing extensions to build more complex menu structures, but
 * that the "notify" methods below will only be called for the item which has
 * actually been added, and not any subsidiary items.
 * <p>
 * Below is a simple example which creates menus for Tools, Classes and Objects. 
 * <p>
 * To activate the menus you instantiate an object of the MenuGenerator class
 * and then register it with the BlueJ proxy object, e.g.:
 * <pre>
 *        MenuBuilder myMenus = new MenuBuilder();
 *        bluej.setMenuGenerator(myMenus);
 * </pre>
 * Note that the MenuGenerator's <code>get*MenuItem()</code> methods:<ol>
 * <li>may be called more than 
 * once during a BlueJ session, they should return a new set of MenuItems for each 
 * invocation. This is a restriction required by the Swing implementation, which 
 * does not allow sharing of MenuItems between menus. You can, of course, share 
 * MenuActions between all of the appropriate MenuItems.
 * <li>may not be called between the registration of a new <code>MenuGenerator</code>
 * and the display of a menu. That is to say old menu items may still be active for previously
 * registered menus, despite the registration of a new <code>MenuGenerator</code>.
 * <li>will be called at least once for every menu which is displayed.
 * </ol>
 *
 * The code for the example MenuBuilder class is:
 * <PRE>
 * import bluej.extensions.*;
 * import javax.swing.*;
 * import java.awt.event.*;
 *
 * class MenuBuilder extends MenuGenerator {
 *     private BPackage curPackage;
 *     private BClass curClass;
 *     private BObject curObject;
 * 
 *     public JMenuItem getToolsMenuItem(BPackage aPackage) {
 *         return new JMenuItem(new SimpleAction("Click Tools", "Tools menu:"));
 *     }
 * 
 *     public JMenuItem getClassMenuItem(BClass aClass) {
 *         return new JMenuItem(new SimpleAction("Click Class", "Class menu:"));
 *     }
 * 
 *     public JMenuItem getObjectMenuItem(BObject anObject) {
 *         return new JMenuItem(new SimpleAction("Click Object", "Object menu:"));
 *     }
 *     
 *     // These methods will be called when
 *     // each of the different menus are about to be invoked.
 *     public void notifyPostToolsMenu(BPackage bp, JMenuItem jmi) {
 *         System.out.println("Post on Tools menu");
 *         curPackage = bp ; curClass = null ; curObject = null;
 *     }
 *     
 *     public void notifyPostClassMenu(BClass bc, JMenuItem jmi) {
 *         System.out.println("Post on Class menu");
 *         curPackage = null ; curClass = bc ; curObject = null;
 *     }
 *     
 *     public void notifyPostObjectMenu(BObject bo, JMenuItem jmi) {
 *         System.out.println("Post on Object menu");
 *         curPackage = null ; curClass = null ; curObject = bo;
 *     }
 *     
 *     // A utility method which pops up a dialog detailing the objects 
 *     // involved in the current (SimpleAction) menu invocation.
 *     private void showCurrentStatus(String header) {
 *         try {
 *             if (curObject != null)
 *                 curClass = curObject.getBClass();
 *             if (curClass != null)
 *                 curPackage = curClass.getPackage();
 *                 
 *             String msg = header;
 *             if (curPackage != null)
 *                 msg += "\nCurrent Package = " + curPackage;
 *             if (curClass != null)
 *                 msg += "\nCurrent Class = " + curClass;
 *             if (curObject != null)
 *                 msg += "\nCurrent Object = " + curObject;
 *             JOptionPane.showMessageDialog(null, msg);
 *         } catch (Exception exc) { }
 *     }
 *     
 *     // The nested class that instantiates the different (simple) menus.
 *     class SimpleAction extends AbstractAction {
 *         private String msgHeader;
 *         
 *         public SimpleAction(String menuName, String msg) {
 *             putValue(AbstractAction.NAME, menuName);
 *             msgHeader = msg;
 *         }
 *         public void actionPerformed(ActionEvent anEvent) {
 *             showCurrentStatus(msgHeader);
 *         }
 *     }
 * }
 * </PRE>
 *
 * @author Damiano Bolla, University of Kent at Canterbury. January 2003
 */
public class MenuGenerator
{
    /**
     * Returns the JMenuItem to be added to the BlueJ Tools menu.
     * Extensions should not retain references to the menu items created.
     * @deprecated As of BlueJ 1.3.5, replaced by {@link #getToolsMenuItem(BPackage bp)}
     */
    public JMenuItem getMenuItem( )
    {
        return null;
    }

    /**
     * Returns the JMenuItem to be added to the BlueJ Tools menu.
     * Extensions should not retain references to the menu items created.
     * @param bp the BlueJ package with which this menu item will be associated.
     */
    public JMenuItem getToolsMenuItem(BPackage bp)
    {
        return null;
    }

    /**
     * Returns the JMenuItem to be added to the BlueJ View menu. Extensions
     * should not retain references to the menu items created.
     * 
     * @param bPackage
     *            The BlueJ package with which this menu item will be
     *            associated.
     * @return The JMenuItem to be added to the BlueJ View menu.
     */
    public JMenuItem getViewMenuItem(BPackage bPackage)
    {
        return null;
    }

    /**
     * Returns the JMenuItem to be added to the BlueJ Package menu. Extensions
     * should not retain references to the menu items created.
     * 
     * @param bPackage
     *            The BlueJ package with which this menu item will be
     *            associated.
     * @return The JMenuItem to be added to the BlueJ Package menu.
     */
    public JMenuItem getPackageMenuItem(BPackage bPackage)
    {
        return null;
    }
  
    /**
     * Returns the JMenuItem to be added to the BlueJ Class menu
     * Extensions should not retain references to the menu items created.
     * @param bc the BlueJ class with which this menu item will be associated.
     */
    public JMenuItem getClassMenuItem(BClass bc)
    {
        return null;
    }

    /**
     * Returns the JMenuItem to be added to the BlueJ Object menu.
     * Extensions should not retain references to the menu items created.
     * @param bo the BlueJ object with which this menu item will be associated.
     */
    public JMenuItem getObjectMenuItem(BObject bo)
    {
        return null;
    }

    /**
     * Called by BlueJ when a tools menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on. <em>Note:</em> Due to a bug in
     * Apple's current Java implementation, this method will not be called when
     * is running on a Mac. It will start working as soon as there's a fix.
     * @param bp the BlueJ package for which the menu is to be displayed
     * @param jmi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostToolsMenu(BPackage bp, JMenuItem jmi) 
    {
        return;
    }

    /**
     * Called by BlueJ when a view menu added by an extension is about to be
     * displayed. An extension can use this notification to decide whether to
     * enable/disable menu items and so on.
     * 
     * @param bPackage
     *            The BlueJ package for which the menu is to be displayed.
     * @param menuItem
     *            The menu item which will be displayed (as provided by the
     *            extension in a previous call to
     *            {@link #getViewMenuItem(BPackage)}).
     */
    public void notifyPostViewMenu(BPackage bPackage, JMenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when a package menu added by an extension is about to be
     * displayed. An extension can use this notification to decide whether to
     * enable/disable menu items and so on.
     * 
     * @param bPackage
     *            The BlueJ package for which the menu is to be displayed.
     * @param menuItem
     *            The menu item which will be displayed (as provided by the
     *            extension in a previous call to
     *            {@link #getPackageMenuItem(BPackage)}).
     */
    public void notifyPostPackageMenu(BPackage bPackage, JMenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when a class menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bc the BlueJ class for which the menu is to be displayed
     * @param jmi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostClassMenu(BClass bc, JMenuItem jmi) 
    {
        return;
    }

    /**
     * Called by BlueJ when an object menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bo the BlueJ object for which the menu is to be displayed
     * @param jmi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostObjectMenu(BObject bo, JMenuItem jmi) 
    {
        return;
    }

}
