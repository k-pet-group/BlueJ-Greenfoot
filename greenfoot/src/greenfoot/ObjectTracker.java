package greenfoot;

import java.rmi.RemoteException;

import rmiextension.wrappers.RObject;
import bluej.runtime.ExecServer;

/**
 * Class that can be used to get the remote version of an object and vice versa.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class ObjectTracker
{
    public static RObject getRObject(Object obj)
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
