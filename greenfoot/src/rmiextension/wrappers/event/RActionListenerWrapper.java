package rmiextension.wrappers.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Wrapper for a remote ActionListener.
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListenerWrapper.java 3124 2004-11-18 16:08:48Z polle $
 * 
 * @see rmiextension.event.RActionListener
 * @see rmiextension.event.RActionListenerWrapper
 *  
 */
public class RActionListenerWrapper
    implements ActionListener, Serializable
{

    private RActionListener remoteActionListener;

    public RActionListenerWrapper(RActionListener remoteActionListener)
    {
        this.remoteActionListener = remoteActionListener;
    }

    public void actionPerformed(ActionEvent e)
    {
        try {
            remoteActionListener.actionPerformed(e);
        }
        catch (RemoteException e1) {
            e1.printStackTrace();
        }
    }

}