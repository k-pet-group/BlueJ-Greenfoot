package bluej.debugger.jdi;

import java.io.*;
import java.util.*;

import bluej.Config;
import bluej.classmgr.ClassMgr;
import bluej.debugger.*;
import bluej.prefmgr.PrefMgr;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

/**
 * A class implementing the execution and debugging primitives needed by
 * BlueJ.
 *
 * Execution and debugging is implemented here on a second ("remote")
 * virtual machine, which gets started from here via the JDI interface.
 *
 * @author  Michael Kolling
 * @version $Id: VMReference.java 2449 2004-01-09 02:29:47Z ajp $
 *
 * The startup process is as follows:
 *
 * TODO: fix this comment!!!!!!
 * 
 *  Debugger		VMEventHandler Thread		Remote VM
 *  ----------------------------------------------------------------------
 *  startDebugger:
 *    start VM --------------------------------------> start
 *    start event handler ---> start                     .
 *    wait                       .
 *      .                        .                       .
 *      .                        .                       .
 *      .                        .                     server class loaded
 *      .                      prepared-event < ---------.
 *  serverClassPrepared() <------.
 *    set break in remote VM
 *    continue remote VM
 *      .  ------------------------------------------> continue
 *      .                        .                       .
 *      .                        .                     hit breakpoint
 *      .                      break-event < ------------.
 *    continue < -----------------.
 *      .
 *      .
 *
 * We can now execute commands on the remote VM by invoking methods
 * using the server thread (which is suspended at the breakpoint).
 * This is done in the "startServer()" method.
 */
class VMReference
{
    // the class name of the execution server class running on the remote VM
    static final String SERVER_CLASSNAME = "bluej.runtime.ExecServer";

    // the field name of the static field within that class
    // the name of the method used to signal a System.exit()
// pending for removal when exit scheme is tested.
//    static final String SERVER_EXIT_MARKER_METHOD_NAME = "exitMarker";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_STARTED_METHOD_NAME = "vmStarted";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "vmSuspend";

    // ==== instance data ====

	// we have a tight coupling between us and the JdiDebugger
	// that creates us
	private JdiDebugger owner = null;
	
    // The remote virtual machine and process we are referring to
    private VirtualMachine machine = null;
    private Process remoteVMprocess = null;

    // The handler for virtual machine events    
    private VMEventHandler eventHandler = null;

    // the class reference to ExecServer
    private ClassType serverClass = null;

    // the thread running inside the ExecServer
    private ThreadReference serverThread = null;

	// the worker thread running inside the ExecServer
	private ThreadReference workerThread = null;
	
	// a record of the threads we start up for
	// redirecting ExecServer streams
	private IOHandlerThread inputStreamRedirector = null;
	private IOHandlerThread outputStreamRedirector = null;
	private IOHandlerThread errorStreamRedirector = null;

    // the current class loader in the ExecServer
    private ClassLoaderReference currentLoader = null;

    // an exception used to interrupt the main thread
    // when simulating a System.exit()
// pending for removal when exit scheme is tested.
//    private ObjectReference exitException = null;

	// map of String names to ExecServer methods
	// used by JdiDebugger.invokeMethod
    private Map execServerMethods = null;

    private int exitStatus;
    private ExceptionDescription lastException;


