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
package bluej.editor.stride;

import javafx.scene.Node;

import bluej.stride.generic.FrameCursor;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXRunnable;

/**
 * An interface implemented by ClassFrame/InterfaceFrame, providing methods
 * needed by FrameEditorTab to control the bird's eye view.
 */
public interface BirdseyeManager
{
    /**
     * Gets the graphical Node corresponding to the frame around which the bird's eye view
     * selection rectangle should be drawn
     */
    Node getNodeForRectangle();

    /**
     * Gets the node which should be in view.  This is typically the header row of
     * the frame which the selection rectangle is drawn around.
     */
    Node getNodeForVisibility();

    /**
     * Notify about a click at the given scene X/Y.
     *
     * The return value will be non-null if there was a frame, 
     * or null if click wasn't on a frame.  Either way, bird's eye view will have been closed.
     */
    FrameCursor getClickedTarget(double sceneX, double sceneY);

    /**
     * Get the frame cursor to focus after we expand the frame which is currently selected.
     */
    FrameCursor getCursorForCurrent();

    /**
     * Move selection up (in response to up arrow key)
     */
    void up();

    /**
     * Move selection down (in response to down arrow key)
     */
    void down();

    /**
     * Test if clicking at the given position would result in selecting a frame
     */
    boolean canClick(double sceneX, double sceneY);
}
