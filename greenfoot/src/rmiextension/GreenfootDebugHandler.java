/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011,2012,2013,2015,2018 Poul Henriksen and Michael Kolling 
 
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
package rmiextension;

import bluej.debugger.VarDisplayInfo;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import greenfoot.actions.ResetWorldAction;
import greenfoot.core.PickActorHelper;
import greenfoot.core.Simulation;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import greenfoot.core.WorldHandler;
import greenfoot.guifx.GreenfootStage;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.record.GreenfootRecorder;
import greenfoot.util.DebugUtil;
import javafx.application.Platform;
import rmiextension.wrappers.RProjectImpl;
import rmiextension.wrappers.WrapperPool;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.objectbench.ObjectBenchEvent;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.debugmgr.objectbench.ObjectBenchListener;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class that does several things:
 * 
 * <p>Firstly, it listens for the debugger terminating the Greenfoot VM, and relaunches Greenfoot.
 * 
 * <p>Secondly, it tries to make sure that the debugger never stops the code
 * entirely outside the user's code (i.e. there should always be some user code
 * somewhere in the call stack) 
 * 
 * @author Neil Brown
 */
public class GreenfootDebugHandler implements DebuggerListener, ObjectBenchInterface, ValueCollection
{  
    private static final String SIMULATION_CLASS = Simulation.class.getName();   
    private static final String[] INVOKE_METHODS = {Simulation.ACT_WORLD, Simulation.ACT_ACTOR,
            Simulation.NEW_INSTANCE, Simulation.RUN_QUEUED_TASKS, Simulation.WORLD_STARTED, Simulation.WORLD_STOPPED};
    private static final String SIMULATION_INVOKE_KEY = SIMULATION_CLASS + "INTERNAL";
    
    private static final String PAUSED_METHOD = Simulation.PAUSED;
    private static final String SIMULATION_THREAD_PAUSED_KEY = "SIMULATION_THREAD_PAUSED";
    private static final String SIMULATION_THREAD_RESUMED_KEY = "SIMULATION_THREAD_RESUMED";
        
    private static final String SIMULATION_THREAD_RUN_KEY = "SIMULATION_THREAD_RUN";
    
    private static final String RESET_CLASS = ResetWorldAction.class.getName();
    private static final String RESET_METHOD = ResetWorldAction.RESET_WORLD;
    private static final String RESET_KEY = "RESET_WORLD";

    private static final String WORLD_HANDLER_CLASS = WorldHandler.class.getName();
    private static final String WORLD_CHANGED_KEY = "WORLD_CHANGED";
    private static final String WORLD_INITIALISING_KEY = "WORLD_INITIALISING";

    private static final String NAME_ACTOR_CLASS = WorldHandlerDelegateIDE.class.getName();
    private static final String NAME_ACTOR_KEY = "NAME_ACTOR";

    private static final String PICK_HELPER_CLASS = PickActorHelper.class.getName();
    private static final String PICK_HELPER_KEY = "PICK_HELPER_PICKED";
    private PickListener pickListener;

    private BProject project;
    private DebuggerThread simulationThread;
    private DebuggerClass simulationClass;
    private GreenfootRecorder greenfootRecorder;
    private SimulationStateListener simulationListener;
    private Map<String,GreenfootObject> objectBench = new HashMap<>();
    private List<ObjectBenchListener> benchListeners = new ArrayList<>();
    
    private File shmFile;

    /**
     * Constructor for GreenfootDebugHandler.
     */
    @OnThread(Tag.FXPlatform)
    private GreenfootDebugHandler(BProject project)
    {
        this.project = project;
        try
        {
            shmFile = initialiseServerDraw(ExtensionBridge.getProject(project), this);
        }
        catch (ProjectNotOpenException pnoe)
        {
            throw new RuntimeException(pnoe);
        }
    }
        
    /**
     * This is the publicly-visible way to add a debugger listener for a particular project.    
     */
    static void addDebuggerListener(BProject project)
    {
        try {
            Project proj = Project.getProject(project.getDir());
            proj.getExecControls().setRestrictedClasses(DebugUtil.restrictedClassesAsNames());

            GreenfootDebugHandler handler = new GreenfootDebugHandler(project);
            proj.getDebugger().addDebuggerListener(handler);
        } catch (ProjectNotOpenException ex) {
            Debug.reportError("Project not open when adding debugger listener in Greenfoot", ex);
        }
    }
    
