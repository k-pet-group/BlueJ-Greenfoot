package bluej.debugger;

// -- select debugger implementation to use: --
//import bluej.debugger.suntools.SunDebugger;
import bluej.debugger.jdi.JdiDebugger;

import java.util.Map;
import java.util.List;

/**
 * A class defining the debugger primitives needed by BlueJ
 * May be supported by different implementations, locally or remotely.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 *
 * @version $Id: Debugger.java 1559 2002-12-06 03:46:43Z ajp $
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

    /** Creation of the real debugger used **/
    // the following line needs to be changed when the debugger
    // implementation changes (this and the import statement above are the
    // only two lines that need to be changed).

    public static Debugger debugger = new JdiDebugger();
    //public static Debugger debugger = new SunDebugger();

    public static void handleExit()
    {
        if(debugger != null) {
            debugger.endDebugger();
            debugger = null;
        }
    }

    /**
     * Start debugging
     */
    protected abstract void startDebugger();


    /**
     * Finish debugging
     */
    protected abstract void endDebugger();


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
     * Set the remote "current directory" for relative file access.
     */
    public abstract void setDirectory(String path);

    public abstract Map runTestSetUp(String loadId, String scopeId, String className);

    public abstract void runTestClass(String loadId, String scopeId, String className);

    public abstract void runTestMethod(String loadId, String scopeId, String className, String methodName);

    /**
     * Serialize an object in the debugger to a file
     */
    public abstract void serializeObject(String scopeId, String instanceName,
                                         String fileName);


    /**
     * Deserialize an object in the debugger from a file
     */
    public abstract DebuggerObject deserializeObject(String loaderId,
                                                     String scopeId,
                                                     String newInstanceName,
                                                     String fileName);

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

    /**
     * Return the jdi thread. This exposes the jdi to Inspectors.
     * If jdi is not being used, it should return null, which is
     * the default implementation.
     */
    public com.sun.jdi.ThreadReference getServerThread() {
        return null;
    }

}
