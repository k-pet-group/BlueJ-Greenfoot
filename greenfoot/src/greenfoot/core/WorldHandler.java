package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.InputManager;
import greenfoot.gui.WorldCanvas;
import greenfoot.mouse.MousePollingManager;
import greenfoot.mouse.WorldLocator;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.util.GraphicsUtilities;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * The worldhandler handles the connection between the World and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 * @version $Id:$
 */
public class WorldHandler
    implements MouseListener, MouseMotionListener, KeyListener, DropTarget, DragListener, SimulationListener
{
    private World initialisingWorld;
    private World world;
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
    private WorldEvent worldEvent;
    private WorldHandlerDelegate handlerDelegate;
    private MousePollingManager mousePollingManager;
    private InputManager inputManager;

    // Offset from the middle of the actor when initiating a drag on an actor.
    private int dragOffsetX;
    private int dragOffsetY;
    // The actor being dragged
    private Actor dragActor;
    private Cursor defaultCursor;

    public static synchronized void initialise(WorldCanvas worldCanvas, WorldHandlerDelegate helper)
    {
        instance = new WorldHandler(worldCanvas, helper);
    }

    /**
     * Return the singleton instance.
     */
    public synchronized static WorldHandler getInstance()
    {
        return instance;
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
        worldEvent = new WorldEvent(this);
        mousePollingManager = new MousePollingManager(new WorldLocator() {
            public Actor getTopMostActorAt(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return WorldHandler.this.getObject(p.x, p.y);
            }

            public int getTranslatedX(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return WorldVisitor.toCellFloor(getWorld(), p.x);
            }

            public int getTranslatedY(MouseEvent e)
            {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), worldCanvas);
                return WorldVisitor.toCellFloor(getWorld(), p.y);
            }
        });

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
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e)
    {
        handlerDelegate.mouseClicked(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e)
    {
        handlerDelegate.maybeShowPopup(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
            Actor actor = getObject(e.getX(), e.getY());
            if (actor != null) {
                dragActor = actor;
                dragBeginX = actor.getX() * world.getCellSize() + world.getCellSize() / 2;
                dragBeginY = actor.getY() * world.getCellSize() + world.getCellSize() / 2;
                dragOffsetX = dragBeginX - e.getX();
                dragOffsetY = dragBeginY - e.getY();
                objectDropped = false;
                worldCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                drag(actor, e.getPoint());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e)
    {
        handlerDelegate.maybeShowPopup(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
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
        // Grab a snapshot of world to avoid concurrency issues
        World world = this.world;

        if (world == null)
            return null;

        Collection<?> objectsThere = WorldVisitor.getObjectsAtPixel(world, x, y);
        if (objectsThere.isEmpty()) {
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

        return topmostActor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {
        handlerDelegate.setQuickAddActive(false);
        worldCanvas.requestFocusInWindow();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {
        if (dragActor != null) {
            ActorVisitor.setLocationInPixels(dragActor, dragBeginX, dragBeginY);
            repaint();
        }
    }

    /**
     * Request a repaints of world
     */
    public void repaint()
    {
        worldCanvas.repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e)
    {}

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
        handlerDelegate.processKeyEvent(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
        worldCanvas.requestFocus();
        handlerDelegate.keyReleased(e);
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
     * 
     * @see #setWorld(World)
     * @param world
     */
    public void setInitialisingWorld(World world)
    {
        this.initialisingWorld = world;
    }

    /**
     * Sets a new world. A world is set in two steps:
     * 
     * 1. When it is partially created the constructor in World will set the
     * world in world handler, so that actors can access the world early on in
     * their own constructors. (with worldInitialising(World world)
     * 
     * 2. When the world-object is fully created (finished the constructor) it
     * will notify the worldhandler that it is fully created. (with setWorld)
     * 
     * @see #setInitialisingWorld(World)
     */
    public synchronized void setWorld(final World world)
    {
        handlerDelegate.setWorld(this.world, world);
        this.world = world;
        initialisingWorld = null;
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                worldCanvas.setWorld(world);
                if (WorldHandler.this.world != null) {
                    fireWorldCreatedEvent();
                }
            }
        });
    }

    /**
     * Returns the world-class that has been instantiated last. This is the
     * class of the current world, if there is a world currently instantiated.
     * 
     * @return The world. Will return 'null' if no world has ever been
     *         instantiated in this project, or the world can no longer be
     *         instantiated.
     */
    public Class<?> getLastWorldClass()
    {
        return handlerDelegate.getLastWorldClass();
    }

    /**
     * Return the currently active world.
     */
    public World getWorld()
    {
        if (world == null)
            return initialisingWorld;
        else
            return world;
    }

    /**
     * Handle drop of actors. Handles all types of drops (Move, QuickAddd etc)
     */
    public boolean drop(Object o, Point p)
    {
        int maxHeight = WorldVisitor.getHeightInPixels(world);
        int maxWidth = WorldVisitor.getWidthInPixels(world);
        int x = (int) p.getX();
        int y = (int) p.getY();

        if (x >= maxWidth || y >= maxHeight) {
            return false;
        }
        else if (o instanceof ObjectDragProxy) {
            // create the real object
            ObjectDragProxy to = (ObjectDragProxy) o;
            to.createRealObject();
            world.removeObject(to);
            objectDropped = true;
            return true;
        }
        else if (o instanceof Actor) {
            Actor actor = (Actor) o;
            if (actor.getWorld() == null) {
                // Under some strange circumstances the world can be null here.
                // This can happen in the GridWorld scenario because it
                // overrides World.addObject().
                return false;
            }
            try {
                ActorVisitor.setLocationInPixels(actor, x, y);
                objectDropped = true;
            }
            catch (IndexOutOfBoundsException e) {
                // it happens...
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Handle drag on actors that are already in the world
     */
    public boolean drag(Object o, Point p)
    {
        if (o instanceof Actor && world != null) {
            int x = WorldVisitor.toCellFloor(getWorld(), (int) p.getX());
            int y = WorldVisitor.toCellFloor(getWorld(), (int) p.getY());
            Actor actor = (Actor) o;
            try {

                int oldX = actor.getX();
                int oldY = actor.getY();

                if (oldX != x || oldY != y) {
                    if (x < world.getWidth() && y < world.getHeight() && x >= 0 && y >= 0) {
                        ActorVisitor.setLocationInPixels(actor, (int) p.getX() + dragOffsetX, (int) p.getY()
                                + dragOffsetY);
                        repaint();
                    }
                    else {
                        ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
                        repaint();
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
     * Adds the object where the mouse event occured.
     * 
     * @return true if location changed
     * @throws IndexOutOfBoundsException
     *             If the coordinates are outside the bounds of the world. Note
     *             that a wrapping world has no bounds.
     */
    public synchronized boolean addObjectAtEvent(Actor actor, MouseEvent e)
    {
        Component source = (Component) e.getSource();
        if (source != worldCanvas) {
            e = SwingUtilities.convertMouseEvent(source, e, worldCanvas);
        }

        World world = getWorld();
        int x = WorldVisitor.toCellFloor(world, e.getX());
        int y = WorldVisitor.toCellFloor(world, e.getY());
        if (x < world.getWidth() && y < world.getHeight()) {
            world.addObject(actor, x, y);
            return true;
        }
        else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        if (o instanceof Actor && world != null) {
            Actor actor = (Actor) o;
            world.removeObject(actor);
        }
    }

    public void dragFinished(Object o)
    {
        handlerDelegate.dragFinished(o);
    }

    protected void fireWorldCreatedEvent()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == WorldListener.class) {
                ((WorldListener) listeners[i + 1]).worldCreated(worldEvent);
            }
        }
    }

    public void fireWorldRemovedEvent()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
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
     * Used to indicate the start of an animation sequence. For use in the
     * collision checker.
     * 
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    public void startSequence()
    {
        WorldVisitor.startSequence(world);
    }

    public WorldCanvas getWorldCanvas()
    {
        return worldCanvas;
    }

    public boolean isObjectDropped()
    {
        return objectDropped;
    }

    public void setObjectDropped(boolean b)
    {
        objectDropped = b;
    }

    public EventListenerList getListenerList()
    {
        return listenerList;
    }

    /**
     * Method that cleans up after a drag, and re-enables the worldhandler to
     * recieve events. It also puts the object back in its original place if it
     * was not dropped.
     */
    public void finishDrag(Object o)
    {
        // if the operation was cancelled, add the object back into the
        // world at its original position
        if (!isObjectDropped() && o instanceof Actor) {
            Actor actor = (Actor) o;
            setObjectDropped(true);
            ActorVisitor.setLocationInPixels(actor, dragBeginX, dragBeginY);
        }
    }

    public void simulationChanged(SimulationEvent e)
    {
        inputManager.simulationChanged(e); // TODO maybe add somewhere else?
        keyboardManager.simulationChanged(e);
        if (e.getType() == SimulationEvent.NEW_ACT) {
            mousePollingManager.newActStarted();
        }
    }

    public InputManager getInputManager()
    {
        return inputManager;
    }

    public void mouseDragged(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            objectDropped = false;
            drag(dragActor, e.getPoint());
        }
    }

    public void mouseMoved(MouseEvent e)
    {}

    /**
     * Get a snapshot of the currently instantiated world or null if no world is
     * instantiated.
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
        canvas.paintObjects(g);
        return img;
    }

}