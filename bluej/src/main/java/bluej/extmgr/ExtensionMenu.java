/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2019,2023  Michael Kolling and John Rosenberg
 
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
package bluej.extmgr;

import bluej.extensions2.MenuGenerator;
import javafx.scene.control.MenuItem;

import java.util.List;

/**
 * This interface provides methods for retrieving custom menu entries of an
 * extension and to notify it if a menu is about to show. Implementations of
 * this interface serve as a bridge between BlueJ and the extension mechanism.
 * 
 * @author Simon Gerlach
 */
public interface ExtensionMenu
{
    /**
     * Calls the extension to get a list of menu items.
     * 
     * @param menuGenerator
     *            The {@link MenuGenerator} which creates the menu.
     * @return The {@link MenuItem}s the extension provides or empty list
     *         if it does not provide a menu entry.
     */
    List<MenuItem> getMenuItems(MenuGenerator menuGenerator);

    /**
     * Post a notification about a menu going to be displayed.
     * 
     * @param menuGenerator
     *            The {@link MenuGenerator} which creates the menu.
     * @param onThisItem
     *            The list of {@link MenuItem}s which are about to show, as previously returned by getMenuItems (it may not be the same list instance exactly, but it will have the same content in the same order).
     */
    void postMenuItems(MenuGenerator menuGenerator, List<MenuItem> onThisItem);
}
