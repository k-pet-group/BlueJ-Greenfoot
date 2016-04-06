/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.generic;

import java.util.Optional;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;

import bluej.stride.slots.HeaderItem;
import bluej.utility.javafx.SharedTransition;

/**
 * An interface for an item within a Frame; either a FrameContentRow or FrameContentCanvas
 */
public interface FrameContentItem
{
    /**
     * Gets all header items, all the way down in the children
     */
    public Stream<HeaderItem> getHeaderItemsDeep();
    /**
     * Gets header items that are directly in this item
     */
    public Stream<HeaderItem> getHeaderItemsDirect();

    /**
     * Gets bounds in terms of the scene
     */
    public Bounds getSceneBounds();

    /**
     * Gets the canvas within this item, if any
     */
    public Optional<FrameCanvas> getCanvas();

    public boolean focusLeftEndFromPrev();
    public boolean focusRightEndFromNext();
    public boolean focusTopEndFromPrev();
    public boolean focusBottomEndFromNext();

    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animation);

    public Node getNode();
}
