/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010,2011,2012,2013 Poul Henriksen and Michael Kolling 
 
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

import greenfoot.actions.ResetWorldAction;
import greenfoot.core.Simulation;
import greenfoot.core.SimulationDebugMonitor;

import java.awt.EventQueue;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rmiextension.wrappers.RProjectImpl;
import rmiextension.wrappers.WrapperPool;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerEvent.BreakpointProperties;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.Invoker;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

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
public class GreenfootDebugHandler implements DebuggerListener
{  
    private static final String SIMULATION_CLASS = Simulation.class.getName();   
    private static final String[] INVOKE_METHODS = {Simulation.ACT_WORLD, Simulation.ACT_ACTOR,
            Simulation.NEW_INSTANCE, Simulation.RUN_QUEUED_TASKS, Simulation.WORLD_STARTED, Simulation.WORLD_STOPPED};
    private static final String SIMULATION_INVOKE_KEY = SIMULATION_CLASS + "INTERNAL";
    
    private static final String PAUSED_METHOD = Simulation.PAUSED;
    private static final String SIMULATION_THREAD_PAUSED_KEY = "SIMULATION_THREAD_PAUSED"; 
        
    private static final String SIMULATION_THREAD_RUN_KEY = "SIMULATION_THREAD_RUN";
    
    private static final String RESET_CLASS = ResetWorldAction.class.getName();
    private static final String RESET_METHOD = ResetWorldAction.RESET_WORLD;
    private static final String RESET_KEY = "RESET_WORLD";
    
    private BProject project;
    private DebuggerThread simulationThread;
    private DebuggerClass simulationClass;
    
    private GreenfootDebugHandler(BProject project)
    {
        this.project = project;
    }
        
    /**
     * This is the publicly-visible way to add a debugger listener for a particular project.    
     */
    static void addDebuggerListener(BProject project)
    {
        try {
            Project proj = Project.getProject(project.getDir());

            // Technically I could collapse the two listeners into one, but they
            // perform orthogonal tasks so it's nicer to keep the code separate:
            GreenfootDebugHandler handler = new GreenfootDebugHandler(project);
            int mstate = proj.getDebugger().addDebuggerListener(handler);
            proj.getDebugger().addDebuggerListener(handler.new GreenfootDebugControlsLink());
            if (mstate == Debugger.IDLE) {
                // The VM may have already started by the time the listener was added. If so,
                // we need to kick off Greenfoot on the other VM here:
                handler.addRunResetBreakpoints(proj.getDebugger());
                ProjectManager.instance().openGreenfoot(project);
            }
        } catch (ProjectNotOpenException ex) {
            Debug.reportError("Project not open when adding debugger listener in Greenfoot", ex);
        }
    }

    private void addRunResetBreakpoints(Debugger debugger)
    {
        try {
            // We have to initialise the class; the IBM JDK otherwise throws an ObjectCollectedException
            // exception, seemingly in error.
            simulationClass = debugger.getClass(SIMULATION_CLASS, true);

            Map<String, String> simulationRunBreakpointProperties = new HashMap<String, String>();
            simulationRunBreakpointProperties.put(SIMULATION_THREAD_RUN_KEY, "TRUE");
            simulationRunBreakpointProperties.put(Debugger.PERSIST_BREAKPOINT_PROPERTY, "TRUE");
            debugger.toggleBreakpoint(simulationClass, "run", true, simulationRunBreakpointProperties);

            Map<String, String> resetBreakpointProperties = new HashMap<String, String>();
            resetBreakpointProperties.put(RESET_KEY, "yes");
            resetBreakpointProperties.put(Debugger.PERSIST_BREAKPOINT_PROPERTY, "TRUE");
            debugger.toggleBreakpoint(RESET_CLASS, RESET_METHOD, true, resetBreakpointProperties);
        }
        catch (ClassNotFoundException cnfe) {
            Debug.reportError("Simulation class could not be located. Possible installation problem.", cnfe);
        }
    }
    
