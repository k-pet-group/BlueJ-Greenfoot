/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012  Michael Kolling and John Rosenberg 
 
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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerEvent;
import bluej.debugger.DebuggerListener;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.DebuggerTerminal;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.DebuggerThreadTreeModel;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.SourceLocation;
import bluej.debugmgr.Invoker;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.InvocationException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VMOutOfMemoryException;
import com.sun.jdi.Value;

/**
 * A class implementing the execution and debugging primitives needed by BlueJ.
 * 
 * <p>This class is tightly coupled with the classes VMReference and
 * VMEventHandler. JdiDebugger is the half of the debugger that is persistent
 * across debugger sessions. VMReference and VMEventHandler will be constructed
 * anew each time a remote VM is started. VMReference handles most of the work
 * of making the remote VM do things. VMEventHandler starts a new thread that
 * listens for remote VM events and calls back into VMReference on reciept of
 * these events.
 * 
 * <p>Most of the actual access to the virtual machine occurs through the
 * MachineLoader thread. When the vm is restarted by user request, a new loader
 * thread is created immediately so that any method calls/etc will execute on
 * the new machine (after waiting until it has loaded).
 * 
 * @author Michael Kolling
 * @author Andrew Patterson
 */
public class JdiDebugger extends Debugger
{
    private static final int loaderPriority = Thread.NORM_PRIORITY - 2;

    // If false, specifies that a new VM should be started when the old one dies
    private boolean autoRestart = true;

    // Did we order the VM to restart ourself? This is used to flag the launch()
    // method that a new loader thread doesn't need to be re-created.
    private boolean selfRestart = false;

    /**
     * The reference to the current remote VM handler. Will be null if the remote VM is not
     * currently running.
     */
    private VMReference vmRef;

    // the thread that we spawn to load the current remote VM
    private MachineLoaderThread machineLoader;
    
    /** An object to provide a lock for server thread execution */
    private Object serverThreadLock = new Object();

    // a set holding all the JdiThreads in the VM
    private JdiThreadSet allThreads;

    // a TreeModel exposing selected JdiThreads in the VM
    private JdiThreadTreeModel treeModel;

    // listeners for events that occur in the debugger
    private List<DebuggerListener> listenerList = new ArrayList<DebuggerListener>();

    // the directory to launch the VM in
    private File startingDirectory;

    // terminal to use for all VM input and output
    private DebuggerTerminal terminal;

    // a Set of strings which have been used as names on the
    // object bench. We endeavour to not reuse them.
    private Set<String> usedNames;

    // indicate whether we want to see system threads
    private boolean hideSystemThreads;
    
    /**
     * Current machine state. This is changed only by the VM event queue (see VMEventHandler),
     * but write access is also protected by the listener list mutex. This makes it possible to
     * add a listener and know the state at the time the listener was added. Furthermore the
     * state will only be set to RUNNING while the server thread lock is also held.
     */
    private int machineState = NOTREADY;
    
    // classpath to be used for the remote VM
    private BPClassLoader lastProjectClassLoader;
    
    // most recent exception description
    private ExceptionDescription lastException;
    
    /** User libraries to be added to VM classpath */
    private URL[] libraries = {};

    /**
     * Construct an instance of the debugger.
     * 
     * <p>This constructor should not be used by the main part of BlueJ. Access
     * should be through Debugger.getDebuggerImpl().
     * 
     * @param startingDirectory
     *            a File representing the directory we should launch the debug
     *            VM in.
     * @param terminal
     *            a Terminal where we can do input/output.
     */
    public JdiDebugger(File startingDirectory, DebuggerTerminal terminal)
    {
        this.startingDirectory = startingDirectory;
        this.terminal = terminal;

        allThreads = new JdiThreadSet();
        treeModel = new JdiThreadTreeModel(new JdiThreadNode());
        usedNames = new TreeSet<String>();
        hideSystemThreads = true;
    }

    @Override
    public void setUserLibraries(URL[] libraries)
    {
        this.libraries = libraries;
    }
    
