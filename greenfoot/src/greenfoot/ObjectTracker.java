package greenfoot;

import java.rmi.RemoteException;

import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;

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
        if(obj instanceof Actor) {
            return Actor.getRObject(obj);
        } else  if( obj instanceof World) {
            return World.getRObject(obj);
        } else {
            Debug.reportError("Could not get remote version of object: " + obj, new Exception());
            return null;
        }
        
    }
    
    public static void forgetRObject(Object obj)
    {
        if (obj instanceof World) {
            World.forgetRObject(obj);
        }
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
