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