/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import greenfoot.core.GreenfootMain;

import java.rmi.RemoteException;
import java.util.Hashtable;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RField;
import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;

/**
 * Class that can be used to get the remote version of an object and vice versa.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class ObjectTracker
{
    /** Lock to ensure that we only have one remoteObjectTracker */
    private static  Object lock = new Object();
    //TODO The cached objects should be cleared at recompile.
    private  static Hashtable<Object,RObject> cachedObjects = new Hashtable<Object,RObject>();

    /**
     * Gets the remote reference to the obj.
     * <p>
     *  
     *  
     * @throws ClassNotFoundException 
     * @throws RemoteException 
     * @throws PackageNotFoundException 
     * @throws ProjectNotOpenException 
     * 
     */
    public static RObject getRObject(Object obj) throws ProjectNotOpenException, PackageNotFoundException, RemoteException, ClassNotFoundException
    {
        synchronized (lock) {
            RObject rObject = cachedObjects.get(obj);
            if (rObject != null) {
                return rObject;
            }
            
            World.setTransportField(obj);
            RClass remoteObjectTracker = null;
            //This can be the same for world and actor apart from above lines.
            if(obj instanceof Actor) {
                Actor.setTransportField(obj);
                remoteObjectTracker = (RClass) ((Actor) obj).getRemoteObjectTracker();
            } else  if( obj instanceof World) {
                World.setTransportField(obj);
                remoteObjectTracker = (RClass) ((World) obj).getRemoteObjectTracker();
            } else {
                Debug.reportError("Could not get remote version of object: " + obj, new Exception());
                return null;
            }
            

            RClass rClass = getRemoteClass(obj, remoteObjectTracker);
            
            RField rField = rClass.getField("transportField");
            rObject = rField.getValue(null);
            cachedObjects.put(obj, rObject);
            return rObject;
        }
    }    

    /**
     * This method ensures that we have the remote (RClass) representation of
     * this class.
     * <p>
     *  
     * @param obj
     * @param remoteObjectTracker 
     * 
     */
    static private RClass getRemoteClass(Object obj, RClass remoteObjectTracker)
    {
        if (remoteObjectTracker == null) {
            String rclassName = obj.getClass().getName();
            remoteObjectTracker = GreenfootMain.getInstance().getProject().getRClass(rclassName);
        }
        return remoteObjectTracker;
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

    
    /**
     * "Forget" about a remote object reference. This is needed to avoid memory
     * leaks (worlds are otherwise never forgotten).
     * @param obj  The object to forget
     */
    public static void forgetRObject(Object obj)
    {
        synchronized (lock) {
            RObject rObject = cachedObjects.remove(obj);
            if (rObject != null) {
                try {
                    rObject.removeFromBench();
                }
                catch (RemoteException re) {
                    throw new Error(re);
                }
                catch (ProjectNotOpenException pnoe) {
                    // shouldn't happen
                }
                catch (PackageNotFoundException pnfe) {
                    // shouldn't happen
                }
            }
        }
    }
    
    /**
     * Clear the cache of remote objects.
     */
    public static void clearRObjectCache()
    {
        synchronized (lock) {
            cachedObjects.clear();
        }
    }
    
}
