package greenfoot.event;

import java.util.EventListener;

/**
 * Listener to recieve notifcations when worlds are created and removed.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public interface WorldListener
    extends EventListener
{
    /**
     * Called when a new world is created and shown.
     */
    public void worldCreated(WorldEvent e);

    /**
     * Called when a world is removed.
     */
    public void worldRemoved(WorldEvent e);
}