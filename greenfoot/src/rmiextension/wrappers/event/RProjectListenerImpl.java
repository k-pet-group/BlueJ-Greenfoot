package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * Abstract implementation of an RProjectListener.
 * 
 * @author Davin McCall
 * @version $Id: RProjectListenerImpl.java 4350 2006-06-12 03:56:19Z davmac $
 */
public abstract class RProjectListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RProjectListener
{
    public RProjectListenerImpl() throws RemoteException
    {
        super();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    public abstract void projectClosing();

}
