package rmiextension.wrappers;

import java.rmi.RemoteException;

import rmiextension.MenuSerializer;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RObject.java 3227 2004-12-08 04:04:58Z davmac $
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
    
    /**
     * Allow an arbitrary method to be invoked with arbitrary parameters.
     * Returns the compiler error message generated, or null if everything
     * went ok.
     * 
     * @param method    The name of the method to invoke
     * @param argTypes  The classnames of the argument types of the method
     * @param argVals   The argument "values" as they should appear in the shell code
     * @throws RemoteException
     */
    public String invokeMethod(String method, String [] argTypes, String [] argVals)
        throws RemoteException;

}