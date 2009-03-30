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
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileListener.java 6216 2009-03-30 13:41:07Z polle $
 */
public interface RCompileListener
    extends Remote
{
    /**
     * This method will be called when a compilation starts. If a long operation
     * must be performed you should start a Thread.
     */
    public void compileStarted(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile error. If
     * a long operation must be performed you should start a Thread.
     */
    public void compileError(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileWarning(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile ends successfully. If a long
     * operation must be performed you should start a Thread.
     */
    public void compileSucceeded(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile fails. If a long operation
     * must be performed you should start a Thread.
     */
    public void compileFailed(RCompileEvent event)
        throws RemoteException;
}