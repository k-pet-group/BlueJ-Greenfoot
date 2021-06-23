/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011,2012,2013,2015,2018,2019,2020,2021 Poul Henriksen and Michael Kolling
 
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
package greenfoot.vmcomm;

import bluej.debugger.VarDisplayInfo;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import greenfoot.core.PickActorHelper;
import greenfoot.core.Simulation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import greenfoot.core.ProjectManager;
import greenfoot.core.WorldHandler;
import greenfoot.guifx.GreenfootStage;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.record.GreenfootRecorder;
import greenfoot.util.DebugUtil;
import javafx.application.Platform;
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
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.JavaNames;
import javafx.geometry.Point2D;
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
    
    private static final String WORLD_HANDLER_CLASS = WorldHandler.class.getName();
    private static final String WORLD_CHANGED_KEY = "WORLD_CHANGED";
    private static final String WORLD_INITIALISING_KEY = "WORLD_INITIALISING";
    private static final String WORLD_INSTANTIATION_ERROR_KEY = "WORLD_INSTANTIATION_ERROR";

    private static final String NAME_ACTOR_CLASS = WorldHandlerDelegateIDE.class.getName();
    private static final String NAME_ACTOR_KEY = "NAME_ACTOR";

    private static final String PICK_HELPER_CLASS = PickActorHelper.class.getName();
    private static final String PICK_HELPER_KEY = "PICK_HELPER_PICKED";
    private PickListener pickListener;

    private Project project;
    private DebuggerThread simulationThread;
    private DebuggerClass simulationClass;
    private GreenfootRecorder greenfootRecorder;
    private SimulationStateListener simulationListener;
    private Map<String,GreenfootObject> objectBench = new HashMap<>();
    private List<ObjectBenchListener> benchListeners = new ArrayList<>();
    
    private VMCommsMain vmComms;
    @OnThread(Tag.VMEventHandler)
    private boolean hasLaunched = false;

    /**
     * Constructor for GreenfootDebugHandler.
     */
    @OnThread(Tag.FXPlatform)
    private GreenfootDebugHandler(Project project) throws IOException
    {
        this.project = project;
        greenfootRecorder = new GreenfootRecorder();
        vmComms = new VMCommsMain(project);
    }
        
    /**
     * This is the publicly-visible way to add a debugger listener for a particular project.    
     */
    @OnThread(Tag.FXPlatform)
    public static void addDebuggerListener(Project project) throws IOException
    {
        project.getExecControls().setRestrictedClasses(DebugUtil.restrictedClassesAsNames());

        GreenfootDebugHandler handler = new GreenfootDebugHandler(project);
        // Add us as a debugger listener, but if the debugger is already idle (because it initialised
        // very quickly, before the server VM got to this point), we must launch now because
        // we won't see the NOTREADY->IDLE transition that we usually wait for, before launching.
        // (We know hasLaunched will be false at this point because we only just made the GreenfootDebugHandler):
        if (project.getDebugger().addDebuggerListener(handler) == Debugger.IDLE)
        {
            project.getDebugger().runOnEventHandler(() -> handler.launch(project.getDebugger()));
        }
        GreenfootStage.makeStage(project, handler).show();
    }
    
    /**
     * Set the listener which will be called when a pick request completes.
     * @param pickListener Will be called with the pickId and list of actors and world at that position.
     */
    public void setPickListener(PickListener pickListener)
    {
        this.pickListener = pickListener;
    }

    @OnThread(Tag.FXPlatform)
    private void addRunResetBreakpoints(Debugger debugger)
    {
        try {
            // We have to initialise the class; the IBM JDK otherwise throws an ObjectCollectedException
            // exception, seemingly in error.
            simulationClass = debugger.getClass(SIMULATION_CLASS, true).get();

            setBreakpoint(debugger, SIMULATION_CLASS, "run", SIMULATION_THREAD_RUN_KEY);
            setBreakpoint(debugger, SIMULATION_CLASS, PAUSED_METHOD, SIMULATION_THREAD_PAUSED_KEY);

            setBreakpoint(debugger, SIMULATION_CLASS, "resumeRunning", SIMULATION_THREAD_RESUMED_KEY);
            setBreakpoint(debugger, WORLD_HANDLER_CLASS, "setInitialisingWorld", WORLD_INITIALISING_KEY);
            setBreakpoint(debugger, WORLD_HANDLER_CLASS, "worldChanged", WORLD_CHANGED_KEY);
            setBreakpoint(debugger, WORLD_HANDLER_CLASS, "worldInstantiationError", WORLD_INSTANTIATION_ERROR_KEY);
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
    @OnThread(Tag.FXPlatform)
    private void setBreakpoint(Debugger debugger, String className, String methodName, String breakpointKey)
    {
        Map<String, String> breakpointProperties = new HashMap<String, String>();
        breakpointProperties.put(breakpointKey, "TRUE");
        breakpointProperties.put(Debugger.PERSIST_BREAKPOINT_PROPERTY, "TRUE");
        debugger.toggleBreakpoint(className, methodName, true, breakpointProperties);
    }

    @OnThread(Tag.Any)
    private boolean isSimulationThread(DebuggerThread dt)
    {
        return dt != null && simulationThread != null && simulationThread.sameThread(dt);
    }
    
    /**
     * Get the inter-VM communications channel.
     */
    public VMCommsMain getVmComms()
    {
        return vmComms;
    }
    
    /**
     * Get the temporary file used as the shared memory communication backing.
     */
    @OnThread(Tag.FXPlatform)
    public File getShmFile()
    {
        return vmComms.getSharedFile();
    }

    /**
     * Get the size of the temporary file used as the shared memory communication backing.
     */
    @OnThread(Tag.FXPlatform)
    public int getShmFileSize()
    {
        return vmComms.getSharedFileSize();
    }    

    /**
     * Gets the last sequence identifier that we've received from the user VM
     */
    public int getLastSeq()
    {
        return vmComms.getLastSeq();
    }
    
    /**
     * Get the recorder instance which tracks actor creation.
     */
    public GreenfootRecorder getRecorder()
    {
        return greenfootRecorder;
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
    @OnThread(Tag.VMEventHandler)
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
            Platform.runLater(() -> project.getExecControls().selectThread(simulationThread));
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
            DebuggerObject actorArray = e.getThread().getStackObjectUntyped(0, 0);
            greenfootRecorder.nameActors(fetchArray(actorArray));
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(WORLD_INITIALISING_KEY) != null)
        {
            greenfootRecorder.clearCode(true);
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(WORLD_INSTANTIATION_ERROR_KEY) != null)
        {
            simulationListener.worldInstantiationError();
            e.getThread().cont();
            return true;
        }
        else if (atBreakpoint && e.getBreakpointProperties().get(WORLD_CHANGED_KEY) != null)
        {
            List<DebuggerField> fields = e.getThread().getCurrentObject(0).getFields();
            DebuggerField worldField = fields.stream().filter(f -> f.getName().equals("world")).findFirst().orElse(null);
            if (worldField != null)
            {
                DebuggerObject worldValue = worldField.getValueObject();
                greenfootRecorder.setWorld(worldValue);
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
                DebuggerObject actorPicksValue = actorPicksField.getValueObject();
                DebuggerObject worldPickValue = worldPickField.getValueObject();
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
        else if (e.isHalt() && isSimulationThread(e.getThread()))
        {
            if (atBreakpoint && e.getBreakpointProperties().get(SIMULATION_THREAD_PAUSED_KEY) != null)
            {
                // They are going to pause; remove all special breakpoints and set them going
                // (so that they actually hit the pause):
                removeSpecialBreakpoints(debugger);
                if (simulationListener != null)
                {
                    simulationListener.simulationPaused();
                }
                e.getThread().cont();
                return true;
            }
            else if (insideUserCode(stack))
            {
                // They are in an act method, make sure the breakpoints are cleared:
                
                // This method can be safely invoked without needing to talk to the worker thread:
                debugger.removeBreakpointsForClass(SIMULATION_CLASS);
                        
                // If they have just hit the breakpoint and are in InvokeAct itself,
                // step-into the World/Actor:
                if (atBreakpoint && e.getBreakpointProperties().get(SIMULATION_INVOKE_KEY) != null) {
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
    @OnThread(Tag.Any)
    @SuppressWarnings("threadchecker")
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
    @OnThread(Tag.VMEventHandler)
    @Override
    public void processDebuggerEvent(final DebuggerEvent e, boolean skipUpdate)
    {
        if (e.getNewState() == Debugger.NOTREADY)
        {
            // We will need to relaunch, so reset the flag:
            hasLaunched = false;
            vmComms.vmTerminated();
            if (simulationListener != null)
            {
                simulationListener.simulationVMTerminated();
            }
        }
        
        if (e.getNewState() == Debugger.IDLE && !hasLaunched)
        {
            launch((Debugger) e.getSource());
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
     * Launches Greenfoot on the debug VM.  Only call this once (check the hasLaunched flag before calling)
     * @param debugger The debugger for the project.
     */
    @OnThread(Tag.VMEventHandler)
    private void launch(Debugger debugger)
    {
        if (! ProjectManager.checkLaunchFailed())
        {
            hasLaunched = true;
            // It is important to have this code run at a later time.
            // If it runs from this thread, it tries to notify the VM event handler,
            // which is currently calling us and we get a deadlock between the two VMs.
            Platform.runLater(() -> {
                objectBench.clear();
                addRunResetBreakpoints(debugger);
                ProjectManager.instance().openGreenfoot(project, GreenfootDebugHandler.this);
            });
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
    @OnThread(Tag.VMEventHandler)
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
            boolean nowSet = debugger.toggleBreakpoint(simulationClass, method, true, Collections.singletonMap(SIMULATION_INVOKE_KEY, "yes"));
            if (!nowSet)
            {
                Debug.reportError("Problem setting special Greenfoot breakpoint");
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

    public void setSimulationListener(SimulationStateListener simulationListener)
    {
        this.simulationListener = simulationListener;
    }

    /**
     * Halts the simulation thread.
     */
    public void haltSimulationThread()
    {
        project.getDebugger().runOnEventHandler(() -> {
            if (simulationThread != null)
            {
                simulationThread.halt();
            }
        });
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
     * Ensure that an object is "on the bench" (known to the debugger and invoker).
     * @param object  The object to put (or find) on the bench
     * @param type  The type of the object
     * @return  The wrapped bench object
     */
    @OnThread(Tag.FXPlatform)
    public NamedValue ensureObjectOnBench(DebuggerObject object, GenTypeClass type)
    {
        GreenfootObject selectedObject = null;
        
        for (GreenfootObject benchObj : objectBench.values())
        {
            if (benchObj.getDebuggerObject().equals(object))
            {
                selectedObject = benchObj;
                break;
            }
        }
        
        // If the object isn't on the bench yet, we must add it now:
        if (selectedObject == null)
        {
            String name = project.getDebugger().guessNewName(object);
            project.getDebugger().addObject(project.getPackage("").getId(), name, object);

            GreenfootObject newObj = new GreenfootObject(object, type, name);
            objectBench.put(name, newObj);
            
            selectedObject = newObj;
        }
        
        return selectedObject;
    }
    
    /**
     * Add an object to the "object bench" and fire an event to listeners notifying that the new object has
     * been selected.
     * 
     * @param object   The object to add
     * @param type     The type of the object
     * @return    The name of the object as it is known to the debugger
     */
    @OnThread(Tag.FXPlatform)
    public void addSelectedObjects(List<DebuggerObject> objects, Point2D screenPosition)
    {
        NamedValue[] values = nameObjects(objects);

        for (ObjectBenchListener l : benchListeners)
        {
            l.objectEvent(new ObjectBenchEvent(this, ObjectBenchEvent.OBJECT_SELECTED, values, screenPosition));
        }
    }

    /**
     * Names each of the objects
     * @param objects The list of debugger objects to name.
     * @return An array of NamedValue, with index 0 corresponding to index 0 in the objects parameter, index 1 to index 1, etc.
     */
    @OnThread(Tag.FXPlatform)
    public NamedValue[] nameObjects(List<DebuggerObject> objects)
    {
        NamedValue[] values = new NamedValue[objects.size()];
        for (int i = 0; i < objects.size(); i++)
        {
            values[i] = ensureObjectOnBench(objects.get(i), objects.get(i).getGenType());
        }
        return values;
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
        private GenTypeClass type;
        private String name;
        private DebuggerObject debuggerObject;
        
        /**
         * Construct a GreenfootObject with the given name and type.
         */
        public GreenfootObject(DebuggerObject object, GenTypeClass type, String name)
        {
            this.type = type;
            this.name = name;
            this.debuggerObject = object;
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
        
        /**
         * Get the DebuggerObject that this GreenfootObject wraps.
         */
        public DebuggerObject getDebuggerObject()
        {
            return debuggerObject;
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
         * Called when the simulation thread has hit a (user) breakpoint
         */
        @OnThread(Tag.Any)
        public void simulationDebugHalted();

        /**
         * Called when the simulation thread has resumed from a (user) breakpoint
         */
        @OnThread(Tag.Any)
        public void simulationDebugResumed();

        /**
         * Called when there is an error while instantiating the world.
         */
        @OnThread(Tag.Any)
        public void worldInstantiationError();

        /**
         * Called when the debug VM has just terminated
         * (but not yet restarted)
         */
        @OnThread(Tag.Any)
        public void simulationVMTerminated();
    }

    /**
     * Set the simulation thread going if it's suspended: The user has clicked reset (or a reset
     * has otherwise been issued, eg after successful compile).
     */
    public void simulationThreadResumeOnResetClick()
    {
        project.getDebugger().runOnEventHandler(() -> {
            if (simulationThread != null && simulationThread.isSuspended())
            {
                simulationThread.cont();
            }
        });
        objectBench.clear();
    }
}
