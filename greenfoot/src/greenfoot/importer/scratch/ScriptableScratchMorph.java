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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

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
    
    protected abstract String greenfootSuperClass();
    protected abstract void constructorContents(StringBuilder acc);

    @Override
    public String saveInto(GProject project) throws IOException
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
        acc.append("public " + className + "()\n{\n");
        constructorContents(acc);    
        acc.append("}\n");
         
        codeForScripts(getBlocks(), acc);
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
    }
    
    
}
