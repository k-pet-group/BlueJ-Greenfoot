package bluej.debugger.jdi;

import bluej.debugger.*;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.utility.*;
import bluej.runtime.ExecServer;
import bluej.terminal.Terminal;

import java.io.*;
import java.util.List;
import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ExceptionEvent;

/**
 * A class implementing the execution and debugging primitives needed by
 * BlueJ.
 *
 * Execution and debugging is implemented here on a second ("remote")
 * virtual machine, which gets started from here via the JDI interface.
 *
 * @author  Michael Kolling
 * @version $Id: JdiDebugger.java 1537 2002-11-29 13:40:19Z ajp $
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
public final class JdiDebugger extends Debugger
{
    // the class name of the execution server class running on the remote VM
    static final String SERVER_CLASSNAME = "bluej.runtime.ExecServer";

    // the field name of the static field within that class that hold the
    // server object
    static final String SERVER_FIELD_NAME = "server";

    // the field name of the static field within that class that hold the
    // terminate exception object
    static final String TERMINATE_FIELD_NAME = "terminateExc";

    // the field name of the static field within that class that hold the
    // exit exception object
    static final String EXIT_FIELD_NAME = "exitExc";

    // the field name of the static field within that class
    // that holds a watched integer for signalling back to the local machine
    static final String EXIT_MARKER_METHOD_NAME = "exitMarker";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "suspendExecution";

    // ==== instance data ====

    // The remote virtual machine used with this debugger
    private VirtualMachine machine = null;

    private Process process = null;
    private VMEventHandler eventHandler = null;

    private ClassType serverClass = null;           // the class of the exec server
    private ObjectReference serverInstance = null;  // the exec server instance
    private ThreadReference serverThread = null;    // the thread of the exec server instance

    private ObjectReference exitException = null; // an exception used to interrupt
    // the main thread when simulating
    // a System.exit()

    private Map execServerMethods = null;           // map of String names to ExecServer methods
                                                    // used by JdiDebugger.invokeMethod
    
    volatile private boolean initialised = false;
    private int machineStatus = IDLE;

    private List savedBreakpoints;              // a list of Location's representing
                                                // a temporarily saved list of breakpoints
                                                // we want to keep
    private int exitStatus;
    private ExceptionDescription lastException;
    private Object executionUserParam;   // a user defined parameter set with
    //  each execution


    public JdiDebugger()
    {
        super();
        executionUserParam = null;
    }


    private synchronized VirtualMachine getVM()
    {
        while(!initialised)
            try {
                wait();
            }
        catch(InterruptedException e) {
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
        Connector.Argument optionsArg =
            (Connector.Argument)arguments.get("options");
        Connector.Argument quoteArg =
            (Connector.Argument)arguments.get("quote");
        //Connector.Argument suspendArg =
        //    (Connector.Argument)arguments.get("suspend");

        if (mainArg == null || optionsArg == null || quoteArg == null) {
            Debug.reportError("Cannot start virtual machine.");
            Debug.reportError("(Incompatible launch connector)");
            return;
        }
        mainArg.setValue(SERVER_CLASSNAME);
        //suspendArg.setValue("false");

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

            machine = connector.launch(arguments);

            process = machine.process();
            
            // redirect error stream from process to System.out
            InputStreamReader processErrorReader 
                = new InputStreamReader(process.getErrorStream());
            //Writer errorWriter = new OutputStreamWriter(System.out);
            Writer errorWriter = Terminal.getTerminal().getErrorWriter();
            redirectIOStream(processErrorReader, errorWriter, false);
            
            // redirect output stream from process to Terminal
            InputStreamReader processInputReader 
                = new InputStreamReader(process.getInputStream());
            Writer terminalWriter = Terminal.getTerminal().getWriter();
            redirectIOStream(processInputReader, terminalWriter, false);
            
            //redirect Terminal input to process output stream
            OutputStreamWriter processWriter 
                = new OutputStreamWriter(process.getOutputStream());
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

        setEventRequests(machine);
        eventHandler = new VMEventHandler(this, machine);

        // now wait until the machine really has started up. We will know that
        // it has when the first breakpoint is hit (see breakEvent).
        try {
            wait();
        }
        catch(InterruptedException e) {}
        initialised = true;
        notifyAll();
        BlueJEvent.raiseEvent(BlueJEvent.CREATE_VM_DONE, null);
    }

    /**
     * Finish debugging
     */
    protected synchronized void endDebugger()
    {
        try {
            if (machine != null) {

                try {
// removed to stop deadlocks on exit
//                    machine.dispose();
                }
                catch (Exception e) {}
                machine = null;
            }
        }
        finally {
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    /**
     * This breakpoint is used to stop the server
     * process to make it wait for our task signals. (We later use the
     * suspended process to perform our task requests.)
     */
    void serverClassAddBreakpoints()
    {
        EventRequestManager erm = machine.eventRequestManager();
        serverClass = findClassByName(machine, SERVER_CLASSNAME, null);

        // set a breakpoint in the suspend method
        Method suspendMethod = findMethodByName(serverClass,
                                                SERVER_SUSPEND_METHOD_NAME);
        if(suspendMethod == null) {
            Debug.reportError("invalid VM server object");
            Debug.reportError("Fatal: User code execution will not work");
            return;
        }
        Location loc = suspendMethod.location();
        BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        bpreq.putProperty("isBluejBreak", "true");
        bpreq.enable();

        // set a breakpoint on a special exitMarker method
        Method exitMarkerMethod = findMethodByName(serverClass,
                                                   EXIT_MARKER_METHOD_NAME);
        Location exitMarkerLoc = exitMarkerMethod.location();

        BreakpointRequest exitbpreq = erm.createBreakpointRequest(exitMarkerLoc);
        exitbpreq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        exitbpreq.putProperty("isExitMarker", "true");
        exitbpreq.enable();
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
        Object args[] = { scopeId, classpath };

        ClassLoaderReference loader = (ClassLoaderReference)
            invokeExecServer(ExecServer.CREATE_LOADER, Arrays.asList(args));

        return new JdiClassLoader(scopeId, loader);
    }

    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
        Object args[] = { loader.getId() };

        invokeExecServer(ExecServer.REMOVE_LOADER, Arrays.asList(args));
    }


    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED).
     */
    public int getStatus()
    {
        return machineStatus;
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
        ClassType shellClass = findClassByName(machine, classname, loader);

        Method runMethod = findMethodByName(shellClass, "run");
        if(runMethod == null) {
            Debug.reportError("Could not find shell run method");
            return;
        }

        // ** call Shell.run() **

        List arguments = new ArrayList();	// empty argument list
        try {
            exitStatus = NORMAL_EXIT;

            // store the users execution parameter. currently, we use this to
            // store the project that started this execution, so that we can
            // find classes for breakpoints later (in case several projects
            // are open).
            executionUserParam = eventParam;
            machineStatus = RUNNING;

            Value returnVal = shellClass.invokeMethod(serverThread,
                                                      runMethod,
                                                      arguments, 0);
            // returnVal is type void
            // 'invokeMethod' is synchronous - when we get here it has
            // finished

            // invokeMethod leaves everything suspended, so restart the
            // system threads...
            resumeMachine();
            //dumpThreadInfo();
        }
        catch(InvocationException e) {
            resumeMachine();
            // exception thrown in remote machine - ignored here. The
            // exception is handled through the exceptionEvent method
        }
        catch(Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            e.printStackTrace();
            exitStatus = EXCEPTION;
            lastException = new ExceptionDescription(
                             "Internal BlueJ error: unexpected exception in remote VM\n" +
                             e);
        }
        machineStatus = IDLE;
        //executionUserParam = null;
    }


    /**
     * Load a class in the remote machine.
     */
    private void loadClass(DebuggerClassLoader loader, String classname)
    {
        Object args[] = { loader.getId(), classname };

        invokeExecServer(ExecServer.LOAD_CLASS, Arrays.asList(args));
    }


    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String newObjectName,
                                    DebuggerObject job)
    {
        Object args[] = { scopeId, newObjectName, ((JdiObject)job).getObjectReference() };

        invokeExecServer( ExecServer.ADD_OBJECT, Arrays.asList(args));
    }

    /**
     * Remove an object from a package scope (when removed from object bench).
     * This has to be done tolerantly: If the named instance is not in the
     * scope, we just quietly return.
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
        Object args[] = { scopeId, instanceName };

        invokeExecServer( ExecServer.REMOVE_OBJECT, Arrays.asList(args) );
    }

    /**
     * Set the class path of the remote VM
     */
    public void setLibraries(String classpath)
    {
        Object args[] = { classpath };

        invokeExecServer( ExecServer.SET_LIBRARIES, Arrays.asList(args));
    }

    /**
     * Set the remote "current directory" for relative file access.
     * Cannot be used currently because all the class loading goes wrong
     * once the directory gets changed. Someone needs to fix the class
     * loading first.
     */
    public void setDirectory(String path)
    {
        Object args[] = { path };

        invokeExecServer(ExecServer.SET_DIRECTORY, Arrays.asList(args));
    }

    public Map runTestSetUp(String loaderId, String scopeId, String className)
    {
        return null;
    }

    public void runTestClass(String loaderId, String scopeId, String className)
    {
    }

    public void runTestMethod(String loaderId, String scopeId, String className, String methodName)
    {
    }    


    /**
     * Serialize an object in the debugger to a file
     */
    public void serializeObject(String scopeId, String instanceName,
                                String fileName)
    {
        Object args[] = { scopeId, instanceName, fileName };
        invokeExecServer(ExecServer.SERIALIZE_OBJECT, Arrays.asList(args));
    }

    /**
     * Deserialize an object in the debugger from a file
     */
    public DebuggerObject deserializeObject(String loaderId, String scopeId,
                                            String newInstanceName, String fileName)
    {
        Object args[] = { scopeId, newInstanceName, fileName };

        ObjectReference objRef = (ObjectReference)
            invokeExecServer(ExecServer.DESERIALIZE_OBJECT, Arrays.asList(args));

        if (objRef == null)
            return null;

        return JdiObject.getDebuggerObject(objRef);
    }

    /**
     * Dispose all top level windows in the remote machine.
     */
    public void disposeWindows()
    {
        invokeExecServer(ExecServer.DISPOSE_WINDOWS, Collections.EMPTY_LIST);
    }

    /**
     * Supress error output on the remote machine.
     */
    public void supressErrorOutput()
    {
        invokeExecServer( ExecServer.SUPRESS_OUTPUT, new ArrayList() );
    }

    /**
     * Restore error output on the remote machine.
     */
    public void restoreErrorOutput()
    {
        invokeExecServer( ExecServer.RESTORE_OUTPUT, new ArrayList() );
    }

    private Value invokeExecServer( String methodName, List args )
    {
        VirtualMachine vm = getVM();

        if(serverInstance == null) {
            if(!setupServerConnection(vm))
                return null;
        }

        // go through the args and if any aren't VM reference types
        // then fail (unless they are strings in which case we
        // mirror them onto the vm)
        for(ListIterator lit = args.listIterator(); lit.hasNext(); ) {
            Object o = lit.next();

            if (o instanceof String) {
                lit.set(vm.mirrorOf((String) o));
            }
            else if (!(o instanceof Mirror)) {
                throw new IllegalArgumentException("invokeExecServer passed a non-Mirror argument");
            }
        }

        // if the VM crashes then many of these methods may fail. Our
        // catch all exception will grab them all allowing our local
        // VM to struggle on without the remote VM (previously, we could
        // not quit the local VM once the remote VM had crashed)

        try {
            Method m = (Method) execServerMethods.get(methodName);

            if (m == null)
                throw new IllegalArgumentException("no ExecServer method called " + methodName);

            Value v = serverInstance.invokeMethod(
                        serverThread, m, args, ClassType.INVOKE_SINGLE_THREADED);

            // invokeMethod leaves everything suspended, so restart the
            // system threads...
            resumeMachine();

            return v;
        }
        catch(com.sun.jdi.InternalException e) {
            // we regularly get an exception here when trying to load a class
            // while the machine is suspended. It doesn't seem to be fatal.
            // so we just ignore internal exceptions for the moment.
        }
        catch(Exception e) {
            Debug.message("sending command " + methodName + " to remote VM failed: " + e);
        }

        return null;
    }

    /**
     * Start the server process on the remote machine to perform a task.
     * Arguments to the server are a task ID specifying what we want done,
     * and four optional string parameters. The string parameters must not
     * be null. The task ID is one of the constants defined in
     * runtime.ExecServer.
     *
     * Returns the class loader if the task is CREATE_LOADER, the new object
     * if the task is DESERIALIZE_OBJECT, null otherwise.
     *
     * This is done synchronously: we return once the remote execution
     * has completed.
     */
