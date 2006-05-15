package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RClassImpl;
import bluej.extensions.event.ClassEvent;

/**
 * Implementation of a remote class event. Wraps a local ClassEvent.
 * 
 * @author Davin McCall
 * @version $Id: RClassEventImpl.java 4261 2006-05-15 10:54:18Z davmac $
 */
public class RClassEventImpl extends java.rmi.server.UnicastRemoteObject
    implements RClassEvent
{
    private ClassEvent event;
    
    /**
     * Construct a remote event wrapper for the supplied ClassEvent.
     * 
     * @throws RemoteException
     */
    public RClassEventImpl(ClassEvent event)
        throws RemoteException
    {
        super();
        this.event = event;
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#getRClass()
     */
    public RClass getRClass() throws RemoteException
    {
        return new RClassImpl(event.getBClass());
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#isClassCompiled()
     */
    public boolean isClassCompiled() throws RemoteException
    {
        return event.isClassCompiled();
    }
}
