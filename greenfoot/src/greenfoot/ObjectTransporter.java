package greenfoot;

import java.rmi.RemoteException;
import java.util.Hashtable;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RObject;
import rmiextension.wrappers.RPackage;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * In order to get the RObject representation of an Object, we need to use a
 * class that exists in the BlueJ project, because the extensions requires this
 * when we want to get the BClass representation.
 * 
 * <p>
 * 
 * To get the RObject class of an Object we need some way to get a reference to
 * the Object in Greenfoot VM from the BlueJ VM. This can be done by putting the
 * Object into a field of another Object for which we already have the RObject.
 * To get this first intial object (actually a class) we use this
 * TransporterClass. An additional problem is that the class that we use to
 * store the object reference in, must be in the BlueJ project in order for it
 * too work with the BlueJ extensions.
 * 
 * <br>
 * 
 * The first the ObjectTranporter is used, we want to get a RClass version of
 * the class that the Object is an instance of. For this to work, the class must
 * be in the project and must be a subclass of ObjectTransporter.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public abstract class ObjectTransporter
{
    /** Remote version of this class */
    private static RClass remoteObjectTracker;
    
    /** The object we want to get a remote version of */
    private static  Object transportField;
    
    /** Lock to ensure that we only have one remoteObjectTracker */
    private static  Object lock = new Object();
    //TODO The cached objects should be cleared at recompile.
    private  static Hashtable cachedObjects = new Hashtable();
    
    /**
     * Gets the remote reference to the obj.
     * 
     * @throws ClassNotFoundException 
     * @throws RemoteException 
     * @throws PackageNotFoundException 
     * @throws ProjectNotOpenException 
     * 
     */
    static RObject getRObject(Object obj) throws ProjectNotOpenException, PackageNotFoundException, RemoteException, ClassNotFoundException
    {
        synchronized (lock) {
            RObject rObject = (RObject) cachedObjects.get(obj);
            if (rObject != null) {
                return rObject;
            }
            transportField = obj;
            rObject = getRemoteClass(obj).getField("transportField").getValue(null);
            cachedObjects.put(obj, rObject);
            return rObject;
        }
    }
    

    /**
     * This method ensures that we have the remote (RClass) representation of
     * this class.
     * 
     * @param obj
     * 
     */
    static private RClass getRemoteClass(Object obj)
    {
        if (remoteObjectTracker == null) {
            try {
                if( ! (obj instanceof ObjectTransporter)) {
                    System.out.println("Tracker is NOT a Actor: " + obj.getClass());
                }
                RPackage pkg = BlueJRMIClient.instance().getPackage();
                remoteObjectTracker = pkg.getRClass(obj.getClass().getName());                
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
            catch (PackageNotFoundException e) {
                e.printStackTrace();
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return remoteObjectTracker;
    }
}
