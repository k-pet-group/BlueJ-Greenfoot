package greenfoot.event;

import greenfoot.GreenfootObject;
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

    public GreenfootObjectInstantiationListener()
        throws RemoteException
    {
        super();
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
        if (realObject instanceof GreenfootObject) {
            GreenfootObject go = (GreenfootObject) realObject;
            DragGlassPane.getInstance().startDrag(go);
        }
    }

}