/*    private Value startServer(int task, String arg1,
                              String arg2, String arg3, String arg4)
    {
            List arguments = new ArrayList(5);
            arguments.add(vm.mirrorOf(task));
            arguments.add(vm.mirrorOf(arg1));
            arguments.add(vm.mirrorOf(arg2));
            arguments.add(vm.mirrorOf(arg3));
            arguments.add(vm.mirrorOf(arg4));
            Value returnVal = execServer.invokeMethod(serverThread,
                                                      performTaskMethod,
                                                      arguments, 0);
            return returnVal;
        }
        }
        return null;
    }
*/

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

        Field serverField = serverClass.fieldByName(SERVER_FIELD_NAME);
        serverInstance = (ObjectReference)serverClass.getValue(serverField);

        if(serverInstance == null) {
            sleep(3000);
            serverInstance = (ObjectReference)serverClass.getValue(serverField);
        }
        if(serverInstance == null) {
            Debug.reportError("Failed to load VM server object");
            Debug.reportError("Fatal: User code execution will not work");
            return false;
        }

        // set up a terminate exception object on the remote machine
        Field termExcField = serverClass.fieldByName(TERMINATE_FIELD_NAME);
        ObjectReference terminateException =
            (ObjectReference)serverClass.getValue(termExcField);
        JdiThread.setTerminateException(terminateException);

        // set up an exit exception object on the remote machine
        Field exitExcField = serverClass.fieldByName(EXIT_FIELD_NAME);
        exitException = (ObjectReference)serverClass.getValue(exitExcField);

        // okay, we have the server object; now get the methods we need

        execServerMethods = new HashMap();

        execServerMethods.put( ExecServer.CREATE_LOADER,
                                findMethodByName(serverClass,ExecServer.CREATE_LOADER));
        execServerMethods.put( ExecServer.REMOVE_LOADER,
                                findMethodByName(serverClass,ExecServer.REMOVE_LOADER));
        execServerMethods.put( ExecServer.LOAD_CLASS,
                                findMethodByName(serverClass,ExecServer.LOAD_CLASS));
        execServerMethods.put( ExecServer.ADD_OBJECT,
                                findMethodByName(serverClass,ExecServer.ADD_OBJECT));
        execServerMethods.put( ExecServer.REMOVE_OBJECT,
                                findMethodByName(serverClass,ExecServer.REMOVE_OBJECT));
        execServerMethods.put( ExecServer.SET_LIBRARIES,
                                findMethodByName(serverClass,ExecServer.SET_LIBRARIES));
        execServerMethods.put( ExecServer.SET_DIRECTORY,
                                findMethodByName(serverClass,ExecServer.SET_DIRECTORY));
        execServerMethods.put( ExecServer.RUN_TEST_SETUP,
                                findMethodByName(serverClass,ExecServer.RUN_TEST_SETUP));
        execServerMethods.put( ExecServer.RUN_TEST_CLASS,
                                findMethodByName(serverClass,ExecServer.RUN_TEST_CLASS));
        execServerMethods.put( ExecServer.RUN_TEST_METHOD,
                                findMethodByName(serverClass,ExecServer.RUN_TEST_METHOD));
        execServerMethods.put( ExecServer.SERIALIZE_OBJECT,
                                findMethodByName(serverClass,ExecServer.SERIALIZE_OBJECT));
        execServerMethods.put( ExecServer.DESERIALIZE_OBJECT,
                                findMethodByName(serverClass,ExecServer.DESERIALIZE_OBJECT));
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


    /**
     * Get a class from the virtual machine.
     */
    public DebuggerClass getClass(String className, DebuggerClassLoader loader)
    {
        loadClass(loader, className);
        ReferenceType classMirror = findClassByName(getVM(), className, loader);
        if(classMirror == null)
            return null;
        else
            return new JdiClass(classMirror);
    }


    /**
     * Get the value of a static field in a class.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
        throws Exception
    {
        DebuggerObject object = null;

        ReferenceType classMirror = findClassByName(getVM(), className, null);

        //Debug.message("[getStaticValue] " + className + ", " + fieldName);

        if(classMirror == null) {
            Debug.reportError("Cannot find class for result value");
            object = null;
        }
        else {
            Field resultField = classMirror.fieldByName(fieldName);
            ObjectReference obj = (ObjectReference)classMirror.getValue(resultField);
            object = JdiObject.getDebuggerObject(obj);
        }

        return object;
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

        if(excClass.equals("bluej.runtime.TerminateException")) {
            // this was an explicit "terminate" by the user
            disposeWindows();
            //restoreErrorOutput();
            exitStatus = TERMINATED;
            machineStatus = IDLE;
            lastException = null;
            //BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_FINISHED, null);
            return;
        }

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
            exitStatus = FORCED_EXIT;
            machineStatus = IDLE;
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

            List stack = new JdiThread(exc.thread(), null).getStack();
            exitStatus = EXCEPTION;
            lastException = new ExceptionDescription(excClass, exceptionText,
                                                     stack);
        }
    }

    /**
     * A breakpoint has been hit or step completed in the specified thread.
     * Find the user thread that started the execution and let it continue.
     * (The user thread is waiting in the waitqueue.)
     */
    public void breakEvent(LocatableEvent event, boolean breakpoint)
    {
        // if the breakpoint is marked as "BluejBreak" then this is our
        // own breakpoint that we have been waiting for at startup
        if("true".equals(event.request().getProperty("isBluejBreak"))) {
            synchronized(this) {
                notifyAll();
            }
        }
        // if the breakpoint is marked as "ExitMarker" then this is our
        // own breakpoint that the RemoteSecurityManager executes in order
        // to signal to us that System.exit() has been called by the AWT
        // thread. If our serverThread is still executing then stop it by simulating
        // an ExitException
        else if("true".equals(event.request().getProperty("isExitMarker"))) {
            if(!serverThread.isSuspended()) {
                try {
                    serverThread.stop(exitException);
                }
                catch(com.sun.jdi.InvalidTypeException ite) { }
            }
        }
        else {
            // breakpoint set by user in user code

            machineStatus = SUSPENDED;
            ThreadReference remoteThread = event.thread();
            JdiThread thread = new JdiThread(remoteThread, executionUserParam);
            if(thread.getClassSourceName(0).startsWith("__SHELL")) {
                // stepped out into the shell class - resume to finish
                getVM().resume();
            }
            else {
                if(breakpoint)
                    BlueJEvent.raiseEvent(BlueJEvent.BREAKPOINT, thread);
                else
                    BlueJEvent.raiseEvent(BlueJEvent.HALT, thread);
            }
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
    private String setBreakpoint(String className, int line,
                                    DebuggerClassLoader loader)
        throws AbsentInformationException
    {
        loadClass(loader, className);
        ClassType remoteClass = findClassByName(getVM(), className, loader);

        if(remoteClass == null)
            return "Class not found";

        Location loc = findLocationInLine(remoteClass, line);
        if(loc == null)
            return Config.getString("debugger.jdiDebugger.noCodeMsg");

        EventRequestManager erm = getVM().eventRequestManager();
        BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
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
    private String clearBreakpoint(String className, int line,
                                       DebuggerClassLoader loader)
        throws AbsentInformationException
    {
        loadClass(loader, className);
        ClassType remoteClass = findClassByName(getVM(), className, loader);

        if(remoteClass == null)
            return "Class not found";

        Location loc = findLocationInLine(remoteClass, line);
        if(loc == null)
            return Config.getString("debugger.jdiDebugger.noCodeMsg");

        EventRequestManager erm = getVM().eventRequestManager();
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
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set/clear the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     *
     * @return  null if there was no problem, or an error string
     */
    public String toggleBreakpoint(String className, int line, boolean set,
                                   DebuggerClassLoader loader)
    {
        //Debug.message("[toggleBreakpoint]: " + className);

        try {
            if(set) {
                return setBreakpoint(className, line, loader);
            }
            else {
                return clearBreakpoint(className, line, loader);
            }
        }
        catch(AbsentInformationException e) {
            return Config.getString("debugger.jdiDebugger.noLineNumberMsg");
        }
        catch(InvalidLineNumberException e) {
            return Config.getString("debugger.jdiDebugger.noCodeMsg");
        }
        catch(Exception e) {
            Debug.reportError("breakpoint error: " + e);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
        }
    }

    /**
     * Temporarily save the breakpoints set in the virtual machine in
     * anticipation that we are about to create a new classloader.
     */
     public void saveBreakpoints()
    {
        VirtualMachine vm = getVM();
        EventRequestManager erm = vm.eventRequestManager();
        savedBreakpoints = new LinkedList();

        List oldBreakpoints = erm.breakpointRequests();
        Iterator it = oldBreakpoints.iterator();

        while(it.hasNext()) {
            BreakpointRequest bp = (BreakpointRequest) it.next();

            if(!bp.location().declaringType().name().equals(SERVER_CLASSNAME)) {
                savedBreakpoints.add(bp.location());
            }
        }

        // we need to throw away all the breakpoints referring to the old
        // class loader but then we need to restore our exitMarker and
        // suspendMethod breakpoints
        erm.deleteAllBreakpoints();
        serverClassAddBreakpoints();
    }

    /**
     * Restore the previosuly saved breakpoints with the new classloader.
     *
     * @param loader  The new class loader to restore the breakpoints into
     */
    public void restoreBreakpoints(DebuggerClassLoader loader)
    {
        VirtualMachine vm = getVM();
        EventRequestManager erm = vm.eventRequestManager();

        if (savedBreakpoints != null) {
            Iterator it = savedBreakpoints.iterator();

            while(it.hasNext()) {
                Location l = (Location) it.next();

                try {
                    setBreakpoint(l.declaringType().name(), l.lineNumber(), loader);
                }
                catch(Exception e) {
                    Debug.reportError("breakpoint error: " + e);
                }
            }
        }

        savedBreakpoints = null;
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
        List threads = getVM().allThreads();
        int len = threads.size();

        List threadList = new ArrayList();

        // reverse order to make display nicer (newer threads first)
        for(int i = 0; i < len; i++) {
            ThreadReference thread = (ThreadReference)threads.get(len-i-1);
            threadList.add(new JdiThread(thread));
        }
        return threadList;
    }

    /**
     *  A thread has been stopped.
     */
    public void halt(DebuggerThread thread)
    {
        machine.suspend();
        machineStatus = SUSPENDED;
        if(thread != null)
            thread.setParam(executionUserParam);
        BlueJEvent.raiseEvent(BlueJEvent.HALT, thread);
    }

    /**
     * A thread has been started again by the user. Make sure that it
     * is indicated in the interface.
     */
    public void cont()
    {
        machineStatus = RUNNING;
        BlueJEvent.raiseEvent(BlueJEvent.CONTINUE, null);
        resumeMachine();
    }

    /**
     * Terminate a thread in the machine.
     */
    public void terminate(DebuggerThread thread)
    {
        //supressErrorOutput();
        thread.terminate();
        //restoreErrorOutput();
    }

    /**
     *  Arrange to show the source location for a specific frame number
     *  of a specific thread. The currently selected frame is stored in the
     *  thread object itself.
     */
    public void showSource(DebuggerThread thread)
    {
        thread.setParam(executionUserParam);
        BlueJEvent.raiseEvent(BlueJEvent.SHOW_SOURCE, thread);
    }


    /**
     * Resume all threads in the VM. If the server thread is idle, make sure
     * that i doesn't get resumed. (The execution server thread waits for
     * tasks suspended at an internal breakpoint - it should never get past
     * this breakpoint.)
     */
    private void resumeMachine()
    {
        if(serverThreadIdle()) {
            serverThread.suspend();
        }
        getVM().resume();
    }

    private boolean serverThreadIdle()
    {
        try {
            return serverThread.isAtBreakpoint() &&
                serverThread.frame(0).location().declaringType().name().equals(
                                                                SERVER_CLASSNAME)
                && (serverThread.suspendCount() == 1);
        }
        catch (IncompatibleThreadStateException exc) {
            Debug.reportError("debugger thread in run-away state...");
            return false;
        }
    }

    // -- support methods --

    /**
     * Find the mirror of a class in the remote VM.
     *
     * The class is expected to exist. We expect only one single
     * class to exist with this name and report an error if more
     * than one is found.
     */
    private ClassType findClassByName(VirtualMachine vm, String classname,
                                      DebuggerClassLoader loader)
    {
        JdiClassLoader jdiLoader = (JdiClassLoader)loader;

        List list = vm.classesByName(classname);
        if(list.size() == 1) {
            return (ClassType)list.get(0);
        }
        else if(list.size() > 1) {
            if(loader == null) {
                Debug.reportError("found more than one class: " + classname);
                return null;
            }
            Iterator iter = list.iterator();
            while(iter.hasNext()) {
                ClassType cl = (ClassType)iter.next();
                if(cl.classLoader() == jdiLoader.getLoader())
                    return cl;
            }
            Debug.reportError("cannot load class: " + classname);
            Debug.reportError("classes found, but none matches loader");
            return null;
        }
        else {
            Debug.reportError("cannot find class " + classname);
            return null;
        }
    }

    /**
     * Find the mirror of a method in the remote VM.
     *
     * The method is expected to exist. We expect only one single
     * method to exist with this name and report an error if more
     * than one is found.
     */
    private Method findMethodByName(ClassType type, String methodName)
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
    private Location findLocationInLine(ClassType cl, int line)
        throws AbsentInformationException
    {
        List list = cl.locationsOfLine(line);
        if(list.size() == 0)
            return null;
        else
            return (Location)list.get(0);
    }

    /**
     *  Set up event requests - this indicated of which events from the
     *  remote VM we want ot be informed.
     */
    private void setEventRequests(VirtualMachine vm)
    {
        EventRequestManager erm = vm.eventRequestManager();
        // want all uncaught exceptions
        erm.createExceptionRequest(null, false, true).enable();
        erm.createClassPrepareRequest().enable();
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
                new Thread("I/O reader") {
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
                new Thread("I/O reader") {
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

    /**
     * Return the jdi thread. This exposes the jdi to Inspectors.
     */
    public com.sun.jdi.ThreadReference getServerThread() {
        return serverThread;
    }
}

