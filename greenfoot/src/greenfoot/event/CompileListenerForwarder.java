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
package greenfoot.event;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;

import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RCompileListenerImpl;

/**
 * Class that forwards compile events to all the compile listeners registered
 * with Greenfoot. This is for performance reasons. Many objects are interested
 * in compile events, and if all these should use remote listeners it might be
 * to heavy. Consider using non remote compile events as well.
 * 
 * <p>Another feature of this class is that the events will be delegated to the compileListeners in
 * the order in which they appear in the list.
 * 
 * @author Poul Henriksen
 */
public class CompileListenerForwarder extends RCompileListenerImpl
{
    private List<? extends CompileListener> compileListeners;

    /**
     * Create a new forwarder that sends events to the list of listeners in the
     * order in which they appear in the list. The first listener in the list
     * will get the event first, and so on.
     * 
     */
    public CompileListenerForwarder(List<? extends CompileListener> compileListeners)
        throws RemoteException
    {
        this.compileListeners = compileListeners;
    }

    public void compileStarted(RCompileEvent event)
        throws RemoteException
    {
        synchronized (compileListeners) {
            for (Iterator<? extends CompileListener> iter = compileListeners.iterator(); iter.hasNext();) {
                CompileListener element = iter.next();
                element.compileStarted(event);
            }
        }
    }

    public void compileError(RCompileEvent event)
        throws RemoteException
    {
        synchronized (compileListeners) {
            for (Iterator<? extends CompileListener> iter = compileListeners.iterator(); iter.hasNext();) {
                CompileListener element = iter.next();
                element.compileError(event);
            }
        }
    }

    public void compileWarning(RCompileEvent event)
        throws RemoteException
    {
        synchronized (compileListeners) {
            for (Iterator<? extends CompileListener> iter = compileListeners.iterator(); iter.hasNext();) {
                CompileListener element = iter.next();
                element.compileWarning(event);
            }
        }
    }

    public void compileSucceeded(RCompileEvent event)
        throws RemoteException
    {
        CompileListener [] listenersCopy;
        
        synchronized (compileListeners) {
            listenersCopy = new CompileListener[compileListeners.size()];
            listenersCopy = compileListeners.toArray(listenersCopy);
        }
        
        for (int i = 0; i < listenersCopy.length; i++) {
            CompileListener listener = listenersCopy[i];
            listener.compileSucceeded(event);
        }
    }

    public void compileFailed(RCompileEvent event)
        throws RemoteException
    {
        synchronized (compileListeners) {
            for (Iterator<? extends CompileListener> iter = compileListeners.iterator(); iter.hasNext();) {
                CompileListener element = iter.next();
                element.compileFailed(event);
            }
        }
    }

}
