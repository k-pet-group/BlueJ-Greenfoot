package greenfoot;

import java.rmi.RemoteException;

import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.runtime.ExecServer;

/**
 * Class that can be used to get the remote version of an object and vice versa.
 * 
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class ObjectTracker
{
    /**
     * Get the RObject version of the obj.
     * 
     * @see greenfoot.ObjectTransporter
     * 
     * @param obj The class of obj must be in the project and be a subclass of greenfoot.ObjectTransporter
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws RemoteException
     * @throws ClassNotFoundException
     */
    public static RObject getRObject(Object obj) throws ProjectNotOpenException, PackageNotFoundException, RemoteException, ClassNotFoundException
    {
        return GreenfootObject.getRObject(obj);
    }
    
    public static Object getRealObject(RObject remoteObj)
    {
        try {
            return ExecServer.getObject(remoteObj.getInstanceName());
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
