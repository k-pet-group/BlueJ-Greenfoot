package rmiextension;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.logging.Logger;

import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.runtime.ExecServer;

/**
 * The objecttracker keeps track of the identity between objects in the Debug-VM
 * and BlueJ-VM Has both a representation in greenfoot and bluej VM.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ObjectTracker.java 3234 2004-12-12 23:59:56Z davmac $
 */
public class ObjectTracker
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private static ObjectTracker instance;
    private RObject remoteObjectTracker;
    public Object transportField;
    public Hashtable cachedObjects = new Hashtable();
    public static final String INSTANCE_NAME = "objectTracker";

    public ObjectTracker()
    {
        instance = this;
    }

    /**
     * Gets the singleton
     * 
     * @return
     */
    public static ObjectTracker instance()
    {
        return instance;
    }

    /**
     * Ensures that the ObejctTracker in the Debug-VM has a ref. to the BlueJ-VM
     * instance
     * 
     * @param remoteObjectTracker
     */
    private void setRemote()
    {
        if (remoteObjectTracker == null) {
            try {
                remoteObjectTracker = BlueJRMIClient.instance().getPackage().getObject(ObjectTracker.INSTANCE_NAME);
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
    }

    /**
     * Gets the BlueJ-VM reference to the obj
     * 
     * @param obj
     * @return
     */
    public synchronized RObject getRObject(Object obj)
    {
        setRemote();

        RObject rObject = (RObject) cachedObjects.get(obj);
        if (rObject != null) {
            return rObject;
        }
        transportField = obj;
        try {
            rObject = remoteObjectTracker.getRClass().getField("transportField").getValue(remoteObjectTracker);
            cachedObjects.put(obj, rObject);
            return rObject;
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    // no longer needed method
//    public JPopupMenu getJPopupMenu(Object obj)
//    {
//        try {
//            return getRObject(obj).getMenu().getPopupMenu();
//        }
//        catch (RemoteException e1) {
//            e1.printStackTrace();
//        }
//        return null;
//    }

    /**
     * @param remoteObj
     * @return
     */
    public Object getRealObject(RObject remoteObj)
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