    /**
     * Launch a remote debug VM using a TCP/IP socket.
     * 
     * @param initDir	the directory to have as a current directory in
     * 					the remote VM
     * @param mgr		the virtual machine manager
     * @return			an instance of a VirtualMachine or null if there
     * 					was an error
     */
    public VirtualMachine localhostSocketLaunch(File initDir, DebuggerTerminal term, VirtualMachineManager mgr)
    {
    	final int CONNECT_TRIES = 10;		// try to connect max of 10 times
    	final int CONNECT_WAIT = 500;		// wait half a sec between each connect
    	
    	int portNumber;
    	
        // launch the VM
        try {
            // get classpath for VM
            String allClassPath = ClassMgr.getClassMgr().getAllClassPath().toString();
            
            ArrayList paramList = new ArrayList(10);
            paramList.add(Config.getJDKExecutablePath("this.key.must.not.exist", "java"));
            paramList.add("-classpath");
            paramList.add(allClassPath);
            paramList.add("-Xdebug");
            if(!PrefMgr.getFlag(PrefMgr.OPTIMISE_VM))
                paramList.add("-Xint");
            if(Config.isMacOS()) {
                paramList.add("-Xdock:icon=" + Config.getBlueJIconPath() + "/vm.icns");
                paramList.add("-Xdock:name=BlueJ Virtual Machine");
            }
            paramList.add("-Xrunjdwp:transport=dt_socket,server=y");
            paramList.add(SERVER_CLASSNAME);
            
            String[] launchParams = (String[])paramList.toArray(new String[0]);
            Process vmProcess = Runtime.getRuntime().exec(launchParams, null, initDir);

			String listenMessage = new BufferedReader(new InputStreamReader(vmProcess.getInputStream())).readLine();
		
			portNumber = extractPortNumber(listenMessage);
				
			if (portNumber == -1) {
				Debug.message("Could not find port number to connect to debugger");
				Debug.message("Line received from debugger was: " + listenMessage);
				return null;
			}
			
			// redirect error stream from process to Terminal
			errorStreamRedirector =
			    redirectIOStream(new InputStreamReader(vmProcess.getErrorStream()),
								//new OutputStreamWriter(System.err),
								term.getErrorWriter(),
								false);

			// redirect output stream from process to Terminal
			outputStreamRedirector = 
			    redirectIOStream(new InputStreamReader(vmProcess.getInputStream()),
								//new OutputStreamWriter(System.err),
								term.getWriter(),
								false);

			// redirect Terminal input to process output stream
			inputStreamRedirector = 
			    redirectIOStream(term.getReader(),
								new OutputStreamWriter(vmProcess.getOutputStream()),
								false);
			remoteVMprocess = vmProcess;
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }

		// now try to connect to the running VM using a TCP/IP socket
		
        AttachingConnector connector = null;
        List connectors = mgr.attachingConnectors();

		// find a socket connector
        Iterator it = connectors.iterator();
        while (it.hasNext()) {
            AttachingConnector c = (AttachingConnector) it.next();

            if (c.transport().name().equals("dt_socket")) {
                connector = c;
                break;
            }
        }

		if (connector == null) {
			throw new IllegalStateException("no JPDA socket launch connector");
		}
		
        Map arguments = connector.defaultArguments();

        Connector.Argument hostnameArg = (Connector.Argument) arguments.get("hostname");
        Connector.Argument portArg = (Connector.Argument) arguments.get("port");

        if (hostnameArg == null || portArg == null) {
            throw new IllegalStateException("incompatible JPDA socket launch connector");
        }

        hostnameArg.setValue("127.0.0.1");
        portArg.setValue(Integer.toString(portNumber));

		// try to connect 10 times, waiting half a sec between each attempt
		for (int i=0; i<CONNECT_TRIES; i++) {
			try {
				VirtualMachine m = connector.attach(arguments);

				return m;
			}
			catch (java.net.ConnectException ce) {
				// on our last attempt, give a stack trace to give some
				// reason for the failure
				if (i == CONNECT_TRIES-1)
					ce.printStackTrace();
					
				try {
					synchronized (this)
					{ wait(CONNECT_WAIT); }
				}
				catch (InterruptedException ie) { }
			}
			catch (Exception e) {
				Debug.reportError("Unable to launch target VM.");
				e.printStackTrace();
				return null;
			}
		}

        return null;
    }

	/**
	 * Parse the message printed when starting up in server=y mode but when
	 * no port is specified. The message contains the port number that
	 * we should use to connect with.
	 * 
	 * @param   msg		the message printed by the debug vm
	 * @return			the port number to use or -1 in case of error
	 */
	private int extractPortNumber(String msg)
	{
		int colonIndex = msg.indexOf(":");
		int val = -1;
					
		try {
			if (colonIndex > -1) {
				val = Integer.parseInt(msg.substring(colonIndex+1).trim());
			}
		} catch (NumberFormatException nfe) {
			return -1;
		}
		
		return val;
	}

    /**
     * Create the second virtual machine and start
     * the execution server (class ExecServer) on that machine.
     */
    public VMReference(JdiDebugger owner, DebuggerTerminal term, File initialDirectory) throws JdiVmCreationException
    {
		this.owner = owner;
		
		// machine will be suspended at startup
        machine = localhostSocketLaunch(initialDirectory, term,
        								Bootstrap.virtualMachineManager());
        //machine = null; //uncomment to simulate inabilty to create debug VM
        if (machine == null) {
            throw new JdiVmCreationException();
        }
		// indicate the events we want to receive
		EventRequestManager erm = machine.eventRequestManager();
		erm.createExceptionRequest(null, false, true).enable();
		erm.createClassPrepareRequest().enable();
		erm.createThreadStartRequest().enable();
		erm.createThreadDeathRequest().enable();

		// start the VM event handler (will handle the VMStartEvent
		// which will set the machine running)
		eventHandler = new VMEventHandler(this, machine);
    }

    /**
     * Wait for all our virtual machine initialisation to occur.
     */
    public synchronized void waitForStartup()
    {
        // now wait until the machine really has started up.

        // first we will get a class prepared event (see serverClassPrepared)
        // second a breakpoint is hit (see breakEvent)
        // when that happens, this wait() is notify()'ed.
        try {
            wait();
        } catch (InterruptedException e) {}
        
        setupServerConnection(machine);

        EventRequestManager erm = machine.eventRequestManager();
        //StepRequest sr = erm.createStepRequest(serverThread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        //sr.putProperty("s","s");
        //sr.addClassExclusionFilter("java.*");
        //sr.addClassExclusionFilter("sun.*");
        //sr.addClassExclusionFilter("bluej.*");
        //sr.addClassExclusionFilter("junit.*");
        //sr.addClassExclusionFilter("com.sun.tools.*");
        //sr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        //sr.enable();
    }

