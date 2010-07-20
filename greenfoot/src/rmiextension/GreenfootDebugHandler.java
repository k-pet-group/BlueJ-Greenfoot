package rmiextension;

import java.awt.EventQueue;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.core.Simulation;
import rmiextension.wrappers.WrapperPool;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerThread;
import bluej.debugger.SourceLocation;
import bluej.debugger.DebuggerEvent.BreakpointProperties;
import bluej.debugger.jdi.JdiDebugger;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

/**
 * A class that listens for the debugger terminating the Greenfoot VM, and relaunches Greenfoot
 */
public class GreenfootDebugHandler implements DebuggerListener
{  
    private GreenfootDebugHandler()
    {
    }
    
    // ------------- DebuggerListener interface ------------
    
    private static GreenfootDebugHandler singleton;
    
    private static final String INVOKE_ACT_CLASS = Simulation.class.getName();
    
    private static String[] INVOKE_ACT_METHODS = {Simulation.ACT_WORLD, Simulation.ACT_ACTOR};
    
    private IdentityHashMap<DebuggerThread, Runnable> scheduledTasks = new IdentityHashMap<DebuggerThread, Runnable>();
    
    public static void addDebuggerListener(BProject project)
    {
        if (singleton == null)
            singleton = new GreenfootDebugHandler();
        try {
            ExtensionBridge.addDebuggerListener(project, singleton);
        } catch (ProjectNotOpenException ex) {
            Debug.reportError("Project not open when adding debugger listener in Greenfoot", ex);
        }
    }

