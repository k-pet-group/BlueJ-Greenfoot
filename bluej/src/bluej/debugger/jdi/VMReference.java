package bluej.debugger.jdi;

import java.io.*;
import java.util.*;

import bluej.*;
import bluej.debugger.*;
import bluej.runtime.ExecServer;
import bluej.terminal.Terminal;
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
 * @version $Id: VMReference.java 1991 2003-05-28 08:53:06Z ajp $
 *
 * The startup process is as follows:
 *
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
 *    continue <-----------------.
 *      .
 *      .
 *
 * We can now execute commands on the remote VM by invoking methods
 * using the server thread (which is suspended at the breakpoint).
 * This is done in the "startServer()" method.
 */
public class VMReference
{
    // the class name of the execution server class running on the remote VM
    static final String SERVER_CLASSNAME = "bluej.runtime.ExecServer";

    // the field name of the static field within that class that hold the
    // server object
    static final String SERVER_FIELD_NAME = "server";

    // the field name of the static field within that class that hold the
    // exit exception object
    static final String EXIT_FIELD_NAME = "exitExc";

    // the field name of the static field within that class
    // the name of the method used to signal a System.exit()
    static final String SERVER_EXIT_MARKER_METHOD_NAME = "exitMarker";

	// the name of the method used to suspend the ExecServer
	static final String SERVER_STARTED_METHOD_NAME = "vmStarted";
	
    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "vmSuspend";

    // ==== instance data ====

    // The remote virtual machine and process we are referring to
    private VirtualMachine machine = null;
    private Process process = null;

	// The handler for virtual machine events    
    private VMEventHandler eventHandler = null;

	// the class reference to ExecServer
	private ClassType serverClass = null;

	// the thread running inside the ExecServer
	private ThreadReference serverThread = null;
    
    // the current class loader in the ExecServer
    private ClassLoaderReference currentLoader = null;
    
	// an exception used to interrupt the main thread
	// when simulating a System.exit()
    private ObjectReference exitException = null;

    private Map execServerMethods = null;           // map of String names to ExecServer methods
                                                    // used by JdiDebugger.invokeMethod
    
    private int machineStatus = Debugger.IDLE;

    private int exitStatus;
    private ExceptionDescription lastException;
    private Object executionUserParam;   // a user defined parameter set with
    //  each execution


	public VirtualMachine newLaunch(File initDir, VirtualMachineManager mgr)
	{
		// launch the VM
		Process p = null;		

		try {
			StringBuffer launchCommand = new StringBuffer();
			
			launchCommand.append("java");
			launchCommand.append(" ");
			launchCommand.append("-classpath \"");
			launchCommand.append(System.getProperty("java.class.path"));
			launchCommand.append("\" ");
			launchCommand.append("-Xdebug -Xint -Xrunjdwp:transport=dt_socket,server=y,address=8000");
			launchCommand.append(" ");
			launchCommand.append(SERVER_CLASSNAME);

			System.out.println(launchCommand.toString());
			p = Runtime.getRuntime().exec(launchCommand.toString(),null,initDir);			
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}

		process = p;
		
		// redirect error stream from process to System.out
		InputStreamReader processErrorReader 
			= new InputStreamReader(p.getErrorStream());
		Writer errorWriter = new OutputStreamWriter(System.err);
		redirectIOStream(processErrorReader, errorWriter, false);
        
		// redirect output stream from process to Terminal
		InputStreamReader processInputReader 
			= new InputStreamReader(p.getInputStream());
		Writer terminalWriter = new OutputStreamWriter(System.out);
		redirectIOStream(processInputReader, terminalWriter, false);
        
		//redirect Terminal input to process output stream
		OutputStreamWriter processWriter 
			= new OutputStreamWriter(p.getOutputStream());
		Reader terminalReader = new InputStreamReader(System.in);
		redirectIOStream(terminalReader, processWriter, false);
		
		AttachingConnector connector = null;
		List connectors = mgr.attachingConnectors();
		
		Iterator it = connectors.iterator();
		while(it.hasNext()) {
			AttachingConnector c = (AttachingConnector) it.next();
			
			if(c.transport().name().equals("dt_socket")) {
				connector = c;
			}
		}

		Map arguments = connector.defaultArguments();

		// "main" is the command line: main class and arguments
		Connector.Argument hostnameArg =
			(Connector.Argument)arguments.get("hostname");
		Connector.Argument portArg =
			(Connector.Argument)arguments.get("port");

		if (hostnameArg == null || portArg == null) {
			throw new IllegalStateException("incompatible JPDA socket launch connector");
		}
		
		hostnameArg.setValue("127.0.0.1");
		portArg.setValue("8000");
		
		try {
			VirtualMachine m = connector.attach(arguments);
			
			return m;
		}
		catch (Exception e) {
			Debug.reportError("Unable to launch target VM.");
			e.printStackTrace();
		}
		
		return null;		
	}
	
