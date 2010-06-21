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
import greenfoot.actions.SaveWorldAction;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.WorldHandler;
import greenfoot.core.WorldInvokeListener;
import greenfoot.gui.DragGlassPane;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.gui.input.InputManager;
import greenfoot.localdebugger.LocalObject;
import greenfoot.platforms.WorldHandlerDelegate;
import greenfoot.record.GreenfootRecorder;
import greenfoot.record.InteractionListener;
import greenfoot.util.GreenfootUtil;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.rmi.RemoteException;
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
    implements WorldHandlerDelegate, ObjectBenchInterface, InteractionListener
{
    protected final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private final static String missingConstructorTitle = Config.getString("world.missing.constructor.title");
    private final static String missingConstructorMsg = Config.getString("world.missing.constructor.msg");
    private final static String continueButtonText = Config.getString("greenfoot.continue");

    private WorldHandler worldHandler;

    private GProject project;
    
    private GreenfootFrame frame;

    private JLabel worldTitle;
    
    // Records actions manually performed on the world:
    private GreenfootRecorder greenfootRecorder;
    private SaveWorldAction saveWorldAction;

    private boolean worldInitialising;

    public WorldHandlerDelegateIDE(GreenfootFrame frame)
    {
        worldTitle = new JLabel();
        worldTitle.setBorder(BorderFactory.createEmptyBorder(18, 0, 4, 0));
        worldTitle.setHorizontalAlignment(SwingConstants.CENTER);
        this.frame = frame;
        saveWorldAction = new SaveWorldAction(this);
        greenfootRecorder = new GreenfootRecorder(saveWorldAction);
    }

    /**
     * Make a popup menu suitable for calling methods on, inspecting and
     * removing an object in the world.
     */
    private JPopupMenu makePopupMenu(final Actor obj)
    {
        JPopupMenu menu = new JPopupMenu();

        ObjectWrapper.createMethodMenuItems(menu, obj.getClass(), new WorldInvokeListener(obj, this, frame, project),
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
                worldHandler.notifyRemovedActor(obj);
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
                    if (rObject != null)
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

    public void discardWorld(World world)
    {        
        ObjectTracker.clearRObjectCache();
    }
    
    //It is important that we reset the recorder here in this method, which is called at the start of the world's constructor.
    // Doing it in setWorld is too late, as we will miss the recording/naming needed
    // that happens when the prepare method creates new actors -- prepare is invoked from the world's constructor.
    public void initialisingWorld(World world)
    {
        worldInitialising = true;
        greenfootRecorder.reset(world);        
    }
    
    public void setWorld(final World oldWorld, final World newWorld)
    {
        worldInitialising = false;
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
                                    .getLocalObject(newWorld), null, false);
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
            if (rObj != null)
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
    
    public void mouseMoved(MouseEvent e)
    {
        // While dragging, other methods set the mouse cursor instead:
        if (false == worldHandler.isDragging()) {
            Actor actor = worldHandler.getObject(e.getX(), e.getY());
            if (actor == null) {
                worldHandler.getWorldCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else {
                worldHandler.getWorldCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
    }


    public void attachProject(Object project)
    {
        this.project = (GProject) project;
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
        
        cls = getLastWorldClass();
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
    
    public GClass getLastWorldGClass()
    {
        String lastWorldClass = project.getLastWorldClassName();
        if(lastWorldClass == null) {
            return null;
        }
        
        try {
            return project.getDefaultPackage().getClass(lastWorldClass);
        }
        catch (Exception e) {
            Debug.reportError("Error trying to get world class", e);
            return null;
        }
    }

    public Class<?> getLastWorldClass()
    {
        try {
            GClass gclass = getLastWorldGClass();
            if (gclass != null) {
                Class<?> rclass = gclass.getJavaClass();
                if (GreenfootUtil.canBeInstantiated(rclass)) {
                    return  rclass;
                }
            }
        }
        catch (Exception e) {
            Debug.reportError("Error trying to get world class", e);
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
    
    public void addActor(Actor actor, int x, int y)
    {
        greenfootRecorder.addActorToWorld(actor, x, y);
    }

    public void createdActor(Class<?> theClass, Object actor, String[] args)
    {
        greenfootRecorder.createActor(theClass, actor, args);
    }

    public void methodCall(Object obj, String actorName, String name, String[] args)
    {
        greenfootRecorder.callActorMethod(obj, actorName, name, args);        
    }

    public void staticMethodCall(String className, String name, String[] args)
    {
        greenfootRecorder.callStaticMethod(className, name, args);        
    }

    public void movedActor(Actor actor, int xCell, int yCell)
    {
        greenfootRecorder.moveActor(actor, xCell, yCell);
    }

    public void removedActor(Actor obj)
    {
        greenfootRecorder.removeActor(obj);        
    }

    public List<String> getInitWorldCode()
    {
        return greenfootRecorder.getCode();
    }

    public void objectAddedToWorld(Actor object)
    {
        if (worldInitialising) {
            try {
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
                for (StackTraceElement item : methods) {
    
                    if (GreenfootRecorder.METHOD_NAME.equals(item.getMethodName()) && item.getClassName().endsWith(getLastWorldGClass().getName())) {
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
            } catch (Exception e) {
                // Never mind then...
            }
        }
    }

    public void clearRecorderCode()
    {
        greenfootRecorder.clearCode(false);        
    }
    
    public void simulationActive()
    {
        greenfootRecorder.clearCode(true);
    }

    public SaveWorldAction getSaveWorldAction()
    {
        return saveWorldAction;
    }
    
    public InteractionListener getInteractionListener()
    {
        return this;
    }
    
}
