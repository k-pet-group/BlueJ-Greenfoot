/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010,2011,2014,2015,2016,2018 Poul Henriksen and Michael Kolling
 
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

import bluej.debugger.DebuggerObject;
import bluej.editor.Editor;
import bluej.utility.javafx.FXPlatformFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import bluej.Config;
import bluej.debugger.gentype.JavaType;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.NormalMethodElement;
import bluej.stride.framedjava.elements.VarElement;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Builder for code sequences representing a recording of what the user has
 * done interactively to the world.
 */
public class GreenfootRecorder
{
    /** A map of known objects to their name as it appears in the code */
    private final HashMap<DebuggerObject, String> objectNames;
    private final ArrayList<CodeElement> code;
    private DebuggerObject world;
    
    public static final String METHOD_ACCESS = "private";
    public static final String METHOD_RETURN = "void";
    public static final String METHOD_NAME = "prepare";
    private String lastWorldClass;
    
    // Only valid if no run has occurred in the mean time. 
    private boolean validToSave = false;

    /**
     * Construct a new GreenfootRecorder instance.
     */
    public GreenfootRecorder()
    {
        objectNames = new HashMap<>();
        code = new ArrayList<>();
    }

    /**
     * Record the interactive construction of an actor object.
     * @param actor   The newly constructed actor
     * @param args     The arguments supplied to the actor's constructor, as Java expresssions
     * @param paramTypes  The parameter types of the called constructor
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    public synchronized void createActor(DebuggerObject actor, String[] args, JavaType[] paramTypes)
    {
        String className = actor.getGenType().toString(true);
        String name = nameActor(actor);
        if (name != null) {
            code.add(new VarElement(null, className, name,  "new " + className
                    + "(" + withCommas(args, paramTypes, false) + ")"));
        }
    }
    
    /**
     * Called when the prepare method is replayed to indicate that the actor's name should be recorded.
     * Returns the name assigned to the actor (or null on failure).
     * 
     * This is called from the debugger thread.
     */
    private synchronized String nameActor(DebuggerObject actor)
    {
        if (objectNames.containsKey(actor))
        {
            return objectNames.get(actor);
        }

        String root = actor.getClassName().replace("." , "").replace("$", "");
        root = root.substring(0, 1).toLowerCase() + root.substring(1);
        String name = root;
        // The first item has no number (e.g. crab), the next one is crab2, then crab3, etc.  There is no crab1.
        // General note: it's important that this naming mechanism remains consistent across Greenfoot versions,
        // because we rely on the names being the same sequence to do save-the-world updates, i.e. if someone
        // has got crab, crab2, crab3, and we save the world again, it's only by naming the objects using
        // the same algorithm that we know the next one should be crab4 (the alternative of introspecting the
        // prepare method to see the existing names would be too intricate -- this way is simple best-effort).
        for (int i = 2; objectNames.values().contains(name); i++)
        {
            name = root + i;
        }
        objectNames.put(actor, name);
        return name;
    }

    /**
     * Names all the actors in the list, in order (first to last)
     */
    public synchronized void nameActors(List<DebuggerObject> actors)
    {
        for (DebuggerObject actor : actors)
        {
            nameActor(actor);
        }
    }
    
    /**
     * Insert commas and other necessary syntax into an argument list
     * @param args      The arguments to a method or constructor call (as Java expressions)
     * @param paramTypes  The parameter types of the method/constructor
     * @return  The arguments as a comma-separated list
     */
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
    private static String withCommas(String[] args, JavaType[] paramTypes, boolean isVarArgs)
    {
        if (args == null) {
            return "";
        }
        
        StringBuffer commaArgs = new StringBuffer();
        
        for (int i = 0; i < args.length;i++) {
            String arg = args[i].trim();
            if (arg.startsWith("{") && arg.endsWith("}")) {
                String paramTypeName;
                if (isVarArgs && i >= paramTypes.length - 1) {
                    paramTypeName = paramTypes[paramTypes.length - 1].toString();
                    paramTypeName = paramTypeName.substring(0,paramTypeName.length()-2);
                }
                else {
                    paramTypeName = paramTypes[i].toString();
                }
                arg = "new " + paramTypeName + " " + arg;
            }
            commaArgs.append(arg);
            if (i != args.length - 1) {
                commaArgs.append(", ");
            }
        }
        return commaArgs.toString();
    }
    
