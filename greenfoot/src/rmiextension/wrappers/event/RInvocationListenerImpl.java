package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationListenerImpl.java,v 1.3 2004/11/18 09:43:50 polle
 *          Exp $
 */
public abstract class RInvocationListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RInvocationListener
{

    /**
     * @throws RemoteException
     */
    public RInvocationListenerImpl(/* InvocationListener invocationListener */)
        throws RemoteException
    {
        super();
        //  this.invocationListener = invocationListener;
    }

    /**
     * This method will be called when an invocation has finished. If a long
     * operation must be performed you should start a Thread.
     */
    public abstract void invocationFinished(RInvocationEvent event)
        throws RemoteException;

}