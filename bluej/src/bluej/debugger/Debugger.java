/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.debugger;

import java.io.File;
import java.util.Map;

import bluej.classmgr.BPClassLoader;
import bluej.debugger.jdi.JdiDebugger;

/**
 * A class defining the debugger primitives needed by BlueJ
 * May be supported by different implementations, locally or remotely.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: Debugger.java 6163 2009-02-19 18:09:55Z polle $
 */
public abstract class Debugger
{
    public static final int NORMAL_EXIT = 0;
    public static final int FORCED_EXIT = 1;
    public static final int EXCEPTION = 2;
    public static final int TERMINATED = 3;

    // machine states
    public static final int UNKNOWN = 0;	// cannot move to this state,
    										// but this can be the oldState in an event
    public static final int NOTREADY = 1;
    public static final int IDLE = 2;
	public static final int RUNNING = 3;
    public static final int SUSPENDED = 4;
    public static final int LAUNCH_FAILED = 5; // failed to launch

	/**
	 * Create an instance of a debugger.
	 * The constructor for the debugger should not be
	 * a long process. Actual startup for the debug
	 * VM should go in launch().
	 * 
	 * @return  a Debugger instance
	 */
	public static Debugger getDebuggerImpl(File startingDirectory, DebuggerTerminal terminal)
	{
        return new JdiDebugger(startingDirectory, terminal);
	}
	
	/**
     * Start debugging.
     * 
     * This can be a lengthy process so this should be executed
     * in a sub thread.
     */
    public abstract void launch();

    /**
     * Finish debugging.
     */
    public abstract void close(boolean restart);

	/**
	 * Add a listener for DebuggerEvents
	 * 
	 * @param l  the DebuggerListener to add
	 */
	public abstract void addDebuggerListener(DebuggerListener l);

	/**
	 * Remove a listener for DebuggerEvents.
	 * 
	 * @param l  the DebuggerListener to remove
	 */
	public abstract void removeDebuggerListener(DebuggerListener l);
	
    /**
     * Create a class loader in the debugger.
     */
    public abstract void newClassLoader(BPClassLoader bpClassLoader);

    /**
     * Remove all breakpoints in the given class.
     */
    public abstract void removeBreakpointsForClass(String className);
	
    /**
     * Add a debugger object into the project scope.
     * 
     * @param   scopeId          the scope identifier
     * @param   newInstanceName  the name of the object
     * @param   dob              the object itself
     * @return  true if the object could be added with this name,
     *          false if there was a name clash.
     */
    public abstract boolean addObject(String scopeId, String newInstanceName, DebuggerObject dob);

    /**
     * Remove a debugger object from the project scope.
     */
    public abstract void removeObject(String scopeId, String instanceName);

	/**
	 * Return the debugger objects that exist in the
	 * debugger.
	 * 
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
	public abstract Map getObjects();
	
	/**
	 * Guess a suitable name for an object about to be put on the object bench.
	 * 
	 * @param	startingName  a fully qualified class name (will be stripped of
	 *                        qualifying part) or a field name that will be used
	 *                        as the basis for the new name.
	 * @return				  a String suitable as a name for an object on the
	 * 						  object bench. 
	 */
	public abstract String guessNewName(String className);
	
	/**
     * Guess a suitable name for an object about to be put on the object bench.
     * 
     * @param obj
     *            the object that will be put on the object bench
     * @return a String suitable as a name for an object on the object bench.
     */
    public abstract String guessNewName(DebuggerObject obj);


    /**
     * Return the machine status; one of the "machine state" constants:
     * (IDLE, RUNNING, SUSPENDED).
     */
    public abstract int getStatus();
    
	/**
	 * Run the setUp() method of a test class and return the created
	 * objects.
	 * 
	 * @param className	the fully qualified name of the class
	 * @return			a Map of (String name, DebuggerObject obj) entries
	 */
    public abstract Map runTestSetUp(String className);

	/**
	 * Run a single test method in a test class and return the result.
	 * 
	 * @param  className  the fully qualified name of the class
	 * @param  methodName the name of the method
	 * @return            a DebuggerTestResult object
	 */
    public abstract DebuggerTestResult runTestMethod(String className, String methodName);

	//	BeanShell
    //public abstract DebuggerObject executeCode(String code);
    
    /**
     * Dispose all top level windows in the remote machine.
     */
    public abstract void disposeWindows();

    /**
     * "Run" a class (i.e. invoke its main method without arguments)
     */
    public abstract DebuggerResult runClassMain(String className)
    	throws ClassNotFoundException;

    /**
     * Instantiate a class using the default constructor for that class.
     * @param className  The name of the class to instantiate
     * @return   The result of the constructor call
     */
    public abstract DebuggerResult instantiateClass(String className);

    /**
     * Instantiate a class using a specific constructor for that class.
     * 
     * @param className  The name of the class to instantiate
     * @param argTypes   The formal parameter types (class names)
     * @param args       The arguments
     * @return   The result of the constructor call
     */
    public abstract DebuggerResult instantiateClass(String className, String [] paramTypes, DebuggerObject [] args);
    
    /**
     * Get a class from the virtual machine, using the current classloader. The class will be
     * initialized if possible. This can cause execution of user code.
     */
    public abstract DebuggerClass getClass(String className)
		throws ClassNotFoundException;

    /**
     * Get the value of a static field in a class
     */
    public abstract DebuggerObject getStaticValue(String className, String fieldName)
		throws ClassNotFoundException;
    
    /**
     * Get a reference to a string in the remote machine whose value is the
     * same as the given value. Returns null if the remote VM terminates
     * or the string cannot be mirrored for some other reason (such as
     * out of memory).
     * 
     * @param value  The string value to mirror
     * @return       The remote object with the same value, or null
     */
    public abstract DebuggerObject getMirror(String value);

    /**
     * Set/clear a breakpoint at a specified line in a class.
     *
     * @param className  The class in which to set the breakpoint.
     * @param line       The line number of the breakpoint.
     * @param set        True to set, false to clear a breakpoint.
     * @return           a string of the error message generated performing
     *                   this operation or null
     */
    public abstract String toggleBreakpoint(String className, int line,
                                            boolean set);

    /**
     * A tree model representing the threads running in
     * the debug VM.
     *  
     * @return  a TreeModel with DebuggerThread objects
     *          as the leaves.
     */
    public abstract DebuggerThreadTreeModel getThreadTreeModel();

    /**
     * Set or clear the option to hide system threads.
     * This method also updates the current display if necessary.
     */
    public abstract void hideSystemThreads(boolean hide);

}
