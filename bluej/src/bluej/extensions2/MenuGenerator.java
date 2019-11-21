/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009,2012,2018,2019  Michael Kolling and John Rosenberg

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
package bluej.extensions2;

import javafx.scene.control.MenuItem;

/**
 * Extensions which wish to add a menu item to BlueJ's menus should register an
 * instance of MenuGenerator with the BlueJ proxy object.
 * <p>
 * A MenuGenerator provides a set of functions which can be called back
 * by BlueJ to request the actual menu items which will be displayed, and
 * to indicate that a particular menu item is about to be displayed, so
 * that an extension can (e.g.) enable or disable appropriate items.
 * <p>
 * Note that the MenuItem which is returned by the extension can itself
 * be a Menu, allowing extensions to build more complex menu structures, but
 * that the "notify" methods below will only be called for the item which has
 * actually been added, and not any subsidiary items.
 * <p>
 * Note that the MenuGenerator's get*MenuItem() methods:
 * <ol>
 *     <li>may be called more than once during a BlueJ session, they should return a new set of MenuItems for each invocation.
 *     This is a restriction required by the JavaFX implementation, which does not allow sharing of MenuItems between menus.
 *     Of course, ActionEvent's handlers can be shared between all of the appropriate MenuItems.</li>
 * <li>may not be called between the registration of a new MenuGenerator and the display of a menu.
 * That is to say old menu items may still be active for previously registered menus, despite the registration of a new MenuGenerator.</li>
 * <li>will be called at least once for every menu which is displayed.</li>
 *</ol>
 * @author Damiano Bolla, University of Kent at Canterbury. January 2003
 */
public class MenuGenerator
{
    /**
     * Returns the MenuItem to be added to the BlueJ Tools menu.
     * Extensions should not retain references to the menu items created.
     * @param bPackage a {@link BPackage} object wrapping the BlueJ package with which this menu item will be associated.
     * @return This method should return a {@link MenuItem} object to be added to the Tools menu for this extensions, <code>null</code> is returned by default.
     */
    public MenuItem getToolsMenuItem(BPackage bPackage)
    {
        return null;
    }

    /**
     * Returns the MenuItem to be added to the BlueJ Package menu. Extensions
     * should not retain references to the menu items created.
     *
     * @param bPackage a {@link BPackage} object wrapping the BlueJ package with which this menu item will be associated.
     * @return This method should return a {@link MenuItem} object to be added to the Ppackage menu for this extensions, <code>null</code> is returned by default.
     */
    public MenuItem getPackageMenuItem(BPackage bPackage)
    {
        return null;
    }

    /**
     * Returns the MenuItem to be added to the BlueJ Class menu
     * Extensions should not retain references to the menu items created.
     * @param bClass a {@link BClass} object wrapping the BlueJ class with which this menu item will be associated.
     * @return This method should return a {@link MenuItem} object to be added to the Class menu for this extensions, <code>null</code> is returned by default.
     */
    public MenuItem getClassMenuItem(BClass bClass)
    {
        return null;
    }

    /**
     * Returns the MenuItem to be added to the BlueJ Object menu.
     * Extensions should not retain references to the menu items created.
     * @param bObject a {@link BObject} object wrapping the BlueJ Object with which this menu item will be associated.
     * @return This method should return a {@link MenuItem} object to be added to the Object menu for this extensions, <code>null</code> is returned by default.
     */
    public MenuItem getObjectMenuItem(BObject bObject)
    {
        return null;
    }

    /**
     * Called by BlueJ when a tools menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bPackage a {@link BPackage} object wrapping the BlueJ package for which this menu is to be displayed.
     * @param menuItem a {@link MenuItem} object about to be be displayed (as provided by the
     * extension in a previous call to {@link #getToolsMenuItem(BPackage)}).
     */
    public void notifyPostToolsMenu(BPackage bPackage, MenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when a package menu added by an extension is about to be
     * displayed. An extension can use this notification to decide whether to
     * enable/disable menu items and so on.
     *
     * @param bPackage a {@link BPackage} object wrapping the BlueJ package for which this menu is to be displayed.
     * @param menuItem a {@link MenuItem} object about to be be displayed (as provided by the
     * extension in a previous call to {@link #getPackageMenuItem(BPackage)}).
     */
    public void notifyPostPackageMenu(BPackage bPackage, MenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when a class menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bClass a {@link BClass} object wrapping the BlueJ class for which this menu is to be displayed.
     * @param menuItem a {@link MenuItem} object about to be be displayed (as provided by the
     * extension in a previous call to {@link #getClassMenuItem(BClass)}).
     */
    public void notifyPostClassMenu(BClass bClass, MenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when an object menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bObject a {@link BObject} object wrapping the BlueJ object for which this menu is to be displayed.
     * @param menuItem a {@link MenuItem} object about to be be displayed (as provided by the
     * extension in a previous call to {@link #getObjectMenuItem(BObject)}).
     */
    public void notifyPostObjectMenu(BObject bObject, MenuItem menuItem)
    {
        return;
    }

}
