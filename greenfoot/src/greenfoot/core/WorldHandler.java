package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.SelectionManager;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;
import greenfoot.localdebugger.LocalObject;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.event.EventListenerList;

import rmiextension.wrappers.RObject;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * The worldhandler handles the connection between the World and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 * @version $Id$
 */
public class WorldHandler
    implements MouseListener, KeyListener, DropTarget, DragListener, ObjectBenchInterface
{
    private World world;
    private WorldCanvas worldCanvas;
    private SelectionManager classSelectionManager;
    private JLabel worldTitle;
    private boolean isQuickAddActive;

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
    
    private GProject project; 
    private EventListenerList listenerList = new EventListenerList();
    private WorldEvent worldEvent;

    
    
    public static synchronized void initialise(WorldCanvas worldCanvas) 
    {
        if(instance == null) {
            instance = new WorldHandler(worldCanvas);
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
     */
    private WorldHandler(WorldCanvas worldCanvas)
    {
        worldTitle = new JLabel();
        worldTitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        worldTitle.setHorizontalAlignment(SwingConstants.CENTER);

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
        this.classSelectionManager = selectionManager;
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
        if (SwingUtilities.isLeftMouseButton(e)) {
            Actor actor = getObject(e.getX(), e.getY());
            if (actor != null) {
                fireObjectEvent(actor);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e)
    {
        maybeShowPopup(e);
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
        maybeShowPopup(e);

    }

    private boolean maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {

            Actor obj = getObject(e.getX(), e.getY());
            if (obj != null) {
                JPopupMenu menu = makePopupMenu(obj);
                // JPopupMenu menu = new JPopupMenu();
                // ObjectWrapper.createMenuItems(menu, ...);
                // new ObjectWrapper();
                // JPopupMenu menu = ObjectTracker.instance().getJPopupMenu(obj,
                // e);
                // menu.setVisible(true);
                menu.show(worldCanvas, e.getX(), e.getY());
            }
            return true;

        }
        return false;
    }

    /**
     * Make a popup menu suitable for calling methods on, inspecting and
     * removing an object in the world.
     */
    private JPopupMenu makePopupMenu(final Actor obj)
    {
        JPopupMenu menu = new JPopupMenu();
        
        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(), new WorldInvokeListener(obj, this, project), new LocalObject(obj), null);
       
        
        menu.addSeparator();

        // "inspect" menu item
        JMenuItem m = getInspectMenuItem(obj);
        menu.add(m);

        // "remove" menu item
        m = new JMenuItem("Remove");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                world.removeObject(obj);
                repaint();
            }
        });
        menu.add(m);
        return menu;
    }

    /**
     * Create a menu item to inspect an object.
     */
    private JMenuItem getInspectMenuItem(final Object obj)
    {
        JMenuItem m = new JMenuItem("Inspect");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                JFrame parent = (JFrame) worldCanvas.getTopLevelAncestor();
                DebuggerObject dObj = new LocalObject(obj);
                String instanceName = "";
                try {
                    RObject rObject = ObjectTracker.getRObject(obj);
                    instanceName = rObject.getInstanceName();
                }
                catch (ProjectNotOpenException e1) {
                    e1.printStackTrace();
                }
                catch (PackageNotFoundException e1) {
                    e1.printStackTrace();
                }
                catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                project.getInspectorInstance(dObj, instanceName, null, null, parent);
            }
        });
        return m;
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
    private Actor getObject(int x, int y)
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
        isQuickAddActive = false;
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
    	processInputEvent(e);
        keyboardManager.keyPressed(e);
    }

    private void processInputEvent(InputEvent e)
    {
        if( ! isQuickAddActive) {
    		isQuickAddActive = e.isShiftDown();
    		quickAddIfActive();
    	}
    }

    private void quickAddIfActive()
    {
        if (isQuickAddActive) {
            ClassView cls = (ClassView) classSelectionManager.getSelected();
            if (cls != null && cls.getRole() instanceof GreenfootClassRole) {
                GreenfootClassRole role = (GreenfootClassRole) cls.getRole();
                Object object = role.createObjectDragProxy();//cls.createInstance();

                Actor actor = (Actor) object;
                int dragOffsetX = 0;
                int dragOffsetY = 0;
                objectDropped = false;
                DragGlassPane.getInstance().startDrag(actor, dragOffsetX, dragOffsetY, this, worldCanvas, false);
                
                // On the mac, the glass pane doesn't seem to receive
                // mouse move events; the shift/move is treated like a drag
                worldCanvas.addMouseMotionListener(DragGlassPane.getInstance());
                worldCanvas.addMouseListener(DragGlassPane.getInstance());
            }
        }
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
        if (isQuickAddActive) {
            isQuickAddActive = e.isShiftDown();
        }
        keyboardManager.keyReleased(e);
    }

    
    /**
     * Attaches a project to this handler.
     */
    public void attachProject(GProject project)
    {
        this.project = project;
    }
            
    /**
     * Sets a new world.
     */
    public void setWorld(World world)
    {
        synchronized (this) {
            if (this.world != null) {
                fireWorldRemovedEvent();
            }
            this.world = world;
        }
        
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                World world = WorldHandler.this.world;
                if (world != null) {
                    worldTitle.setText(world.getClass().getName());
                }
                worldCanvas.setWorld(world); // TODO consider removing this and only
                // rely on observer
                worldTitle.setEnabled(true);
                MouseListener listeners[] = worldTitle.getMouseListeners();
                for (int i = 0; i < listeners.length; i++) {
                    worldTitle.removeMouseListener(listeners[i]);
                }

                worldTitle.addMouseListener(new MouseAdapter() {
                    public void mouseReleased(MouseEvent e)
                    {
                        maybeShowPopup(e);
                    }

                    public void mousePressed(MouseEvent e)
                    {
                        maybeShowPopup(e);
                    }

                    private void maybeShowPopup(MouseEvent e)
                    {
                        Object world = WorldHandler.this.world;
                        if (e.isPopupTrigger() && world != null) {
                            JPopupMenu menu = new JPopupMenu();
                            
                            ObjectWrapper.createMethodMenuItems(menu, world.getClass(), new WorldInvokeListener(world,
                                    WorldHandler.this, project), new LocalObject(world), null);
                            menu.addSeparator();
                            // "inspect" menu item
                            JMenuItem m = getInspectMenuItem(world);
                            menu.add(m);
                            menu.show(worldTitle, e.getX(), e.getY());
                        }
                    }
                });
                
                if (world != null) {
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
        return worldTitle;
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
                if (actor.getWorld() == null) {
                    getWorld().addObject(actor, x, y);
                }
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
            catch (IndexOutOfBoundsException e) {
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

        if (!isQuickAddActive) {
            // re-enable keylistener after object drag
            worldCanvas.addMouseListener(this);
            worldCanvas.addKeyListener(this);

            // if the operation was cancelled, add the object back into the
            // world at its original position
            if (!objectDropped && o instanceof Actor) {
                Actor actor = (Actor) o;
                int x = WorldVisitor.toCellFloor(getWorld(), dragBeginX);
                int y = WorldVisitor.toCellFloor(getWorld(), dragBeginY);
                getWorld().addObject(actor, x,y);
                objectDropped = true;
            }
        }
        else if (objectDropped) {
            // Quick-add another object
            quickAddIfActive();
        }
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
                project.removeAllInspectors();
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
    
    protected void fireWorldRemovedEvent()
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

    /**
     * Fire an object event for the named object. This will
     * notify all listeners that have registered interest for
     * notification on this event type.
     */
    public void fireObjectEvent(Actor actor)
    {
        class GNamedValue implements NamedValue {
            private String name;
            public GNamedValue(String instanceName)
            {
                name = instanceName;
            }

            public JavaType getGenType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public String getName()
            {
                return name;
            }

            public boolean isFinal()
            {
                // TODO Auto-generated method stub
                return false;
            }

            public boolean isInitialized()
            {
                return true;
            }            
        }
        GNamedValue value =null;
        try {
            RObject rObj = ObjectTracker.getRObject(actor);
            value =  new GNamedValue(rObj.getInstanceName());
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
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
        } 
        // guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {   // I don't understand this - why step 2? (mik)
            if (listeners[i] == ObjectBenchListener.class) {
                ((ObjectBenchListener)listeners[i+1]).objectEvent(
                        new ObjectBenchEvent(this,
                                ObjectBenchEvent.OBJECT_SELECTED, value));
            }
        }
    }
    
    /**
     * Add listener to recieve events when objects in the world are clicked.
     * @param listener
     */
    public void addObjectBenchListener(ObjectBenchListener listener)
    {
        listenerList.add(ObjectBenchListener.class, listener);
    }
    
    
    /**
     * Add listener to recieve events when objects in the world are clicked.
     * @param listener
     */
    public void removeObjectBenchListener(ObjectBenchListener listener)
    {
        listenerList.remove(ObjectBenchListener.class, listener);
    }
    
    /* (non-Javadoc)
     * @see bluej.debugmgr.objectbench.ObjectBenchInterface#hasObject(java.lang.String)
     */
    public boolean hasObject(String name)
    {
        return false;
    }
}