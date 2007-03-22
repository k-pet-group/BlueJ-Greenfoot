package greenfoot.event;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.WorldHandler;

import java.awt.event.MouseEvent;

/**
 * Listens for new instances of GrenfootObjects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ActorInstantiationListener.java,v 1.6 2004/11/18
 *          09:43:52 polle Exp $
 */
public class ActorInstantiationListener
{
    private WorldHandler worldHandler;
    
    public ActorInstantiationListener(WorldHandler worldHandler)
    {
        super();
        this.worldHandler = worldHandler;
    }

    /**
     * Notification for when an object has been created.
     * @param realObject  The newly instantiated object
     * @param e           The mouse event used to locate where to position the actor
     *                    (if the object is an actor)
     */
    public void localObjectCreated(Object realObject, MouseEvent e)
    {
        if(realObject instanceof Actor) {
            worldHandler.addObjectAtEvent((Actor) realObject, e);
            worldHandler.repaint();
        }
        else if(realObject instanceof greenfoot.World) {
            worldHandler.setWorld((World) realObject);
        }
    }

}