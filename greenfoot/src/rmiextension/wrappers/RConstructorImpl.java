package rmiextension.wrappers;

import java.rmi.RemoteException;

import bluej.extensions.BConstructor;
import bluej.extensions.BObject;
import bluej.extensions.InvocationArgumentException;
import bluej.extensions.InvocationErrorException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RConstructorImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RConstructorImpl extends java.rmi.server.UnicastRemoteObject
    implements RConstructor
{
    private BConstructor bConstructor;

    /**
     * @return
     */
    public Class[] getParameterTypes()
        throws RemoteException
    {
        return bConstructor.getParameterTypes();
    }

    /**
     * @param parameter
     * @return
     */
    public boolean matches(Class[] parameter)
        throws RemoteException
    {
        return bConstructor.matches(parameter);
    }

    /**
     * @param initargs
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws InvocationArgumentException
     * @throws InvocationErrorException
     */
    public RObject newInstance(Object[] initargs)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, InvocationArgumentException,
        InvocationErrorException
    {
        BObject bObject = bConstructor.newInstance(initargs);
        RObject rObject = WrapperPool.instance().getWrapper(bObject);
        return rObject;
    }

    /**
     * @throws RemoteException
     */
    public RConstructorImpl()
        throws RemoteException
    {
        super();
    }

    public RConstructorImpl(BConstructor bConstructor)
        throws RemoteException
    {
        this.bConstructor = bConstructor;
        if (bConstructor == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    public String getToString()
        throws RemoteException
    {
        return bConstructor.toString();
    }
}