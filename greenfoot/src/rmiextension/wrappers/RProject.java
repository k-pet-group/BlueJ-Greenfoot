package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.event.RProjectListener;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RProject.java 4349 2006-06-12 03:07:04Z davmac $
 */
public interface RProject
    extends java.rmi.Remote
{
    /**
     * Close this project
     */
    public abstract void close()
        throws RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract File getDir()
        throws ProjectNotOpenException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract String getName()
        throws ProjectNotOpenException, RemoteException;

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract RPackage getPackage(String name)
        throws ProjectNotOpenException, RemoteException;

    public abstract RPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException;

    /**
     * @throws ProjectNotOpenException
     */
    public abstract void save()
        throws ProjectNotOpenException, RemoteException;
    
    /**
     * Open the "readme" editor for this project.
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
}