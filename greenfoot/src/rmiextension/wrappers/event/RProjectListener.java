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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * A listener for project events. Currently the only event is a project close.
 * 
 * @author Davin McCall
 * @version $Id: RProjectListener.java 6216 2009-03-30 13:41:07Z polle $
 */
public interface RProjectListener
    extends Remote
{
    /**
     * The project is about to close. This is called before the project actually
     * closes, and will only be called if the close() method in RProject is used
     * to close the project.
     * 
     * @throws RemoteException
     */
    public void projectClosing() throws RemoteException;
}