	public VirtualMachine defaultLaunch(VirtualMachineManager mgr)
	{
		VirtualMachine m = null;
		Process p = null;
		
		LaunchingConnector connector = mgr.defaultConnector();
		//Debug.message("connector: " + connector.name());
		//Debug.message("transport: " + connector.transport().name());

		Map arguments = connector.defaultArguments();
		// dumpConnectorArgs(arguments);

		// "main" is the command line: main class and arguments
		Connector.Argument mainArg =
			(Connector.Argument)arguments.get("main");
		Connector.Argument optionsArg =
			(Connector.Argument)arguments.get("options");
		Connector.Argument quoteArg =
			(Connector.Argument)arguments.get("quote");

		if (mainArg == null || optionsArg == null || quoteArg == null) {
			throw new IllegalStateException("incompatible JPDA launch connector");
		}
		mainArg.setValue(SERVER_CLASSNAME);

		try {
			// set the optionsArg for the VM launcher
			{
				String vmOptions = Config.getSystemPropString("VmOptions");
				String localVMClassPath = "-classpath " + quoteArg.value() +
											System.getProperty("java.class.path") +
											quoteArg.value();

				if (vmOptions == null)
					optionsArg.setValue(localVMClassPath);
				else
					optionsArg.setValue(vmOptions + " " + localVMClassPath);
			}

			m = connector.launch(arguments);

			p = m.process();
        
			// redirect error stream from process to System.out
			InputStreamReader processErrorReader 
				= new InputStreamReader(p.getErrorStream());
			//Writer errorWriter = new OutputStreamWriter(System.out);
			Writer errorWriter = Terminal.getTerminal().getErrorWriter();
			redirectIOStream(processErrorReader, errorWriter, false);
        
			// redirect output stream from process to Terminal
			InputStreamReader processInputReader 
				= new InputStreamReader(p.getInputStream());
			Writer terminalWriter = Terminal.getTerminal().getWriter();
			redirectIOStream(processInputReader, terminalWriter, false);
        
			//redirect Terminal input to process output stream
			OutputStreamWriter processWriter 
				= new OutputStreamWriter(p.getOutputStream());
			Reader terminalReader = Terminal.getTerminal().getReader();
			redirectIOStream(terminalReader, processWriter, false);

		}
		catch (VMStartException vmse) {
			Debug.reportError("Target VM did not initialise.");
			Debug.reportError("(check the 'VmOptions' setting in 'bluej.defs'.)");
			Debug.reportError(vmse.getMessage() + "\n");
			dumpFailedLaunchInfo(vmse.process());
		}
		catch (Exception e) {
			Debug.reportError("Unable to launch target VM.");
			e.printStackTrace();
		}

		return m;
	}
	
