package rmiextension.wrappers;

import java.rmi.RemoteException;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RField.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RField
    extends java.rmi.Remote
{
    /**
     * @return
     */
    public abstract int getModifiers()
        throws RemoteException;

    /**
     * @return
     */
    public abstract String getName()
        throws RemoteException;

    /**
     * @return
     */
    public abstract Class getType()
        throws RemoteException;

    /**
     * @param onThis
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RObject getValue(RObject onThis)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @param fieldName
     * @return
     */
    public abstract boolean matches(String fieldName)
        throws RemoteException;
}