package rmiextension.wrappers.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * Remote ActionListener that delegates events to multiple ActionListeners
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListenerImpl.java 3124 2004-11-18 16:08:48Z polle $
 * 
 * @see rmiextension.event.RActionListener
 * @see rmiextension.event.RActionListenerWrapper
 */
public class RActionListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RActionListener
{
    private transient ActionListener[] actionListeners;
    private transient Object source;

    /**
     * Creates a new remote ActionListener that will delegate the actionEvents
     * to the given actionListeners.
     * 
     * 
     * @param actionListeners
     *            The bunch of ActionListeners to delegate events to
     * @param source
     *            The source that the event should get. (because it can't use
     *            the remote source!)
     * 
     * @throws RemoteException
     */
    public RActionListenerImpl(ActionListener[] actionListeners, Object source)
        throws RemoteException
    {
        this.actionListeners = actionListeners;
        this.source = source;
    }

    /**
     * Delegates the ActionEvents to all the listeners that was specified in the
     * constructor
     * 
     * @param e
     *            the ActionEvent that will be dispatched. The source will be
     *            replaced by the one defined in the constructor.
     * @see rmiextension.event.RActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e)
        throws RemoteException
    {
        //TODO maybe do the stuff with SwingUtilities.invokeLater
        e.setSource(source);
        for (int i = 0; i < actionListeners.length; i++) {
            ActionListener actionListener = actionListeners[i];
            actionListener.actionPerformed(e);
        }
    }

}