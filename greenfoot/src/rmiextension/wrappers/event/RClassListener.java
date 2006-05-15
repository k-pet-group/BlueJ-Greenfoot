package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing remote class listeners.
 * 
 * @author Davin McCall
 * @version $Id: RClassListener.java 4261 2006-05-15 10:54:18Z davmac $
 */
public interface RClassListener extends Remote
{
    /**
     * The class state changed. This means that the class source was
     * changed so that the class is now uncompiled, or the class was
     * compiled.
     * 
     * <p>Use event.isCompiled() to check the class compilation state.
     * 
     * @throws RemoteException  if a remote exception occurs
     */
    public void classStateChanged(RClassEvent event)
        throws RemoteException;

}
