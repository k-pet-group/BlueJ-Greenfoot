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


/**
 * A factory class for creating Frame objects of type T.
 */
public interface FrameFactory<T extends Frame>
{
    /**
     * Creates a frame (main factory method)
     */
    T createBlock(InteractionManager editor);
    
    /**
     * Creates a frame with the given contents.
     * By default, throws an exception (override if you want to support this)
     */
    default T createBlock(InteractionManager editor, List<Frame> contents)
    {
        throw new IllegalArgumentException("Cannot create " + getBlockClass() + " with frame contents");
    }
    
    /**
     * Gets the class for type T (work-around for lack of reification)
     */
    Class<T> getBlockClass();
}
