package bluej.debugger.jdi;

import bluej.debugger.*;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.runtime.ExecServer;
import bluej.terminal.Terminal;

import java.io.*;
import java.util.Collection;
import java.util.Vector;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Hashtable;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ExceptionRequest;
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
 * @version $Id: JdiDebugger.java 680 2000-09-01 13:06:00Z ajp $
 *
 * The startup process is as follows:
 *
 *
 *  Debugger		VMEventHandler Thread		Remote VM
 *  ----------------------------------------------------------------------
 *  startDebugger:
 *    start VM --------------------------------------> start
 *    start event handler ---> start                     .
 *      .                        .                       .
 *      .                        .                       .
 *      .                        .                     server class loaded
 *      .                      prepared-event < ---------.
 *  serverClassPrepared() <------.
 *    set break in remote VM
 *    continue remote VM
 *    wait
 *      .                        .
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

    // options for the remote virtual machine
    static final String VM_OPTIONS = "-classic";

    // the field name of the static field within that class that hold the
    // server object
    static final String SERVER_FIELD_NAME = "server";

    // the field name of the static field within that class that hold the
    // terminate exception object
    static final String TERMINATE_FIELD_NAME = "terminateExc";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "suspendExecution";

    // the name of the method called to signal the ExecServer to start a new
    // task
    static final String SERVER_PERFORM_METHOD_NAME = "performTask";


    // ==== instance data ====

    // The remote virtual machine used with this debugger
    private VirtualMachine machine = null;

    private Process process = null;
    private VMEventHandler eventHandler = null;

    private ReferenceType serverClass = null;  // the class of the exec server
    private ObjectReference execServer = null; // the exec server object
    private Method performTaskMethod = null;
    private ThreadReference serverThread = null;
    volatile private boolean initialised = false;
    private int machineStatus = IDLE;

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
        Connector.Argument optionsArg =
            (Connector.Argument)arguments.get("options");
        //Connector.Argument suspendArg =
        //    (Connector.Argument)arguments.get("suspend");

        if (mainArg == null || optionsArg == null) {
            Debug.reportError("Cannot start virtual machine.");
            Debug.reportError("(Incompatible launch connector)");
            return;
        }
        mainArg.setValue(SERVER_CLASSNAME);
        //suspendArg.setValue("false");

        try {
            machine = null;

            try {
                machine = connector.launch(arguments);
            }
            catch (VMStartException vmse) {
                Debug.reportError("Target VM failed to initialise with default arguments");
                Debug.reportError("Now trying to initialise VM with -classic option");
            }

            if (machine == null) {
                optionsArg.setValue(VM_OPTIONS);
                machine = connector.launch(arguments);
            }

            process = machine.process();
            redirectIOStream(process.getErrorStream(), System.out, false);
            redirectIOStream(process.getInputStream(),
                             Terminal.getTerminal().getOutputStream(), false);
            redirectIOStream(Terminal.getTerminal().getInputStream(),
                             process.getOutputStream(), false);

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

        // now wait until the machine really has started up. We will know that
        // it has when the first breakpoint is hit (see breakEvent).
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
        try {
            if (machine != null) {

                try {
                    machine.dispose();
                }
                catch (Exception e) { }

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
     * This method is called by the VMEventHandler when the execution server
     * class (ExecServer) has been loaded into the VM. We use this to set
     * a breakpoint in the server class. This is really still part of the
     * initialisation process. This breakpoint is used to stop the server
     * process to make it wait for our task signals. (We later use the
     * suspended process to perform our task requests.)
     */
    void serverClassPrepared()
    {
        serverClass = findClassByName(machine, SERVER_CLASSNAME, null);
        Method suspendMethod = findMethodByName(serverClass,
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
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        bpreq.putProperty("isBluejBreak", "true");
        //bpreq.setSuspendPolicy(EventRequest.SUSPEND_NONE);
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
        ClassLoaderReference loader = (ClassLoaderReference)
            startServer(ExecServer.CREATE_LOADER, scopeId, classpath, "", "");

        return new JdiClassLoader(scopeId, loader);
    }

    /**
     * Remove a class loader
     */
    public void removeClassLoader(DebuggerClassLoader loader)
    {
        startServer(ExecServer.REMOVE_LOADER, loader.getId(), "", "", "");
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
            // exception thrown in remote machine - ignored here. The
            // exception is handled through the exceptionEvent method
        }
        catch(Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            exitStatus = EXCEPTION;
            lastException = new ExceptionDescription(
                                   "Internal BlueJ error!",
                                   "Cannot execute remote command",
                                   null, 0);
        }
        machineStatus = IDLE;
        //executionUserParam = null;
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
        startServer(ExecServer.REMOVE_OBJECT, scopeId, instanceName, "", "");
    }

    /**
     * Set the class path of the remote VM
     */
    public void setLibraries(String classpath)
    {
        startServer(ExecServer.SET_LIBRARIES, classpath, "", "", "");
    }

    /**
     * Set the remote "current directory" for relative file access.
     * Cannot be used currently because all the class loading goes wrong
     * once the directory gets changed. Someone needs to fix the class
     * loading first.
     */
    public void setDirectory(String path)
    {
        startServer(ExecServer.SET_DIRECTORY, path, "", "", "");
    }

    /**
     * Serialize an object in the debugger to a file
     */
    public void serializeObject(String scopeId, String instanceName,
                                String fileName)
    {
        startServer(ExecServer.SERIALIZE_OBJECT, scopeId, instanceName, fileName, "");
    }

    /**
     * Deserialize an object in the debugger from a file
     */
    public DebuggerObject deserializeObject(String loaderId, String scopeId,
                                            String newInstanceName, String fileName)
    {
        ObjectReference objRef = (ObjectReference)
            startServer(ExecServer.DESERIALIZE_OBJECT, loaderId, scopeId, newInstanceName, fileName);

        if (objRef == null)
            return null;

        return JdiObject.getDebuggerObject(objRef);
    }

    /**
     * Dispose all top level windows in the remote machine.
     */
    public void disposeWindows()
    {
        startServer(ExecServer.DISPOSE_WINDOWS, "", "", "", "");
    }

    /**
     * Supress error output on the remote machine.
     */
    public void supressErrorOutput()
    {
        startServer(ExecServer.SUPRESS_OUTPUT, "", "", "", "");
    }

    /**
     * Restore error output on the remote machine.
     */
    public void restoreErrorOutput()
    {
        startServer(ExecServer.RESTORE_OUTPUT, "", "", "", "");
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
    private Value startServer(int task, String arg1,
                              String arg2, String arg3, String arg4)
    {
        VirtualMachine vm = getVM();

        if(execServer == null) {
            if(!setupServerConnection(vm))
                return null;
        }

        // if the VM crashes then many of these methods may fail. Our
        // catch all exception will grab them all allowing our local
        // VM to struggle on without the remote VM (previously, we could
        // not quit the local VM once the remote VM had crashed)

     	try {
            List arguments = new ArrayList(5);
            arguments.add(vm.mirrorOf(task));
            arguments.add(vm.mirrorOf(arg1));
            arguments.add(vm.mirrorOf(arg2));
            arguments.add(vm.mirrorOf(arg3));
            arguments.add(vm.mirrorOf(arg4));

      	    Value returnVal = execServer.invokeMethod(serverThread,
                                                      performTaskMethod,
                                                      arguments, 0);
            // invokeMethod leaves everything suspended, so restart the
            // system threads...
            resumeMachine();
            return returnVal;
        }
        catch(com.sun.jdi.InternalException e) {
            // we regularly get an exception here when trying to load a class
            // while the machine is suspended. It doesn't seem to be fatal.
            // so we just ignore internal exceptions for the moment.
      	}
        catch(Exception e) {
            Debug.message("sending command to remote VM failed: " + e);
            Debug.message("task: " + task + " " + arg1 + " " + arg2);
      	}
        return null;
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

        Field serverField = serverClass.fieldByName(SERVER_FIELD_NAME);
        execServer = (ObjectReference)serverClass.getValue(serverField);

        if(execServer == null) {
            sleep(3000);
            execServer = (ObjectReference)serverClass.getValue(serverField);
        }
        if(execServer == null) {
            Debug.reportError("Failed to load VM server object");
            Debug.reportError("Fatal: User code execution will not work");
            return false;
        }

        Field excField = serverClass.fieldByName(TERMINATE_FIELD_NAME);
        ObjectReference terminateException =
            (ObjectReference)serverClass.getValue(excField);
        JdiThread.setTerminateException(terminateException);

        // okay, we have the server object; now get the perform method

        performTaskMethod = findMethodByName(serverClass,
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
            exitStatus = TERMINATED;
            machineStatus = IDLE;
            lastException = null;
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
        //    val = (StringReference)execServer.invokeMethod(serverThread,
        //  						getMessageMethod,
        //  						null, 0);
        //} catch(Exception e) {
        //    Debug.reportError("Problem getting exception message: " + e);
        //}

        String exceptionText =
            (val == null ? null : val.value());

        if(excClass.equals("bluej.runtime.ExitException")) {
        Debug.message("exit!!!");
            // this was a "System.exit()", not a real exception!
            exitStatus = FORCED_EXIT;
            machineStatus = IDLE;
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
        ClassType remoteClass = findClassByName(vm, className, loader);

        if(remoteClass == null)
            return "Class not found";

        try {
            Location loc = findLocationInLine(remoteClass, line);
            if(loc == null)
                return Config.getString("debugger.jdiDebugger.noCodeMsg");

            EventRequestManager erm = vm.eventRequestManager();
            if(set) {
                BreakpointRequest bpreq = erm.createBreakpointRequest(loc);
                bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
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
                return Config.getString("debugger.jdiDebugger.noBreakpointMsg");
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
     * List all the threads being debugged as a Vector containing elements
     * of type DebuggerThread. Filter out threads that belong to system,
     * returning only user threads. This can be done only if the machine
     * is currently suspended.
     *
     * @return  A vector of threads (type JdiThread), or null if the machine
     *		is currently running
     */
    public Vector listThreads()
    {
        List threads = getVM().allThreads();
        int len = threads.size();

        Vector threadVec = new Vector();

        // reverse order to make display nicer (newer threads first)
        for(int i = 0; i < len; i++) {
            ThreadReference thread = (ThreadReference)threads.get(len-i-1);
            String name = thread.name();
            threadVec.addElement(new JdiThread(thread));
        }
        return threadVec;
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
        supressErrorOutput();
        thread.terminate();
        disposeWindows();
        restoreErrorOutput();
        exitStatus = TERMINATED;
        machineStatus = IDLE;
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_FINISHED, null);
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
        if(serverThreadIdle())
            serverThread.suspend();
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
     *  Find the mirror of a class in the remote VM. The class is expected
     *  to exist. We expect only one single class to exist with this name
     *  and report an error if more than one is found.
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
    private void redirectIOStream(final InputStream inStream,
                                  final OutputStream outStream,
                                  boolean buffered)
    {
        Thread thr;

        if(buffered) {
            thr = new Thread("I/O reader") {
                    public void run() {
                        try {
                            dumpStreamBuffered(inStream, outStream);
                        } catch (IOException ex) {
                            Debug.reportError("Cannot read output user VM.");
                        }
                    }
                };
        }
        else {
            thr = new Thread("I/O reader") {
                    public void run() {
                        try {
                            dumpStream(inStream, outStream);
                        } catch (IOException ex) {
                            Debug.reportError("Cannot read output user VM.");
                        }
                    }
                };
        }
        thr.setPriority(Thread.MAX_PRIORITY-1);
        thr.start();
    }

    private void dumpStream(InputStream inStream, OutputStream outStream)
        throws IOException
    {
        int ch;
        while ((ch = inStream.read()) != -1) {
            outStream.write(ch);
            outStream.flush();
        }
    }

    private void dumpStreamBuffered(InputStream inStream,
                                    OutputStream outStream)
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

    public void dumpThreadInfo()
    {
        Debug.message("threads:");
        Debug.message("--------");

        Vector threads = listThreads();
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

}

