package bluej.debugger.jdi;

import bluej.debugger.*;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.runtime.BlueJRuntime;
import bluej.pkgmgr.Package;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Map;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;

import java.io.*;

/**
 ** A class implementing the debugger primitives needed by BlueJ
 ** Implemented in a remote VM (via JDI interface)
 **
 ** @author Michael Kolling
 **/

public class JdiDebugger extends Debugger
{
    static final String RUNTIME_CLASSNAME = "bluej.runtime.BlueJRuntime";
    static final String MAIN_THREADGROUP = "bluej.runtime.BlueJRuntime.main";
	

    private VirtualMachine vm = null;
    private Process process = null;
    private int outputCompleteCount = 0;

    private Hashtable waitqueue = new Hashtable();
    private int exitStatus;
    private String exceptionMsg;

    /**
     * Start debugging. I.e. create the second virtual machine (which
     *  we from now on refer to as the "RemoteDebugger").
     */
    public synchronized void startDebugger()
    {
  	if(vm != null)
  	    return;

	BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM, null);

        VirtualMachineManager mgr = Bootstrap.virtualMachineManager();
        LaunchingConnector connector;

        connector = mgr.defaultConnector();
	//Debug.message("connector: " + connector.name());
	//Debug.message("transport: " + connector.transport().name());

        Map arguments = connector.defaultArguments();

	// "main" is the command line: main class and arguments
        Connector.Argument mainArg = 
	    (Connector.Argument)arguments.get("main");

	// "options" contains runtime options to the target VM
        Connector.Argument optionsArg = 
	    (Connector.Argument)arguments.get("options");

        if ((optionsArg == null) || (mainArg == null)) {
	    Debug.reportError("Cannot start virtual machine.");
	    Debug.reportError("(Incompatible launch connector)");
	    return;
        }

        mainArg.setValue("");
        optionsArg.setValue("");

        try {
            //VirtualMachine vm = connector.launch(arguments);
            //process = vm.process();
            //displayRemoteOutput(process.getErrorStream());
            //displayRemoteOutput(process.getInputStream());
	    BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
        } catch (Exception e) {
            Debug.reportError("Unable to launch target VM.");
            e.printStackTrace();
        }
    }
	
    /**
     * Finish debugging
     */
    protected synchronized void finishDebugging()
    {
	Debug.message("[finishDebugging]");
//  	if(remoteDebugger != null) {
//  	    remoteDebugger.close();
//  	    remoteDebugger = null;
//  	}
    }
	
    /**
     * Check whether we are currently debugging
     */
    public boolean isActive()
    {
	Debug.message("[isActive]");
	return true; // ###
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
	Debug.message("[createClassLoader]");
//  	String[] args = { BlueJRuntime.CREATE_LOADER, scopeId, classpath };
//  	runtimeCmd(args, "");

//  	return new JdiClassLoader(scopeId);
	return null;
    }
	
    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
	Debug.message("[removeClassLoader]");
//  	String[] args = { BlueJRuntime.REMOVE_LOADER, ((JdiClassLoader)loader).getId() };
//  	runtimeCmd(args, "");
    }

    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName)
    {
	Debug.message("[addObjectToScope]");
//  	String[] args = { BlueJRuntime.ADD_OBJECT, 
//  			  scopeId, instanceName, fieldName, newObjectName };
//  	runtimeCmd(args, "");
    }
	
    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
	Debug.message("[removeObjectFromScope]");
//  	String[] args = { BlueJRuntime.REMOVE_OBJECT, 
//  			  scopeId, instanceName };
//  	runtimeCmd(args, "");
    }

    /**
     * Load a class into the remote machine
     */
    public void loadClass(DebuggerClassLoader loader, String classname)
    {
	Debug.message("[loadClass]");
//  	String[] args = { BlueJRuntime.LOAD_CLASS, 
//  			  ((JdiClassLoader)loader).getId(), classname };
//  	runtimeCmd(args, "");
    }

    /**
     * "Start" a class (i.e. invoke its main method)
     */
    public void startClass(DebuggerClassLoader loader, String classname, 
			   String[] args, Package pkg)
    {
	Debug.message("[startClass]");
  	if(vm != null)
  	    return;

	BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM, null);

        VirtualMachineManager mgr = Bootstrap.virtualMachineManager();
        LaunchingConnector connector;

        connector = mgr.defaultConnector();
        Debug.message("connector: " + connector.name());
        Debug.message("transport: " + connector.transport().name());

        Map arguments = connector.defaultArguments();

	// "main" is the command line: main class and arguments
        Connector.Argument mainArg = 
	    (Connector.Argument)arguments.get("main");

	// "options" contains runtime options to the target VM
        Connector.Argument optionsArg = 
	    (Connector.Argument)arguments.get("options");

        if ((optionsArg == null) || (mainArg == null)) {
	    Debug.reportError("Cannot start virtual machine.");
	    Debug.reportError("(Incompatible launch connector)");
	    return;
        }

	String command = classname;
	if(args != null)
	    for (int i=0; i < args.length; i++)
		command = command + " " + args[i];

        mainArg.setValue(command);
        optionsArg.setValue("");

        try {
            VirtualMachine vm = connector.launch(arguments);
            process = vm.process();
            //displayRemoteOutput(process.getErrorStream());
            //displayRemoteOutput(process.getInputStream());
	    BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
	} catch (VMStartException vmse) {
            Debug.reportError("Target VM did not initialise.");
            Debug.reportError(vmse.getMessage() + "\n");
            dumpFailedLaunchInfo(vmse.process());
        } catch (Exception e) {
            Debug.reportError("Unable to launch target VM.");
            e.printStackTrace();
        }
