package bluej.runtime;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

import junit.framework.*;
//BeanShell
//import bsh.*;

/**
 * Class that controls the runtime of code executed within BlueJ.
 * Sets up the initial thread state, etc.
 *
 * This class both holds runtime attributes and executes commands.
 * Execution is done through JDI reflection from the JdiDebugger class.
 *
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: ExecServer.java 3029 2004-09-30 23:57:43Z davmac $
 */
public class ExecServer
{
    // task type constants (these constants must match the name of the
    // corresponding methods in this ExecServer source). Methods to call are
    // obtained using reflection by JdiDebugger (using these strings).

    //public static final String NEW_LOADER       = "newLoader";
    //public static final String LOAD_CLASS       = "loadClass";
    //public static final String ADD_OBJECT       = "addObject";
    //public static final String REMOVE_OBJECT    = "removeObject";
//	BeanShell    
    //public static final String GET_OBJECTS      = "getObjects";
    //public static final String SET_LIBRARIES    = "setLibraries";
    //public static final String RUN_TEST_SETUP   = "runTestSetUp";
    //public static final String RUN_TEST_METHOD  = "runTestMethod";
    //public static final String SUPRESS_OUTPUT   = "supressOutput";
    //public static final String RESTORE_OUTPUT   = "restoreOutput";
    // public static final String DISPOSE_WINDOWS  = "disposeWindows";
//	BeanShell    
    //public static final String EXECUTE_CODE     = "executeCode";
    //public static final String NEW_THREAD       = "newThread";
    
	// these fields will be fetched by VMReference
	
    // the initial thread that starts main()
	public static final String MAIN_THREAD_NAME = "mainThread";
	public static Thread mainThread = null;
	
	// a worker thread that we create
	public static final String WORKER_THREAD_NAME = "workerThread";
	public static Thread workerThread = null;
    
    // Parameters for main thread actions
    public static String classToRun;
    public static String methodToRun;
    public static int execAction;   // EXEC_SHELL, TEST_SETUP or TEST_RUN
    public static Object methodReturn;
    public static Throwable exception;
    
    public static final String CLASS_TO_RUN_NAME = "classToRun";
    public static final String METHOD_TO_RUN_NAME = "methodToRun";
    public static final String EXEC_ACTION_NAME = "execAction";
    public static final String METHOD_RETURN_NAME = "methodReturn";
    public static final String EXCEPTION_NAME = "exception";
    
    // Possible actions for the main thread
    public static final int EXEC_SHELL = 0;  // Execute a shell class
    public static final int TEST_SETUP = 1;
    public static final int TEST_RUN = 2;
    public static final int DISPOSE_WINDOWS = 3;
    public static final int EXIT_VM = 4;
    
    // Parameter for worker thread actions
    public static int workerAction;
    public static String objectName;
    public static Object object;
    public static String classPath;
    public static String className;
    
    public static Object workerReturn;
    
    public static final String WORKER_ACTION_NAME = "workerAction";
    public static final String OBJECTNAME_NAME = "objectName";
    public static final String OBJECT_NAME = "object";
    public static final String CLASSPATH_NAME = "classPath";
    public static final String CLASSNAME_NAME = "className";
    public static final String WORKER_RETURN_NAME = "workerReturn";
    
    public static final int REMOVE_OBJECT = 0;
    public static final int ADD_OBJECT    = 1;
    public static final int LOAD_CLASS    = 2;
    public static final int NEW_LOADER    = 3;
    
    
    // a BeanShell interpreter that we use for executing code
//	BeanShell    
    //private static Interpreter interpreter;
        
	// the class manager on this machine
    private static RemoteClassMgr classmgr;

    // the current class loader
	private static ClassLoader currentLoader;
	// a hashmap of names to objects
    private static Map objects = new HashMap();
    
    /**
     * We need to keep track of open windows so that we can dispose of them
     * when simulating a System.exit() call
     */
    private static List openWindows = Collections.synchronizedList(new LinkedList());
    private static boolean disposingAllWindows = false; // true while we are disposing

    private static PrintStream systemErr = System.err;
    private static ByteArrayOutputStream throwawayErr = null;

