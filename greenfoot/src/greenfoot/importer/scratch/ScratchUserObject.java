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

    private static void codeForBlock(ScratchObject block, StringBuilder acc)
    {
        // Each block is an array of entries.  For example, if you have
        // a repeat block, that will be an array of three things:
        // [doRepeat, <the number of counts>, <the block to repeat>]
        // The block to repeat will in turn be an array and so on!
        ScratchObject[] blockContents = (ScratchObject[])block.getValue();
        
        if ("doRepeat".equals(blockContents[0].getValue())) {
            acc.append("int loopCount = ");
            codeForBlock(blockContents[1], acc);
            acc.append(";\n");
            acc.append("for (int i = 0; i < loopCount;i++)\n{\n");
            codeForBlock(blockContents[2], acc);
            acc.append("}\n");
        }
        else if ("MouseClickEventHatMorph".equals(blockContents[0].getValue())) {
            if ("Scratch-MouseClickEvent".equals(blockContents[1].getValue())) {
                acc.append("if (Greenfoot.mouseClicked(this))");
                //TODO put curly brackets in properly
            }
        }
        else if ("EventHatMorph".equals(blockContents[0].getValue())) {
            if ("Scratch-StartClicked".equals(blockContents[1].getValue())) {
                // Don't need special code for when flag clicked
            }
        }
        else if ("randomFrom:to:".equals(blockContents[0].getValue())) {
            int from = (Integer)blockContents[1].getValue();
            int to = (Integer)blockContents[2].getValue();
            if (from == 0) {
                acc.append("Greenfoot.randomNumber(").append(to).append(")");
            } else {
                acc.append("(Greenfoot.randomNumber(").append(to - from).append(") + ").append(from).append(")");
            }
        }
        else if ("turnRight:".equals(blockContents[0].getValue())) {
            String degrees = blockContents[1].getValue().toString();
            acc.append("setRotation(getRotation() - ").append(degrees).append(");\n");
        }
        else if (blockContents[0] instanceof ScratchObjectArray) {
            codeForBlock(blockContents[0], acc);
        }
        else {
            StringBuilder tmp = new StringBuilder();
            for (ScratchObject o : blockContents) {
                tmp.append(o).append(",");
            }
            ScratchImport.print("Unknown code: " + tmp.toString());
        }
    }

    protected static void codeForScripts(ScratchObjectArray scripts, StringBuilder acc)
    {
        acc.append("public void act()\n{\n");
        // Each item in the array of blocks is a separate script chunk in
        // the scripts window in Scratch:
        for (ScratchObject scriptChunk : scripts.getValue()) {
            ScratchObject[] info = (ScratchObject[])scriptChunk.getValue();
            //First entry is always a ScratchPrimitive with a Point
            //  (location in the Scratch script window; we can ignore)
            //Second entry is the actual block array:
            
            for (ScratchObject block : (ScratchObject[])info[1].getValue()) {
                // Each item in this array is a separate block:
                ScratchUserObject.codeForBlock(block, acc);
            }
        }
        acc.append("}\n");
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ScratchUserObject [id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }
    
    
}