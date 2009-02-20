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
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for remote actionListeners. Actually it does not implement
 * actionlistener Because it isn't possible to make actionPerformed throw the
 * RemoteException when implenting it from actionListener.
 * 
 * @see rmiextension.event.RActionListener
 * @see rmiextension.event.RActionListenerWrapper
 * 
 * @author Poul Henriksen
 * @version $Id: RActionListener.java 6170 2009-02-20 13:29:34Z polle $
 *  
 */
public interface RActionListener
    extends Remote
{
    public void actionPerformed(ActionEvent e)
        throws RemoteException;
}