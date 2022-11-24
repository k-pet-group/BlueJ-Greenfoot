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

import java.util.ArrayList;

/**
 * Represents a reference to a real ScratchObject
 * 
 * This class is used when first loading from the store file, but after resolve
 * is called, a real (non-reference) ScratchObject will be returned, and thus
 * all instances of ScratchObjectReference will disappear from view.
 * 
 * @author neil
 *
 */
class ScratchObjectReference extends ScratchObject
{
    // The one-based index into the object table
    private int index;
    
    public ScratchObjectReference(int i)
    {
        index = i;
    }

    public ScratchObject resolve(ArrayList<ScratchObject> objects)
    {
        // Convert one-based index to zero-based index into the array list:
        return objects.get(index - 1);
    }
    
    public String toString()
    {
        return "{#" + index + "}";
    }
}