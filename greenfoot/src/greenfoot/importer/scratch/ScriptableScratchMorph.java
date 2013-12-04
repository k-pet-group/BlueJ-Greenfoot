/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2013  Poul Henriksen and Michael Kolling 
 
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
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import bluej.utility.Debug;

public abstract class ScriptableScratchMorph extends Morph
{
    private String[] costumes;
    private String mungedName;
    private File javaFile;

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
        if (mungedName == null) {
            Debug.message("Munging: " + getObjName());
            mungedName = ScratchImport.mungeUnique(getObjName());
            Debug.message("Munged to: " + mungedName);
        }
        return mungedName;
    }
    
    public ScratchObjectArray getBlocks()
    {
        return (ScratchObjectArray)scratchObjects.get(super.fields() + 2);
    }
    
    public ScratchObjectArray getMedia()
    {
        return (ScratchObjectArray)scratchObjects.get(super.fields() + 4);
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
    
    public int getCostumeCount()
    {
        return ((ScratchObjectArray)scratchObjects.get(super.fields() + 4)).getValue().length;
    }
    
    protected abstract String greenfootSuperClass();
    protected abstract void constructorContents(StringBuilder acc);

    @Override
    public File saveInto(File destDir, Properties props, String prefix) throws IOException
    {
        if (javaFile != null) return javaFile;
        
        // blocksBin is at the same index for all scriptable things:
        ScratchObject imageMedia = getCostume();            
        String className = getObjNameJava();
        
        StringBuilder acc = new StringBuilder();
        acc.append("import greenfoot.*;\npublic class " + className);
        
        String superClass = greenfootSuperClass();
        if (superClass != null) {
            acc.append(" extends ").append(superClass);
        }
        
        costumes = new String[getCostumeCount()];
        
        acc.append("\n{\n");
        acc.append("private static final String[] COSTUMES = new String[] {");
        int i = 0;
        int curCostume = 0;
        for (ImageMedia img : getCostumes()) {
            if (i != 0) acc.append(", ");
            costumes[i] = img.saveInto(destDir, props, className + "_").getName();
            acc.append("\"").append(costumes[i]).append("\"");
            if (img == imageMedia) {
                curCostume = i;
            }
            i += 1;
        }
        acc.append("};\n");
        acc.append("private static final int[] X_OFFSETS = new int[] {");
        i = 0;
        for (ImageMedia img : getCostumes()) {
            if (i != 0) acc.append(", ");
            acc.append(
                  getScaleAmount().x.multiply(
                    new BigDecimal(img.getWidth() / 2).subtract(
                      img.getRotationCentre().x
                    )
                  ).intValue());
            i++;
        }
        acc.append("};\n");
        
        acc.append("private static final int[] Y_OFFSETS = new int[] {");
        i = 0;
        for (ImageMedia img : getCostumes()) {
            if (i != 0) acc.append(", ");
            acc.append(
                    getScaleAmount().y.multiply(
                      new BigDecimal(img.getHeight() / 2).subtract(
                        img.getRotationCentre().y
                      )
                    ).intValue());
            i++;
        }
        acc.append("};\n");
        
        acc.append("private int curCostume = ").append(curCostume).append(";\n");
        
        acc.append("public " + className + "()\n{\n");
        constructorContents(acc);    
        acc.append("}\n");
        
        addHelpers(acc);

        codeForScripts(getBlocks(), acc, new LoopVarIterator());
        acc.append("}\n");
        
        javaFile = new File(destDir, className + ".java");
        FileWriter javaFileWriter = new FileWriter(javaFile);
        javaFileWriter.write(acc.toString());
        javaFileWriter.close();
        
        for (ScratchObject media : getMedia()) {
            media.saveInto(destDir, props, className + "_");
        }
        
        
        File imageFile = imageMedia.saveInto(destDir, props, className + "_");
        props.setProperty("class." + className + ".image", imageFile.getName());
        
        return javaFile;
    }

    protected void addHelpers(StringBuilder acc)
    {
    }

    private void codeForBlock(ScratchObject block, StringBuilder decl, StringBuilder method, LoopVarIterator loopVars)
    {
        // Each block is an array of entries.  For example, if you have
        // a repeat block, that will be an array of three things:
        // [doRepeat, <the number of counts>, <the block to repeat>]
        // The block to repeat will in turn be an array and so on!
        ScratchObject[] blockContents = (ScratchObject[])block.getValue();
        
        if ("doRepeat".equals(blockContents[0].getValue())) {
            String var = loopVars.next();
            method.append("for (int " + var + " = 0; " + var + " < ").append(blockContents[1].getValue()).append(";" + var + "++)\n{\n");
            codeForBlock(blockContents[2], decl, method, new LoopVarIterator(loopVars));
            method.append("}\n");
        }
        else if ("doPlaySoundAndWait".equals(blockContents[0].getValue())) {
            String soundName = ScratchImport.mungeUnique("snd" + (String)blockContents[1].getValue());
            decl.append("GreenfootSound ").append(soundName).append(";\n");
            method.append("if (" + soundName + " == null || !" + soundName + ".isPlaying()) {\n")
                  .append(soundName).append(" = new GreenfootSound(\"")
                  .append(getObjNameJava()).append("_")
                  .append(blockContents[1].getValue())
                  .append(".wav\");\n")
                  .append(soundName).append(".play();\n}\n");
            // TODO need a state machine to wait in the state until done playing
        }
        else if ("playSound:".equals(blockContents[0].getValue())) {
            method.append("new GreenfootSound(\"")
                  .append(getObjNameJava()).append("_")
                  .append(blockContents[1].getValue())
                  .append(".wav\").play();\n");
        }
        else if ("setGraphicEffect:to:".equals(blockContents[0].getValue())) {
            if ("ghost".equals(blockContents[1].getValue())) {
                method.append("getImage().setTransparency(")
                      .append(new BigDecimal(255).subtract(((BigDecimal)blockContents[2].getValue()).multiply(BigDecimal.valueOf(255.0 / 100.0))).intValue())
                      .append(");\n");
            }
        }
        else if ("hide".equals(blockContents[0].getValue())) {
            method.append("getImage().setTransparency(0);\n");
        }
        else if ("show".equals(blockContents[0].getValue())) {
            method.append("getImage().setTransparency(255);\n");
        }
        else if ("lookLike:".equals(blockContents[0].getValue())) {
            if (blockContents[1].getValue() instanceof String) {
                String costumeRoot = getObjNameJava() + "_" + (String)blockContents[1].getValue();
                
                int costumeFile = findCostume(costumeRoot + ".png");
                if (costumeFile >= 0) {
                    costumeRoot += ".png";
                } else {
                    costumeFile = findCostume(costumeRoot + ".jpg");
                    if (costumeFile >= 0) costumeRoot += ".jpg";
                }
                if (costumeFile >= 0) {
                    method.append("curCostume = ")
                          .append(costumeFile)
                          .append(";\n");
                    method.append("setImage(\"").append(costumeRoot).append("\");\n");
                    method.append("getImage().scale(")
                          .append(getBounds().x2.subtract(getBounds().x).intValue())
                          .append(", ")
                          .append(getBounds().y2.subtract(getBounds().y).intValue())
                          .append(");\n");
                    method.append("getWorld().repaint();\n");
                } else {
                    Debug.message("Could not locate costume: " + costumeFile);
                }
            }
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
        else if ("say:duration:elapsed:from:".equals(blockContents[0].getValue())) {
            if (blockContents.length >= 3) {
                method.append("{\nBubble bubble = new Bubble(\"")
                      .append((String)blockContents[1].getValue())
                      .append("\");\n");
                method.append("getWorld().addObject(bubble, getX(), getY());");
                method.append("getWorld().repaint();\n");
                codeForBlock(new ScratchObjectArray(new ScratchObject[] {new ScratchPrimitive("wait:elapsed:from:"), blockContents[2]}), decl, method, loopVars);
                method.append("getWorld().removeObject(bubble);\n}\n");
            }
        }
        else if ("gotoX:y:".equals(blockContents[0].getValue())) {
            method.append("setLocation((getWorld().getWidth() / 2) + ")
                  .append(((BigDecimal)blockContents[1].getValue()).intValue())
                  .append(" + X_OFFSETS[curCostume]")
                  .append(", (getWorld().getHeight() / 2) - ")
                  .append(((BigDecimal)blockContents[2].getValue()).intValue())
                  .append(" + Y_OFFSETS[curCostume]")
                  .append(");\n");
        }
        else if ("turnRight:".equals(blockContents[0].getValue())) {
            String degrees = blockContents[1].getValue().toString();
            method.append("turn(-").append(degrees).append(");\n");
        }
        else if ("nextCostume".equals(blockContents[0].getValue())) {
            method.append("curCostume = (curCostume + 1) % COSTUMES.length;\n");
            method.append("setImage(COSTUMES[curCostume]);\n");
            method.append("getImage().scale(")
                  .append(getBounds().x2.subtract(getBounds().x).intValue())
                  .append(", ")
                  .append(getBounds().y2.subtract(getBounds().y).intValue())
                  .append(");\n");
            method.append("getWorld().repaint();\n");
        }
        else if ("wait:elapsed:from:".equals(blockContents[0].getValue())) {
            if (blockContents[1].getValue() instanceof BigDecimal) {
                BigDecimal seconds = (BigDecimal) blockContents[1].getValue();
                method.append("try {\n");
                method.append("Thread.sleep(").append(seconds.scaleByPowerOfTen(3).intValue()).append(");\n");
                method.append("} catch (InterruptedException e) { }");
            }
        }
        else if (blockContents[0] instanceof ScratchObjectArray) {
            for (ScratchObject blockContent : blockContents) {
                codeForBlock(blockContent, decl, method, loopVars);
            }
        }
        else {
            StringBuilder tmp = new StringBuilder();
            for (ScratchObject o : blockContents) {
                tmp.append(o).append(",");
            }
            Debug.message("Unknown Scratch block/code: " + tmp.toString());
        }
    }

    private int findCostume(String searchFor)
    {
        for (int i = 0; i < costumes.length;i++) {
            if (searchFor.equals(costumes[i])) {
                return i;
            }
        }
        return -1;
    }

    private void codeForScripts(ScratchObjectArray scripts, StringBuilder acc, LoopVarIterator loopVars)
    {
        StringBuilder decl = new StringBuilder();
        StringBuilder method = new StringBuilder();
        StringBuilder firstTimeCode = new StringBuilder();
        
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
                        codeForBlock(block, decl, method, loopVars);
                    }
                    
                    method.append("}");
                }
            }
            else if ("EventHatMorph".equals(firstBlockContents[0].getValue())) {
                if ("Scratch-StartClicked".equals(firstBlockContents[1].getValue())) {
                    // Don't need any special code for when flag clicked, that's just the first time
                    // that the act() method will be run anyway:
                    
                    ScratchObject[] subsequentBlocks = Arrays.copyOfRange(blocks, 1, blocks.length);
                    
                    if (subsequentBlocks.length == 1 && isLoop(subsequentBlocks[0])) {
                        codeForBlock(subsequentBlocks[0], decl, method, loopVars);
                    } else {
                        
                        int i;
                        for (i = 0; i < subsequentBlocks.length;i++) {
                            ScratchObject[] blockContents = (ScratchObject[]) subsequentBlocks[i].getValue();
                            if (!isLoop(blockContents[0])) {
                                codeForBlock(subsequentBlocks[i], decl, firstTimeCode, loopVars);
                            } else {
                                break;
                            }
                        }

                        if (i < subsequentBlocks.length) {
                            ScratchObject[] blockContents = (ScratchObject[]) subsequentBlocks[i].getValue();
                            //Must be a loop:
                            if ("doForever".equals(blockContents[0].getValue())) {
                                //Just do it each time in the act method:
                                codeForBlock(blockContents[1], decl, method, loopVars);
                            } else if ("doRepeat".equals(blockContents[0].getValue())) {
                                decl.append("private int loopCount = 0;\n");
                                
                                method.append("if (loopCount < ")
                                      .append(blockContents[1].getValue())
                                      .append(") {\n");
                                method.append("loopCount += 1;\n");
                                codeForBlock(blockContents[2], decl, method, loopVars);
                                method.append("}\n");
                            }
                        }
                    }
                }
            } else if ("KeyEventHatMorph".equals(firstBlockContents[0].getValue())) {
                String scratchKeyName = (String) firstBlockContents[1].getValue();
                String greenfootKeyName = null;
                if (scratchKeyName.endsWith(" arrow")) {
                    greenfootKeyName = scratchKeyName.substring(0, scratchKeyName.length() - " arrow".length());
                }
                
                if (greenfootKeyName != null) {
                    method.append("if (Greenfoot.isKeyDown(\"" + greenfootKeyName + "\")) {\n");
                    
                    ScratchObject[] subsequentBlocks = Arrays.copyOfRange(blocks, 1, blocks.length);
                    for (ScratchObject block : subsequentBlocks) {
                        codeForBlock(block, decl, method, loopVars);
                    }
                    method.append("}\n");
                }
            }
            else {
                // else ignore for now
                Debug.message("Ignoring block headed: " + firstBlockContents[0].getValue());
            }            
        }
        
        acc.append(decl);
        if (firstTimeCode.length() > 0) {
            acc.append("private boolean firstTime = true;\n");
        }
        acc.append("public void act()\n{\n");
        if (firstTimeCode.length() > 0) {
            acc.append("if (firstTime) {\n");
            acc.append("firstTime = false;\n");
            acc.append(firstTimeCode);
            acc.append("}\n");
        }
        acc.append(method);
        acc.append("}\n");
    }

    private static boolean isLoop(ScratchObject scratchObject)
    {
        return "doForever".equals(scratchObject.getValue()) || "doRepeat".equals(scratchObject.getValue());
    }

    public ScratchPoint getScaleAmount()
    {
        return new ScratchPoint(new BigDecimal(1), new BigDecimal(1));
    }
    
    private static class LoopVarIterator
    {
        int i = 0;
        
        public LoopVarIterator() {}
        public LoopVarIterator(LoopVarIterator it) { i = it.i; }

        public String next()
        {
            switch (i++)
            {
            case 0: return "i";
            case 1: return "j";
            case 2: return "k";
            default: return "i" + (i - 2);
            }
        }

    }
}
