/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugger.jdi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.debugger.Debugger.EventHandlerRunnable;
import bluej.debugger.RunOnThread;
import bluej.utility.DialogManager;
import bluej.utility.javafx.FXPlatformSupplier;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Boot;
import bluej.Config;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerEvent.BreakpointProperties;
import bluej.debugger.DebuggerResult;
import bluej.debugger.DebuggerTerminal;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.prefmgr.PrefMgr;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.Utility;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassObjectReference;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VMMismatchException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import javafx.application.Platform;

/**
 * A class implementing the execution and debugging primitives needed by BlueJ.
 * 
 * <p>Execution and debugging is implemented here on a second ("remote") virtual
 * machine, which gets started from here via the JDI interface.
 * 
 * <p>The startup process is as follows:
 * 
 * <ol>
 * <li>Debugger spawns a MachineLoaderThread which begins to load the debug vm
 *    Any access to the debugger during this time uses getVM() which waits
 *    for the machine to be loaded.
 *    (see JdiDebugger.MachineLoaderThread).
 * <li>The MachineLoaderThread creates a VMReference representing the vm. The
 *    VMReference in turn creates a VMEventHandler to receive events from the
 *    debug VM.
 * <li>A "ClassPrepared" event is received telling BlueJ that the ExecServer
 *    class has been loaded. At this point, breakpoints are set in certain
 *    places within the server class. Execution in the debug VM continues.
 * <li>The ExecServer "main" method spawns two threads. One is the "server"
 *    thread used to run user code. The "worker" thread is used for helper
 *    functions which do not execute user code paths. Both threads hit the
 *    breakpoints which have been set. This causes a breakpoint event to occur.
 * <li>The breakpoint events are trapped. When the server thread hits the
 *    "vmStarted" breakpoint, the VM is considered to be started.
 * </ol>
 * 
 * <p>We can now execute commands on the remote VM by invoking methods using the
 * server thread (which is suspended at the breakpoint). 
 * 
 * <p>Non-user code used by BlueJ is run a separate "worker" thread.
 * 
 * @author Michael Kolling
 */
@OnThread(Tag.Any)
public class VMReference
{
    // the class name of the execution server class running on the remote VM
    static final String SERVER_CLASSNAME = ExecServer.class.getName();

    // the name of the method used to suspend the ExecServer
    static final String SERVER_STARTED_METHOD_NAME = "vmStarted";

    // the name of the method used to suspend the ExecServer
    static final String SERVER_SUSPEND_METHOD_NAME = "vmSuspend";

    // the name of the method used to show  the terminal on input
    public static final String SERVER_SHOW_TERMINAL_ON_INPUT_NAME = "showTerminalOnInput";

    // A map which can be used to map instances of VirtualMachine to VMReference 
    private static Map<VirtualMachine, VMReference> vmToReferenceMap = new HashMap<VirtualMachine, VMReference>();
    
    // ==== instance data ====

    // we have a tight coupling between us and the JdiDebugger
    // that creates us
    private JdiDebugger owner = null;
    private DebuggerTerminal term;
    // The remote virtual machine and process we are referring to
    private VirtualMachine machine = null;

    // The handler for virtual machine events
    private VMEventHandler eventHandler = null;

    // the class reference to ExecServer
    private ClassType serverClass = null;

    // the thread running inside the ExecServer
    private JdiThread serverThread = null;
    private boolean serverThreadStarted = false;

    // the worker thread running inside the ExecServer
    private ThreadReference workerThread = null;
    private boolean workerThreadReady = false;
    private boolean workerThreadReserved = false;

    // a record of the threads we start up for
    // redirecting ExecServer streams
    @OnThread(Tag.Any)
    private IOHandlerThread inputStreamRedirector = null;
    @OnThread(Tag.Any)
    private IOHandlerThread outputStreamRedirector = null;
    @OnThread(Tag.Any)
    private IOHandlerThread errorStreamRedirector = null;

    // the current class loader in the ExecServer
    private ClassLoaderReference currentLoader = null;

    private int exitStatus;
    @OnThread(Tag.Any)
    private ExceptionDescription lastException;

    // Boolean flag indicating if the VM is being closed by BlueJ internally.
    private boolean queuedForClose = false;

    /**
     * Launch a remote debug VM using a TCP/IP socket.
     * 
     * @param initDir
     *            the directory to have as a current directory in the remote VM
     * @param libraries
     *            libraries to be added to the VM startup classpath
     * @param mgr
     *            the virtual machine manager
     * @return an instance of a VirtualMachine or null if there was an error
     */
    @OnThread(Tag.Any)
    public VirtualMachine localhostSocketLaunch(File initDir, URL[] libraries, DebuggerTerminal term,
            VirtualMachineManager mgr)
    {
        final int CONNECT_TRIES = 1; // try to connect max of 5 times
        final int CONNECT_WAIT = 500; // wait half a sec between each connect

        String [] launchParams;

        // launch the VM using the runtime classpath.
        Boot boot = Boot.getInstance();
        List<File> filesPath = Utility.urlsToFiles(boot.getRuntimeUserClassPath());
        List<File> javafxPath = Utility.urlsToFiles(boot.getJavaFXClassPath());
        List<File> libraryPaths = Utility.urlsToFiles(libraries);
        List<File> classPath = new ArrayList<>();
        classPath.addAll(filesPath);
        classPath.addAll(javafxPath);
        classPath.addAll(libraryPaths);
        String allClassPath = Utility.toClasspathString(classPath);
        
        ArrayList<String> paramList = new ArrayList<String>(11);
        
        /* // Uncomment this if you want to get a command window showing
           // for the debug VM on Windows.  Useful to let you hit Ctrl+Break and see thread dump
           // in case of deadlock
        paramList.add("cmd.exe");
        paramList.add("/C");
        paramList.add("start");
        paramList.add("cmd.exe");
        paramList.add("/K");
        */
        paramList.add(Config.getJDKExecutablePath(null, "java"));
        
        //check if any vm args are specified in Config, at the moment these
        //are only Locale options: user.language and user.country
        
        paramList.addAll(Config.getDebugVMArgs());
        
        paramList.add("-classpath");
        paramList.add(allClassPath);
        if (Config.isMacOS()) {
            paramList.add("-Xdock:icon=" + Config.getBlueJIconPath() + "/" + Config.getVMIconsName());
            paramList.add("-Xdock:name=" + Config.getVMDockName());
        }

        // Index for where the transport parameter is to be added
        int transportIndex = paramList.size();

        String streamEncoding = Config.getPropString("bluej.terminal.encoding", null);
        if (streamEncoding != null) {
            // Set the input/output encoding to the same as the terminal encoding, to avoid confusion
            // that mismatching these two causes. See bug #509.
            paramList.add("-Dfile.encoding=" + streamEncoding);
        }
        
        paramList.add(SERVER_CLASSNAME);
        
        // set output encoding if specified, default is to use system default
        // this gets passed to ExecServer's main as an arg which can then be 
        // used to specify encoding
        if(streamEncoding != null) {
            paramList.add(streamEncoding);
        }
        
        String transport = Config.getPropString("bluej.vm.transport", "dt_socket");
        
        List<ListeningConnector> connectors = new ArrayList<ListeningConnector>(mgr.listeningConnectors());

        // find the known connectors - order them by preference:
        Iterator<ListeningConnector> it = connectors.iterator();
        while (it.hasNext()) {
            ListeningConnector c = it.next();
            if (c.transport().name().equals(transport)) {
                // We've found the preferred connector
                it.remove();
                connectors.add(0, c);
                break;
            }
        }
        
        Throwable [] failureReasons = new Throwable[connectors.size()];
        
        for (int i = 0; i < CONNECT_TRIES; i++) {
            for (int j = 0; j < connectors.size(); j++) {
                ListeningConnector connector = connectors.get(j);
                try {
                    // Set up connection arguments
                    Map<String, Argument> arguments = connector.defaultArguments();
                    Connector.Argument timeoutArg = arguments.get("timeout");
                    if (timeoutArg != null) {
                        // The timeout appears to be in milliseconds.
                        // The default is apparently no timeout.
                        String timeOutVal = Config.getPropString("bluej.vm.connect.timeout", "10000");
                        timeoutArg.setValue(timeOutVal);
                    }
                    
                    // Make sure the local address is localhost, not the
                    // machine name, as using the machine name causes problems on some systems
                    // when the network is disconnected (because the machine name binds to
                    // the network IP, not to localhost):
                    String listenAddress = null;
                    if (connector.transport().name().equals("dt_socket") && arguments.containsKey("localAddress"))
                    {
                        listenAddress = InetAddress.getByName(null).getHostAddress();
                        arguments.get("localAddress").setValue(listenAddress);
                    }
                    
                    // Listening connectors can only listen on one address at a time -
                    // Synchronize to prevent problems.
                    synchronized (connector) {
                        String address = connector.startListening(arguments);
                        if (listenAddress != null) {
                            // It seems the address name returned by connector.startListening(...) may be the host name,
                            // even though we specifically asked for localhost. So here we'll force it to the localhost
                            // IP address:
                            int colonIndex = address.lastIndexOf(':');
                            if (colonIndex != -1) {
                                address = listenAddress + address.substring(colonIndex);
                            }
                        }
                        Debug.log("" + System.currentTimeMillis() + ": Listening for JDWP connection on address: " + address);
                        paramList.add(transportIndex, "-agentlib:jdwp=transport=" + connector.transport().name()
                                + ",address=" + address);
                        launchParams = paramList.toArray(new String[paramList.size()]);
                        paramList.remove(transportIndex);

                        final Process remoteVMprocess;
                        try {
                            remoteVMprocess = launchVM(initDir, launchParams);
                        }
                        catch (Throwable t) {
                            connector.stopListening(arguments);
                            throw t;
                        }

                        try {
                            machine = connector.accept(arguments);
                            redirectToTerminal(term, remoteVMprocess, streamEncoding);
                        }
                        catch (Throwable t) {
                            // failed to connect.
                            closeIO();
                            try {
                                // Ask for the exit value, since that allows us to test
                                // whether the process has already exited.
                                int exitCode = remoteVMprocess.exitValue();
                                Debug.log("" + System.currentTimeMillis() + ": remote VM process has prematurely terminated with exit code: " + exitCode);
                                drainOutput(remoteVMprocess);
                            }
                            catch (IllegalThreadStateException itse) {}
                            remoteVMprocess.destroy();
                            throw t;
                        }
                        finally {
                            connector.stopListening(arguments);
                        }
                    }
                    
                    Debug.log("Connected to debug VM via " + connector.transport().name() + " transport...");
                    setupEventHandling();
                    if (waitForStartup()) {
                        Debug.log("Communication with debug VM fully established.");
                        return machine;
                    }
                    else {
                        Debug.log("Error: Debug VM not signalling startup.");
                    }
                }
                catch(Throwable t) {
                    failureReasons[j] = t;
                }
            }
            
            // Do a small wait between connection attempts
            try {
                if (i != CONNECT_TRIES - 1) {
                    Thread.sleep(CONNECT_WAIT);
                }
            }
            catch (InterruptedException ie) { break; }
        }

        // failed to connect
        Writer dbgStream = Debug.getDebugStream();
        synchronized (dbgStream) {
            Debug.message("" + System.currentTimeMillis() + ": Failed to connect to debug VM. Reasons follow:");
            for (int i = 0; i < connectors.size(); i++) {
                Debug.message(connectors.get(i).transport().name() + " transport:");
                PrintWriter pw = new PrintWriter(dbgStream);
                failureReasons[i].printStackTrace(pw);
                pw.flush();
            }
        }

        NetworkTest.doTest();
        
        return null;
    }
    
