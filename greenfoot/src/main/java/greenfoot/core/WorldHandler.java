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
package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.Simulation.SimulationRunnable;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.input.KeyboardManager;
import greenfoot.gui.input.mouse.MousePollingManager;
import greenfoot.gui.input.mouse.WorldLocator;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import bluej.debugmgr.objectbench.ObjectBenchInterface;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The worldhandler handles the connection between the World and the
 * VMCommsSimulation.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public class WorldHandler
    implements SimulationListener
{
    /** A flag to check whether a world has been set. Can be tested/cleared by callers. */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean worldIsSet;

    // Note: this field is used by name in GreenfootDebugHandler, so don't rename/remove without altering that code.
    @OnThread(Tag.Any)
    private volatile World world;

    // where did the the drag/drop operation begin? In pixels
    private int dragBeginX;
    private int dragBeginY;

    @OnThread(Tag.Any)
    private final KeyboardManager keyboardManager;
    @OnThread(Tag.Any)
    private static WorldHandler instance;
    @OnThread(Tag.Any)
    private final List<WorldListener> worldListeners = new ArrayList<>();
    @OnThread(Tag.Any)
    private WorldHandlerDelegate handlerDelegate;
    @OnThread(Tag.Any)
    private final MousePollingManager mousePollingManager;

    // Offset from the middle of the actor when initiating a drag on an actor.
    private int dragOffsetX;
    private int dragOffsetY;
    // The actor being dragged
    private Actor dragActor;
    private boolean dragActorMoved;
    private int dragId;
    
    /**
     * Initialise the WorldHandler singleton.
     * 
     * @param worldCanvas  the WorldCanvas to connect to
     * @param helper       the handler delegate for operations
     */    
    @OnThread(Tag.Any)
    public static synchronized void initialise(WorldHandlerDelegate helper)
    {
        instance = new WorldHandler(helper);
    }
    
    /**
     * Initialiser for unit testing.
     */
    @OnThread(Tag.Any)
    public static synchronized void initialise()
    {
        instance = new WorldHandler();
    }
    
    /**
     * Return the singleton instance.
     */
    @OnThread(Tag.Any)
    public synchronized static WorldHandler getInstance()
    {
        return instance;
    }

    /**
     * Constructor used for unit testing.
     */
    @OnThread(Tag.Any)
    private WorldHandler() 
    {
        instance = this;
        keyboardManager = new KeyboardManager();
        mousePollingManager = new MousePollingManager(null);
        handlerDelegate = new WorldHandlerDelegate() {

            @Override
            public void discardWorld(World world)
            {                
            }

            @Override
            public void instantiateNewWorld(String className, Runnable runIfError)
            {
            }

            @Override
            public void setWorld(World oldWorld, World newWorld)
            {
            }
            
            @Override
            public void objectAddedToWorld(Actor actor)
            {
            }

            @Override
            public String ask(String prompt)
            {
                return "";
            }

            @Override
            public void paint(World drawWorld, boolean forcePaint)
            {
                
            }

            @Override
            public void notifyStoppedWithError()
            {

            }
        };
    }
        
    /**
     * Creates a new worldHandler and sets up the connection between worldCanvas
     * and world.
     * 
     * @param handlerDelegate
     */
    @OnThread(Tag.Any)
    private WorldHandler(WorldHandlerDelegate handlerDelegate)
    {
        instance = this;
        this.handlerDelegate = handlerDelegate;
        
        mousePollingManager = new MousePollingManager(null);

        keyboardManager = new KeyboardManager();
    }

    /**
     * Get the keyboard manager.
     */
    @OnThread(Tag.Any)
    public KeyboardManager getKeyboardManager()
    {
        return keyboardManager;
    }

    /**
     * Get the mouse manager.
     */
    @OnThread(Tag.Any)
    public MousePollingManager getMouseManager()
    {
        return mousePollingManager;
    }

    /**
     * Drag operation starting.
     */
    @OnThread(Tag.Simulation)
    public void startDrag(Actor actor, Point p, int dragId)
    {
        dragActor = actor;
        dragActorMoved = false;
        dragBeginX = ActorVisitor.getX(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragBeginY = ActorVisitor.getY(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragOffsetX = dragBeginX - p.x;
        dragOffsetY = dragBeginY - p.y;
        this.dragId = dragId;
        drag(actor, p);
    }
    
    public boolean isDragging()
    {
        return dragActor != null;
    }

    /**
     * Returns an object from the given world at the given pixel location. If multiple objects
     * exist at the one location, this method returns the top-most one according to
     * paint order.
     * 
     * @param x
     *            The x-coordinate
     * @param y
     *            The y-coordinate
     */
    private static Actor getObject(World world, int x, int y)
    {
        if (world == null)
        {
            return null;
        }
        
        Collection<?> objectsThere = WorldVisitor.getObjectsAtPixel(world, x, y);
        if (objectsThere.isEmpty())
        {
            return null;
        }

        Iterator<?> iter = objectsThere.iterator();
        Actor topmostActor = (Actor) iter.next();
        int seq = ActorVisitor.getLastPaintSeqNum(topmostActor);

        while (iter.hasNext())
        {
            Actor actor = (Actor) iter.next();
            int actorSeq = ActorVisitor.getLastPaintSeqNum(actor);
            if (actorSeq > seq)
            {
                topmostActor = actor;
                seq = actorSeq;
            }
        }
        
        return topmostActor;
    }

    /*
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {
        if (dragActor != null) {
            dragActorMoved = false;
            Simulation.getInstance().runLater(new SimulationRunnable() {
                private Actor dragActor = WorldHandler.this.dragActor;
                private int dragBeginX = WorldHandler.this.dragBeginX;
                private int dragBeginY = WorldHandler.this.dragBeginY;
                @Override
                public void run()
                {
                    ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                    repaint();
                }
            });
        }
    }

    /**
     * Request a repaint of the world.
     */
    public void repaint()
    {
        paint(true);
    }
    
    /**
     * Request a repaint of the world.
     * Call only from the simulation thread.
     */
    public void repaintAndWait()
    {
        repaint();
    }

    /**
     * Instantiate a new world and do any initialisation needed to activate that
     * world.
     * 
     * @param className The fully qualified name of the world class to instantiate
     *                  if a specific class is wanted.  If null, use the most recently
     *                  instantiated world class.
     */
    @OnThread(Tag.Any)
    public void instantiateNewWorld(String className)
    {
        handlerDelegate.instantiateNewWorld(className, () -> worldInstantiationError());
    }

    /**
     * Notify that construction of a new world has started.  Note that this method
     * has a special breakpoint set by GreenfootDebugHandler, so do not remove/rename
     * without also editing that code.
     */
    public void setInitialisingWorld(World world)
    {
        handlerDelegate.initialisingWorld(world.getClass().getName());
    }

    /** 
     * Removes the current world. This can be called from any thread.
     */
    @OnThread(Tag.Any)
    public void discardWorld()
    {
        final World discardedWorld;
        synchronized (this)
        {
            if (world == null)
            {
                return;
            }

            handlerDelegate.discardWorld(world);
            discardedWorld = world;
            world = null;
        }
        // Do this outside the synchronized block to prevent us owning
        // both the WorldHandler and Simulation monitors at the same time:
        Simulation.getInstance().runLater(() -> {
            fireWorldRemovedEvent(discardedWorld);
        });
    }

    /**
     * Check whether a world has been set (via {@link #setWorld()}) since the "world is set" flag was last cleared.
     */
    @OnThread(Tag.Any)
    public synchronized boolean checkWorldSet()
    {
        return worldIsSet;
    }

    /**
     * Clear the "world is set" flag.
     */
    @OnThread(Tag.Any)
    public synchronized void clearWorldSet()
    {
        worldIsSet = false;
    }

    /**
     * Sets a new world.
     * 
     * @param world  The new world. Must not be null.
     * @param byUserCode Was this world set by a call to Greenfoot.setWorld (which thus would
     *                   have come from the user's code)?  If false, it means it was set by our own
     *                   internal code, e.g. initialisation during standalone, or GUI interactions
     *                   in the IDE.
     */
    @OnThread(Tag.Any)
    public synchronized void setWorld(final World world, boolean byUserCode)
    {
        worldIsSet = true;
        
        handlerDelegate.setWorld(this.world, world);
        mousePollingManager.setWorldLocator(new WorldLocator() {
            @Override
            @OnThread(Tag.Simulation)
            public Actor getTopMostActorAt(int x, int y)
            {
                return getObject(world, x, y);
            }

            @Override
            @OnThread(Tag.Any)
            public int getTranslatedX(int x)
            {
                return WorldVisitor.toCellFloor(world, x);
            }

            @Override
            @OnThread(Tag.Any)
            public int getTranslatedY(int y)
            {
                return WorldVisitor.toCellFloor(world, y);
            }
        });
        this.world = world;
        
        Simulation.getInstance().runLater(() -> {
            fireWorldCreatedEvent(world);
        });

        worldChanged(byUserCode);
    }

    /**
     * This is a special method which will have a breakpoint set by the GreenfootDebugHandler
     * class.  Do not remove or rename without also changing that class.
     * 
     * @param byUserCode Was this world set by a call to Greenfoot.setWorld (which thus would
     *                   have come from the user's code)?  If false, it means it was set by our own
     *                   internal code, e.g. initialisation during standalone, or GUI interactions
     *                   in the IDE.  This param is marked unused but actually
     *                   GreenfootDebugHandler will inspect it via JDI
     */
    @OnThread(Tag.Any)
    private void worldChanged(boolean byUserCode)
    {
    }

    /**
     * This is a special method which will have a breakpoint set by the GreenfootDebugHandler
     * class.  Do not remove or rename without also changing that class.
     * It is called where there is an error instantiated the world class
     * (as a result of a user interactive creation, not user code)
     */
    @OnThread(Tag.Any)
    private void worldInstantiationError()
    {
    }

    /**
     * Return the currently active world.
     */
    public synchronized World getWorld()
    {
        return world;
    }
    
    /**
     * Checks if there is a world set.
     * 
     * This is not the same as checking if getWorld() is null, because getWorld()
     * can return a world being initialised.  This method checks if a world has
     * actually been set.
     */
    public synchronized boolean hasWorld()
    {
        return world != null;
    }

    /**
     * Handle drop of actors. Handles QuickAdd
     * 
     * When existing actors are dragged around in the world, that uses drag -- drop is *not* called for those
     */
    public boolean drop(Object o, Point p)
    {
        final World world = this.world;
        
        int maxHeight = WorldVisitor.getHeightInPixels(world);
        int maxWidth = WorldVisitor.getWidthInPixels(world);
        final int x = (int) p.getX();
        final int y = (int) p.getY();

        if (x >= maxWidth || y >= maxHeight || x < 0 || y < 0) {
            return false;
        }
        else if (o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {
            // object received from the inspector via the Get button.
            Actor actor = (Actor) o;
            addActorAtPixel(actor, x, y);
            return true;
        }
        else if (o instanceof Actor) {
            final Actor actor = (Actor) o;
            if (ActorVisitor.getWorld(actor) == null) {
                // Under some strange circumstances the world can be null here.
                // This can happen in the GridWorld scenario because it
                // overrides World.addObject().
                return false;
            }
            Simulation.getInstance().runLater(() -> ActorVisitor.setLocationInPixels(actor, x, y));
            dragActorMoved = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Handle drag on actors that are already in the world.
     * <p>
     * This must be called on the simulation thread.
     */
    @OnThread(Tag.Simulation)
    public boolean drag(Object o, Point p)
    {
        World world = this.world;
        if (o instanceof Actor && world != null)
        {
            int x = WorldVisitor.toCellFloor(world, (int) p.getX() + dragOffsetX);
            int y = WorldVisitor.toCellFloor(world, (int) p.getY() + dragOffsetY);
            final Actor actor = (Actor) o;
            try
            {
                int oldX = ActorVisitor.getX(actor);
                int oldY = ActorVisitor.getY(actor);

                if (oldX != x || oldY != y)
                {
                    if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                            && x >= 0 && y >= 0)
                    {
                        ActorVisitor.setLocationInPixels(actor,
                                (int) p.getX() + dragOffsetX,
                                (int) p.getY() + dragOffsetY);
                        dragActorMoved = true;
                        repaint();
                    }
                    else
                    {
                        ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                        x = WorldVisitor.toCellFloor(getWorld(), dragBeginX);
                        y = WorldVisitor.toCellFloor(getWorld(), dragBeginY);
                        
                        dragActorMoved = false; // Pinged back to where it was

                        repaint();
                        return false;
                    }
                }
            }
            catch (IndexOutOfBoundsException e) {}
            catch (IllegalStateException e)
            {
                // If World.addObject() has been overridden the actor might not
                // have been added to the world and we will get this exception
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Add an actor at the given pixel co-ordinates. The co-ordinates are translated
     * into cell co-ordinates, and the actor is added at those cell co-ordinates, if they
     * are within the world.
     * 
     * @return  true if the Actor was added into the world; false if the co-ordinates were
     *          outside the world.
     */
    @OnThread(Tag.Any)
    public boolean addActorAtPixel(final Actor actor, int xPixel, int yPixel)
    {
        final World world = this.world;
        final int x = WorldVisitor.toCellFloor(world, xPixel);
        final int y = WorldVisitor.toCellFloor(world, yPixel);
        if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                && x >= 0 && y >= 0) {
            Simulation.getInstance().runLater(() -> {
                world.addObject(actor, x, y);
                // Make sure we repaint after user adds something to the world,
                // otherwise will look like lag:
                Simulation.getInstance().paintRemote(true);
            });
            return true;
        }
        else {
            return false;
        }
    }

    @OnThread(Tag.Simulation)
    protected void fireWorldCreatedEvent(World newWorld)
    {
        WorldEvent worldEvent = new WorldEvent(newWorld);
        for (WorldListener worldListener : worldListeners)
        {
            worldListener.worldCreated(worldEvent);
        }
    }

    @OnThread(Tag.Simulation)
    public void fireWorldRemovedEvent(World discardedWorld)
    {
        WorldEvent worldEvent = new WorldEvent(discardedWorld);
        for (WorldListener worldListener : worldListeners)
        {
            worldListener.worldRemoved(worldEvent);
        }
    }

    /**
     * Add a worldListener to listen for when a worlds are created and removed.
     * Events will be delivered on the Simulation thread.
     * 
     * @param l Listener to add
     */
    @OnThread(Tag.Any)
    public void addWorldListener(WorldListener l)
    {
        worldListeners.add(0, l);
    }

    /**
     * Used to indicate the start of a simulation round. For use in the
     * collision checker. Called from the simulation thread.
     * 
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    private void startSequence()
    {
        // Guard against world getting nulled concurrently:
        World world = this.world;
        if (world != null) {
            WorldVisitor.startSequence(world);
            mousePollingManager.newActStarted();
        }
    }

    /**
     * Completes the current drag if it is the given drag ID
     */
    @OnThread(Tag.Any)
    public void finishDrag(int dragId)
    {
        Simulation.getInstance().runLater(() -> {
            // if the operation was cancelled, add the object back into the
            // world at its original position
            if (this.dragId == dragId)
            {
                if (dragActorMoved)
                {
                    // This makes sure that a single (final) setLocation
                    // call is received by the actor when dragging ends.
                    // This matters if the actor has overridden setLocation
                    int ax = ActorVisitor.getX(dragActor);
                    int ay = ActorVisitor.getY(dragActor);
                    // First we set the position to be the pre-drag position.
                    // This means that if the user overrides setLocation and
                    // chooses not to call the inherited setLocation, the position
                    // will be as if the drag never happened:
                    ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                    dragActor.setLocation(ax, ay);
                }
                dragActor = null;
            }
        });
    }

    @OnThread(Tag.Simulation)
    public void simulationChangedSync(SyncEvent e)
    {
        if (e == SyncEvent.NEW_ACT_ROUND) {
            startSequence();
        }
        else if (e == SyncEvent.STARTED)
        {
            mousePollingManager.startedRunning();
        }
    }

    @Override
    public @OnThread(Tag.Any) void simulationChangedAsync(AsyncEvent e)
    {
    }

    /**
     * Get the object bench if it exists. Otherwise return null.
     */
    public ObjectBenchInterface getObjectBench()
    {
        if(handlerDelegate instanceof ObjectBenchInterface) {
            return (ObjectBenchInterface) handlerDelegate;
        }
        else {
            return null;
        }
    }

    /**
     * This is a hook called by the World whenever an actor gets added to it. When running in the IDE,
     * this allows names to be assigned to the actors for interaction recording purposes.
     */
    public void objectAddedToWorld(Actor object)
    {
        handlerDelegate.objectAddedToWorld(object);
    }

    /**
     * Ask a question, with a given prompt, to the user (i.e. implement Greenfoot.ask()).
     * Must be called on the simulation thread.
     */
    public String ask(String prompt)
    {
        return handlerDelegate.ask(prompt);
    }

    /**
     * Continue an actor drag operation. Can be called from any thread.
     * 
     * @param dragId   The identifier of the drag operation (must match the
     *                 identifier used in {@link #startDrag}).
     * @param x        The x-coordinate in pixels of the drag location
     * @param y        The y-coordinate in pixels of the drag location
     */
    @OnThread(Tag.Any)
    public void continueDragging(int dragId, int x, int y)
    {
        Simulation.getInstance().runLater(() -> {
            if (dragId == this.dragId)
            {
                drag(dragActor, new Point(x, y));
                // We're gonna need another paint after this:
                Simulation.getInstance().paintRemote(true);
                
            }
        });
    }

    /**
     * The simulation had some user code which threw an exception
     * that was not caught by the user code.
     */
    public void notifyStoppedWithError()
    {
        handlerDelegate.notifyStoppedWithError();
    }

    /**
     * Repaint the world.
     * @param forcePaint Force paint (ignore any optimisations to not paint frames too often, etc)
     */
    public void paint(boolean forcePaint)
    {
        handlerDelegate.paint(world, forcePaint);
    }

    /**
     * The focus has changed on the world display, so tell the keyboard manager.
     * @param focused true if gained focus, false if lost focus.
     */
    @OnThread(Tag.Any)
    public void worldFocusChanged(boolean focused)
    {
        if (focused)
        {
            keyboardManager.focusGained();
        }
        else
        {
            keyboardManager.focusLost();
        }
    }

    /**
     * Notify that the world construction has completed.
     */
    public void finishedInitialisingWorld()
    {
        handlerDelegate.finishedInitialisingWorld();
    }
}
