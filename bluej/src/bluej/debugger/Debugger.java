package bluej.debugger;

import java.util.List;
import java.util.Map;

import bluej.debugger.jdi.JdiDebugger;

/**
 * A class defining the debugger primitives needed by BlueJ
 * May be supported by different implementations, locally or remotely.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 *
 * @version $Id: Debugger.java 1954 2003-05-15 06:06:01Z ajp $
 */
public abstract class Debugger
{
    public static final int NORMAL_EXIT = 0;
    public static final int FORCED_EXIT = 1;
    public static final int EXCEPTION = 2;
    public static final int TERMINATED = 3;

    // machine states
    public static final int IDLE = 0;
    public static final int RUNNING = 1;
    public static final int SUSPENDED = 2;

	/**
	 * Create an instance of a debugger.
	 * The constructor for the debugger should not be
	 * a long process. Actual startup for the debug
	 * VM should go in startDebugger().
	 * 
	 * @return  a Debugger instance
	 */
	public static Debugger getDebuggerImpl()
	{
		return new JdiDebugger();
	}
	
	/**
     * Start debugging
     */
    public abstract void startDebugger();


    /**
     * Finish debugging
     */
    public abstract void endDebugger();


    /**
     * Create a class loader
     */
    public abstract DebuggerClassLoader createClassLoader(String scopeId,
                                                          String classpath);

    /**
     * Remove a class loader
     */
    public abstract void removeClassLoader(DebuggerClassLoader loader);


    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public abstract void addObjectToScope(String scopeId, String newObjectName,
                                            DebuggerObject dob);


    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public abstract void removeObjectFromScope(String scopeId,
                                               String instanceName);


    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED).
     */
    public abstract int getStatus();


    /**
     * Set the remote VM classpath
     */
    public abstract void setLibraries(String classpath);

	/**
	 * Run the setUp() method of a test class and return the created
	 * objects.
	 * 
	 * @param loadId	the ID representing the classloader on the remote VM
	 * @param scopeId	the scope ID representing the object bench on the remote VM
	 * @param className	the fully qualified name of the class
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
    public abstract Map runTestSetUp(String loadId, String scopeId, String className);

	/**
	 * Run a single test method in a test class and return the result.
	 * 
	 * @param loadId	the ID representing the classloader on the remote VM
	 * @param scopeId	the scope ID representing the object bench on the remote VM
	 * @param className	the fully qualified name of the class
	 * @param methodName the name of the method
	 * @return			a DebuggerTestResult object
	 */
    public abstract DebuggerTestResult runTestMethod(String loadId, String scopeId, String className, String methodName);

    /**
     * Dispose all top level windows in the remote machine.
     */
    public abstract void disposeWindows();


    /**
     * "Start" a class (i.e. invoke its main method without arguments)
     */
    public abstract void startClass(DebuggerClassLoader loader,
                                    String classname, Object eventParam);


    /**
     * Get a class from the virtual machine.
     */
    public abstract DebuggerClass getClass(String className, DebuggerClassLoader loader);

    /**
     * Get the value of a static field in a class
     */
    public abstract DebuggerObject getStaticValue(String className, String fieldName);

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     */
    public abstract String toggleBreakpoint(String className, int line,
                                            boolean set,
                                            DebuggerClassLoader loader);

    /**
     * Save the breakpoints which have been setup in the debugger
     */
    public abstract void saveBreakpoints();

    /**
     * Restore the previously saved breakpoints using the new class loader
     */
    public abstract void restoreBreakpoints(DebuggerClassLoader loader);

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, BREAKPOINT, TERMINATED).
     */
    public abstract int getExitStatus();


    /**
     * Return a description of the last exception.
     */
    public abstract ExceptionDescription getException();


    /**
     * List all the threads being debugged as a list containing elements
     * of type DebuggerThread. Filter out threads that belong to system,
     * returning only user threads. This can be done only if the machine
     * is currently suspended.
     *
     * @return  A list of threads, or null if the machine is currently
     *		running
     */
    public abstract List listThreads();


    /**
     * Stop the machine. It can be restarted later with "cont()".
     */
    public abstract void halt(DebuggerThread thread);


    /**
     * A thread has been started again by the user. Make sure that it
     * is indicated in the interface.
     */
    public abstract void cont();


    /**
     * Terminate a thread in the machine.
     */
    public abstract void terminate(DebuggerThread thread);


    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread. The currently selected frame is stored in the
     * thread object itself.
     */
    public abstract void showSource(DebuggerThread thread);
}