    /**
     * Read and log anything that the remote VM process output before it died.
     */
    private void drainOutput(Process remoteVMprocess)
    {
        InputStreamReader stdout = new InputStreamReader(remoteVMprocess.getInputStream());
        char charBuf[] = new char[2048];
        
        try {
            int numRead = stdout.read(charBuf);
            if (numRead != -1) {
                String output = new String(charBuf, 0, numRead);
                Debug.message("Output from remote process stdout: " + output);
            }
            
            InputStreamReader stderr = new InputStreamReader(remoteVMprocess.getErrorStream());
            numRead = stderr.read(charBuf);
            if (numRead != -1) {
                String output = new String(charBuf, 0, numRead);
                Debug.message("Output from remote process stderr: " + output);
            }
        }
        catch (IOException ioe) {
            Debug.message("IOException while trying to draing stdout/stderr of remote process: " + ioe.getMessage());
        }
    }
    
    private void setupEventHandling()
    {
        // indicate the events we want to receive
        EventRequestManager erm = machine.eventRequestManager();
        erm.createExceptionRequest(null, false, true).enable();
        erm.createClassPrepareRequest().enable();
        
        EventRequest tsr = erm.createThreadStartRequest();
        tsr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        tsr.enable();
        
        tsr = erm.createThreadDeathRequest();
        tsr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        tsr.enable();

        // start the VM event handler (will handle the VMStartEvent
        // which will set the machine running)
        eventHandler = new VMEventHandler(this, machine);
    }

    /**
     * Launch the debug VM and set up the I/O connectors to the terminal.
     * @param initDir   the directory which the vm should be started in
     * @param params    the parameters (including executable as first param)
     */
    @OnThread(Tag.Any)
    private Process launchVM(File initDir, String [] params)
        throws IOException
    {    
        Process vmProcess = Runtime.getRuntime().exec(params, null, initDir);
        BufferedReader bro = new BufferedReader(new InputStreamReader(vmProcess.getInputStream()));
        BufferedReader bre = new BufferedReader(new InputStreamReader(vmProcess.getErrorStream()));
        
        // grab anything else the VM spits out before we try to connect to it.
        try {
            
            StringBuffer extraOut = new StringBuffer();
            StringBuffer extraErr = new StringBuffer();
            // Two streams to check: standard output and standard error
                
            char [] buf = new char[1024];
            Thread.sleep(200); // A little extra time for initial output
            for (int i = 0; i < 5; i++) {
                Thread.sleep(200);
                
                // discontinue if no data available or stream closed
                boolean keepReading = false;
                if (bro.ready()) {
                    int len = bro.read(buf);
                    if (len != -1) {
                        extraOut.append(buf, 0, len);
                    }
                    keepReading = true;
                }
                if (bre.ready()) {
                    int len = bre.read(buf);
                    if (len != -1) {
                        extraErr.append(buf, 0, len);
                    }
                    keepReading = true;
                }
                
                if (! keepReading) {
                    break;
                }
            }
            if (extraOut.length() != 0) {
                Debug.message("Extra output from debug VM on launch:" + extraOut);
            }
            if (extraErr.length() != 0) {
                Debug.message("Error output from debug VM on launch:" + extraErr);
            }
        }
        catch (InterruptedException ie) {}
        
        
        return vmProcess;
    }
    
    /**
     * Redirect input, output and error streams of the remote process to the terminal.
     */
    @OnThread(Tag.Any)
    private void redirectToTerminal(DebuggerTerminal term, Process vmProcess, String streamEncoding) throws UnsupportedEncodingException
    {
        // redirect standard streams from process to Terminal
        // error stream System.err
        Reader errorReader = null;
        // output stream System.out
        Reader outReader = null;
        // input stream System.in
        Writer inputWriter = null;
        
        if(streamEncoding == null) {
            errorReader = new InputStreamReader(vmProcess.getErrorStream());
            outReader = new InputStreamReader(vmProcess.getInputStream());
            inputWriter = new OutputStreamWriter(vmProcess.getOutputStream());            
        }
        // if specified in bluej.defs
        else {
            errorReader = new InputStreamReader(vmProcess.getErrorStream(), streamEncoding); 
            outReader = new InputStreamReader(vmProcess.getInputStream(), streamEncoding);
            inputWriter = new OutputStreamWriter(vmProcess.getOutputStream(), streamEncoding);
        }
        
        errorStreamRedirector = redirectIOStream(errorReader, term.getErrorWriter());
        outputStreamRedirector = redirectIOStream(outReader, term.getWriter());
        inputStreamRedirector = redirectIOStream(term.getReader(), inputWriter);
    }

    /**
     * Create the second virtual machine and start the execution server (class
     * ExecServer) on that machine.
     */
    @OnThread(Tag.Any)
    public VMReference(JdiDebugger owner, DebuggerTerminal term, File initialDirectory, URL[] libraries)
        throws JdiVmCreationException
    {
        this.owner = owner;
        this.term = term;
        
        // machine will be suspended at startup
        machine = localhostSocketLaunch(initialDirectory, libraries, term, Bootstrap.virtualMachineManager());
        if (machine == null) {
            throw new JdiVmCreationException();
        }
        
        // Add our machine into the map
        vmToReferenceMap.put(machine, this);
    }

