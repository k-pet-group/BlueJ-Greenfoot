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
import java.util.List;

/**
 * A Scratch user-defined object (as opposed to an array or primitive type).
 * @author neil
 *
 */
class ScratchUserObject extends ScratchObject
{
    // See the table in Scratch-Object IO.ObjStream.<class>.userClasses
    protected static final int SCRATCH_SPRITE_MORPH = 124;
    protected static final int SCRATCH_STAGE_MORPH = 125;
    protected static final int IMAGE_MEDIA = 162;
    protected static final int SOUND_MEDIA = 164;
    
    private int id;
    private int version;
    protected List<ScratchObject> scratchObjects;
    public ScratchUserObject(int id, int version, List<ScratchObject> scratchObjects)
    {
        this.id = id;
        this.version = version;
        this.scratchObjects = scratchObjects;
    }
    
    @Override public ScratchObject resolve(ArrayList<ScratchObject> objects)
    {
        for (int i = 0; i < scratchObjects.size(); i++) {
            ScratchObject scratchObject = scratchObjects.get(i);
            if (scratchObject != null) {
                scratchObjects.set(i, scratchObject.resolve(objects));
            }
        }
        return this;
    }

    /**
     * The number of fields that this class loads from the file in total.
     * 
     * This should usually be implemented as super.fields() + N.
     */
    public int fields()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ScratchUserObject [id=");
        builder.append(id);
        builder.append("]\n  {");
        for (ScratchObject o : scratchObjects) {
            builder.append(o).append(",");
        }
        builder.append("}");
        return builder.toString();
    }
    
    
}