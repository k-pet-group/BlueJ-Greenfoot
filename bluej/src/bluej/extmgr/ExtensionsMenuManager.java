/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2016,2019  Michael Kolling and John Rosenberg
 
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

import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXPlatformSupplier;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;


/**
 * Manages the interface between a menu and extensions.
 * An instance of this class is attached to each popup menu that extensions
 * may add items to.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004,2005
 */
@OnThread(Tag.FXPlatform)
public final class ExtensionsMenuManager
{
    private final ExtensionsManager extMgr;
    private final SeparatorMenuItem menuSeparator;
    // Either popupMenu will be non-null, or menu, but not both:
    @OnThread(Tag.Any)
    private final ContextMenu popupMenu;
    @OnThread(Tag.Any)
    private final Menu menu;

    @OnThread(Tag.Any)
    public synchronized void setMenuGenerator(ExtensionMenu menuGenerator)
    {
        this.menuGenerator = menuGenerator;
    }

    @OnThread(value = Tag.Any,requireSynchronized = true)
    private ExtensionMenu menuGenerator;

    /**
     * Constructor for the MenuManager object.
     *
     * @param  aPopupMenu  The menu that extensions are attaching to.
     * @param menuGenerator
     */
    public ExtensionsMenuManager(ContextMenu aPopupMenu, ExtensionsManager extMgr, ExtensionMenu menuGenerator)
    {
        this.extMgr = extMgr;
        this.menuGenerator = menuGenerator;
        menuSeparator = new SeparatorMenuItem();
        this.popupMenu = aPopupMenu;
        popupMenu.setOnShowing(this::menuWillBecomeVisible);
        this.menu = null;
    }
    public ExtensionsMenuManager(Menu aMenu, ExtensionsManager extMgr, ExtensionMenu menuGenerator)
    {
        this.extMgr = extMgr;
        this.menuGenerator = menuGenerator;
        menuSeparator = new SeparatorMenuItem();
        this.menu = aMenu;
        menu.setOnShowing(this::menuWillBecomeVisible);
        this.popupMenu = null;
    }

    /**
     * Add all the menu currently available to the menu.
     * This may be called any time BlueJ feels that the menu needs to be updated.
     *
     * @param  onThisProject  a specific project to look for, or null for all projects.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void addExtensionMenu(Project onThisProject)
    {
        // Get all menus that can be possibly be generated now.
        List<MenuItem> menuItems = extMgr.getMenuItems(menuGenerator, onThisProject);

        // Take copy to iterate because we're doing removal:
        final ObservableList<MenuItem> items = getItems();
        ArrayList<MenuItem> oldPopupItems = new ArrayList<>(items);
        for (MenuItem aComponent : oldPopupItems) {
            // Only remove if it was an extension item:
            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getProperties().get(
                    "bluej.extmgr.ExtensionWrapper");

            if (aWrapper == null) {
                continue;
            }

            items.remove(aComponent);
        }

        items.remove(menuSeparator);

        // If the provided menu is empty we are done here.
        if (!menuItems.isEmpty())
        {
            items.add(menuSeparator);
            if (popupMenu!=null)
            {
                popupMenu.getItems().addAll(menuItems);
            }
            else
            {
                menu.getItems().addAll(menuItems);
            }
        }
    }

    @OnThread(Tag.Any)
    private FXPlatformRunnable supplierToRunnable(FXPlatformSupplier<Menu> menuFXSupplier)
    {
        return () -> {menuFXSupplier.get();};
    }

    private ObservableList<MenuItem> getItems()
    {
        if (popupMenu != null)
            return popupMenu.getItems();
        else
            return menu.getItems();
    }

    /**
     * Notify to all valid extensions that this menu Item is about to be displayed.
     *
     * @param  event  The associated event
     */
    private void menuWillBecomeVisible(Event event)
    {
        int itemsCount = 0;

        // Take copy to iterate because we're doing removal:
        final ObservableList<MenuItem> items = getItems();
        ArrayList<MenuItem> popupMenuItems = new ArrayList<>(items);
        for (MenuItem aComponent : popupMenuItems) {

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getProperties().get(
                    "bluej.extmgr.ExtensionWrapper");

            if (aWrapper == null) {
                continue;
            }

            if (!aWrapper.isValid())
            {
                items.remove(aComponent);
            }
            else
            {
                synchronized (this)
                {
                    aWrapper.safePostMenuItem(menuGenerator, aComponent);
                }
            }
            itemsCount++;
        }

        if (itemsCount <= 0) {
            items.remove(menuSeparator);
        }
    }
}
