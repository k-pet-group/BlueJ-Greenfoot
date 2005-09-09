package rmiextension.wrappers;

import java.rmi.RemoteException;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RObject.java 3556 2005-09-09 13:40:58Z polle $
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
     * @throws PackageNotFoundException 
     */
    public abstract RClass getRClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException, PackageNotFoundException;

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

    
    // no longer needed
//    public MenuSerializer getMenu()
//        throws RemoteException;
    
    /**
     * Allow an arbitrary method to be invoked with arbitrary parameters.
     * 
     * If a compilation error occurs, returns the generated error message
     * preceded by an exclamation mark (!).<p>
     * 
     * If successful and a return value exists, put the return value on the
     * bench and return its name.<p>
     * 
     * Otherwise (no error, no return value) return null. 
     * 
     * @param method    The name of the method to invoke
     * @param argTypes  The classnames of the argument types of the method
     * @param argVals   The argument "values" as they should appear in the shell code
     * @throws RemoteException
     */
    public String invokeMethod(String method, String [] argTypes, String [] argVals)
        throws RemoteException;

}