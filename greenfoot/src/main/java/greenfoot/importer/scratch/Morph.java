/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.importer.scratch;

import java.util.List;

/**
 * Mirrors the Scratch Morph class, the base class for a lot of things in Scratch.
 * 
 * Mainly relevant because it holds the bounds (particularly useful for sprites).
 * 
 * @author neil
 *
 */
public class Morph extends ScratchUserObject
{
    public Morph(int id, int version, List<ScratchObject> scratchObjects)
    {
        super(id, version, scratchObjects);
    }

    // Fields:
    //  bounds (Rectangle), owner (?), submorphs (array), color (Color), flags (int), placeholder (null)
    
    public int fields()
    {
        return 6; 
    }
    
    public ScratchRectangle getBounds()
    {
        return (ScratchRectangle)scratchObjects.get(0);
    }
}
