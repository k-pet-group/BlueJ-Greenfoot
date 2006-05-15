package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * Abstract implementation of the RClassListener interface
 * @author dam
 *
 */
public abstract class RClassListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RClassListener
{
    public RClassListenerImpl() throws RemoteException
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassListener#classStateChanged(rmiextension.wrappers.event.RClassEvent)
     */
    public abstract void classStateChanged(RClassEvent event)
        throws RemoteException;

}
