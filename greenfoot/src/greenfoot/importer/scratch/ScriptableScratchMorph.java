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
    
    public String getObjName()
    {
        return (String)scratchObjects.get(super.fields() + 0).getValue();
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
        String className = getObjName();
        
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
