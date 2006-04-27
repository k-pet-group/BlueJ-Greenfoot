package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.actions.PauseSimulationAction;
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import rmiextension.wrappers.RObject;
import bluej.debugger.DebuggerObject;
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
    implements MouseListener, KeyListener, DropTarget, DragListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private World world;
    private WorldCanvas worldCanvas;
    private SelectionManager classSelectionManager;
    private JLabel worldTitle = new JLabel();
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
    
    /**
     * Creates a new worldHandler and sets up the connection between worldCanvas
     * and world.
     */
    private WorldHandler(GProject project, WorldCanvas worldCanvas, World world)
    {

        this.project = project;
        this.worldCanvas = worldCanvas;
        worldEvent = new WorldEvent(this);
        worldCanvas.addMouseListener(this);
        worldCanvas.addKeyListener(this);
        worldCanvas.setDropTargetListener(this);
        LocationTracker.instance().setComponent(worldCanvas);
        installNewWorld(world);
        keyboardManager = new KeyboardManager();
        DragGlassPane.getInstance().addKeyListener(this);
    }
    
    public synchronized static WorldHandler instance() 
    {
        return instance;
    }

    public static synchronized void initialise(GProject project, WorldCanvas worldCanvas, World world) 
    {
        if(instance == null) {
            instance = new WorldHandler(project, worldCanvas, world);
        } else {
            throw (new IllegalStateException("Can only intiliase this singleton once."));
        }
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
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_DOWN_MASK) != 0)) {
            // Actor gObject = getObject(e.getX(), e.getY());
            // RObject rObject = ObjectTracker.instance().getRObject(gObject);
            // TODO: inspect rObject

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
        worldCanvas.requestFocusInWindow();
        processInputEvent(e);
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

    /**
     * Returns a list of all objects.
     * 
     */
    public List getActors()
    {
        return world.getObjects(null);
    }
    
    public Object getWorldLock() {
        return world;
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
     * Sets a new world.
     * 
     * @param world
     */
    public void setWorld(World world)
    {
        installNewWorld(world);
    }

    public void installNewWorld(World world)
    {
        if(this.world != null) {
            PauseSimulationAction.getInstance().actionPerformed(null);
        }
        
        this.world = world;

        if (world != null) {
            worldTitle.setText(world.getClass().getName());
        }
        worldCanvas.setWorld(world); // TODO consider removing this and only
        // rely on observer
        worldTitle.setEnabled(true);
        worldTitle.setHorizontalAlignment(SwingConstants.CENTER);
        MouseListener listeners[] = worldTitle.getMouseListeners();
        for (int i = 0; i < listeners.length; i++) {
            MouseListener listener = listeners[i];
            worldTitle.removeMouseListener(listener);
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
        fireWorldCreatedEvent();

    }

    /**
     * @return
     */
    public Component getWorldTitle()
    {
        return worldTitle;
    }
    
    public World getWorld() {
        return world;
    }

    public WorldCanvas getWorldCanvas()
    {
        return worldCanvas;
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
            try {
                Actor actor = (Actor) o;
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

    public boolean drag(Object o, Point p)
    {
        if (o instanceof Actor && world != null) {
            int x = (int) p.getX();
            int y = (int) p.getY();
            Actor actor = (Actor) o;
            
            try {
                if (actor.getWorld() == null) {
                    addObjectAtPixel(actor, x, y);
                }
                int oldX = actor.getX();
                int oldY = actor.getY();
                ActorVisitor.setLocationInPixels(actor, x, y);
                if (oldX != actor.getX() || oldY != actor.getY()) {
                    repaint();
                }      
            }
            catch (IndexOutOfBoundsException e) {
                world.removeObject(actor);
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Adds the object at the specified pixel location.
     * 
     * @return true if location changed
     * @throws IndexOutOfBoundsException If the coordinates are outside the
     *             bounds of the world. Note that a wrapping world has no
     *             bounds.
     */
    public boolean addObjectAtPixel(Actor actor, int x, int y)
    {
        int xCell = WorldVisitor.toCellFloor(world, x);
        int yCell = WorldVisitor.toCellFloor(world, y);
        if (actor.getWorld() != null) {
            if (x == actor.getX() && y == actor.getY()) {
                return false;
            }
        }
        world.addObject(actor, xCell, yCell);
        return true;
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
                addObjectAtPixel(actor, dragBeginX, dragBeginY);
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
        project.removeAllInspectors();
        setWorld(null);
        fireWorldRemovedEvent();
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
}