package bluej.debugger;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.runtime.BlueJRuntime;
import bluej.pkgmgr.Package;

import java.util.Hashtable;
import java.util.Vector;
import sun.tools.debug.*;

/**
 ** @version $Id: SunDebugger.java 56 1999-04-30 01:33:50Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** A class implementing the debugger primitives needed by BlueJ
 ** Implemented in a remote VM (via sun.tools.debug)
 **/

public class SunDebugger extends Debugger

	implements DebuggerCallback
{
    static final String RUNTIME_CLASSNAME = "bluej.runtime.BlueJRuntime";
    static final String MAIN_THREADGROUP = "bluej.runtime.BlueJRuntime.main";
	
    private RemoteDebugger remoteDebugger;
    private Hashtable waitqueue = new Hashtable();
    private int exitStatus;
    private String exceptionMsg;

    /**
     * Start debugging. I.e. create the second virtual machine (which
     *  we from now on refer to as the "RemoteDebugger").
     */
    public synchronized void startDebugger()
    {
	if(remoteDebugger != null)
	    return;

	try {
	    BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM, null);
	    remoteDebugger = new RemoteDebugger("", this, false);
	    String[] args = { BlueJRuntime.INIT };
	    runtimeCmd(args, "");		// Initialise
	    BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
	} catch(Exception e) {
	    Utility.reportError("Failed to start debugger: " + e);
	}
    }
	
    /**
     * Finish debugging
     */
    protected synchronized void finishDebugging()
    {
	if(remoteDebugger != null) {
	    remoteDebugger.close();
	    remoteDebugger = null;
	}
    }
	
    /**
     * Check whether we are currently debugging
     */
    public boolean isActive()
    {
	return (remoteDebugger != null);
    }
	
    private RemoteDebugger getDebugger()
    {
	if(remoteDebugger == null)
	    startDebugger();
	return remoteDebugger;
    }

    /**
     * Create a class loader. This really creates two distinct class
     *  loader objects: a DebuggerClassLoader which is handed back to the
     *  caller and a BlueJClassLoader internally in the BlueJRuntime.
     *  The DebuggerClassLoader serves as a handle to the
     *  BlueJClassLoader. The connection is made by an ID (a String),
     *  stored in the DebuggerClassLoader, with which the BlueJClassLoader
     *  can be looked up.
     */
    public DebuggerClassLoader createClassLoader(String scopeId, String classpath)
    {
	String[] args = { BlueJRuntime.CREATE_LOADER, scopeId, classpath };
	runtimeCmd(args, "");

	return new SunClassLoader(scopeId);
    }
	
    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
	String[] args = { BlueJRuntime.REMOVE_LOADER, ((SunClassLoader)loader).getId() };
	runtimeCmd(args, "");
    }

    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName)
    {
	String[] args = { BlueJRuntime.ADD_OBJECT, 
			  scopeId, instanceName, fieldName, newObjectName };
	runtimeCmd(args, "");
    }
	
    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
	String[] args = { BlueJRuntime.REMOVE_OBJECT, 
			  scopeId, instanceName };
	runtimeCmd(args, "");
    }

    /**
     * Load a class into the remote machine
     */
    public void loadClass(DebuggerClassLoader loader, String classname)
    {
	String[] args = { BlueJRuntime.LOAD_CLASS, 
			  ((SunClassLoader)loader).getId(), classname };
	runtimeCmd(args, "");
    }

    /**
     * "Start" a class (i.e. invoke its main method)
     */
    public void startClass(DebuggerClassLoader loader, String classname, 
			   String[] args, Package pkg)
    {
	int length = (args == null) ? 0 : args.length;
	String[] allArgs = new String[length + 3];
	allArgs[0] = BlueJRuntime.START_CLASS;
	allArgs[1] = ((SunClassLoader)loader).getId();
	allArgs[2] = classname;
		
	if(args != null)
	    System.arraycopy(args, 0, allArgs, 3, args.length);

	exitStatus = NORMAL_EXIT;	// for now, we assume all goes okay...
	
	runtimeCmd(allArgs, pkg);
    }
	
    /**
     * Have BlueJRuntime execute a command. This is done synchronously -
     * the calling thread is held here until the remote thread has finished.
     *
     * This method is executed by the BlueJ compile/exec thread.
     */
    private synchronized void runtimeCmd(String[] args, Object pkg)
    {
	// Execute a BlueJ "runtime command" via
	// bluej.runtime.BlueJRuntime

	String[] allArgs = new String[args.length + 1];
	allArgs[0] = RUNTIME_CLASSNAME;
	System.arraycopy(args, 0, allArgs, 1, args.length);
		
	try {
	    RemoteThreadGroup tg = getDebugger().run(allArgs.length, allArgs);
	    RemoteThread[] threads = tg.listThreads(false);
			
	    if(threads.length > 0) {
		RemoteThread thread = threads[0];

		//Debug.message("runtimeCmd(" + args[0] + ") waiting on thread " + thread);
		waitqueue.put(thread, pkg);

		while(waitqueue.containsKey(thread)) {
		    try {
			wait();
			//Debug.message("runtimeCmd(" + args[0] + ") woke up");
		    } catch(InterruptedException e) {
			// ignore it
		    }
		}
	    }
	    else {
		//Debug.message("runtimeCmd(" + args[0] + ") finished");
	    }
	} catch(Exception e) {
	    Utility.reportError("exception executing runtime command " + e);
	}
    }

    /**
     * Get the value of a static field in a class.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
	throws Exception
    {
	RemoteClass cl = getDebugger().findClass(className);
	RemoteObject obj = (RemoteObject)cl.getFieldValue(fieldName);
	DebuggerObject ret = new SunObject(obj);
		
	return ret;
    }

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     */
    public String toggleBreakpoint(String className, int line, boolean set,
				   DebuggerClassLoader loader)
    {
	try {
	    loadClass(loader, className);
	    RemoteClass cl = getDebugger().findClass(className);
	    if(cl == null)
		return "Class not found";

	    String result;
	    if(set)
		result = cl.setBreakpointLine(line);
	    else
		result = cl.clearBreakpointLine(line);
	    return result;
	}
	catch (Exception e) {
	    Debug.message("could not set breakpoint: " + e);
	    return "Internal error while setting breakpoint";
	}

    }