    /**
     * Close down this virtual machine.
     */
    public synchronized void close()
    {
    	// close our IO redirectors
    	if (inputStreamRedirector != null) {
			inputStreamRedirector.close();
			inputStreamRedirector.interrupt();
    	}
    		
		if (errorStreamRedirector != null) {
			errorStreamRedirector.close();
			errorStreamRedirector.interrupt();
		}

		if (outputStreamRedirector != null) {
			outputStreamRedirector.close();
			outputStreamRedirector.interrupt();
		}
    		
        // can cause deadlock - why bother
        // lets just nuke it
        //machine.dispose();
        if (remoteVMprocess != null) {
            remoteVMprocess.destroy();
        }
        machine = null;
    }

    /**
     * This method is called by the VMEventHandler when the execution server
     * class (ExecServer) has been loaded into the VM. We use this to set
     * a breakpoint in the server class. This is really still part of the
     * initialisation process.
     */
    void serverClassPrepared()
    {
        // remove the "class prepare" event request (not needed anymore)

        EventRequestManager erm = machine.eventRequestManager();
        List list = erm.classPrepareRequests();
        if (list.size() != 1)
            Debug.reportError("oops - found more than one prepare request!");
        ClassPrepareRequest cpreq = (ClassPrepareRequest) list.get(0);
        erm.deleteEventRequest(cpreq);

		try {
			serverClass = (ClassType) findClassByName(SERVER_CLASSNAME, null);
		}
		catch (ClassNotFoundException cnfe) {
			throw new IllegalStateException(
				"can't find class " + SERVER_CLASSNAME + " in debug virtual machine");
		}

        // add the breakpoints (these may be cleared later on and so will
        // need to be readded)
        serverClassAddBreakpoints();
    }

    /**
     * This breakpoint is used to stop the server
     * process to make it wait for our task signals. (We later use the
     * suspended process to perform our task requests.)
     */
    private void serverClassAddBreakpoints()
    {
        EventRequestManager erm = machine.eventRequestManager();

        // set a breakpoint in the vm started method
        {
            Method startedMethod = findMethodByName(serverClass, SERVER_STARTED_METHOD_NAME);
            if (startedMethod == null) {
                throw new IllegalStateException(
                    "can't find method " + SERVER_CLASSNAME + "." +
                    							SERVER_STARTED_METHOD_NAME);
            }
            Location loc = startedMethod.location();
            BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
            bpreq.setSuspendPolicy(EventRequest.SUSPEND_ALL);
            // the presence of this property indicates to breakEvent that we are
            // a special type of breakpoint
            bpreq.putProperty(SERVER_STARTED_METHOD_NAME, "yes");
            bpreq.enable();
        }

        // set a breakpoint in the suspend method
        {
            Method suspendMethod = findMethodByName(serverClass, SERVER_SUSPEND_METHOD_NAME);
            if (suspendMethod == null) {
                throw new IllegalStateException(
                    "can't find method " + SERVER_CLASSNAME + "." +
                    						SERVER_SUSPEND_METHOD_NAME);
            }
            Location loc = suspendMethod.location();
            BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
            bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            // the presence of this property indicates to breakEvent that we are
            // a special type of breakpoint
            bpreq.putProperty(SERVER_SUSPEND_METHOD_NAME, "yes");
            // the presence of this property indicates that we should not
            // be restarted after receiving this event
            bpreq.putProperty(VMEventHandler.DONT_RESUME, "yes");
            bpreq.enable();
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
        if (serverClass == null) {
			Debug.reportError("server class not initialised!");
			return false;
        }

        // set up an exit exception object on the remote machine
// pending for removal when exit scheme is tested.
//        exitException = getStaticField(serverClass,
//        								 ExecServer.EXIT_EXCEPTION_NAME);

		// get our main server thread
		serverThread = (ThreadReference) getStaticField(serverClass,
														ExecServer.MAIN_THREAD_NAME);		

		// get our worker thread
		workerThread = (ThreadReference) getStaticField(serverClass,
														ExecServer.WORKER_THREAD_NAME);		

		if (serverThread == null || workerThread == null) {
			Debug.reportError("Cannot find fields on remote VM");
			return false;
		}

        // okay, we have the server objects; now get the methods we need
        execServerMethods = new HashMap();

        execServerMethods.put(ExecServer.NEW_LOADER,
            findMethodByName(serverClass, ExecServer.NEW_LOADER));
        execServerMethods.put(ExecServer.LOAD_CLASS,
        	findMethodByName(serverClass, ExecServer.LOAD_CLASS));
        execServerMethods.put(ExecServer.ADD_OBJECT,
            findMethodByName(serverClass, ExecServer.ADD_OBJECT));
        execServerMethods.put(ExecServer.GET_OBJECTS,
            findMethodByName(serverClass, ExecServer.GET_OBJECTS));
        execServerMethods.put(ExecServer.REMOVE_OBJECT,
            findMethodByName(serverClass, ExecServer.REMOVE_OBJECT));
        execServerMethods.put(ExecServer.RUN_TEST_SETUP,
            findMethodByName(serverClass, ExecServer.RUN_TEST_SETUP));
        execServerMethods.put(ExecServer.RUN_TEST_METHOD,
            findMethodByName(serverClass, ExecServer.RUN_TEST_METHOD));
        execServerMethods.put(ExecServer.SUPRESS_OUTPUT,
            findMethodByName(serverClass, ExecServer.SUPRESS_OUTPUT));
        execServerMethods.put(ExecServer.RESTORE_OUTPUT,
            findMethodByName(serverClass, ExecServer.RESTORE_OUTPUT));
        execServerMethods.put(ExecServer.DISPOSE_WINDOWS,
            findMethodByName(serverClass, ExecServer.DISPOSE_WINDOWS));
        execServerMethods.put(ExecServer.EXECUTE_CODE,
            findMethodByName(serverClass, ExecServer.EXECUTE_CODE));
        
        //Debug.message(" connection to remote VM established");
        return true;
    }

    // -- all methods below here are for after the VM has started up

    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED).
     * 
     * The other state NOTREADY cannot be returned by the method
     * because NOTREADY implies that this class has not been constructed.
     * NOTREADY may be returned in JdiDebugger.getStatus().
     */
    int getStatus()
    {
		if (serverThread.isSuspended()) {
			if (isAtMainBreakpoint(serverThread))
				return Debugger.IDLE;
			else
				return Debugger.SUSPENDED;			
		}
		else
			return Debugger.RUNNING;
    }

