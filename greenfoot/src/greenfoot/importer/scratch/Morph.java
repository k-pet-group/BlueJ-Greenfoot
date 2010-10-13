package greenfoot.importer.scratch;

import java.awt.Rectangle;
import java.util.List;

public class Morph extends ScratchUserObject
{
    public Morph(int id, int version, List<ScratchObject> scratchObjects)
    {
        super(id, version, scratchObjects);
    }

    // Fields:
    //  bounds (Rectangle), owner (?), submorphs (array), color (Color), flags (int), placeholder (null)
    
    public int fields()
    {
        return 6; 
    }
    
    public Rectangle getBounds()
    {
        return (Rectangle)scratchObjects.get(0).getValue();
    }
}