	/**
	 * Create the second virtual machine and start
	 * the execution server (class ExecServer) on that machine.
	 */
    public VMReference(File initialDirectory)
    {
		VirtualMachineManager mgr = Bootstrap.virtualMachineManager();
	
		machine = newLaunch(initialDirectory, mgr);

		// want all uncaught exceptions and all class prepare events
		EventRequestManager erm = machine.eventRequestManager();
		erm.createExceptionRequest(null, false, true).enable();
		erm.createClassPrepareRequest().enable();

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
		}
		catch(InterruptedException e) {}	
	}

    /**
     * Close down this virtual machine.
     */
    public synchronized void close()
    {
    	// causes deadlock - why bother
    	// lets just nuke it
    	//machine.dispose();
		if (process != null) {
			process.destroy();
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
		if(list.size() != 1)
			Debug.reportError("oops - found more than one prepare request!");
		ClassPrepareRequest cpreq = (ClassPrepareRequest)list.get(0);
		erm.deleteEventRequest(cpreq);

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
        serverClass = (ClassType) findClassByName(SERVER_CLASSNAME, null);

		// set a breakpoint in the vm started method
        {
	        Method startedMethod = findMethodByName(serverClass,
	                                                SERVER_STARTED_METHOD_NAME);
	        if(startedMethod == null) {
				throw new IllegalStateException("can't find method ExecServer." +
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
			Method suspendMethod = findMethodByName(serverClass,
													SERVER_SUSPEND_METHOD_NAME);
			if(suspendMethod == null) {
				throw new IllegalStateException("can't find method ExecServer." +
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

        // set a breakpoint on a special exitMarker method
        Method exitMarkerMethod = findMethodByName(serverClass,
                                                   SERVER_EXIT_MARKER_METHOD_NAME);
        Location exitMarkerLoc = exitMarkerMethod.location();

        BreakpointRequest exitbpreq = erm.createBreakpointRequest(exitMarkerLoc);
        exitbpreq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        exitbpreq.putProperty(SERVER_EXIT_MARKER_METHOD_NAME, "yes");
        exitbpreq.enable();
    }

	/**
	 * Find the components on the remote VM that we need to talk to it:
	 * the execServer object, the performTaskMethod, and the serverThread.
	 * These three variables (mirrors to the remote entities) are set up here.
	 * This needs to be done only once.
	 */
	private boolean setupServerConnection(VirtualMachine vm)
	{
		if(serverClass == null)
			Debug.reportError("server class not initialised!");

		// set up an exit exception object on the remote machine
		Field exitExcField = serverClass.fieldByName(EXIT_FIELD_NAME);
		exitException = (ObjectReference)serverClass.getValue(exitExcField);

		// okay, we have the server object; now get the methods we need

		execServerMethods = new HashMap();

		execServerMethods.put( ExecServer.NEW_LOADER,
								findMethodByName(serverClass,ExecServer.NEW_LOADER));
		execServerMethods.put( ExecServer.LOAD_CLASS,
								findMethodByName(serverClass,ExecServer.LOAD_CLASS));
		execServerMethods.put( ExecServer.ADD_OBJECT,
								findMethodByName(serverClass,ExecServer.ADD_OBJECT));
		execServerMethods.put( ExecServer.REMOVE_OBJECT,
								findMethodByName(serverClass,ExecServer.REMOVE_OBJECT));
		execServerMethods.put( ExecServer.SET_LIBRARIES,
								findMethodByName(serverClass,ExecServer.SET_LIBRARIES));
		execServerMethods.put( ExecServer.RUN_TEST_SETUP,
								findMethodByName(serverClass,ExecServer.RUN_TEST_SETUP));
		execServerMethods.put( ExecServer.RUN_TEST_METHOD,
								findMethodByName(serverClass,ExecServer.RUN_TEST_METHOD));
		execServerMethods.put( ExecServer.SUPRESS_OUTPUT,
								findMethodByName(serverClass,ExecServer.SUPRESS_OUTPUT));
		execServerMethods.put( ExecServer.RESTORE_OUTPUT,
								findMethodByName(serverClass,ExecServer.RESTORE_OUTPUT));
		execServerMethods.put( ExecServer.DISPOSE_WINDOWS,
								findMethodByName(serverClass,ExecServer.DISPOSE_WINDOWS));

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

	// -- all methods below here are for after the VM has started up
	
	/**
	 * Return the machine status; one of the "machine state" constants:
	 * (IDLE, RUNNING, SUSPENDED).
	 */
	public int getStatus()
	{
		return machineStatus;
	}
	
	/**
	 * Instruct the remote machine to construct a new class loader
	 * and return its reference.
	 */
	/*package*/ ClassLoaderReference newClassLoader(String classPath)
	{	 
		Object args[] = { classPath };

		ClassLoaderReference loader = (ClassLoaderReference)
		 invokeExecServer(ExecServer.NEW_LOADER, Arrays.asList(args));

		currentLoader = loader;
					
		return loader;
	}

	/**
	 * Load a class in the remote machine and return its reference.
	 */
	/*package*/ ReferenceType loadClass(String className)
		throws ClassNotFoundException
	{
		Object args[] = { className };

		Value v = invokeExecServer(ExecServer.LOAD_CLASS, Arrays.asList(args));

		if (v.type().name().equals("java.lang.Class")) {
			ReferenceType rt = findClassByName(className, currentLoader);
			
			if (rt != null)
				return rt;
		}

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
	public void runShellClass(String className, Object eventParam)
	{
		ClassType shellClass = null;

		try {
			shellClass = (ClassType) loadClass(className);
		}
		catch (ClassNotFoundException cfne) { }

		if (shellClass == null) {
			Debug.reportError("Could not find shell class " + className);
			return;
		}
			
		Method runMethod = findMethodByName(shellClass, "run");
		if(runMethod == null) {
			Debug.reportError("Could not find shell run method");
			return;
		}

		// ** call Shell.run() **
		try {
			exitStatus = Debugger.NORMAL_EXIT;

			// store the users execution parameter. currently, we use this to
			// store the project that started this execution, so that we can
			// find classes for breakpoints later (in case several projects
			// are open).
			executionUserParam = eventParam;
			machineStatus = Debugger.RUNNING;

			Value v = invokeStaticRemoteMethod(shellClass, runMethod,
											Collections.EMPTY_LIST, false);

		}
		catch(VMDisconnectedException e) {
			exitStatus = Debugger.TERMINATED;
		}
		catch(Exception e) {
			// remote invocation failed
			Debug.reportError("starting shell class failed: " + e);
			e.printStackTrace();
			exitStatus = Debugger.EXCEPTION;
			lastException = new ExceptionDescription(
							 "Internal BlueJ error: unexpected exception in remote VM\n" +
							 e);
		}
		machineStatus = Debugger.IDLE;
		//executionUserParam = null;
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
	/*package*/ Value invokeExecServer( String methodName, List args )
    {
        if(serverThread == null) {
            if(!setupServerConnection(machine))
                return null;
        }

        Method m = (Method) execServerMethods.get(methodName);

        if (m == null)
            throw new IllegalArgumentException("no ExecServer method called " + methodName);

		return invokeStaticRemoteMethod(serverClass, m, args, true);
    }

	/**
	 * Invoked a static method on a class in the remote VM.
	 * Note that all arguments to methods must be either String's
	 * or objects that are already mirrored onto the remote VM.
	 * 
	 * @param cl		    the reference to the class the method exists in
	 * @param methodName    the name of the method
	 * @param args			the List of arguments to the method
	 * @param propagateException whether exceptions thrown should be ignored
	 * 							 or returned as the return value
	 * @return				the return value of the method call
	 * 						as an mirrored object on the VM
	 */
	private Value invokeStaticRemoteMethod(ClassType cl, Method m,
											List args, boolean propagateException)
	{
		// go through the args and if any aren't VM reference types
		// then fail (unless they are strings in which case we
		// mirror them onto the vm)
		for(ListIterator lit = args.listIterator(); lit.hasNext(); ) {
			Object o = lit.next();

			if (o instanceof String) {
				lit.set(machine.mirrorOf((String) o));
			}
			else if (!(o instanceof Mirror)) {
				throw new IllegalArgumentException("invokeStaticRemoteMethod passed a non-Mirror argument");
			}
		}

		//machine.setDebugTraceMode(VirtualMachine.TRACE_EVENTS | VirtualMachine.TRACE_OBJREFS);

		try {
			// if serverThread has not returned to its breakpoint yet, we
			// must be patient
			while (!serverThread.isAtBreakpoint()) {
				// System.out.print(".");
				//try { wait(100); } catch (InterruptedException ie) { }
			}

			Value v = cl.invokeMethod(serverThread, m, args, 0);

			// invokeMethod leaves everything suspended, so restart
			// all the threads
			machine.resume();
			
			// our serverThread in the ExecServer will now continue in
			// an infinite loop and return to a breakpoint. This will then
			// suspend it (see VMEventHandler).
			// This is the state we need - all threads running
			// except serverThread (which should be waiting at a breakpoint).
			return v;
		}
		/*
		 * IllegalArgumentException - if the method is not a member of this class or a superclass, if the size of the argument list does not match the number of declared arguemnts for the method, or if the method is an initializer, constructor or static intializer. 
										if any argument in the argument list is not assignable to the corresponding method argument type. 
			ClassNotLoadedException - if any argument type has not yet been loaded through the appropriate class loader. 
			IncompatibleThreadStateException - if the specified thread has not been suspended by an event. 
			InvocationException - if the method invocation resulted in an exception in the target VM. 
			InvalidTypeException - If the arguments do not meet this requirement -- Object arguments must be assignment compatible with the argument type. This implies that the argument type must be loaded through the enclosing class's class loader. Primitive arguments must be either assignment compatible with the argument type or must be convertible to the argument type without loss of information. See JLS section 5.2 for more information on assignment compatibility.
		 */		
		catch(InvocationException e) {
			// exception thrown in remote machine
			// we can either propagate the exception as a value
			if (propagateException)
				return e.exception();
			// or ignore it because it will be handled
			// in exceptionEvent()
		}
		catch(com.sun.jdi.InternalException e) {
			e.printStackTrace();
			// we regularly get an exception here when trying to load a class
			// while the machine is suspended. It doesn't seem to be fatal.
			// so we just ignore internal exceptions for the moment.
		}
		catch(Exception e) {
			Debug.message("sending command " + m.name() + " to remote VM failed: " + e);
		}

		machine.setDebugTraceMode(VirtualMachine.TRACE_NONE);

		return null;
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
     *  An exception was thrown in the remote machine. Analyse the exception
     *  and store it in 'lastException'. It will be picked uplater.
     */
    public void exceptionEvent(ExceptionEvent exc)
    {
        String excClass = exc.exception().type().name();
        ObjectReference remoteException = exc.exception();

        // get the exception text
        // attention: the following depends on the (undocumented) fact that
        // the internal exception message field is named "detailMessage".
        Field msgField =
            remoteException.referenceType().fieldByName("detailMessage");
        StringReference val =
            (StringReference)remoteException.getValue(msgField);

        //better: get message via method call
        //Method getMessageMethod = findMethodByName(
        //				   remoteException.referenceType(),
        //				   "getMessage");
        //StringReference val = null;
        //try {
        //    val = (StringReference)serverInstance.invokeMethod(serverThread,
        //  						getMessageMethod,
        //  						null, 0);
        //} catch(Exception e) {
        //    Debug.reportError("Problem getting exception message: " + e);
        //}

        String exceptionText = (val == null ? null : val.value());

        if(excClass.equals("bluej.runtime.ExitException")) {

            // this was a "System.exit()", not a real exception!
            exitStatus = Debugger.FORCED_EXIT;
            machineStatus = Debugger.IDLE;
            lastException = new ExceptionDescription(exceptionText);
        }
        else {		// real exception

            Location loc = exc.location();
            String sourceClass = loc.declaringType().name();
            String fileName;
            try {
                fileName = loc.sourceName();
            }
            catch(Exception e) {
                fileName = null;
            }
            int lineNumber = loc.lineNumber();

            List stack = new JdiThread(this, exc.thread(), null).getStack();
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription(excClass, exceptionText,
                                                     stack);
        }
    }

    /**
     * A breakpoint has been hit or step completed in the specified thread.
     * Find the user thread that started the execution and let it continue.
     * (The user thread is waiting in the waitqueue.)
     */
    public void breakpointEvent(LocatableEvent event, boolean breakpoint)
    {
        // if the breakpoint is marked as with the SERVER_STARTED property
        // then this is our own breakpoint that we have been waiting for at startup
        if(event.request().getProperty(SERVER_STARTED_METHOD_NAME) != null) {
			// wake up the waitForStartup() method
            synchronized(this) {
            	notifyAll();
            }
        }
		// if the breakpoint is marked with the SERVER_SUSPEND property
		// then it is our main server worker thread returning to its breakpoint
		// after completing some work. We want to leave it suspended here until
		// it is required to do more work.
		else if(event.request().getProperty(SERVER_SUSPEND_METHOD_NAME) != null) {
			// do nothing
		}
        // if the breakpoint is marked as "ExitMarker" then this is our
        // own breakpoint that the RemoteSecurityManager executes in order
        // to signal to us that System.exit() has been called by the AWT
        // thread. If our serverThread is still executing then stop it by simulating
        // an ExitException
        else if(event.request().getProperty(SERVER_EXIT_MARKER_METHOD_NAME) != null) {
			// TODO: why make sure this is not suspended??? ajp 27/5/03
			if(!serverThread.isSuspended()) {
                try {
                    serverThread.stop(exitException);
                }
                catch(com.sun.jdi.InvalidTypeException ite) { }
            }
        }
        else {
			// listBreakpoints();
			
            // breakpoint set by user in user code
            machineStatus = Debugger.SUSPENDED;
            ThreadReference remoteThread = event.thread();
            System.out.println(remoteThread);
            JdiThread thread = new JdiThread(this, remoteThread, executionUserParam);
            if(thread.getClassSourceName(0).startsWith("__SHELL")) {
                // stepped out into the shell class - resume to finish
                System.out.println("stepping out into SHELL class");
                machine.resume();
            }
            else {
                if(breakpoint)
                    BlueJEvent.raiseEvent(BlueJEvent.BREAKPOINT, thread);
                else
                    BlueJEvent.raiseEvent(BlueJEvent.HALT, thread);
            }
        }
    }

	public void listBreakpoints()
	{
		List l = machine.eventRequestManager().breakpointRequests();
		Iterator it = l.iterator();
		while(it.hasNext()) {
			BreakpointRequest bp = (BreakpointRequest) it.next();
			System.out.println(bp + " " + bp.location().declaringType().classLoader());	
		}        	      	
	}

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Set a breakpoint at a specified line in a class.
     *
     * @param   className  The class in which to set the breakpoint.
     * @param   line       The line number of the breakpoint.
     * @return  null if there was no problem, or an error string
     */
    /*package*/ String setBreakpoint(String className, int line)
        throws AbsentInformationException
    {
		ReferenceType remoteClass = null;
		try {
			remoteClass = loadClass(className);
		}
		catch (ClassNotFoundException cnfe) {
			return "class " + className + " not found";
		}

        Location loc = findLocationInLine(remoteClass, line);
        if(loc == null) {
			return Config.getString("debugger.jdiDebugger.noCodeMsg");
        }

        EventRequestManager erm = machine.eventRequestManager();
        BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
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
    /*package*/ String clearBreakpoint(String className, int line)
        throws AbsentInformationException
    {
		ReferenceType remoteClass = null;
		try {
			remoteClass = loadClass(className);
		}
		catch (ClassNotFoundException cnfe) {
			return "class " + className + " not found";
		}

        Location loc = findLocationInLine(remoteClass, line);
        if(loc == null)
            return Config.getString("debugger.jdiDebugger.noCodeMsg");

        EventRequestManager erm = machine.eventRequestManager();
        boolean found = false;
        List list = erm.breakpointRequests();
        for (int i=0 ; i < list.size() ; i++) {
            BreakpointRequest bp = (BreakpointRequest)list.get(i);
            if(bp.location().equals(loc)) {
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
	 */
	public ObjectReference getStaticValue(String className, String fieldName)
	{
		DebuggerObject object = null;
		ReferenceType cl = null;

		try {
			cl = loadClass(className);
		}
		catch (ClassNotFoundException cnfe) {
			return null;
		}

		Field resultField = cl.fieldByName(fieldName);
		if (resultField == null)
			return null;
                
		ObjectReference obj = (ObjectReference)cl.getValue(resultField);

		return obj;
	}

    /**
     * Return a list of the Locations of user breakpoints in the
     * VM.
     */
    public List getBreakpoints()
    {
		EventRequestManager erm = machine.eventRequestManager();
		List breaks = new LinkedList();

        List allBreakpoints = erm.breakpointRequests();
        Iterator it = allBreakpoints.iterator();

        while(it.hasNext()) {
            BreakpointRequest bp = (BreakpointRequest) it.next();

            if(bp.location().declaringType().classLoader() == currentLoader) {
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
		EventRequestManager erm = machine.eventRequestManager();

		// we need to throw away all the breakpoints referring to the old
		// class loader but then we need to restore our exitMarker and
		// suspendMethod breakpoints
		erm.deleteAllBreakpoints();
		serverClassAddBreakpoints();

        Iterator it = saved.iterator();

        while(it.hasNext()) {
            Location l = (Location) it.next();

            try {
                setBreakpoint(l.declaringType().name(), l.lineNumber());
            }
            catch(AbsentInformationException aie) {
                Debug.reportError("breakpoint error: " + aie);
            }
        }
    }

    /**
     * List all the threads being debugged as a list containing elements
     * of type DebuggerThread. Filter out threads that belong to system,
     * returning only user threads. This can be done only if the machine
     * is currently suspended.
     *
     * @return  A list of threads (type JdiThread), or null if the machine
     *		is currently running
     */
    public List listThreads()
    {
        List threads = machine.allThreads();
        int len = threads.size();

        List threadList = new ArrayList();

        // reverse order to make display nicer (newer threads first)
        for(int i = 0; i < len; i++) {
            ThreadReference thread = (ThreadReference)threads.get(len-i-1);
            threadList.add(new JdiThread(this, thread));
        }
        return threadList;
    }

    /**
     *  A thread has been stopped.
     */
    public void halt(DebuggerThread thread)
    {
        machine.suspend();
        machineStatus = Debugger.SUSPENDED;
        if(thread != null)
            thread.setParam(executionUserParam);
    }

    /**
     * A thread has been started again by the user. Make sure that it
     * is indicated in the interface.
     */
    public void cont()
    {
        machineStatus = Debugger.RUNNING;
        machine.resume();
    }

    /**
     *  Arrange to show the source location for a specific frame number
     *  of a specific thread. The currently selected frame is stored in the
     *  thread object itself.
     */
    public void showSource(DebuggerThread thread)
    {
        thread.setParam(executionUserParam);
    }

    // -- support methods --

    /**
     * Find the mirror of a class/interface/array in the remote VM.
     *
     * The class is expected to exist. We expect only one single
     * class to exist with this name and return null if more
     * than one is found. Returns null if the class could not be
     * found.
     * 
     * This should only be used for classes that we know exist
     * and are loaded ie ExecServer etc.
     */
    private ReferenceType findClassByName(String className,
    										ClassLoaderReference clr)
    {
		// find the class
        List list = machine.classesByName(className);
        if(list.size() == 1) {
            return (ReferenceType)list.get(0);
        }
        else if(list.size() > 1) {
			Iterator iter = list.iterator();
			while(iter.hasNext()) {
				ReferenceType cl = (ReferenceType)iter.next();
			   	if(cl.classLoader() == clr)
					return cl;
			}
        }
		return null;
    }

    /**
     * Find the mirror of a method in the remote VM.
     *
     * The method is expected to exist. We expect only one single
     * method to exist with this name and report an error if more
     * than one is found.
     */
    /*package*/ Method findMethodByName(ClassType type, String methodName)
    {
        List list = type.methodsByName(methodName);
        if(list.size() != 1) {
            throw new IllegalArgumentException("getting method " + methodName + " resulted in " + list.size() + " methods");
        }
        return (Method)list.get(0);
    }

    /**
     *  Find the first location in a given line in a class.
     */
    private Location findLocationInLine(ReferenceType cl, int line)
        throws AbsentInformationException
    {
        List list = cl.locationsOfLine(line);
        if(list.size() == 0)
            return null;
        else
            return (Location)list.get(0);
    }

    /**
     *	Create a thread that will retrieve any output from the remote
     *  machine and direct it to our terminal (or vice versa).
     */
    private void redirectIOStream(final Reader reader,
                                  final Writer writer,
                                  boolean buffered)
    {
        Thread thr;

        if(buffered) {
            thr =
                new Thread("I/O reader (buffered)") {
                        public void run() {
                            try {
                                dumpStreamBuffered(reader, writer);
                            }
                            catch (IOException ex) {
                                Debug.reportError("Cannot read output user VM.");
                            }
                        }
                    };
        }
        else {
            thr =
                new Thread("I/O reader (unbuffered)") {
                        public void run() {
                            try {
                                dumpStream(reader, writer);
                            }
                            catch (IOException ex) {
                                Debug.reportError("Cannot read output user VM.");
                            }
                        }
                    };
        }
        thr.setPriority(Thread.MAX_PRIORITY-1);
        thr.start();
    }

    private void dumpStream(Reader reader, Writer writer)
        throws IOException
    {
        int ch;
        while ((ch = reader.read()) != -1) {
            writer.write(ch);
            writer.flush();
        }
    }

    private void dumpStreamBuffered(Reader reader,
                                    Writer writer)
        throws IOException
    {
        BufferedReader in =
            new BufferedReader(reader);
     
        String line;
        while ((line = in.readLine()) != null) {
            line += '\n';
            writer.write(line.toCharArray(), 0, line.length());
            writer.flush();
        }
    }

    private void dumpFailedLaunchInfo(Process process) {
        try {
            InputStreamReader processErrorReader 
                = new InputStreamReader(process.getErrorStream());
            OutputStreamWriter errorWriter = new OutputStreamWriter(System.out);
            dumpStream(processErrorReader, errorWriter);
            //dumpStream(process.getErrorStream(), System.out);
            //dumpStream(process.getInputStream(), System.out);
        }
        catch (IOException e) {
            Debug.message("Unable to display process output: " +
                          e.getMessage());
        }
    }

    private void sleep(int millisec)
    {
        synchronized(this) {
            try {
                wait(millisec);
            }
            catch(InterruptedException e) {}
        }
    }

    public void dumpThreadInfo()
    {
        Debug.message("threads:");
        Debug.message("--------");

        List threads = listThreads();
        if(threads == null)
            Debug.message("cannot get thread info!");
        else {
            for(int i = 0; i < threads.size(); i++) {
                JdiThread thread = (JdiThread)threads.get(i);
                String status = thread.getStatus();
                Debug.message(thread.getName() + " [" + status + "]");
                try{
                    Debug.message("  group: " +
                                  ((JdiThread)thread).getRemoteThread().
                                  threadGroup());
                    Debug.message("  suspend count: " +
                                  ((JdiThread)thread).getRemoteThread().
                                  suspendCount());
                    Debug.message("  monitor: " +
                                  ((JdiThread)thread).getRemoteThread().
                                  currentContendedMonitor());
                }
                catch (Exception e) {
                    Debug.message("  monitor: exc: " + e); }
            }
        }
    }
    
    public void dumpConnectorArgs(Map arguments)
    {
		// debug code to print out all existing arguments and their
		// description
	  	Collection c = arguments.values();
	  	Iterator i = c.iterator();
	  	while(i.hasNext()) {
	  	    Connector.Argument a = (Connector.Argument)i.next();
	  	    Debug.message("arg name: " + a.name());
	  	    Debug.message("  descr: " + a.description());
	  	    Debug.message("  value: " + a.value());
	  	}
    }
}
