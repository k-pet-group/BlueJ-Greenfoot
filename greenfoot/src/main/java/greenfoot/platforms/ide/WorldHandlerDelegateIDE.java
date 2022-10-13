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

import bluej.runtime.ExecServer;
import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.ImageCache;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.vmcomm.VMCommsSimulation;
import greenfoot.vmcomm.VMCommsSimulation.PaintWhen;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.record.GreenfootRecorder;
import greenfoot.util.GreenfootUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Color;
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
    implements WorldHandlerDelegate
{
    protected final Color envOpColour = new Color(152,32,32);
    
    private final VMCommsSimulation vmCommsSimulation;
    
    private boolean worldInitialising;
    private final List<Actor> actorsToName = new ArrayList<>();
    private String mostRecentlyInstantiatedWorldClassName;

    public WorldHandlerDelegateIDE(VMCommsSimulation vmCommsSimulation)
    {
        this.vmCommsSimulation = vmCommsSimulation;
    }

    /**
     * Clear the world from the cache.
     * @param world  World to discard
     */
    @Override
    public void discardWorld(World world)
    {        
        ImageCache.getInstance().clearImageCache();
        vmCommsSimulation.setWorld(null);
    }
    
    @Override
    public void setWorld(final World oldWorld, final World newWorld)
    {
        nameActors(actorsToName.toArray(new Actor[0]));
        
        //greenfootRecorder.setWorld(newWorld);
        if (oldWorld != null) {
            discardWorld(oldWorld);
        }
        vmCommsSimulation.setWorld(newWorld);
    }

    @Override
    public void initialisingWorld(String className)
    {
        worldInitialising = true;
        mostRecentlyInstantiatedWorldClassName = className;
        actorsToName.clear();
    }

    @Override
    public void finishedInitialisingWorld()
    {
        worldInitialising = false;
    }

    @Override
    @OnThread(Tag.Simulation)
    public void paint(World drawWorld, boolean forcePaint)
    {
        vmCommsSimulation.paintRemote(forcePaint ? PaintWhen.FORCE : PaintWhen.IF_DUE);
    }

    @Override
    public void notifyStoppedWithError()
    {
        vmCommsSimulation.notifyStoppedWithError();
    }

    @Override
    public void instantiateNewWorld(String className, Runnable runIfError)
    {
        // If not-null, store it as the most recent, ready to be used by getLastWorldClass
        if (className != null)
        {
            mostRecentlyInstantiatedWorldClassName = className;
        }
        
        //greenfootRecorder.reset();
        worldInitialising = true;
        Class<? extends World> cls = getLastWorldClass();

        if (cls == null)
        {
            // Can occur if last instantiated world class is not compiled,
            // or if the specified world class is not found, or if no world
            // class name has ever been specified.
            runIfError.run();
            return;
        }

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
            }
            catch (LinkageError | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
                // InstantiationException means abstract class; shouldn't happen
                runIfError.run();
            }
            catch (InvocationTargetException ite) {
                // This can happen if a static initializer block throws a Throwable.
                // Or for other reasons.
                ite.getCause().printStackTrace();
                runIfError.run();
            }
            catch (Exception e) {
                System.err.println("Exception during World initialisation:");
                e.printStackTrace();
                runIfError.run();
            }
            worldInitialising = false;
        });
    }

    /**
     * Get the last world class that was instantiated, if it can (still) be instantiated.
     * May return null.
     */
    private Class<? extends World> getLastWorldClass()
    {
        if (mostRecentlyInstantiatedWorldClassName != null)
        {
            try
            {
                // it is important that we use the right classloader
                ClassLoader classLdr = ExecServer.getCurrentClassLoader();
                Class<?> cls = Class.forName(mostRecentlyInstantiatedWorldClassName, false, classLdr);
                if (GreenfootUtil.canBeInstantiated(cls))
                {
                    return cls.asSubclass(World.class);
                }
            }
            catch (java.lang.ClassNotFoundException cnfe)
            {
                // couldn't load: that's ok, we return null
                // cnfe.printStackTrace();
            }
            catch (ClassCastException cce)
            {
                // The class is (no longer) a world class: ok, ignore
            }
            catch (LinkageError e)
            {
                // TODO log this properly? It can happen for various reasons, not
                // necessarily a real error.
                e.printStackTrace();
            }
        }
        return null;
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
            String lastWorldClassName = mostRecentlyInstantiatedWorldClassName;
            if (lastWorldClassName == null) {
                return;
            }
            
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
     * Is the world currently initialising?
     */
    public boolean initialising()
    {
        return worldInitialising;
    }

    @Override
    public String ask(final String prompt)
    {
        // As I accidentally discovered while developing, this method
        // will go wrong if called off the simulation thread.
        // That should be fine, because Greenfoot methods should always
        // be called from the simulation thread, but it's worth an
        // explicit exception rather than getting stuck in a loop:
        if (!Simulation.getInstance().equals(Thread.currentThread()))
            throw new RuntimeException("Greenfoot.ask can only be called from the main simulation thread");
        
        // Make a new ID for the ask request:
        int askId = vmCommsSimulation.getAskId();
        
        // This will block the simulation thread until we get an answer,
        // but that is the semantics of Greenfoot.ask so it's fine:
        return vmCommsSimulation.doAsk(askId, prompt);
    }
}
