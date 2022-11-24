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
import java.util.Arrays;
import java.util.Iterator;

/**
 * An array (well, collection) of ScratchObjects.
 * 
 * This needs its own class, rather than using ScratchPrimitive, because resolve
 * calls must recurse into the array in case there are object-references in the array
 * (in fact, this is almost always the case).
 * 
 * It is possible for entries in the array to be null, although the array
 * itself should not be null.
 * 
 * @author neil
 */
class ScratchObjectArray extends ScratchObject implements Iterable<ScratchObject>
{
    private ScratchObject[] value;

    public ScratchObjectArray(ScratchObject[] value)
    {
        this.value = value;
    }
    
    @Override public ScratchObject[] getValue()
    {
        return value;
    }

    @Override
    public ScratchObject resolve(ArrayList<ScratchObject> objects)
    {
        for (int i = 0; i < value.length; i++) {
            if (value[i] != null) {
                value[i] = value[i].resolve(objects);
            }
        }
        return this;
    }

    @Override
    public Iterator<ScratchObject> iterator()
    {
        return Arrays.asList(value).iterator();
    }
}