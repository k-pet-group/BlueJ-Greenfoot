/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Wrapper for a remote ActionListener.
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListenerWrapper.java 6170 2009-02-20 13:29:34Z polle $
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