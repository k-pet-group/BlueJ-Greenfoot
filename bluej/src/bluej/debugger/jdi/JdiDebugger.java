package bluej.debugger.jdi;

import java.io.File;
import java.util.*;

import bluej.*;
import bluej.classmgr.ClassMgr;
import bluej.debugger.*;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.runtime.ExecServer;
import bluej.utility.*;

import com.sun.jdi.*;

/**
 * A class implementing the execution and debugging primitives needed by
 * BlueJ.
 * 
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: JdiDebugger.java 1991 2003-05-28 08:53:06Z ajp $
 */
public class JdiDebugger extends Debugger
{
	// the synch object for loading.
	// See the inner class MachineLoaderThread (at the bottom)
	volatile private boolean vmReady = false;

	private VMReference vmRef;
	private MachineLoaderThread machineLoader;
	
	private File startingDirectory; // needed on restart()
	private Set usedNames;
	
    public JdiDebugger(File startingDirectory)
    {
        super();

		this.startingDirectory = startingDirectory;
		usedNames = new TreeSet();
    }

	public void launch()
	{
//		PkgMgrFrame.displayMessage(p, Config.getString("pkgmgr.creatingVM"));
//		PkgMgrFrame.displayMessage(p, Config.getString("pkgmgr.creatingVMDone"));
		vmReady = false;

		// start the MachineLoader (a separate thread) to load the
		// remote virtual machine in the background
		machineLoader = new MachineLoaderThread();
		// lower priority to improve GUI response time
		machineLoader.setPriority(Thread.currentThread().getPriority() - 1);
		machineLoader.start();	
	}
	
	public void close()
	{	
		vmReady = false;
		vmRef.close();
		vmRef = null;
	}

	public void restart()
	{
		close();
		launch();	
	}
	
	private VMReference getVM()
	{
		return machineLoader.getVM();
	}
	
	/**
	 * Return the machine status; one of the "machine state" constants:
	 * (IDLE, RUNNING, SUSPENDED).
	 */
	public int getStatus()
	{
		return getVM().getStatus();
	}

	/**
	 * Guess a suitable name for an object about to be put on the object bench.
	 * 
	 * @param	className	the fully qualified name of the class of object
	 * @return				a String suitable as a name for an object on the
	 * 						object bench. 
	 */
	public String guessNewName(String className)
	{
		String baseName = JavaNames.getBase(className);

		// truncate long names to  OBJ_NAME_LENGTH plus _instanceNum
		int stringEndIndex =
			baseName.length() > Invoker.OBJ_NAME_LENGTH ? Invoker.OBJ_NAME_LENGTH : baseName.length();

		String newName = Character.toLowerCase(baseName.charAt(0)) +
			baseName.substring(1, stringEndIndex);

		int num = 1;
		
		while(usedNames.contains(newName + "_" + num))
			num++;
			
		return newName + "_" + num;
	}
	
	/**
	 * Create a class loader in the debugger.
	 */
    public void newClassLoader(String classPath)
    {
		usedNames.clear();

		getVM().newClassLoader(classPath);
    }

	/**
	 * Create a class loader in the debugger but retain
	 * any user created breakpoints.
	 */
	public void newClassLoaderLeavingBreakpoints(String classPath)
	{
		List savedBreakpoints;	// a list of Location's representing
								// a temporarily saved list of breakpoints
								// we want to keep

		savedBreakpoints = getVM().getBreakpoints();
		newClassLoader(classPath);
		getVM().restoreBreakpoints(savedBreakpoints);
	}


    /**
     * Add an object to a package scope. The object is held in field
     * 'fieldName' in object 'instanceName'.
     */
    public void addObjectToScope(String scopeId, String newObjectName,
                                    DebuggerObject job)
    {
        Object args[] = { scopeId, newObjectName, ((JdiObject)job).getObjectReference() };

		getVM().invokeExecServer( ExecServer.ADD_OBJECT, Arrays.asList(args));
		
		usedNames.add(newObjectName);
    }

