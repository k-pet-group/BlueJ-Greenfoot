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

import greenfoot.core.GClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public abstract class ScriptableScratchMorph extends Morph
{

    public ScriptableScratchMorph(int id, int version,
            List<ScratchObject> scratchObjects)
    {
        super(id, version, scratchObjects);
    }
    
    // Fields:
    //  objName (String), vars (?), blocksBin (array), isClone (boolean), media (array), costume (SObject, 162)
    
    @Override public int fields()
    {
        return super.fields() + 6;
        
    }
    
    // Make this private to prevent accidental use by sub-classes
    // (most of the time we are using objName as a proxy for class name,
    //  but objName can contain spaces so isn't always a valid Java identifier)
    private String getObjName()
    {
        return (String)scratchObjects.get(super.fields() + 0).getValue();
    }
    
    public String getObjNameJava()
    {
        return getObjName().replace(' ', '_');
    }
    
    public ScratchObjectArray getBlocks()
    {
        return (ScratchObjectArray)scratchObjects.get(super.fields() + 2);
    }

    public ImageMedia getCostume()
    {
        return (ImageMedia)scratchObjects.get(super.fields() + 5);
    }
    
    public ImageMedia[] getCostumes()
    {
        ArrayList<ImageMedia> imgs = new ArrayList<ImageMedia>();
        ScratchObjectArray objArray = (ScratchObjectArray)scratchObjects.get(super.fields() + 4);
        for (ScratchObject o : objArray.getValue()) {
            if (o instanceof ImageMedia) {
                imgs.add((ImageMedia)o);
            }
        }
        return imgs.toArray(new ImageMedia[0]);
    }
    
    protected abstract String greenfootSuperClass();
    protected abstract void constructorContents(StringBuilder acc);

    @Override
    public File saveInto(File destDir, Properties props) throws IOException
    {
     // blocksBin is at the same index for all scriptable things:
        ScratchObject imageMedia = getCostume();            
        String className = getObjNameJava();
        
        StringBuilder acc = new StringBuilder();
        acc.append("import greenfoot.*;\npublic class " + className);
        
        String superClass = greenfootSuperClass();
        if (superClass != null) {
            acc.append(" extends ").append(superClass);
        }
        
        acc.append("\n{\n");
        acc.append("private static final String[] COSTUMES = new String[] {");
        int i = 0;
        int curCostume = 0;
        for (ImageMedia img : getCostumes()) {
            if (i != 0) acc.append(", ");
            acc.append("\"").append(img.saveInto(destDir, props)).append("\"");
            if (img == imageMedia) {
                curCostume = i;
            }
            i += 1;
        }
        acc.append("};\n");
        acc.append("private int curCostume = ").append(curCostume).append(";\n");
        
        acc.append("public " + className + "()\n{\n");
        constructorContents(acc);    
        acc.append("}\n");
        
        addHelpers(acc);

        codeForScripts(getBlocks(), acc);
        acc.append("}\n");
        
        File javaFile = new File(destDir, className + ".java");
        FileWriter javaFileWriter = new FileWriter(javaFile);
        javaFileWriter.write(acc.toString());
        javaFileWriter.close();
        
        
        File imageFile = imageMedia.saveInto(destDir, props);
        props.setProperty("class." + className + ".image", imageFile.getName());
        
        return javaFile;
    }

    protected void addHelpers(StringBuilder acc)
    {
    }

    private void codeForBlock(ScratchObject block, StringBuilder decl, StringBuilder method)
    {
        // Each block is an array of entries.  For example, if you have
        // a repeat block, that will be an array of three things:
        // [doRepeat, <the number of counts>, <the block to repeat>]
        // The block to repeat will in turn be an array and so on!
        ScratchObject[] blockContents = (ScratchObject[])block.getValue();
        
        if ("doRepeat".equals(blockContents[0].getValue())) {
            method.append("int loopCount = ")
                  .append(blockContents[1].getValue())
                  .append(";\n");
            method.append("for (int i = 0; i < loopCount;i++)\n{\n");
            codeForBlock(blockContents[2], decl, method);
            method.append("}\n");
        }
        else if ("doForever".equals(blockContents[0].getValue())) {
            // We take do forever to mean "do once per act() invocation:
            codeForBlock(blockContents[1], decl, method);
        }
        else if ("doPlaySoundAndWait".equals(blockContents[0].getValue())) {
            decl.append("GreenfootSound snd;");
            method.append("if (snd == null || !snd.isPlaying()) {\n")
            	  .append(" snd = new GreenfootSound(\"")
                  .append(blockContents[1].getValue())
                  .append(".wav\");\nsnd.play();\n}\n");
            // TODO need a state machine to wait in the state until done playing
        }
        else if ("forward:".equals(blockContents[0].getValue())) {
            method.append("move(").append(blockContents[1].getValue()).append(");\n");
        }
        else if ("bounceOffEdge".equals(blockContents[0].getValue())) {
            method.append("if (atWorldEdge()) turn(180);\n");
        }
        else if ("randomFrom:to:".equals(blockContents[0].getValue())) {
            int from = (Integer)blockContents[1].getValue();
            int to = (Integer)blockContents[2].getValue();
            if (from == 0) {
                method.append("Greenfoot.randomNumber(").append(to).append(")");
            } else {
                method.append("(Greenfoot.randomNumber(").append(to - from).append(") + ").append(from).append(")");
            }
        }
        else if ("turnRight:".equals(blockContents[0].getValue())) {
            String degrees = blockContents[1].getValue().toString();
            method.append("turn(-").append(degrees).append(");\n");
        }
        else if ("nextCostume".equals(blockContents[0].getValue())) {
            method.append("curCostume = (curCostume + 1) % COSTUMES.length;\n");
            method.append("setImage(COSTUMES[curCostume]);\n");
            method.append("getImage().scale(")
            .append(getBounds().x2.subtract(getBounds().x))
            .append(", ")
            .append(getBounds().y2.subtract(getBounds().y))
            .append(");\n");
        }
        else if (blockContents[0] instanceof ScratchObjectArray) {
            for (ScratchObject blockContent : blockContents) {
                codeForBlock(blockContent, decl, method);
            }
        }
        else {
            StringBuilder tmp = new StringBuilder();
            for (ScratchObject o : blockContents) {
                tmp.append(o).append(",");
            }
            ScratchImport.print("Unknown code: " + tmp.toString());
        }
    }

    private void codeForScripts(ScratchObjectArray scripts, StringBuilder acc)
    {
        StringBuilder decl = new StringBuilder();
        StringBuilder method = new StringBuilder();
        method.append("public void act()\n{\n");
        // Each item in the array of blocks is a separate script chunk in
        // the scripts window in Scratch:
        for (ScratchObject scriptChunk : scripts.getValue()) {
            ScratchObject[] info = (ScratchObject[])scriptChunk.getValue();
            //First entry is always a ScratchPrimitive with a Point
            //  (location in the Scratch script window; we can ignore)
            //Second entry is the actual block array.
            
            ScratchObject[] blocks = (ScratchObject[])info[1].getValue();
            
            // Each item in this array is a separate block, 
            // taken together they are a chunk.
            
            // We are only interested (for now, at least) in blocks that
            // can run automatically, either from the outset or when clicked.
    
            ScratchObject[] firstBlockContents = (ScratchObject[])blocks[0].getValue();
            
            if ("MouseClickEventHatMorph".equals(firstBlockContents[0].getValue())) {
                if ("Scratch-MouseClickEvent".equals(firstBlockContents[1].getValue())) {
                    method.append("if (Greenfoot.mouseClicked(this)) {");
                    //TODO actually this should set a boolean indicating we've started and should run in future
                    for (ScratchObject block : Arrays.copyOfRange(blocks, 1, blocks.length)) {
                        codeForBlock(block, decl, method);
                    }
                    
                    method.append("}");
                }
            }
            else if ("EventHatMorph".equals(firstBlockContents[0].getValue())) {
                if ("Scratch-StartClicked".equals(firstBlockContents[1].getValue())) {
                    // Don't need any special code for when flag clicked:
                    
                    for (ScratchObject block : Arrays.copyOfRange(blocks, 1, blocks.length)) {
                        codeForBlock(block, decl, method);
                    }
                }
            }
            // else ignore for now
            
        }
        method.append("}\n");
        acc.append(decl).append(method);
    }
    
    
}