    /**
     * Instruct the remote machine to construct a new class loader
     * and return its reference.
     */
    ClassLoaderReference newClassLoader(String classPath)
    {
        ClassLoaderReference loader = null;
        Object args[] = { classPath };

        try {
            loader =
            (ClassLoaderReference) invokeExecServerWorker(ExecServer.NEW_LOADER, Arrays.asList(args));
        }
        catch (InvocationException ie) { }
        
        currentLoader = loader;

        return loader;
    }

    /**
     * Load a class in the remote machine and return its reference.
     * Note that this function never returns null.
     * 
     * @return a Reference to the class mirrored in the remote VM
     * @throws ClassNotFoundException
     */
    ReferenceType loadClass(String className)
    	throws ClassNotFoundException
    {
        Value v = null;
        Object args[] = { className };

        try {
            v = invokeExecServerWorker(ExecServer.LOAD_CLASS, Arrays.asList(args));
            // we get back a reference to an instance of a class of type "java.lang.Class"
            // but we know the class has been loaded in the remote VM.
            // now we find an actual reference to the class type.
            if (v.type().name().equals("java.lang.Class")) {
                ReferenceType rt = findClassByName(className, currentLoader);

                if (rt != null)
                    return rt;
            }
        }
        catch (InvocationException ie) { }
        

        throw new ClassNotFoundException(className);
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
    public void runShellClass(String className)
		throws ClassNotFoundException
    {
        ClassType shellClass = (ClassType) loadClass(className);

        Method runMethod = findMethodByName(shellClass, "run");
        if (runMethod == null) {
            Debug.reportError("Could not find shell run method");
            return;
        }

		// Debug.message("[VMRef] starting " + className);
        // ** call Shell.run() **
        try {
            exitStatus = Debugger.NORMAL_EXIT;

			owner.raiseStateChangeEvent(Debugger.IDLE, Debugger.RUNNING);

            Value v =
                invokeStaticRemoteMethod(serverThread, shellClass, runMethod,
                							Collections.EMPTY_LIST, false, false);

        } catch (VMDisconnectedException e) {
            exitStatus = Debugger.TERMINATED;
        } catch (Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            e.printStackTrace();
            exitStatus = Debugger.EXCEPTION;
            lastException =
                new ExceptionDescription(
                    "Internal BlueJ error: unexpected exception in remote VM\n" + e);
        }
		owner.raiseStateChangeEvent(Debugger.RUNNING, Debugger.IDLE);
    }

	/**
	 * Cause the exec server to execute a method.
	 * Note that all arguments to methods must be either String's
	 * or objects that are already mirrored onto the remote VM.
	 * 
	 * @param methodName    the name of the method in the class ExecServer
	 * @param args			the List of arguments to the method
	 * @return				the return value of the method call
	 * 						as an mirrored object on the VM
	 */
	Value invokeExecServerWorker(String methodName, List args) throws InvocationException
	{
		if (serverThread == null) {
			if (!setupServerConnection(machine))
				return null;
		}

		//Debug.message("[VMRefWorker] Invoking " + methodName);
		Method m = (Method) execServerMethods.get(methodName);

		if (m == null)
			throw new IllegalArgumentException("no ExecServer method called " + methodName);

		return invokeStaticRemoteMethod(workerThread, serverClass, m, args, true, true);
	}


    /**
     * Cause the exec server to execute a method.
     * Note that all arguments to methods must be either String's
     * or objects that are already mirrored onto the remote VM.
     * 
     * @param methodName    the name of the method in the class ExecServer
     * @param args			the List of arguments to the method
     * @return				the return value of the method call
     * 						as an mirrored object on the VM
     */
    Value invokeExecServer(String methodName, List args) throws InvocationException
    {
        if (serverThread == null) {
            if (!setupServerConnection(machine))
                return null;
        }

		//Debug.message("[VMRefMain] Invoking " + methodName);
        Method m = (Method) execServerMethods.get(methodName);

        if (m == null)
            throw new IllegalArgumentException("no ExecServer method called " + methodName);

        return invokeStaticRemoteMethod(serverThread, serverClass, m, args, true, false);
    }


    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, TERMINATED).
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
	 * The VM has reached its startup point.
	 */
	public void vmStartEvent(VMStartEvent vmse)
	{
	}

