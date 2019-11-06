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
 *
 * @author Damiano Bolla, University of Kent at Canterbury. January 2003
 */
public class MenuGenerator
{
    /**
     * Returns the MenuItem to be added to the BlueJ Tools menu.
     * Extensions should not retain references to the menu items created.
     * @param bp the BlueJ package with which this menu item will be associated.
     */
    public MenuItem getToolsMenuItem(BPackage bp)
    {
        return null;
    }

    /**
     * Returns the MenuItem to be added to the BlueJ Package menu. Extensions
     * should not retain references to the menu items created.
     *
     * @param bPackage
     *            The BlueJ package with which this menu item will be
     *            associated.
     * @return The MenuItem to be added to the BlueJ Package menu.
     */
    public MenuItem getPackageMenuItem(BPackage bPackage)
    {
        return null;
    }

    /**
     * Returns the JMenuItem to be added to the BlueJ Class menu
     * Extensions should not retain references to the menu items created.
     * @param bc the BlueJ class with which this menu item will be associated.
     */
    public MenuItem getClassMenuItem(BClass bc)
    {
        return null;
    }

    /**
     * Returns the MenuItem to be added to the BlueJ Object menu.
     * Extensions should not retain references to the menu items created.
     * @param bo the BlueJ object with which this menu item will be associated.
     */
    public MenuItem getObjectMenuItem(BObject bo)
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
     * @param mi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostToolsMenu(BPackage bp, MenuItem mi)
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
    public void notifyPostPackageMenu(BPackage bPackage, MenuItem menuItem)
    {
        return;
    }

    /**
     * Called by BlueJ when a class menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bc the BlueJ class for which the menu is to be displayed
     * @param mi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostClassMenu(BClass bc, MenuItem mi)
    {
        return;
    }

    /**
     * Called by BlueJ when an object menu added by an extension is about to
     * be displayed. An extension can use this notification to decide whether
     * to enable/disable menu items and so on.
     * @param bo the BlueJ object for which the menu is to be displayed
     * @param mi the menu item which will be displayed (as provided by the
     * extension in a previous call to getToolsMenuItem)
     */
    public void notifyPostObjectMenu(BObject bo, MenuItem mi)
    {
        return;
    }

}
