/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2016  Michael Kolling and John Rosenberg 
 
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

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.WindowEvent;

import bluej.pkgmgr.Project;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Manages the interface between a menu and extensions.
 * An instance of this class is attached to each popup menu that extensions
 * may add items to.
 *
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004,2005
 */
@OnThread(Tag.FXPlatform)
public final class FXMenuManager
{
    private final ExtensionsManager extMgr;
    private final SeparatorMenuItem menuSeparator;
    @OnThread(Tag.Any)
    private final ContextMenu popupMenu;
    private final ExtensionMenu menuGenerator;

    /**
     * Constructor for the MenuManager object.
     *
     * @param  aPopupMenu  The menu that extensions are attaching to.
     * @param menuGenerator
     */
    public FXMenuManager(ContextMenu aPopupMenu, ExtensionsManager extMgr, ExtensionMenu menuGenerator)
    {
        this.extMgr = extMgr;
        this.popupMenu = aPopupMenu;
        this.menuGenerator = menuGenerator;
        popupMenu.setOnShowing(this::popupMenuWillBecomeVisible);
        menuSeparator = new SeparatorMenuItem();
    }

    /**
     * Add all the menu currently available to the menu.
     * This may be called any time BlueJ feels that the menu needs to be updated.
     *
     * @param  onThisProject  a specific project to look for, or null for all projects.
     */
    @OnThread(Tag.Swing)
    public void addExtensionMenu(Project onThisProject)
    {
        // Get all menus that can be possibly be generated now.
        List<JMenuItem> menuItems = extMgr.getMenuItems(menuGenerator, onThisProject);

        FXRunnable addItems = JavaFXUtil.swingMenuItemsToContextMenu(popupMenu, menuItems, null, (swingItem, fxItem) -> {
            fxItem.getProperties().put("bluej.extmgr.ExtensionWrapper", swingItem.getClientProperty("bluej.extmgr.ExtensionWrapper"));
            fxItem.getProperties().put("bluej.extmgr.JMenuItem", swingItem);
        });
        
        Platform.runLater(() -> {
            // Take copy to iterate because we're doing removal:
            ArrayList<MenuItem> oldPopupItems = new ArrayList<>(popupMenu.getItems());
            for (MenuItem aComponent : oldPopupItems) {
                // Only remove if it was an extension item:
                ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getProperties().get(
                        "bluej.extmgr.ExtensionWrapper");
                
                if (aWrapper == null) {
                    continue;
                }
    
                popupMenu.getItems().remove(aComponent);
            }
    
            popupMenu.getItems().remove(menuSeparator);
    
            // If the provided menu is empty we are done here.
            if (!menuItems.isEmpty())
            {
                popupMenu.getItems().add(menuSeparator);
                addItems.run();
            }
        });
        
    }

    /**
     * Notify to all valid extensions that this menu Item is about to be displayed.
     *
     * @param  event  The associated event
     */
    public void popupMenuWillBecomeVisible(WindowEvent event)
    {
        int itemsCount = 0;

        // Take copy to iterate because we're doing removal:
        ArrayList<MenuItem> popupMenuItems = new ArrayList<>(popupMenu.getItems());
        for (MenuItem aComponent : popupMenuItems) {

            ExtensionWrapper aWrapper = (ExtensionWrapper) aComponent.getProperties().get(
                    "bluej.extmgr.ExtensionWrapper");

            if (aWrapper == null) {
                continue;
            }

            JMenuItem swingItem = (JMenuItem)aComponent.getProperties().get("bluej.extmgr.JMenuItem");

            SwingUtilities.invokeLater(() -> {
                if (!aWrapper.isValid())
                {
                    Platform.runLater(() -> {popupMenu.getItems().remove(aComponent);});
                }
                else
                {
                    aWrapper.safePostMenuItem(menuGenerator, swingItem);
                }
            });
            itemsCount++;
        }

        if (itemsCount <= 0) {
            popupMenu.getItems().remove(menuSeparator);
        }
    }
}
