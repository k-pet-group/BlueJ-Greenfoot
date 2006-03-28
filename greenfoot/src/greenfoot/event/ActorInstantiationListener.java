package greenfoot.event;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.ObjectTracker;
import greenfoot.core.ObjectDragProxy;
import greenfoot.core.WorldHandler;
import greenfoot.gui.DragGlassPane;

import java.rmi.RemoteException;

import rmiextension.wrappers.RObject;
import rmiextension.wrappers.event.RInvocationEvent;
import rmiextension.wrappers.event.RInvocationListenerImpl;

/**
 * Listens for new instances of GrenfootObjects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ActorInstantiationListener.java,v 1.6 2004/11/18
 *          09:43:52 polle Exp $
 */
public class ActorInstantiationListener extends RInvocationListenerImpl
{
    private WorldHandler worldHandler;
    
    public ActorInstantiationListener(WorldHandler worldHandler)
        throws RemoteException
    {
        super();
        this.worldHandler = worldHandler;
    }

    public void invocationFinished(RInvocationEvent event)
        throws RemoteException
    {
        Object result = event.getResult();

        if (result == null || !(result instanceof RObject)) {
            return;
        }

        RObject remoteObj = (RObject) result;

        if (event.getMethodName() != null) {
            //this is not a call from a constructor
            return;
        }

        Object realObject = ObjectTracker.getRealObject(remoteObj);
        localObjectCreated(realObject);
    }
    
    /**
     * Second entry point, for when an object has been created locally.
     * @param realObject  The newly instantiated object
     */
    public void localObjectCreated(Object realObject)
    {
        if (realObject instanceof ObjectDragProxy) {
            Actor go = (Actor) realObject;
            int xoffset = 0;
            int yoffset = 0;
            DragGlassPane.getInstance().startDrag(go, xoffset, yoffset, null, null, false);
        }
        else if(realObject instanceof Actor) {
            // We do not need to add the object to the world, since the object
            // is automatically added in the constructor of Actor

            // We should however make sure that the world is repainted once the
            // object is created:
            worldHandler.repaint();
        }
        else if(realObject instanceof greenfoot.World) {
            worldHandler.installNewWorld((World) realObject);
        }
    }

}