    /**
     * Creates a shared memory buffer (using a file mmap-ed into memory), and constructs
     * a graphical window to show the outcome of the drawing on each animation pulse,
     * as well as forwarding the events received to the shared memory buffer.
     *
     * This functionality will become part of the main Greenfoot window's code once
     * that gets moved across to the server VM.
     */
    @OnThread(Tag.FXPlatform)
    private File initialiseServerDraw(Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        try
        {
            File shmFile = File.createTempFile("greenfoot", "shm");
            FileChannel fc = new RandomAccessFile(shmFile, "rw").getChannel();
            MappedByteBuffer sharedMemoryByte = fc.map(MapMode.READ_WRITE, 0, 10_000_000L);
            GreenfootStage.makeStage(project, greenfootDebugHandler, fc, sharedMemoryByte).show();
            return shmFile;
        }
        catch (IOException e)
        {
            // TODO this must be handled appropriately.
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the listener which will be called when a pick request completes.
     * @param pickListener Will be called with the pickId and list of actors and world at that position.
     */
    public void setPickListener(PickListener pickListener)
    {
        this.pickListener = pickListener;
    }

    private void addRunResetBreakpoints(Debugger debugger)
    {
        try {
            // We have to initialise the class; the IBM JDK otherwise throws an ObjectCollectedException
            // exception, seemingly in error.
            simulationClass = debugger.getClass(SIMULATION_CLASS, true).get();

            setBreakpoint(debugger, SIMULATION_CLASS, "run", SIMULATION_THREAD_RUN_KEY);
            setBreakpoint(debugger, SIMULATION_CLASS, PAUSED_METHOD, SIMULATION_THREAD_PAUSED_KEY);

            setBreakpoint(debugger, SIMULATION_CLASS, "resumeRunning", SIMULATION_THREAD_RESUMED_KEY);
            setBreakpoint(debugger, RESET_CLASS, RESET_METHOD, RESET_KEY);
            setBreakpoint(debugger, WORLD_HANDLER_CLASS, "setInitialisingWorld", WORLD_INITIALISING_KEY);
            setBreakpoint(debugger, WORLD_HANDLER_CLASS, "worldChanged", WORLD_CHANGED_KEY);
            setBreakpoint(debugger, NAME_ACTOR_CLASS, "nameActors", NAME_ACTOR_KEY);
            setBreakpoint(debugger, PICK_HELPER_CLASS, "picked", PICK_HELPER_KEY);
        }
        catch (ClassNotFoundException cnfe) {
            Debug.reportError("Simulation class could not be located. Possible installation problem.", cnfe);
        }
    }

    /**
     * Sets a breakpoint in the given class and method, and identifies it by setting a
     * breakpoint point property with breakpointKey mapped to "TRUE"
     */
    private void setBreakpoint(Debugger debugger, String className, String methodName, String breakpointKey)
    {
        Map<String, String> breakpointProperties = new HashMap<String, String>();
        breakpointProperties.put(breakpointKey, "TRUE");
        breakpointProperties.put(Debugger.PERSIST_BREAKPOINT_PROPERTY, "TRUE");
        debugger.toggleBreakpoint(className, methodName, true, breakpointProperties);
    }

    private boolean isSimulationThread(DebuggerThread dt)
    {
        return dt != null && simulationThread != null && simulationThread.sameThread(dt);
    }
    
    /**
     * Get the temporary file used as the shared memory communication backing.
     */
    public File getShmFile()
    {
        return shmFile;
    }
    
    /**
     * An early examination of the debugger event (gets called before processDebuggerEvent)
     * 
     * This method is responsible for checking where the debugger has stopped (if it has),
     * and deciding whether it should be run on for a bit until it reaches user code.
     * 
     * This method does not actually run it on; see the comments on the scheduledTasks field
     * at the top of the class for how it works.
     */
    @Override
    public boolean examineDebuggerEvent(final DebuggerEvent e)
    {
        final Debugger debugger = (Debugger)e.getSource();
        List<SourceLocation> stack = e.getThread().getStack();
        boolean atBreakpoint = e.getID() == DebuggerEvent.THREAD_BREAKPOINT && e.getThread() != null && e.getBreakpointProperties() != null;
        if (atBreakpoint && e.getBreakpointProperties().get(SIMULATION_THREAD_RUN_KEY) != null)
        {
            // This is the breakpoint at the very beginning of the simulation thread;
            // record this thread as being the simulation thread and set it running again:
            simulationThread = e.getThread();
            try {
                RProjectImpl rproj = WrapperPool.instance().getWrapper(project);
                rproj.setSimulationThread(simulationThread);
            }
            catch (RemoteException re) {
                Debug.reportError("Unexpected exception getting project wrapper: ", re);
            }
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(SIMULATION_THREAD_RESUMED_KEY) != null)
        {
            if (simulationListener != null)
            {
                simulationListener.simulationStartedRunning();
            }
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(NAME_ACTOR_KEY) != null)
        {
            VarDisplayInfo varDisplayInfo = e.getThread().getLocalVariables(0).get(0);
            greenfootRecorder.nameActors(fetchArray(varDisplayInfo.getFetchObject().get()));
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(WORLD_INITIALISING_KEY) != null)
        {
            greenfootRecorder.clearCode(true);
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(WORLD_CHANGED_KEY) != null)
        {
            List<DebuggerField> fields = e.getThread().getCurrentObject(0).getFields();
            DebuggerField worldField = fields.stream().filter(f -> f.getName().equals("world")).findFirst().orElse(null);
            if (worldField != null)
            {
                DebuggerObject worldValue = worldField.getValueObject(null);
                if (greenfootRecorder != null)
                {
                    greenfootRecorder.setWorld(worldValue);
                }
                boolean byUserCode = e.getThread().getLocalVariables(0).get(0).getValue().equals("true");
                if (simulationListener != null && !byUserCode)
                {
                    simulationListener.simulationInitialisedWorld();
                }
            }
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(PICK_HELPER_KEY) != null)
        {
            List<DebuggerField> fields = e.getThread().getCurrentObject(0).getFields();
            DebuggerField actorPicksField = fields.stream().filter(f -> f.getName().equals("actorPicks")).findFirst().orElse(null);
            DebuggerField worldPickField = fields.stream().filter(f -> f.getName().equals("worldPick")).findFirst().orElse(null);
            DebuggerField pickIdField = fields.stream().filter(f -> f.getName().equals("pickId")).findFirst().orElse(null);
            // Should always be non-null, but check in case:
            if (actorPicksField != null && worldPickField != null && pickIdField != null)
            {
                DebuggerObject actorPicksValue = actorPicksField.getValueObject(null);
                DebuggerObject worldPickValue = worldPickField.getValueObject(null);
                int pickIdValue = Integer.parseInt(pickIdField.getValueString());
                // Should always be true, but check in case:
                if (actorPicksValue != null && actorPicksValue.isArray() && worldPickValue != null)
                {
                    List<DebuggerObject> picksElements = fetchArray(actorPicksValue);
                    Platform.runLater(() -> {
                        if (pickListener != null)
                        {
                            pickListener.picked(pickIdValue, picksElements, worldPickValue);
                        }
                    });
                }
            }
            // Must resume the thread afterwards:
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(RESET_KEY) != null)
        {
            // The user has clicked reset,
            // Set the simulation thread going if it's suspended:
            if (simulationThread.isSuspended()) {
                simulationThread.cont();
            }

            Platform.runLater(new Runnable() {
                public void run()
                {
                    try {
                        ExtensionBridge.clearObjectBench(project);
                    }
                    catch (ProjectNotOpenException e) { }

                    // Run the GUI thread on:
                    e.getThread().cont();
                };
            });
            
            return true;
        }
        else if (e.isHalt() && isSimulationThread(e.getThread()))
        {
            if (e.getBreakpointProperties().get(SIMULATION_THREAD_PAUSED_KEY) != null) {
                // They are going to pause; remove all special breakpoints and set them going
                // (so that they actually hit the pause):
                removeSpecialBreakpoints(debugger);
                if (simulationListener != null)
                {
                    simulationListener.simulationPaused();
                }
                e.getThread().cont();
                return true;
            } else if (insideUserCode(stack)) {
                // They are in an act method, make sure the breakpoints are cleared:
                
                // This method can be safely invoked without needing to talk to the worker thread:
                debugger.removeBreakpointsForClass(SIMULATION_CLASS);
                        
                // If they have just hit the breakpoint and are in InvokeAct itself,
                // step-into the World/Actor:
                if (e.getBreakpointProperties().get(SIMULATION_INVOKE_KEY) != null) {
                    e.getThread().stepInto();
                    return true;
                } else if (inInvokeMethods(stack, 0)) {
                    // Finished calling act() and have stepped out; run to next one:
                    runToInternalBreakpoint(debugger, e.getThread());
                    return true;                    
                } //otherwise they are in their own code
            } else  {
                if (inPauseMethod(stack)) {
                    // They are paused, just set them running again and forget it:
                    e.getThread().cont();
                } else {
                    // They are not in an act() method and not paused; run until they get to an act() method:
                    runToInternalBreakpoint(debugger, e.getThread());
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Fetches all the objects in a debug VM array into
     * a server VM list of debug objects (the array elements).
     * @param arrayValue
     * @return
     */
    private List<DebuggerObject> fetchArray(DebuggerObject arrayValue)
    {
        List<DebuggerObject> elements = new ArrayList<>(arrayValue.getElementCount());
        for (int i = 0; i < arrayValue.getElementCount(); i++)
        {
            elements.add(arrayValue.getElementObject(i));
        }
        return elements;
    }

    /**
     * Processes a debugger event.  This is called after examineDebuggerEvent, with a second
     * parameter that effectively corresponds to the return result of examineDebuggerEvent.
     * 
     * <p>Thus, if the parameter is true, we look for a scheduled task to run.
     * 
     * <p>We call threadHalted if necessary.
     */
    @Override
    public void processDebuggerEvent(final DebuggerEvent e, boolean skipUpdate)
    {
        if (e.getNewState() == Debugger.IDLE && e.getOldState() == Debugger.NOTREADY)
        {
            if (! ProjectManager.checkLaunchFailed())
            {
                //It is important to have this code run at a later time.
                //If it runs from this thread, it tries to notify the VM event handler,
                //which is currently calling us and we get a deadlock between the two VMs.
                Platform.runLater(() -> {
                    objectBench.clear();
                    addRunResetBreakpoints((Debugger) e.getSource());
                    ProjectManager.instance().openGreenfoot(project, GreenfootDebugHandler.this);
                });
            }
        }
        
        if (!skipUpdate)
        {
            if (e.isHalt() && isSimulationThread(e.getThread())&& simulationListener != null)
            {
                simulationListener.simulationDebugHalted();
            }
            else if (e.getID() == DebuggerEvent.THREAD_CONTINUE && isSimulationThread(e.getThread()) && simulationListener != null)
            {
                simulationListener.simulationDebugResumed();
            }
        }
    }

    /**
     * Runs the debugger on until it hits the special invoke-act breakpoints that occur
     * just before user code might be encountered.  This method doesn't actually check if you're
     * thereabouts already, so it should be only called once you've checked that you actually
     * want to run onwards.
     * 
     * Returns a task that will run them onwards, which can be scheduled as you like
     */
    private void runToInternalBreakpoint(final Debugger debugger, final DebuggerThread thread)
    {
        // Set a break point where we want them to be:
        setSpecialBreakpoints(debugger);

        // Then set them running again:
        thread.cont();
    }
    
    /**
     * Works out if we are currently in a call to the World or Actor act() methods
     * by looking in the call stack for them. Strictly speaking, we might not be
     * truly inside the user code: it might be we are about to enter or have just
     * left the act() method. It is only valid to call this for the simulation
     * thread.
     */
    private static boolean insideUserCode(List<SourceLocation> stack)
    {
        for (int i = 0; i < stack.size();i++) {
            if (inInvokeMethods(stack, i)) {
                return true;
            }
        }
        return false;
    }
   
    /**
     * Works out if the specified frame in the call-stack is in one of the special invoke-act
     * methods that call the World and Actor's act() methods or the method that runs
     * other user code on the simulation thread 
     */
    private static boolean inInvokeMethods(List<SourceLocation> stack, int frame)
    {
        if (frame < stack.size()) {
            String className = stack.get(frame).getClassName();
            if (className.equals(SIMULATION_CLASS)) {
                String methodName = stack.get(frame).getMethodName();
                for (String actMethod : INVOKE_METHODS) {
                    if (actMethod.equals(methodName)) {
                        return true;
                    }
                }
            }
            else if (JavaNames.getBase(className).startsWith(Invoker.SHELLNAME)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Works out if they are currently paused by looking at the call stack
     * while they are suspended.
     */
    private static boolean inPauseMethod(List<SourceLocation> stack)
    {
        for (SourceLocation loc : stack) {
            if (loc.getClassName().equals(SIMULATION_CLASS) && loc.getMethodName().equals(PAUSED_METHOD)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets breakpoints in the special invoke-act methods that call the World and Actor's
     * act() methods, and the method that constructs new objects, and the method called when
     * the simulation will pause.  These breakpoints will thus be encountered immediately before control
     * would descend into the World and Actor's act() methods or other tasks (i.e. potential user code),
     * or if the simulation is going to wait for the user to click the controls (e.g. end of an
     * Act, or because the simulation is now going to be Paused).
     */
    private void setSpecialBreakpoints(final Debugger debugger)
    {
        for (String method : INVOKE_METHODS) {
            String err = debugger.toggleBreakpoint(simulationClass, method, true, Collections.singletonMap(SIMULATION_INVOKE_KEY, "yes"));
            if (err != null) {
                Debug.reportError("Problem setting special breakpoint: " + err);
            }
        }
    }

    /**
     * Removes the breakpoints added by setSpecialBreakpoints
     */
    private void removeSpecialBreakpoints(Debugger debugger)
    {
        for (String method : INVOKE_METHODS)
        {
            debugger.toggleBreakpoint(simulationClass, method, false, Collections.singletonMap(SIMULATION_INVOKE_KEY, "yes"));
        }
    }

    public void setGreenfootRecorder(GreenfootRecorder greenfootRecorder)
    {
        this.greenfootRecorder = greenfootRecorder;
    }

    public void setSimulationListener(SimulationStateListener simulationListener)
    {
        this.simulationListener = simulationListener;
    }
    
    @Override
    public String addObject(DebuggerObject object, GenTypeClass type,
            String name)
    {
        while (objectBench.get(name) != null) {
            name += "_"; // TODO improve
        }
        
        objectBench.put(name, new GreenfootObject(object, type, name));
        return name;
    }
    
    /**
     * Add an object to the "object bench" and fire an event to listeners notifying that the new object has
     * been selected.
     * 
     * @param object   The object to add
     * @param type     The type of the object
     * @param name     The desired name of the object
     * @return    The actual chosen name
     */
    public String addSelectedObject(DebuggerObject object, GenTypeClass type, String name)
    {
        while (objectBench.get(name) != null)
        {
            name += "_"; // TODO improve
        }
        
        GreenfootObject newObj = new GreenfootObject(object, type, name);
        objectBench.put(name, newObj);
        
        for (ObjectBenchListener l : benchListeners)
        {
            l.objectEvent(new ObjectBenchEvent(this, ObjectBenchEvent.OBJECT_SELECTED, newObj));
        }
        
        return name;
    }
    
    @Override
    public void addObjectBenchListener(ObjectBenchListener l)
    {
        benchListeners.add(l);
    }
    
    @Override
    public void removeObjectBenchListener(ObjectBenchListener l)
    {
        benchListeners.remove(l);
    }
    
    @Override
    public boolean hasObject(String name)
    {
        return objectBench.get(name) != null;
    }
    
    @Override
    public NamedValue getNamedValue(String name)
    {
        return objectBench.get(name);
    }
    
    @Override
    public Iterator<? extends NamedValue> getValueIterator()
    {
        return objectBench.values().iterator();
    }

    /**
     * An object on the "object bench". In Greenfoot this is largely virtualised.
     */
    private static class GreenfootObject implements NamedValue
    {
        private DebuggerObject object;
        private GenTypeClass type;
        private String name;
        
        /**
         * Construct a GreenfootObject with the given name and type.
         */
        public GreenfootObject(DebuggerObject object, GenTypeClass type, String name)
        {
            this.object = object;
            this.type = type;
            this.name = name;
        }
        
        @Override
        public JavaType getGenType()
        {
            return type;
        }
        
        @Override
        public String getName()
        {
            return name;
        }
        
        @Override
        public boolean isFinal()
        {
            return false;
        }
        
        @Override
        public boolean isInitialized()
        {
            return true;
        }
    }
    
    /**
     * A listener for results of pick requests.
     */
    public static interface PickListener
    {
        // World is only relevant if actors list is empty.
        @OnThread(Tag.Any)
        public void picked(int pickId, List<DebuggerObject> actors, DebuggerObject world);
    }

    /**
     * A listener to the simulation's state
     */
    public static interface SimulationStateListener
    {
        /**
         * Called when the simulation starts running
         */
        @OnThread(Tag.Any)
        public void simulationStartedRunning();

        /**
         * Called when the simulation has been paused in a normal manner
         * (i.e. either user hit pause, or called Greenfoot.stop)
         */
        @OnThread(Tag.Any)
        public void simulationPaused();

        /**
         * Called once the world has been initialised
         */
        @OnThread(Tag.Any)
        public void simulationInitialisedWorld();

        /**
         * Called when the simulation thread has hit a (user) breakpoint
         */
        @OnThread(Tag.Any)
        public void simulationDebugHalted();

        /**
         * Called when the simulation thread has resumed from a (user) breakpoint
         */
        @OnThread(Tag.Any)
        public void simulationDebugResumed();
    }
}
