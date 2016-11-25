/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg

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
package bluej.stride.slots;

import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.generic.Frame;
import bluej.utility.javafx.SharedTransition;

import javafx.collections.ObservableList;
import javafx.scene.Node;

import java.util.Collections;
import java.util.List;

/**
 * A HeaderItem is anything which might appear in a FlowPane header in a Frame.  This is typically
 * a slot, or a label.
 *
 * Importantly, there is a one-to-many correspondence between HeaderItem and actual Node components to
 * display in the FlowPane (not one-to-one as you might expect).  This is because things like ExpressionSlot
 * are one HeaderItem, but are implemented using multiple nodes (which means that they can then wrap individually
 * in the FlowPane, rather than as one clump, for long expressions).
 *
 * Generally, HeaderItem is just used to get hold of the graphical nodes (via getComponents); anything more
 * complicated is usually related to slots, which can be accessed via the asEditable method.
 */
public interface HeaderItem
{
    public ObservableList<? extends Node> getComponents();
    
    // Returns null if not editable
    public EditableSlot asEditable();

    default public List<? extends PossibleLink> findLinks() {return Collections.emptyList();};

    /**
     * Notifies the slot to change to the current view (normal, Java preview, bird's eye)
     * @see bluej.stride.generic.Frame.View
     */
    void setView(Frame.View oldView, Frame.View newView, SharedTransition animate);

}
