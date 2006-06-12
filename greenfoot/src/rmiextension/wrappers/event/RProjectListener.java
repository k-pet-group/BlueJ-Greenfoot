package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A listener for project events. Currently the only event is a project close.
 * 
 * @author Davin McCall
 * @version $Id: RProjectListener.java 4349 2006-06-12 03:07:04Z davmac $
 */
public interface RProjectListener
    extends Remote
{
    /**
     * The project is about to close. This is called before the project actually
     * closes, and will only be called if the close() method in RProject is used
     * to close the project.
     * 
     * @throws RemoteException
     */
    public void projectClosing() throws RemoteException;
}