//  	int length = (args == null) ? 0 : args.length;
//  	String[] allArgs = new String[length + 3];
//  	allArgs[0] = BlueJRuntime.START_CLASS;
//  	allArgs[1] = ((JdiClassLoader)loader).getId();
//  	allArgs[2] = classname;
		
//  	if(args != null)
//  	    System.arraycopy(args, 0, allArgs, 3, args.length);

//  	exitStatus = NORMAL_EXIT;	// for now, we assume all goes okay...
	
//  	runtimeCmd(allArgs, pkg);
    }

    private void dumpFailedLaunchInfo(Process process) {
        try {
            dumpStream(process.getErrorStream());
            dumpStream(process.getInputStream());
        } catch (IOException e) {
            Debug.message("Unable to display process output: " +
			  e.getMessage());
        }
    }

    private void dumpStream(InputStream stream) throws IOException {
        PrintStream outStream = System.out;
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(stream));
        String line;
        while ((line = in.readLine()) != null) {
            outStream.println(line);
        }
    }


    /**
     * Show or hide the text terminal.
     */
    public void showTerminal(boolean show)
    {
//  	String[] args = new String[2];
//  	args[0] = BlueJRuntime.TERM_COMMAND;
//  	if(show)
//  	    args[1] = BlueJRuntime.TC_SHOW;
//  	else
//  	    args[1] = BlueJRuntime.TC_HIDE;
//  	runtimeCmd(args, "");
    }

    /**
     * Clear the text terminal.
     */
    public void clearTerminal()
    {
//  	String[] args = { BlueJRuntime.TERM_COMMAND, BlueJRuntime.TC_CLEAR };
//  	runtimeCmd(args, "");
    }

    /**
     * Have BlueJRuntime execute a command. This is done synchronously -
     * the calling thread is held here until the remote thread has finished.
     *
     * This method is executed by the BlueJ compile/exec thread.
     */
    private synchronized void runtimeCmd(String[] args, Object pkg)
    {
//  	// Execute a BlueJ "runtime command" via
//  	// bluej.runtime.BlueJRuntime

//  	String[] allArgs = new String[args.length + 1];
//  	allArgs[0] = RUNTIME_CLASSNAME;
//  	System.arraycopy(args, 0, allArgs, 1, args.length);
		
//  	try {
//  	    RemoteThreadGroup tg = getDebugger().run(allArgs.length, allArgs);
//  	    RemoteThread[] threads = tg.listThreads(false);
			
//  	    if(threads.length > 0) {
//  		RemoteThread thread = threads[0];

//  		//Debug.message("runtimeCmd(" + args[0] + ") waiting on thread " + thread);
//  		waitqueue.put(thread, pkg);

//  		while(waitqueue.containsKey(thread)) {
//  		    try {
//  			wait();
//  			//Debug.message("runtimeCmd(" + args[0] + ") woke up");
//  		    } catch(InterruptedException e) {
//  			// ignore it
//  		    }
//  		}
//  	    }
//  	    else {
//  		//Debug.message("runtimeCmd(" + args[0] + ") finished");
//  	    }
//  	} catch(Exception e) {
//  	    Debug.reportError("exception executing runtime command " + e);
//  	}
    }

    /**
     * Get the value of a static field in a class.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
	throws Exception
    {
	Debug.message("[getStaticValue]");
//  	RemoteClass cl = getDebugger().findClass(className);
//  	RemoteObject obj = (RemoteObject)cl.getFieldValue(fieldName);
//  	DebuggerObject ret = JdiObject.getDebuggerObject(obj);
		
//  	return ret;
	return null;
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
//  	try {
//  	    loadClass(loader, className);
//  	    RemoteClass cl = getDebugger().findClass(className);
//  	    if(cl == null)
//  		return "Class not found";

//  	    String result;
//  	    if(set)
//  		result = cl.setBreakpointLine(line);
//  	    else
//  		result = cl.clearBreakpointLine(line);
//  	    return result;
//  	}
//  	catch (Exception e) {
//  	    Debug.message("could not set breakpoint: " + e);
//  	    return "Internal error while setting breakpoint";
//  	}

	return null;
    }

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
//  	RemoteThreadGroup[] tgroups = getDebugger().listThreadGroups(null);
		
//  	Vector allThreads = new Vector();
//  	for(int i = 0; i < tgroups.length; i++) {
//  	    if(tgroups[i].getName().equals(MAIN_THREADGROUP)) {
//  		RemoteThread[] threads = tgroups[i].listThreads(false);
//  		for(int j = 0; j < threads.length; j++) {
//  		    allThreads.addElement(new JdiThread(threads[j]));
//  		    //Debug.message("thread: " + threads[j].getName() +
//  		    //		  " group: " + tgroups[i].getName());
//  		}
//  	    }
//  	}

//  	int len = allThreads.size();
//  	DebuggerThread[] ret = new DebuggerThread[allThreads.size()];
//  	// reverse order to make display nicer (newer threads first)
//  	for(int i = 0; i < len; i++)
//  	    ret[i] = (DebuggerThread)allThreads.elementAt(len - i - 1);
//  	return ret;
	return null;
    }
	
    /**
     * A thread has been stopped by the user. Make sure that the source 
     * is shown.
     */
    public void threadStopped(DebuggerThread thread)
    {
//  	Package pkg = (Package)waitqueue.get(
//  				     ((JdiThread)thread).getRemoteThread());

//  	if(pkg == null)
//  	    Debug.reportError("cannot find class for stopped thread");
//  	else {
//  	    pkg.hitBreakpoint(thread.getClassSourceName(0),
//  			      thread.getLineNumber(0), 
//  			      thread.getName(), true);
//  	}
    }

    /**
     * A thread has been started again by the user. Make sure that it 
     * is indicated in the interface.
     */
    public void threadContinued(DebuggerThread thread)
    {
//  	Package pkg = (Package)waitqueue.get(
//  				     ((JdiThread)thread).getRemoteThread());
//  	if(pkg != null)
//  	    pkg.getFrame().continueExecution();
    }

    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread.
     */
    public void showSource(DebuggerThread thread, int frameNo)
    {
//  	Package pkg = (Package)waitqueue.get(
//  				     ((JdiThread)thread).getRemoteThread());

//  	if(pkg != null) {
//  	    pkg.hitBreakpoint(thread.getClassSourceName(frameNo),
//  			      thread.getLineNumber(frameNo), 
//  			      thread.getName(), false);
//  	}
    }

    // --- DebuggerCallback interface --- 
	
    /**
     * Print text to the debugger's console window.
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public void printToConsole(String text) throws Exception
    {
//  	System.out.print(text);
    }

    /**
     * A breakpoint has been hit in the specified thread. Find the user
     * thread that started the execution and let it continue. (The user
     * thread is waiting in the waitqueue.)
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
//      public synchronized void breakpointEvent(RemoteThread rt) throws Exception
//      {
//  	//Debug.message("JdiDebugger: breakpointEvent " + rt);

//  	Package pkg = (Package)waitqueue.get(rt);

//  	if(pkg == null)
//  	    Debug.reportError("cannot find thread for breakpoint");
//  	else {
//  	    JdiThread thread = new JdiThread(rt);
//  	    pkg.hitBreakpoint(thread.getClassSourceName(0), 
//  			      thread.getLineNumber(0), 
//  			      thread.getName(), true);
//  	}
//      }

//      /**
//       * An exception has occurred.
//       *
//       * @exception java.lang.Exception if a general exception occurs.
//       */
//      public synchronized void exceptionEvent(RemoteThread rt, String errorText)
//  	throws Exception
//      {
//  	//Debug.message("JdiDebugger: exception event ");

