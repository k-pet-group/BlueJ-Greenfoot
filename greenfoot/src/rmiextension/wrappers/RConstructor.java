package rmiextension.wrappers;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RConstructor.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RConstructor
    extends Remote
{
    public Class[] getParameterTypes()
        throws RemoteException;

    public boolean matches(Class[] parameter)
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