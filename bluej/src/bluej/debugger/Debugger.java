/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugger;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import bluej.classmgr.BPClassLoader;
import bluej.debugger.jdi.JdiDebugger;
import bluej.debugger.jdi.TestResultsWithRunTime;
import bluej.utility.javafx.FXPlatformSupplier;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A class defining the debugger primitives needed by BlueJ. May be supported by different
 * implementations, locally or remotely.
 * 
 * <p>Debugger has a listener interface to allow listening for certain events. Events reported to
 * the listener are guaranteed to be serialised, that is, a callback will not be entered while
 * the previous callback is still executing.
 * 
 * <p>Part of the listener interface is notification of debugger state changes. There possible
 * states are UNKNOWN, NOTREADY, IDLE, RUNNING, SUSPENDED and LAUNCH_FAILED. Only certain
 * transitions are possible:
 * 
 * <ul>
 * <li>UNKNOWN to NOTREADY: when the debugger is first launched
 * <li>NOTREADY to IDLE:  when the debugger finishes launching or restarting
 * <li>IDLE to RUNNING:  when the debugger begins execution of user code
 * <li>IDLE to NOTREADY:  when the virtual machine restarts (possibly for external reasons)
 * <li>RUNNING to IDLE:  when user code finishes or is otherwise terminated
 * <li>RUNNING to SUSPENDED:  when a breakpoint is hit etc.
 * <li>SUSPENDED to RUNNING:  when execution is continued after a breakpoint etc.
 * </ul>
 * 
 * Transitions that do not conform to the list are modified by inserting appropriate transitions.
 * For instance a transition from RUNNING to NOTREADY is represented as a transition first to
 * IDLE and then to NOTREADY. 
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 */
public abstract class Debugger
{
    // Set this key with a non-null value on any breakpoints that you want to
    // persist through calls to removeBreakpointsForClass, and through the clear-all breakpoint
    // removal that happens, for example, when a new class loader is added to the VM
    public static final String PERSIST_BREAKPOINT_PROPERTY = "VMReference.PERSIST_BREAKPOINT";    
    
    public static final int NORMAL_EXIT = 0;
    public static final int EXCEPTION = 2;
    public static final int TERMINATED_BY_USER_SYSTEM_EXIT = 3;
    public static final int TERMINATED_BY_BLUEJ = 4;

    /** Virtual machine states **/
    /** The unknown state can only be the previous state, and only in the first state change */
    public static final int UNKNOWN = 0;
    /** The debugger is not yet ready to execute code etc */
    public static final int NOTREADY = 1;
    /** The debugger is idle: ready to execute */
    public static final int IDLE = 2;
    /** The debugger is currently running user code */ 
    public static final int RUNNING = 3;
    /** The debugger is suspended, i.e. running user code but stopped at a breakpoint/step */
    public static final int SUSPENDED = 4;
    /** The debugger failed to start. */
    public static final int LAUNCH_FAILED = 5;

    /**
     * Create an instance of a debugger.
     * The constructor for the debugger should not be
     * a long process. Actual startup for the debug
     * VM should go in launch().
     * 
     * @return  a Debugger instance
     */
    public static Debugger getDebuggerImpl(File startingDirectory, DebuggerTerminal terminal, DebuggerThreadListener debuggerThreadListener)
    {
        return new JdiDebugger(startingDirectory, terminal, debuggerThreadListener);
    }

    /**
     * Set the user libraries to be added to the system classpath when the VM is launched
     * or restarted.
     */
    public abstract void setUserLibraries(URL[] libraries);
    
    /**
     * Launch a VM for running user code, which will be controlled by this debugger instance.
     * This should be called only once.
     * 
     * <p>This can be a lengthy process so this should be executed in a sub thread.
     */
    @OnThread(Tag.Any)
    public abstract void launch();

    /**
     * Terminate the debug VM, stop all user processes etc. Optionally restart afterwards.
     */
    public abstract void close(boolean restart);

    /**
     * Add a listener for DebuggerEvents
     * 
     * @param l  the DebuggerListener to add
     * @return  the machine state at the time the listener was added
     *          (any changes from this state will have been signalled to the listener)
     */
    public abstract int addDebuggerListener(DebuggerListener l);

    /**
     * Remove a listener for DebuggerEvents.
     * 
     * @param l  the DebuggerListener to remove
     */
    @OnThread(Tag.Any)
    public abstract void removeDebuggerListener(DebuggerListener l);

    /**
     * Create a class loader in the debugger.
     */
    public abstract void newClassLoader(BPClassLoader bpClassLoader);

    /**
     * Remove all breakpoints in the given class.
     */
    @OnThread(Tag.Any)
    public abstract void removeBreakpointsForClass(String className);

    /**
     * Add a debugger object into the project scope.
     * 
     * @param   scopeId          the scope identifier
     * @param   newInstanceName  the name of the object
     * @param   dob              the object itself
     * @return  true if the object could be added with this name,
     *          false if there was a name clash.
     */
    public abstract boolean addObject(String scopeId, String newInstanceName, DebuggerObject dob);

    /**
     * Remove a debugger object from the project scope.
     */
    public abstract void removeObject(String scopeId, String instanceName);

