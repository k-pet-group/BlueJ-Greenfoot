package bluej.debugger;

import bluej.runtime.BlueJRuntime;
import bluej.pkgmgr.Package;

/**
 ** @version $Id: LocalDebugger.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** A class implementing the debugger primitives needed by BlueJ
 ** Implemented in the local VM (via reflection, etc.)
 **/

public class LocalDebugger extends Debugger
{
    boolean active = false;
	
    /**
     ** Start debugging
     **/
    protected void startDebugger()
    {
	// TODO: start up
			
	active = true;
    }
	
    /**
     ** Finish debugging
     **/
    protected void finishDebugging()
    {
	if(active)
	    {
		// TODO: shut down
			
		active = false;
	    }
    }
	
    /**
     ** Check whether we are currently debugging
     **/
    public boolean isActive()
    {
	return active;
    }
	
    /**
     ** Get a class loader
     **/
    public DebuggerClassLoader createClassLoader(String scopeId, String classpath)
    {
	return null;
    }
	
    /**
     ** Remove a class loader
     **/
    public void removeClassLoader(DebuggerClassLoader loader)
    {
    }
	
    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName)
    {
    }

    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
    }

    /**
     * Load a class into the remote machine
     */
    public void loadClass(DebuggerClassLoader loader, String classname)
    {
    }

    /**
     * "Start" a class (i.e. invoke its main method)
     */
    public void startClass(DebuggerClassLoader loader, String classname, 
			   String[] args, Package pkg)
    {
    }
	
    /**
     * Have BlueJRuntime execute a command
     */
    private void runtimeCmd(String[] args)
    {
	// debugger.
    }

    /**
     ** Get the value of a static field in a class
     **/
    public DebuggerObject getStaticValue(String className, String fieldName)
    {
	return null;
    }
	
    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @arg className  The class in which to set the breakpoint.
     * @arg line       The line number of the breakpoint.
     * @arg set        True to set, false to clear a breakpoint.
     */
    public String toggleBreakpoint(String className, int line, boolean set,
				   DebuggerClassLoader loader)
    {
	return "";
    }

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION).
     */ 
    public int getExitStatus()
    {
	return 0;
    }

    /**
     * Return the text of the last exception.
     */
    public String getExceptionText()
    {
	return "";
    }

    /**
     ** List all the threads being debugged
     **/
    public DebuggerThread[] listThreads()
	throws Exception
    {
	return new DebuggerThread[0];
    }

    /**
     * A thread has been stopped by the user. Make sure that the source 
     * is shown.
     */
    public void threadStopped(DebuggerThread thread)
    {
    }

    /**
     * A thread has been started again by the user. Make sure that it 
     * is indicated in the interface.
     */
    public void threadContinued(DebuggerThread thread)
    {
    }

    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread.
     */
    public void showSource(DebuggerThread thread, int frameNo)
    {
    }

}