    /**
     * Wait for all our virtual machine initialisation to occur.
     */
    public synchronized boolean waitForStartup()
    {
        serverThreadStartWait();
        
        if (! setupServerConnection(machine)) {
            return false;
        }
        
        return true;
    }

    /**
     * Close down this virtual machine.
     */
    @OnThread(Tag.Any)
    public synchronized void close()
    {
        if (machine != null) {
            queuedForClose = true;
            closeIO();
            // cause the debug VM to exit when disposed
            try {
                setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.EXIT_VM));
                machine.dispose();
            }
            catch(VMDisconnectedException vmde) {}
        }
    }

    /**
     * Close I/O redirectors.
     */
    public void closeIO()
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
    }

    /**
     * This method is called by the VMEventHandler when the execution server
     * class (ExecServer) has been loaded into the VM. We use this to set a
     * breakpoint in the server class. This is really still part of the
     * initialisation process.
     */
    void serverClassPrepared()
    {
        // remove the "class prepare" event request (not needed anymore)

        EventRequestManager erm = machine.eventRequestManager();
        List<ClassPrepareRequest> list = erm.classPrepareRequests();
        erm.deleteEventRequests(list);

        try {
            serverClass = (ClassType) findClassByName(SERVER_CLASSNAME, null);
        }
        catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException("can't find class " + SERVER_CLASSNAME + " in debug virtual machine");
        }

        // add the breakpoints (these may be cleared later on and so will
        // need to be readded)
        serverClassAddBreakpoints();
    }
    
    private Location findMethodLocation(ReferenceType classType, String methodName)
    {
        Method method = findMethodByName(classType, methodName);
        if (method == null) {
            throw new IllegalStateException("can't find method " + classType.name() + "."
                    + methodName);
        }
        return method.location();
    }

    /**
     * This breakpoint is used to stop the server process to make it wait for
     * our task signals. (We later use the suspended process to perform our task
     * requests.)
     */
    private void serverClassAddBreakpoints()
    {
        EventRequestManager erm = machine.eventRequestManager();

        // set a breakpoint in the vm started method
        {
            BreakpointRequest serverBreakpoint = erm.createBreakpointRequest(findMethodLocation(serverClass, SERVER_STARTED_METHOD_NAME));
            serverBreakpoint.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            // the presence of this property indicates to breakEvent that we are
            // a special type of breakpoint
            serverBreakpoint.putProperty(SERVER_STARTED_METHOD_NAME, "yes");
            serverBreakpoint.putProperty(VMEventHandler.DONT_RESUME, "yes");
            serverBreakpoint.putProperty(Debugger.PERSIST_BREAKPOINT_PROPERTY, "yes");
            serverBreakpoint.enable();
        }

        // set a breakpoint in the suspend method
        {
            BreakpointRequest workerBreakpoint = erm.createBreakpointRequest(findMethodLocation(serverClass, SERVER_SUSPEND_METHOD_NAME));
            workerBreakpoint.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
            // the presence of this property indicates to breakEvent that we are
            // a special type of breakpoint
            workerBreakpoint.putProperty(SERVER_SUSPEND_METHOD_NAME, "yes");
            // the presence of this property indicates that we should not
            // be restarted after receiving this event
            workerBreakpoint.putProperty(VMEventHandler.DONT_RESUME, "yes");
            workerBreakpoint.putProperty(Debugger.PERSIST_BREAKPOINT_PROPERTY, "yes");
            workerBreakpoint.enable();
        }

        // set a breakpoint in the showTerminaOnInput method
        {
            BreakpointRequest serverBreakpoint = erm.createBreakpointRequest(findMethodLocation(serverClass, SERVER_SHOW_TERMINAL_ON_INPUT_NAME));
            serverBreakpoint.setSuspendPolicy(EventRequest.SUSPEND_NONE);
            // the presence of this property indicates to breakEvent that we are
            // a special type of breakpoint
            serverBreakpoint.putProperty(SERVER_SHOW_TERMINAL_ON_INPUT_NAME, "yes");
            serverBreakpoint.putProperty(Debugger.PERSIST_BREAKPOINT_PROPERTY, "yes");
            serverBreakpoint.enable();
        }

    }

    /**
     * Find the components on the remote VM that we need to talk to it: the
     * execServer object, the performTaskMethod, and the serverThread. These
     * three variables (mirrors to the remote entities) are set up here. This
     * needs to be done only once.
     */
    private boolean setupServerConnection(VirtualMachine vm)
    {
        if (serverClass == null) {
            Debug.reportError("server class not initialised!");
            return false;
        }

        // get our main server thread
        // serverThread = (ThreadReference) getStaticFieldObject(serverClass, ExecServer.MAIN_THREAD_NAME);

        // get our worker thread
        workerThread = (ThreadReference) getStaticFieldObject(serverClass, ExecServer.WORKER_THREAD_NAME);

        if (serverThread == null || workerThread == null) {
            Debug.reportError("Cannot find fields on remote VM");
            return false;
        }

        //Debug.message(" connection to remote VM established");
        return true;
    }

    // -- all methods below here are for after the VM has started up

    /**
     * Instruct the remote machine to construct a new class loader and return its
     * reference.
     * 
     * May throw VMDisconnectedException.
     * 
     * @param urls  the classpath as an array of URL
     */
    @OnThread(Tag.Any)
    ClassLoaderReference newClassLoader(URL [] urls)
    {
        synchronized(workerThread) {
            workerThreadReadyWait();
            workerThreadReserved = true;
            setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.NEW_LOADER));
            
            StringBuffer newcpath = new StringBuffer(200);
            for (int index = 0; index < urls.length; index++) {
                newcpath.append ( urls[index].toString());
                newcpath.append ('\n');
            }
            
            setStaticFieldObject(serverClass, ExecServer.CLASSPATH_NAME, newcpath.toString());
            
            workerThreadReady = false;
            workerThread.resume();
            workerThreadFinishWait();
            
            currentLoader = (ClassLoaderReference) getStaticFieldObject(serverClass, ExecServer.WORKER_RETURN_NAME);
            workerThreadReserved = false;
            workerThread.notify();
            
            return currentLoader;
        }
    }
    
    /**
     * Get an ObjectReference mirroring a String. May throw
     * VMDisconnectedException, VMOutOfMemoryException.
     * 
     * @param value  The string to mirror on the remote VM.
     * @return       The mirror object
     */
    public StringReference getMirror(String value)
    {
        return machine.mirrorOf(value);
    }
    
    /**
     * Load a class in the remote machine and return its reference. Note that
     * this function never returns null.
     * 
     * @return a Reference to the class mirrored in the remote VM
     * @throws ClassNotFoundException  if the remote class can't be loaded
     */
    @OnThread(Tag.NOTVMEventHandler)
    ReferenceType loadClass(String className)
        throws ClassNotFoundException
    {
        ReferenceType rt = loadClass(className, null);
        if (rt == null) {
            throw new ClassNotFoundException(className);
        }
        return rt;
    }
    
    /**
     * Load a class in the remote VM using the given class loader.
     * @param className  The name of the class to load
     * @param clr        The remote classloader reference to use, or null to use
     *                   the current established project classloader
     * @return     A reference to the loaded class, or null if the class could not be loaded.
     */
    @OnThread(Tag.NOTVMEventHandler)
    ReferenceType loadClass(String className, ClassLoaderReference clr)
    {
        synchronized(workerThread) {
            workerThreadReadyWait();
            workerThreadReserved = true;
            setStaticFieldValue(serverClass, ExecServer.CLASSLOADER_NAME, clr);
            setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.LOAD_CLASS));
            setStaticFieldObject(serverClass, ExecServer.CLASSNAME_NAME, className);
            
            workerThreadReady = false;
            workerThread.resume();
            workerThreadFinishWait();
            
            ClassObjectReference robject = (ClassObjectReference) getStaticFieldObject(serverClass, ExecServer.WORKER_RETURN_NAME);
            workerThreadReserved = false;
            workerThread.notify();
            
            if (robject == null) {
                return null;
            }
            
            return robject.reflectedType();
        }
    }
    
    /**
     * Load and initialize a class in the remote machine, and return a reference to it.
     * Initialization causes static initializer assignments and blocks to be executed in
     * the remote machine. This method will not return until all such blocks have completed
     * execution.
     * 
     * @param className  The name of the class to load
     * @return           A reference to the class
     * @throws ClassNotFoundException  If the class could not be found
     */
    ReferenceType loadInitClass(String className)
        throws ClassNotFoundException
    {
        try {
            serverThreadStartWait();
            
            // Store the class and method to call
            setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, className);
            setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.LOAD_INIT_CLASS));
            
            // Resume the thread, wait for it to finish and the new thread to start
            serverThreadStarted = false;
            resumeServerThread();
            serverThreadStartWait();
            
            // Get return value
            ClassObjectReference rval = (ClassObjectReference) getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
            if (rval == null)
                throw new ClassNotFoundException("Remote class not found: " + className);
            
            // check for and report exceptions which occurred during initialization
            ObjectReference exception = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
            if (exception != null) {
                exceptionEvent(new InvocationException(exception));
            }
            
            return rval.reflectedType();
        }
        catch (VMDisconnectedException vde) {
            throw new ClassNotFoundException("Remote class not loaded due to VM termination.");
        }
    }

    /**
     * "Start" a class (i.e. invoke its main method)
     * 
     * @param className
     *            the class to start
     */
    @OnThread(Tag.NOTVMEventHandler)
    public DebuggerResult runShellClass(String className)
    {
        // Calls to this method are protected by serverThreadLock in JdiDebugger
        
        // Debug.message("[VMRef] starting " + className);
        // ** call Shell.run() **
        try {
            exitStatus = Debugger.NORMAL_EXIT;

            serverThreadStartWait();
            
            // Store the class and method to call
            setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, className);
            setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.EXEC_SHELL));
            
            // Resume the thread, wait for it to finish and the new thread to start
            serverThreadStarted = false;
            resumeServerThread();
            serverThreadStartWait();
            
            // Get return value and check for exceptions
            ObjectReference rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
            if (rval == null) {
                ObjectReference exception = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
                if (exception != null) {
                    exceptionEvent(new InvocationException(exception));
                    return new DebuggerResult(lastException);
                }
            }
            
            ObjectReference objR = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
            return new DebuggerResult(JdiObject.getDebuggerObject(objR));
        }
        catch (VMDisconnectedException e) {
            exitStatus = getDebuggerExitStatus();
            return new DebuggerResult(exitStatus);
        }
        catch (Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            e.printStackTrace();
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription("Internal BlueJ error: unexpected exception in remote VM\n" + e);
        }
        
        return new DebuggerResult(lastException);
    }

    private int getDebuggerExitStatus()
    {
        return queuedForClose ? Debugger.TERMINATED_BY_BLUEJ : Debugger.TERMINATED_BY_USER_SYSTEM_EXIT;
    }

    /**
     * Invoke the default constructor for some class, and return the resulting object.
     */
    @OnThread(Tag.NOTVMEventHandler)
    public DebuggerResult instantiateClass(String className)
    {
        ObjectReference obj = null;
        exitStatus = Debugger.NORMAL_EXIT;
        try {
            obj = invokeConstructor(className);
        }
        catch (VMDisconnectedException e) {
            exitStatus = getDebuggerExitStatus();
            // return null; // debugger state change handled elsewhere
            return new DebuggerResult(exitStatus);
        }
        catch (Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            e.printStackTrace();
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription("Internal BlueJ error: unexpected exception in remote VM\n" + e);
        }
        if (obj == null) {
            return new DebuggerResult(lastException);
        }
        else {
            ObjectReference objFinal = obj;
            return new DebuggerResult(JdiObject.getDebuggerObject(objFinal));
        }
    }

    /**
     * Invoke a particular constructor with arguments. The parameter types
     * of the constructor must be supplied (String[]) as well as the
     * argument values (ObjectReference []).
     * 
     * @param className  The name of the class to construct an instance of
     * @param paramTypes The parameter types of the constructor (class names)
     * @param args      The argument values to use in the constructor call
     * 
     * @return  The newly constructed object (or null if error/exception
     *          occurs)
     */
    @OnThread(Tag.NOTVMEventHandler)
    public DebuggerResult instantiateClass(String className, String [] paramTypes, ObjectReference [] args)
    {
        ObjectReference obj = null;
        exitStatus = Debugger.NORMAL_EXIT;
        try {
            obj = invokeConstructor(className, paramTypes, args);
        }
        catch (VMDisconnectedException e) {
            exitStatus = getDebuggerExitStatus();
            return new DebuggerResult(exitStatus); // debugger state change handled elsewhere
        }
        catch (Exception e) {
            // remote invocation failed
            Debug.reportError("starting shell class failed: " + e);
            e.printStackTrace();
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription("Internal BlueJ error: unexpected exception in remote VM\n" + e);
        }
        if (obj == null) {
            return new DebuggerResult(lastException);
        }
        else {
            ObjectReference objFinal = obj;
            return new DebuggerResult(JdiObject.getDebuggerObject(objFinal));
        }
    }
    
    /**
     * Emit a thread halted/resumed event for the given thread.
     */
    @OnThread(Tag.Any)
    public void emitThreadEvent(JdiThread thread, boolean halted)
    {
        eventHandler.emitThreadEvent(thread, halted);
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
        serverThreadStarted = false;
    }

    /**
     * The VM has been disconnected or ended.
     */
    @OnThread(Tag.VMEventHandler)
    public void vmDisconnectEvent()
    {
        synchronized (this) {
            // Do the owner disconnect first, because it is synchronized on
            // JdiDebugger. This allows machine loader thread to check the exit
            // status in a meaningful way.
            owner.vmDisconnect();
            
            // If VM disconnect occurs during invocation, the server thread won't
            // restart in this VM; the method waiting for it to start will hang
            // indefinitely unless we kick it here.
            exitStatus = getDebuggerExitStatus();
            if (!serverThreadStarted) {
                notifyAll();
            }
        }
        
        if (workerThread != null) {
            synchronized (workerThread) {
                if (!workerThreadReady)
                    workerThread.notifyAll();
            }
        }

        synchronized (vmToReferenceMap) {
            vmToReferenceMap.remove(machine);
        }
    }

    /**
     * A thread has started.
     */
    @OnThread(Tag.VMEventHandler)
    public void threadStartEvent(ThreadStartEvent tse)
    {
        owner.threadStart(tse.thread());
    }

    /**
     * A thread has died.
     */
    @OnThread(Tag.VMEventHandler)
    public void threadDeathEvent(ThreadDeathEvent tde)
    {
        ThreadReference tr = tde.thread();
        owner.threadDeath(tr);

        // There appears to be a VM bug related to system.exit() being called
        // in an invocation thread. The event is only seen as a thread death.
        // Only affects some platforms/vm versions some of the time.
        //if (tr == serverThread && serverThreadStarted || tr == workerThread)
        //    close();
    }
    
    /**
     * A thread has been suspended (due to a breakpoint, step, or
     * call to DebuggerThread.halt()).
     */
    @OnThread(Tag.VMEventHandler)
    public void threadHaltedEvent(JdiThread thread)
    {
        owner.threadHalted(thread);
    }
    
    /**
     * A thread has been resumed.
     */
    @OnThread(Tag.VMEventHandler)
    public void threadResumedEvent(JdiThread thread)
    {
        owner.threadResumed(thread);
    }

    /**
     * An exception has occurred in a thread.
     * 
     * It doesn't really make sense to do anything here. Any exception which occurs
     * in the primary execution thread does not come through here.
     */
    public void exceptionEvent(ExceptionEvent exc)
    {
        // ObjectReference remoteException = exc.exception();

        // get the exception text
        // attention: the following depends on the (undocumented) fact that
        // the internal exception message field is named "detailMessage".
        
        // Field msgField = remoteException.referenceType().fieldByName("detailMessage");
        // StringReference msgVal = (StringReference) remoteException.getValue(msgField);

        // String exceptionText = (msgVal == null ? null : msgVal.value());
        // String excClass = exc.exception().type().name();

        // List<SourceLocation> stack = JdiThread.getStack(exc.thread());
    }

    /**
     * Invoke an arbitrary method on an object, using the worker thread.
     * If the called method exits via an exception, this method returns null.
     * 
     * @param o     The object to invoke the method on
     * @param m     The method to invoke
     * @param args  The arguments to pass to the method (List of Values)
     * @return      The return Value from the method
     */
    private Value safeInvoke(ObjectReference o, Method m, List<? extends Value> args)
    {
        synchronized (workerThread) {
            workerThreadReadyWait();
            Value v = null;

            try {
                v = o.invokeMethod(workerThread, m, args, ObjectReference.INVOKE_SINGLE_THREADED);
            }
            catch (ClassNotLoadedException cnle) {}
            catch (InvalidTypeException ite) {}
            catch (IncompatibleThreadStateException itse) {}
            catch (InvocationException ie) {}

            return v;
        }
    }
    
    public void exceptionEvent(InvocationException exc)
    {
        List<Value> empty = new LinkedList<Value>();
        
        ObjectReference remoteException = exc.exception();
        Field msgField = remoteException.referenceType().fieldByName("detailMessage");
        StringReference msgVal = (StringReference) remoteException.getValue(msgField);
        String exceptionText = (msgVal == null ? null : msgVal.value());
        String excClass = exc.exception().type().name();
        
        ReferenceType remoteType = exc.exception().referenceType();
        List<Method> getStackTraceMethods = remoteType.methodsByName("getStackTrace");
        Method getStackTrace = (Method)getStackTraceMethods.get(0);
        ArrayReference stackValue = (ArrayReference)safeInvoke(exc.exception(),  getStackTrace, empty);
        
        ObjectReference [] stackt = (ObjectReference [])stackValue.getValues().toArray(new ObjectReference[0]);
        List<SourceLocation> stack = new LinkedList<SourceLocation>();
        
        // "stackt" is now an array of Values. Each Value represents a
        // "StackTraceElement" object.
        if (stackt.length > 0) {
            ReferenceType StackTraceElementType = (ReferenceType)stackt[0].type();
            Method getClassName = (Method)StackTraceElementType.methodsByName("getClassName").get(0);
            Method getFileName = (Method)StackTraceElementType.methodsByName("getFileName").get(0);
            Method getLineNum = (Method)StackTraceElementType.methodsByName("getLineNumber").get(0);
            Method getMethodName = (Method)StackTraceElementType.methodsByName("getMethodName").get(0);
            
            for(int i = 0; i < stackt.length; i++) {
                Value classNameV = safeInvoke(stackt[i], getClassName, empty);
                Value fileNameV = safeInvoke(stackt[i], getFileName, empty);
                Value methodNameV = safeInvoke(stackt[i], getMethodName, empty);
                Value lineNumV = safeInvoke(stackt[i], getLineNum, empty);
                
                String className = ((StringReference)classNameV).value();
                String fileName = null;
                if (fileNameV != null) {
                    fileName = ((StringReference)fileNameV).value();
                }
                String methodName = ((StringReference)methodNameV).value();
                int lineNumber = ((IntegerValue)lineNumV).value();
                stack.add(new SourceLocation(className,fileName,methodName,lineNumber));
            }
        }
        
        // stack is a list of SourceLocation (bluej.debugger.SourceLocation)
        
        exitStatus = Debugger.EXCEPTION;
        lastException = new ExceptionDescription(excClass, exceptionText, stack);
    }

    
    /**
     * A breakpoint has been hit or step completed in a thread.
     */
    @OnThread(Tag.VMEventHandler)
    public void breakpointEvent(LocatableEvent event, int debuggerEventType, boolean skipUpdate)
    {
        // if the breakpoint is marked as with the SERVER_STARTED property
        // then this is our own breakpoint that is used to detect when a new
        // server thread has started (which happens at startup, and when user
        // code completes execution).
        if (event.request().getProperty(SERVER_STARTED_METHOD_NAME) != null) {
            // wake up the waitForStartup() method
            synchronized (this) {
                serverThreadStarted = true;
                serverThread = owner.findThread(event.thread());
                owner.raiseStateChangeEvent(Debugger.IDLE);
                notifyAll();
            }
        }
        // if the breakpoint is marked with the SERVER_SUSPEND property
        // then it is the worker thread returning to its breakpoint
        // after completing some work. We want to leave it suspended here until
        // it is required to do more work.
        else if (event.request().getProperty(SERVER_SUSPEND_METHOD_NAME) != null) {

            if (workerThread == null) {
                workerThread = event.thread();
            }
            
            synchronized (workerThread) {
                workerThreadReady = true;
                workerThread.notifyAll();
            }
        }
        // if the breakpoint is marked with the SERVER_SHOW_TERMINAL_ON_INPUT_NAME
        //
        else if (event.request().getProperty(SERVER_SHOW_TERMINAL_ON_INPUT_NAME) != null) {
            this.term.showOnInput();
        }
        else {
            // breakpoint set by user in user code
            if (serverThread.sameThread(event.thread())) {
                owner.raiseStateChangeEvent(Debugger.SUSPENDED);
            }
            
            Location location = event.location();
            String className = location.declaringType().name();
            String fileName;
            try {
                fileName = location.sourceName();
            }
            catch (AbsentInformationException e) {
                fileName = null;
            }

            // A breakpoint in the shell class or a BlueJ runtime class means that
            // the user has stepped past the end of their own code
            if (fileName != null && fileName.startsWith("__SHELL")
                    || className != null && className.startsWith("bluej.runtime.")) {
                event.thread().resume();
                return;
            }

            // signal the breakpoint/step to the user
            owner.breakpoint(event.thread(), debuggerEventType, skipUpdate, makeBreakpointProperties(event.request()));
        }
    }

    private BreakpointProperties makeBreakpointProperties(final EventRequest request)
    {
        if (request == null)
            return null;
        else
            return new DebuggerEvent.BreakpointProperties() {
                @OnThread(Tag.Any)
                public Object get(Object key)
                {
                    return request.getProperty(key);
                }
            };
    }

    @OnThread(Tag.VMEventHandler)
    public boolean screenBreakpointEvent(LocatableEvent event, int debuggerEventType)
    {
        BreakpointProperties props = makeBreakpointProperties(event.request());
        for (String special : Arrays.asList(
                SERVER_STARTED_METHOD_NAME, 
                SERVER_SUSPEND_METHOD_NAME, 
                SERVER_SHOW_TERMINAL_ON_INPUT_NAME))
        {
            if (props.get(special) != null)
            {
                return true;
            }
        }
        return owner.screenBreakpoint(event.thread(), debuggerEventType, props);
    }

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Find and load all classes declared in the same source file as className
     * and then find the Location object for the source at the line 'line'.
     */
    @OnThread(Tag.FXPlatform)
    private Location loadClassesAndFindLine(String className, int line)
    {
        ReferenceType remoteClass = null;
        try {
            remoteClass = loadClass(className);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
        List<ReferenceType> allTypesInFile = new ArrayList<ReferenceType>();

        // find all ReferenceType's declared in this source file
        buildNestedTypes(remoteClass, allTypesInFile);

        Iterator<ReferenceType> it = allTypesInFile.iterator();
        while (it.hasNext()) {
            ReferenceType r = it.next();

            try {
                List<Location> list = r.locationsOfLine(line);
                if (list.size() > 0)
                    return (Location) list.get(0);
            }
            catch (AbsentInformationException aie) {}
        }
        return null;
    }

    /**
     * Recursively construct a list of all Types started with rootType and
     * including all its nested types.
     * 
     * @param rootType
     *            the root to start building at
     * @param l
     *            the List to add the reference types to
     */
    private void buildNestedTypes(ReferenceType rootType, List<ReferenceType> l)
    {
        try {
            synchronized(workerThread) {
                workerThreadReadyWait();
                workerThreadReserved = true;
                setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.LOAD_ALL));
                
                // parameters
                setStaticFieldObject(serverClass, ExecServer.CLASSNAME_NAME, rootType.name());
                
                workerThreadReady = false;
                workerThread.resume();
                
                workerThreadFinishWait();
                ObjectReference or = getStaticFieldObject(serverClass, ExecServer.WORKER_RETURN_NAME);
                workerThreadReserved = false;
                workerThread.notify();
                
                ArrayReference inners = (ArrayReference) or;
                Iterator<Value> i = inners.getValues().iterator();
                while (i.hasNext()) {
                    ClassObjectReference cor = (ClassObjectReference) i.next();
                    ReferenceType rt = cor.reflectedType();
                    if (rt.isPrepared()) {
                        l.add(rt);
                    }
                }
            }
        }
        catch (VMDisconnectedException vmde) {}
        catch (VMMismatchException vmmme) {}
    }

    /**
     * Set a breakpoint at a specified line in a class.
     * 
     * @param className
     *            The class in which to set the breakpoint.
     * @param line
     *            The line number of the breakpoint.
     * @param properties The collection of properties to set on the breakpoint.  Can be null.
     * @return null if there was no problem, or an error string
     */
    @OnThread(Tag.FXPlatform)
    String setBreakpoint(String className, int line, Map<String, String> properties)
    {
        Location location = loadClassesAndFindLine(className, line);
        if (location == null) {
            return Config.getString("debugger.jdiDebugger.noCodeMsg");
        }
        EventRequestManager erm = machine.eventRequestManager();
        BreakpointRequest bpreq = erm.createBreakpointRequest(location);
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        bpreq.putProperty(VMEventHandler.DONT_RESUME, "yes");
        if (properties != null) {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                bpreq.putProperty(property.getKey(), property.getValue());
            }
        }
        bpreq.enable();

        return null;
    }

    String setBreakpoint(ReferenceType classType, int line, Map<String, String> properties)
    {
        try {
            List<Location> locations = classType.locationsOfLine(line);
            if (locations.isEmpty()) {
                return Config.getString("debugger.jdiDebugger.noCodeMsg");
            }

            setBreakpoint(locations.get(0), properties);
            return null;
        }
        catch (AbsentInformationException aie) {
            return Config.getString("debugger.jdiDebugger.noCodeMsg");
        }
    }
    
    void setBreakpoint(Location location, Map<String,String> properties)
    {
        EventRequestManager erm = machine.eventRequestManager();
        BreakpointRequest bpreq = erm.createBreakpointRequest(location);
        bpreq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        bpreq.putProperty(VMEventHandler.DONT_RESUME, "yes");
        if (properties != null) {
            for (Map.Entry<String, String> property : properties.entrySet()) {
                bpreq.putProperty(property.getKey(), property.getValue());
            }
        }
        bpreq.enable();
    }
    
    // As above but sets the breakpoint on the first line of a given method
    @OnThread(Tag.FXPlatform)
    String setBreakpoint(String className, String methodName, Map<String, String> properties)
    {
        try {
            loadClass(className);
            ClassType classType = (ClassType)findClassByName(className);
            Location loc = findMethodLocation(classType, methodName);
            return setBreakpoint(className, loc.lineNumber(), properties);
        } catch (ClassNotFoundException e) {
            return "Could not find class: " + className; 
        }
    }
    
    String setBreakpoint(ReferenceType classType, String methodName, Map<String, String> properties)
    {
        Location loc = findMethodLocation(classType, methodName);
        setBreakpoint(loc, properties);
        return null;
    }    
    

    /**
     * Clear all the breakpoints at a specified line in a class.
     * 
     * @param className
     *            The class in which to clear the breakpoints.
     * @param line
     *            The line number of the breakpoint.
     * @return null if there was no problem, or an error string
     */
    @OnThread(Tag.FXPlatform)
    String clearBreakpoint(String className, int line)
    {
        Location location = loadClassesAndFindLine(className, line);
        if (location == null) {
            return Config.getString("debugger.jdiDebugger.noCodeMsg");
        }

        return clearBreakpoint(location);
    }
    
    // As above but clears the breakpoint in a given method (as set by the corresponding setBreakpoint method that takes a method name)
    String clearBreakpoint(String className, String methodName)
    {
        try {
            ClassType classType = (ClassType)findClassByName(className);
            Location loc = findMethodLocation(classType, methodName);
            return clearBreakpoint(loc);
        }  catch (ClassNotFoundException e) {
            return "Could not find class: " + className; 
        }
    }
    
    String clearBreakpoint(ReferenceType classType, String methodName)
    {
        Location loc = findMethodLocation(classType, methodName);
        return clearBreakpoint(loc);
    }
    
    String clearBreakpoint(Location location)
    {
        EventRequestManager erm = machine.eventRequestManager();
        boolean found = false;
        List<BreakpointRequest> list = erm.breakpointRequests();
        for (int i = 0; i < list.size(); i++) {
            BreakpointRequest bp = list.get(i);
            if (bp.location().equals(location)) {
                erm.deleteEventRequest(bp);
                found = true;
            }
        }
        // bp not found
        if (found) {
            return null;
        }
        else {
            return Config.getString("debugger.jdiDebugger.noBreakpointMsg");
        }
    }

    
    /**
     * Remove all user breakpoints
     */
    public void clearAllBreakpoints()
    {
        EventRequestManager erm = machine.eventRequestManager();
        List<BreakpointRequest> breaks = new LinkedList<BreakpointRequest>();

        List<BreakpointRequest> allBreakpoints = erm.breakpointRequests();
        Iterator<BreakpointRequest> it = allBreakpoints.iterator();

        while (it.hasNext()) {
            BreakpointRequest bp = (BreakpointRequest) it.next();
            if (bp.getProperty(Debugger.PERSIST_BREAKPOINT_PROPERTY) == null) {
                breaks.add(bp);
            }
        }

        erm.deleteEventRequests(breaks);
    }
    
    /**
     * Remove all breakpoints for the given class.
     */
    public void clearBreakpointsForClass(String className)
    {
        EventRequestManager erm = machine.eventRequestManager();

        List<BreakpointRequest> allBreakpoints = erm.breakpointRequests();
        Iterator<BreakpointRequest> it = allBreakpoints.iterator();
        List<BreakpointRequest> toDelete = new LinkedList<BreakpointRequest>();

        while (it.hasNext()) {
            BreakpointRequest bp = it.next();

            ReferenceType bpType = bp.location().declaringType();
            if (bpType.name().equals(className) && bp.getProperty(Debugger.PERSIST_BREAKPOINT_PROPERTY) == null) {
                toDelete.add(bp);
            }
        }
        
        erm.deleteEventRequests(toDelete);
    }
    // -- support methods --

    /**
     * Wait for the "server" thread to start. This must be synchronized on
     * serverThreadLock (in JdiDebugger).
     */
    private void serverThreadStartWait()
    {
        synchronized(this) {
            try {
                while (!serverThreadStarted) {
                    if (exitStatus == Debugger.TERMINATED_BY_BLUEJ || exitStatus == Debugger.TERMINATED_BY_USER_SYSTEM_EXIT)
                        throw new VMDisconnectedException();
                    wait(); // wait for new thread to start
                }
            }
            catch (InterruptedException ie) {}
        }
    }
    
    /**
     * Resume the server thread to begin executing some function.
     * 
     * Calls to this method should be synchronized on the serverThreadLock
     * (in JdiDebugger).
     */
    @SuppressWarnings("threadchecker")
    private void resumeServerThread()
    {
        synchronized (eventHandler) {
            serverThread.contServerThread();
            owner.raiseStateChangeEvent(Debugger.RUNNING);
        }
        // Note, we do the state change after the resume because the state
        // change may throw VMDisconnectedException (in which case we don't
        // want to go into the RUNNING state).
    }
    
    /**
     * Wait until the "worker" thread is ready for use. This method should
     * be called with the workerThread monitor held.
     * 
     * @throws VMDisconnectedException  if the VM terminates.
     */
    private void workerThreadReadyWait()
    {
        try {
            while (!workerThreadReady || workerThreadReserved) {
                if (exitStatus == Debugger.TERMINATED_BY_BLUEJ || exitStatus == Debugger.TERMINATED_BY_USER_SYSTEM_EXIT) {
                    throw new VMDisconnectedException();
                }
                workerThread.wait();
            }
        }
        catch(InterruptedException ie) {}
    }
    
    /**
     * Wait until the "worker" thread has finished executing. This
     * should be called only if workerThreadReserved has been set to
     * true by the current thread.
     * 
     * @throws VMDisconnectedException  if the VM terminates.
     */
    private void workerThreadFinishWait()
    {
        try {
            while (!workerThreadReady) {
                if (exitStatus == Debugger.TERMINATED_BY_BLUEJ || exitStatus == Debugger.TERMINATED_BY_USER_SYSTEM_EXIT) {
                    throw new VMDisconnectedException();
                }
                workerThread.wait();
            }
        }
        catch(InterruptedException ie) {}
    }

    public FXPlatformSupplier<DebuggerResult> launchFXApp(String className)
    {
        ObjectReference obj = null;
        exitStatus = Debugger.NORMAL_EXIT;
        try {
            obj = launchFXAppHelper(className);
        }
        catch (VMDisconnectedException e) {
            exitStatus = getDebuggerExitStatus();
            // return null; // debugger state change handled elsewhere
            return () -> new DebuggerResult(exitStatus);
        }
        catch (Exception e) {
            // remote invocation failed
            Debug.reportError("Launch FX app failed: " + e);
            e.printStackTrace();
            exitStatus = Debugger.EXCEPTION;
            lastException = new ExceptionDescription("Internal BlueJ error: unexpected exception in remote VM\n" + e);
        }
        if (obj == null) {
            return () -> new DebuggerResult(lastException);
        }
        else {
            ObjectReference objFinal = obj;
            return () -> new DebuggerResult(JdiObject.getDebuggerObject(objFinal));
        }
    }

    private ObjectReference launchFXAppHelper(String className)
    {
        // Calls to this method are serialized via serverThreadLock in JdiDebugger
        serverThreadStartWait();

        // Store the class and method to call
        setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, className);
        setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.LAUNCH_FX_APP));

        // Resume the thread, wait for it to finish and the new thread to start
        serverThreadStarted = false;
        resumeServerThread();
        serverThreadStartWait();

        // Get return value and check for exceptions
        Value rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
        if (rval == null) {
            ObjectReference exception = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
            if (exception != null) {
                exceptionEvent(new InvocationException(exception));
            }
        }
        return (ObjectReference) rval;
    }

    /**
     * Invoke the default constructor for the given class and return a reference
     * to the generated instance.
     */
    private ObjectReference invokeConstructor(String className)
    {
        // Calls to this method are serialized via serverThreadLock in JdiDebugger
        
        serverThreadStartWait();

        // Store the class and method to call
        setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, className);
        setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.INSTANTIATE_CLASS));
        
        // Resume the thread, wait for it to finish and the new thread to start
        serverThreadStarted = false;
        resumeServerThread();
        serverThreadStartWait();
        
        // Get return value and check for exceptions
        Value rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
        if (rval == null) {
            ObjectReference exception = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
            if (exception != null) {
                exceptionEvent(new InvocationException(exception));
            }
        }
        return (ObjectReference) rval;
    }
    
    /**
     * Invoke a particular constructor with arguments. The parameter types
     * of the constructor must be supplied (String[]) as well as the
     * argument values (ObjectReference []).
     * 
     * @param className  The name of the class to construct an instance of
     * @param paramTypes The parameter types of the constructor (class names)
     * @param args      The argument values to use in the constructor call
     * 
     * @return  The newly constructed object
     */
    @OnThread(Tag.NOTVMEventHandler)
    private ObjectReference invokeConstructor(String className, String [] paramTypes, ObjectReference [] args)
    {
        // Calls to this method are serialized via serverThreadLock in JdiDebugger
        
        serverThreadStartWait();
        boolean needsMachineResume = false;
        
        try {
            int length = paramTypes.length;
            if (args.length != length) {
                throw new IllegalArgumentException();
            }

            // Store the class, parameter types and arguments

            ArrayType objectArray = (ArrayType) loadClass("[Ljava.lang.Object;");
            ArrayType stringArray = (ArrayType) loadClass("[Ljava.lang.String;");

            // avoid problems with ObjectCollectedExceptions, see:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257193
            // We suspend the machine which seems to help prevent GC from occurring.
            machine.suspend();
            needsMachineResume = true;
            ArrayReference argsArray = objectArray.newInstance(length);
            ArrayReference typesArray = stringArray.newInstance(length);
            
            // Even with a suspended virtual machine, these arrays have been known to be garbage collected.
            // Force them to remain uncollected:
            while (true) {
                try {
                    argsArray.disableCollection();
                    break;
                }
                catch (ObjectCollectedException oce) {
                    argsArray = objectArray.newInstance(length);
                }
            }
            
            while (true) {
                try {
                    typesArray.disableCollection();
                    break;
                }
                catch (ObjectCollectedException oce) {
                    typesArray = stringArray.newInstance(length);
                }
            }
            
            // Fill the arrays with the correct values
            for (int i = 0; i < length; i++) {
                StringReference s = machine.mirrorOf(paramTypes[i]);
                typesArray.setValue(i, s);
                argsArray.setValue(i, args[i]);
            }
            
            setStaticFieldValue(serverClass, ExecServer.PARAMETER_TYPES_NAME, typesArray);
            setStaticFieldValue(serverClass, ExecServer.ARGUMENTS_NAME, argsArray);
            typesArray.enableCollection();
            argsArray.enableCollection();

            setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, className);
            setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.INSTANTIATE_CLASS_ARGS));
            machine.resume();
            needsMachineResume = false;
            
            // Resume the thread, wait for it to finish and the new thread to start
            serverThreadStarted = false;
            resumeServerThread();
            serverThreadStartWait();
            
            // Get return value and check for exceptions
            Value rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
            if (rval == null) {
                ObjectReference exception = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
                if (exception != null) {
                    exceptionEvent(new InvocationException(exception));
                }
            }
            return (ObjectReference) rval;

        }
        catch (ClassNotFoundException cnfe) { }
        catch (ClassNotLoadedException cnle) { }
        catch (InvalidTypeException ite) { }
        finally {
            if (needsMachineResume) {
                machine.resume();
            }
        }
        
        return null;
    }
    
    // Calls to this method are serialized via serverThreadLock in JdiDebugger
    public Value invokeTestSetup(String cl)
            throws InvocationException
    {
        // Make sure the server thread has started
        serverThreadStartWait();
        
        // Store the class and method to call
        setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, cl);
        setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.TEST_SETUP));
        
        // Resume the thread, wait for it to finish and the new thread to start
        serverThreadStarted = false;
        resumeServerThread();
        serverThreadStartWait();
        
        // Get return value and check for exceptions
        Value rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
        if (rval == null) {
            ObjectReference e = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
            if (e != null) {
                exceptionEvent(new InvocationException(e));
                throw new InvocationException(e);
            }
        }
        return rval;
    }
    
    /**
     * Run a JUnit test on a single test method or all test methods (including setup/teardown).
     * @param cl     The class containing the test methods
     * @return  null if all tests passed, or an ArrayReference if any fails, 
     *          which has a length of [1 + 7*(number of failures/errors)]
     *          The first item of the array contains the runtime of executing all tests,
     *          then each failure/error has seven consecutive items in the array which contains: 
     *          [1] = the exception message (or "no exception message")<br>
     *          [2] = the stack trace as a string (or "no stack trace")<br>
     *          [3] = the name of the class in which the exception/failure occurred<br>
     *          [4] = the source filename for where the exception/failure occurred<br>
     *          [5] = the name of the method in which the exception/failure occurred<br>
     *          [6] = the line number where the exception/failure occurred (a string)<br>
     *          [7] = "failure" or "error" (string)<br>with:
     * @throws InvocationException
     */
    public Value invokeRunTest(String cl, String method)
        throws InvocationException
    {
        // Calls to this method are serialized via serverThreadLock in JdiDebugger

        serverThreadStartWait();
        
        // Store the class and method to call
        setStaticFieldObject(serverClass, ExecServer.CLASS_TO_RUN_NAME, cl);
        setStaticFieldObject(serverClass, ExecServer.METHOD_TO_RUN_NAME, method);
        setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.TEST_RUN));

        // Resume the thread, wait for it to finish and the new thread to start
        serverThreadStarted = false;
        resumeServerThread();
        serverThreadStartWait();
        
        Value rval = getStaticFieldObject(serverClass, ExecServer.METHOD_RETURN_NAME);
        if (rval == null) {
            ObjectReference e = getStaticFieldObject(serverClass, ExecServer.EXCEPTION_NAME);
            if (e != null) {
                exceptionEvent(new InvocationException(e));
                throw new InvocationException(e);
            }
        }
        return rval;
    }

    /**
     * Dispose of all gui windows opened from the debug vm.
     */
    void disposeWindows()
    {
        // Calls to this method are serialized via serverThreadLock in JdiDebugger

        serverThreadStartWait();
            
        // set the action to "dispose windows"
        setStaticFieldValue(serverClass, ExecServer.EXEC_ACTION_NAME, machine.mirrorOf(ExecServer.DISPOSE_WINDOWS));
        
        // Resume the thread, it then proceeds to remove open windows
        serverThreadStarted = false;
        resumeServerThread();
        // We don't bother waiting for it to finish
    }
    
    /**
     * Add an object to the object map on the debug vm.
     * @param instanceName  the name of the object to add
     * @param object        a reference to the object to add
     */
    void addObject(String scopeId, String instanceName, ObjectReference object)
    {
        try {
            synchronized(workerThread) {
                workerThreadReadyWait();
                setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.ADD_OBJECT));
                
                // parameters
                setStaticFieldObject(serverClass, ExecServer.OBJECTNAME_NAME, instanceName);
                setStaticFieldValue(serverClass, ExecServer.OBJECT_NAME, object);
                setStaticFieldObject(serverClass, ExecServer.SCOPE_ID_NAME, scopeId);
                
                workerThreadReady = false;
                workerThread.resume();
            }
        }
        catch (VMDisconnectedException vmde) {}
        catch (VMMismatchException vmmme) {}
    }
    
    /**
     * Remove an object from the object map on the debug vm.
     * @param instanceName   the name of the object to remove
     */
    void removeObject(String scopeId, String instanceName)
    {
        synchronized(workerThread) {
            try {
                workerThreadReadyWait();
                setStaticFieldValue(serverClass, ExecServer.WORKER_ACTION_NAME, machine.mirrorOf(ExecServer.REMOVE_OBJECT));
        
                // parameters
                setStaticFieldObject(serverClass, ExecServer.OBJECTNAME_NAME, instanceName);
                setStaticFieldObject(serverClass, ExecServer.SCOPE_ID_NAME, scopeId);
        
                workerThreadReady = false;
                workerThread.resume();
            }
            catch(VMDisconnectedException vmde) { }
        }
    }

    /**
     * Check whether a thread is sitting on the server thread breakpoint. 
     */
    static boolean isAtMainBreakpoint(ThreadReference tr)
    {
        try {
            return (tr.isAtBreakpoint() && SERVER_CLASSNAME.equals(tr.frame(0).location().declaringType().name()));
        }
        catch (IncompatibleThreadStateException e) {
            return false;
        }
    }

    /**
     * Get a reference to an object from a static field in some class in the
     * debug VM.
     * 
     * VMDisconnected exception may be thrown.
     * 
     * @param cl         The class containing the field
     * @param fieldName  The name of the field
     * @return    An ObjectReference referring to the object
     */
    ObjectReference getStaticFieldObject(ClassType cl, String fieldName)
    {
        Field resultField = cl.fieldByName(fieldName);

        if (resultField == null)
            throw new IllegalArgumentException("getting field " + fieldName + " resulted in no fields");

        return (ObjectReference) cl.getValue(resultField);
    }
    
    /**
     * Set the value of a static field in the debug VM.
     * @param cl         The class containing the field
     * @param fieldName  The name of the field
     * @param value      The value to which the field must be set
     */
    void setStaticFieldValue(ClassType cl, String fieldName, Value value)
    {
        Field field = cl.fieldByName(fieldName);
        
        try {
            cl.setValue(field,value);
        }
        catch(InvalidTypeException ite) { }
        catch(ClassNotLoadedException cnle) { }
    }

    /**
     * Set the value of some static field as a string. A mirror of the given
     * string value is created on the debug VM.
     */
    void setStaticFieldObject(ClassType cl, String fieldName, String value)
    {
        // Any mirror object which is created is prone to being garbage
        // collected before we can assign it to a field. This causes an
        // ObjectCollectedException. using the "disableCollection" method seems
        // to help but there is still a window between object creation and that
        // method being called, so we catch the exception and use a more
        // forceful approach in that case.
        
        try 
        {
            StringReference s = null;
            
            if (value != null)
            {
                s = machine.mirrorOf(value);
                s.disableCollection();
            }
            setStaticFieldValue(cl, fieldName, s);
            if (value != null)
            {
                s.enableCollection();
            }
        }
        catch(ObjectCollectedException oce) {
            machine.suspend();
            StringReference s;
            if (value != null)
            {
                s = machine.mirrorOf(value);
                setStaticFieldValue(cl, fieldName, s);
            }
            machine.resume();
        }
    }

    /**
     * Find the mirror of a class/interface/array in the remote VM.
     * 
     * The class is expected to exist. We expect only one single class to exist
     * with this name. Throws a ClassNotFoundException if the class could not be
     * found.
     * 
     * This should only be used for classes that we know exist and are loaded ie
     * ExecServer etc.
     */
    private ReferenceType findClassByName(String className, ClassLoaderReference clr)
        throws ClassNotFoundException
    {
        // find the class
        List<ReferenceType> list = machine.classesByName(className);
        if (list.size() == 1) {
            return (ReferenceType) list.get(0);
        }
        else if (list.size() > 1) {
            Iterator<ReferenceType> iter = list.iterator();
            while (iter.hasNext()) {
                ReferenceType cl = iter.next();
                if (cl.classLoader() == clr)
                    return cl;
            }
        }
        throw new ClassNotFoundException(className);
    }

    /**
     * Find the mirror of a class/interface/array in the remote VM.
     * 
     * @param className
     *            the name of the class to find
     * @return a reference to the class
     * 
     * @throws ClassNotFoundException
     */
    public ReferenceType findClassByName(String className)
        throws ClassNotFoundException
    {
        return findClassByName(className, currentLoader);
    }

    /**
     * Find the mirror of a method in the remote VM.
     * 
     * The method is expected to exist. We expect only one single method to
     * exist with this name and report an error if more than one is found.
     */
    Method findMethodByName(ReferenceType type, String methodName)
    {
        List<Method> list = type.methodsByName(methodName);
        if (list.size() != 1) {
            throw new IllegalArgumentException("getting method " + methodName + " resulted in " + list.size()
                    + " methods");
        }
        return (Method) list.get(0);
    }

    /**
     * Create a thread that will retrieve any output from the remote machine and
     * direct it to our terminal (or vice versa).
     */
    private IOHandlerThread redirectIOStream(final Reader reader, final Writer writer)
    {
        IOHandlerThread thr;

        thr = new IOHandlerThread(reader, writer);
        thr.setPriority(Thread.MAX_PRIORITY - 1);
        thr.start();

        return thr;
    }

    /**
     * Sets which field future constructor/method invocations will run on.
     *
     * Communicates the change to the debug VM, but obviously will only affect
     * future methods, not any already running.
     */
    public void setRunOnThread(RunOnThread runOnThread)
    {
        // In Greenfoot, we run on the Simulation thread, which is handled on the debug VM,
        // so we don't want to mess with the setting from the server VM.  Just ignore:
        if (Config.isGreenfoot())
            return;

        int fieldValue;
        switch (runOnThread)
        {
            case FX:
                fieldValue = ExecServer.RUN_ON_FX_THREAD;
                break;
            case SWING:
                fieldValue = ExecServer.RUN_ON_SWING_THREAD;
                break;
            default:
                fieldValue = ExecServer.RUN_ON_DEFAULT_THREAD;
                break;
        }

        synchronized(workerThread)
        {
            workerThreadReadyWait();
            setStaticFieldValue(serverClass, ExecServer.RUN_ON_THREAD_NAME, machine.mirrorOf(fieldValue));
        }
    }

    @OnThread(Tag.Any)
    public void runOnEventHandler(EventHandlerRunnable runnable)
    {
        eventHandler.queueRunnable(runnable);
    }

    /**
     * The thread for retrieving output from the remote machine and redirecting
     * it to the terminal.
     */
    private class IOHandlerThread extends Thread
    {
        private Reader reader;
        private Writer writer;
        private volatile boolean keepRunning = true;

        @OnThread(Tag.Any)
        IOHandlerThread(Reader reader, Writer writer)
        {
            super("BlueJ I/O Handler");
            this.reader = reader;
            this.writer = writer;
            setPriority(Thread.MIN_PRIORITY);
        }

        @OnThread(Tag.Any)
        public void close()
        {
            keepRunning = false;
        }

        public void run()
        {
            try {
                // An arbitrary buffer size.
                char [] chbuf = new char[4096];
                
                while (keepRunning) {
                    int numchars = reader.read(chbuf);
                    if (numchars == -1) {
                        keepRunning = false;
                    }
                    else if (keepRunning) {
                        writer.write(chbuf, 0, numchars);
                        if (! reader.ready()) {
                            writer.flush();
                        }
                    }
                }
            }
            catch (IOException ex) {
                // Debug.reportError("Cannot read output user VM.");
            }
        }
    }

    /**
     * Find the VMReference which corresponds to the supplied VirtualMachine instance.
     */
    public static VMReference getVmForMachine(VirtualMachine mc)
    {
        synchronized (vmToReferenceMap) {
            return (VMReference) vmToReferenceMap.get(mc);
        }
    }
    
    /*
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
    */
}