    /**
     * Main method.
     *
     */
    public static void main(String[] args)
        throws Throwable
    {
		// Debug.message("[VM] creating server object");

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

        // construct a BeanShell interpreter
//    	BeanShell    
        //interpreter = new Interpreter();
        //interpreter.setStrictJava(true);
        
		// record our main thread
		// mainThread = Thread.currentThread();
		
        workerThread = Thread.currentThread();

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
		// vmStarted();
        newThread();
		
        // an infinite loop.. 
        while(true) {
            vmSuspend();
            switch(workerAction) {
                case ADD_OBJECT:
                    addObject(objectName, object);
                    break;
                case REMOVE_OBJECT:
                    removeObject(objectName);
                    break;
                case LOAD_CLASS:
                    try {
                        workerReturn = loadClass(className);
                    }
                    catch(ClassNotFoundException cnfe) {
                        workerReturn = null;
                    }
                    break;
                case NEW_LOADER:
                    workerReturn = newLoader(classPath);
                    break;
                case EXIT_VM:
                    System.exit(0);
            }
        }
        // System.err.println("worker thread bye bye");
		//if(! shouldDie)
		//    System.err.println("main thread bye bye");
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
	 * This method is used to suspend the execution of the worker threads.
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
        //Debug.message("[VM] getScope");
        return objects;

//    	BeanShell    
        //Map hashMap = new HashMap();
        //String varNames[] = interpreter.getNameSpace().getVariableNames();
        //
        //try {
        //    for (int i=0; i<varNames.length; i++) {
        //        if (varNames[i].equals("bsh"))
        //            continue;
        //        hashMap.put(varNames[i], interpreter.getNameSpace().getVariable(varNames[i]));
        //    }
        //}
        //catch(UtilEvalError uee) {
        //    uee.printStackTrace();    
        //}
        //return hashMap;
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

//    	BeanShell    
        //if (classPath.equals("."))
        //    return currentLoader;
        
        ////interpreter.setClassLoader(currentLoader);
        //interpreter.getNameSpace().clear();
        //try {
        //    URL url = new File(classPath).toURL();
        //    interpreter.getNameSpace().getClassManager().addClassPath(url);
        //    //addClassPath ();"); 
        //    interpreter.eval("import *;");
        //    //interpreter.eval("setAccessibility(true);");
        //}
        //catch (EvalError ee) { ee.printStackTrace(); }
        //catch (Exception e) { e.printStackTrace(); }
        
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
		
        // until we first setup the class loader
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
            //
            // WRONG! we need to force the initilisation of the class.
            // When inspecting we need the class to be initialized in order
            // to show the values of static fields.
            forceInitialisation(cl);              
        }         
       
       
        return cl;
    }
    
    /**
     * Forces the initialisation of a class by accessing all the public static fields. 
     * If it has no public static fields (that are not also final) it is not initialised. 
     * 
     * @param cl The class to initialise
     */
    private static void forceInitialisation(Class cl) {
        Field[] fields = cl.getFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            int modifiers = field.getModifiers();
            if(Modifier.isStatic(modifiers)) {
                try {
                    field.get(null);
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
        }
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
//    	BeanShell    
        //try {
        //    interpreter.set(instanceName, value);           
        //}
        //catch (EvalError ee) {
        //    ee.printStackTrace();    
        //}
    }

    /**
     * Update the remote VM with the list of user/system libraries
     * which the user has created using the ClassMgr.
     */
    /*
    private static void setLibraries(String libraries)
    {
		// Debug.message("[VM] setLibraries: " + libraries);
        classmgr.setLibraries(libraries);
    }
    */
  
   
//    /**
//     * Get all objects in this machine
//     */
//    private static Object[] getObjects()
//    {
//        try {
//            Map map = getScope();
//            //String varNames[] = interpreter.getNameSpace().getVariableNames();
//            Object obs[] = new Object[map.size()*2];
//
//            Iterator keyIterator = map.keySet().iterator();      
//            
//            try {
//                int i = 0;
//                
//                while(keyIterator.hasNext()) {
//                    String aKey = (String) keyIterator.next();
//                    // fill in the return array in the format
//                    // name, object, name, object
//                    obs[i*2] = aKey;
//                    obs[i*2+1] = interpreter.getNameSpace().getVariable(aKey);
//                    i++;
//                }
//                return obs;
//            }
//            catch(UtilEvalError uee) { }
//
//            
//        } catch(Exception e) { e.printStackTrace(); }
//        
//        return new Object[0];    
//    }
    
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
		// Debug.message("[VM] runTestSetUp" + className);

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

            Method setUpMethod = findMethod(cl, "setUp", null);
            if (setUpMethod != null) {
                setUpMethod.setAccessible(true);
                setUpMethod.invoke(testCase, null);
            }

            // pick up all declared fields
            // this will not get inherited fields!! (would need to deal
            // with them some other way)            
            Field fields[] = cl.getDeclaredFields();
            // we make it one bigger than double the number of fields to store the
            // test case object which is used later for extracting (possibly generic) fields
            // whose exact generic types may not be available via class level
            // reflection

            Object obs[] = new Object[fields.length*2 + 1];

            for(int i=0; i<fields.length; i++) {
                // make sure we can access the field regardless of protection
                fields[i].setAccessible(true);
                // fill in the return array in the format
                // name, object, name, object
                obs[i*2] = fields[i].getName();
                obs[i*2+1] = fields[i].get(testCase);
            }
            //add the testcase as the last object in the array
            obs[obs.length-1] = testCase;
            return obs;
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

        return new Object[0];
    }
    
    /**
     * Find a method in the class, regardless of visibility. This is
     * essentially the same as Class.getMethod(), except that it also returns
     * non-public methods.
     * 
     * @param cl         The class to search
     * @param name       The name of the method
     * @param paramtypes The argument types
     * @return  The method, or null if not found.
     */
    static private Method findMethod(Class cl, String name, Class[] paramtypes)
    {
        while (cl != null) {
            try {
                return cl.getDeclaredMethod(name, paramtypes);
            }
            catch (NoSuchMethodException nsme) {}

            cl = cl.getSuperclass();
        }

        return null;
    }

	/**
     * Execute a JUnit test method and return the result.<p>
     * 
     * The array returned in case of failure or error contains:<br>
     *  [0] = "failure" or "error" (string)<br>
     *  [1] = the exception message (or "no exception message")<br>
     *  [2] = the stack trace as a string (or "no stack trace")<br>
     *  [3] = the name of the class in which the exception/failure occurred<br>
     *  [4] = the source filename for where the exception/failure occurred<br>
     *  [5] = the name of the method in which the exception/failure occurred<br>
     *  [6] = the line number where the exception/failure occurred (a string)
     * 
     * @return an array in case of failure or error, and null if the test ran
     *         successfully.
     */
    private static Object[] runTestMethod(String className, String methodName)
        throws ClassNotFoundException
    {
		// Debug.message("[VM] runTestMethod" + className + " " + methodName);

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

		RemoteTestRunner runner = new RemoteTestRunner();

        TestResult tr = runner.doRun(suite);

		if (tr.errorCount() > 1 || tr.failureCount() > 1)
			throw new IllegalStateException("error or failure count was > 1");
			
		if (tr.errorCount() == 1) {
			for (Enumeration e = tr.errors(); e.hasMoreElements(); ) {
				Object result[] = new Object[7];
				TestFailure tf = (TestFailure)e.nextElement();
				
				result[0] = tf.isFailure() ? "failure" : "error";
				result[1] = tf.exceptionMessage() != null ? tf.exceptionMessage() : "no exception message";
				result[2] = tf.trace() != null ? tf.trace() : "no trace";
				StackTraceElement [] ste = tf.thrownException().getStackTrace();
                result[3] = ste[0].getClassName();
                result[4] = ste[0].getFileName();
                result[5] = ste[0].getMethodName();
                result[6] = String.valueOf(ste[0].getLineNumber());
                
				return result;
			}
			// should not reach here
			throw new IllegalStateException("errorCount was 1 but found no errors");
		}

		if (tr.failureCount() == 1) {
			for (Enumeration e = tr.failures(); e.hasMoreElements(); ) {
				Object result[] = new Object[7];
				TestFailure tf = (TestFailure)e.nextElement();
				
				result[0] = tf.isFailure() ? "failure" : "error";
				result[1] = tf.exceptionMessage() != null ? tf.exceptionMessage() : "no exception message";
				result[2] = tf.trace() != null ? tf.trace() : "no trace";
                StackTraceElement [] ste = tf.thrownException().getStackTrace();
                
                // search the stack trace backward until finding a class not
                // part of the junit framework
                int i = 0;
                while(i < ste.length && ste[i].getClassName().startsWith("junit."))
                    i++;
                result[3] = ste[i].getClassName();
                result[4] = ste[i].getFileName();
                result[5] = ste[i].getMethodName();
                result[6] = String.valueOf(ste[i].getLineNumber());

				return result;
			}
			// should not reach here
			throw new IllegalStateException("failureCount was 1 but found no errors");
		}

		// success
		return null;
    }

