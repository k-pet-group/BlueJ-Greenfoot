package greenfoot.importer.scratch;

import java.awt.Rectangle;
import java.util.List;

public class ScratchSpriteMorph extends ScriptableScratchMorph
{
    public ScratchSpriteMorph(int version, List<ScratchObject> scratchObjects)
    {
        super(ScratchUserObject.SCRATCH_SPRITE_MORPH, version, scratchObjects);
    }

    @Override
    protected void constructorContents(StringBuilder acc)
    {
        acc.append("GreenfootImage img = getImage();\n");
        acc.append("img.scale(")
           .append(((Rectangle)scratchObjects.get(0).getValue()).width)
           .append(", ")
           .append(((Rectangle)scratchObjects.get(0).getValue()).height)
           .append(");\n");
    }

    @Override
    protected String greenfootSuperClass()
    {
        return "Actor";
    }

}