    /**
     * Remove an object from a package scope (when removed from object bench).
     * This has to be done tolerantly: If the named instance is not in the
     * scope, we just quietly return.
     */
    public void removeObjectFromScope(String scopeId, String instanceName)
    {
        Object args[] = { scopeId, instanceName };

		getVM().invokeExecServer( ExecServer.REMOVE_OBJECT, Arrays.asList(args) );
    }

    /**
     * Set the class path of the remote VM
     */
    public void setLibraries(String classpath)
    {
        Object args[] = { classpath };

		getVM().invokeExecServer( ExecServer.SET_LIBRARIES, Arrays.asList(args));
    }

	/**
	 */
    public Map runTestSetUp(String className)
    {
        Object args[] = { className };

        ArrayReference arrayRef = (ArrayReference) getVM().invokeExecServer(ExecServer.RUN_TEST_SETUP, Arrays.asList(args));
       
        // the returned array consists of double the number of fields created by running test setup
        // they alternate, fieldname, fieldvalue, fieldname, fieldvalue
        // ie.
        // arrayRef[0] = a field name 0 (StringReference)
        // arrayRef[1] = a field value 0 (ObjectReference)
        // arrayRef[2] = a field name 1 (StringReference)
        // arrayRef[3] = a field value 1 (ObjectReference)
        //
        // we could return a Map from RUN_TEST_SETUP but then we'd have to use JDI
        // reflection to make method calls on Map in order to extract the values
        Map returnMap = new HashMap();

        if (arrayRef != null) {
            for(int i=0; i<arrayRef.length(); i+=2)
                returnMap.put(((StringReference) arrayRef.getValue(i)).value(),
                                JdiObject.getDebuggerObject((ObjectReference)arrayRef.getValue(i+1)));
        }

        // the resulting map consists of entries (String fieldName, JdiObject obj)
        return returnMap;
    }

	/**
	 */
    public DebuggerTestResult runTestMethod(String className, String methodName)
    {
        Object args[] = { className, methodName };

		ArrayReference arrayRef = (ArrayReference) getVM().invokeExecServer(ExecServer.RUN_TEST_METHOD, Arrays.asList(args));
      
        if (arrayRef != null && arrayRef.length() > 2) {
			String failureType = ((StringReference) arrayRef.getValue(0)).value();
			String exMsg = ((StringReference) arrayRef.getValue(1)).value();
			String traceMsg = ((StringReference) arrayRef.getValue(2)).value();

			if (failureType.equals("failure"))
				return new JdiTestResultFailure(className, methodName, exMsg, traceMsg);
			else
				return new JdiTestResultError(className, methodName, exMsg, traceMsg);
        }
		
		// a null means that we had success. Return a success test result
		return new JdiTestResult(className, methodName);
    }    

    /**
     * Dispose all top level windows in the remote machine.
     */
    public void disposeWindows()
    {
		getVM().invokeExecServer(ExecServer.DISPOSE_WINDOWS, Collections.EMPTY_LIST);
    }

    /**
     * Supress error output on the remote machine.
     */
    public void supressErrorOutput()
    {
		getVM().invokeExecServer( ExecServer.SUPRESS_OUTPUT, Collections.EMPTY_LIST );
    }

    /**
     * Restore error output on the remote machine.
     */
    public void restoreErrorOutput()
    {
		getVM().invokeExecServer( ExecServer.RESTORE_OUTPUT, Collections.EMPTY_LIST );
    }

	/**
	 * "Start" a class (i.e. invoke its main method)
	 *
	 * @param classname		the class to start
	 * @param eventParam	when a BlueJEvent is generated for a
	 *				breakpoint, this parameter is passed as the
	 *				event parameter
	 */
	public void runClassMain(String className, Object eventParam)
	{
		getVM().runShellClass(className, eventParam);
	}

    /**
     * Get a class from the virtual machine.
     * Return null if the class could not be found.
     */
    public DebuggerClass getClass(String className)
    {
		try {
			ReferenceType classMirror = getVM().loadClass(className);

			return new JdiClass(classMirror);
		}
		catch (ClassNotFoundException cnfe) {
			return null;
		}
    }

