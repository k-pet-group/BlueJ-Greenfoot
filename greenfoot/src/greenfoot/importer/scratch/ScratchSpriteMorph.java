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
 * Mirrors the Scratch ScratchSpriteMorph class, which is the equivalent of an actor.
 * @author neil
 *
 */
public class ScratchSpriteMorph extends ScriptableScratchMorph
{
    public ScratchSpriteMorph(int version, List<ScratchObject> scratchObjects)
    {
        super(ScratchUserObject.SCRATCH_SPRITE_MORPH, version, scratchObjects);
    }
    
    // Fields:
    //  visibility, scalePoint, rotationDegrees, rotationStyle, volume, tempoBPM, draggable, sceneStates, lists
    
    @Override public int fields()
    {
        return 9;
    }

    @Override
    protected void constructorContents(StringBuilder acc)
    {
        acc.append("GreenfootImage img = getImage();\n");
        acc.append("img.scale(")
           .append(getBounds().width)
           .append(", ")
           .append(getBounds().height)
           .append(");\n");
    }

    @Override
    protected String greenfootSuperClass()
    {
        return "Actor";
    }

}
