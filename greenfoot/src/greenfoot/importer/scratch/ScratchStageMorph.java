package greenfoot.importer.scratch;

import java.awt.Rectangle;
import java.util.List;

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
        ScratchImage image = getCostume().getImage();
        acc.append("super(").append(image.getWidth()).append(", ").append(image.getHeight()).append(", 1);\n");
        
        ScratchObjectArray sprites = getSprites();
        for (ScratchObject o : sprites.getValue()) {
            ScratchSpriteMorph sprite = (ScratchSpriteMorph)o;
            String spriteName = sprite.getObjName();
            acc.append("addObject(new ").append(spriteName).append("(), ");
            acc.append((int)((Rectangle)sprite.scratchObjects.get(0).getValue()).getCenterX());
            acc.append(", ");
            acc.append((int)((Rectangle)sprite.scratchObjects.get(0).getValue()).getCenterY());
            acc.append(");\n");
        }
    }

    @Override
    protected String greenfootSuperClass()
    {
        return "World";
    }
}

