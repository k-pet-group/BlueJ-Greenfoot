package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RProject.java 4279 2006-05-16 11:20:51Z davmac $
 */
public interface RProject
    extends java.rmi.Remote
{
    /**
     * @throws ProjectNotOpenException
     */
    public abstract void close()
        throws ProjectNotOpenException, RemoteException;

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

}