    private boolean isSimulationThread(DebuggerThread dt)
    {
        return dt != null && simulationThread != null && simulationThread.sameThread(dt);
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
        
        if (e.getID() == DebuggerEvent.THREAD_BREAKPOINT
            && e.getThread() != null &&
            e.getBreakpointProperties().get(SIMULATION_THREAD_RUN_KEY) != null) {
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
            
        } else if (e.getID() == DebuggerEvent.THREAD_BREAKPOINT
                && atResetBreakpoint(e.getBreakpointProperties())) {
            // The user has clicked reset,
            // Set the simulation thread going if it's suspended:
            if (simulationThread.isSuspended()) {
                simulationThread.cont();
            }

            EventQueue.invokeLater(new Runnable() {
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
        } else if (e.isHalt() && isSimulationThread(e.getThread())) {
            if (atPauseBreakpoint(e.getBreakpointProperties())) {
                // They are going to pause; remove all special breakpoints and set them going
                // (so that they actually hit the pause):
                debugger.removeBreakpointsForClass(SIMULATION_CLASS);
                e.getThread().cont();
                return true;
            } else if (insideUserCode(stack)) {
                // They are in an act method, make sure the breakpoints are cleared:
                
                // This method can be safely invoked without needing to talk to the worker thread:
                debugger.removeBreakpointsForClass(SIMULATION_CLASS);
                        
                // If they have just hit the breakpoint and are in InvokeAct itself,
                // step-into the World/Actor:
                if (atInvokeBreakpoint(e.getBreakpointProperties())) {
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
        if (e.getNewState() == Debugger.IDLE && e.getOldState() == Debugger.NOTREADY) {
            if (! ProjectManager.checkLaunchFailed()) {
                //It is important to have this code run at a later time.
                //If it runs from this thread, it tries to notify the VM event handler,
                //which is currently calling us and we get a deadlock between the two VMs.
                EventQueue.invokeLater(new Runnable() {
                    public void run()
                    {
                        addRunResetBreakpoints((Debugger) e.getSource());
                        ProjectManager.instance().openGreenfoot(project);
                    }
                });
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
     * Works out if they are at the breakpoint triggered by the user clicking
     * the Reset button.
     */
    private static boolean atResetBreakpoint(BreakpointProperties props)
    {
        return props != null && props.get(RESET_KEY) != null;
    }
    
    /**
     * Works out if they are currently in the Simulation.PAUSED method by looking 
     * for the special breakpoint property
     */
    private static boolean atPauseBreakpoint(BreakpointProperties props)
    {
        return props != null && props.get(SIMULATION_THREAD_PAUSED_KEY) != null;
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
     * Checks if the given breakpoint is an invoke breakpoint set by 
     * the setSpecialBreakpoints call, below  
     */
    private static boolean atInvokeBreakpoint(BreakpointProperties props)
    {
        return props != null && props.get(SIMULATION_INVOKE_KEY) != null;
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
        
        String err = debugger.toggleBreakpoint(simulationClass, PAUSED_METHOD, true, Collections.singletonMap(SIMULATION_THREAD_PAUSED_KEY, "yes"));
        if (err != null) {
            Debug.reportError("Problem setting special breakpoint: " + err);
        }
    }
    
    /**
     * A second debug listener that only worries about enabling and disabling the
     * Act/Run/Pause buttons according to whether the Simulation thread is currently at
     * a breakpoint.
     */
    private class GreenfootDebugControlsLink implements DebuggerListener
    {
        private LinkedList<String> queuedStateVars = new LinkedList<String>();
        private Object SEND_EVENT = new Object();
        private String CLASS_NAME = SimulationDebugMonitor.class.getName();
        
        private void simplifyEvents()
        {
            // If there is more than one event, it must be made redundant by the latest
            // event:
            while (queuedStateVars.size() > 1) {
                queuedStateVars.removeFirst();
            }
        }
        
        private class SendNextEvent implements Runnable
        {
            private Debugger debugger;
            
            public SendNextEvent(Debugger debugger)
            {
                this.debugger = debugger;
            }

            @Override
            public void run()
            {
                // We hold the monitor until the object has been instantiated, to prevent race hazards:
                synchronized (SEND_EVENT) {
                    String stateVar;
                    synchronized (queuedStateVars) {
                        simplifyEvents();
                        if (queuedStateVars.isEmpty()) {
                            return;
                        }
                        stateVar = queuedStateVars.removeFirst();
                    }
                    try {
                        DebuggerClass simMonClass = debugger.getClass(CLASS_NAME, true);
                        DebuggerObject stateObject = null;
                        for (int i = 0; ; i++) {
                            DebuggerField simMonField = simMonClass.getStaticField(i);
                            if (simMonField.getName().equals(stateVar)) {
                                stateObject = simMonField.getValueObject(null);
                                break;
                            }
                        }
                        debugger.instantiateClass(CLASS_NAME, new String[] {"java.lang.Object"},
                                new DebuggerObject[] {stateObject});
                    } catch (ClassNotFoundException ex) {
                        Debug.reportError("Could not find internal class " + CLASS_NAME, ex);
                    }
                }
            }
        }
        
        @Override
        public boolean examineDebuggerEvent(DebuggerEvent e)
        {
            return false;
        }

        @Override
        public synchronized void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate)
        {
            final String stateVar;
            if (e.isHalt()) {
                if (isSimulationThread(e.getThread())) {
                    stateVar = "NOT_RUNNING";
                }
                else {
                    return;
                }
            }
            else if (e.getID() == DebuggerEvent.THREAD_CONTINUE) {
                if (isSimulationThread(e.getThread())) {
                    stateVar = "RUNNING";
                }
                else {
                    return;
                }
            } else {
                return;
            }
            
            final Debugger debugger = (Debugger) e.getSource();
            
            /* We are on the BlueJ VM, but we need to adjust the state of the buttons
             * on the Greenfoot VM (aka Debug VM).  We use this slight hack of constructing
             * an object on the Greenfoot VM that will do the work for us there.
             * 
             * For a parameter, we pass one of the static objects that the class holds
             * (this was more obviously do-able than passing a boolean constant, fix it if you know how)
             * 
             * We must do this in a new thread because we'll deadlock if we try to directly
             * create the object from a debug handler as we are. 
             */

            synchronized (queuedStateVars) {
                queuedStateVars.addLast(stateVar);
                new Thread (new SendNextEvent(debugger)).start();
            }                
        }
    }
}