    /**
     * Get the value of a static field in a class.
     * Return null if the field or class could not be found.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
    {
		//Debug.message("[getStaticValue] " + className + ", " + fieldName);
		ObjectReference ob = getVM().getStaticValue(className, fieldName);
		
		if (ob != null)
			return JdiObject.getDebuggerObject(ob);
		else
			return null;
    }

    /**
     * Return the status of the last invocation. One of (NORMAL_EXIT,
     * FORCED_EXIT, EXCEPTION, TERMINATED).
     */
    public int getExitStatus()
    {
        return getVM().getExitStatus();
    }


    /**
     * Return the text of the last exception.
     */
    public ExceptionDescription getException()
    {
        return getVM().getException();
    }

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

    /**
     * Set a breakpoint at a specified line in a class.
     *
     * @param   className  The class in which to set the breakpoint.
     * @param   line       The line number of the breakpoint.
     * @return  null if there was no problem, or an error string
     */
    private String setBreakpoint(String className, int line)
        throws AbsentInformationException
    {
    	return getVM().setBreakpoint(className, line);
    }

    /**
     * Clear all the breakpoints at a specified line in a class.
     *
     * @param   className  The class in which to clear the breakpoints.
     * @param   line       The line number of the breakpoint.
     * @return  null if there was no problem, or an error string
     */
    private String clearBreakpoint(String className, int line)
        throws AbsentInformationException
    {
		return getVM().clearBreakpoint(className, line);
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
    public String toggleBreakpoint(String className, int line, boolean set)
    {
        //Debug.message("[toggleBreakpoint]: " + className);

        try {
            if(set) {
                return setBreakpoint(className, line);
            }
            else {
                return clearBreakpoint(className, line);
            }
        }
        catch(AbsentInformationException e) {
            return Config.getString("debugger.jdiDebugger.noLineNumberMsg");
        }
        catch(Exception e) {
            Debug.reportError("breakpoint error: " + e);
            return Config.getString("debugger.jdiDebugger.internalErrorMsg");
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
    	return getVM().listThreads();
    }

    /**
     *  A thread has been stopped.
     */
    public void halt(DebuggerThread thread)
    {
    	getVM().halt(thread);
        BlueJEvent.raiseEvent(BlueJEvent.HALT, thread);
    }

    /**
     * A thread has been started again by the user. Make sure that it
     * is indicated in the interface.
     */
    public void cont()
    {
		getVM().cont();
		BlueJEvent.raiseEvent(BlueJEvent.CONTINUE, null);
    }

    /**
     * Arrange to show the source location for a specific frame number
     * of a specific thread. The currently selected frame is stored in the
     * thread object itself.
     */
    public void showSource(DebuggerThread thread)
    {
        getVM().showSource(thread);
        BlueJEvent.raiseEvent(BlueJEvent.SHOW_SOURCE, thread);
    }

/*
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
*/
    // -- support methods --

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
	 * A thread which loads a new instance of the debugger.
	 */
	class MachineLoaderThread extends Thread
	{
		 MachineLoaderThread() { }
 
		 public synchronized void run()
		 {
			PkgMgrFrame.displayMessage(Config.getString("pkgmgr.creatingVM"));

			vmRef = new VMReference(startingDirectory); // File projectDir
			vmRef.waitForStartup();
		
			vmReady = true;
	
			newClassLoader(startingDirectory.getAbsolutePath());
			setLibraries(ClassMgr.getClassMgr().getAllClassPath().toString());
				
			notifyAll();	// wake any internal getVM() calls that
							// are waiting for us to finish


			PkgMgrFrame.displayMessage(Config.getString("pkgmgr.creatingVMDone"));

		 }
		 
		private synchronized VMReference getVM()
		{
			while(!vmReady) 
				try {
					wait();
				}
			catch(InterruptedException e) { }

			return vmRef;
		}
	} 

}
