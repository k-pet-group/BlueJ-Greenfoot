/*
 This file is part of the BlueJ program. 
 Copyright (C) 2023  Michael Kolling and John Rosenberg

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
package bluej.prefmgr;

import javafx.scene.Node;

import java.util.List;

/**
 * An item that can be shown at the bottom of the miscellaneous preferences panel.
 * 
 * It inherits the begin/commit/revert events from PrefPanelListener
 */
public interface MiscPrefPanelItem extends PrefPanelListener
{
    /**
     * Gets the nodes to add to the VBox as the content of the extra item
     */
    public List<Node> getMiscPanelContents();

    /**
     * Gets the title (already localised) of the item, to put as the header for that section.
     */
    public String getMiscPanelTitle();
}
