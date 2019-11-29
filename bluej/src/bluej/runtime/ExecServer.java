/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2016,2017,2018,2019  Michael Kolling and John Rosenberg
 
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.collections.ListChangeListener.Change;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.SwingUtilities;

/**
 * Class that controls the runtime of code executed within BlueJ.
 * Sets up the initial thread state, etc.
 *
 * <p>This class both holds runtime attributes and executes commands.
 * Execution is done through JDI reflection from the JdiDebugger class.
 *
 * @author  Michael Kolling
 * @author  Andrew Patterson
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


    // Parameter for which thread to run on:
    public static final int RUN_ON_DEFAULT_THREAD = 0;
    public static final int RUN_ON_FX_THREAD = 1;
    public static final int RUN_ON_SWING_THREAD = 2;
    public static final int RUN_ON_CUSTOM_THREAD = 3;

    // Parameters for main thread actions
    public static int threadToRunOn = RUN_ON_DEFAULT_THREAD;
    // If threadToRunOn == RUN_ON_CUSTOM_THREAD, then this runner will be used as the "runLater".
    // Currently used by Greenfoot to run on the simulation thread, but without introducing a dependency
    // from ExecServer (which exists in BlueJ) to Greenfoot code.
    private static Consumer<Runnable> customThreadRunner;
    public static String classToRun;
    public static String methodToRun;
    public static String [] parameterTypes;
    public static Object [] arguments;
    public static int execAction = -1;   // EXEC_SHELL, TEST_SETUP or TEST_RUN
    
    public static Object methodReturn;
    public static Class<?> executedClass;
    public static Throwable exception;
    
    // These constant values must match the variable names declared above
    public static final String RUN_ON_THREAD_NAME = "threadToRunOn";
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
    public static final int LAUNCH_FX_APP = 8;

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
    private static Map<String,BJMap<String,Object>> objectMaps = new HashMap<String,BJMap<String,Object>>();
    
    /**
     * We need to keep track of open windows so that we can dispose of them
     * when simulating a System.exit() call
     */
    private static List<Window> openWindows = Collections.synchronizedList(new LinkedList<Window>());
    private static boolean disposingAllWindows = false; // true while we are disposing

    /**
     * Main method.
     */
    public static void main(String[] args)
        throws Throwable
    {
        // Set up an input stream filter to detect "End of file" signals
        // (CTRL-Z or CTRL-D typed in terminal)
        System.setIn(new BJInputStream(System.in));
        
        // Set up encoding for the terminal, the only arg that should be passed in
        // is the encoding eg. "UTF-8", otherwise do nothing
        if(args.length > 0 && !args[0].equals("")) {
            try {
                System.setOut(new PrintStream(System.out, true, args[0]));
                System.setErr(new PrintStream(System.err, true, args[0]));
            }
            catch (UnsupportedEncodingException uee) {
                // Do nothing; don't use the requested encoding
            }
        }
        
        // Set up the worker thread. The worker thread can be used to perform certain actions
        // when the main thread is busy. Actions on the worker thread are guaranteed to execute
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
                                ((Class<?>) workerReturn).getFields();
                                
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
                    if (source instanceof Window) {
                        addWindow((Window) source);
                        Utility.bringToFront((Window) source);
                        // To make sure that screen readers an
                        // nounce the window being open,
                        // we de-focus and re-focus it once the right application has focus:
                        // Disabling this code due to it causing issues, see ticket #516.
                        //     KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                        //     ((Window)source).requestFocus();
                    }
                } else if(event.getID() == WindowEvent.WINDOW_CLOSED) {
                    if (source instanceof Window) {
                        removeWindow((Window) source);
                    }
                }
            }
        };

        toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);
        
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
     * This method is used to show the terminal window in case
     * the java program asks for input from the user.
     */
    public static void showTerminalOnInput()
    {
        // <<READING-REQUEST BREAKPOINT!>
    }

    /**
     * Add the object to our list of open windows
     *
     * @param   o   a window object which has just been opened
     */
    private static void addWindow(Window o)
    {
        openWindows.add(o);
    }

    /**
     * Remove the object from our list of open windows
     *
     * @param   o   a window object which has just been closed
     */
    private static void removeWindow(Window o)
    {
        if(!disposingAllWindows)   // don't bother if we are clearing up just now
            openWindows.remove(o);
    }

    /**
     * Find a scoping Map for the given scopeId
     */
    static BJMap<String,Object> getScope(String scopeId)
    {
        synchronized (objectMaps) {
            BJMap<String,Object> m = objectMaps.get(scopeId);
            if (m == null) {
                m = new BJMap<String,Object>();
                objectMaps.put(scopeId, m);
            }
            return m;
        }
    }

    /**
     * Create a new class loader for a given classpath.
     * @param urlListAsString a URL list written as a single string (the \n is used to divide entries)
     * @return a URLClassLoader that can be used to load user classes.
     */
    private static ClassLoader newLoader(String urlListAsString )
    {
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
    public static Class<?> loadAndInitClass(String className)
    {
        Throwable exception = null;
        Class<?> cl;
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
    private static Class<?>[] loadAllClasses(String className)
    {
        List<Class<?>> l = new ArrayList<Class<?>>();
        
        try {
            Class<?> c = currentLoader.loadClass(className);
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
    private static void getDeclaredInnerClasses(Class<?> c, List<Class<?>> list)
    {
        try {
            Class<?> [] rlist = c.getDeclaredClasses();
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
        BJMap<String,Object> scope = getScope(scopeId);
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
        Class<?> cl = loadAndInitClass(className);
        
        try {
            // construct an instance of the test case (firstly trying the
            // String argument constructor - then the no-arg constructor)
            Object testCase = null;

            Class<?> [] partypes = new Class[1];
            partypes[0] = String.class;
            try {
                Constructor<?> ct = cl.getConstructor(partypes);

                Object arglist[] = new Object[1];
                arglist[0] = "TestCase " + className;
                testCase = ct.newInstance(arglist);
            }
            catch(NoSuchMethodException nsme) {
                testCase = null;                
            }

            if (testCase == null) {
                testCase = cl.getDeclaredConstructor().newInstance();
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
    static private Method findMethod(Class<?> cl, String name, Class<?>[] paramtypes)
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
     * A class to record successes and failures during a JUnit test run.
     */
    private static class TestRecorder implements TestExecutionListener
    {
        private final List<Object[]> testDetails = new ArrayList<>();
        private long executionStartTime;
        private long executionRunTime = -1;

        public void testPlanExecutionStarted(TestPlan testPlan)
        {
            executionStartTime = System.currentTimeMillis();
        }

        public void testPlanExecutionFinished(TestPlan testPlan)
        {
            executionRunTime = System.currentTimeMillis() - executionStartTime;
        }

        public long getExecutionRunTime()
        {
            return executionRunTime;
        }

        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult)
        {
            // Retrieved tests (not container)
            if (testIdentifier.isTest())
            {
                Object[] r = new Object[9];
                // The name of the method: we remove anything added by JUnit after the arguments brackets
                // as for Junit 5 the framework may add the index of the test iteration
                // if any argument value is available, we put it into the brackets
                // Note: when JUnit 4 methods are reported, they do not contain brackets in their display name.
                r[0] = (testIdentifier.getLegacyReportingName().contains("(")) ?
                        testIdentifier.getLegacyReportingName()
                                .substring(0, testIdentifier.getLegacyReportingName().lastIndexOf('(') + 1)
                                + String.join(", ", UnitTestExtension.getArgsAsStrList())
                                + ")" :
                        (testIdentifier.getLegacyReportingName() + "()");
                // The display name of the test for that method, if none we set it to an empty String
                r[1] = (testIdentifier.getDisplayName() != null) ? testIdentifier.getDisplayName() : "";

                // Check if the test was successful or not
                if (testExecutionResult.getStatus() == Status.SUCCESSFUL)
                {
                    r[2] = r[3] = r[4] = r[5] = r[6] = r[7] = "";
                    r[8] = "success";
                }
                else
                {
                    if (testExecutionResult.getThrowable().isPresent() && java.lang.AssertionError.class.isAssignableFrom(testExecutionResult.getThrowable().get().getClass()))
                    {
                        r[8] = "failure";
                    }
                    else
                    {
                        r[8] = "error";
                    }
                    if (testExecutionResult.getThrowable().isPresent())
                    {
                        Throwable throwableRes = testExecutionResult.getThrowable().get();

                        r[2] = throwableRes.getMessage() != null ? throwableRes.getMessage() : "no exception message";
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        throwableRes.printStackTrace(pw);
                        r[3] = throwableRes.getStackTrace().length > 0 ? sw.toString() : "no trace";
                        // search the stack trace backward until finding a class not
                        // part of the org.junit framework
                        StackTraceElement[] ste = throwableRes.getStackTrace();
                        int k = 0;
                        while (k < ste.length && ste[k].getClassName().startsWith("org.junit."))
                        {
                            k++;
                        }
                        r[4] = ste[k].getClassName();
                        r[5] = ste[k].getFileName();
                        r[6] = ste[k].getMethodName();
                        r[7] = String.valueOf(ste[k].getLineNumber());
                    }
                    else
                    {
                        r[2] = r[3] = r[4] = r[5] = r[6] = r[7] = "";
                    }
                }
                testDetails.add(r);
            }
        }
    }

    /**
     * Execute a JUnit test on a single test method or all test methods in a test class
     * and return the result.<p>
     *
     * The array returned in case of failure/error has a length of [1 + 9*(number of methods tested)].<br>
     * The first item of the array contains the runtime of executing all tests in milliseconds expressed  
     * as a decimal integer, then each test has eight consecutive items in the array which 
     * contains:<br>
     *  [0] = the method name<br>
     *  [1] = the method display name (for JUnit 5, empty for other frameworks) <br>
     *  [2] = the exception message (or "no exception message"), blank if success<br>
     *  [3] = the stack trace as a string (or "no stack trace"), blank if success<br>
     *  [4] = the name of the class in which the exception/failure occurred, blank if success<br>
     *  [5] = the source filename for where the exception/failure occurred, blank if success<br>
     *  [6] = the name of the method in which the exception/failure occurred, blank if success<br>
     *  [7] = the line number where the exception/failure occurred (a string), blank if success<br>
     *  [8] = "failure" or "error" or "success" (string)<br>
     *      
     * @return an array of length [1 + 9*(number of tests run)]
     */
    private static Object[] runTestMethod(String className, String methodName)
    {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors((methodName != null) ? selectMethod(className + "#" +  methodName) : selectClass(className))
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "true") //required to use our extension
                .build();

        // Load the implementation of InvocationInterceptor with Java ServiceLoader.
        ServiceLoader.load(InvocationInterceptor.class);

        Launcher launcher = LauncherFactory.create();
        TestRecorder recorder = new TestRecorder();
        launcher.registerTestExecutionListeners(recorder);
        launcher.execute(request);
        return Stream.concat(Stream.of(String.valueOf(recorder.getExecutionRunTime())),
                recorder.testDetails.stream().flatMap(t -> Arrays.stream(t))).toArray();
    }

    /**
     * Remove an object from the scope.
     */
    private static void removeObject(String scopeId, String instanceName)
    {
        //Debug.message("[VM] removeObject: " + instanceName);
        BJMap<String,Object> scope = getScope(scopeId);
        synchronized (scope) {
            scope.remove(instanceName);
        }
    }

    /**
     * Dispose of all the top level windows we think are open.
     *
     * Must be static because it is used by RemoteSecurityManager without a execServer reference
     */
    private static void disposeWindows()
    {
        synchronized(openWindows) {
            disposingAllWindows = true;
            Iterator<Window> it = openWindows.iterator();

            while(it.hasNext()) {
                it.next().dispose();
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
    private static void clearInputBuffer()
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

    private static interface RunnableThrows
    {
        public void run() throws Throwable;
    }
    
    /**
     * Bug in the java debug VM means that exception events are unreliable 
     * if we re-use the same thread over and over. So, whenever running user
     * code results in an exception, this method is used to spawn a new thread.
     */
    private static void newThread()
    {        
        final Thread oldThread = mainThread;
        // Then make a new one.
        mainThread = new Thread("main") {
            public void run()
            {
                try {
                    if(oldThread != null) {
                        oldThread.join();
                    }
                }
                catch(InterruptedException ie) { }
                
                vmStarted();
                
                // Execute the command
                methodReturn = null;
                exception = null;
                Thread.currentThread().setContextClassLoader(currentLoader);

                try {
                    switch(execAction) {
                        case EXEC_SHELL:
                        {
                            // Execute a shell class.
                            methodReturn = null;
                            executedClass = null;
                            
                            clearInputBuffer();
                            Class<?> c = currentLoader.loadClass(classToRun);
                            executedClass = c;
                            // Class c = cloader.loadClass(classToRun);
                            Method m = c.getMethod("run", new Class[0]);
                            runOnTargetThread(() -> {
                                try
                                {
                                    methodReturn = m.invoke(null, new Object[0]);
                                }
                                catch (InvocationTargetException ite)
                                {
                                    throw ite.getCause();
                                }
                            });
                            break;
                        }
                        case INSTANTIATE_CLASS:
                        {
                            // Instantiate a class using the default
                            // constructor
                            clearInputBuffer();
                            Class<?> c = currentLoader.loadClass(classToRun);
                            Constructor<?> cons = c.getDeclaredConstructor(new Class[0]);
                            cons.setAccessible(true);
                            runOnTargetThread(() -> {
                                try {
                                    methodReturn = cons.newInstance((Object []) null);
                                }
                                catch (InvocationTargetException ite) {
                                    throw ite.getCause();
                                }
                            });
                            break;
                        }
                        case INSTANTIATE_CLASS_ARGS:
                        {
                            // Instantiate a class using specified parameter
                            // types and arguments
                            clearInputBuffer();
                            Class<?> c = currentLoader.loadClass(classToRun);
                            Class<?> [] paramClasses = new Class[parameterTypes.length];
                            for (int i = 0; i < parameterTypes.length; i++) {
                                if (classLoader == null)
                                    classLoader = currentLoader;
                                
                                paramClasses[i] = Class.forName(parameterTypes[i], false, currentLoader);
                            }
                            Constructor<?> cons = c.getDeclaredConstructor(paramClasses);
                            cons.setAccessible(true);
                            runOnTargetThread(() -> {
                                try {
                                    methodReturn = cons.newInstance(arguments);
                                }
                                catch (InvocationTargetException ite) {
                                    throw ite.getCause();
                                }
                            });
                            break;
                        }
                        case LAUNCH_FX_APP:
                            // The preloader will tell us the Application reference:
                            CompletableFuture<Application> theApp = new CompletableFuture<>();
                            new Thread(() -> {
                                FXPreloader.theApp = theApp;
                                // Use a preloader to be able to find out the Application reference:
                                System.setProperty("javafx.preloader", FXPreloader.class.getName());
                                Application.launch((Class<? extends Application>)loadAndInitClass(classToRun));
                            }, "JavaFX BlueJ Helper").start();
                            // Return null if it takes too long to initialise.  This is most likely
                            // due to the Application class's constructor doing a lot of work,
                            // but it may also be related to a failure in loading.
                            try
                            {
                                methodReturn = theApp.get(3000, TimeUnit.MILLISECONDS);
                            }
                            catch (InterruptedException | TimeoutException | ExecutionException e)
                            {
                                // If anything goes wrong, we just won't add to the object bench:
                                methodReturn = null;
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

    private static void runOnTargetThread(RunnableThrows runnable) throws Throwable
    {
        if (threadToRunOn == RUN_ON_DEFAULT_THREAD)
        {
            runnable.run();
        }
        else
        {
            CompletableFuture<Optional<Throwable>> f = new CompletableFuture<>();
            Runnable wrapped = () ->
            {
                try
                {
                    runnable.run();
                    f.complete(Optional.empty());
                }
                catch (Throwable t)
                {
                    f.complete(Optional.of(t));
                }
            };
            if (threadToRunOn == RUN_ON_FX_THREAD)
            {
                // Initialise FX toolkit in case the user hasn't:
                SwingUtilities.invokeAndWait(() -> new JFXPanel());
                // Then call runLater:
                Platform.runLater(wrapped);
            }
            else if (threadToRunOn == RUN_ON_SWING_THREAD)
            {
                SwingUtilities.invokeLater(wrapped);
            }
            else if (threadToRunOn == RUN_ON_CUSTOM_THREAD && customThreadRunner != null)
            {
                customThreadRunner.accept(wrapped);
            }
            Optional<Throwable> t = f.get();
            if (t.isPresent())
            {
                throw t.get();
            }
        }
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
     * Gets an object in the scope. Used by greenfoot.
     * 
     * @param instanceName The name of the object
     * @return The object
     */
    public static Object getObject(String instanceName)
    {
        BJMap<String,Object> m = getScope(scopeId);
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
     * Get the name-to-object map for the current package scope.
     * Access to the map must be synchronized.
     */
    public static BJMap<String,Object> getObjectMap()
    {
        return getScope(scopeId);
    }

    /**
     * Execute user code using the custom "runLater" action.
     */
    public static void setCustomRunOnThread(Consumer<Runnable> customThreadRunner)
    {
        ExecServer.threadToRunOn = RUN_ON_CUSTOM_THREAD;
        ExecServer.customThreadRunner = customThreadRunner;
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

    // A preloader for FX, only used to find out the reference of the Application instance.
    public static class FXPreloader extends Preloader
    {
        // This should only be filled once per VM run because we exit afterwards.
        // But just in case, we initialise it when we start each Application
        public static CompletableFuture<Application> theApp;

        @Override
        @OnThread(Tag.FXPlatform)
        public void start(Stage primaryStage) throws Exception
        {
            // Add a listener for a new Stage appearing:
            javafx.stage.Window.getWindows().addListener(
                    (Change<? extends javafx.stage.Window> c) -> {
                boolean anyAdded = false;
                while (c.next())
                    anyAdded |= c.wasAdded();
                if (anyAdded)
                {
                    // We don't bring the window itself to the front as that may
                    // mess up user's program.  We just bring the app to the front:
                    Utility.appToFront();
                }
            });
        }

        @Override
        public void handleStateChangeNotification(StateChangeNotification info)
        {
            super.handleStateChangeNotification(info);
            switch (info.getType())
            {
                case BEFORE_START:
                    theApp.complete(info.getApplication());
                    break;
            }
        }
    }
}
