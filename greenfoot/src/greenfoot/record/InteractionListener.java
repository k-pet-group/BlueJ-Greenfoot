/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011,2014  Poul Henriksen and Michael Kolling 
 
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

import java.lang.reflect.Method;

import greenfoot.Actor;
import bluej.debugger.gentype.JavaType;
import bluej.views.CallableView;

/**
 * An interface currently used to provide hooks for recording the save-the-world.
 */
public interface InteractionListener
{
    /**
     * An interactive method/constructor call has begun execution.
     */
    public void beginCallExecution(CallableView callableView);
    
    /**
     * Notify that an actor was constructed interactively by the user.
     * @param actor   The actor object
     * @param String[] args   The constructor arguments (as Java expressions)
     */
    public void createdActor(Object actor, String[] args, JavaType[] argTypes);

    /**
     * A world was constructed interactively. This would normally, but not necessarily,
     * mean that the constructed world is now the current world.
     * 
     * @param world  The newly constructed world
     */
    public void worldConstructed(Object world);
    
    /**
     * A method was called and successfully returned (no exception was
     * thrown).
     * 
     * @param obj  The target of the method call - will be null for a static method
     * @param targetName   The name of the target object or class 
     * @param methodName   The name of the called method
     * @param args       The method arguments (as java expressions)
     * @param argTypes   The argument types of the method. For a varargs method the last type will be an array.
     */
    public void methodCall(Object obj, String targetName, Method method, String[] args, JavaType[] argTypes);

    public void removedActor(Actor obj);

    public void objectAddedToWorld(Actor object);
}
