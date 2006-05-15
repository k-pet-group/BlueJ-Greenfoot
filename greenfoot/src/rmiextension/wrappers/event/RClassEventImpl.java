package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.WrapperPool;
import bluej.extensions.event.ClassEvent;

/**
 * Implementation of a remote class event. Wraps a local ClassEvent.
 * 
 * @author Davin McCall
 * @version $Id: RClassEventImpl.java 4262 2006-05-15 12:21:15Z davmac $
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
        return WrapperPool.instance().getWrapper(event.getBClass());
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#isClassCompiled()
     */
    public boolean isClassCompiled() throws RemoteException
    {
        return event.isClassCompiled();
    }
}
