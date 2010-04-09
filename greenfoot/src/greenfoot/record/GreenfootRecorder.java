/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010 Poul Henriksen and Michael Kolling 
 
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

package greenfoot.record;

import java.rmi.RemoteException;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

import greenfoot.Actor;
import greenfoot.ObjectTracker;
import greenfoot.World;

public class GreenfootRecorder
{
    private IdentityHashMap<Object, String> objectNames;
    private LinkedList<String> code;
    private World world;
    private Actor currentlyDraggedActor;
    
    public GreenfootRecorder()
    {
        objectNames = new IdentityHashMap<Object, String>();
        code = new LinkedList<String>();
    }

    public void createActor(Class<?> theClass, Object actor, String[] args)
    {
        currentlyDraggedActor = null;
        try {
            String name = ObjectTracker.getRObject(actor).getInstanceName();
            objectNames.put(actor, name);
            
            code.add(theClass.getCanonicalName() + " " + name + " = new " + theClass.getCanonicalName() + "(" + withCommas(args) + ");");
        }
        catch (Exception e) {
            Debug.reportError("Error recording code for creating actor", e);
        }
    }

    private static String withCommas(String[] args)
    {
        StringBuffer commaArgs = new StringBuffer();
        
        for (int i = 0; i < args.length;i++) {
            commaArgs.append(args[i]);
            if (i != args.length - 1) {
                commaArgs.append(", ");
            }
        }
        return commaArgs.toString();
    }
    
    public void addActorToWorld(Actor actor, int x, int y)
    {
        currentlyDraggedActor = null;
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            //oops!
            Debug.reportError("WorldRecorder.addActorToWorld called with unknown actor");
            return;
        }
        code.add("addObject(" + actorObjectName + ", " + x + ", " + y + ");");
    }

    public void callActorMethod(Object obj, String actorName, String name, String[] args)
    {
        currentlyDraggedActor = null;
        if (world != null && world == obj) {
            // Called on the world, so don't use the world's object name before the call:
            code.add(name + "(" + withCommas(args) + ");");
        } else {
            code.add(actorName + "." + name + "(" + withCommas(args) + ");");
        }
    }

    public void callStaticMethod(String className, String name, String[] args)
    {
        currentlyDraggedActor = null;
        // No difference in syntax, so no need to replicate the code:
        callActorMethod(null, className, name, args);
    }

    public void reset(World newWorld)
    {
        world = newWorld;
        code.clear();
        objectNames.clear();
        currentlyDraggedActor = null;
    }

    public void moveActor(Actor actor, int xCell, int yCell)
    {        
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user drags them around afterwards.
            Debug.reportError("WorldRecorder.moveActor called with unknown actor (created programmatically?)");
            return;
        }
        if (actor == currentlyDraggedActor) {
            // Remove the last line, which must be the previous setLocation caused by the drag: 
            code.removeLast();
        } else {
            currentlyDraggedActor = actor;
        }
        code.add(actorObjectName + ".setLocation(" + xCell + ", " + yCell + ");");
    }

    public void removeActor(Actor obj)
    {
        currentlyDraggedActor = null;
        String actorObjectName = objectNames.get(obj);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user tries to remove them afterwards:
            Debug.reportError("WorldRecorder.removeActor called with unknown actor (created programmatically?)");
            return;
        }
        code.add("removeObject(" + actorObjectName + ");");
        objectNames.remove(obj);
    }

    public List<String> getCode()
    {
        return code;
    }


}
