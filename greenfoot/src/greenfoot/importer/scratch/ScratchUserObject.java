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
import greenfoot.core.GProject;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

class ScratchUserObject extends ScratchObject
{
    // See the table in Scratch-Object IO.ObjStream.<class>.userClasses
    private static final int SCRATCH_SPRITE_MORPH = 124;
    private static final int SCRATCH_STAGE_MORPH = 125;
    private static final int IMAGE_MEDIA = 162;
    
    private int id;
    private int version;
    private List<ScratchObject> scratchObjects;
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
    
    public static int mediaFields()
    {
        return 1; //mediaName
    }

    private boolean isScriptable()
    {
        return id == SCRATCH_STAGE_MORPH || id == SCRATCH_SPRITE_MORPH;
    }
    
    @Override public String saveInto(GProject project) throws IOException
    {
        if (isScriptable()) {
            // blocksBin is at the same index for all scriptable things:
            ScratchObjectArray scripts = (ScratchObjectArray)scratchObjects.get(morphFields() + 2);
            ScratchObject imageMedia = scratchObjects.get(morphFields() + 5);            
            String className = scratchObjects.get(morphFields()).toString();
            StringBuilder acc = new StringBuilder();
            acc.append("import greenfoot.*;\npublic class " + className);
            
            if (id == SCRATCH_STAGE_MORPH) {
                acc.append(" extends World");
            } else if (id == SCRATCH_SPRITE_MORPH) {
                acc.append(" extends Actor");
            }
            
            acc.append("\n{\n");
            if (id == SCRATCH_STAGE_MORPH) {
                ScratchImage image = (ScratchImage)((ScratchUserObject)imageMedia).scratchObjects.get(mediaFields() + 0);
                acc.append("public " + className + "()\n{\n");
                acc.append("super(").append(image.getWidth()).append(", ").append(image.getHeight()).append(", 1);\n");
                
                ScratchObjectArray sprites = (ScratchObjectArray)scratchObjects.get(scriptableScratchMorphFields() + 4);
                for (ScratchObject o : sprites.getValue()) {
                    ScratchUserObject sprite = (ScratchUserObject)o;
                    String spriteName = sprite.scratchObjects.get(morphFields() + 0).toString();
                    acc.append("addObject(new ").append(spriteName).append("(), ");
                    acc.append((int)((Rectangle)sprite.scratchObjects.get(0).getValue()).getCenterX());
                    acc.append(", ");
                    acc.append((int)((Rectangle)sprite.scratchObjects.get(0).getValue()).getCenterY());
                    acc.append(");\n");
                }
                
                acc.append("}\n");
            } else if (id == SCRATCH_SPRITE_MORPH) {
                acc.append("public " + className + "()\n{\n");
                acc.append("GreenfootImage img = getImage();\n");
                acc.append("img.scale(")
                   .append(((Rectangle)scratchObjects.get(0).getValue()).width)
                   .append(", ")
                   .append(((Rectangle)scratchObjects.get(0).getValue()).height)
                   .append(");\n");
                acc.append("}\n");
            } 
            codeForScripts(scripts, acc);
            acc.append("}\n");
            
            File javaFile = new File(project.getDefaultPackage().getDir(), className + ".java");
            FileWriter javaFileWriter = new FileWriter(javaFile);
            javaFileWriter.write(acc.toString());
            javaFileWriter.close();
            
            GClass gcls = project.getDefaultPackage().newClass(className);
            
            javaFileWriter = new FileWriter(javaFile);
            javaFileWriter.write(acc.toString());
            javaFileWriter.close();
            
            
            String imageFile = imageMedia.saveInto(project);
            gcls.setClassProperty("image", imageFile);
            
            return javaFile.getName();
        } else if (id == IMAGE_MEDIA) {
            // Save the image that we are wrapping:
            String imageFileName = scratchObjects.get(mediaFields() + 0).saveInto(project);
            
            Debug.message("ImageMedia wrapping: " + imageFileName);
            
            return imageFileName;
            
            //TODO use mediaName and do the saving here rather than in SImage
        }
        return null;
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

    private static void codeForScripts(ScratchObjectArray scripts, StringBuilder acc)
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