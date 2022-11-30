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

import bluej.stride.framedjava.frames.StrideCategory;

/**
 * A utility predicate-like class for checking which frames are accepted in a given canvas.
 * 
 * We need two methods because there isn't a one-to-one correspondence between category
 * and frame.  Specifically, var frames can be inserted in three categories (var, local constant, class constant)
 * and only accepted by certain canvases, but after insertion, they can be moved around
 * freely between canvases.
 */
public interface FrameTypeCheck
{
    /**
     * Checks if the given category can be inserted as new.
     */
    public boolean canInsert(StrideCategory category);

    /**
     * Checks if the given frame class can be moved here.
     */
    public boolean canPlace(Class<? extends Frame> type);
}
