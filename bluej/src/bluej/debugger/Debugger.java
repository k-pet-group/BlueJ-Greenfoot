package bluej.debugger;

// select debugger implementation to use:
import bluej.debugger.suntools.SunDebugger;
//import bluej.debugger.jdi.JdiDebugger;

import bluej.pkgmgr.Package;

/**
 ** A class defining the debugger primitives needed by BlueJ
 ** May be supported by different implementations, locally or remotely.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: Debugger.java 86 1999-05-18 02:49:53Z mik $
 **/

public abstract class Debugger
{
    public static final int NORMAL_EXIT = 0;
    public static final int FORCED_EXIT = 1;
    public static final int EXCEPTION = 2;

    /** Creation of the real debugger used **/
    // the following line needs to be changed when the debugger 
    // implementation changes (this and the import statement abobe are the 
    // only two lines that need to be changed).

    //public static Debugger debugger = new JdiDebugger();
    public static Debugger debugger = new SunDebugger();
	
    public static void handleExit()
    {
	if(debugger != null) {
	    debugger.finishDebugging();
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
    protected abstract void finishDebugging();

    /**
     * Check whether we are currently debugging
     */
    public abstract boolean isActive();

    /**
     * Create a class loader
     */
    public abstract DebuggerClassLoader createClassLoader(String scopeId, String classpath);

    /**
     * Remove a class loader
     */
    public abstract void removeClassLoader(DebuggerClassLoader loader);

    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public abstract void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName);

    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public abstract void removeObjectFromScope(String scopeId, String instanceName);

    /**
     * Load a class into the remote machine
     */
    public abstract void loadClass(DebuggerClassLoader loader, 
				    String classname);

    /**
     * "Start" a class (i.e. invoke its main method)
     */
    public abstract void startClass(DebuggerClassLoader loader, 
				    String classname, String[] args, 
				    Package pkg);

    /**
     * Show or hide the text terminal.
     */
    public abstract void showTerminal(boolean show);

    /**
     * Clear the text terminal.
     */
    public abstract void clearTerminal();

    /**
     * Get the value of a static field in a class
     */
    public abstract DebuggerObject getStaticValue(String className, String fieldName)
	throws Exception;
	
    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @arg className  The class in which to set the breakpoint.
     * @arg line       The line number of the breakpoint.
     * @arg set        True to set, false to clear a breakpoint.
     */
    public abstract String toggleBreakpoint(String className, int line, 
					    boolean set, 
					    DebuggerClassLoader loader);

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, BREAKPOINT).
     */ 
    public abstract int getExitStatus();

    /**
     * Return the text of the last exception.
     */
    public abstract String getExceptionText();

    /**
     * List all the threads being debugged
     */
    public abstract DebuggerThread[] listThreads()
	throws Exception;

    /**
     * A thread has been stopped by the user. Make sure that the source 
     * is shown.
     */
    public abstract void threadStopped(DebuggerThread thread);

    /**
     * A thread has been started again by the user. Make sure that it 
     * is indicated in the interface.
     */
    public abstract void threadContinued(DebuggerThread thread);

    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread.
     */
    public abstract void showSource(DebuggerThread thread, int frameNo);
}
