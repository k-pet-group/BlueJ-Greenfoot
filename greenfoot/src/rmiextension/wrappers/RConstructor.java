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
package rmiextension.wrappers;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RConstructor.java 8234 2010-09-02 10:17:24Z nccb $
 */
public interface RConstructor
    extends Remote
{
    public Class<?>[] getParameterTypes()
        throws RemoteException;

    public boolean matches(Class<?>[] parameter)
        throws RemoteException;

    public RObject newInstance(Object[] initargs)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, InvocationArgumentException,
        InvocationErrorException;

    /**
     * This should actually have been the toString() method, but we cannot add
     * an exception to an inherited method. And to get RMI to work, it must
     * throw RemoteException.
     * 
     * @return
     */
    public String getToString()
        throws RemoteException;
}