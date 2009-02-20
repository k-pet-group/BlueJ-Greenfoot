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