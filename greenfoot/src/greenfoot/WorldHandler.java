package greenfoot;

import greenfoot.gui.DragGlassPane;
import greenfoot.gui.DragListener;
import greenfoot.gui.DropTarget;
import greenfoot.gui.WorldCanvas;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.SelectionManager;
import greenfoot.gui.classbrowser.role.ClassRole;
import greenfoot.gui.classbrowser.role.GreenfootClassRole;
import greenfoot.localdebugger.LocalObject;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.objectbench.ObjectWrapper;

/**
 * The worldhandler handles the connection between the GreenfootWorld and the
 * WorldCanvas.
 * 
 * @author Poul Henriksen
 * @version $Id: WorldHandler.java 3462 2005-06-20 14:00:42Z polle $
 */
public class WorldHandler
    implements MouseListener, KeyListener, DropTarget, DragListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private GreenfootWorld world;
    private WorldCanvas worldCanvas;
    private SelectionManager classSelectionManager;
    private JLabel worldTitle = new JLabel();
    private int delay;
    private boolean isQuickAddActive;

    // where did the the drag/drop operation begin?
    private int dragBeginX;
    private int dragBeginY;

    /**
     * Whether the object was dropped, or more specifically, whether it does not
     * need to be replaced if the drop is cancelled.
     */
    private boolean objectDropped = true; // true if the object was dropped

    private static WorldHandler instance;
    
    /**
     * Creates a new worldHandler and sets up the connection between worldCanvas
     * and world
     * 
     * @param worldCanvas
     * @param world
     */
    private WorldHandler(WorldCanvas worldCanvas, GreenfootWorld world)
    {

        this.worldCanvas = worldCanvas;
        worldCanvas.addMouseListener(this);
        worldCanvas.addKeyListener(this);
        worldCanvas.setDropTargetListener(this);
        installNewWorld(world);
        DragGlassPane.getInstance().addKeyListener(this);

    }
    
    public synchronized static WorldHandler instance() 
    {
        return instance;
    }

    public static synchronized void initialise(WorldCanvas worldCanvas, GreenfootWorld world) 
    {
        if(instance == null) {
            instance = new WorldHandler(worldCanvas, world);
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

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_DOWN_MASK) != 0)) {
            // GreenfootObject gObject = getObject(e.getX(), e.getY());
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
            GreenfootObject go = getObject(e.getX(), e.getY());
            if (go != null) {
                dragBeginX = go.getX() * world.getCellSize();
                dragBeginY = go.getY() * world.getCellSize();
                int dragOffsetX = dragBeginX - e.getX();
                int dragOffsetY = dragBeginY - e.getY();
                objectDropped = false;
                DragGlassPane.getInstance().startDrag(go, dragOffsetX, dragOffsetY, this);

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

            GreenfootObject obj = getObject(e.getX(), e.getY());
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
    private JPopupMenu makePopupMenu(final GreenfootObject obj)
    {
        JPopupMenu menu = new JPopupMenu();
        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(), new WorldInvokeListener(obj, this),
                Collections.EMPTY_MAP);
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
                ObjectInspector.getInstance(dObj, "", null, null, parent);
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
    private GreenfootObject getObject(int x, int y)
    {
        if (world == null)
            return null;

        Collection objectsThere = world.getObjectsAtPixel(x, y);
        if (objectsThere.size() < 1) {
            return null;
        }

        Iterator iter = objectsThere.iterator();
        GreenfootObject go = null;
        while (iter.hasNext()) {
            go = (GreenfootObject) iter.next();
        }
        return go;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {
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

    /**
     * Returns a list of all objects.
     * 
     */
    public List getGreenfootObjects()
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
        logger.info("KEY TYPED");
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
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

                GreenfootObject go = (GreenfootObject) object;
                int dragOffsetX = 0;
                int dragOffsetY = 0;
                objectDropped = false;
                DragGlassPane.getInstance().startDrag(go, dragOffsetX, dragOffsetY, this);

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
    }

    /**
     * Sets a new world.
     * 
     * @param world
     */
    public void setWorld(GreenfootWorld world)
    {
        installNewWorld(world);
    }

    public void installNewWorld(GreenfootWorld world)
    {
        this.world = world;

        if (world != null) {
            world.addObserver(worldCanvas);
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
                            WorldHandler.this), Collections.EMPTY_MAP);
                    menu.addSeparator();
                    // "inspect" menu item
                    JMenuItem m = getInspectMenuItem(world);
                    menu.add(m);
                    menu.show(worldTitle, e.getX(), e.getY());
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
    
    public GreenfootWorld getWorld() {
        return world;
    }

    public WorldCanvas getWorldCanvas()
    {
        return worldCanvas;
    }

    public boolean drop(Object o, Point p)
    {
        if( o instanceof ObjectDragProxy) {
            //create the real object
            ObjectDragProxy to = (ObjectDragProxy) o;
            to.createRealObject();
            world.removeObject(to);
            objectDropped = true;
            return true;
        }
        else if (o instanceof GreenfootObject) {
            try {
                GreenfootObject go = (GreenfootObject) o;
                int x = (int) p.getX();
                int y = (int) p.getY();
                go.setLocationInPixels(x, y);
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
        if (o instanceof GreenfootObject && world != null) {
            int x = (int) p.getX();
            int y = (int) p.getY();
            
            LocationTracker.instance().setLocation(x, y);
            GreenfootObject go = (GreenfootObject) o;
            world.addObject(go);
            try {
                go.setLocationInPixels(x, y);
            }
            catch (IndexOutOfBoundsException e) {
                LocationTracker.instance().reset();
                world.removeObject(go);
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        LocationTracker.instance().reset();
        if (o instanceof GreenfootObject) {
            GreenfootObject go = (GreenfootObject) o;
            world.removeObject(go);
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
            // world
            // at its original position
            if (!objectDropped && o instanceof GreenfootObject) {
                GreenfootObject go = (GreenfootObject) o;
                go.setLocationInPixels(dragBeginX, dragBeginY);
                world.addObject(go);
                objectDropped = true;
            }
        }
        else if (objectDropped) {
            // Quick-add another object
            quickAddIfActive();
        }
    }
}