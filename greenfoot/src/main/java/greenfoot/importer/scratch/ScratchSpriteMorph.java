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

import java.math.BigDecimal;
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
           .append(getBounds().getWidth().intValue())
           .append(", ")
           .append(getBounds().getHeight().intValue())
           .append(");\n");
    }

    @Override
    protected String greenfootSuperClass()
    {
        return "Actor";
    }
    
    @Override
    public ScratchPoint getScaleAmount()
    {
        return (ScratchPoint) scratchObjects.get(super.fields() + 1);
    }

    public ScratchPoint getGreenfootCentre()
    {
        return new ScratchPoint(getBounds().getMiddle().x.subtract(
                                    getScaleAmount().x.subtract(new BigDecimal(1)).multiply(
                                      getCostume().getRotationCentre().x
                                    )
                                )
                               ,getBounds().getMiddle().y.subtract(
                                    getScaleAmount().y.subtract(new BigDecimal(1)).multiply(
                                      getCostume().getRotationCentre().y
                                    )
                               ) );
                
    }
    
    @Override
    protected void addHelpers(StringBuilder acc)
    {
        acc.append("private void turn(int angle)\n{\nsetRotation(getRotation() + angle);\n}\n");
        acc.append("private void move(int speed)\n{\ndouble angle = Math.toRadians( getRotation() );\nint x = (int) Math.round(getX() + Math.cos(angle) * speed);\nint y = (int) Math.round(getY() + Math.sin(angle) * speed);\nsetLocation(x, y);\n}\n");
        acc.append("private boolean atWorldEdge()\n{\nif(getX() < 20 || getX() > getWorld().getWidth() - 20) return true;\nif(getY() < 20 || getY() > getWorld().getHeight() - 20) return true;\nelse return false;\n}\n");
        //acc.append("private void setTopLeft(int x, int y)\n{setLocation(x + (getImage().getWidth()/2),y + (getImage().getHeight()/2));\n}\n");
    }

}
