/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2016  Poul Henriksen and Michael Kolling
 
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

import bluej.utility.Utility;
import greenfoot.core.GNamedValue;
import greenfoot.core.GreenfootLauncherDebugVM;
import greenfoot.core.GreenfootMain;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

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
    private static Map<Object, String> cachedObjects = new IdentityHashMap<>();

    /**
     * Gets the remote reference to an object. If there is currently no remote reference,
     * one is created.
     *  
     * @throws RemoteException  if an exception occurred in the remote VM
     */
    public static String getRObjectName(Object obj) throws RemoteException
    {
        synchronized (cachedObjects) {
            String rObjectName = cachedObjects.get(obj);
            if (rObjectName != null) {
                return rObjectName;
            }
            
            GreenfootLauncherDebugVM.getInstance().setTransportField(obj);
            rObjectName = GreenfootMain.getInstance().getProject().getRProject().getRemoteObjectName();
            
            if (rObjectName != null) {
                cachedObjects.put(obj, rObjectName);
            }
            
            return rObjectName;
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
     * Clear the cache of remote objects.
     */
    public static void clearRObjectCache()
    {
        synchronized (cachedObjects) {
            cachedObjects.clear();
        }
    }

    public static List<String> getRObjectNames(List<Object> actors) throws RemoteException
    {
        List<Object> stillToName = new ArrayList<>();
        synchronized (cachedObjects) {
            for (Object actor : actors)
            {
                String rObjectName = cachedObjects.get(actor);
                if (rObjectName == null)
                {
                    stillToName.add(actor);
                }
            }

            GreenfootLauncherDebugVM.getInstance().setTransportField(stillToName.toArray());
            List<String> rObj = GreenfootMain.getInstance().getProject().getRProject().getRemoteObjectNames();

            for (int i = 0; i < stillToName.size();i++)
            {
                if (rObj.get(i) != null)
                    cachedObjects.put(stillToName.get(i), rObj.get(i));
            }

            return Utility.mapList(actors, cachedObjects::get);
        }
    }
}
