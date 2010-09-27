/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.GNamedValue;
import greenfoot.core.GreenfootLauncherDebugVM;
import greenfoot.core.GreenfootMain;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;

import rmiextension.wrappers.RObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.runtime.BJMap;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.JavaUtils;

/**
 * Class that can be used to get the remote version of an object and vice versa.
 * 
 * @author Poul Henriksen
 */
public class ObjectTracker
{
    private static HashMap<Object,RObject> cachedObjects = new HashMap<Object,RObject>();

    /**
     * Gets the remote reference to an object. If there is currently no remote reference,
     * one is created.
     *  
     * @throws RemoteException  if an exception occurred in the remote VM
     */
    public static RObject getRObject(Object obj) throws RemoteException
    {
        synchronized (cachedObjects) {
            RObject rObject = cachedObjects.get(obj);
            if (rObject != null) {
                return rObject;
            }
            
            GreenfootLauncherDebugVM.getInstance().setTransportField(obj);
            RObject rObj = GreenfootMain.getInstance().getProject().getRProject().getRemoteObject();
            
            if (rObj != null) {
                cachedObjects.put(obj,rObj);
            }
            
            return rObj;
        }
    }    

    /**
     * Get the complete set of registered objects as a ValueCollection.
     */
    public static ValueCollection getObjects()
    {
        return new ValueCollection()
        {
            BJMap<String,Object> map;
            String [] names;
            
            private void initNames()
            {
                if (names == null) {
                    map = ExecServer.getObjectMap();
                    synchronized (map) {
                        Object [] keys = map.getKeys();
                        names = new String[keys.length];
                        System.arraycopy(keys, 0, names, 0, keys.length);
                    }
                }
            }
            
            @Override
            public GNamedValue getNamedValue(String name)
            {
                initNames();
                synchronized (map) {
                    Object o = map.get(name);
                    if (o != null) {
                        JavaType type = JavaUtils.genTypeFromClass(o.getClass());
                        return new GNamedValue(name, type);
                    }
                    return new GNamedValue(name, JavaUtils.genTypeFromClass(Object.class));
                }
            }
            
            @Override
            public Iterator<? extends NamedValue> getValueIterator()
            {
                initNames();
                return new Iterator<GNamedValue>() {
                    int index = 0;
                    
                    @Override
                    public boolean hasNext()
                    {
                        return index < names.length;
                    }
                    @Override
                    public GNamedValue next()
                    {
                        return getNamedValue(names[index++]);
                    }
                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
    
    /**
     * Get the local object corresponding to a remote object.
     */
    public static Object getRealObject(RObject remoteObj)
    {
        try {
            return ExecServer.getObject(remoteObj.getInstanceName());
        }
        catch (RemoteException e) {
           Debug.reportError("Unexpected exception getting remote object name", e);
        }
        return null;
    }

    /**
     * Clear the cache of remote objects.
     */
    public static void clearRObjectCache()
    {
        synchronized (cachedObjects) {
            cachedObjects.clear();
        }
    }
    
}
