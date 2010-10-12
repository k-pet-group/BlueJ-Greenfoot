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

class ScratchUserObject extends ScratchObject
{
    private int id;
    private int version;
    private List<ScratchObject> scratchObjects;
    public ScratchUserObject(int id, int version, List<ScratchObject> scratchObjects)
    {
        this.id = id;
        this.version = version;
        this.scratchObjects = scratchObjects;
    }
    
    public ScratchObject resolve(ArrayList<ScratchObject> objects)
    {
        for (int i = 0; i < scratchObjects.size(); i++) {
            ScratchObject scratchObject = scratchObjects.get(i);
            if (scratchObject != null) {
                scratchObjects.set(i, scratchObject.resolve(objects));
            }
        }
        return this;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SObject [id=");
        builder.append(id);
        builder.append(", members=[");
        for (ScratchObject m : scratchObjects) {
            if (m != null) {
                builder.append(m.toString()).append(",");
            } else {
                builder.append("null,");
            }
            
        }
        builder.append("], version=");
        builder.append(version);
        builder.append("]");
        /*if (id == 125) {
            builder.append("\n");
            Member[] blocks = (Member[])members.get(morphFields() + 2).getValue();
            for (Member m : blocks) {
                builder.append(m).append(",");
            }
        }
        */
        return builder.toString();
    }
    
    public static int morphFields()
    {
        return 6; //bounds (Rectangle), owner (?), submorphs (array), color (Color), flags (int), placeholder (null)
    }
    
    public static int scriptableScratchMorphFields()
    {
        return morphFields() + 6; //objName (String), vars (?), blocksBin (array), isClone (boolean), media (array), costume (SObject, 162)
    }
    
    public static int scratchStageMorphFields()
    {
        return scriptableScratchMorphFields() + 9;
          // zoom (int), hPan (int), vPan (int), obsoleteSavedState (?), sprites (array), volume (int), tempoBPM (int), sceneStates (?), lists(?) 
    }
    
    public static int mediaFields()
    {
        return 1; //mediaName
    }
           
    public ScratchUserObject getStage()
    {
        return id == 125 ? this : null;
    }
    
    public ScratchUserObject getSprite()
    {
        return id == 124 ? this : null;
    }
    
    public ScratchObjectArray getBlocks()
    {
        if (getStage() != null || getSprite() != null) {
            return (ScratchObjectArray)scratchObjects.get(morphFields() + 2);
        } else {
            return null;
        }
    }        
}