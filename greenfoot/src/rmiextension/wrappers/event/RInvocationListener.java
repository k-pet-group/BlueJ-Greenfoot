package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RInvocationListener
    extends java.rmi.Remote
{

    /**
     * This method will be called when an invocation has finished.
     */
    public void invocationFinished(RInvocationEvent event)
        throws RemoteException;

}