/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package rmiextension.wrappers.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * Remote ActionListener that delegates events to multiple ActionListeners
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListenerImpl.java 6216 2009-03-30 13:41:07Z polle $
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