//    private static Object executeCode(String code) throws Throwable
//    {
//        System.out.println("[VM] executeCode " + code);
//        // eval a statement and get the result
//        try {
//            return interpreter.eval(code);             
//        }
//        catch (TargetError e) {
//            // The script threw an exception
//            e.getTarget().printStackTrace();
//            throw e.getTarget();
//        } catch ( ParseException e ) {
//            e.printStackTrace();
//        } catch ( EvalError e ) {
//            e.printStackTrace();
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }
//        return null;
//    }
    
    /**
     * Remove an object from the scope.
     */
    private static void removeObject(String instanceName)
    {
        //Debug.message("[VM] removeObject: " + instanceName);
        Map scope = getScope();
        scope.remove(instanceName);
//		BeanShell
        //try {
        //    interpreter.unset(instanceName);
        //}
        //catch (EvalError ee) { }
    }

    /**
     * Redirect System.err to an invisible sink.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    /*
    static void supressOutput()
    {
        throwawayErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(throwawayErr));
    }
    */

    /**
     * Restore the standard System.err.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    /*
    static void restoreOutput()
    {
        System.setErr(systemErr);
    }
    */

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
    
    /**
     * Clear the system input buffer. This is used between method calls to
     * make sure that System.in.read() doesn't read input which was buffered
     * during the last method call but never read.
     */
    static void clearInputBuffer()
    {
        try {
            int n = System.in.available();
            while(n != 0) {
                System.in.skip(n);
                n = System.in.available();
            }
        }
        catch(IOException ioe) { }
    }
    
    /**
     * Bug in the java debug VM means that exception events are unreliable 
     * if we re-use the same thread over and over. So, whenever running user
     * code results in an exception, this method is used to spawn a new thread.
     */
    static void newThread()
    {        
        final Thread oldThread = mainThread;
        // Then make a new one.
        mainThread = new Thread("main") {
            public void run()
            {
                try {
                if(oldThread != null)
                    oldThread.join();
                }
                catch(InterruptedException ie) { }
                
                vmStarted();
                // int count = 0;
                
                // wait in an infinit(ish) loop.. (we want the loop to finish if
                // for some reason vmSuspend() is not working - say the connecting
                // VM has failed and therefore the breakpoint has been removed -
                // in this case we will fall through the loop and exit)
                //while(count++ < 100000 && ! shouldDie) {
                // Wait for a command from the BlueJ VM
                //vmSuspend();
                
                // Execute the command
                methodReturn = null;
                exception = null;
                try {
                    switch(execAction) {
                        case EXEC_SHELL:
                            // Execute a shell class.
                            clearInputBuffer();
                            Class c = currentLoader.loadClass(classToRun);
                            Method m = c.getMethod("run", new Class[0]);
                            try {
                                methodReturn = m.invoke(null, new Object[0]);
                            }
                            catch(InvocationTargetException ite) {
                                throw ite.getCause();
                            }
                            break;
                        case TEST_SETUP:
                            methodReturn = runTestSetUp(classToRun);
                            break;
                        case TEST_RUN:
                            methodReturn = runTestMethod(classToRun, methodToRun);
                            break;
                        case DISPOSE_WINDOWS:
                            disposeWindows();
                            break;
                        case EXIT_VM:
                            System.exit(0);
                        default:
                    }
                }
                catch(Throwable t) {
                    exception = t;
                }
                finally {
                    execAction = EXIT_VM;
                    newThread();
                }
            }
        };
        mainThread.start();
    }
}
