package bluej.debugger.jdi;

import bluej.debugger.*;

import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.runtime.ExecServer;
import bluej.terminal.Terminal;

import bluej.pkgmgr.Package;

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
import java.util.*;

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
    static final String SERVER_SIGNAL_METHOD_NAME = "signalStartTask";


    //static final String MAIN_THREADGROUP = "bluej.runtime.BlueJRuntime.main";

    private Process process = null;
    private VMEventHandler eventHandler = null;
    private ObjectReference execServer = null;
    private Method signalMethod = null;
    private ThreadReference serverThread = null;
    volatile private boolean initialised = false;

    private int exitStatus;
    private ExceptionDescription lastException;


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
            displayRemoteOutput(process.getErrorStream());
            displayRemoteOutput(process.getInputStream());
            getRemoteInput(process.getOutputStream());
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

	// now wait until the machine really has sterted up. We will know that it
	// has when the first breakpoint is hit (see breakpointEvent).
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
	ReferenceType serverType = null;

	// ** find the mirror of the server object **

	List list = machine.classesByName(SERVER_CLASSNAME);
	if(list.size() > 1)
	    Debug.message("Warning: more than one VM server object found");
	if(list.size() < 1)
	    Debug.reportError("Cannot find VM server object");
	else {
	    serverType = (ReferenceType)list.get(0);
	}

	// ** find the suspend method **

	list = serverType.methodsByName(SERVER_SUSPEND_METHOD_NAME);
	Method suspendMethod = null;
	if(list.size() != 1)
	    Debug.reportError("Problem getting suspend method");
	else
	    suspendMethod = (Method)list.get(0);
  
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

	list = erm.classPrepareRequests();
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
	startServer(ExecServer.CREATE_LOADER, scopeId, classpath, "");
	return new JdiClassLoader(scopeId);
    }
	

    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
	startServer(ExecServer.REMOVE_LOADER, loader.getId(), "", "");
    }


    /**
     * "Start" a class (i.e. invoke its main method)
     */
    public void startClass(DebuggerClassLoader loader, String classname, 
			   Package pkg)
    {
	startServer(ExecServer.LOAD_CLASS, loader.getId(), classname, pkg);

	// ** find the mirror of the server object **

	List list = machine.classesByName(classname);
	if(list.size() != 1)
	    Debug.reportError("error starting class " + classname);

	ReferenceType shellType = (ReferenceType)list.get(0);
	Field shellObjectField = shellType.fieldByName("shellObject");
	ObjectReference shellObject = 
	    (ObjectReference)shellType.getValue(shellObjectField);

	// ** find the run method **

	list = shellType.methodsByName("run");
	if(list.size() != 1)
	    Debug.reportError("Problem getting 'run' method");

	Method runMethod = (Method)list.get(0);

	if(runMethod == null) {
	    Debug.reportError("Could not find shell run method");
	    return;
	}

	// ** call shellObject.run() **

	List arguments = new ArrayList();
  	try {
	    exitStatus = NORMAL_EXIT;
  	    Value returnVal = shellObject.invokeMethod(serverThread, 
						      runMethod, 
						      arguments, 0);
	    // returnVal is type void
	}
  	catch(InvocationException e) {
	    // exception thrown in remote machine
	}
  	catch(Exception e) {
	    // remote invocation failed
	    Debug.message("starting shell class failed: " + e);
	    exitStatus = EXCEPTION;
	    lastException = new ExceptionDescription("Internal BlueJ error!",
					     "Cannot execute remote command",
					     null, 0);
  	}
    }


    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String instanceName, 
				 String fieldName, String newObjectName)
    {
	Debug.message("[addObjectToScope] - NYI");
	//  	String[] args = { BlueJRuntime.ADD_OBJECT, 
	//  			  scopeId, instanceName, fieldName, newObjectName };
	//  	runtimeCmd(args, "");
    }
	
    /**
     * Remove an object from a package scope (when removed from object bench)
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
	Debug.message("[removeObjectFromScope] - NYI");
	//  	String[] args = { BlueJRuntime.REMOVE_OBJECT, 
	//  			  scopeId, instanceName };
	//  	runtimeCmd(args, "");
    }


    /**
     * Start the server process on the remote machine to perform a task.
     * Arguments to the server are a task ID specifying what we want done,
     * and two optional string parameters. The string parameters must not
     * be null. The task ID is one of the constants defined in
     * runtime.ExecServer.
     *
     * This is done synchronously: we return once the remote execution
     * has completed.
     */
    private void startServer(int task, String arg1, String arg2, Object pkg)
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

  	try {
  	    Value returnVal = execServer.invokeMethod(serverThread, 
						      signalMethod, 
						      arguments, 0);
	    // returnVal currently unused (void)
	}
  	catch(Exception e) {
	    Debug.message("sending command to remote VM failed: " + e);
  	}
    }


    /**
     * Find the components on the remote VM that we need to talk to it:
     * the execServer object, the signalMethod, and the serverThread.
     * These three variables (mirrors to the remote entities) are set up here.
     * This needs to be done only once.
     */
    private boolean setupServerConnection(VirtualMachine vm)
    {
	ReferenceType serverType = null;

	// try to get the mirror of the server object

	List list = vm.classesByName(SERVER_CLASSNAME);
	if(list.size() > 1)
	    Debug.message("Warning: more than one VM server object found");
	if(list.size() < 1)
	    Debug.reportError("Cannot find VM server object");
	else {
	    serverType = (ReferenceType)list.get(0);
	    Field serverField = serverType.fieldByName(SERVER_FIELD_NAME);
	    execServer = (ObjectReference)serverType.getValue(serverField);
	    if(execServer == null) {
		sleep(3000);
		execServer = (ObjectReference)serverType.getValue(serverField);
	    }
	}

	if(execServer == null) {
	    Debug.reportError("Failed to load VM server object");
	    Debug.reportError("Fatal: User code execution will not work");
	    return false;
	}

	// okay, we have the server object; now get the signal method

	list = serverType.methodsByName(SERVER_SIGNAL_METHOD_NAME);
	if(list.size() != 1)
	    Debug.reportError("Problem getting server signal method");
	else
	    signalMethod = (Method)list.get(0);
  
	if(signalMethod == null) {
	    Debug.reportError("invalid VM server object");
	    Debug.reportError("Fatal: User code execution will not work");
	    return false;
	}

	list = vm.allThreads();
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
	DebuggerObject object;

	//Debug.message("[getStaticValue] " + className);

	List list = getVM().classesByName(className);
	if(list.size() > 1)
	    Debug.message("Warning: more than one class found");
	if(list.size() < 1) {
	    Debug.reportError("Cannot find class for result value");
	    object = null;
	}
	else {
	    ReferenceType classMirror = (ReferenceType)list.get(0);
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
//  	List list = remoteException.referenceType().methodsByName("getMessage");
//  	if(list.size() != 1)
//  	    Debug.reportError("Problem getting exception message");

//  	Method getMessageMethod = (Method)list.get(0);
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
	else
	    Debug.message("[JdiDebugger] breakpointEvent");


//  	Package pkg = (Package)waitqueue.get(rt);

//  	if(pkg == null)
//  	    Debug.reportError("cannot find thread for breakpoint");
//  	else {
//  	    JdiThread thread = new JdiThread(rt);
//  	    pkg.hitBreakpoint(thread.getClassSourceName(0), 
//  			      thread.getLineNumber(0), 
//  			      thread.getName(), true);
//  	}
    }


    //====


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
	
    private void setEventRequests(VirtualMachine vm) {
        EventRequestManager erm = vm.eventRequestManager();
        // want all uncaught exceptions
        ExceptionRequest excReq = erm.createExceptionRequest(null, 
                                                             false, true); 
        excReq.enable();
        erm.createClassPrepareRequest().enable();
    }


    /**	
     *	Create a thread that will retrieve any output from the remote
     *  machine and direct it to our terminal.
     */
    private void displayRemoteOutput(final InputStream stream) {
	Thread thr = new Thread("output reader") { 
	    public void run() {
                try {
                    dumpStream(stream, 
			 Terminal.getTerminal().getOutputStream());
                } catch (IOException ex) {
                    Debug.reportError("Cannot read output user VM.");
                }
	    }
	};
	thr.setPriority(Thread.MAX_PRIORITY-1);
	thr.start();
    }


    /**	
     *	Create a thread that will direct terminal input to the remote
     *  machine.
     */
    private void getRemoteInput(final OutputStream stream) {
	Thread thr = new Thread("input reader") { 
	    public void run() {
                try {
                    dumpStream(
			 Terminal.getTerminal().getInputStream(),
			 stream);
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

