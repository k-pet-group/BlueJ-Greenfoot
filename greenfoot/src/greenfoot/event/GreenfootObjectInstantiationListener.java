package greenfoot.event;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootWorld;
import greenfoot.core.ObjectDragProxy;
import greenfoot.core.WorldHandler;
import greenfoot.gui.DragGlassPane;

import java.rmi.RemoteException;

import rmiextension.ObjectTracker;
import rmiextension.wrappers.RObject;
import rmiextension.wrappers.event.RInvocationEvent;
import rmiextension.wrappers.event.RInvocationListenerImpl;

/**
 * Listens for new instances of GrenfootObjects
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootObjectInstantiationListener.java,v 1.6 2004/11/18
 *          09:43:52 polle Exp $
 */
public class GreenfootObjectInstantiationListener extends RInvocationListenerImpl
{
    private WorldHandler worldHandler;
    
    public GreenfootObjectInstantiationListener(WorldHandler worldHandler)
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

        Object realObject = ObjectTracker.instance().getRealObject(remoteObj);
        localObjectCreated(realObject);
    }
    
    /**
     * Second entry point, for when an object has been created locally.
     * @param realObject  The newly instantiated object
     */
    public void localObjectCreated(Object realObject)
    {
        if (realObject instanceof ObjectDragProxy) {
            GreenfootObject go = (GreenfootObject) realObject;
            int xoffset = 0;
            int yoffset = 0;
            DragGlassPane.getInstance().startDrag(go, xoffset, yoffset, null, null);
        }
        else if(realObject instanceof GreenfootObject) {
            //We do nothing, since the object is automatically added to the world in the constructor of GreenfootObject
        }
        else if(realObject instanceof greenfoot.GreenfootWorld) {
            worldHandler.installNewWorld((GreenfootWorld) realObject);
        }
    }

}