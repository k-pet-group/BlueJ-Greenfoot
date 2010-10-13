package greenfoot.importer.scratch;

import java.util.List;

public class ScratchMedia extends ScratchUserObject
{

    public ScratchMedia(int id, int version, List<ScratchObject> scratchObjects)
    {
        super(id, version, scratchObjects);
    }
    
    // Fields:
    //  mediaName (String)
    
    @Override public int fields()
    {
        return 1;
    }

    public String getMediaName()
    {
        return (String)scratchObjects.get(0).getValue();
    }
}
