package bluej.runtime;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import junit.framework.*;
import bluej.utility.Debug;

/**
 * Class that controls the runtime of code executed within BlueJ.
 * Sets up a SecurityManager, initial thread state, etc.
 *
 * This class both holds runtime attributes and executes commands.
 * Execution is done through JDI reflection from the JdiDebugger class.
 *
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: ExecServer.java 2026 2003-06-11 07:55:32Z ajp $
 */
public class ExecServer
{
    // Task type constants (these constants must match the name of the
    // corresponding methods in this ExecServer source). Methods to call are
    // obtained using reflection by JdiDebugger (using these strings).

    public static final String NEW_LOADER       = "newLoader";
    public static final String LOAD_CLASS       = "loadClass";
    public static final String ADD_OBJECT       = "addObject";
    public static final String REMOVE_OBJECT    = "removeObject";
    public static final String SET_LIBRARIES    = "setLibraries";
    public static final String RUN_TEST_SETUP   = "runTestSetUp";
    public static final String RUN_TEST_METHOD  = "runTestMethod";
    public static final String SUPRESS_OUTPUT   = "supressOutput";
    public static final String RESTORE_OUTPUT   = "restoreOutput";
    public static final String DISPOSE_WINDOWS  = "disposeWindows";

    /*package*/ static ExitException exitExc = new ExitException("0");

    private static RemoteClassMgr classmgr;
	private static ClassLoader currentLoader;	// the current loader
	
    private static Map scopes = new HashMap();

    /**
     * We need to keep track of open windows so that we can dispose of them
     * when simulating a System.exit() call
     */
    private static List openWindows = Collections.synchronizedList(new LinkedList());
    private static boolean disposingAllWindows = false; // true while we are dsposing

    private static PrintStream systemErr = System.err;
    private static ByteArrayOutputStream throwawayErr = null;

    /**
     * Main method.
     *
     */
    public static void main(String[] args)
        throws Throwable
    {
		//Debug.message("[VM] creating server object");

		classmgr = new RemoteClassMgr();

		// the following causes the class loader mechanism to be initialised:
		// we attempt to load a (non-existent) class
		try {
			newLoader(".");
			loadClass("Dummy");
			currentLoader = null;
		}
		catch(Exception e) {
			// ignore - we will get a ClassNotFound exception here
		}

		// register a listener to record all window opens and closes
		Toolkit toolkit = Toolkit.getDefaultToolkit();

		AWTEventListener listener = new AWTEventListener()
		{
			public void eventDispatched(AWTEvent event)
			{
				if(event.getID() == WindowEvent.WINDOW_OPENED) {
					addWindow(event.getSource());
				} else if(event.getID() == WindowEvent.WINDOW_CLOSED) {
					removeWindow(event.getSource());
				}
			}
		};

		toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);

		// we create the security manager last so that hopefully, all the system/AWT
		// threads will have been created and we can then rig our security manager
		// to make all user-created threads go into a single thread group
		System.setSecurityManager(new RemoteSecurityManager());

		// signal with a breakpoint that we have performed out VM initialisation
		vmStarted();
		
		int count = 0;
		
		// an infinite loop.. 
		while(count++ < 1000000) {
			vmSuspend();
		}
		
