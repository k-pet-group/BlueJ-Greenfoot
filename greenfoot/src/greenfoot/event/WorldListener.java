package greenfoot.event;

import java.util.EventListener;

/**
 * Listener for events on the world (GreenfootWorld)
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public interface WorldListener extends EventListener
{
    public void worldChanged(WorldEvent e);
}
