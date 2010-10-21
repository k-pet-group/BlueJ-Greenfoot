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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * A Scratch object.  This can be a user-object (which includes sprites, stages, images, etc)
 * or it can be a primitive (byte array, colour, etc); this is taken care of by various
 * sub-classes.
 * @author neil
 */
public class ScratchObject
{
    /**
     * Resolves all ScratchObjectReferences inside this object.
     * 
     * Because Scratch objects stored in the file may have circular references,
     * they are stored in a long table, and any pointers to objects in the table
     * are stored as ScratchObjectReference.  Once everything is loaded,
     * we invoke this method everywhere, passing the object table,
     * so that the references can get resolved.
     * 
     * @param objects
     * @return The resolved object.  Returns "this" by default,
     *   but ScratchObjectReferences will return a different object.
     */
    public ScratchObject resolve(ArrayList<ScratchObject> objects)
    {
        return this;
    }
    
    /**
     * Gets the value for this object.  You will mainly want to call this
     * and cast the result when you have an expectation of the inner class.
     * For example, you may expect a colour, so you call getValue() and cast
     * to java.awt.Color.
     * @return The value represented by this object; by default returns "this",
     *   but ScratchPrimitive will return the primitive value inside.
     */
    public Object getValue()
    {
        return this;
    }

    /**
     * Saves the item (e.g. image, sound, class) in the given project
     */
    public File saveInto(File destDir, Properties props, String prefix) throws IOException
    {
        return null;
    }
}