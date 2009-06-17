/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.ObjectTracker;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.GProject;
import greenfoot.core.ProjectProperties;
import greenfoot.core.WorldHandler;
import greenfoot.core.WorldInvokeListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.gui.input.InputManager;
import greenfoot.localdebugger.LocalObject;
import greenfoot.platforms.WorldHandlerDelegate;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import bluej.utility.Debug;


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

    private final static String missingConstructorTitle = Config.getString("world.missing.constructor.title");
    private final static String missingConstructorMsg = Config.getString("world.missing.constructor.msg");
    private final static String continueButtonText = Config.getString("greenfoot.continue");

    private WorldHandler worldHandler;

    private GProject project;
    
    private GreenfootFrame frame;

    private JLabel worldTitle;

    private String lastWorldClass; 
    
    public WorldHandlerDelegateIDE(GreenfootFrame frame)
    {
        worldTitle = new JLabel();
        worldTitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        worldTitle.setHorizontalAlignment(SwingConstants.CENTER);
        this.frame = frame;
    }

    /**
     * Make a popup menu suitable for calling methods on, inspecting and
     * removing an object in the world.
     */
    private JPopupMenu makePopupMenu(final Actor obj)
    {
        JPopupMenu menu = new JPopupMenu();

        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(), new WorldInvokeListener(obj, this, frame, project),
                LocalObject.getLocalObject(obj), null);

        // "inspect" menu item
        JMenuItem m = getInspectMenuItem(obj);
        menu.add(m);

        // "remove" menu item
        m = new JMenuItem(Config.getString("world.handlerDelegate.remove"));
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
        JMenuItem m = new JMenuItem(Config.getString("world.handlerDelegate.inspect"));
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
                frame.getInspectorInstance(dObj, instanceName, null, null, parent);
            }
        });
        m.setFont(PrefMgr.getStandoutMenuFont());
        m.setForeground(envOpColour);
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


    public void discardWorld(World world) {        
        // Remove the  world and actors from the remote object caches
        ObjectTracker.forgetRObject(world);
        List<Actor> oldActors = new ArrayList<Actor>(WorldVisitor.getObjectsListInPaintOrder(world));
        for (Iterator<Actor> i = oldActors.iterator(); i.hasNext();) {
            Actor oldActor = i.next();
            ObjectTracker.forgetRObject(oldActor);
        }
    }
    
    public void setWorld(final World oldWorld, final World newWorld)
    {
        if (newWorld != null) {
            lastWorldClass = newWorld.getClass().getName();
        }
        if (oldWorld != null) {
            discardWorld(oldWorld);
        }

        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                createWorldTitle(newWorld);
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
                                    newWorld, WorldHandlerDelegateIDE.this, frame, project), LocalObject
                                    .getLocalObject(newWorld), null);
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
    
    /**
     * Creates and sets the title of the world in the UI.
     * 
     * @param newWorld The world for which a title should be set
     */
    private void createWorldTitle(final World newWorld)
    {
        if (newWorld == null) {
            return;
        }
        String className = newWorld.getClass().getName();
        String objName = className.substring(0, 1).toLowerCase() + className.substring(1);
        worldTitle.setText(objName);
        worldTitle.setEnabled(true);
    }
    
    public void dragFinished(Object o)
    {
        worldHandler.finishDrag(o);
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

    public Component getWorldTitle()
    {
        return worldTitle;
    }

    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    public void instantiateNewWorld()
    {
        Class<?> cls = getLastWorldClass();
        if(cls == null) {
            try {
            	List<Class<?>> worldClasses = project.getDefaultPackage().getWorldClasses();
            	if(worldClasses.isEmpty() ) {
            		return;
            	}
            	cls = worldClasses.get(0);
            }
            catch (ProjectNotOpenException pnoe) {
            	return;
            }
            catch (RemoteException re) {
            	re.printStackTrace();
            	return;
            }
        }
        
        try {
            World w = (World) cls.newInstance();      
            worldHandler.setWorld(w);
        }
        catch (LinkageError e) { }
        catch (InstantiationException e) {
            showMissingConstructorDialog();
        }
        catch (IllegalAccessException e) {
            showMissingConstructorDialog();
        }
        catch (Throwable ise) {
            // This can happen if a static initializer block throws a Throwable.
            // Or for other reasons.
            ise.printStackTrace();
        }
    }

    private void showMissingConstructorDialog()
    {
        JButton button = new JButton(continueButtonText);
        MessageDialog msgDialog = new MessageDialog(frame, missingConstructorMsg, missingConstructorTitle, 50, new JButton[]{button});
        msgDialog.display();
    }

    public Class<?> getLastWorldClass()
    {
        if(lastWorldClass == null) {
            return null;
        }
        
        try {
        	List<Class<?>> worldClasses = project.getDefaultPackage().getWorldClasses();

        	//Has to be one of the currently instantiable world classes.
        	for (Class<?> worldClass : worldClasses) {
        		if(worldClass.getName().equals(lastWorldClass)) {
        			return worldClass;
        		}                
        	}
        }
        catch (ProjectNotOpenException pnoe) {}
        catch (RemoteException re) {
        	re.printStackTrace();
        }
        
        return null;
    }

    public InputManager getInputManager()
    {
        InputManager inputManager = new InputManager();       
        DragGlassPane.getInstance().addMouseListener(inputManager);
        DragGlassPane.getInstance().addMouseMotionListener(inputManager);
        DragGlassPane.getInstance().addKeyListener(inputManager);        
        inputManager.setIdleListeners(worldHandler, worldHandler, worldHandler);
        inputManager.setDragListeners(null, DragGlassPane.getInstance(), DragGlassPane.getInstance());
        inputManager.setMoveListeners(worldHandler, worldHandler, worldHandler);
        
        return inputManager;
    }
}
