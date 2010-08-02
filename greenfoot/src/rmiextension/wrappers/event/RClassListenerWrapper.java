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

import bluej.extensions.event.ClassEvent;
import bluej.extensions.event.ClassListener;
import bluej.utility.Debug;

/**
 * Wraps a remote class listener (RClassListener) as a local listener.
 */
public class RClassListenerWrapper implements ClassListener
{
    private RClassListener remoteListener;
    private RBlueJImpl bluej;
    
    public RClassListenerWrapper(RBlueJImpl bluej, RClassListener listener)
    {
        this.bluej = bluej;
        remoteListener = listener;
    }
    
    public void classStateChanged(ClassEvent event)
    {
        try {
            remoteListener.classStateChanged(new RClassEventImpl(event));
        }
        catch (ServerError se) {
            Debug.reportError("Remote class listener ServerError", se.getCause());
        }
        catch (ServerException se) {
            Debug.reportError("Remote class listener ServerException", se.getCause());
        }
        catch (RemoteException re) {
            // Connection interrupted or other problem; remote VM no longer accessible
            bluej.removeClassListener(remoteListener);
        }
    }

}
