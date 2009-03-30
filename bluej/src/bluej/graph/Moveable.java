/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.graph;

/**
 * @author fisker
 *
  */
public interface Moveable
{
    /**
     * Return the current x coordinate.
     */
    public int getX();

    /**
     * Return the current y coordinate.
     */
    public int getY();
    
    /**
     * @return Returns the ghostX.
     */
    public int getGhostX();
    
    /**
     * @return Returns the ghostX.
     */
    public int getGhostY();
    
    /**
     * Set the position of the ghost image given a delta to the real size.
     */
    public void setGhostPosition(int deltaX, int deltaY);

    /**
     * Set the size of the ghost image.
     */
    public void setGhostSize(int ghostWidth, int ghostHeight);

    /**
     * Set the target's position to its ghost position.
     */
    public void setPositionToGhost();
    
    /** 
     * Ask whether we are currently dragging. 
     */
    public boolean isDragging();
    
    /**
     * Set whether or not we are currently dragging this class
     * (either moving or resizing).
     */
    public void setDragging(boolean isDragging);
    
    /**
     * Tell whether this element is indeed currently moveable.
     */
    public boolean isMoveable();
    
    /**
     * Specify whether this element is indeed currently moveable.
     */
    public void setIsMoveable(boolean isMoveable);

    /**
     * Tell whether this element is resizable.
     */
    public boolean isResizable();

}
