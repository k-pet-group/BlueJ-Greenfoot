package bluej.debugger.jdi;

import java.io.File;
import java.util.*;

import javax.swing.event.EventListenerList;

import bluej.Config;
import bluej.debugger.*;
import bluej.debugmgr.Invoker;
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
 * @version $Id: JdiDebugger.java 2039 2003-06-19 06:03:24Z ajp $
 */
public class JdiDebugger extends Debugger
{
	// the synch object for loading.
	// See the inner class MachineLoaderThread (at the bottom)
	volatile private boolean vmReady = false;

	// the reference to the current remote VM handler
	private VMReference vmRef;
	
	// the thread that we spawned to load the current remote VM
	private MachineLoaderThread machineLoader;

	// a TreeModel exposing all the JdiThreads in the VM
	private JdiThreadTreeModel treeModel;
	
	// listeners for events that occur in the debugger	
	private EventListenerList listenerList = new EventListenerList();

	// the directory to launch the VM in
	private File startingDirectory;
	
	// a Set of strings which have been used as names on the
	// object bench. We endeavour to not reuse them.
	private Set usedNames;
	
	/**
	 * Construct an instance of the debugger.
	 *
	 * This constructor should not be used by the main part of BlueJ. Access
	 * should be through Debugger.getDebuggerImpl().
	 *  
	 * @param startingDirectory  a File representing the directory
	 *                           we should launch the debug VM in.
	 */	
    public JdiDebugger(File startingDirectory)
    {
		this.startingDirectory = startingDirectory;
		
		treeModel = new JdiThreadTreeModel(new JdiThreadNode());
		usedNames = new TreeSet();
    }

	/**
	 * Start debugging.
	 */
	public void launch()
	{
		raiseStateChangeEvent(Debugger.NOTREADY);

		if (vmReady)
			throw new IllegalStateException("JdiDebugger.launch() was called but the debugger was already loaded");

		// start the MachineLoader (a separate thread) to load the
		// remote virtual machine in the background
		machineLoader = new MachineLoaderThread();
		// lower priority to improve GUI response time
		machineLoader.setPriority(Thread.currentThread().getPriority() - 1);
		machineLoader.start();	
	}
	
	/**
	 * Finish debugging.
	 */
	public void close()
	{	
		raiseStateChangeEvent(Debugger.NOTREADY);

		vmReady = false;

		treeModel.setRoot(new JdiThreadNode());
		treeModel.reload();
		usedNames.clear();

		// kill the remote debugger process
		vmRef.close();
		vmRef = null;

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
		if (!vmReady)
			return Debugger.NOTREADY;
		else
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
		// a list of Location's representing a temporarily
		// saved list of breakpoints we want to keep
		List savedBreakpoints;

		savedBreakpoints = getVM().getBreakpoints();
		newClassLoader(classPath);
		getVM().restoreBreakpoints(savedBreakpoints);
	}


    /**
     * Add an object to a package scope.
     */
    public boolean addObject(String newInstanceName, DebuggerObject dob)
    {
        Object args[] = { newInstanceName, ((JdiObject)dob).getObjectReference() };

		getVM().invokeExecServer( ExecServer.ADD_OBJECT, Arrays.asList(args));
		
		usedNames.add(newInstanceName);
		
		return true;
    }

