package bluej.debugger.jdi;

import bluej.debugger.*;

import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.runtime.ExecServer;
import bluej.terminal.Terminal;

import java.util.Hashtable;
//import java.util.Vector;
import java.util.Map;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;

import java.io.*;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

/**
 ** A class implementing the debugger primitives needed by BlueJ
 ** Implemented in a remote VM (via JDI interface)
 **
 ** @author Michael Kolling
 **/

public class JdiDebugger extends Debugger
{
    // the class name of the execution server class running on the remote VM
    static final String SERVER_CLASSNAME = "bluej.runtime.ExecServer";

    // the field name of the static field within that class that hold the
    // server object
    static final String SERVER_FIELD_NAME = "server";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "suspendExecution";

    // the name of the method called to signal the ExecServer to start a new task
    static final String SERVER_PERFORM_METHOD_NAME = "performTask";

    // name of the threadgroup that contains user threads
    static final String MAIN_THREADGROUP = "main";

    private Process process = null;
    private VMEventHandler eventHandler = null;
    private ObjectReference execServer = null;
    private Method performTaskMethod = null;
    private ThreadReference serverThread = null;
    volatile private boolean initialised = false;

    private int exitStatus;
    private ExceptionDescription lastException;
    private Hashtable activeThreads;


    public JdiDebugger()
    {
	super();
	activeThreads = new Hashtable();
    }

    private VirtualMachine machine = null;
    private synchronized VirtualMachine getVM()
    {
	while(!initialised)
	    try {
		wait();
	    } catch(InterruptedException e) {
	    }
	    
	return machine;
    }

    /**
     * Start debugging. I.e. create the second virtual machine and start
     *  the execution server (class ExecServer) on that machine.
     */
    public synchronized void startDebugger()
    {
  	if(initialised)
  	    return;

	BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM, null);

        VirtualMachineManager mgr = Bootstrap.virtualMachineManager();
        LaunchingConnector connector;

        connector = mgr.defaultConnector();
	//Debug.message("connector: " + connector.name());
	//Debug.message("transport: " + connector.transport().name());

        Map arguments = connector.defaultArguments();

	// debug code to print out all existing arguments and their
	// description
	//  	Collection c = arguments.values();
	//  	Iterator i = c.iterator();
	//  	while(i.hasNext()) {
	//  	    Connector.Argument a = (Connector.Argument)i.next();
	//  	    Debug.message("arg name: " + a.name());
	//  	    Debug.message("  descr: " + a.description());
	//  	    Debug.message("  value: " + a.value());
	//  	}

	// "main" is the command line: main class and arguments
        Connector.Argument mainArg = 
	    (Connector.Argument)arguments.get("main");

	// "startMode" determiones whether remote VM starts immediately
	// or is initially halted. possible values: "interrupted", "running"
        Connector.Argument modeArg = 
	    (Connector.Argument)arguments.get("startMode");

        if ((modeArg == null) || (mainArg == null)) {
	    Debug.reportError("Cannot start virtual machine.");
	    Debug.reportError("(Incompatible launch connector)");
	    return;
        }

        mainArg.setValue(SERVER_CLASSNAME);
	//modeArg.setValue("running");  // use this to start the machine running.
	// default is to suspend on first 
	// instruction.

        try {
            machine = connector.launch(arguments);
            process = machine.process();
            redirectIOStream(process.getErrorStream(), System.out);
            redirectIOStream(process.getInputStream(),
			     Terminal.getTerminal().getOutputStream());
            redirectIOStream(Terminal.getTerminal().getInputStream(),
			     process.getOutputStream());
			   
	} catch (VMStartException vmse) {
            Debug.reportError("Target VM did not initialise.");
            Debug.reportError(vmse.getMessage() + "\n");
            dumpFailedLaunchInfo(vmse.process());
        } catch (Exception e) {
            Debug.reportError("Unable to launch target VM.");
            e.printStackTrace();
        }

        setEventRequests(machine);
	eventHandler = new VMEventHandler(this, machine);

