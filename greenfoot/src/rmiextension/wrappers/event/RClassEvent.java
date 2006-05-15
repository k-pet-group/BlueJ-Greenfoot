package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;

/**
 * Interface for remote class events.
 * 
 * @author Davin McCall
 * @version $Id: RClassEvent.java 4261 2006-05-15 10:54:18Z davmac $
 */
public interface RClassEvent extends Remote
{
    /**
     * Get the class on which the event occurred.
     * 
     * @throws RemoteException
     */
    public RClass getRClass() throws RemoteException;
    
    /**
     * Check whether the class in the event is compiled.
     * 
     * @throws RemoteException
     */
    public boolean isClassCompiled() throws RemoteException;
}
