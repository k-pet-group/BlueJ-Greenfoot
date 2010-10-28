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

import java.util.LinkedList;
import java.util.List;

/**
 * Mirrors the Scratch ScratchStageMorph class, which is the equivalent of World.
 * @author neil
 *
 */
public class ScratchStageMorph extends ScriptableScratchMorph
{
    public ScratchStageMorph(int version, List<ScratchObject> scratchObjects)
    {
        super(ScratchUserObject.SCRATCH_STAGE_MORPH, version, scratchObjects);
    }
    
    // Fields:
    //  zoom (int), hPan (int), vPan (int), obsoleteSavedState (?), sprites (array), volume (int), tempoBPM (int), sceneStates (?), lists(?)
    
    @Override public int fields()
    {
        return super.fields() + 9;
    }
    
    ScratchObjectArray getSprites()
    {
        return (ScratchObjectArray)scratchObjects.get(super.fields() + 4);
    }

    @Override
    protected void constructorContents(StringBuilder acc)
    {
        ImageMedia image = getCostume();
        //TODO should this actually be our bounds rather than the world's image -- can we be stretched?
        acc.append("super(").append(image.getWidth()).append(", ").append(image.getHeight()).append(", 1);\n");
        
        LinkedList<String> classes = new LinkedList<String>();
        
        ScratchObjectArray sprites = getSprites();
        for (ScratchObject o : sprites.getValue()) {
            ScratchSpriteMorph sprite = (ScratchSpriteMorph)o;
            String spriteName = sprite.getObjNameJava();
            acc.append("addObject(new ").append(spriteName).append("(), ");
            acc.append(sprite.getGreenfootCentre().x.intValue());
            acc.append(", ");
            acc.append(sprite.getGreenfootCentre().y.intValue());
            acc.append(");\n");
            
            // Add at beginning so that later classes will get drawn first:
            classes.addFirst(spriteName); 
        }
        
        if (!classes.isEmpty()) {
            acc.append("setPaintOrder(Bubble.class");
            for (String cls : classes) {
                acc.append(", ").append(cls).append(".class");
            }
            acc.append(");\n");
        }
    }

    @Override
    protected String greenfootSuperClass()
    {
        return "World";
    }
}

