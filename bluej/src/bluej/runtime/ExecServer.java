/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.runtime;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import bluej.utility.Utility;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Class that controls the runtime of code executed within BlueJ.
 * Sets up the initial thread state, etc.
 *
 * This class both holds runtime attributes and executes commands.
 * Execution is done through JDI reflection from the JdiDebugger class.
 *
 * @author  Michael Kolling
 * @author  Andrew Patterson
 * @version $Id: ExecServer.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ExecServer
{
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
    public static String [] parameterTypes;
    public static Object [] arguments;
    public static int execAction;   // EXEC_SHELL, TEST_SETUP or TEST_RUN
    
    public static Object methodReturn;
    public static Class executedClass;
    public static Throwable exception;
    
    // These constant values must match the variable names declared above
    public static final String CLASS_TO_RUN_NAME = "classToRun";
    public static final String METHOD_TO_RUN_NAME = "methodToRun";
    public static final String PARAMETER_TYPES_NAME = "parameterTypes";
    public static final String ARGUMENTS_NAME = "arguments";
    public static final String EXEC_ACTION_NAME = "execAction";
    public static final String METHOD_RETURN_NAME = "methodReturn";
    public static final String EXCEPTION_NAME = "exception";
    public static final String EXECUTED_CLASS_NAME = "executedClass";
    
    // Possible actions for the main thread
    public static final int EXEC_SHELL = 0;  // Execute a shell class
    public static final int TEST_SETUP = 1;
    public static final int TEST_RUN = 2;
    public static final int DISPOSE_WINDOWS = 3;
    public static final int EXIT_VM = 4;
    public static final int LOAD_INIT_CLASS = 5;  // load and initialize a class
    public static final int INSTANTIATE_CLASS = 6; // use default constructor
    public static final int INSTANTIATE_CLASS_ARGS = 7; // use constructor
        // with specified parameter types and arguments
    
    // Parameter for worker thread actions
    public static int workerAction = EXIT_VM;
    public static String objectName;
    public static Object object;
    public static String classPath;
    public static String className;
    public static String scopeId;
    public static ClassLoader classLoader = null; // null to use current loader.
    
    public static Object workerReturn;
    
    // These constant values must match the variable names declared above
    public static final String WORKER_ACTION_NAME = "workerAction";
    public static final String OBJECTNAME_NAME = "objectName";
    public static final String OBJECT_NAME = "object";
    public static final String CLASSPATH_NAME = "classPath";
    public static final String CLASSNAME_NAME = "className";
    public static final String WORKER_RETURN_NAME = "workerReturn";
    public static final String SCOPE_ID_NAME = "scopeId";
    public static final String CLASSLOADER_NAME = "classLoader";
    
    // possible actions for worker thread
    public static final int REMOVE_OBJECT = 0;
    public static final int ADD_OBJECT    = 1;
    public static final int LOAD_CLASS    = 2;
    public static final int NEW_LOADER    = 3;
    // EXIT_VM ( = 4) is also used in the worker thread
    public static final int LOAD_ALL      = 5; // load class and inner classes

    // the current class loader
    private static ClassLoader currentLoader;

    // The loader that loads the greenfoot application classes. This is the
    // loader that gets used the first time anything is loaded in the debugvm.
    // This loader will be the parent loader for all loaders created later on.
    // The reason we need to do this, is to keep using the same class for
    // GreenfootObject and GreenfootWorld in order to cast newly created objects
    // into these types.
    //private static ClassLoader greenfootLoader;
    
    // a hashmap of names to objects
    // private static Map objects = new HashMap();
    private static Map objectMaps = new HashMap();
    
    /**
     * We need to keep track of open windows so that we can dispose of them
     * when simulating a System.exit() call
     */
    private static List openWindows = Collections.synchronizedList(new LinkedList());
    private static boolean disposingAllWindows = false; // true while we are disposing

    /**
     * Main method.
     *
     */
    public static void main(String[] args)
        throws Throwable
    {
        // Set up an input stream filter to detect "End of file" signals
        // (CTRL-Z or CTRL-D typed in terminal)
        System.setIn(new BJInputStream(System.in));
        
        // Set up encoding for the terminal, the only arg that should be passed in
        // is the encoding eg. "UTF-8, otherwise do nothing
        if(args.length > 0 && !args[0].equals("")) {
            System.setOut(new PrintStream(System.out, true, args[0]));
        }
        
        // Set up the worker thread. The worker thread can be used to perform certain actions
        // when the main thread is busy. Actions on the worker thread are guarenteed to execute
        // in a timely manner - for this reason they must not execute user code.
        workerThread = new Thread("BlueJ worker thread")
        {
            public void run()
            {
                while(true) {
                    vmSuspend();
                    switch(workerAction) {
                        case ADD_OBJECT:
                            addObject(scopeId, objectName, object);
                            object = null;
                            break;
                        case REMOVE_OBJECT:
                            removeObject(scopeId, objectName);
                            break;
                        case LOAD_CLASS:
                            try {
                                if (classLoader == null)
                                    classLoader = currentLoader;
                                
                                workerReturn = Class.forName(className, false, currentLoader);
                                // Cause the class to be prepared (ie. its fields and methods
                                // enumerated). Otherwise we can get ClassNotPreparedException
                                // when we try and get the fields on the other VM.
                                ((Class) workerReturn).getFields();
                                
                                classLoader = null;  // reset for next call
                            }
                            catch(Throwable cnfe) {
                                workerReturn = null;
                            }
                            break;
                        case NEW_LOADER:
                            workerReturn = newLoader(classPath);
                            break;
                        case EXIT_VM:
                            System.exit(0);
                        case LOAD_ALL:
                            workerReturn = loadAllClasses(className);
                    }
                    // After any action, set the next action to exit. If connection to
                    // primary VM is lost, the secondary VM (i.e. this VM) will then exit.
                    workerAction = EXIT_VM;
                }
            }
        };

        // register a listener to record all window opens and closes
        Toolkit toolkit = Toolkit.getDefaultToolkit();

        AWTEventListener listener = new AWTEventListener()
        {
            public void eventDispatched(AWTEvent event)
            {
                Object source = event.getSource();
                if(event.getID() == WindowEvent.WINDOW_OPENED) {                    
                    addWindow(source);
                    if(source instanceof Window) {
                        Utility.bringToFront((Window) source);
                    }
                } else if(event.getID() == WindowEvent.WINDOW_CLOSED) {
                    removeWindow(source);
                }
            }
        };

        toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);

        // we create the security manager last so that hopefully, all the system/AWT
        // threads will have been created and we can then rig our security manager
        // to make all user-created threads go into a single thread group
        System.setSecurityManager(new RemoteSecurityManager());

        // signal with a breakpoint that we have performed our VM
        // initialization, at the same time, create the initial server thread.
        newThread();
		
        // Set the worker thread in motion also. Give it maximum priority so that it can
        // be guarenteed to execute in a timely manner, and won't get starved by user code
        // executing in other threads.
        workerThread.setPriority(Thread.MAX_PRIORITY);
        workerThread.start();
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
    static Map getScope(String scopeId)
    {
        synchronized (objectMaps) {
            Map m = (Map) objectMaps.get(scopeId);
            if (m == null) {
                m = new HashMap();
                objectMaps.put(scopeId, m);
            }
            return m;
        }
    }

    // -- methods called by reflection from JdiDebugger --
    // --
    // -- methods that can be made private have been, as
    // -- the reflection is still able to access them
    // -- (RemoteSecurityManager switches all reflection
    // --  access checks off)

    /**
     * Create a new class loader for a given classpath.
     * @param urlListAsString a URL list written as a single string (the \n is used to divide entries)
     * @return a URLClassLoader that can be used to load user classes.
     */
    private static ClassLoader newLoader(String urlListAsString )
    {
//      JOptionPane.showMessageDialog(null,"ExecServer.newLoader() urlListAsString="+urlListAsString);
        
        String [] splits = urlListAsString.split("\n");
        URL []urls = new URL[splits.length];
        
        for (int index = 0; index < splits.length; index++)
            try {
                urls[index] = new URL(splits[index]);
            }
        catch (MalformedURLException mfue) {
            // Should never happen but if it does we want to know about it
            System.err.println("ExecServer.newLoader() Malformed URL=" + splits[index]);
        }
        
        currentLoader = new URLClassLoader(urls);
        
        synchronized (objectMaps) {
            objectMaps.clear();
        }
        
        return currentLoader;
    }

    /**
     * Load (and prepare) a class in the remote runtime. Return null if the class could not
     * be loaded.
     */
    public static Class loadAndInitClass(String className)
    {
    	//Debug.message("[VM] loadClass: " + className);
        
        if (currentLoader == null) {
          // It does not make much sense to load something without a loader, better signal it immediatly.
          System.err.println("ExecServer.loadClass() currentLoader=null");
        }
        
        Throwable exception = null;
        Class cl;
        try {
            cl = Class.forName(className, true, currentLoader);
        }
        catch (ClassNotFoundException cnfe) {
            // class definitely doesn't exist
            cl = null;
        }
        catch (ExceptionInInitializerError eiie) {
            // The class was loaded it, but an exception occurred during initialization.
            // As this is an error in user code, we want to report it.
            exception = eiie.getCause();
            
            // Now get the class again, uninitialized this time
            try {
                cl = Class.forName(className, false, currentLoader);
            }
            catch (ClassNotFoundException cnfe) {
                // this shouldn't happen anyway.
                cl = null;
            }
        }
        catch (Throwable err) {
            // There are numerous other linkage problems. Also there is the possibility that
            // a static initialization block will throw an instance of java.lang.Error, which
            // will not be wrapped in an ExceptionInInitializerError (unfortunately). In either
            // case we probably should let the user know what happened.
            
            exception = err;

            // The class may exist, but not be initializable for some reason.
            try {
                cl = Class.forName(className, false, currentLoader);
            }
            catch (Throwable t) {
                cl = null;
            }
        }
        
        // If we have an exception to report, filter and report it.
        if (exception != null) {
            StackTraceElement [] stackTrace = exception.getStackTrace();
            
            // filter bluej.runtime.ExecServer from the stack trace
            int i;
            for (i = stackTrace.length - 1; i > 0; i--) {
                String stClassName = stackTrace[i].getClassName();
                if (! stClassName.startsWith("bluej.runtime.ExecServer")
                        && ! stClassName.startsWith("java.lang.Class"))
                    break;
            }
            StackTraceElement [] newStackTrace = new StackTraceElement[i+1];
            System.arraycopy(stackTrace, 0, newStackTrace, 0, i+1);
            exception.setStackTrace(newStackTrace);
            recordException(exception);
        }
       
        return cl;
    }
    
    /**
     * Load a class, and all its inner classes.
     */
    private static Class [] loadAllClasses(String className)
    {
        List l = new ArrayList();
        
        try {
            Class c = currentLoader.loadClass(className);
            c.getFields(); // prepare class
            l.add(c);
            getDeclaredInnerClasses(c, l);
            
            // Now we want the anonymous inner classes:
            int i = 1;
            while(true) {
                c = currentLoader.loadClass(className + '$' + i);
                c.getFields();
                l.add(c);
                i++;
            }
        }
        catch (Throwable t) {}
        
        return (Class []) l.toArray(new Class[l.size()]);
    }
    
    /**
     * Add the declared inner classes of the given class to the given
     * list, recursively.
     */
    private static void getDeclaredInnerClasses(Class c, List list)
    {
        try {
            Class [] rlist = c.getDeclaredClasses();
            for (int i = 0; i < rlist.length; i++) {
                c = rlist[i];
                c.getFields(); // force preparation
                list.add(rlist[i]);
                getDeclaredInnerClasses(rlist[i], list);
            }
        }
        catch (Throwable t) {}
    }
    
    /**
     * Add an object into a package scope (for possible use as parameter
     * later). Used after object creation to add the newly created object
     * to the scope.
     *
     * Must be static because it is used by Shell without a execServer reference
     */
    static void addObject(String scopeId, String instanceName, Object value)
    {
        // Debug.message("[VM] addObject: " + instanceName + " " + value);
        Map scope = getScope(scopeId);
        synchronized (scope) {
            scope.put(instanceName, value);
            scope.notify(); // in case Greenfoot is waiting for this object
        }
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
    {
		// Debug.message("[VM] runTestSetUp" + className);

		Class cl = loadAndInitClass(className);
        
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
                setUpMethod.invoke(testCase, (Object []) null);
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
    {
		// Debug.message("[VM] runTestMethod" + className + " " + methodName);

		Class cl = loadAndInitClass(className);
        
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
    private static void removeObject(String scopeId, String instanceName)
    {
        //Debug.message("[VM] removeObject: " + instanceName);
        Map scope = getScope(scopeId);
        synchronized (scope) {
            scope.remove(instanceName);
        }
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
                        {
                            // Execute a shell class.
                            methodReturn = null;
                            executedClass = null;
                            
                            clearInputBuffer();
                            // ShellClassLoader cloader = new ShellClassLoader(currentLoader, classToRun);
                            Class c = currentLoader.loadClass(classToRun);
                            executedClass = c;
                            // Class c = cloader.loadClass(classToRun);
                            Method m = c.getMethod("run", new Class[0]);
                            try {
                                methodReturn = m.invoke(null, new Object[0]);
                            }
                            catch(InvocationTargetException ite) {
                                throw ite.getCause();
                            }
                            break;
                        }
                        case INSTANTIATE_CLASS:
                        {
                            // Instantiate a class using the default
                            // constructor
                            clearInputBuffer();
                            Class c = currentLoader.loadClass(classToRun);
                            Constructor cons = c.getDeclaredConstructor(new Class[0]);
                            cons.setAccessible(true);
                            try {
                                methodReturn = cons.newInstance((Object []) null);
                            }
                            catch (InvocationTargetException ite) {
                                throw ite.getCause();
                            }
                            break;
                        }
                        case INSTANTIATE_CLASS_ARGS:
                        {
                            // Instantiate a class using specified parameter
                            // types and arguments
                            clearInputBuffer();
                            Class c = currentLoader.loadClass(classToRun);
                            Class [] paramClasses = new Class[parameterTypes.length];
                            for (int i = 0; i < parameterTypes.length; i++) {
                                if (classLoader == null)
                                    classLoader = currentLoader;
                                
                                paramClasses[i] = Class.forName(parameterTypes[i], false, currentLoader);
                            }
                            Constructor cons = c.getDeclaredConstructor(paramClasses);
                            cons.setAccessible(true);
                            try {
                                methodReturn = cons.newInstance(arguments);
                            }
                            catch (InvocationTargetException ite) {
                                throw ite.getCause();
                            }
                            break;
                        }
                        case TEST_SETUP:
                            methodReturn = runTestSetUp(classToRun);
                            break;
                        case TEST_RUN:
                            methodReturn = runTestMethod(classToRun, methodToRun);
                            break;
                        case DISPOSE_WINDOWS:
                            disposeWindows();
                            break;
                        case LOAD_INIT_CLASS:
                            try {
                                methodReturn = loadAndInitClass(classToRun);
                            }
                            catch(Throwable cnfe) {
                                methodReturn = null;
                            }
                            break;
                        case EXIT_VM:
                            System.exit(0);
                        default:
                    }
                }
                catch(Throwable t) {
                    // record that an exception occurred
                    recordException(t);
                }
                finally {
                    // Set execAction to EXIT_VM, so if the main bluej process has died,
                    // this vm will exit also.
                    execAction = EXIT_VM;
                    newThread();
                }
            }
        };
        mainThread.start();
    }
    
    /**
     * Record that an exception occurred, as well as printing a filtered stack trace.
     * @param t  the exception which was caught
     */
    private static void recordException(Throwable t)
    {
        // record that an exception occurred
        exception = t;
        
        // print a filtered stack trace to System.err
        StackTraceElement [] stackTrace = t.getStackTrace();
        int i;
        for(i = 0; i < stackTrace.length; i++) {
            if(stackTrace[i].getClassName().startsWith("__SHELL"))
                break;
        }
        StackTraceElement [] newStackTrace = new StackTraceElement[i];
        System.arraycopy(stackTrace, 0, newStackTrace, 0, i);
        t.setStackTrace(newStackTrace);
        t.printStackTrace();
    }
    

    /**
     * Gets an object in the scope.
     * <br>
     * 
     * Used by greenfoot.
     * 
     * @param instanceName The name of the object
     * @return The object
     */
    public static Object getObject(String instanceName) {
        Map m = getScope(scopeId);
        Object rval = null;
        
        try {
            synchronized (m) {
                rval = m.get(instanceName);
                // Sometimes the object isn't available yet - the worker thread
                // hasn't stored it in the map yet. In that case we'll wait to
                // be notified that an object has been stored.
                if (rval == null) {
                    m.wait();
                    rval = m.get(instanceName);
                }
            }
        }
        catch (InterruptedException ie) {}
        
        return rval;
    }
    
    /**
     * Get the current class loader used to load user classes.
     * 
     * @return  The current class loader
     */
    public static ClassLoader getCurrentClassLoader()
    {
        return currentLoader;
    }
    
    /**
     * Set the current class loader, to be used for loading user classes.
     * 
     * @param newLoader   The new class loader
     */
    public static void setClassLoader(ClassLoader newLoader)
    {
        currentLoader = newLoader;
    }
}