    /**
     * An actor was interactively added to the world: record the interaction
     * @param actor   The added actor
     * @param x       The actor's x position
     * @param y       The actor's y position
     */
    @OnThread(Tag.Any)
    public synchronized void addActorToWorld(DebuggerObject actor, int x, int y)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            //An actor that we don't know about is being added to the world: ignore
            return;
        }
        code.add(callElement("addObject(" + actorObjectName + "," + String.valueOf(x) + "," + String.valueOf(y) + ")"));
    }

    /**
     * Record an interactive method call on object (actor or world). Called after the method
     * successfully returns.
     * 
     * @param obj        The object on which the method was invoked
     * @param actorName  The assigned object name
     * @param method     The method being called
     * @param args       The arguments to the method, as Java expressions
     * @param paramTypes  The parameter types of the method
     */
    public synchronized void callActorOrWorldMethod(DebuggerObject obj, Method method,
                                                    String[] args, JavaType[] paramTypes)
    {
        if (obj != null && !objectNames.containsKey(obj) && !obj.equals(world)) {
            //Method is being called on an actor we don't know about: ignore
            return;
        }
        String name;
        if (world != null && world.equals(obj)) {
            // Called on the world, so don't use the world's object name before the call:
            name = method.getName();
        }
        else {
            name = objectNames.get(obj) + "." + method.getName();
        }
        code.add(callElement(name + "(" + withCommas(args, paramTypes, method.isVarArgs()) + ")"));
    }

    /**
     * Record an interactive static method call. Called after the method
     * successfully returns.
     * 
     * @param className  The name of the class to which the called method belongs
     * @param method     The method
     * @param args       The arguments to the method, as a
     * @param argTypes
     */
    public void callStaticMethod(String className, Method method, String[] args, JavaType[] argTypes)
    {
        code.add(callElement(className + "." + method.getName() + 
                "(" + withCommas(args, argTypes, method.isVarArgs()) + ")"));
    }
    
    /**
     * Notify the recorder that it should clear its recording.
     * 
     * @param clearObjectNames Whether to clear the object names
     */
    public synchronized void clearCode(boolean clearObjectNames)
    {
        code.clear();
        if (clearObjectNames)
        {
            objectNames.clear();
        }
    }

    /**
     * Notify the recorder that a new world has become the current world.
     */
    public synchronized void setWorld(DebuggerObject newWorld)
    {
        world = newWorld;
        lastWorldClass = newWorld.getClassName();
    }

    /**
     * Record a dragged actor interaction. This is currently called from the simulation
     * thread (i.e. with the world locked).
     */
    public synchronized void moveActor(DebuggerObject actor, int xCell, int yCell)
    {
        String actorObjectName = objectNames.get(actor);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user drags them around afterwards.
            // We'll just have to ignore it
            return;
        }
        code.add(callElement(actorObjectName + ".setLocation(" + String.valueOf(xCell) + "," + String.valueOf(yCell) + ")"));
    }

    /**
     * Record a remove actor interaction.
     */
    public void removeActor(DebuggerObject obj)
    {
        String actorObjectName = objectNames.get(obj);
        if (null == actorObjectName) {
            // This could happen with programmatically generated actors (e.g. in a World's method)
            // if the user tries to remove them afterwards.
            // We'll just have to ignore it
            return;
        }
        code.add(callElement("removeObject(" + actorObjectName + ")"));
        objectNames.remove(obj);
    }

    /**
     * Retrieve the code elements representing the interactions recorded up to this point.
     */
    public synchronized List<CodeElement> getCode()
    {
        return new LinkedList<CodeElement>(code);
    }

    public NormalMethodElement getPrepareMethod()
    {
        StringBuffer documentation = new StringBuffer();
        documentation.append(Config.getString("record.method.comment1")).append("\n");
        documentation.append(Config.getString("record.method.comment2"));
        
        return new NormalMethodElement(METHOD_ACCESS, METHOD_RETURN, METHOD_NAME,
                null, code, documentation.toString());
    }

    public CallElement getPrepareMethodCall()
    {
        return callElement(METHOD_NAME + "()");
    }
    
    private CallElement callElement(String content)
    {
        return new CallElement(content, content);
    }

    /**
     * Write the recorded code to the world class. 
     * @param getEditor A function that takes a world class name and returns an editor
     * @return True if world code successfully saved, false if it could not because save the world isn't valid.
     */
    @OnThread(Tag.FXPlatform)
    public boolean writeCode(FXPlatformFunction<String, Editor> getEditor)
    {
        if (!validToSave)
        {
            return false;
        }
        
        NormalMethodElement method = getPrepareMethod();
        CallElement methodCall = getPrepareMethodCall();

        Editor editor = getEditor.apply(lastWorldClass);
        editor.insertMethodCallInConstructor(lastWorldClass, methodCall, inserted -> {});
        editor.insertAppendMethod(method, inserted -> {
            if (inserted)
            {
                editor.setEditorVisible(true, false);
            }
        });
        // Now that we've inserted the code, we must reset the recorder,
        // so that if the user saves the world again before re-compiling,
        // it doesn't insert the same code twice.  If the user scrubs our method
        // and saves the world before re-compiling this will then go wrong
        // (by inserting code depending on objects no longer there) but that
        // seems less likely:
        clearCode(false);
        
        return true;
    }

    /**
     * Mark the recording as not valid (because Act/Run has been used)
     */
    public void invalidateRecording()
    {
        validToSave = false;
    }

    /**
     * Mark the recording as valid, because the user has manually created a World, 
     * or triggered a reset (including by recompiling or focusing main window).
     */
    public void recordingValid()
    {
        validToSave = true;
    }
}
