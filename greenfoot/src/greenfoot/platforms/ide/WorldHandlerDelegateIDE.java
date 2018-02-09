/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014,2015,2016,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.platforms.ide;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.ClassStateManager;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.ImageCache;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationUIListener;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.input.InputManager;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.record.GreenfootRecorder;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


/**
 * Implementation for running in the Greenfoot IDE.
 * 
 * @author Poul Henriksen
 */
public class WorldHandlerDelegateIDE
    implements WorldHandlerDelegate, SimulationUIListener
{
    protected final Color envOpColour = new Color(152,32,32);

    private WorldHandler worldHandler;

    private GProject project;
    
    private GreenfootFrame frame;
    
    private boolean worldInitialising;
    private boolean worldInvocationError;
    private boolean missingConstructor;
    private final List<Actor> actorsToName = new ArrayList<>();

    public WorldHandlerDelegateIDE(GreenfootFrame frame,
            ClassStateManager classStateManager)
    {
        this.frame = frame;
    }

    /**
     * Clear the world from the cache.
     * @param world  World to discard
     */
    @Override
    public void discardWorld(World world)
    {        
        ImageCache.getInstance().clearImageCache();
        frame.stopWaitingForAnswer();
    }
    
    @Override
    public void setWorld(final World oldWorld, final World newWorld)
    {
        nameActors(actorsToName.toArray(new Actor[0]));
        
        worldInvocationError = false;
        //greenfootRecorder.setWorld(newWorld);
        if (oldWorld != null) {
            discardWorld(oldWorld);
        }
    }
    
    /**
     * Attach to a particular project. This should be called whenever the project
     * changes.
     */
    public void attachProject(Object project)
    {
        this.project = (GProject) project;
    }

    @Override
    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    @Override
    public void initialisingWorld()
    {
        worldInitialising = true;
        actorsToName.clear();
    }

    @Override
    public void instantiateNewWorld()
    {
        if (project == null) {
            return;
        }
        
        //greenfootRecorder.reset();
        worldInitialising = true;
        worldInvocationError = false;
        Class<? extends World> cls = getLastWorldClass();
        GClass lastWorldGClass = getLastWorldGClass();

        if (lastWorldGClass == null) {
            // Either the last instantiated world no longer exists, or there is no record
            // of a last instantiated world class. Find a world arbitrarily.
            List<Class<? extends World>> worldClasses = project.getDefaultPackage().getWorldClasses();
            if(worldClasses.isEmpty() ) {
                return;
            }

            for (Class<? extends World> wclass : worldClasses) {
                try {
                    wclass.getConstructor(new Class<?>[0]);
                    cls = wclass;
                    break;
                }
                catch (LinkageError | NoSuchMethodException e) { }
            }
            if (cls == null) {
                // Couldn't find a world with a suitable constructor
                missingConstructor = true;
                return;
            }
        }

        if (cls == null) {
            // Can occur if last instantiated world class is not compiled.
            return;
        }

        frame.updateBackgroundMessage();
        frame.beginExecution();

        final Class<? extends World> icls = cls;
        Simulation.getInstance().runLater(() -> {
            try {
                Constructor<?> cons = icls.getConstructor(new Class<?>[0]);
                WorldHandler.getInstance().clearWorldSet();
                World newWorld = (World) Simulation.newInstance(cons);
                if (! WorldHandler.getInstance().checkWorldSet()) {
                    ImageCache.getInstance().clearImageCache();
                    WorldHandler.getInstance().setWorld(newWorld, false);
                }
                project.setLastWorldClassName(icls.getName());
            }
            catch (LinkageError e) { }
            catch (NoSuchMethodException | IllegalAccessException nsme) {
                EventQueue.invokeLater(() -> {
                    missingConstructor = true;
                });
            }
            catch (InstantiationException e) {
                // abstract class; shouldn't happen
            }
            catch (InvocationTargetException ite) {
                // This can happen if a static initializer block throws a Throwable.
                // Or for other reasons.
                ite.getCause().printStackTrace();
                EventQueue.invokeLater(() -> {
                    worldInvocationError = true;
                    frame.updateBackgroundMessage();
                });
            }
            catch (Exception e) {
                System.err.println("Exception during World initialisation:");
                e.printStackTrace();
                EventQueue.invokeLater(() -> {
                    worldInvocationError = true;
                    frame.updateBackgroundMessage();
                });
            }
            EventQueue.invokeLater(() -> {
                worldInitialising = false;
                frame.endExecution();
            });
        });
    }

    /**
     * Get the last-instantiated world class if known and possible. May return null.
     */
    public GClass getLastWorldGClass()
    {
        if (project == null) {
            return null;
        }
        
        String lastWorldClass = project.getLastWorldClassName();
        if(lastWorldClass == null) {
            return null;
        }
        
        return project.getDefaultPackage().getClass(lastWorldClass);
    }
    
    /**
     * Get the last world class that was instantiated, if it can (still) be instantiated.
     * May return null.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends World> getLastWorldClass()
    {
        GClass gclass = getLastWorldGClass();
        if (gclass != null) {
            Class<? extends World> rclass = (Class<? extends World>) gclass.getJavaClass();
            if (GreenfootUtil.canBeInstantiated(rclass)) {
                return  rclass;
            }
        }

        return null;
    }

    @Override
    public InputManager getInputManager()
    {
        InputManager inputManager = new InputManager();       
        inputManager.setIdleListeners(worldHandler, null, null);
        inputManager.setMoveListeners(worldHandler, null, null);
        
        return inputManager;
    }

    @Override
    public void objectAddedToWorld(Actor object)
    {
        if (worldInitialising) {
            // This code is nasty; we look at the stack trace to see if
            // we have been called from the prepare() method of the world class.
            //
            // We do this so that when the prepare() method is called again from the
            // code, we give the first names to those objects that are created in the prepare()
            // method -- which should then be identical to the names the objects had when
            // they were first recorded.  That way we can record additional code,
            // and the names of the live objects will be the same as the names of the objects
            // when the code was initially recorded.
            //
            // I don't know if getting the stack trace is slow, but it's probably
            // still more efficient (in time and memory) than giving every actor a name.
            // Also, this code only runs in the IDE, not in the stand-alone version
            // And I've now added a check above to make sure this is only done while the 
            // world is being initialised (which is when prepare() would be called).
            StackTraceElement[] methods = Thread.currentThread().getStackTrace();

            boolean gonePastUs = false;
            GClass lastWorldGClass = getLastWorldGClass();
            if (lastWorldGClass == null) {
                return;
            }
            String lastWorldClassName = getLastWorldGClass().getName();
            
            for (StackTraceElement item : methods) {
                if (GreenfootRecorder.METHOD_NAME.equals(item.getMethodName()) &&
                        item.getClassName().equals(lastWorldClassName)) {
                    // This call gives the object a name,
                    // which will be necessary for appending operations with the object to the world's code:
                    actorsToName.add(object);
                    return;
                }

                if (gonePastUs && item.getClassName().startsWith("java.")) {
                    //We won't find any java.* classes between us and the prepare method, so if
                    //we do hit one, we know we won't find anything; this should speed things up a bit:
                    return;
                }

                gonePastUs = gonePastUs || "objectAddedToWorld".equals(item.getMethodName());
            }
        }
    }

    /**
     * This is a special method that will have a breakpoint set on it
     * by GreenfootDebugHandler to watch out for actors which should
     * be named.  Do not remove or rename without also editing that code.
     */
    private void nameActors(Actor[] actor)
    {
    }

    /**
     * Notify that the simulation has become active ("act" or "run" pressed). Any recorded interaction
     * then becomes invalid.
     */
    @Override
    public void simulationActive()
    {
    }
    
    /**
     * Is the world currently initialising?
     */
    public boolean initialising()
    {
        return worldInitialising;
    }

    /**
     * Did the last world invocation end in an error?
     */
    public boolean initialisationError()
    {
        return worldInvocationError;
    }

    /**
     * Is there a default constructor in the world subclass?
     * 
     * @return true if the world subclass does not have a default constructor
     */
    public boolean isMissingConstructor()
    {
        return missingConstructor;
    }

    /**
     * Sets a flag which indicates whether the world subclass misses a default constructor or not
     * 
     * @param missingConstructor a boolean flag, which is true if there is no default constructor
     * in the world subclass
     */
    public void setMissingConstructor(boolean missingConstructor)
    {
        this.missingConstructor = missingConstructor;
    }

    @Override
    public String ask(final String prompt, WorldCanvas worldCanvas)
    {
        // As I accidentally discovered while developing, this method
        // will go wrong if called off the simulation thread.
        // That should be fine, because Greenfoot methods should always
        // be called from the simulation thread, but it's worth an
        // explicit exception rather than getting stuck in a loop:
        if (!Simulation.getInstance().equals(Thread.currentThread()))
            throw new RuntimeException("Greenfoot.ask can only be called from the main simulation thread");
        
        // Make a new ID for the ask request:
        int askId = worldCanvas.getAskId();
        // Keeping polling the server VM until we get an answer.
        // This will block the simulation thread until we get an answer,
        // but that is the semantics of Greenfoot.ask so it's fine:
        while (true)
        {
            String answer = worldCanvas.paintRemote(true, askId, prompt);
            if (answer != null)
            {
                return answer;
            }

            try
            {
                Thread.sleep(200);
            }
            catch (InterruptedException e)
            {
            }
        }
    }
}
