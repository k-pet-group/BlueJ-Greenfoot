package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationListener.java 3262 2005-01-12 03:30:49Z davmac $
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