		System.err.println("bye bye");
    }

    /**
     *  This method is used to generate an event which is recorded
     *  by the local VM when handling System.exit().
     *
     *  See RemoteSecurityManager for details.
     */
    public static void exitMarker()
    {
        // <NON SUSPENDING BREAKPOINT!>
    }

	/**
	 * This method is used to suspend the execution of the
	 * machine to indicate that everything is up and running.
	 */
	public static void vmStarted()
	{
		// <SUSPENDING BREAKPOINT!>
	}

	/**
	 * This method is used to suspend the execution of this server thread.
	 * This is done via a breakpoint: a breakpoint is set in this method
	 * so calling this method suspends execution.
	 */
	public static void vmSuspend()
	{
		// <SUSPENDING BREAKPOINT!>
	}

    /**
     * Add the object to our list of open windows
     *
     * @param   o   a window object which has just been opened
     */
    private static void addWindow(final Object o)
    {
        openWindows.add(o);
    }

    /**
     * Remove the object from our list of open windows
     *
     * @param   o   a window object which has just been closed
     */
    private static void removeWindow(Object o)
    {
        if(!disposingAllWindows)   // don't bother if we are clearing up just now
            openWindows.remove(o);
    }

    /**
     * Find a scoping Map for the given scopeId
     */
    static Map getScope()
    {
        //Debug.message("[VM] getScope" + scopeId);
        //Map scope = (Map)scopes.get(scopeId);

        //if(scope == null) {
        //    scope = new HashMap();
        //    scopes.put(scopeId, scope);
       // }
        return scopes;
    }

    // -- methods called by reflection from JdiDebugger --
    // --
    // -- methods that can be made private have been, as
    // -- the reflection is still able to access them
    // -- (RemoteSecurityManager switches all reflection
    // --  access checks off)

    /**
     * Create a new class loader for a given classpath.
     */
    private static ClassLoader newLoader(String classPath)
    {
        //Debug.message("[VM] newLoader " + classPath);
    	currentLoader = classmgr.getLoader(classPath);

		return currentLoader;
    }

    /**
     * Load (and prepare) a class in the remote runtime.
     */
    private static Class loadClass(String className)
        throws ClassNotFoundException
    {
    	//Debug.message("[VM] loadClass: " + className);
		Class cl = null;
		
		if (currentLoader == null) {
			cl = classmgr.getLoader().loadClass(className);	
		}
		else {
			cl = currentLoader.loadClass(className);	
		}

        // run the initialisation ("prepare" method) of the class.
        // For our SHELL clasees, this is important.
        // This guarantees that the class is properly prepared, as well as
        // executing some init code in that shell method.
	    try {
	        Method m = cl.getMethod("prepare", null);
	        m.invoke(null, null);
	    } catch(Exception e) {
	        // ignore - some classes don't have prepare method. attempt to
	        // call will still prepare the class
	    }

        return cl;
    }

    /**
     * Add an object into a package scope (for possible use as parameter
     * later). Used after object creation to add the newly created object
     * to the scope.
     *
     * Must be static because it is used by Shell without a execServer reference
     */
    static void addObject(String instanceName, Object value)
    {
        // Debug.message("[VM] addObject: " + instanceName + " " + value);
        Map scope = getScope();
        scope.put(instanceName, value);
    }

    /**
     * Update the remote VM with the list of user/system libraries
     * which the user has created using the ClassMgr.
     */
    private static void setLibraries(String libraries)
    {
        classmgr.setLibraries(libraries);
    }

    /**
     * Execute a JUnit test case setUp method.
     * 
     * @return  an array consisting of String, Object pairs. For n fixture objects
     *          there will be n*2 entries in the array. Putting it in an array saves
     *          having to make lots of reflective List and HashMap calls on the
     *          calling virtual machine. Once the calling VM gets this array it can
     *          put it into a more suitable data structure itself.
     */
    private static Object[] runTestSetUp(String className)
        throws ClassNotFoundException
    {
		Class cl = loadClass(className);
        
        try {
            // construct an instance of the test case (firstly trying the
            // String argument constructor - then the no-arg constructor)
            Object testCase = null;

            Class partypes[] = new Class[1];
            partypes[0] = String.class;
            try {
                Constructor ct = cl.getConstructor(partypes);

                Object arglist[] = new Object[1];
                arglist[0] = "TestCase " + className;
                testCase = ct.newInstance(arglist);
            }
            catch(NoSuchMethodException nsme) {
                testCase = null;                
            }

            if (testCase == null) {
                testCase = cl.newInstance();
            }
                        
            // cannot execute setUp directly because it is protected
            // we can however use reflection to call it because this VM
            // has access protection disabled
            // TODO: this will not execute inherited setUp methods!!!
            try {
                Method setUpMethod = cl.getDeclaredMethod("setUp", null);

                if (setUpMethod != null) {
                    setUpMethod.setAccessible(true);
                    setUpMethod.invoke(testCase, null);
                }
            }
            catch(NoSuchMethodException nsme) {
            }

            // pick up all declared fields
            // this will not get inherited fields!! (would need to deal
            // with them some other way)            
            Field fields[] = cl.getDeclaredFields();
            Object obs[] = new Object[fields.length*2];

            for(int i=0; i<fields.length; i++) {
                // make sure we can access the field regardless of protection
                fields[i].setAccessible(true);
                // fill in the return array in the format
                // name, object, name, object
                obs[i*2] = fields[i].getName();
                obs[i*2+1] = fields[i].get(testCase);
            }

            return obs;
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

        return new Object[0];
    }

	/**
	 * Execute a JUnit test method and return the result.
	 * 
	 * @return  an array in case of failure or error, and null if
	 *          the test ran successfully.
	 */
    private static Object[] runTestMethod(String className, String methodName)
        throws ClassNotFoundException
    {
		Class cl = loadClass(className);
        
        TestCase testCase = null;
        
        // construct a testcase using
        // the String constructor and passing in our
        // method name as a parameter     
        try {
            Class partypes[] = new Class[1];
            partypes[0] = String.class;
            Constructor ct = cl.getConstructor(partypes);

            Object arglist[] = new Object[1];
            arglist[0] = methodName;
            testCase = (TestCase) ct.newInstance(arglist);
        }
        catch (NoSuchMethodException nsme) { }
        catch (InstantiationException ie) { throw new IllegalArgumentException("ie"); }
        catch (IllegalAccessException iae) { throw new IllegalArgumentException("iae"); }
        catch (InvocationTargetException ite) { throw new IllegalArgumentException("ite"); }

		// if that failed, construct a testcase using
		// the no-arguement constructor
        if (testCase == null) {
            try {
                testCase = (TestCase) cl.newInstance();
                testCase.setName(methodName);
            }
            catch (InstantiationException ie) { }
            catch (IllegalAccessException iae) { }
        }
        
        TestSuite suite = new TestSuite("bluej");
        suite.addTest(testCase);

        TestResult tr = RemoteTestRunner.run(suite);

		if (tr.errorCount() > 1 || tr.failureCount() > 1)
			throw new IllegalStateException("error or failure count was > 1");
			
		if (tr.errorCount() == 1) {
			for (Enumeration e = tr.errors(); e.hasMoreElements(); ) {
				Object result[] = new Object[3];
				TestFailure tf = (TestFailure)e.nextElement();
				
				result[0] = tf.isFailure() ? "failure" : "error";
				result[1] = tf.exceptionMessage() != null ? tf.exceptionMessage() : "no exception message";
				result[2] = tf.trace() != null ? tf.trace() : "no trace";
				
				return result;
			}
			// should not reach here
			throw new IllegalStateException("errorCount was 1 but found no errors");
		}

		if (tr.failureCount() == 1) {
			for (Enumeration e = tr.failures(); e.hasMoreElements(); ) {
				Object result[] = new Object[3];
				TestFailure tf = (TestFailure)e.nextElement();
				
				result[0] = tf.isFailure() ? "failure" : "error";
				result[1] = tf.exceptionMessage() != null ? tf.exceptionMessage() : "no exception message";
				result[2] = tf.trace() != null ? tf.trace() : "no trace";

				return result;
			}
			// should not reach here
			throw new IllegalStateException("failureCount was 1 but found no errors");
		}

		// success
		return null;
    }

    /**
     * Remove an object from a package scope.
     *
     * This has to be done tolerantly: (why? ajp 22/5)
     *  If the named instance is not in the scope, we just quetly return.
     */
    private static void removeObject(String instanceName)
    {
        //Debug.message("[VM] removeObject: " + instanceName);
        Map scope = getScope();
        scope.remove(instanceName);
    }

    /**
     * Redirect System.err to an invisible sink.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    static void supressOutput()
    {
        throwawayErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(throwawayErr));
    }

    /**
     * Restore the standard System.err.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    static void restoreOutput()
    {
        System.setErr(systemErr);
    }

    /**
     * Dispose of all the top level windows we think are open.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    static void disposeWindows()
    {
        synchronized(openWindows) {
            disposingAllWindows = true;
            Iterator it = openWindows.iterator();

            while(it.hasNext()) {
                Object o = it.next();

                if (o instanceof Window) {
                    Window w = (Window) o;
                    w.dispose();
                }
            }
            openWindows.clear();
            disposingAllWindows = false;
        }
    }
}
