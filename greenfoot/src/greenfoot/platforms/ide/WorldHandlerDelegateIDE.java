/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013,2014  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.actions.SaveWorldAction;
import greenfoot.core.ClassStateManager;
import greenfoot.core.GClass;
import greenfoot.core.GNamedValue;
import greenfoot.core.GProject;
import greenfoot.core.ImageCache;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.core.WorldInvokeListener;
import greenfoot.event.SimulationUIListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.input.InputManager;
import greenfoot.localdebugger.LocalObject;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.record.GreenfootRecorder;
import greenfoot.record.InteractionListener;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import rmiextension.wrappers.RObject;
import rmiextension.wrappers.RProject;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.views.CallableView;


/**
 * Implementation for running in the Greenfoot IDE.
 * 
 * @author Poul Henriksen
 */
public class WorldHandlerDelegateIDE
    implements WorldHandlerDelegate, ObjectBenchInterface, InteractionListener, SimulationUIListener
{
    protected final Color envOpColour = Config.ENV_COLOUR;

    private final static int WORLD_INITIALISING_TIMEOUT = 4000;

    private WorldHandler worldHandler;

    private GProject project;
    
    private GreenfootFrame frame;
    private InspectorManager inspectorManager;
    
    // Records actions manually performed on the world:
    private GreenfootRecorder greenfootRecorder;
    private SaveWorldAction saveWorldAction;

    private boolean worldInitialising;
    private long startedInitialisingAt;
    private boolean worldInvocationError;
    private boolean missingConstructor;
    private boolean vmRestarted;
    
    public WorldHandlerDelegateIDE(GreenfootFrame frame, InspectorManager inspectorManager,
            ClassStateManager classStateManager)
    {
        this.frame = frame;
        this.inspectorManager = inspectorManager;
        greenfootRecorder = new GreenfootRecorder();
        saveWorldAction = new SaveWorldAction(greenfootRecorder, classStateManager);
        saveWorldAction.setRecordingValid(false);
    }

    /**
     * Make a popup menu suitable for calling methods on, inspecting and
     * removing an object in the world.
     */
    public JPopupMenu makeActorPopupMenu(final Actor obj)
    {
        JPopupMenu menu = new JPopupMenu();

        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(),
                new WorldInvokeListener(frame, obj, this, inspectorManager, this, project),
                LocalObject.getLocalObject(obj), null, false);

        // "inspect" menu item
        JMenuItem m = getInspectMenuItem(obj);
        menu.add(m);

        // "remove" menu item
        m = new JMenuItem(Config.getString("world.handlerDelegate.remove"));
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                worldHandler.getWorld().removeObject(obj);
                removedActor(obj);
                worldHandler.repaint();
            }
        });
        m.setFont(PrefMgr.getStandoutMenuFont());
        m.setForeground(envOpColour);
        menu.add(m);
        return menu;
    }

    /**
     * Create a pop-up allowing the user to call methods, inspect and "Save the World"
     * on the World object.
     */
    public JPopupMenu makeWorldPopupMenu(final World world)
    {
        if (world == null) {
            return null;
        }
        
        JPopupMenu menu = new JPopupMenu();
        
        ObjectWrapper.createMethodMenuItems(menu, world.getClass(),
                new WorldInvokeListener(frame, world, WorldHandlerDelegateIDE.this,
                        inspectorManager, this, project),
                LocalObject.getLocalObject(world), null, false);
        // "inspect" menu item
        JMenuItem m = getInspectMenuItem(world);

        // "save the world" menu item
        JMenuItem saveTheWorld = new JMenuItem(saveWorldAction);
        saveTheWorld.setFont(PrefMgr.getStandoutMenuFont());
        saveTheWorld.setForeground(envOpColour);
        
        menu.add(m);
        menu.add(saveTheWorld);
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
                    if (rObject != null) {
                        instanceName = rObject.getInstanceName();
                    }
                }
                catch (RemoteException e1) {
                    Debug.reportError("Could not get instance name for inspection", e1);
                }
                inspectorManager.getInspectorInstance(dObj, instanceName, null, null, parent);
            }
        });
        m.setFont(PrefMgr.getStandoutMenuFont());
        m.setForeground(envOpColour);
        return m;
    }

    /**
     * Shows a pop-up menu if the MouseEvent is a pop-up trigger.
     * Pop-up menu depends on if the MouseEvent occurred on an Actor
     * or the world.
     */
    @Override
    public boolean maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            JPopupMenu menu;
            Actor obj = worldHandler.getObject(e.getX(), e.getY());
            // if null then the user clicked on the world
            if (obj == null) {
                menu = makeWorldPopupMenu(worldHandler.getWorld());
            }
            else {
                menu = makeActorPopupMenu(obj);
            }
            
            if (menu != null) {
                menu.show(worldHandler.getWorldCanvas(), e.getX(), e.getY());
            }
            return true;

        }
        return false;
    }
    
    /**
     * Displays the world pop-up menu in the location specified
     * by the parameter MouseEvent.
     * @param e Used to get the component to display in as well as the x
     * and y coordinates.
     */
    public void showWorldPopupMenu(MouseEvent e)
    {
        JPopupMenu menu = makeWorldPopupMenu(worldHandler.getWorld());
        if (menu != null) {
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Clear the world from the cache.
     * @param world  World to discard
     */
    @Override
    public void discardWorld(World world)
    {        
        ObjectTracker.clearRObjectCache();
    }
    
    @Override
    public void setWorld(final World oldWorld, final World newWorld)
    {
        worldInvocationError = false;
        greenfootRecorder.clearCode(false);
        greenfootRecorder.setWorld(newWorld);
        if (oldWorld != null) {
            discardWorld(oldWorld);
        }
        
        GClass lastWorld = null;
        if (project != null && newWorld != null) {
            String lastWorldClass = newWorld.getClass().getName();
            if (lastWorldClass != null) {
                lastWorld = project.getDefaultPackage().getClass(lastWorldClass);
            }
        }

        saveWorldAction.setLastWorldGClass(lastWorld);
    }
    
    /**
     * Fire an object event for the named object. This will
     * notify all listeners that have registered interest for
     * notification on this event type.
     */
    public void fireObjectEvent(Actor actor)
    {
        GNamedValue value =null;
        try {
            RObject rObj = ObjectTracker.getRObject(actor);
            if (rObj != null) {
                value =  new GNamedValue(rObj.getInstanceName(), null);
            }
        }
        catch (RemoteException e) {
            Debug.reportError("Error when trying to get object instance name", e);
        }
        
        if (value != null) {
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
    }
    
    @Override
    public void addObjectBenchListener(ObjectBenchListener listener)
    {
        worldHandler.getListenerList().add(ObjectBenchListener.class, listener);
    }
    
    @Override
    public void removeObjectBenchListener(ObjectBenchListener listener)
    {
        worldHandler.getListenerList().remove(ObjectBenchListener.class, listener);
    }
    
    @Override
    public boolean hasObject(String name)
    {
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            Actor actor = worldHandler.getObject(e.getX(), e.getY());
            if (actor != null) {
                fireObjectEvent(actor);
            }
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e)
    {
        // While dragging, other methods set the mouse cursor instead:
        if (false == worldHandler.isDragging()) {
            Actor actor = worldHandler.getObject(e.getX(), e.getY());
            if (actor == null) {
                worldHandler.getWorldCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            else {
                worldHandler.getWorldCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
    }

    /**
     * Attach to a particular project. This should be called whenever the project
     * changes.
     */
    public void attachProject(Object project)
    {
        this.project = (GProject) project;
    }

    @Override
    public void setWorldHandler(WorldHandler handler)
    {
        this.worldHandler = handler;
    }

    @Override
    public void instantiateNewWorld()
    {
        final RProject rProject = project.getRProject();
        try {
            if (!rProject.isVMRestarted()) {
                greenfootRecorder.reset();
                worldInitialising = true;
                worldInvocationError = false;
                Class<? extends World> cls = getLastWorldClass();
                GClass lastWorldGClass = getLastWorldGClass();

                if (lastWorldGClass == null) {
                    // Either the last instantiated world no longer exists, or there is no record
                    // of a last instantiated world class. Find a world arbitrarily.
                    List<Class<? extends World>> worldClasses = project.getDefaultPackage().getWorldClasses();
                    if(worldClasses.isEmpty() ) {
                        return;
                    }

                    for (Class<? extends World> wclass : worldClasses) {
                        try {
                            wclass.getConstructor(new Class<?>[0]);
                            cls = wclass;
                            break;
                        }
                        catch (LinkageError le) { }
                        catch (NoSuchMethodException nsme) { }
                    }
                    if (cls == null) {
                        // Couldn't find a world with a suitable constructor
                        missingConstructor = true;
                        return;
                    }
                }

                if (cls == null) {
                    // Can occur if last instantiated world class is not compiled.
                    return;
                }
                
                startedInitialisingAt = System.currentTimeMillis();
                frame.updateBackgroundMessage();

                final Timer timer = new Timer(WORLD_INITIALISING_TIMEOUT, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        if (worldInitialising) {
                            frame.updateBackgroundMessage();
                        }
                    }
                });
                timer.setRepeats(false);
                timer.start();
                
                final Class<? extends World> icls = cls;
                Simulation.getInstance().runLater(new Runnable() {

                    @Override
                    public void run()
                    {
                        try {
                            Constructor<?> cons = icls.getConstructor(new Class<?>[0]);
                            WorldHandler.getInstance().clearWorldSet();
                            World newWorld = (World) Simulation.newInstance(cons);
                            if (! WorldHandler.getInstance().checkWorldSet()) {
                                ImageCache.getInstance().clearImageCache();
                                WorldHandler.getInstance().setWorld(newWorld);
                            }
                            saveWorldAction.setRecordingValid(true);
                            project.setLastWorldClassName(icls.getName());
                        }
                        catch (LinkageError e) { }
                        catch (NoSuchMethodException nsme) {
                            missingConstructor = true;
                        }
                        catch (InstantiationException e) {
                            // abstract class; shouldn't happen
                        }
                        catch (IllegalAccessException e) {
                            missingConstructor = true;
                        }
                        catch (InvocationTargetException ite) {
                            // This can happen if a static initializer block throws a Throwable.
                            // Or for other reasons.
                            ite.getCause().printStackTrace();
                            worldInvocationError = true;
                            frame.updateBackgroundMessage();
                        }
                        catch (Exception e) {
                            System.err.println("Exception during World initialisation:");
                            e.printStackTrace();
                            worldInvocationError = true;
                            frame.updateBackgroundMessage();
                        }
                        worldInitialising = false;
                        timer.stop();
                    }
                });
            }
            else {
                vmRestarted = true;
                rProject.setVmRestarted(false);
            }
        }
        catch (RemoteException ex) {
            Debug.reportError("RemoteException checking VM state in WorldHandlerDelegateIDE", ex);
        }
    }

    /**
     * Get the last-instantiated world class if known and possible. May return null.
     */
    public GClass getLastWorldGClass()
    {
        if (project == null) {
            return null;
        }
        
        String lastWorldClass = project.getLastWorldClassName();
        if(lastWorldClass == null) {
            return null;
        }
        
        return project.getDefaultPackage().getClass(lastWorldClass);
    }
    
    /**
     * Get the last world class that was instantiated, if it can (still) be instantiated.
     * May return null.
     */
    @SuppressWarnings("unchecked")
    private Class<? extends World> getLastWorldClass()
    {
        GClass gclass = getLastWorldGClass();
        if (gclass != null) {
            Class<? extends World> rclass = (Class<? extends World>) gclass.getJavaClass();
            if (GreenfootUtil.canBeInstantiated(rclass)) {
                return  rclass;
            }
        }

        return null;
    }

    @Override
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
    
    @Override
    public void beginCallExecution(CallableView callableView)
    {
        if (callableView.isConstructor() && World.class.isAssignableFrom(callableView.getDeclaringView().getViewClass())) {
            worldInitialising = true;
            greenfootRecorder.reset();
            saveWorldAction.setRecordingValid(true);            
        }
    }
    
    @Override
    public void worldConstructed(Object world)
    {
        worldInitialising = false;
        if (project != null) {
            project.setLastWorldClassName(world.getClass().getName());
        }
    }
    
    @Override
    public void addActor(Actor actor, int x, int y)
    {
        greenfootRecorder.addActorToWorld(actor, x, y);
    }

    @Override
    public void createdActor(Object actor, String[] args, JavaType[] argTypes)
    {
        greenfootRecorder.createActor(actor, args, argTypes);
    }

    @Override
    public void methodCall(Object obj, String actorName, Method method, String[] args, JavaType[] argTypes)
    {
        if (obj != null) {
            greenfootRecorder.callActorMethod(obj, actorName, method, args, argTypes);
        }
        else {
            greenfootRecorder.callStaticMethod(actorName, method, args, argTypes);
        }
    }
    
    @Override
    public void actorDragged(Actor actor, int xCell, int yCell)
    {
        greenfootRecorder.moveActor(actor, xCell, yCell);
    }

    @Override
    public void removedActor(Actor obj)
    {
        greenfootRecorder.removeActor(obj);        
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
            GClass lastWorldGClass = getLastWorldGClass();
            if (lastWorldGClass == null) {
                return;
            }
            String lastWorldClassName = getLastWorldGClass().getName();
            
            for (StackTraceElement item : methods) {
                if (GreenfootRecorder.METHOD_NAME.equals(item.getMethodName()) &&
                        item.getClassName().equals(lastWorldClassName)) {
                    // This call gives the object a name,
                    // which will be necessary for appending operations with the object to the world's code:
                    greenfootRecorder.nameActor(object);
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
     * Notify that the simulation has become active ("act" or "run" pressed). Any recorded interaction
     * then becomes invalid.
     */
    @Override
    public void simulationActive()
    {
        greenfootRecorder.clearCode(true);
        saveWorldAction.setRecordingValid(false);
    }

    public SaveWorldAction getSaveWorldAction()
    {
        return saveWorldAction;
    }
    
    /**
     * Is the world currently initialising?
     */
    public boolean initialising()
    {
        return worldInitialising;
    }

    /**
     * Returns true if the world is currently initialising, and has gone behind its timeout
     */
    public boolean initialisingForTooLong()
    {
        return worldInitialising && System.currentTimeMillis() > startedInitialisingAt + WORLD_INITIALISING_TIMEOUT;
    }
    
    /**
     * Did the last world invocation end in an error?
     */
    public boolean initialisationError()
    {
        return worldInvocationError;
    }

    /**
     * Is there a default constructor in the world subclass?
     * 
     * @return true if the world subclass does not have a default constructor
     */
    public boolean isMissingConstructor()
    {
        return missingConstructor;
    }

    /**
     * Sets a flag which indicates whether the world subclass misses a default constructor or not
     * 
     * @param missingConstructor a boolean flag, which is true if there is no default constructor
     * in the world subclass
     */
    public void setMissingConstructor(boolean missingConstructor)
    {
        this.missingConstructor = missingConstructor;
    }

    /**
     * Has the VM just been restarted?
     * 
     * @return true if the VM just been restarted 
     */
    public boolean isVmRestarted()
    {
        return vmRestarted;
    }

    /**
     * Sets the VM state, has it just been restarted or not 
     * 
     * @param vmRestarted a boolean flag, which is true if the VM just been restarted
     */
    public void setVmRestarted(boolean vmRestarted)
    {
        this.vmRestarted = vmRestarted;
    }
}
