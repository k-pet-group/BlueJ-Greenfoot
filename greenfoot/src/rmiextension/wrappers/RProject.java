/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013  Poul Henriksen and Michael Kolling 
 
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

import rmiextension.wrappers.event.RProjectListener;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Interface for a remote project.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public interface RProject
    extends java.rmi.Remote
{
    /**
     * Close this project. This will cause the local VM to
     * terminate.
     */
    public abstract void close()
        throws RemoteException;

    /**
     * Get the project directory.
     * @throws ProjectNotOpenException
     */
    public abstract File getDir()
        throws ProjectNotOpenException, RemoteException;

    /**
     * Get the project's name.
     * 
     * @throws ProjectNotOpenException
     */
    public abstract String getName()
        throws ProjectNotOpenException, RemoteException;

    /**
     * Get a remote reference to a package within the project, by name.
     * 
     * @throws ProjectNotOpenException
     */
    public abstract RPackage getPackage(String name)
        throws ProjectNotOpenException, RemoteException;

    /**
     * Create a new package within the project.
     * 
     * @throws ProjectNotOpenException     if the project is no longer open
     * @throws PackageAlreadyExistsException  if the package already exists
     * @throws RemoteException  if a remote exception occurred
     */
    public abstract RPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException;

    /**
     * Get all packages within this project.
     * 
     * @throws ProjectNotOpenException
     */
    public abstract RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException;

    /**
     * Request a save of all open files in the project.
     * 
     * @throws ProjectNotOpenException
     */
    public abstract void save()
        throws ProjectNotOpenException, RemoteException;
    
    /**
     * Open the "readme" editor for this project.
     * 
     * @throws ProjectNotOpenException
     * @throws RemoteException
     */
    public abstract void openReadmeEditor()
        throws ProjectNotOpenException, RemoteException;
    
    /**
     * Add a listener to this project
     * @param listener  The listener to add
     * @throws RemoteException
     */
    public abstract void addListener(RProjectListener listener)
        throws RemoteException;
    
    /**
     * Remove a listener from this project
     * @param listener  The listener to remove
     * @throws RemoteException
     */
    public abstract void removeListener(RProjectListener listener)
        throws RemoteException;
    
    /**
     * Get a remote reference to the object in the launcher "transport" field. The purpose
     * of this is to allow obtaining a remote reference to a local object, by means of:
     * 
     * <ol>
     * <li>(in the user VM) Storing a reference to the object into the transport field
     * <li>Calling this method.
     * </ul>
     * 
     * @return  A remote reference to the transport field object.
     * @throws RemoteException  If an RMI exception occurs.
     */
    public abstract RObject getRemoteObject()
        throws RemoteException;

    /**
     * Toggles the BlueJ debugger (Shows/Hides)
     * @throws RemoteException  if an RMI error occurs
     */
    public abstract void toggleExecControls()
        throws RemoteException;

    /**
     * @return Whether or not the debugger window is currently visible
     * @throws RemoteException   if an RMI error occurs
     * @throws ProjectNotOpenException   if the project is no longer open
     */
    public abstract boolean isExecControlVisible()
        throws RemoteException, ProjectNotOpenException;

    /**
     * Restart the debug VM
     * @throws RemoteException   if an RMI error occurs
     * @throws ProjectNotOpenException   if the project is no longer open
     */
    public abstract void restartVM()
        throws RemoteException, ProjectNotOpenException;

    /**
     * @return Whether or not the VM had been restarted
     * @throws RemoteException   if an RMI error occurs
     */
    public abstract boolean isVMRestarted() throws RemoteException;

    /**
     * Change the state of VM that indicates if it had been restarted
     * @throws RemoteException   if an RMI error occurs
     */
    public void setVmRestarted(boolean vmRestarted) throws RemoteException;
}