	/**
	 * The VM has been disconnected or ended.
	 */
	public void vmExitEvent()
	{
		owner.vmExit();
	}

	/**
	 * The VM has been disconnected or ended.
	 */
	public void vmDisconnectEvent()
	{
		owner.vmDisconnect();
	}
	
	/**
	 * A thread has started.
	 */
	public void threadStartEvent(ThreadStartEvent tse)
	{
		owner.threadStart(tse.thread());
	}

	/**
	 * A thread has died.
	 */
	public void threadDeathEvent(ThreadDeathEvent tde)
	{
		owner.threadDeath(tde.thread());
	}

    /**
     * An exception has occurred in a thread.
     * 
     * Analyse the exception and store it in 'lastException'.
     * It will be picked up later.
     */
    public void exceptionEvent(ExceptionEvent exc)
    {
        ObjectReference remoteException = exc.exception();

        // get the exception text
        // attention: the following depends on the (undocumented) fact that
        // the internal exception message field is named "detailMessage".
        Field msgField = remoteException.referenceType().fieldByName("detailMessage");
        StringReference msgVal = (StringReference) remoteException.getValue(msgField);

        String exceptionText = (msgVal == null ? null : msgVal.value());
		String excClass = exc.exception().type().name();

// PENDING: to be removed after exit scheme is tested
        // if (excClass.equals("bluej.runtime.ExitException")) {
// 
            // // this was a "System.exit()", not a real exception!
            // exitStatus = Debugger.FORCED_EXIT;
			// owner.raiseStateChangeEvent(Debugger.RUNNING, Debugger.IDLE);
            // lastException = new ExceptionDescription(exceptionText);
        // } 
        // else {
        	// real exception

            Location loc = exc.location();
            String sourceClass = loc.declaringType().name();
            String fileName;
            try {
                fileName = loc.sourceName();
            } catch (AbsentInformationException e) {
                fileName = null;
            }
            int lineNumber = loc.lineNumber();

            List stack = JdiThread.getStack(exc.thread());
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription(excClass, exceptionText, stack);
//        }
    }

    /**
     * A breakpoint has been hit or step completed in a thread.
     */
    public void breakpointEvent(LocatableEvent event, boolean breakpoint)
    {
        if (event.request().getProperty("s") != null) {
            System.out.println(event);
        }
        
        // if the breakpoint is marked as with the SERVER_STARTED property
        // then this is our own breakpoint that we have been waiting for at startup
        else if (event.request().getProperty(SERVER_STARTED_METHOD_NAME) != null) {
            // wake up the waitForStartup() method
            synchronized (this) {
                notifyAll();
            }
        }
        // if the breakpoint is marked with the SERVER_SUSPEND property
        // then it is a main/worker thread returning to its breakpoint
        // after completing some work. We want to leave it suspended here until
        // it is required to do more work.
        else if (event.request().getProperty(SERVER_SUSPEND_METHOD_NAME) != null) {
			// do nothing except signify our change of state
			if (serverThread != null && serverThread.equals(event.thread()))
				owner.raiseStateChangeEvent(Debugger.RUNNING, Debugger.IDLE);
        } 
        else {
            // breakpoint set by user in user code
			if (serverThread.equals(event.thread()))
				owner.raiseStateChangeEvent(Debugger.RUNNING, Debugger.SUSPENDED);

			// a breakpoint/step event in our SHELL class
			// means the user has stepped past the end of a method
			// and we should continue the machine
			Location location = event.location();
			String fileName;
			try {
				fileName = location.sourceName();
			} catch (AbsentInformationException e) {
				fileName = null;
			}

			if (fileName != null && fileName.startsWith("__SHELL")) {
				machine.resume();
			}
			else {
				// otherwise, signal the breakpoint/step to the user
				owner.breakpoint(event.thread());
			}
        }
    }

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