    /**
     * Return the debugger objects that exist in the
     * debugger.
     * 
     * @return  a Map of (String name, DebuggerObject obj) entries
     */
    public abstract Map<String,DebuggerObject> getObjects();

    /**
     * Guess a suitable name for an object about to be put on the object bench.
     * 
     * @param  startingName  a fully qualified class name (will be stripped of
     *                        qualifying part) or a field name that will be used
     *                        as the basis for the new name.
     * @return  a String suitable as a name for an object on the object bench. 
     */
    public abstract String guessNewName(String className);

    /**
     * Guess a suitable name for an object about to be put on the object bench.
     * 
     * @param obj      the object that will be put on the object bench
     * @return a String suitable as a name for an object on the object bench.
     */
    public abstract String guessNewName(DebuggerObject obj);

    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED, NOTREADY).
     */
    public abstract int getStatus();
    
    /**
     * Run the setUp() method of a test class and return the created
     * objects.
     * 
     * @param className  the fully qualified name of the class
     * @return          a Map of (String name, DebuggerObject obj) entries
     */
    @OnThread(Tag.Any)
    public abstract FXPlatformSupplier<Map<String,DebuggerObject>> runTestSetUp(String className);

    /**
     * Run a single test method or all test methods in a test class and return the result.
     * 
     * @param  className  the fully qualified name of the class
     * @param  methodName
     *            the name of the method, it can be null if the test runs on all test methods
     * @return a TestResultsWithRunTime object that wraps the test result and test's runtime
     */
    @OnThread(Tag.Any)
    public abstract TestResultsWithRunTime runTestMethod(String className, String methodName);
    
    /**
     * Dispose all top level windows in the remote machine.
     */
    @OnThread(Tag.Any)
    public abstract void disposeWindows();

    /**
     * "Run" a class (i.e. invoke its main method without arguments)
     */
    @OnThread(Tag.NOTVMEventHandler)
    public abstract DebuggerResult runClassMain(String className)
        throws ClassNotFoundException;

    /**
     * Instantiate a class using the default constructor for that class.
     * @param className  The name of the class to instantiate
     * @return   The result of the constructor call
     */
    @OnThread(Tag.NOTVMEventHandler)
    public abstract DebuggerResult instantiateClass(String className);

    /**
     * Instantiate a class using a specific constructor for that class.
     * 
     * @param className  The name of the class to instantiate
     * @param argTypes   The formal parameter types (class names)
     * @param args       The arguments
     * @return   The result of the constructor call
     */
    @OnThread(Tag.NOTVMEventHandler)
    public abstract DebuggerResult instantiateClass(String className, String [] paramTypes,
            DebuggerObject [] args);
    
    /**
     * Get a class from the virtual machine, using the current classloader.
     * 
     * @param className   The name of the class to load
     * @param initialize  Whether to initialize the class. Initialization causes execution
     *                    of user code, which may take an arbitrary amount of time.
     *                    Initialization will not be performed if the debugger is already
     *                    running user code (i.e. the state is RUNNING).
     * 
     * @throws ClassNotFoundException if the class couldn't be located.
     */
    @OnThread(Tag.NOTVMEventHandler)
    public abstract FXPlatformSupplier<DebuggerClass> getClass(String className, boolean initialize)
        throws ClassNotFoundException;

    public abstract CompletableFuture<FXPlatformSupplier<DebuggerResult>> launchFXApp(String className);

    /**
     * Get a reference to a string in the remote machine whose value is the
     * same as the given value. Returns null if the remote VM terminates
     * or the string cannot be mirrored for some other reason (such as
     * out of memory).
     * 
     * @param value  The string value to mirror
     * @return       The remote object with the same value, or null
     */
    @OnThread(Tag.FXPlatform)
    public abstract DebuggerObject getMirror(String value);

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     * @param properties Extra properties to set on the breakpoint.  Can (and usually should) be null.
     * @return           a string of the error message generated performing
     *                   this operation or null
     */
    @OnThread(Tag.FXPlatform)
    public abstract String toggleBreakpoint(String className, int line,
                                            boolean set, Map<String, String> properties);

    /**
     * Set/clear a breakpoint at a specified method in a class (specified by name).
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     * @param properties Extra properties to set on the breakpoint.  Can (and usually should) be null.
     * @return           a string of the error message generated performing
     *                   this operation or null
     */
    @OnThread(Tag.FXPlatform)
    public abstract String toggleBreakpoint(String className, String method, boolean set,
                                            Map<String,String> properties);
    
    /**
     * Set/clear a breakpoint at a specified method in a class.
     * It is safe to call this method from a debugger event listener (unlike
     * the other toggleBreakpoint() methods).
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     * @param properties Extra properties to set on the breakpoint.  Can (and usually should) be null.
     * @return           a string of the error message generated performing
     *                   this operation or null
     */
    @OnThread(Tag.Any)
    public abstract String toggleBreakpoint(DebuggerClass debuggerClass, String method, boolean set,
            Map<String, String> properties);

    /**
     * Sets which thread invoked methods/constructors should be run on.
     */
    public abstract void setRunOnThread(RunOnThread runOnThread);
    
    public static interface EventHandlerRunnable
    {
        @OnThread(Tag.VMEventHandler)
        public void run();
    }
    
    @OnThread(Tag.Any)
    public abstract void runOnEventHandler(EventHandlerRunnable runnable);
}