/**
 * Classloader used in greenfoot to ensure that the user's classes are reloaded.
 * 
 * For now, this is done by checking whether the class is in the default
 * package, which indicates that it is a user class.
 * 
 * @author Poul Henriksen
 */
//class GreenfootClassLoader extends URLClassLoader {
//    public GreenfootClassLoader(URL[] urls)
//    {
//        super(urls);
//     
//    }
//    public GreenfootClassLoader(URL[] urls, ClassLoader parent)
//    {
//        super(urls, parent);
//     
//    }
//    public GreenfootClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory)
//    {
//        super(urls, parent, factory);
//     
//    }
//    
//    /**
//     * If the class is in the default package (the name contains no '.') it will
//     * be reloaded. If not, it will use the normal URLClassloader mechanism.
//     */
//    public Class loadClass(String filename)
//        throws ClassNotFoundException
//    {
//        Class c = null;
//        if (filename.indexOf(".") == -1) {
//            try {
//                c = findClass(filename);
//            }
//            catch (ClassNotFoundException e) {}
//        }
//        if (c == null) {
//            c = super.loadClass(filename);
//        }
//        return c;
//    }
//    
//    protected Class<?> findClass(String filename) throws ClassNotFoundException
//    {
//        boolean debugmode = filename.indexOf('.') == -1 || filename.equals("greenfoot.Actor");
//        if (debugmode)
//        System.out.println("Find class: " + filename);
//        Class c = super.findClass(filename);
//        if (debugmode);
//        System.out.println("  (Found! : " + filename);
//        return c;
//    }
//}