	/**
	 * Find and load all classes declared in the same source file as
	 * className and then find the Location object for the source at
	 * the line 'line'.
	 */
	private Location loadClassesAndFindLine(String className, int line)
	{
		ReferenceType remoteClass = null;
		try {
			remoteClass = loadClass(className);
		} catch (ClassNotFoundException cnfe) {
			return null;
		}
		List allTypesInFile = new ArrayList();
   	
		// find all ReferenceType's declared in this source file
		// TODO: not done properly - only already initialised inner
		// classes will be found
		buildNestedTypes(remoteClass, allTypesInFile);		

		Iterator it = allTypesInFile.iterator();
		while(it.hasNext()) {
			ReferenceType r = (ReferenceType) it.next();

			try {
				List list = r.locationsOfLine(line);
				if (list.size() > 0)
					return (Location) list.get(0);
			}
			catch (AbsentInformationException aie) { }
		}
		return null;
	}

	/**
	 * Recursively construct a list of all Types started with
	 * rootType and including all its nested types.
	 * 
	 * @param rootType  the root to start building at
	 * @param l         the List to add the reference types to
	 */
	private void buildNestedTypes(ReferenceType rootType, List l)
	{
		l.add(rootType);

		List nestedTypes = rootType.nestedTypes();
		Iterator it = nestedTypes.iterator();
		while(it.hasNext()) {
			ReferenceType r = (ReferenceType) it.next();
			buildNestedTypes(r, l);
		}
	}

    /**
     * Set a breakpoint at a specified line in a class.
     *
     * @param   className  The class in which to set the breakpoint.
     * @param   line       The line number of the breakpoint.
     * @return  null if there was no problem, or an error string
     */
    String setBreakpoint(String className, int line)
    {
		Location location = loadClassesAndFindLine(className, line);
		if (location == null) {
			return Config.getString("debugger.jdiDebugger.noCodeMsg");
		}

        EventRequestManager erm = machine.eventRequestManager();
        BreakpointRequest bpreq = erm.createBreakpointRequest(location);
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        bpreq.putProperty(VMEventHandler.DONT_RESUME, "yes");
        bpreq.enable();

        return null;
    }

    /**
     * Clear all the breakpoints at a specified line in a class.
     *
     * @param   className  The class in which to clear the breakpoints.
     * @param   line       The line number of the breakpoint.
     * @return  null if there was no problem, or an error string
     */
    String clearBreakpoint(String className, int line)
    {
		Location location = loadClassesAndFindLine(className, line);
		if (location == null) {
			return Config.getString("debugger.jdiDebugger.noCodeMsg");
		}

        EventRequestManager erm = machine.eventRequestManager();
        boolean found = false;
        List list = erm.breakpointRequests();
        for (int i = 0; i < list.size(); i++) {
            BreakpointRequest bp = (BreakpointRequest) list.get(i);
            if (bp.location().equals(location)) {
                erm.deleteEventRequest(bp);
                found = true;
            }
        }
        // bp not found
        if (found)
            return null;
        else
            return Config.getString("debugger.jdiDebugger.noBreakpointMsg");
    }

    /**
     * Get the value of a static field in a class.
     * 
     * @return a reference to the object in the field or null if the field
     *         could not be found
     * @throws ClassNotFoundException
     */
    public ObjectReference getStaticValue(String className, String fieldName)
		throws ClassNotFoundException
    {
        ReferenceType cl = loadClass(className);

        Field resultField = cl.fieldByName(fieldName);
        if (resultField == null)
            return null;

        return (ObjectReference) cl.getValue(resultField);
    }

    /**
     * Return a list of the Locations of user breakpoints in the
     * VM.
     */
    public List getBreakpoints()
    {
		// Debug.message("[VMRef] getBreakpoints()");
		
		EventRequestManager erm = machine.eventRequestManager();
        List breaks = new LinkedList();

        List allBreakpoints = erm.breakpointRequests();
        Iterator it = allBreakpoints.iterator();

        while (it.hasNext()) {
            BreakpointRequest bp = (BreakpointRequest) it.next();

            if (bp.location().declaringType().classLoader() == currentLoader) {
                breaks.add(bp.location());
            }
        }

        return breaks;
    }

    /**
     * Restore the previosuly saved breakpoints with the new classloader.
     *
     * @param loader  The new class loader to restore the breakpoints into
     */
    public void restoreBreakpoints(List saved)
    {
    	// Debug.message("[VMRef] restoreBreakpoints()");

        EventRequestManager erm = machine.eventRequestManager();

		// create the list of locations - converted to the new classloader
		// this has to be done before we suspend the machine because
		// loadClassesAndFindLine needs the machine running to work
		// see bug #526
		List newSaved = new ArrayList();

		Iterator savedIterator = saved.iterator();

		while (savedIterator.hasNext()) {
			Location oldLocation = (Location) savedIterator.next();

			Location newLocation = loadClassesAndFindLine(
									oldLocation.declaringType().name(),
									oldLocation.lineNumber());
									
			if (newLocation != null) {
				newSaved.add(newLocation);
			}
		}

        // to stop our server thread getting away from us, lets halt the
        // VM temporarily
        machine.suspend();

        // we need to throw away all the breakpoints referring to the old
        // class loader but then we need to restore our exitMarker and
        // suspendMethod breakpoints
        erm.deleteAllBreakpoints();
        serverClassAddBreakpoints();

		// add all the new breakpoints we have created
        Iterator it = newSaved.iterator();

        while (it.hasNext()) {
            Location l = (Location) it.next();

			BreakpointRequest bpreq = erm.createBreakpointRequest(l);
			bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			bpreq.putProperty(VMEventHandler.DONT_RESUME, "yes");
			bpreq.enable();
        }

		// start the machine back up
        machine.resume();
    }