    /**
     * Remove an object from a package scope (when removed from object bench).
     */
    public void removeObject(String instanceName)
    {
		if (!vmReady)
			return;
    		
        Object args[] = { instanceName };

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

	public Map getObjects()
	{
		// the returned array consists of double the number of objects
		// they alternate, name, object, name, object
		// ie.
		// arrayRef[0] = a field name 0 (StringReference)
		// arrayRef[1] = a field value 0 (ObjectReference)
		// arrayRef[2] = a field name 1 (StringReference)
		// arrayRef[3] = a field value 1 (ObjectReference)
		//

		return null;
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
		if (!vmReady)
			return;

		getVM().invokeExecServer(ExecServer.DISPOSE_WINDOWS, Collections.EMPTY_LIST);
    }

    /**
     * Supress error output on the remote machine.
     */
    public void supressErrorOutput()
    {
		if (!vmReady)
			return;

		getVM().invokeExecServer( ExecServer.SUPRESS_OUTPUT, Collections.EMPTY_LIST );
    }

    /**
     * Restore error output on the remote machine.
     */
    public void restoreErrorOutput()
    {
		if (!vmReady)
			return;

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
	public void runClassMain(String className)
		throws ClassNotFoundException
	{
		getVM().runShellClass(className);
	}

    /**
     * Get a class from the virtual machine.
     * Return null if the class could not be found.
     */
    public DebuggerClass getClass(String className)
		throws ClassNotFoundException
    {
		ReferenceType classMirror = getVM().loadClass(className);

		return new JdiClass(classMirror);
    }

    /**
     * Get the value of a static field in a class.
     * Return null if the field or class could not be found.
     */
    public DebuggerObject getStaticValue(String className, String fieldName)
    	throws ClassNotFoundException
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

	public void addDebuggerListener(DebuggerListener l)
	{
		listenerList.add(DebuggerListener.class, l);
	}

	public void removeDebuggerListener(DebuggerListener l)
	{
		listenerList.remove(DebuggerListener.class, l);
	}

	// notify all listeners that have registered interest for
	// notification on this event type.
	private void fireTargetEvent(DebuggerEvent ce)
	{
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i] == DebuggerListener.class) {
				((DebuggerListener)listeners[i+1]).debuggerEvent(ce);
			}
		}
	}

	void raiseStateChangeEvent(int newState)
	{
		fireTargetEvent(new DebuggerEvent(this, DebuggerEvent.DEBUGGER_STATE, newState));
	}

    // ==== code for active debugging: setting breakpoints, stepping, etc ===

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
                return getVM().setBreakpoint(className, line);
            }
            else {
                return getVM().clearBreakpoint(className, line);
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

	public void halt(ThreadReference tr)
	{
		JdiThreadNode jtn = treeModel.findThreadNode(tr);

		if (jtn != null) {
			treeModel.nodeChanged(jtn);		

			fireTargetEvent(new DebuggerEvent(this,
												 DebuggerEvent.THREAD_HALT,
												 new JdiThread(treeModel, tr)));
		}
		else {
			Debug.message("Received HALT for unknown thread " + tr);
		}
	}
	
	public void breakpoint(ThreadReference tr)
	{
		JdiThreadNode jtn = treeModel.findThreadNode(tr);

		if (jtn != null) {
			treeModel.nodeChanged(jtn);		

			fireTargetEvent(new DebuggerEvent(this,
												 DebuggerEvent.THREAD_BREAKPOINT,
												 new JdiThread(treeModel, tr)));
		}
		else {
			Debug.message("Received breakpoint for unknown thread " + tr);
		}

		/*JdiThread thread = new JdiThread(null, remoteThread, executionUserParam);
		if (thread.getClassSourceName(0).startsWith("__SHELL")) {
			// stepped out into the shell class - resume to finish
			machine.resume();
		} else {
			if (breakpoint)
				BlueJEvent.raiseEvent(BlueJEvent.BREAKPOINT, thread);
			else
				BlueJEvent.raiseEvent(BlueJEvent.HALT, thread);
		} */
	}


	/**
	 * List all threads being debugged as a TreeModel.
	 *
	 * @return  A tree model of all the threads.
	 */
	public DebuggerThreadTreeModel getThreadTreeModel()
	{
		return treeModel; 
	}
	
	void threadStart(ThreadReference tr)
	{
		if (treeModel == null)
			return;

		synchronized(treeModel.getRoot()) {
			JdiThreadNode root = treeModel.findThreadNode(tr.threadGroup());
			
			if (root == null) {
				// System.out.println("unknown thread group " + tr.threadGroup());
				root = treeModel.getThreadRoot();
			}
						
			treeModel.insertNodeInto(new JdiThreadNode(new JdiThread(treeModel, tr)), root, 0);
		}
	}

	void threadDeath(ThreadReference tr)
	{
		if (treeModel == null)
			return;
			
		synchronized(treeModel.getRoot()) {
			JdiThreadNode jtn = treeModel.findThreadNode(tr);
		
			if (jtn != null) {
				treeModel.removeNodeFromParent(jtn);
			}
		}
	}

    // -- support methods --

    public void dumpThreadInfo()
    {
    	getVM().dumpThreadInfo();
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

			vmRef = new VMReference(JdiDebugger.this, startingDirectory);
			vmRef.waitForStartup();
		
			vmReady = true;
	
			newClassLoader(startingDirectory.getAbsolutePath());
				
			notifyAll();	// wake any internal getVM() calls that
							// are waiting for us to finish

			raiseStateChangeEvent(Debugger.IDLE);

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
