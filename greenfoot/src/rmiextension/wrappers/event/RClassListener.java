/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface representing remote class listeners.
 * 
 * @author Davin McCall
 */
public interface RClassListener extends Remote
{
    /**
     * The class state changed. This means that the class source was
     * changed so that the class is now uncompiled, or the class was
     * compiled.
     * 
     * <p>Use event.isCompiled() to check the class compilation state.
     * 
     * <p>This is called from the Swing event queue on the main VM, so calls which
     * require access to the event queue must not be made from the same thread.
     * 
     * @throws RemoteException  if a remote exception occurs
     */
    public void classStateChanged(RClassEvent event)
        throws RemoteException;

}