    // -- support methods --

	/**
	 * Invoked a static method on a class in the remote VM.
	 * Note that all arguments to methods must be either String's
	 * or objects that are already mirrored onto the remote VM.
	 * 
	 * @param thr           the thread to use as a worker
	 * @param cl		    the reference to the class the method exists in
	 * @param m    			the Method we want to call
	 * @param args			the List of arguments to the method
	 * @param propagateException whether exceptions thrown should be ignored
	 * 							 or returned as the return value
	 * @return				the return value of the method call
	 * 						as a mirrored object on the VM
	 */
	private Value invokeStaticRemoteMethod(ThreadReference thr,
										   ClassType cl,
                                           Method m,
										   List args,
										   boolean propagateException,
										   boolean dontSuspendAll) throws InvocationException
	{
		final int smallDelay = 50;	// milliseconds
    	
		// go through the args and if any aren't VM reference types
		// then fail (unless they are strings in which case we
		// mirror them onto the vm)
		for (ListIterator lit = args.listIterator(); lit.hasNext();) {
			Object o = lit.next();

			if (o instanceof String) {
				lit.set(machine.mirrorOf((String) o));
			} else if (!(o instanceof Mirror)) {
				throw new IllegalArgumentException("invokeStaticRemoteMethod passed a non-Mirror argument");
			}
		}

		// machine.setDebugTraceMode(VirtualMachine.TRACE_EVENTS | VirtualMachine.TRACE_OBJREFS);

		try {
			// if the thread has not returned to its breakpoint yet, we
			// must be patient
			while(!thr.isAtBreakpoint()) {
				synchronized(this) {
					try { wait(smallDelay); } catch (InterruptedException ie) {}
				}
			}

			Value v = null;
			
			if (dontSuspendAll) {
				v = cl.invokeMethod(thr, m, args, ObjectReference.INVOKE_SINGLE_THREADED);
			} else {
				v = cl.invokeMethod(thr, m, args, 0);

				// invokeMethod leaves everything suspended, so restart
				// all the threads
				machine.resume();
			}

			// we shouldn't return until the thread makes it back to its
			// breakpoint
			while(!thr.isAtBreakpoint()) {
				synchronized(this) {
					try { wait(smallDelay); } catch (InterruptedException ie) {}
				}
			}

			// our serverThread in the ExecServer has now
			// returned to a breakpoint. This will then
			// suspend it (see VMEventHandler).
			// This is the state we need - all threads running
			// except serverThread (which should be waiting at a breakpoint).
			return v;
		}
		
		// the first three exceptions would all be the result
		// of errors in our code. Lets print a stack trace.
		catch (IllegalArgumentException iae) {
			Debug.message(iae.toString());        	
		}
		catch (ClassNotLoadedException cnle) {
			Debug.message(cnle.toString());        	
		}
		catch (InvalidTypeException ite) {
			Debug.message(ite.toString());
		}
		catch (InvocationException e) {
			// exception thrown in remote machine
			// we can either propagate the exception as a value
			if (!dontSuspendAll)
				machine.resume();
			if (propagateException)
				throw e;
			// or ignore it because it will be handled
			// in exceptionEvent()
		}
		catch (com.sun.jdi.InternalException e) {
			e.printStackTrace();
			// TODO: is this true?? ajp 28/5/03
			//we regularly get an exception here when trying to load a class
			// while the machine is suspended. It doesn't seem to be fatal.
			// so we just ignore internal exceptions for the moment.
		}
		catch (VMDisconnectedException e) {
			// vm has died or been killed
			throw e;		
		}
		catch (Exception e) {
			Debug.message("sending command " + m.name() + " to remote VM failed: " + e);
		}

		//machine.setDebugTraceMode(VirtualMachine.TRACE_NONE);
		return null;
	}

	/**
	 * 
	 */
	static boolean isAtMainBreakpoint(ThreadReference tr)
	{
		try {
            return (tr.isAtBreakpoint() &&
                  SERVER_CLASSNAME.equals(tr.frame(0).location().declaringType().name()));
        } catch (IncompatibleThreadStateException e) {
            return false;
        }
	}

