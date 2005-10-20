package greenfoot.event;

import java.util.EventObject;

/**
 * A world event
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class WorldEvent extends EventObject
{
    private int type;

    public WorldEvent(Object source)
    {
        super(source);
    }
}