//      /**
//       * Get the value of a static integer field of a remote class.
//       */
//      public int getStaticIntValue(String className, String fieldName)
//  	throws Exception
//      {
//  	RemoteClass cl = getDebugger().findClass(className);
//  	RemoteInt val = (RemoteInt)cl.getFieldValue(fieldName);
//  	return val.get();
//      }
	
//      /**
//       * Get the value of a static String field of a remote class.
//       */
//      public String getStaticStringValue(String className, String fieldName)
//  	throws Exception
//      {
//  	RemoteClass cl = getDebugger().findClass(className);
//  	RemoteString val = (RemoteString)cl.getFieldValue(fieldName);
//  	return val.toString();
//      }

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION).
     */ 
    public int getExitStatus()
    {
	return exitStatus;
    }

    /**
     * Return the text of the last exception.
     */
    public String getExceptionText()
    {
	return exceptionMsg;
    }

    /**
     * List all the threads being debugged
     */
    public DebuggerThread[] listThreads()
	throws Exception
    {
	RemoteThreadGroup[] tgroups = getDebugger().listThreadGroups(null);
		
	Vector allThreads = new Vector();
	for(int i = 0; i < tgroups.length; i++) {
	    if(tgroups[i].getName().equals(MAIN_THREADGROUP)) {
		RemoteThread[] threads = tgroups[i].listThreads(false);
		for(int j = 0; j < threads.length; j++) {
		    allThreads.addElement(new SunThread(threads[j]));
		    //Debug.message("thread: " + threads[j].getName() +
		    //		  " group: " + tgroups[i].getName());
		}
	    }
	}

	int len = allThreads.size();
	DebuggerThread[] ret = new DebuggerThread[allThreads.size()];
	// reverse order to make display nicer (newer threads first)
	for(int i = 0; i < len; i++)
	    ret[i] = (DebuggerThread)allThreads.elementAt(len - i - 1);
	return ret;
    }
	
    /**
     * A thread has been stopped by the user. Make sure that the source 
     * is shown.
     */
    public void threadStopped(DebuggerThread thread)
    {
	Package pkg = (Package)waitqueue.get(
				     ((SunThread)thread).getRemoteThread());

	if(pkg == null)
	    Utility.reportError("cannot find class for stopped thread");
	else {
	    pkg.hitBreakpoint(thread.getClassSourceName(0),
			      thread.getLineNumber(0), 
			      thread.getName(), true);
	}
    }

    /**
     * A thread has been started again by the user. Make sure that it 
     * is indicated in the interface.
     */
    public void threadContinued(DebuggerThread thread)
    {
	Package pkg = (Package)waitqueue.get(
				     ((SunThread)thread).getRemoteThread());
	if(pkg != null)
	    pkg.getFrame().continueExecution();
    }

    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread.
     */
    public void showSource(DebuggerThread thread, int frameNo)
    {
	Package pkg = (Package)waitqueue.get(
				     ((SunThread)thread).getRemoteThread());

	if(pkg != null) {
	    pkg.hitBreakpoint(thread.getClassSourceName(frameNo),
			      thread.getLineNumber(frameNo), 
			      thread.getName(), false);
	}
    }

    // --- DebuggerCallback interface --- 
	
    /**
     * Print text to the debugger's console window.
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public void printToConsole(String text) throws Exception
    {
	System.out.print(text);
    }

    /**
     * A breakpoint has been hit in the specified thread. Find the user
     * thread that started the execution and let it continue. (The user
     * thread is waiting in the waitqueue.)
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public synchronized void breakpointEvent(RemoteThread rt) throws Exception
    {
	//Debug.message("SunDebugger: breakpointEvent " + rt);

	Package pkg = (Package)waitqueue.get(rt);

	if(pkg == null)
	    Utility.reportError("cannot find thread for breakpoint");
	else {
	    SunThread thread = new SunThread(rt);
	    pkg.hitBreakpoint(thread.getClassSourceName(0), 
			      thread.getLineNumber(0), 
			      thread.getName(), true);
	}
    }

    /**
     * An exception has occurred.
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public synchronized void exceptionEvent(RemoteThread rt, String errorText)
	throws Exception
    {
	//Debug.message("SunDebugger: exception event ");

	// System.exit() gets caught by the security manager and translated
	// into an exception called "BlueJ-Exit". First, we check for this.
	// Otherwise it's a real exception.

	int pos = errorText.indexOf("BlueJ-Exit:");
	if(pos != -1) {
	    exitStatus = FORCED_EXIT;
	    // get exit code
	    int endpos = errorText.indexOf(":", pos+11);  // find second ":"
	    exceptionMsg = errorText.substring(pos+11, endpos);
	}
	else {
	    exitStatus = EXCEPTION;
	    exceptionMsg = errorText;
	}

	if(waitqueue.containsKey(rt)) {	// someone is waiting on this...
	    waitqueue.remove(rt);
	    notifyAll();
	}
	else
	    rt.stop();
    }

    /**
     * A thread has died.
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public synchronized void threadDeathEvent(RemoteThread rt) 
	throws Exception
    {
	//Debug.message("SunDebugger: threadDeathEvent " + rt);

	if(waitqueue.containsKey(rt)) {	// someone is waiting on this...
	    waitqueue.remove(rt);
	    notifyAll();
	}
	else
	    // Don't need the thread any more - it can go away
	    rt.stop();
    }

    /**
     * The client interpreter has exited, either by returning from its
     *  main thread, or by calling System.exit().
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public void quitEvent() throws Exception
    {
	Debug.message("SunDebugger: quitEvent");
    }
}
