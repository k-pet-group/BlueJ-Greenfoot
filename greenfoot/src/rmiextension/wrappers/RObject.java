package rmiextension.wrappers;

import java.rmi.RemoteException;

import rmiextension.MenuSerializer;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RObject.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RObject
    extends java.rmi.Remote
{
    /**
     * @param instanceName
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void addToBench(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RClass getRClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     */
    public abstract String getInstanceName()
        throws RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void removeFromBench()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public MenuSerializer getMenu()
        throws RemoteException;
}