	/**
	 * 
	 * @param cl
	 * @param fieldName
	 * @return
	 */
	private ObjectReference getStaticField(ClassType cl, String fieldName)
	{
		Field resultField = cl.fieldByName(fieldName);

		if (resultField == null)
			throw new IllegalArgumentException(
				"getting field " + fieldName + " resulted in no fields");

		return (ObjectReference) cl.getValue(resultField);
	}
	
    /**
     * Find the mirror of a class/interface/array in the remote VM.
     *
     * The class is expected to exist. We expect only one single
     * class to exist with this name. Throws a ClassNotFoundException
     * if the class could not be found.
     * 
     * This should only be used for classes that we know exist
     * and are loaded ie ExecServer etc.
     */
    private ReferenceType findClassByName(String className, ClassLoaderReference clr)
    	throws ClassNotFoundException
    {
        // find the class
        List list = machine.classesByName(className);
        if (list.size() == 1) {
            return (ReferenceType) list.get(0);
        } else if (list.size() > 1) {
            Iterator iter = list.iterator();
            while (iter.hasNext()) {
                ReferenceType cl = (ReferenceType) iter.next();
                if (cl.classLoader() == clr)
                    return cl;
            }
        }
		throw new ClassNotFoundException(className);
    }

    /**
     * Find the mirror of a method in the remote VM.
     *
     * The method is expected to exist. We expect only one single
     * method to exist with this name and report an error if more
     * than one is found.
     */
    Method findMethodByName(ClassType type, String methodName)
    {
        List list = type.methodsByName(methodName);
        if (list.size() != 1) {
            throw new IllegalArgumentException(
                "getting method " + methodName + " resulted in " + list.size() + " methods");
        }
        return (Method) list.get(0);
    }

    /**
     * Create a thread that will retrieve any output from the remote
     * machine and direct it to our terminal (or vice versa).
     */
    private IOHandlerThread redirectIOStream(final Reader reader, final Writer writer, boolean buffered)
    {
        IOHandlerThread thr;

        thr = new IOHandlerThread(reader, writer, buffered);
        thr.setPriority(Thread.MAX_PRIORITY - 1);
        thr.start();
        
        return thr;
    }

	/**
	 * The thread for retrieving output from the remote machine
	 * and redirecting it to the terminal.
	 */
    private class IOHandlerThread extends Thread
    {
        private Reader reader;
        private Writer writer;
        private boolean buffered;
		private volatile boolean keepRunning = true;
		
        IOHandlerThread(Reader reader, Writer writer, boolean buffered)
        {
            super("BlueJ I/O Handler " + (buffered ? "(buffered)" : "(unbuffered)"));
            this.reader = reader;
            this.writer = writer;
            this.buffered = buffered;
        }

		public void close()
		{
			keepRunning = false;
		}
		
        public void run()
        {
            try {
                if (buffered) {
					BufferedReader in = new BufferedReader(reader);

					String line;
					while (keepRunning && (line = in.readLine()) != null) {
						line += '\n';
						writer.write(line.toCharArray(), 0, line.length());
						writer.flush();
					}
                }
                else {
					int ch;
					while (keepRunning && (ch = reader.read()) != -1) {
						writer.write(ch);
						writer.flush();
					}
                }
            } catch (IOException ex) {
                // Debug.reportError("Cannot read output user VM.");
            }
        }
    }

    private void sleep(int millisec)
    {
        synchronized (this) {
            try {
                wait(millisec);
            } catch (InterruptedException e) {}
        }
    }

	public void dumpBreakpoints()
	{
		List l = machine.eventRequestManager().breakpointRequests();
		Iterator it = l.iterator();
		while (it.hasNext()) {
			BreakpointRequest bp = (BreakpointRequest) it.next();
			Debug.message(bp + " " + bp.location().declaringType().classLoader());
		}
	}

    public void dumpThreadInfo()
    {
        Debug.message("threads:");
        Debug.message("--------");

        List threads = machine.allThreads();
        if (threads == null)
            Debug.message("cannot get thread info!");
        else {
            for (int i = 0; i < threads.size(); i++) {
                JdiThread thread = (JdiThread) threads.get(i);
                String status = thread.getStatus();
                Debug.message(thread.getName() + " [" + status + "]");
                try {
                    Debug.message(
                        "  group: " + ((JdiThread) thread).getRemoteThread().threadGroup());
                    Debug.message(
                        "  suspend count: "
                            + ((JdiThread) thread).getRemoteThread().suspendCount());
                    Debug.message(
                        "  monitor: "
                            + ((JdiThread) thread).getRemoteThread().currentContendedMonitor());
                } catch (Exception e) {
                    Debug.message("  monitor: exc: " + e);
                }
            }
        }
    }

    public void dumpConnectorArgs(Map arguments)
    {
        // debug code to print out all existing arguments and their
        // description
        Collection c = arguments.values();
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Connector.Argument a = (Connector.Argument) i.next();
            Debug.message("arg name: " + a.name());
            Debug.message("  descr: " + a.description());
            Debug.message("  value: " + a.value());
        }
    }
}
