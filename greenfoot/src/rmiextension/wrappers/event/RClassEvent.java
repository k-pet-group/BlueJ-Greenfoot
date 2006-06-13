package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;

/**
 * Interface for remote class events.
 * 
 * @author Davin McCall
 * @version $Id: RClassEvent.java 4356 2006-06-13 05:20:11Z davmac $
 */
public interface RClassEvent extends Remote
{
    /**
     * Get the event ID.
     * 
     * @throws RemoteException
     */
    public int getEventId() throws RemoteException;
    
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
    
    /**
     * Get the new class name (valid for a CHANGING_NAME event).
     * 
     * @throws RemoteException
     */
    public String getOldName() throws RemoteException;
}
