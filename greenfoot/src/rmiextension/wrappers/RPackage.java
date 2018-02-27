/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2014,2016  Poul Henriksen and Michael Kolling 
 
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

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.BObject;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;

/**
 * The remote interface for a package.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public interface RPackage
    extends java.rmi.Remote
{
    /**
     * Get a remote reference to a class within the package.
     */
    public abstract RClass getRClass(String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Get all classes within the package
     */
    public abstract RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Get the project to which this package belongs.
     */
    public abstract RProject getProject()
        throws ProjectNotOpenException, RemoteException;

    /**
     * Close the package.
     */
    public void close() throws RemoteException;
}