	// now wait until the machine really has sterted up. We will know that
	// it has when the first breakpoint is hit (see breakpointEvent).
	try {
	    wait();
	} catch(InterruptedException e) {}
	initialised = true;
	notifyAll();
	BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
    }
	
    /**
     * Finish debugging
     */
    protected synchronized void endDebugger()
    {
	Debug.message("[endDebugger]");
        try {
            if (machine != null) {
                machine.dispose();
                machine = null;
            }
        } finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }


    /**
     * This method is called by the VMEventHandler when the execution server
     * class (ExecServer) has been loaded into the VM. We use this to set
     * a breakpoint in the server class. This is really still part of the
     * initialisation process. This breakpoint is used to stop the server 
     * process to make it wait for our task signals. (We later use the 
     * suspended process to perform our task requests.)
     */
    void serverClassPrepared()
    {
	ReferenceType serverType = findClassByName(machine, SERVER_CLASSNAME);
	Method suspendMethod = findMethodByName(serverType, 
						SERVER_SUSPEND_METHOD_NAME);
	if(suspendMethod == null) {
	    Debug.reportError("invalid VM server object");
	    Debug.reportError("Fatal: User code execution will not work");
	    return;
	}

	// ** set a breakpoint in the suspend method **

	Location loc = suspendMethod.location();
	EventRequestManager erm = machine.eventRequestManager();
	BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
	bpreq.enable();

	// ** remove the "class prepare" event request (not needed anymore) **

	List list = erm.classPrepareRequests();
	if(list.size() != 1)
	    Debug.reportError("oops - found more than one prepare request!");
	ClassPrepareRequest cpreq = (ClassPrepareRequest)list.get(0);
	erm.deleteEventRequest(cpreq);
    }


    /**
     * Create a class loader. This really creates two distinct class
     *  loader objects: a DebuggerClassLoader (more specifically, in this
     *  case, a JdiClassLoader) which is handed back to the caller and a
     *  BlueJClassLoader on the remote VM.
     *  The DebuggerClassLoader serves as a handle to the BlueJClassLoader. 
     *  The connection is made by an ID (a String), stored in the 
     *  DebuggerClassLoader, with which the BlueJClassLoader can be looked up.
     */
    public DebuggerClassLoader createClassLoader(String scopeId, 
						 String classpath)
    {
	startServer(ExecServer.CREATE_LOADER, scopeId, classpath, "", "");
	return new JdiClassLoader(scopeId);
    }
	

    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
	startServer(ExecServer.REMOVE_LOADER, loader.getId(), "", "", "");
    }


    /**
     * "Start" a class (i.e. invoke its main method)
     *
     * @param loader		the class loader to use
     * @param classname		the class to start
     * @param eventParam	when a BlueJEvent is generated for a
     *				breakpoint, this parameter is passed as the
     *				event parameter
     */
    public void startClass(DebuggerClassLoader loader, String classname, 
			   Object eventParam)
    {
	loadClass(loader, classname);
	ClassType shellClass = findClassByName(machine, classname);

	Method runMethod = findMethodByName(shellClass, "run");
	if(runMethod == null) {
	    Debug.reportError("Could not find shell run method");
	    return;
	}

	// ** call Shell.run() **

	List arguments = new ArrayList();	// empty argument list
  	try {
	    exitStatus = NORMAL_EXIT;
	    // the following is in preparation for running several threads
	    // concurrently: we remember which thread is used for executing
	    // in which package (although, currently, there is always only
	    // one thread at a time, the serverThread).
	    activeThreads.put(serverThread, eventParam);
  	    Value returnVal = shellClass.invokeMethod(serverThread, 
						      runMethod, 
						      arguments, 0);
	    // returnVal is type void
	    // 'invokeMethod' is synchronous - when we get here it has
	    // finished
	}
  	catch(InvocationException e) {
	    // exception thrown in remote machine - ignored here. The
	    // exception is handled through the exceptionEvent method
	}
  	catch(Exception e) {
	    // remote invocation failed
	    Debug.message("starting shell class failed: " + e);
	    exitStatus = EXCEPTION;
	    lastException = new ExceptionDescription(
					"Internal BlueJ error!",
					"Cannot execute remote command",
					null, 0);
  	}
	activeThreads.remove(serverThread);
    }


    /**
     * Load a class in the remote machine.
     */
    private void loadClass(DebuggerClassLoader loader, String classname)
    {
	startServer(ExecServer.LOAD_CLASS, loader.getId(), classname, "", "");
    }


    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName)
    {
	//Debug.message("[addObjectToScope]: " + newObjectName);
	startServer(ExecServer.ADD_OBJECT, scopeId, instanceName, 
		    fieldName, newObjectName);
    }
	
    /**
     * Remove an object from a package scope (when removed from object bench).
     * This has to be done tolerantly: If the named instance is not in the
     * scope, we just quetly return. 
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
	//Debug.message("[removeObjectFromScope]: " + instanceName);
	startServer(ExecServer.REMOVE_OBJECT, scopeId, instanceName, "", "");
    }


    /**
     * Start the server process on the remote machine to perform a task.
     * Arguments to the server are a task ID specifying what we want done,
     * and four optional string parameters. The string parameters must not
     * be null. The task ID is one of the constants defined in
     * runtime.ExecServer.
     *
     * This is done synchronously: we return once the remote execution
     * has completed.
     */
    private void startServer(int task, String arg1, String arg2, 
			     String arg3, String arg4)
    {
	VirtualMachine vm = getVM();

	if(execServer == null) {
	    if(! setupServerConnection(vm))
		return;
	}

	List arguments = new ArrayList(3);
	arguments.add(vm.mirrorOf(task));
	arguments.add(vm.mirrorOf(arg1));
	arguments.add(vm.mirrorOf(arg2));
	arguments.add(vm.mirrorOf(arg3));
	arguments.add(vm.mirrorOf(arg4));

  	try {
  	    Value returnVal = execServer.invokeMethod(serverThread, 
						      performTaskMethod, 
						      arguments, 0);
	    // returnVal currently unused (void)
	}
  	catch(Exception e) {
	    Debug.message("sending command to remote VM failed: " + e);
  	}
    }


    /**
     * Find the components on the remote VM that we need to talk to it:
     * the execServer object, the performTaskMethod, and the serverThread.
     * These three variables (mirrors to the remote entities) are set up here.
     * This needs to be done only once.
     */
    private boolean setupServerConnection(VirtualMachine vm)
    {
	ReferenceType serverType = findClassByName(vm, SERVER_CLASSNAME);

	Field serverField = serverType.fieldByName(SERVER_FIELD_NAME);
	execServer = (ObjectReference)serverType.getValue(serverField);

	if(execServer == null) {
	    sleep(3000);
	    execServer = (ObjectReference)serverType.getValue(serverField);
	}
	if(execServer == null) {
	    Debug.reportError("Failed to load VM server object");
	    Debug.reportError("Fatal: User code execution will not work");
	    return false;
	}

	// okay, we have the server object; now get the perform method

	performTaskMethod = findMethodByName(serverType, 
					     SERVER_PERFORM_METHOD_NAME);
	if(performTaskMethod == null) {
	    Debug.reportError("invalid VM server object");
	    Debug.reportError("Fatal: User code execution will not work");
	    return false;
	}

	List list = vm.allThreads();
	for (int i=0 ; i<list.size() ; i++) {
	    ThreadReference threadRef = (ThreadReference)list.get(i);
	    if("main".equals(threadRef.name()))
		serverThread = threadRef;
	}

	if(serverThread == null) {
	    Debug.reportError("Cannot find server thread on remote VM");
	    Debug.reportError("Fatal: User code execution will not work");
	    return false;
	}

	//Debug.message(" connection to remote VM established");
	return true;
    }


    /**
     * Get the value of a static field in a class.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
	throws Exception
    {
	DebuggerObject object = null;

	ReferenceType classMirror = findClassByName(getVM(), className);

	//Debug.message("[getStaticValue] " + className);

	if(classMirror == null) {
	    Debug.reportError("Cannot find class for result value");
	    object = null;
	}
	else {
	    Field resultField = classMirror.fieldByName(fieldName);
	    ObjectReference obj = 
		(ObjectReference)classMirror.getValue(resultField);
	    object = JdiObject.getDebuggerObject(obj);
	}

	return object;
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
    public ExceptionDescription getException()
    {
	return lastException;
    }


    /**
     *  An exception was thrown in the remote machine.
     */
    public void exceptionEvent(ExceptionEvent exc)
    {
	//Debug.message("[exceptionEvent] ");
	String excClass = exc.exception().type().name();
	ObjectReference remoteException = exc.exception();

	// attention: the following depends on the (undocumented) fact that 
	// the internal exception message field is named "detailMessage".
  	Field msgField = 
  	    remoteException.referenceType().fieldByName("detailMessage");
  	StringReference val = 
  	    (StringReference)remoteException.getValue(msgField);

	//better: get message via method call
	//  	Method getMessageMethod = findMethodByName(
	//  					   remoteException.referenceType(),
	//  					   "getMessage");
	//  	StringReference val = null;
	//    	try {
	//  	    val = (StringReference)execServer.invokeMethod(serverThread, 
	//  						getMessageMethod, 
	//  						null, 0);
	//  	} catch(Exception e) {
	//  	    Debug.reportError("Problem getting exception message: " + e);
	//  	}

	String exceptionText = 
	    (val == null ? null : val.value());

	if(excClass.equals("bluej.runtime.ExitException")) {
	    // this was a "System.exit()", not a real exception!
	    exitStatus = FORCED_EXIT;
	    lastException = new ExceptionDescription(exceptionText);
	}
	else {		// real exception

	    Location loc = exc.location();
	    String fileName;
	    try {
		fileName = loc.sourceName();
	    } catch(Exception e) {
		fileName = null;
	    }
	    int lineNumber = loc.lineNumber();

	    exitStatus = EXCEPTION;
	    lastException = new ExceptionDescription(excClass, exceptionText,
						     fileName, lineNumber);
	}
    }

    /**
     * A breakpoint has been hit in the specified thread. Find the user
     * thread that started the execution and let it continue. (The user
     * thread is waiting in the waitqueue.)
     */
    public void breakpointEvent(BreakpointEvent event)
    {
	// if we hit a breakpoint before the VM is initialised, then it is our
	// own breakpoint that we have been waiting for at startup
	if(!initialised) {
	    synchronized(this) {
		notifyAll();
	    }
	}
	else {
	    Debug.message("[JdiDebugger] breakpointEvent");

	    ThreadReference remoteThread = event.thread();
	    Object pkg = activeThreads.get(remoteThread);
	    if(pkg == null)
		Debug.reportError("cannot find breakpoint thread!");
	    else {
		JdiThread thread = new JdiThread(remoteThread, pkg);
		BlueJEvent.raiseEvent(BlueJEvent.BREAKPOINT, thread);
	    }
	}
    }


    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     *
     * @return  null if there was no problem, or an error string
     */
    public String toggleBreakpoint(String className, int line, boolean set,
				   DebuggerClassLoader loader)
    {
	//Debug.message("[toggleBreakpoint]: " + className);

	VirtualMachine vm = getVM();

	loadClass(loader, className);
  	ClassType remoteClass = findClassByName(vm, className);
  	if(remoteClass == null)
  	    return "Class not found";

	try {
	    Location loc = findLocationInLine(remoteClass, line);
	    if(loc == null)
		return "Cannot set breakpoint: no code in this line";

	    EventRequestManager erm = vm.eventRequestManager();
	    if(set) {
		BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
		bpreq.enable();
		return null;
	    }
	    else {	// clear breakpoint
		List list = erm.breakpointRequests();
		for (int i=0 ; i < list.size() ; i++) {
		    BreakpointRequest bp = (BreakpointRequest)list.get(i);
		    if(bp.location().equals(loc)) {
			erm.deleteEventRequest(bp);
			return null;
		    }
		}
		// bp not found
		return "Clear breakpoint: no breakpoint found in this line.";
	    }
	}
	catch(AbsentInformationException e) {
	    return "This class has been compiled without line number\n" +
		"information. You cannot set breakpoints.";
	}
	catch(InvalidLineNumberException e) {
	    return "Cannot set breakpoint: no code in this line";
	}
	catch(Exception e) {
	    Debug.reportError("breakpoint error: " + e);
	    return "There was an internal error while attempting to\n" +
		   "set this breakpoint";
	}
    }

    /**
     * List all the threads being debugged as a Vector containing elements
     * of type DebuggerThread. Filter out threads that belong
     * to system, returning only user threads.
     */
    public Vector listThreads()
    {
	List threads = getVM().allThreads();
	int len = threads.size();

	Vector threadVec = new Vector(len);

	// reverse order to make display nicer (newer threads first)
	for(int i = 0; i < len; i++) {
	    ThreadReference thread = (ThreadReference)threads.get(len-i-1);
	    if(thread.threadGroup().name().equals(MAIN_THREADGROUP)) {

		String name = thread.name();
		if(! name.startsWith("AWT-") &&	       // known system threads
		   ! name.startsWith("SunToolkit.") && 
		   ! name.equals("TimerQueue"))
		    threadVec.add(i, new JdiThread(thread));
	    }
	}
	return threadVec;
    }
	
    //====


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


    // -- support methods --

    /** 
     *  Find the mirror of a class in the remote VM. The class is expected
     *  to exist. We expect only one single class to exist with this name
     *  and report an error if more than one is found.
     */
    private ClassType findClassByName(VirtualMachine vm, String classname) 
    {
	List list = vm.classesByName(classname);
	if(list.size() != 1) {
	    Debug.reportError("error finding class " + classname);
	    Debug.reportError("number of classes found: " + list.size());
	    return null;
	}
	return (ClassType)list.get(0);
    }

    /** 
     *  Find the mirror of a method in the remote VM. The method is expected
     *  to exist. We expect only one single method to exist with this name
     *  and report an error if more than one is found.
     */
    private Method findMethodByName(ReferenceType type, String methodName) 
    {
	List list = type.methodsByName(methodName);
	if(list.size() != 1) {
	    Debug.reportError("Problem getting method: " + methodName);
	    return null;
	}
	return (Method)list.get(0);
    }

    /** 
     *  Find the first location in a given line in a class.
     */
    private Location findLocationInLine(ClassType cl, int line) 
      throws Exception
    {
	List list = cl.locationsOfLine(line);
	if(list.size() == 0)
	    return null;
	else
	    return (Location)list.get(0);
    }

    private void setEventRequests(VirtualMachine vm) 
    {
        EventRequestManager erm = vm.eventRequestManager();
        // want all uncaught exceptions
        ExceptionRequest excReq = erm.createExceptionRequest(null, 
                                                             false, true); 
        excReq.enable();
        erm.createClassPrepareRequest().enable();
    }


    /**	
     *	Create a thread that will retrieve any output from the remote
     *  machine and direct it to our terminal (or vice versa).
     */
    private void redirectIOStream(final InputStream inStream,
				  final OutputStream outStream) 
    {
	Thread thr = new Thread("I/O reader") { 
	    public void run() {
                try {
                    dumpStream(inStream, outStream);
                } catch (IOException ex) {
                    Debug.reportError("Cannot read output user VM.");
                }
	    }
	};
	thr.setPriority(Thread.MAX_PRIORITY-1);
	thr.start();
    }

    //      private void dumpStream(InputStream inStream, OutputStream outStream) 
    //  	throws IOException 
    //      {
    //          int ch;
    //          while ((ch = inStream.read()) != -1) {
    //              outStream.write(ch);
    //          }
    //      }

    private void dumpStream(InputStream inStream, OutputStream outStream) 
	throws IOException 
    {
        BufferedReader in = 
            new BufferedReader(new InputStreamReader(inStream));
	OutputStreamWriter out =
	    new OutputStreamWriter(outStream);
        String line;
        while ((line = in.readLine()) != null) {
            out.write(line);
            out.write("\n");
	    out.flush();
        }
    }

    private void dumpFailedLaunchInfo(Process process) {
        try {
            dumpStream(process.getErrorStream(), System.out);
            dumpStream(process.getInputStream(), System.out);
        } catch (IOException e) {
            Debug.message("Unable to display process output: " +
			  e.getMessage());
        }
    }

    private void sleep(int millisec)
    {
	synchronized(this) {
	    try {
		wait(millisec);
	    } catch(InterruptedException e) {}
	}
    }
}