    public void processDebuggerEvent(DebuggerEvent e, boolean skipUpdate)
    {
        if (skipUpdate) {
            Debug.message("processDebuggerEvent: looking for scheduled tasks for: " + e.hashCode() + " in size " + scheduledTasks.size());
            // Now is the time to schedule running the action that will restart the VM.
            // We didn't do this earlier because we needed to wait for the thread to be suspended again
            // during the event handling
            
            Runnable task = scheduledTasks.remove(e.getThread());
            if (task != null) {
                Debug.message("processDebuggerEvent: scheduling task");
                EventQueue.invokeLater(task);
            } else {
                Debug.message("processDebuggerEvent: no task found");
            }
            
        } else if (e.getNewState() == Debugger.NOTREADY && e.getOldState() == Debugger.IDLE) {
            final JdiDebugger debugger = (JdiDebugger)e.getSource();
            
            //It is important to have this code run at a later time.
            //If it runs from this thread, it tries to notify us and we get some
            //sort of RMI deadlock between the two VMs (I think).
            // XXX
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    try { 
                        BProject bProject = bluej.pkgmgr.Project.getProject(debugger.getStartingDirectory()).getBProject();
                        WrapperPool.instance().remove(bProject);
                        BPackage bPackage = bProject.getPackages()[0];
                        WrapperPool.instance().remove(bPackage);
                        ProjectManager.instance().openGreenfoot(new Project(bPackage));
                    } catch (Exception ex) {
                        Debug.reportError("Exception while trying to relaunch Greenfoot", ex);
                    }
                }
            });
            
        } else if (e.getID() == DebuggerEvent.THREAD_BREAKPOINT || e.getID() == DebuggerEvent.THREAD_HALT) {
            Debug.message("GreenfootRelauncher: converting debugger event into thread halted");
            threadHalted((Debugger)e.getSource(), e.getThread(), e.getBreakpointProperties());
        }
    }
    
    private static boolean isSimulationThread(List<SourceLocation> stack)
    {
        if (stack.isEmpty())
            return false;
        SourceLocation root = stack.get(stack.size() - 1);
        
        return root.getClassName().equals("greenfoot.core.Simulation");
    }
    
    private static boolean insideActMethod(List<SourceLocation> stack)
    {
        for (int i = 0; i < stack.size();i++) {
            if (inInvokeActMethods(stack, i))
                return true;
        }
        return false;
    }
    
    public boolean threadHalted(final Debugger debugger, final DebuggerThread thread)
    {
        return threadHalted(debugger, thread, null);
    }

    public boolean threadHalted(final Debugger debugger, final DebuggerThread thread, BreakpointProperties props)
    {
        final List<SourceLocation> stack = thread.getStack();
        
        Debug.message("GreenfootRelauncher.threadHalted");
        
        if (isSimulationThread(stack)) {
            if (insideActMethod(stack)) {
                // It's okay, they are in an act method, make sure the breakpoints are cleared:
                debugger.removeBreakpointsForClass(INVOKE_ACT_CLASS);
                //This method can be safely invoked without needing to talk to the worker thread
                        
                // If they have just hit the breakpoint and are in InvokeAct itself,
                // step-into the World/Actor:
                if (atInvokeActBreakpoint(props)) {
                    Debug.message("  At InvokeAct breakpoint, step-into threadHalted!");
                    // Avoid tricky re-entrant issues:
                    EventQueue.invokeLater(new Runnable() { public void run() {
                        thread.stepInto();
                    }});
                    return true;
                } else if (inInvokeActMethods(stack, 1) && "act".equals(stack.get(0).getMethodName())
                        && (World.class.getName().equals(stack.get(0).getClassName())
                            || Actor.class.getName().equals(stack.get(0).getClassName())
                          )) {
                    //The InvokeAct class has called directly into World/Actor
                    // This means the user hasn't provided any code for that world/actor.
                    // Rather than show them being stuck in the Greenfoot classes, let's continue
                    // to find some user code:
                    EventQueue.invokeLater(runToAct(debugger, thread));
                } else {
                    Debug.message("  Via InvokeAct but top is:" + thread.getStack().get(0).getClassName());
                }
            } else { 
                // Set this going now; we are not the examine method:
                EventQueue.invokeLater(runToAct(debugger, thread));
                return true;
            }
        }
        
        return false;
    }

    @Override
    public boolean examineDebuggerEvent(final DebuggerEvent e)
    {
        JdiDebugger debugger = (JdiDebugger)e.getSource();
        List<SourceLocation> stack = e.getThread().getStack();
        
        if (isSimulationThread(stack)) {
            Debug.message("GreenfootRelauncher.screenDebuggerEvent: " + e.toString());
            Debug.message("  " + stack.get(0).toString());
        }
        
        if ((e.getID() == DebuggerEvent.THREAD_BREAKPOINT || e.getID() == DebuggerEvent.THREAD_HALT)
                && isSimulationThread(stack)) {
            if (insideActMethod(stack)) {
                // It's okay, they are in an act method, make sure the breakpoints are cleared:
                
                //This method can be safely invoked without needing to talk to the worker thread:
                debugger.removeBreakpointsForClass(INVOKE_ACT_CLASS);
                
                        
                // If they have just hit the breakpoint and are in InvokeAct itself,
                // step-into the World/Actor:
                if (atInvokeActBreakpoint(e.getBreakpointProperties())) {
                    Debug.message("  At InvokeAct breakpoint, step-into screen: " + stack.get(0).getLineNumber() + "; scheduling task for: " + e.hashCode());
                    // Make sure it doesn't start up again by default:
                    //e.getThread().suspend();
                    // Avoid tricky re-entrant issues:
                    scheduledTasks.put(e.getThread(), new Runnable() { public void run() {
                        e.getThread().stepInto();
                    }});
                    return true;
                } else if (inInvokeActMethods(stack, 0)) {
                    // Finished calling act() and have stepped out; run to next one:
                    Debug.message("  In InvokeAct but have gone beyond act(); resume!");
                    // Make sure it doesn't start up again until we say so:
                    //e.getThread().suspend();
                    scheduledTasks.put(e.getThread(), runToAct(debugger, e.getThread()));
                    return true;                    
                } //otherwise they are in their own code
            } else {
                //e.getThread().suspend();
                scheduledTasks.put(e.getThread(), runToAct(debugger, e.getThread()));
                Debug.message("GreenfootRelauncher.screenDebuggerEvent: running to act");
                return true;
            }
        }

        return false;
    }

    private boolean atInvokeActBreakpoint(BreakpointProperties props)
    {
        return props.get(INVOKE_ACT_CLASS) != null;
    }
    
    private static boolean inInvokeActMethods(List<SourceLocation> stack, int frame)
    {
        if (frame < stack.size()) {
            if (stack.get(frame).getClassName().equals(INVOKE_ACT_CLASS)) {
                String methodName = stack.get(frame).getMethodName();
                for (String actMethod : INVOKE_ACT_METHODS) {
                    if (actMethod.equals(methodName))
                        return true;
                }
            }
        }
        
        return false;
    }

    private static Runnable runToAct(final Debugger debugger, final DebuggerThread thread)
    {
        //This method is called (via several others) from the thread that handles VM events
        //If we directly toggle breakpoints from this thread, it tries to wake up
        //the worker thread to do the toggling.  But that only works if it can send events
        //to the VM-event-handling-thread and get a response -- but that would be us!
        
        //To avoid that nasty re-entrant deadlock, we must run this method in a different thread:
        
        return new Runnable () {
            public void run () {
                // They aren't in an act method; let's get them there
                //Debug.message("GreenfootRelauncher.runToAct");

                // First, set a break point where we want them to be:
                setInvokeActBreakpoints(debugger);

                //Debug.message("GreenfootRelauncher.runToAct #2");

                // Then set them running again:
                thread.cont();

                // We will be called again once they hit the breakpoint, but we'll exit in the loop above
                //Debug.message("GreenfootRelauncher.runToAct #3");
            }
        };
    }
        
    private static void setInvokeActBreakpoints(final Debugger debugger)
    {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(INVOKE_ACT_CLASS, "yes");
        
        for (String method : INVOKE_ACT_METHODS) {
            String err = debugger.toggleBreakpoint(INVOKE_ACT_CLASS, method, true, properties);
            if (err != null) {
                Debug.message("Problem setting breakpoint: " + err);
            }
        }
    }

}
