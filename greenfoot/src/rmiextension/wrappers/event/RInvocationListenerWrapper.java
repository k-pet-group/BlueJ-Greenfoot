package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import bluej.extensions.event.InvocationEvent;
import bluej.extensions.event.InvocationListener;

/**
 * Wrapper for remote invocation listeners
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationListenerWrapper.java,v 1.4 2004/11/18 09:43:50 polle
 *          Exp $
 */
public class RInvocationListenerWrapper
    implements InvocationListener
{
    private RInvocationListener remoteListener;

    public RInvocationListenerWrapper(RInvocationListener remoteListener)
    {
        this.remoteListener = remoteListener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see bluej.extensions.event.InvocationListener#invocationFinished(bluej.extensions.event.InvocationEvent)
     */
    public void invocationFinished(InvocationEvent event)
    {
        try {
            RInvocationEvent rEvent = new RInvocationEventImpl(event);

            //TODO can give java.net.ConnectException. Might be because the
            // project is closed. Should remember to d-register listeneres
            remoteListener.invocationFinished(rEvent);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }

    }

}