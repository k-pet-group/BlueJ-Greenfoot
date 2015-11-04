/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import java.util.List;

public interface CursorFinder
{

    /** 
     * Finds a cursor for this Y coordinate (in scene coordinates).  Essentially there are three cases:
     * 
     * 1. The point is before this block.  Because we scan down the blocks in order
     *    using this method, it should return the given prevCursor.
     *    
     * 2. The point is within this block.  This method should return the nearest
     *    cursor, e.g. the cursor before if the point is in the top half of the block
     *    and the cursor after if it is in the bottom half, or a similar appropriate heuristic.
     *    
     * 3. The point is after this block.  This method should still return the last cursor.
     *
     * @param sceneY The Y in scene coordinates at which to find the nearest cursor
     * @param prevCursor The cursor before this frame, or null
     * @param nextCursor The cursor after this frame, or null
     * @param exclude Cursors in this frame that are not a valid answer (e.g. because they are being dragged and we are looking for a drag target).
     * @param canDescend If true, can go into subframes to find a closest cursor.  If false, only look
     *                   at cursors at the top level.
     */
    public FrameCursor findCursor(double sceneX, double sceneY, FrameCursor prevCursor, FrameCursor nextCursor, List<Frame> exclude, boolean isDrag, boolean canDescend);

}