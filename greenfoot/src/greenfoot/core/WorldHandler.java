/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.input.InputManager;
import greenfoot.gui.input.KeyboardManager;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.gui.input.mouse.MousePollingManager;
import greenfoot.gui.input.mouse.WorldLocator;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.util.GraphicsUtilities;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import bluej.debugmgr.objectbench.ObjectBenchInterface;

/**
 * The worldhandler handles the connection between the World and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 */
public class WorldHandler
    implements TriggeredMouseListener, TriggeredMouseMotionListener, TriggeredKeyListener, DropTarget, DragListener, SimulationListener
{
    /** A flag to check whether a world has been set. Can be tested/cleared by callers. */
    private boolean worldIsSet;

    private World initialisingWorld;
    private volatile World world;
    private WorldCanvas worldCanvas;

    // where did the the drag/drop operation begin? In pixels
    private int dragBeginX;
    private int dragBeginY;

    /**
     * Whether the object was dropped, or more specifically, whether it does not
     * need to be replaced if the drop is cancelled.
     */
    private boolean objectDropped = true; // true if the object was dropped

    private KeyboardManager keyboardManager;
    private static WorldHandler instance;
    private EventListenerList listenerList = new EventListenerList();
    private WorldHandlerDelegate handlerDelegate;
    private MousePollingManager mousePollingManager;
    private InputManager inputManager;

    // Offset from the middle of the actor when initiating a drag on an actor.
    private int dragOffsetX;
    private int dragOffsetY;
    // The actor being dragged
    private Actor dragActor;
    private boolean dragActorMoved;
    private Cursor defaultCursor;
    
    /** Lock used for world manipulation */
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    /** Timeout used for readers attempting to acquire lock */
    public static final int READ_LOCK_TIMEOUT = 500;
    
    /** Condition used to wait for repaint */
    private Object repaintLock = new Object();
    private boolean isRepaintPending = false;
    
    public static synchronized void initialise(WorldCanvas worldCanvas, WorldHandlerDelegate helper)
    {
        instance = new WorldHandler(worldCanvas, helper);
    }
    
    /**
     * Initialiser for unit testing.
     */
    public static synchronized void initialise()
    {
        instance = new WorldHandler();
    }
    
    /**
     * Return the singleton instance.
     */
    public synchronized static WorldHandler getInstance()
    {
        return instance;
    }

    /**
     * Constructor used for unit testing.
     */
    private WorldHandler() 
    {
        instance = this;
        mousePollingManager = new MousePollingManager(null);
        handlerDelegate = new WorldHandlerDelegate() {

            @Override
            public void discardWorld(World world)
            {                
            }

            @Override
            public InputManager getInputManager()
            {
                return null;
            }

            @Override
            public void instantiateNewWorld()
            {
            }

            @Override
            public boolean maybeShowPopup(MouseEvent e)
            {
                return false;
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
            }
            
            @Override
            public void mouseMoved(MouseEvent e)
            {
            }

            @Override
            public void setWorld(World oldWorld, World newWorld)
            {
            }

            @Override
            public void setWorldHandler(WorldHandler handler)
            {
            }
            
            @Override
            public void addActor(Actor actor, int x, int y)
            {
            }

            @Override
            public void actorDragged(Actor actor, int xCell, int yCell)
            {
            }
            
            @Override
            public void objectAddedToWorld(Actor actor)
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
    private WorldHandler(final WorldCanvas worldCanvas, WorldHandlerDelegate handlerDelegate)
    {
        instance = this;
        this.handlerDelegate = handlerDelegate;
        this.handlerDelegate.setWorldHandler(this);

        this.worldCanvas = worldCanvas;
        
        mousePollingManager = new MousePollingManager(null);

        worldCanvas.setDropTargetListener(this);

        LocationTracker.instance().setSourceComponent(worldCanvas);
        keyboardManager = new KeyboardManager();
        worldCanvas.addFocusListener(keyboardManager);

        inputManager = handlerDelegate.getInputManager();
        addWorldListener(inputManager);
        inputManager.setRunningListeners(getKeyboardManager(), mousePollingManager, mousePollingManager);
        worldCanvas.addMouseListener(inputManager);
        worldCanvas.addMouseMotionListener(inputManager);
        worldCanvas.addKeyListener(inputManager);
        inputManager.init();

        defaultCursor = worldCanvas.getCursor();
    }

    /**
     * Get the keyboard manager.
     */
    public KeyboardManager getKeyboardManager()
    {
        return keyboardManager;
    }

    /**
     * Get the mouse manager.
     */
    public MousePollingManager getMouseManager()
    {
        return mousePollingManager;
    }

    /*
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e)
    {
        handlerDelegate.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        World world = this.world;
        boolean isPopUp = handlerDelegate.maybeShowPopup(e);
        if (world != null && SwingUtilities.isLeftMouseButton(e) && !isPopUp) {
            Actor actor = getObject(e.getX(), e.getY());
            if (actor != null) {
                Point p = e.getPoint();
                startDrag(actor, p, world);
            }
        }
    }

    /**
     * Drag operation starting. Called on the Swing event dispatch thread.
     */
    private void startDrag(Actor actor, Point p, World world)
    {
        dragActor = actor;
        dragActorMoved = false;
        dragBeginX = ActorVisitor.getX(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragBeginY = ActorVisitor.getY(actor) * world.getCellSize() + world.getCellSize() / 2;
        dragOffsetX = dragBeginX - p.x;
        dragOffsetY = dragBeginY - p.y;
        objectDropped = false;
        SwingUtilities.getWindowAncestor(worldCanvas).toFront();
        worldCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        drag(actor, p);
    }
    
    public boolean isDragging()
    {
        return dragActor != null;
    }

    /*
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e)
    {
        handlerDelegate.maybeShowPopup(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragActor != null && dragActorMoved) {
                // This makes sure that a single (final) setLocation
                // call is received by the actor when dragging ends.
                // This matters if the actor has overridden setLocation
                Simulation.getInstance().runLater(new Runnable() {
                    private Actor dragActor = WorldHandler.this.dragActor;
                    @Override
                    public void run()
                    {
                        int ax = ActorVisitor.getX(dragActor);
                        int ay = ActorVisitor.getY(dragActor);
                        // First we set the position to be the pre-drag position.
                        // This means that if the user overrides setLocation and 
                        // chooses not to call the inherited setLocation, the position
                        // will be as if the drag never happened:
                        ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
                        dragActor.setLocation(ax, ay);
                        handlerDelegate.actorDragged(dragActor, ax, ay);
                    }
                });
            }
            dragActor = null;
            worldCanvas.setCursor(defaultCursor);
        };
    }

    /**
     * Returns an object at the given pixel location. If multiple objects exist
     * at the one location, this method returns the top-most one according to
     * paint order.
     * 
     * @param x
     *            The x-coordinate
     * @param y
     *            The y-coordinate
     */
    public Actor getObject(int x, int y)
    {
        return getObject(this.world, x, y);
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
        if (world == null) {
            return null;
        }
        
        int timeout = READ_LOCK_TIMEOUT;
        try {
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {

                Collection<?> objectsThere = WorldVisitor.getObjectsAtPixel(world, x, y);
                if (objectsThere.isEmpty()) {
                    lock.readLock().unlock();
                    return null;
                }

                Iterator<?> iter = objectsThere.iterator();
                Actor topmostActor = (Actor) iter.next();
                int seq = ActorVisitor.getLastPaintSeqNum(topmostActor);

                while (iter.hasNext()) {
                    Actor actor = (Actor) iter.next();
                    int actorSeq = ActorVisitor.getLastPaintSeqNum(actor);
                    if (actorSeq > seq) {
                        topmostActor = actor;
                        seq = actorSeq;
                    }
                }
                
                lock.readLock().unlock();
                return topmostActor;
            }
        }
        catch (InterruptedException ie) {}

        return null;
    }

    /*
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {
        worldCanvas.requestFocusInWindow();
    }

    /*
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {
        if (dragActor != null) {
            dragActorMoved = false;
            Simulation.getInstance().runLater(new Runnable() {
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
     * Request a repaints of world
     */
    public void repaint()
    {
        worldCanvas.repaint();
    }
    
    /**
     * Request a repaint of the world, and wait (with a timeout) until the repaint actually occurs.
     */
    public void repaintAndWait()
    {
        worldCanvas.repaint();

        boolean isWorldLocked = lock.isWriteLockedByCurrentThread();
        
        synchronized (repaintLock) {
            // If the world lock is held, as it should be unless this method is called from
            // a user-created thread, we should unlock it to allow the repaint to occur.
            if (isWorldLocked) {
                lock.writeLock().unlock();
            }
            
            // When the repaint actually happens, repainted() will be called, which
            // sets isRepaintPending false and signals repaintLock.
            isRepaintPending = true;
            try {
                do {
                    repaintLock.wait(100);
                } while (isRepaintPending);
            }
            catch (InterruptedException ie) {
                throw new ActInterruptedException();
            }
            finally {
                isRepaintPending = false; // in case our wait interrupted/timed out
                if (isWorldLocked) {
                    lock.writeLock().lock();
                }
            }
        }
    }

    /**
     * The world has been painted.
     */
    public void repainted()
    {
        synchronized (repaintLock) {
            if (isRepaintPending) {
                isRepaintPending = false;
                repaintLock.notify();
            }
        }
        Simulation.getInstance().worldRepainted();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (dragActor != null) {
                dragActorMoved = false;
                Simulation.getInstance().runLater(new Runnable() {
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
                dragActor = null;
                worldCanvas.setCursor(defaultCursor);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        //TODO: is this really necessary?
        worldCanvas.requestFocus();
    }

    /**
     * Get the world lock, used to control access to the world.
     */
    public ReentrantReadWriteLock getWorldLock()
    {
        return lock;
    }
    
    /**
     * Instantiate a new world and do any initialisation needed to activate that
     * world.
     * 
     * @return The new World or null if an error occured
     */
    public void instantiateNewWorld()
    {
        handlerDelegate.instantiateNewWorld();
    }

    /**
     * Notify that construction of a new world has started.
     * @see #setWorld(World)
     */
    public void setInitialisingWorld(World world)
    {
        this.initialisingWorld = world;
    }

    /** 
     * Removes the current world.
     */
    public synchronized void discardWorld()
    {
        if(world == null) return;
        handlerDelegate.discardWorld(world); 
        final World discardedWorld = world;
        world = null;

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                worldCanvas.setWorld(null);
                fireWorldRemovedEvent(discardedWorld);
            }
        });
    }

    /**
     * Check whether a world has been set (via {@link #setWorld()}) since the "world is set" flag was last cleared.
     */
    public synchronized boolean checkWorldSet()
    {
        return worldIsSet;
    }

    /**
     * Clear the "world is set" flag.
     */
    public synchronized void clearWorldSet()
    {
        worldIsSet = false;
    }

    /**
     * Sets a new world.
     * 
     * @param world  The new world. Must not be null.
     */
    public synchronized void setWorld(final World world)
    {
        worldIsSet = true;
        
        handlerDelegate.setWorld(this.world, world);
        mousePollingManager.setWorldLocator(new WorldLocator() {
            @Override
            public Actor getTopMostActorAt(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return getObject(world, p.x, p.y);
            }

            @Override
            public int getTranslatedX(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return WorldVisitor.toCellFloor(world, p.x);
            }

            @Override
            public int getTranslatedY(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return WorldVisitor.toCellFloor(world, p.y);
            }
        });
        this.world = world;
        
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                if(worldCanvas != null) {
                    worldCanvas.setWorld(world);
                }
                fireWorldCreatedEvent(world);
            }
        });
    }

    /**
     * Return the currently active world.
     */
    public synchronized World getWorld()
    {
        if (world == null) {
            return initialisingWorld;
        }
        else {
            return world;
        }
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
        else if (o instanceof ObjectDragProxy) {
            // create the real object
            final ObjectDragProxy to = (ObjectDragProxy) o;
            to.createRealObject();
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    world.removeObject(to);
                }
            });
            objectDropped = true;
            return true;
        }
        else if (o instanceof Actor && ActorVisitor.getWorld((Actor) o) == null) {
            // object received from the inspector via the Get button.
            Actor actor = (Actor) o;
            addActorAtPixel(actor, x, y);
            objectDropped = true;
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
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    ActorVisitor.setLocationInPixels(actor, x, y);
                }
            });
            dragActorMoved = true;
            objectDropped = true;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Handle drag on actors that are already in the world.
     * 
     * <p>This is called on the Swing event dispatch thread.
     */
    public boolean drag(Object o, Point p)
    {
        World world = this.world;
        if (o instanceof Actor && world != null) {
            int x = WorldVisitor.toCellFloor(world, (int) p.getX() + dragOffsetX);
            int y = WorldVisitor.toCellFloor(world, (int) p.getY() + dragOffsetY);
            final Actor actor = (Actor) o;
            try {
                int oldX = ActorVisitor.getX(actor);
                int oldY = ActorVisitor.getY(actor);

                if (oldX != x || oldY != y) {
                    if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                            && x >= 0 && y >= 0) {
                        WriteLock writeLock = lock.writeLock();
                        // The only reason we would fail to obtain the lock is if a repaint
                        // is happening at this very instant. That shouldn't be too much of
                        // a problem; it will mean a slight glitch in the drag, probably not
                        // noticeable.
                        if (writeLock.tryLock()) {
                            ActorVisitor.setLocationInPixels(actor,
                                    (int) p.getX() + dragOffsetX,
                                    (int) p.getY() + dragOffsetY);
                            writeLock.unlock();
                            dragActorMoved = true;
                            repaint();
                        }
                    }
                    else {
                        WriteLock writeLock = lock.writeLock();
                        if (writeLock.tryLock()) {
                            ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                            x = WorldVisitor.toCellFloor(getWorld(), dragBeginX);
                            y = WorldVisitor.toCellFloor(getWorld(), dragBeginY);
                            handlerDelegate.actorDragged(actor, x, y);
                            writeLock.unlock();
                            
                            dragActorMoved = false; // Pinged back to where it was

                            repaint();
                        }
                        return false;
                    }
                }
            }
            catch (IndexOutOfBoundsException e) {}
            catch (IllegalStateException e) {
                // If World.addObject() has been overridden the actor might not
                // have been added to the world and we will get this exception
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Adds the object where the mouse event occurred.
     * 
     * @return true if successful, or false if the mouse event was outside the world bounds.
     */
    public synchronized boolean addObjectAtEvent(Actor actor, MouseEvent e)
    {
        Component source = (Component) e.getSource();
        if (source != worldCanvas) {
            e = SwingUtilities.convertMouseEvent(source, e, worldCanvas);
        }
        int xPixel = e.getX();
        int yPixel = e.getY();
        return addActorAtPixel(actor, xPixel, yPixel);
    }

    /**
     * Add an actor at the given pixel co-ordinates. The co-ordinates are translated
     * into cell co-ordinates, and the actor is added at those cell co-ordinates, if they
     * are within the world.
     * 
     * @return  true if the Actor was added into the world; false if the co-ordinates were
     *          outside the world.
     */
    private boolean addActorAtPixel(final Actor actor, int xPixel, int yPixel)
    {
        final World world = this.world;
        final int x = WorldVisitor.toCellFloor(world, xPixel);
        final int y = WorldVisitor.toCellFloor(world, yPixel);
        if (x < WorldVisitor.getWidthInCells(world) && y < WorldVisitor.getHeightInCells(world)
                && x >= 0 && y >= 0) {
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    world.addObject(actor, x, y);
                }
            });
            handlerDelegate.addActor(actor, x, y);
            return true;
        }
        else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        if (o instanceof Actor && world != null) {
            final Actor actor = (Actor) o;
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    world.removeObject(actor);
                }
            });
        }
    }

    public void dragFinished(Object o)
    {
        finishDrag(o);
    }

    protected void fireWorldCreatedEvent(World newWorld)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        WorldEvent worldEvent = new WorldEvent(newWorld);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldListener.class) {
                ((WorldListener) listeners[i + 1]).worldCreated(worldEvent);
            }
        }
    }

    public void fireWorldRemovedEvent(World discardedWorld)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        WorldEvent worldEvent = new WorldEvent(discardedWorld);
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldListener.class) {
                ((WorldListener) listeners[i + 1]).worldRemoved(worldEvent);
            }
        }
    }

    /**
     * Add a worldListener to listen for when a worlds are created and removed.
     * Events will be delivered on the GUI event thread.
     * 
     * @param l
     *            Listener to add
     */
    public void addWorldListener(WorldListener l)
    {
        listenerList.add(WorldListener.class, l);
    }

    /**
     * Removes a worldListener.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeWorldListener(WorldListener l)
    {
        listenerList.remove(WorldListener.class, l);
    }

    /**
     * Used to indicate the start of a simulation round. For use in the
     * collision checker. Called from the simulation thread.
     * 
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    public void startSequence()
    {
        // Guard against world getting nulled concurrently:
        World world = this.world;
        if (world != null) {
            WorldVisitor.startSequence(world);
            mousePollingManager.newActStarted();
        }
    }

    public WorldCanvas getWorldCanvas()
    {
        return worldCanvas;
    }

    public EventListenerList getListenerList()
    {
        return listenerList;
    }

    /**
     * Method that cleans up after a drag, and re-enables the worldhandler to
     * receive events. It also puts the object back in its original place if it
     * was not dropped.
     */
    public void finishDrag(Object o)
    {
        // if the operation was cancelled, add the object back into the
        // world at its original position
        if (!objectDropped && o instanceof Actor) {
            final Actor actor = (Actor) o;
            objectDropped = true;
            dragActorMoved = false;
            Simulation.getInstance().runLater(new Runnable() {
                @Override
                public void run()
                {
                    ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                }
            });
        }
    }

    public void simulationChanged(SimulationEvent e)
    {
        inputManager.simulationChanged(e);
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
    
    public InputManager getInputManager()
    {
        return inputManager;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            objectDropped = false;
            drag(dragActor, e.getPoint());
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        objectDropped = false;
        if (dragActor != null) {
            drag(dragActor, e.getPoint());
        }
        handlerDelegate.mouseMoved(e);
    }

    /**
     * Get a snapshot of the currently instantiated world or null if no world is
     * instantiated.
     * 
     * Must be called on the EDT.
     */
    public BufferedImage getSnapShot()
    {
        if (world == null) {
            return null;
        }

        WorldCanvas canvas = getWorldCanvas();
        BufferedImage img = GraphicsUtilities.createCompatibleImage(WorldVisitor.getWidthInPixels(world), WorldVisitor
                .getHeightInPixels(world));
        Graphics2D g = img.createGraphics();
        g.setColor(canvas.getBackground());
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        canvas.paintBackground(g);

        int timeout = READ_LOCK_TIMEOUT;
        // We need to sync when calling the paintObjects
        try {
            if (lock.readLock().tryLock(timeout, TimeUnit.MILLISECONDS)) {
                try {
                    canvas.paintObjects(g);
                }
                finally {
                    lock.readLock().unlock();
                }
            }
        }
        catch (InterruptedException e) {
        }
        return img;
    }

    @Override
    public void listeningEnded()
    {
    }

    @Override
    public void listeningStarted(Object obj)
    {
        World world = this.world;
        
        // If the obj is not null, it means we have to activate the dragging of that object.
        if (world != null && obj != null && obj != dragActor && obj instanceof Actor) {
            Actor actor = (Actor) obj;
            int ax = ActorVisitor.getX(actor);
            int ay = ActorVisitor.getY(actor);
            int x = (int) Math.floor(WorldVisitor.getCellCenter(world, ax));
            int y = (int) Math.floor(WorldVisitor.getCellCenter(world, ay));
            Point p = new Point(x, y);
            startDrag(actor, p, world);
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
}
