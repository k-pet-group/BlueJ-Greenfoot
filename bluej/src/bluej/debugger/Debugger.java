package bluej.debugger;

import java.io.File;
import java.util.Map;

import bluej.debugger.jdi.JdiDebugger;

/**
 * A class defining the debugger primitives needed by BlueJ
 * May be supported by different implementations, locally or remotely.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: Debugger.java 2039 2003-06-19 06:03:24Z ajp $
 */
public abstract class Debugger
{
    public static final int NORMAL_EXIT = 0;
    public static final int FORCED_EXIT = 1;
    public static final int EXCEPTION = 2;
    public static final int TERMINATED = 3;

    // machine states
    public static final int NOTREADY = 0;
    public static final int IDLE = 1;
	public static final int RUNNING = 2;
    public static final int SUSPENDED = 3;

	/**
	 * Create an instance of a debugger.
	 * The constructor for the debugger should not be
	 * a long process. Actual startup for the debug
	 * VM should go in launch().
	 * 
	 * @return  a Debugger instance
	 */
	public static Debugger getDebuggerImpl(File startingDirectory)
	{
        return new JdiDebugger(startingDirectory);
	}
	
	/**
     * Start debugging.
     * 
     * This can be a lengthy process so this should be executed
     * in a sub thread.
     */
    public abstract void launch();

    /**
     * Finish debugging.
     */
    public abstract void close();

	/**
	 * Add a listener for DebuggerEvents
	 * 
	 * @param l  the DebuggerListener to add
	 */
	public abstract void addDebuggerListener(DebuggerListener l);

	/**
	 * Remove a listener for DebuggerEvents.
	 * 
	 * @param l  the DebuggerListener to remove
	 */
	public abstract void removeDebuggerListener(DebuggerListener l);
	
    /**
     * Create a class loader in the debugger.
     */
    public abstract void newClassLoader(String classPath);

	/**
     * Create a class loader in the debugger but retain
     * any user created breakpoints.
	 */
	public abstract void newClassLoaderLeavingBreakpoints(String classPath);

    /**
     * Add a debugger object into the project scope.
     * 
     * @param   newInstanceName  the name of the object
     *          dob              the object itself
     * @return  true if the object could be added with this name,
     *          false if there was a name clash.
     */
    public abstract boolean addObject(String newInstanceName, DebuggerObject dob);

    /**
     * Remove a debugger object from the project scope.
     */
    public abstract void removeObject(String instanceName);

	/**
	 * Return the debugger objects that exist in the
	 * debugger.
	 * 
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
	public abstract Map getObjects();
	
	/**
	 * Guess a suitable name for an object about to be put on the object bench.
	 * 
	 * @param	startingName  a fully qualified class name (will be stripped of
	 *                        qualifying part) or a field name that will be used
	 *                        as the basis for the new name.
	 * @return				  a String suitable as a name for an object on the
	 * 						  object bench. 
	 */
	public abstract String guessNewName(String className);

    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED).
     */
    public abstract int getStatus();
    
	/**
	 * Run the setUp() method of a test class and return the created
	 * objects.
	 * 
	 * @param className	the fully qualified name of the class
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
    public abstract Map runTestSetUp(String className);

	/**
	 * Run a single test method in a test class and return the result.
	 * 
	 * @param  className  the fully qualified name of the class
	 * @param  methodName the name of the method
	 * @return            a DebuggerTestResult object
	 */
    public abstract DebuggerTestResult runTestMethod(String className, String methodName);

    /**
     * Dispose all top level windows in the remote machine.
     */
    public abstract void disposeWindows();

    /**
     * "Run" a class (i.e. invoke its main method without arguments)
     */
    public abstract void runClassMain(String className)
    	throws ClassNotFoundException;

    /**
     * Get a class from the virtual machine.
     */
    public abstract DebuggerClass getClass(String className)
		throws ClassNotFoundException;

    /**
     * Get the value of a static field in a class
     */
    public abstract DebuggerObject getStaticValue(String className, String fieldName)
		throws ClassNotFoundException;

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     * @return           a string of the error message generated performing
     *                   this operation or null
     */
    public abstract String toggleBreakpoint(String className, int line,
                                            boolean set);

    /**
     * Return the status of the last runClassMain(). One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, BREAKPOINT, TERMINATED).
     */
    public abstract int getExitStatus();


    /**
     * Return a description of the last exception if the status of
     * the last runClassMain() was EXCEPTION.
     */
    public abstract ExceptionDescription getException();

    /**
     * A tree model representing the threads running in
     * the debug VM.
     *  
     * @return  a TreeModel with DebuggerThread objects
     *          as the leaves.
     */
    public abstract DebuggerThreadTreeModel getThreadTreeModel();
}
