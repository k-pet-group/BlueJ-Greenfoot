/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
import java.rmi.ServerError;
import java.rmi.ServerException;

import rmiextension.wrappers.RBlueJImpl;

import bluej.extensions.event.InvocationEvent;
import bluej.extensions.event.InvocationListener;
import bluej.utility.Debug;

/**
 * Wrapper for remote invocation listeners
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class RInvocationListenerWrapper
    implements InvocationListener
{
    private RInvocationListener remoteListener;
    private RBlueJImpl blueJ;

    public RInvocationListenerWrapper(RInvocationListener remoteListener)
    {
        this.remoteListener = remoteListener;
    }

    /*
     * @see bluej.extensions.event.InvocationListener#invocationFinished(bluej.extensions.event.InvocationEvent)
     */
    public void invocationFinished(InvocationEvent event)
    {
        try {
            RInvocationEvent rEvent = new RInvocationEventImpl(event);
            remoteListener.invocationFinished(rEvent);
        }
        catch (ServerError se) {
            Debug.reportError("Remote compile listener ServerError", se.getCause());
        }
        catch (ServerException se) {
            Debug.reportError("Remote compile listener ServerException", se.getCause());
        }
        catch (RemoteException re) {
            // Connection interrupted or other problem; remote VM no longer accessible
            blueJ.removeInvocationListener(remoteListener);
        }
    }

}