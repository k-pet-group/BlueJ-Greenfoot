package greenfoot.platforms.ide;

import greenfoot.Actor;
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.LocationTracker;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.core.WorldInvokeListener;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.SelectionManager;
import greenfoot.gui.classbrowser.role.ActorClassRole;
import greenfoot.localdebugger.LocalObject;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import rmiextension.wrappers.RObject;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.prefmgr.PrefMgr;


/**
 * Implementation for running in the Greenfoot IDE.
 * 
 * @author Poul Henriksen
 *
 */
public class WorldHandlerDelegateIDE
    implements WorldHandlerDelegate, ObjectBenchInterface
{
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private SelectionManager classSelectionManager;

    private boolean isQuickAddActive;

    private WorldHandler worldHandler;

    private GProject project;

    private JLabel worldTitle;

    private String lastWorldClass; 
    
    public WorldHandlerDelegateIDE()
    {
        worldTitle = new JLabel();
        worldTitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        worldTitle.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * Sets the selection manager.
     * 
     * @param selectionManager
     */
    public void setSelectionManager(Object selectionManager)
    {
        this.classSelectionManager = (SelectionManager) selectionManager;
    }

    /**
     * Make a popup menu suitable for calling methods on, inspecting and
     * removing an object in the world.
     */
    private JPopupMenu makePopupMenu(final Actor obj)
    {
        JPopupMenu menu = new JPopupMenu();

        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(), new WorldInvokeListener(obj, this, project),
                LocalObject.getLocalObject(obj), null);

        menu.addSeparator();

        // "inspect" menu item
        JMenuItem m = getInspectMenuItem(obj);
        m.setFont(PrefMgr.getStandoutMenuFont());
        m.setForeground(envOpColour);
        menu.add(m);

        // "remove" menu item
        m = new JMenuItem("Remove");
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                worldHandler.getWorld().removeObject(obj);
                worldHandler.repaint();
            }
        });
        m.setFont(PrefMgr.getStandoutMenuFont());
        m.setForeground(envOpColour);
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
                JFrame parent = (JFrame) worldHandler.getWorldCanvas().getTopLevelAncestor();
                DebuggerObject dObj = LocalObject.getLocalObject(obj);
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
                catch (bluej.extensions.ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
                project.getInspectorInstance(dObj, instanceName, null, null, parent);
            }
        });
        return m;
    }

    /**
     * Shows the popup menu if the mouseevent is a popup trigger.
     */
    public boolean maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            Actor obj = worldHandler.getObject(e.getX(), e.getY());
            if (obj != null) {
                JPopupMenu menu = makePopupMenu(obj);
                // JPopupMenu menu = new JPopupMenu();
                // ObjectWrapper.createMenuItems(menu, ...);
                // new ObjectWrapper();
                // JPopupMenu menu = ObjectTracker.instance().getJPopupMenu(obj,
                // e);
                // menu.setVisible(true);
                menu.show(worldHandler.getWorldCanvas(), e.getX(), e.getY());
            }
            return true;

        }
        return false;
    }

    public void setQuickAddActive(boolean b)
    {
        isQuickAddActive = b;

    }

    /**
     * Process key event for quickadd
     * 
     */
    public void processKeyEvent(KeyEvent e)
    {
        if (!isQuickAddActive) {
            isQuickAddActive = e.isShiftDown();
            if (isQuickAddActive) {
                // When shift is pressed any existing drags should be cancelled.
                // For instance dragging object instantiated via interactively
                // calling the constructor.
                DragGlassPane.getInstance().cancelDrag();
            }
            quickAddIfActive();
        }
    }

    /**
     * Do a "quick add" of the currently selected class, *iff* quick-add is "active"
     * (i.e. if shift is currently pressed).
     */
    private void quickAddIfActive()
    {
        if (isQuickAddActive) {
            ClassView cls = (ClassView) classSelectionManager.getSelected();
            if (cls != null && cls.getRole() instanceof ActorClassRole) {
                ActorClassRole role = (ActorClassRole) cls.getRole();
                Actor actor = role.createObjectDragProxy();// cls.createInstance();

                int dragOffsetX = 0;
                int dragOffsetY = 0;
                worldHandler.setObjectDropped(false);
                DragGlassPane.getInstance().startDrag(actor, dragOffsetX, dragOffsetY, worldHandler, worldHandler.getWorldCanvas(), false);

                // On the mac, the glass pane doesn't seem to receive
                // mouse move events; the shift/move is treated like a drag
                worldHandler.getWorldCanvas().addMouseMotionListener(DragGlassPane.getInstance());
                worldHandler.getWorldCanvas().addMouseListener(DragGlassPane.getInstance());
            }
        }
    }

    public void keyReleased(KeyEvent e)
    {
        if (isQuickAddActive) {
            isQuickAddActive = e.isShiftDown();
        }
    }

    public void setWorld(final World oldWorld, final World newWorld)
    {
        if (newWorld != null) {
            lastWorldClass = newWorld.getClass().getName();
        }
        if (oldWorld != null) {
            // Remove the old world and actors from the remote object caches
            ObjectTracker.forgetRObject(oldWorld);
            List<Actor> oldActors = new ArrayList<Actor>(WorldVisitor.getObjectsList(oldWorld));
            for (Iterator<Actor> i = oldActors.iterator(); i.hasNext();) {
                Actor oldActor = i.next();
                ObjectTracker.forgetRObject(oldActor);
            }

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    worldHandler.fireWorldRemovedEvent();
                }
            });
            Simulation.getInstance().setPaused(true);
        }

        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                if (newWorld != null) {
                    worldTitle.setText(newWorld.getClass().getName());
                }
                worldHandler.getWorldCanvas().setWorld(newWorld); // TODO consider removing this and only rely on server
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
                        if (e.isPopupTrigger() && worldHandler.getWorld() != null) {
                            JPopupMenu menu = new JPopupMenu();

                            ObjectWrapper.createMethodMenuItems(menu, newWorld.getClass(), new WorldInvokeListener(
                                    newWorld, WorldHandlerDelegateIDE.this, project), LocalObject
                                    .getLocalObject(newWorld), null);
                            menu.addSeparator();
                            // "inspect" menu item
                            JMenuItem m = getInspectMenuItem(newWorld);
                            menu.add(m);
                            menu.show(worldTitle, e.getX(), e.getY());
                        }
                    }
                });

            }
        });
    }

    public void dragFinished(Object o)
    {
        if (!isQuickAddActive) {
            worldHandler.finishDrag(o);
        }
        else if (worldHandler.isObjectDropped()) {
            // Quick-add another object
            quickAddIfActive();
        }
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
        catch (bluej.extensions.ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        // guaranteed to return a non-null array
        Object[] listeners = worldHandler.getListenerList().getListenerList();
        // process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) { 
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
        worldHandler.getListenerList().add(ObjectBenchListener.class, listener);
    }
    
    
    /**
     * Add listener to recieve events when objects in the world are clicked.
     * @param listener
     */
    public void removeObjectBenchListener(ObjectBenchListener listener)
    {
        worldHandler.getListenerList().remove(ObjectBenchListener.class, listener);
    }
    
    /* (non-Javadoc)
     * @see bluej.debugmgr.objectbench.ObjectBenchInterface#hasObject(java.lang.String)
     */
    public boolean hasObject(String name)
    {
        return false;
    }

    public void mouseClicked(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            Actor actor = worldHandler.getObject(e.getX(), e.getY());
            if (actor != null) {
                fireObjectEvent(actor);
            }
        }
    }

    public void attachProject(Object project)
    {
        this.project = (GProject) project;

        ProjectProperties probs = this.project.getProjectProperties();
        lastWorldClass = probs.getString("world.lastInstantiated");
    }

    public void reset()
    {
        project.removeAllInspectors();
    }

    public Component getWorldTitle()
    {
        return worldTitle;
    }

    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    public World instantiateNewWorld()
    {
        Class cls = getLastWorldClass();
        if(cls == null) {
            List<Class> worldClasses = GreenfootMain.getInstance().getPackage().getWorldClasses();
            if(worldClasses.isEmpty() ) {
                return null;
            }
            cls = worldClasses.get(0);
        }
        
        try {
            World w = (World) cls.newInstance();            
            ActorInstantiationListener invocationListener = GreenfootMain.getInstance().getInvocationListener();
            if(invocationListener != null) {
                invocationListener.localObjectCreated(w, LocationTracker.instance().getMouseButtonEvent());
            }
            return w;
        }
        catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public Class getLastWorldClass()
    {
        if(lastWorldClass == null) {
            return null;
        }
        List<Class> worldClasses = GreenfootMain.getInstance().getPackage().getWorldClasses();
        
        //Has to be one of the currently instantiable world classes.
        for (Class worldClass : worldClasses) {
            if(worldClass.getName().equals(lastWorldClass)) {
                return worldClass;
            }                
        }        
        return null;
    }

}