    /**
     * Start debugging.
     */
    @Override
    public synchronized void launch()
    {
        // This could be either an initial launch (selfRestart == false) or
        // a restart (selfRestart == true and machineLoader != null). In the
        // latter case, there's no need to create a new machine loader, as
        // that's pre-done in close(), below.

        if (vmRef != null) {
            throw new IllegalStateException("JdiDebugger.launch() was called but the debugger was already loaded");
        }

        if (machineLoader != null && !selfRestart) {
            // Attempt to restart VM while already restarting - ignored,
            // except when self-restarting, seeing as the new machine loader
            // has already been created in that case.
            return;
        }

        autoRestart = true;

        // start the MachineLoader (a separate thread) to load the
        // remote virtual machine in the background

        if (!selfRestart) {
            // if selfRestart == true, this has already been done
            machineLoader = new MachineLoaderThread();
        }
        selfRestart = false;
        // lower priority to improve GUI response time
        machineLoader.setPriority(loaderPriority);
        machineLoader.start();
    }

    /**
     * Close this VM, possibly restart it.
     */
    @Override
    public synchronized void close(boolean restart)
    {
        // There are essentially three states the remote process could be in:
        // started, stopping, or launching. It will not already be stopped
        // as this only occurs when the project is closed.
        //
        // Following conditions are true in each state:
        //
        // Started: vmRunning = true.
        //
        // Stopping:  (for restart)
        //            vmRunning = false. selfRestart = true.
        //            machineLoader != null.
        //                   - or -
        //            (for permanent close)
        //            vmRunning = false. selfRestart = false.
        //            machineLoader == null.
        //
        // Launching: vmRunning = false. selfRestart = false.
        //              machineLoader != null.

        if (vmRef != null) {
            // The process is already started. We want to stop it (and
            // possibly to then restart it).
            autoRestart = restart;
            selfRestart = restart;

            // Create the new machine loader thread. That way any operation
            // on the VM between now and the time the new machine has finished
            // loading, can sleep until the new machine is ready.
            if (selfRestart) {
                machineLoader = new MachineLoaderThread();
            }

            // kill the remote debugger process
            vmRef.close();

            // we will eventually get a vmDisconnect event and end up in
            // method vmDisconnect() (below)
        }
        // The state is either "launching" or "stopping for restart". In either
        // case, if restart parameter == true, no further action is necessary. If
        // restart is false, we need to make sure no relaunch occurs (or it is
        // halted immediately).
        else if (!restart) {
            autoRestart = false;
            selfRestart = false;
            machineLoader = null;
        }
    }

    /**
     * Add a listener for DebuggerEvents
     * 
     * @param l
     *            the DebuggerListener to add
     */
    public int addDebuggerListener(DebuggerListener l)
    {
        synchronized (listenerList) {
            listenerList.add(l);
            return machineState;
        }
    }

    /**
     * Remove a listener for DebuggerEvents.
     * 
     * @param l
     *            the DebuggerListener to remove
     */
    public void removeDebuggerListener(DebuggerListener l)
    {
        synchronized (listenerList) {
            listenerList.remove(l);
        }
    }
    
    /**
     * Get a copy of the listener list.
     */
    private DebuggerListener [] getListeners()
    {
        synchronized (listenerList) {
            return listenerList.toArray(new DebuggerListener[listenerList.size()]);
        }
    }

    /**
     * Guess a suitable name for an object about to be put on the object bench.
     * 
     * @param className
     *            the fully qualified name of the class of object
     * @return a String suitable as a name for an object on the object bench.
     */
    public String guessNewName(String className)
    {
        // className can have array brackets at the end which is not suitable
        // for an identifier. We'll strip them out
        className = className.replace('[', ' ').replace(']', ' ').trim();

        String baseName = JavaNames.getBase(className);

        // truncate long names to OBJ_NAME_LENGTH plus _instanceNum
        int stringEndIndex = baseName.length() > Invoker.OBJ_NAME_LENGTH ? Invoker.OBJ_NAME_LENGTH : baseName.length();

        String newName = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1, stringEndIndex);

        int num = 1;

        synchronized(this) {
            while (usedNames.contains(newName + num))
                num++;
        }

