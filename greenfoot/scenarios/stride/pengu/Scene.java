// WARNING: This file is auto-generated and any changes to it will be overwritten
import java.util.*;
import greenfoot.*;

/**
 * This is the whole scene. It creates and contains the objects that are in it.
 */
public class Scene extends World
{

    /**
     * 
     */
    public Scene()
    {
        super(750, 500, 1);
        addObject( new  Cliff(false), 85, 441);
        addObject( new  Cliff(true), 665, 441);
        
        addObject( new  Cloud(), 369, 315);
        addObject( new  Pengu(), 66, 244);
    }
}
