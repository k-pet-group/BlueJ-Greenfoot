package greenfoot.event;

import greenfoot.GreenfootWorld;
import greenfoot.WorldHandler;

import java.rmi.RemoteException;

import rmiextension.ObjectTracker;
import rmiextension.wrappers.RObject;
import rmiextension.wrappers.event.RInvocationEvent;
import rmiextension.wrappers.event.RInvocationListenerImpl;

/**
 * Listener that listens for the creation of GreenfootWorld objects and installs
 * the new world.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldCreationListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public class WorldCreationListener extends RInvocationListenerImpl
{
    private WorldHandler handler;

    public WorldCreationListener(WorldHandler handler)
        throws RemoteException
    {
        super();
        this.handler = handler;
    }

    public void invocationFinished(RInvocationEvent event)
        throws RemoteException
    {
        Object result = event.getResult();

        if (result == null || !(result instanceof RObject)) {
            return;
        }

        RObject remoteObj = (RObject) result;

        Object realObject = ObjectTracker.instance().getRealObject(remoteObj);

        if (realObject instanceof GreenfootWorld) {
            handler.installNewWorld(remoteObj);
        }
    }
}