        return newName + num;
    }
    
    
    public String guessNewName(DebuggerObject obj)       
    {
        String name = null;
        DebuggerClass cls = obj.getClassRef();
        
        if(cls.isEnum()) {
            Value val = obj.getObjectReference();
            name = JdiUtils.getJdiUtils().getValueString(val);
        }
        
        if(name == null) {
            name = cls.getName();
        }
        
        return guessNewName(name);
    }
   
    /**
     * Create a class loader in the debugger.
     * @param bpClassLoader the class loader that should be used to load the user classes in the remote VM.
     */
    public synchronized void newClassLoader(BPClassLoader bpClassLoader)
    {
        // lastProjectClassLoader is used if there is a VM restart
        if (bpClassLoader != null) {
            lastProjectClassLoader = bpClassLoader;
        }
        else {
            return;
        }
    
        VMReference vmr = getVMNoWait();
        if (vmr != null) {
            usedNames.clear();
            try {
                vmr.clearAllBreakpoints();
                vmr.newClassLoader(bpClassLoader.getURLs());
            }
            catch (VMDisconnectedException vmde) {}
        }
    }

    /**
     * Remove all breakpoints in the given class.
     */
    public void removeBreakpointsForClass(String className)
    {
        VMReference vmr = getVMNoWait();
        if (vmr != null) {
            vmr.clearBreakpointsForClass(className);
        }
    }
    
    /**
     * Add a debugger object into the project scope.
     * 
     * @param newInstanceName
     *            the name of the object dob the object itself
     * @return true if the object could be added with this name, false if there
     *         was a name clash.
     */
    public boolean addObject(String scopeId, String newInstanceName, DebuggerObject dob)
    {
        VMReference vmr = getVMNoWait();
        if (vmr != null) {
            vmr.addObject(scopeId, newInstanceName, ((JdiObject) dob).getObjectReference());
            synchronized (this) {
                usedNames.add(newInstanceName);
            }
        }
        return true;
    }

    /**
     * Remove an object from a package scope (when removed from object bench).
     */
    public void removeObject(String scopeId, String instanceName)
    {
        VMReference vmr = getVMNoWait();
        if (vmr != null) {
            vmr.removeObject(scopeId, instanceName);
        }
    }

    /**
     * Return the debugger objects that exist in the debugger.
     * 
     * @return a Map of (String name, DebuggerObject obj) entries
     */
    public Map<String, DebuggerObject> getObjects()
    {
        throw new IllegalStateException("not implemented");
        // the returned array consists of double the number of objects
        // they alternate, name, object, name, object
        // ie.
        // arrayRef[0] = a field name 0 (StringReference)
        // arrayRef[1] = a field value 0 (ObjectReference)
        // arrayRef[2] = a field name 1 (StringReference)
        // arrayRef[3] = a field value 1 (ObjectReference)
        //
    }
    
    /**
     * Return the machine status; one of the "machine state" constants:
     * NOTREADY, IDLE, RUNNING, or SUSPENDED.
     */
    public int getStatus()
    {
        return machineState;
    }
    
    /*
     * @see bluej.debugger.Debugger#getMirror(java.lang.String)
     */
    public DebuggerObject getMirror(String value)
    {
        VMReference vmr = getVM();
        if (vmr != null) {
            try {
                return JdiObject.getDebuggerObject(vmr.getMirror(value));
            }
            catch (VMDisconnectedException vde) { }
            catch (VMOutOfMemoryException vmoome) { }
        }
        return null;
    }

    /**
     * Return the text of the last exception.
     */
    public ExceptionDescription getException()
    {
        return lastException;
    }

    /**
     * List all threads being debugged as a TreeModel.
     * 
     * @return A tree model of all the threads.
     */
    public DebuggerThreadTreeModel getThreadTreeModel()
    {
        return treeModel;
    }

    // ------ Following methods run code on server thread in debug VM ------
    //
    // These methods synchronise on serverThreadLock.

    /**
     * Run the setUp() method of a test class and return the created objects.
     * 
     * @param className
     *            the fully qualified name of the class
     * @return a Map of (String name, DebuggerObject obj) entries
     *         null if an error occurs (such as VM termination)
     */
    public Map<String, DebuggerObject> runTestSetUp(String className)
    {
        ArrayReference arrayRef = null;
        Map<String, DebuggerObject> returnMap = new HashMap<String, DebuggerObject>();
        
        VMReference vmr = getVM();
        try {
            synchronized (serverThreadLock) {
                if (vmr != null) {
                    arrayRef = (ArrayReference) vmr.invokeTestSetup(className);
                }
                
                // the returned array consists of double the number of fields created by
                // running test setup plus one extra slot
                // they alternate, fieldname, fieldvalue, fieldname, fieldvalue
                // ie.
                // arrayRef[0] = a field name 0 (StringReference)
                // arrayRef[1] = a field value 0 (ObjectReference)
                // arrayRef[2] = a field name 1 (StringReference)
                // arrayRef[3] = a field value 1 (ObjectReference)
                // with the last slot being reserved for the ObjectReference of the actual 
                // test object. This is used to extract (potentially generic) fields.
                // we could return a Map from RUN_TEST_SETUP but then we'd have to use
                // JDI reflection to make method calls on Map in order to extract the values
                
                if (arrayRef != null) {
                    
                    // The test case object
                    ObjectReference testObject = (ObjectReference)arrayRef.getValue(arrayRef.length()-1);
                    // get the associated JdiObject so that we can get potentially generic fields 
                    // from the test case.
                    JdiObject jdiTestObject = JdiObject.getDebuggerObject(testObject);
                    
                    // last slot in array is test case object so it does not get touched here
                    // our iteration boundary is therefore one less than array length
                    for (int i = 0; i < arrayRef.length() - 1; i += 2) {
                        String fieldName = ((StringReference) arrayRef.getValue(i)).value();
                        Field testField = testObject.referenceType().fieldByName(fieldName);            
                        returnMap.put(fieldName, JdiObject
                                .getDebuggerObject((ObjectReference) arrayRef.getValue(i + 1), testField, jdiTestObject));
                    }
                }
            }
        }
        catch (InvocationException e) {
            // what to do here??
            return null;
        }
        catch (VMDisconnectedException e) {
            return null;
        }
        
        // the resulting map consists of entries (String fieldName, JdiObject
        // obj)
        return returnMap;
    }

    /**
     * Run a single test method in a test class and return the result.
     * 
     * @param className
     *            the fully qualified name of the class
     * @param methodName
     *            the name of the method
     * @return a DebuggerTestResult object
     */
    public DebuggerTestResult runTestMethod(String className, String methodName)
    {
        ArrayReference arrayRef = null;

        try {
            VMReference vmr = getVM();
            synchronized (serverThreadLock) {
                if (vmr != null) {
                    arrayRef = (ArrayReference) vmr.invokeRunTest(className, methodName);
                }
                
                if (arrayRef == null || arrayRef.length() == 0) {
                    return new JdiTestResultError(className, methodName, "VM returned unknown result", "", null, 0);
                }
                
                int runTimeMs = Integer.parseInt(((StringReference) arrayRef.getValue(0)).value());
                
                if (arrayRef != null && arrayRef.length() > 5) {
                    String failureType = ((StringReference) arrayRef.getValue(7)).value();
                    String exMsg = ((StringReference) arrayRef.getValue(1)).value();
                    String traceMsg = ((StringReference) arrayRef.getValue(2)).value();
                    
                    String failureClass = ((StringReference) arrayRef.getValue(3)).value();
                    String failureSource = ((StringReference) arrayRef.getValue(4)).value();
                    String failureMethod = ((StringReference) arrayRef.getValue(5)).value();
                    int lineNo = Integer.parseInt(((StringReference) arrayRef.getValue(6)).value());
                    SourceLocation failPoint = new SourceLocation(failureClass, failureSource, failureMethod, lineNo);
                    
                    if (failureType.equals("failure")) {
                        return new JdiTestResultFailure(className, methodName, exMsg, traceMsg, failPoint, runTimeMs);
                    }
                    else {
                        return new JdiTestResultError(className, methodName, exMsg, traceMsg, failPoint, runTimeMs);
                    }
                    
                } else if (arrayRef != null && arrayRef.length() == 1) {
                    // Success - extract the run time in mS
                    return new JdiTestResult(className, methodName, runTimeMs);
                }
            }
        }
        catch (InvocationException ie) {
            // what to do here??
            return new JdiTestResultError(className, methodName, "Internal invocation error", "", null, 0);
        }
        catch (VMDisconnectedException vmde) {
            return new JdiTestResultError(className, methodName, "VM restarted", "", null, 0);
        }
        
        // should never get here
        return new JdiTestResultError(className, methodName, "VM returned unknown result", "", null, 0);
    }

    /**
     * Dispose all top level windows in the remote machine.
     */
    public void disposeWindows()
    {
        VMReference vmr = getVMNoWait();

        try {
            synchronized (serverThreadLock) {
            if (vmr != null)
                vmr.disposeWindows();
            }
        }
        catch (VMDisconnectedException e) {}
    }

    /**
     * "Start" a class (i.e. invoke its main method)
     * 
     * @param classname
     *            the class to start
     */
    public DebuggerResult runClassMain(String className)
        throws ClassNotFoundException
    {
        VMReference vmr = getVM();
        synchronized (serverThreadLock) {
            if (vmr != null) {
                return vmr.runShellClass(className);
            }
            else {
                return null;
            }
        }
    }
    
    /**
     * Construct a class instance using the default constructor.
     */
    public DebuggerResult instantiateClass(String className)
    {
        VMReference vmr = getVM();
        if (vmr != null) {
            synchronized (serverThreadLock) {
                return vmr.instantiateClass(className);
            }
        }
        else {
            return new DebuggerResult(Debugger.TERMINATED);
        }
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.Debugger#instantiateClass(java.lang.String, java.lang.String[], bluej.debugger.DebuggerObject[])
     */
    public DebuggerResult instantiateClass(String className, String[] paramTypes, DebuggerObject[] args)
    {
        // If there are no arguments, use the default constructor
        if (paramTypes == null || args == null || paramTypes.length == 0 || args.length == 0) {
            return instantiateClass(className);
        }
        
        VMReference vmr = getVM();
        if (vmr != null) {
            // Convert the args array from DebuggerObject[] to ObjectReference[]
            ObjectReference [] orArgs = new ObjectReference[args.length];
            for (int i = 0; i < args.length; i++) {
                JdiObject jdiObject = (JdiObject) args[i];
                orArgs[i] = jdiObject.getObjectReference(); 
            }
            
            synchronized (serverThreadLock) {
                return vmr.instantiateClass(className, paramTypes, orArgs);
            }
        }
        else {
            return new DebuggerResult(Debugger.TERMINATED);
        }
    }
    
    /*
     * @see bluej.debugger.Debugger#getClass(java.lang.String, boolean)
     */
    public DebuggerClass getClass(String className, boolean initialize)
        throws ClassNotFoundException
    {
        VMReference vmr = getVM();
        if (vmr == null) {
            throw new ClassNotFoundException("Virtual machine terminated.");
        }
            
        ReferenceType classMirror;
        synchronized (serverThreadLock) {
            // machineState can only be changed *to* RUNNING while the serverThreadLock is held, so
            // this check is safe:
            if (initialize && machineState != Debugger.RUNNING) {
                classMirror = vmr.loadInitClass(className);
            }
            else {
                classMirror = vmr.loadClass(className);
            }
        }

        return new JdiClass(classMirror);
    }

    // ----- end server thread methods -----
    
    /**
     * notify all listeners that have registered interest for
     * notification on this event type.
     */ 
    private void fireTargetEvent(DebuggerEvent ce, boolean skipUpdate)
    {
        // Guaranteed to return a non-null array
        DebuggerListener[] listeners = getListeners();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1; i >= 0; i --) {
            listeners[i].processDebuggerEvent(ce, skipUpdate);
        }
    }
    
    void raiseStateChangeEvent(int newState)
    {
        // It might look this method should be synchronized, but it shouldn't,
        // because state change is effectively serialized by VMEventHandler (except
        // in some cases where it is known that no VM is running).
        
        if (newState != machineState) {
            
            // Going from SUSPENDED to any other state must ass through RUNNING
            if (machineState == SUSPENDED && newState != RUNNING) {
                doStateChange(SUSPENDED, RUNNING);
            }
            
            // If going from RUNNING state to NOTREADY state, first pass
            // through IDLE state
            if (machineState == RUNNING && newState == NOTREADY) {
                doStateChange(RUNNING, IDLE);
            }
            
            doStateChange(machineState, newState);
        }
    }
    
    private void doStateChange(int oldState, int newState)
    {
        DebuggerListener[] ll;
        synchronized (listenerList) {
            ll = listenerList.toArray(new DebuggerListener[listenerList.size()]);
            machineState = newState;
        }
        
        for (DebuggerListener l : ll) {
            l.processDebuggerEvent(new DebuggerEvent(this, DebuggerEvent.DEBUGGER_STATECHANGED,
                    oldState, newState), false);
        }
    }

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Set/clear a breakpoint at a specified line in a class.
     * 
     * @param className
     *            The class in which to set/clear the breakpoint.
     * @param line
     *            The line number of the breakpoint.
     * @param set
     *            True to set, false to clear a breakpoint.
     * 
     * @return null if there was no problem, or an error string
     */
    public String toggleBreakpoint(String className, int line, boolean set, Map<String, String> properties)
    {
        // Debug.message("[toggleBreakpoint]: " + className + " line " + line);

        VMReference vmr = getVM();
        try {
            if (vmr != null) {
                if (set) {
                    return vmr.setBreakpoint(className, line, properties);
                }
                else {
                    return vmr.clearBreakpoint(className, line);
                }
            }
            else {
                return "VM terminated.";
            }
        }
        catch (Exception e) {
            Debug.reportError("breakpoint error: " + e);
            e.printStackTrace(System.out);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
        }
    }
    
    /*
     * @see bluej.debugger.Debugger#toggleBreakpoint(java.lang.String, java.lang.String, boolean, java.util.Map)
     */
    public String toggleBreakpoint(String className, String method, boolean set, Map<String, String> properties)
    {
        VMReference vmr = getVM();
        try {
            if (vmr != null) {
                if (set) {
                    return vmr.setBreakpoint(className, method, properties);
                }
                else {
                    return vmr.clearBreakpoint(className, method);
                }
            }
            else {
                return "VM terminated.";
            }
        }
        catch (Exception e) {
            Debug.reportError("breakpoint error: " + e);
            e.printStackTrace(System.out);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
        }
    }
    
    /*
     * @see bluej.debugger.Debugger#toggleBreakpoint(bluej.debugger.DebuggerClass, java.lang.String, boolean, java.util.Map)
     */
    public String toggleBreakpoint(DebuggerClass debuggerClass, String method, boolean set, Map<String, String> properties)
    {
        VMReference vmr = getVM();
        try {
            if (vmr != null) {
                JdiClass jdiClass = (JdiClass) debuggerClass;
                if (set) {
                    return vmr.setBreakpoint(jdiClass.remoteClass, method, properties);
                }
                else {
                    return vmr.clearBreakpoint(jdiClass.remoteClass, method);
                }
            }
            else {
                return "VM terminated.";
            }
        }
        catch (Exception e) {
            Debug.reportError("breakpoint error: " + e);
            e.printStackTrace(System.out);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
        }
    }


    /**
     * Called by VMReference when a breakpoint/step is encountered in the
     * debugger VM.
     * 
     * @param tr   the thread in which code hit the breakpoint/step
     * @param bp   true for a breakpoint, false for a step
     */
    public void breakpoint(final ThreadReference tr, final int debuggerEventType, boolean skipUpdate, DebuggerEvent.BreakpointProperties props)
    {
        final JdiThread breakThread = allThreads.find(tr);
        if (false == skipUpdate) {
            treeModel.syncExec(new Runnable() {
                public void run()
                {
                    JdiThreadNode jtn = treeModel.findThreadNode(tr);
                    // if the thread at the breakpoint is not currently displayed,
                    // display it now.
                    if (jtn == null) {
                        JdiThreadNode root = treeModel.getThreadRoot();
                        treeModel.insertNodeInto(new JdiThreadNode(breakThread), root, 0);
                    }
                    else {
                        treeModel.nodeChanged(jtn);
                    }
                }
            });
        }

        fireTargetEvent(new DebuggerEvent(this, debuggerEventType, breakThread, props), skipUpdate);
    }
    
    /**
     * Screen a breakpoint/step event through interested listeners.
     * 
     * @param thread  The thread which hit the breakpoint/step event.
     * @param breakpoint  True if this is a breakpoint; false if a step. Note that both can occur
     *                    simultaneously, in which case both are screened individually.
     * @param props   The breakpoint properties (if any).
     * @return   true if the event is screened, that is, the GUI should not be updated because the
     *                result of the event is temporary.
     */
    public boolean screenBreakpoint(ThreadReference thread, int debuggerEventType,
            DebuggerEvent.BreakpointProperties props)
    {
        JdiThread breakThread = allThreads.find(thread);
        breakThread.stopped();
        
        DebuggerEvent event = new DebuggerEvent(this, debuggerEventType, breakThread, props);
        
        boolean done = false;
        // Guaranteed to return a non-null array
        DebuggerListener[] listeners = getListeners();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1; i >= 0; i--) {
            done = done || listeners[i].examineDebuggerEvent(event);
        }
        return done;
    }

    // - event handling

    /**
     * Called by VMReference when the machine disconnects. The disconnect event
     * follows a machine 'exit' event.
     */
    synchronized void vmDisconnect()
    {
        if (autoRestart) {
            
            allThreads.clear();
            
            // It's possible to receive vmDisconnect before we're even aware that
            // we're running. We can ignore it in that case. Synchronization insures
            // that valid disconnect events are never lost.
            if (vmRef != null) {
                // Indicate to the launch procedure that we are not in a launch 
                // (see launch()).
                //
                // In the case of a self-restart, a new machine loader has only
                // just been set-up, so don't trash it now!
                if (!selfRestart) {
                    machineLoader = new MachineLoaderThread();
                }
                selfRestart = true;
                
                vmRef.closeIO();
                vmRef = null;
                
                raiseStateChangeEvent(Debugger.NOTREADY);

                launch();
                
                usedNames.clear();
                treeModel.syncExec(new Runnable() {
                    public void run()
                    {
                        treeModel.setRoot(new JdiThreadNode());
                        treeModel.reload();
                    }
                });
            }
        }
    }

    /**
     * Called by VMReference when a thread is started in the debugger VM.
     * 
     * Use this event to keep our thread tree model up to date. Currently we
     * ignore the thread group and construct all threads at the same level.
     */
    void threadStart(final ThreadReference tr)
    {
        final JdiThread newThread = new JdiThread(this, tr);
        allThreads.add(newThread);
        treeModel.syncExec(new Runnable() {
            public void run()
            {
                displayThread(newThread);
            }
        });
    }

    /**
     * Called by VMReference when a thread dies in the debugger VM.
     * 
     * Use this event to keep our thread tree model up to date.
     */
    void threadDeath(final ThreadReference tr)
    {
        allThreads.removeThread(tr);
        treeModel.syncExec(new Runnable() {
            public void run()
            {
                JdiThreadNode jtn = treeModel.findThreadNode(tr);
                if (jtn != null) {
                    treeModel.removeNodeFromParent(jtn);
                }
            }
        });
    }

    /**
     * Set or clear the option to hide system threads. This method also updates
     * the current display if necessary.
     */
    public void hideSystemThreads(boolean hide)
    {
        if (hideSystemThreads == hide)
            return;

        hideSystemThreads = hide;
        updateThreadDisplay();
    }

    /**
     * Re-build the treeModel for the currently displayed threads using the
     * allThreads set and the 'hideSystemThreads' flag.
     */
    private void updateThreadDisplay()
    {
        treeModel.setRoot(new JdiThreadNode());

        for (Iterator<JdiThread> it = allThreads.iterator(); it.hasNext();) {
            JdiThread currentThread = (JdiThread) it.next();
            displayThread(currentThread);
        }

        treeModel.reload();
    }

    /**
     * Add the given thread to the displayed threads if appropriate. System
     * threads are displayed conditional on the 'hideSystemThreads' flag.
     */
    private void displayThread(JdiThread newThread)
    {
        if (!hideSystemThreads || !newThread.isKnownSystemThread()) {
            JdiThreadNode root = treeModel.getThreadRoot();
            treeModel.insertNodeInto(new JdiThreadNode(newThread), root, 0);
        }
    }

    // -- support methods --

    /**
     * Get the VM, waiting for it to finish loading first (if necessary). In
     * rare cases, when the project has been closed, this may return null.
     * 
     * @return the VM reference.
     */
    private VMReference getVM()
    {
        MachineLoaderThread mlt = machineLoader;
        if (mlt == null) {
            return null;
        }
        else {
            return mlt.getVM();
        }
    }
    
    /**
     * Get the VM if available, but don't wait for it (to finish loading).
     * 
     * @return  The VMReference or null if it's not available.
     */
    private VMReference getVMNoWait()
    {
        // Store a single value of machineLoader in a local variable to avoid
        // synchronization issues.
        MachineLoaderThread mlt = machineLoader;
        if (mlt == null)
            return null;
        else
            return mlt.getVMNoWait();
    }

    /**
     * A thread which loads a new instance of the debugger.
     */
    class MachineLoaderThread extends Thread
    {
        MachineLoaderThread()
        {}

        public void run()
        {
            try {
                VMReference newVM = new VMReference(JdiDebugger.this, terminal, startingDirectory, libraries);

                BPClassLoader lastLoader;
                synchronized(JdiDebugger.this) {
                    if (! autoRestart) {
                        newVM.close();
                        JdiDebugger.this.notifyAll();
                        return;
                    }
                    lastLoader = lastProjectClassLoader;
                }
                
                // Do this outside of the synchronized blocks, mainly to avoid holding
                // the monitor unnecessarily:
                newVM.newClassLoader(lastLoader.getURLs());

                synchronized(JdiDebugger.this) {
                    vmRef = newVM;
                }
            }
            catch (JdiVmCreationException e) {
                raiseStateChangeEvent(Debugger.LAUNCH_FAILED);
            }

            // wake any internal getVM() calls that
            // are waiting for us to finish
            synchronized(JdiDebugger.this) {
                JdiDebugger.this.notifyAll();
            }
        }

        private VMReference getVM()
        {
            synchronized (JdiDebugger.this) {
                // We can't just rely on synchronization, since it's possible that
                // getVM() may creep in before run() begins execution. That's why
                // we use notify()/wait().
                while (vmRef == null && autoRestart) {
                    try {
                        JdiDebugger.this.wait();
                    }
                    catch (InterruptedException e) {}
                }
                    
                return vmRef;
            }
        }
        
        /**
         * Get the VM reference, without waiting for it to start. If no VM has started,
         * this returns null.
         */
        private VMReference getVMNoWait()
        {
            synchronized (JdiDebugger.this) {
                return vmRef;
            }
        }
    }

    /**
     * Emit an event (to listeners) due to a thread being halted.
     */
    void emitThreadHaltEvent(JdiThread thread)
    {
        vmRef.emitThreadEvent(thread, true);
    }
    
    /**
     * Emit an event (to listeners) due to a thread being resumed.
     */
    void emitThreadResumedEvent(JdiThread thread)
    {
        vmRef.emitThreadEvent(thread, false);
    }
    
    /**
     * A thread has become halted; inform listeners.
     */
    void threadHalted(final JdiThread thread)
    {
        DebuggerEvent event = new DebuggerEvent(this, DebuggerEvent.THREAD_HALT_UNKNOWN, thread, null);
        
        boolean skipUpdate = false;
        // Guaranteed to return a non-null array
        DebuggerListener[] listeners = getListeners();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1; i >= 0; i--) {
            skipUpdate |= listeners[i].examineDebuggerEvent(event);
        }
        
        if (! skipUpdate) {
            treeModel.syncExec(new Runnable() {
                public void run()
                {
                    JdiThreadNode threadNode = treeModel.findThreadNode(thread.getRemoteThread());
                    if (threadNode != null) {
                        treeModel.nodeChanged(threadNode);
                    }
                }
            });
        }
        
        fireTargetEvent(event, skipUpdate);
    }
    
    /**
     * A thread has been resumed; inform listeners.
     */
    void threadResumed(final JdiThread thread)
    {
        DebuggerEvent event = new DebuggerEvent(this, DebuggerEvent.THREAD_CONTINUE, thread, null);
        
        boolean skipUpdate = false;
        // Guaranteed to return a non-null array
        DebuggerListener [] listeners = getListeners();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            skipUpdate |= listeners[i].examineDebuggerEvent(event);
        }

        if (! skipUpdate) {
            treeModel.syncExec(new Runnable() {
                public void run()
                {
                    JdiThreadNode threadNode = treeModel.findThreadNode(thread.getRemoteThread());
                    if (threadNode != null) {
                        treeModel.nodeChanged(threadNode);
                    }
                }
            });
        }
        
        fireTargetEvent(event, skipUpdate);
    }

    /**
     * The server thread has been resumed: updated its internal isSuspended status,
     * but no need to fire listeners
     * @param serverThread
     */
    public void serverThreadResumed(ThreadReference serverThread)
    {
        allThreads.find(serverThread).notifyResumed();
    }
}
