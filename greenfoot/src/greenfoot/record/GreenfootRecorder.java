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
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;

import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

import greenfoot.Actor;
import greenfoot.ObjectTracker;

public class GreenfootRecorder
{
    private IdentityHashMap<Object, String> objectNames;
    private List<String> code;
    
    public GreenfootRecorder()
    {
        objectNames = new IdentityHashMap<Object, String>();
        code = new LinkedList<String>();
    }

    public void createActor(Class<?> theClass, Object actor, String[] args)
    {
        try {
            String name = ObjectTracker.getRObject(actor).getInstanceName();
            objectNames.put(actor, name);
            
            StringBuffer commaArgs = new StringBuffer();
            
            for (int i = 0; i < args.length;i++) {
                commaArgs.append(args[i]);
                if (i != args.length - 1) {
                    commaArgs.append(", ");
                }
            }
            
            code.add(theClass.getCanonicalName() + " " + name + " = new " + theClass.getCanonicalName() + "(" + commaArgs.toString() + ");");
            
            spitCode();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //generateObjectName(theClass);
    }
    
    private String generateObjectName(Class<?> theClass)
    {
        String className = theClass.getName();
        // Take the last part of the possibly-qualified name:
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1) {
            className = className.substring(lastDot + 1);
        }
        
        // It is very important that we copy the names into a new collection here.
        // The values() method on IdentityHashMap also uses identity for equality
        // which is not what we want for Strings!
        ArrayList<String> existingNames = new ArrayList<String>(objectNames.values());
        
        // Find the first name not already taken of the form classname0, classname1, etc
        for (int n = 0;;n++) {
            String candidateName = className.toLowerCase() + n;
            if (false == existingNames.contains(candidateName)) {
                return candidateName;
            }
        }
    }
    
    public void addActorToWorld(Actor actor, int x, int y)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            //oops!
            Debug.reportError("WorldRecorder.addActorToWorld called with unknown actor");
            return;
        }
        code.add("addObject(" + actorObjectName + ", " + x + ", " + y + ");");
        
        spitCode();
    }
    
    //TEMP for debugging:
    private void spitCode()
    {
        int i = code.size() - 1;
        Debug.message("#" + i + code.get(i));
    }

}
