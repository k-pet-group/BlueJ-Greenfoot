package rmiextension.wrappers.event;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileEvent.java 3124 2004-11-18 16:08:48Z polle $
 *  
 */
public interface RCompileEvent
    extends Remote
{
    /**
     * @return
     */
    public int getErrorLineNumber()
        throws RemoteException;

    /**
     * @return
     */
    public String getErrorMessage()
        throws RemoteException;

    /**
     * @return
     */
    public int getEvent()
        throws RemoteException;

    /**
     * @return
     */
    public File[] getFiles()
        throws RemoteException;

    /**
     * @param aLineNumber
     */
    public void setErrorLineNumber(int aLineNumber)
        throws RemoteException;

    /**
     * @param anErrorMessage
     */
    public void setErrorMessage(String anErrorMessage)
        throws RemoteException;
}