//  	// System.exit() gets caught by the security manager and translated
//  	// into an exception called "BlueJ-Exit". First, we check for this.
//  	// Otherwise it's a real exception.

//  	int pos = errorText.indexOf("BlueJ-Exit:");
//  	if(pos != -1) {
//  	    exitStatus = FORCED_EXIT;
//  	    // get exit code
//  	    int endpos = errorText.indexOf(":", pos+11);  // find second ":"
//  	    exceptionMsg = errorText.substring(pos+11, endpos);
//  	}
//  	else {
//  	    exitStatus = EXCEPTION;
//  	    exceptionMsg = errorText;
//  	}

//  	if(waitqueue.containsKey(rt)) {	// someone is waiting on this...
//  	    waitqueue.remove(rt);
//  	    notifyAll();
//  	}
//  	else
//  	    rt.stop();
//      }

//      /**
//       * A thread has died.
//       *
//       * @exception java.lang.Exception if a general exception occurs.
//       */
//      public synchronized void threadDeathEvent(RemoteThread rt) 
//  	throws Exception
//      {
//  	//Debug.message("JdiDebugger: threadDeathEvent " + rt);

//  	if(waitqueue.containsKey(rt)) {	// someone is waiting on this...
//  	    waitqueue.remove(rt);
//  	    notifyAll();
//  	}
//  	else
//  	    // Don't need the thread any more - it can go away
//  	    rt.stop();
//      }

    /**
     * The client interpreter has exited, either by returning from its
     *  main thread, or by calling System.exit().
     *
     * @exception java.lang.Exception if a general exception occurs.
     */
    public void quitEvent() throws Exception
    {
//  	Debug.message("JdiDebugger: quitEvent");
    }
}
