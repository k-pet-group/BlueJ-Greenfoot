package rmiextension.wrappers.event;

import java.awt.event.ActionEvent;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for remote actionListeners. Actually it does not implement
 * actionlistener Because it isn't possible to make actionPerformed throw the
 * RemoteException when implenting it from actionListener.
 * 
 * @see rmiextension.event.RActionListener
 * @see rmiextension.event.RActionListenerWrapper
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListener.java 3124 2004-11-18 16:08:48Z polle $
 *  
 */
public interface RActionListener
    extends Remote
{
    public void actionPerformed(ActionEvent e)
        throws RemoteException;
}