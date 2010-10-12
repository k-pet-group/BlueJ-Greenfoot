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
    // See the table in Scratch-Object IO.ObjStream.<class>.userClasses
    private static final int SCRATCH_SPRITE_MORPH = 124;
    private static final int SCRATCH_STAGE_MORPH = 125;
    
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

    // Number of fields in the Morph class
    public static int morphFields()
    {
        return 6; //bounds (Rectangle), owner (?), submorphs (array), color (Color), flags (int), placeholder (null)
    }
    
    // Number of fields in the ScriptableScratchMorph class (including those from the Morph super-class)
    public static int scriptableScratchMorphFields()
    {
        return morphFields() + 6; //objName (String), vars (?), blocksBin (array), isClone (boolean), media (array), costume (SObject, 162)
    }
    
 // Number of fields in the ScratchStageMorph class (including those from the ScriptableScratchMorph super-class)
    public static int scratchStageMorphFields()
    {
        return scriptableScratchMorphFields() + 9;
          // zoom (int), hPan (int), vPan (int), obsoleteSavedState (?), sprites (array), volume (int), tempoBPM (int), sceneStates (?), lists(?) 
    }
           
    public ScratchUserObject getStage()
    {
        return id == SCRATCH_STAGE_MORPH ? this : null;
    }
    
    public ScratchUserObject getSprite()
    {
        return id == SCRATCH_SPRITE_MORPH ? this : null;
    }
    
    public ScratchObjectArray getBlocks()
    {
        if (getStage() != null || getSprite() != null) {
            // blocksBin is at the same index for both:
            return (ScratchObjectArray)scratchObjects.get(morphFields() + 2);
        } else {
            return null;
        }
    }        
}