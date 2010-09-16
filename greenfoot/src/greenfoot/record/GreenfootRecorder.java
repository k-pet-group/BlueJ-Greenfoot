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

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import rmiextension.wrappers.RObject;

import bluej.debugger.gentype.JavaType;
import bluej.utility.Debug;

import greenfoot.Actor;
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.actions.SaveWorldAction;

public class GreenfootRecorder
{
    private IdentityHashMap<Object, String> objectNames;
    private LinkedList<String> code;
    private World world;
    private SaveWorldAction action;
    
    public static final String METHOD_NAME = "prepare";
    
    public GreenfootRecorder(SaveWorldAction action)
    {
        objectNames = new IdentityHashMap<Object, String>();
        code = new LinkedList<String>();
        this.action = action;
        this.action.setRecordingValid(false);
    }

    public void createActor(Object actor, String[] args, JavaType[] argTypes)
    {
        Class<?> theClass = actor.getClass();
        String name = nameActor(actor);
        if (name != null) {
            code.add(theClass.getCanonicalName() + " " + name + " = new " + theClass.getCanonicalName() + "(" + withCommas(args, argTypes) + ");");
        }
    }
    
    // Called when the prepare method is replayed to indicate that the actor's name should be recorded
    // ready for its use in further manipulations:
    public String nameActor(Object actor)
    {
        try {
            RObject rObject = ObjectTracker.getRObject(actor);
            if (rObject != null) {
                String name = rObject.getInstanceName();
                objectNames.put(actor, name);
                return name;
            } else {
                return null;
            }
        }
        catch (Exception e) {
            Debug.reportError("Error naming actor", e);
            return null;
        }
    }
    
    private static String withCommas(String[] args, JavaType[] paramArgs)
    {
        if (args == null)
            return "";
        
        StringBuffer commaArgs = new StringBuffer();
        
        for (int i = 0; i < args.length;i++) {
        	String arg = args[i].trim();
        	if (arg.startsWith("{") && arg.endsWith("}")) {
        		arg = "new " + paramArgs[i] + " " + arg;
        	}
            commaArgs.append(arg);
            if (i != args.length - 1) {
                commaArgs.append(", ");
            }
        }
        return commaArgs.toString();
    }
    
    public void addActorToWorld(Actor actor, int x, int y)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            //An actor that we don't know about is being added to the world: ignore
            return;
        }
        code.add("addObject(" + actorObjectName + ", " + x + ", " + y + ");");
    }

    public void callActorMethod(Object obj, String actorName, String name, String[] args, JavaType[] argTypes)
    {
        if (null == objectNames.get(obj) && obj != world) {
            //Method is being called on an actor we don't know about: ignore
            return;
        }
        if (world != null && world == obj) {
            // Called on the world, so don't use the world's object name before the call:
            code.add(name + "(" + withCommas(args, argTypes) + ");");
        } else {
            code.add(actorName + "." + name + "(" + withCommas(args, argTypes) + ");");
        }
    }

    public void callStaticMethod(String className, String name, String[] args, JavaType[] argTypes)
    {
        // No difference in syntax, so no need to replicate the code:
        callActorMethod(null, className, name, args, argTypes);
    }
    
    public void clearCode(boolean simulationStarted)
    {
        code.clear();
        if (simulationStarted) {
            objectNames.clear();
            action.setRecordingValid(false);
        }
    }

    public void reset(World newWorld)
    {
        world = newWorld;
        objectNames.clear();
        clearCode(false);
        action.setRecordingValid(true);
    }

    public void moveActor(Actor actor, int xCell, int yCell)
    {        
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user drags them around afterwards.
            // We'll just have to ignore it
            return;
        }
        code.add(actorObjectName + ".setLocation(" + xCell + ", " + yCell + ");");
    }

    public void removeActor(Actor obj)
    {
        String actorObjectName = objectNames.get(obj);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user tries to remove them afterwards.
         // We'll just have to ignore it
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
