package rmiextension.wrappers;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.swing.JPopupMenu;

import rmiextension.MenuSerializer;
import bluej.extensions.BClass;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen
 * @version $Id: RObjectImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RObjectImpl extends UnicastRemoteObject
    implements RObject
{
    /**
     * @throws RemoteException
     */
    protected RObjectImpl()
        throws RemoteException
    {
        super();
    }

    public RObjectImpl(BObject bObject)
        throws RemoteException
    {
        this.bObject = bObject;
        if (bObject == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    BObject bObject;

    /**
     * @param instanceName
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void addToBench(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        bObject.addToBench(instanceName);
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RClass getRClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        BClass wrapped = bObject.getBClass();
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     */
    public String getInstanceName()
        throws RemoteException
    {
        return bObject.getInstanceName();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        BPackage wrapped = bObject.getPackage();
        RPackage wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void removeFromBench()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        bObject.removeFromBench();
    }

    public MenuSerializer getMenu()
        throws RemoteException
    {
        JPopupMenu menu = (JPopupMenu) bObject.getMenu();
        return new MenuSerializer(menu);
    }

}