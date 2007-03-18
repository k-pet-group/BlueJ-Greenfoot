package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.classbrowser.SelectionManager;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.*;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.EventListenerList;

/**
 * The worldhandler handles the connection between the World and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class WorldHandler implements MouseListener, KeyListener, DropTarget, DragListener
{
    private World world;
    private WorldCanvas worldCanvas;

    // where did the the drag/drop operation begin?
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

    
    
    public static synchronized void initialise(WorldCanvas worldCanvas, WorldHandlerDelegate helper) 
    {
        if(instance == null) {
            instance = new WorldHandler(worldCanvas, helper);
        } else {
            throw (new IllegalStateException("Can only intiliase this singleton once."));
        }
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
     * @param handlerDelegate 
     */
    private WorldHandler(WorldCanvas worldCanvas, WorldHandlerDelegate handlerDelegate)
    {
        this.handlerDelegate = handlerDelegate;
        this.handlerDelegate.setWorldHandler(this);

        this.worldCanvas = worldCanvas;
        worldEvent = new WorldEvent(this);
        worldCanvas.addMouseListener(this);
        worldCanvas.addKeyListener(this);
        worldCanvas.setDropTargetListener(this);
        
        LocationTracker.instance().setSourceComponent(worldCanvas);
        
        keyboardManager = new KeyboardManager();
        DragGlassPane.getInstance().addKeyListener(this);
    }
    
    /**
     * Sets the selection manager.
     * 
     * 
     * @param selectionManager
     */
    public void setSelectionManager(SelectionManager selectionManager)
    {
        handlerDelegate.setSelectionManager(selectionManager);
    }
    
    /**
     * Get the keyboard manager.
     */
    public KeyboardManager getKeyboardManager()
    {
        return keyboardManager;
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
                dragBeginX = actor.getX() * world.getCellSize() + world.getCellSize()/2;
                dragBeginY = actor.getY() * world.getCellSize() + world.getCellSize()/2;
                int dragOffsetX = dragBeginX - e.getX();
                int dragOffsetY = dragBeginY - e.getY();
                objectDropped = false;
                DragGlassPane.getInstance().startDrag(actor, dragOffsetX, dragOffsetY, this, worldCanvas, false);

                // While the drag is occuring, the world handler no longer
                // processes mouse/key events
                worldCanvas.removeMouseListener(this);
                worldCanvas.removeKeyListener(this);
                worldCanvas.addMouseMotionListener(DragGlassPane.getInstance());
                worldCanvas.addMouseListener(DragGlassPane.getInstance());
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

    }

    public int getDragBeginX() {
        return dragBeginX;
    }

    public int getDragBeginY() {
        return dragBeginY;
    }
    
    /**
     * TODO: this method should be removed when it is posisble to select among
     * multiple objects from a popup menu.
     * 
     * Returns the object at the given pixel location.
     * 
     * @param x
     * @param y
     * @return
     */
    public Actor getObject(int x, int y)
    {
        if (world == null)
            return null;

        Collection objectsThere = WorldVisitor.getObjectsAtPixel(world, x, y);
        if (objectsThere.size() < 1) {
            return null;
        }

        Iterator iter = objectsThere.iterator();
        Actor actor = null;
        while (iter.hasNext()) {
            actor = (Actor) iter.next();
        }
        return actor;
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
    {
        keyboardManager.keyTyped(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
    	handlerDelegate.processKeyEvent(e);
        keyboardManager.keyPressed(e);
    }




    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
        DragGlassPane.getInstance().cancelDrag(); // dragEnded/dragFinished
        worldCanvas.requestFocus();
        
        handlerDelegate.keyReleased(e);
        keyboardManager.keyReleased(e);
    }
    
    /**
     * Attaches a project to this handler. Must be of type GProject. The
     * parameter is not of type GProject to make WorldHandler independent of
     * GProject which is necessary to run scenarios on different platforms.
     */
    public void attachProject(Object project)
    {
        handlerDelegate.attachProject(project);
    }
            
    /**
     * Sets a new world.
     */
    public synchronized void setWorld(World world)
    {
    
        handlerDelegate.setWorld(this.world, world);
        
        this.world = world;

        worldCanvas.setWorld(world);
        
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {

                if (WorldHandler.this.world != null) {
                    fireWorldCreatedEvent();
                }
            }
        });
      
    }

    /**
     * @return
     */
    public Component getWorldTitle()
    {
        return handlerDelegate.getWorldTitle();
    }
    
    public World getWorld()
    {
        return world;
    }

    public boolean drop(Object o, Point p)
    {
        int maxHeight = WorldVisitor.getHeightInPixels(world);
        int maxWidth = WorldVisitor.getWidthInPixels(world);
        int x = (int) p.getX();
        int y = (int) p.getY();
        
        if(x >= maxWidth || y >= maxHeight) {
            return false;
        }
        else if( o instanceof ObjectDragProxy) {
            //create the real object
            ObjectDragProxy to = (ObjectDragProxy) o;
            to.createRealObject();
            world.removeObject(to);
            objectDropped = true;
            return true;
        }
        else if (o instanceof Actor) {
            Actor actor = (Actor) o;
            if(actor.getWorld() == null) {
                // Under some strange cirumstances the world can be null here.
                // This can happen in the GridWorld scenario because it
                // overrides World.addObject().
                return false;
            }
            try {
                ActorVisitor.setLocationInPixels(actor, x, y);
                objectDropped = true;
            }
            catch(IndexOutOfBoundsException e) {
                //it happens...
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see greenfoot.gui.DropTarget#drag(java.lang.Object, java.awt.Point)
     */
    public boolean drag(Object o, Point p)
    {
        if (o instanceof Actor && world != null) {
            int x = WorldVisitor.toCellFloor(getWorld(), (int) p.getX());
            int y = WorldVisitor.toCellFloor(getWorld(), (int) p.getY());
            Actor actor = (Actor) o;
            try {
                //make sure the object is added to the world.
                getWorld().addObject(actor, x, y);                

                int oldX = actor.getX();
                int oldY = actor.getY();

                if (oldX != x || oldY != y) {
                    if (x < world.getWidth() && y < world.getHeight() && x >= 0 && y >= 0) {
                        Simulation.getInstance().dragObject(actor, x, y);
                    }
                    else {
                        world.removeObject(actor);
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
     * @throws IndexOutOfBoundsException If the coordinates are outside the
     *             bounds of the world. Note that a wrapping world has no
     *             bounds.
     */
    public synchronized boolean addObjectAtEvent(Actor actor, MouseEvent e)
    {
        Component source = (Component) e.getSource();
        if (source != worldCanvas) {
            e = SwingUtilities.convertMouseEvent(source, e, worldCanvas);
        }
        int x = WorldVisitor.toCellFloor(getWorld(), e.getX());
        int y = WorldVisitor.toCellFloor(getWorld(), e.getY());
        if(x < getWorld().getWidth() && y < getWorld().getHeight()) {
            getWorld().addObject(actor, x, y);
            return true;
        } else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        if (o instanceof Actor) {
            Actor actor = (Actor) o;
            world.removeObject(actor);
        }
    }

    public void dragFinished(Object o)
    {
        DragGlassPane drag = DragGlassPane.getInstance();
        worldCanvas.removeMouseListener(drag);
        worldCanvas.removeMouseMotionListener(drag);

        handlerDelegate.dragFinished(o);
    
    }

    /**
     * Resets the world.
     *
     */
    public void reset()
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                handlerDelegate.reset();
                setWorld(null);
            }
        });
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
     * Used to indicate the start of an animation sequence. For use in the collision checker.
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
     * 
     */
    public void finishDrag(Object o)
    {
        // re-enable keylistener after object drag
        getWorldCanvas().addMouseListener(this);
        getWorldCanvas().addKeyListener(this);

        // if the operation was cancelled, add the object back into the
        // world at its original position
        if (!isObjectDropped() && o instanceof Actor) {
            Actor actor = (Actor) o;
            int x = WorldVisitor.toCellFloor(world, getDragBeginX());
            int y = WorldVisitor.toCellFloor(world, getDragBeginY());
            world.addObject(actor, x, y);
            setObjectDropped(true);
        }
    }

}