//class DebugClassLoader extends URLClassLoader
//{
//    public DebugClassLoader(URL[] urls)
//    {
//        super(urls);
//     
//    }
//    public DebugClassLoader(URL[] urls, ClassLoader parent)
//    {
//        super(urls, parent);
//     
//    }
//    public DebugClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory)
//    {
//        super(urls, parent, factory);
//     
//    }
//    
//    /**
//     * If the class is in the default package (the name contains no '.') it will
//     * be reloaded. If not, it will use the normal URLClassloader mechanism.
//     */
//    public Class loadClass(String filename)
//        throws ClassNotFoundException
//    {
//        boolean debugmode = filename.indexOf('.') == -1 || filename.equals("greenfoot.Actor");
//        
//        if (debugmode)
//            System.out.println("DebugClassLoader, loadClass: " + filename);
//        Class c = super.loadClass(filename);
//        if (debugmode)
//            System.out.println("  Loaded: " + filename);
//        return c;
//    }
//    
//    protected Class<?> findClass(String filename) throws ClassNotFoundException
//    {
//        boolean debugmode = filename.indexOf('.') == -1 || filename.equals("greenfoot.Actor");
//        if (debugmode)
//        System.out.println("DBCL, find class: " + filename);
//        Class c = super.findClass(filename);
//        if (debugmode)
//        System.out.println("  Found: " + filename);
//        return c;
//    }
//}
