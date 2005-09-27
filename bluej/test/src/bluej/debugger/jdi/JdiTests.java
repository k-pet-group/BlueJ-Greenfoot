package bluej.debugger.jdi;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import junit.framework.TestCase;
import bluej.Boot;
import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.debugger.*;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.ConstructorView;
import bluej.views.View;

/**
 * Tests for the debugger.
 *  
 * @author Davin McCall
 * @version $Id: JdiTests.java 3591 2005-09-27 05:03:14Z davmac $
 */
public class JdiTests extends TestCase
{
    // various vars useful for testing
    boolean failed = false;
    boolean flag1 = false;
    
    /**
     * Return the path element of a URL, properly decoded - that is: replace 
     * each char encoded as "%xx" with its real character.
     * 
     * This is from bluej.Boot.
     */
    private static String getURLPath(String url)
    {
        // Get rid of the initial "file:" string
        if (!url.startsWith("file:"))
            throw new IllegalStateException("Unexpected format of jar file URL (class Boot.java): " + url);
        url = url.substring(5);
        
        try {
            return java.net.URLDecoder.decode(url, "UTF-8");
        }
        catch(UnsupportedEncodingException exc) {
            return null;
        }
    }
    
    protected void setUp()
    {
        try {
            
            // Create a new instance of "Boot" and set the static instance
            // field as a reference to it

            Class bootClass = Class.forName("bluej.Boot");
            
            Constructor bootConstructor = bootClass.getDeclaredConstructor(new Class[] { String[].class, Properties.class });
            bootConstructor.setAccessible(true);
            Boot bootInstance = (Boot) bootConstructor.newInstance(new Object[] { new String[0], new Properties() });
            
            // call initializeBoot method
            Method initMethod = bootClass.getDeclaredMethod("initializeBoot", null);
            initMethod.setAccessible(true);
            initMethod.invoke(bootInstance, null);

            // set bootInstance field
            Field [] fields =  bootClass.getDeclaredFields();
            setStaticField(fields, "instance", bootInstance);

            // initialize Config
            Config.initialise(Boot.getInstance().getBluejLibDir(), new Properties());

        }
        catch(Throwable t) {
            t.printStackTrace();
            throw new IllegalStateException();
        }
    }
    
    protected void setStaticField(Field [] fields, String fieldName, Object value) throws IllegalAccessException
    {
        int i = 0;
        for (i = 0; i < fields.length; i++) {
            if (fields[i].getName() == fieldName) {
                fields[i].setAccessible(true);
                fields[i].set(null, value);
                return;
            }
        }
        throw new IllegalStateException();
    }

    protected Object getInstanceFieldObj(Field [] fields, Object instance, String fieldName)
                                                                throws IllegalAccessException
    {
        int i = 0;
        for (i = 0; i < fields.length; i++) {
            if (fields[i].getName() == fieldName) {
                fields[i].setAccessible(true);
                return fields[i].get(instance);
            }
        }
        return null;
    }
    
    protected void tearDown()
    {
    }

    /**
     * The purpose of this test is to ensure that when the debugger is reset,
     * the old debugger process is terminated even if it was running in an
     * infinite loop.
     * (fails in version 2.0.0)<p>
     * 
     * 21/9/04
     * @author Davin McCall
     */
    public void test1() throws Throwable
    {
        // locate the test project
        File launchDir = Boot.getInstance().getBluejLibDir();
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        
        // launch the debugger
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        debugger.newClassLoader(new BPClassLoader(new URL[] {launchDir.toURI().toURL()}, null));
        
        // wait til the debugger process has actually started
        debugger.getClass("shellInfiniteLoop");

        // load the test class, execute it
        new Thread() {
            public void run() {
                try {
                    debugger.runClassMain("shellInfiniteLoop");
                }
                catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        }.start();
        
        // find the debugger process
        Field [] fields =  debugger.getClass().getDeclaredFields();
        Object vmRef = getInstanceFieldObj(fields, debugger, "vmRef");
        fields = vmRef.getClass().getDeclaredFields();
        final Process process = (Process) getInstanceFieldObj(fields, vmRef, "remoteVMprocess");
        
        // reset the debugger
        debugger.close(false);
        
        // make sure the old process dies
        final Thread self = Thread.currentThread();
        new Thread() {
            public void run() {
                try {
                    process.waitFor();
                }
                catch(InterruptedException ie) {}
                self.interrupt();
            }
        }.start();
        
        try {
            Thread.sleep(5000); // 5 seconds should be plenty
        }
        catch(InterruptedException ie) {}
        
        boolean passed;
        try {
            process.exitValue();
            passed = true;
        }
        catch(IllegalThreadStateException itse) {
            passed = false;
        }
        
        if (! passed)
            process.destroy();
        
        assertTrue(passed);
        bluej.utility.Debug.message("TEST1 COMPLETED.");
    }
    
    /**
     * This tests that restarting the debug vm when an object is on the bench
     * goes smoothly.  Previously this caused a VMDisconnectedException.
     * (fails in version 2.0.1)<p>
     * 
     * 06/10/04
     * 
     * @author Davin McCall
     */
    public void test2() throws Exception
    {
        // Open up a project in a Package Manager frame
        File launchDir = Boot.getInstance().getBluejLibDir();
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test2");
        String projectPath = launchDir.getPath();
        
        Project.createNewProject(projectPath); // may already exist
        Project proj = Project.openProject(projectPath);
        if(proj == null)
            throw new NullPointerException();
        
        final Package pkg = new Package(proj);
        
        class Test2Runnable implements Runnable {
            
            public PkgMgrFrame pmf;
            
            public void run()
            {
                pmf = PkgMgrFrame.createFrame(pkg);
                
                // Find the default constructor for java.lang.Object
                
                View classView = View.getView(Object.class);
                ConstructorView[] constructors = classView.getConstructors();
                ConstructorView defObjCons = null;
                for (int i = 0; i < constructors.length; i++) {
                    if (constructors[i].getParameterCount() == 0)
                        defObjCons = constructors[i];
                }
                
                // Call the constructor, place the object on the bench.
                
                // pkg.getEditor().raiseMethodCallEvent(pkg, defObjCons);
                
                ResultWatcher watcher = new ResultWatcher() {
                    public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                    {
                        if (result != null) {
                            ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, "result");
                            pmf.getObjectBench().addObject(wrapper);

                            pmf.getPackage().getDebugger().addObject(pmf.getPackage().getId(), wrapper.getName(), result);

                            // pmf.getObjectBench().addInteraction(ir);
                            
                            synchronized(JdiTests.this) {
                                JdiTests.this.notify();
                            }
                        }
                        else {
                            // we can get here if the machine is terminated mid way
                            // through
                            // throw new Exception("test failure");
                        }
                    }

                    public void putError(String msg)
                    {}
                    public void putException(String msg)
                    {}
                };
                
                // The result watcher gets its messages on the GUI event queue.
                // However, junit runs on the GUI queue. To avoid deadlock, we
                // install a new queue for the result watcher.
                MyEventQueue myEventQueue = new MyEventQueue();
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(myEventQueue);
                synchronized(JdiTests.this) {
                    // new Invoker(this, cv, watcher).invokeInteractive();
                    new Invoker(pmf, defObjCons, watcher).invokeDirect(new String[0]);
                    
                    try {
                        JdiTests.this.wait();
                    }
                    catch(InterruptedException ie) {
                        throw new RuntimeException("test failure");
                    }
                }
                myEventQueue.pop();
            }
        }
        
        final Test2Runnable rb = new Test2Runnable();
        EventQueue.invokeAndWait(rb);
                    
        // Now reset the vm
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                rb.pmf.restartDebugger();
                rb.pmf.closePackage();
                PkgMgrFrame.closeFrame(rb.pmf);
            }
        });
    }
    
    /**
     * This tests that stepping into a breakpoint suspends the thread only
     * once. (fails in version 2.0.1).<p>
     * 
     * 12/10/04
     * 
     * @author Davin McCall
     */
    public void test3() throws Exception
    {
        // launch the debugger
        File launchDir = Boot.getInstance().getBluejLibDir();
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        debugger.newClassLoader(new BPClassLoader(new URL[] {launchDir.toURI().toURL()}, null));
        
        // wait til the debugger process has actually started
        debugger.getClass("breakpointTester");
        
        // set the breakpoints
        String x;
        x = debugger.toggleBreakpoint("breakpointTester", 21, true);
        if (x != null)
            System.out.println(x);
        x = debugger.toggleBreakpoint("breakpointTester", 22, true);
        if (x != null)
            System.out.println(x);
        
        // start class execution
        TestDebuggerListener tdl = new TestDebuggerListener(this);
        debugger.addDebuggerListener(tdl);
        
        Thread n;
        synchronized(this) {
            n = new Thread() {
                public void run() {
                    try {
                        debugger.runClassMain("breakpointTester");
                    }
                    catch(Throwable t) {
                        t.printStackTrace();
                    }
                }
            };
            n.start();
            
            // wait for first breakpoint hit
            while(tdl.breakpointEvents == 0)
                wait();
        
            // do a step
            tdl.lastEvent.getThread().step();
            
            // should get a step event and a breakpoint event
            while(tdl.breakpointEvents != 2 && tdl.stepEvents != 1)
                wait();
        
            // do a thread continue
            tdl.lastEvent.getThread().cont();
        }
        
        // wait (with timeout) for execution to finish
        n.join(5000); // wait 5 seconds at most
        
        // execution finished: ok, test passed.
        assertFalse(n.isAlive());
    }
    
    /**
     * This tests that throwing an exception at the top level works correctly.
     * (Ie. throwing an exception directly from the code pad works & doesn't
     * cause an internal BlueJ error).<p>
     * 
     * 19/11/04
     * 
     * @author Davin McCall
     */
    public void test4() throws Exception
    {
        // launch the debugger
        File launchDir = Boot.getInstance().getBluejLibDir();
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        debugger.newClassLoader(new BPClassLoader(new URL[] {launchDir.toURI().toURL()}, null));
        
        // load the test class, execute it
        Thread n = new Thread() {
            public void run() {
                try {
                    debugger.runClassMain("__SHELL99");
                }
                catch(Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        n.start();
        
        n.join();
        ExceptionDescription ed = debugger.getException();
        if (ed != null && ed.getText() != null)
            assertFalse(ed.getText().startsWith("Internal BlueJ error"));
        
        debugger.close(false);
    }
    
    /**
     * Test that an exception in a static initializer doesn't cause getClass
     * to bomb. (Causes hanging on inspection of a class).
     */
    public void test5() throws Exception
    {
        // launch the debugger
        File launchDir = Boot.getInstance().getBluejLibDir();
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.newClassLoader(new BPClassLoader(new URL[] {launchDir.toURI().toURL()}, null));

        // wait until it is ready
        TestDebuggerListener tdl = new TestDebuggerListener(this);
        debugger.addDebuggerListener(tdl);
        debugger.launch();
        tdl.waitReady();
        
        Thread n = new Thread() {
            public void run()
            {
                try {
                    debugger.getClass("initException");
                    flag1 = true;
                }
                catch (ClassNotFoundException cnfe) {}
            }
        };
        n.start();
        
        // wait at most 3 seconds for the class to be loaded
        n.join(3000);
        assertTrue(flag1);
        flag1 = false;
    }
    
    class TestDebuggerListener implements DebuggerListener
    {
        private Object syncObject;  // object used for synchronization
        
        public int breakpointEvents = 0;  // count of breakpoint events
        public int stepEvents = 0;   // count of step events
        public int currentState = Debugger.NOTREADY;  // current debugger state
        
        public DebuggerEvent lastEvent;
        
        public TestDebuggerListener(Object syncObject)
        {
            this.syncObject = syncObject;
        }
        
        /**
         * wait until the debugger is in the "idle" state.
         */
        public void waitReady()
        {
            synchronized (syncObject) {
                while (currentState != Debugger.IDLE) {
                    try {
                        syncObject.wait();
                    }
                    catch (InterruptedException ie) { }
                }
            }
        }
        
        public void debuggerEvent(DebuggerEvent e)
        {
            synchronized(syncObject) {
                
                if(e.getID() == DebuggerEvent.THREAD_BREAKPOINT)
                    breakpointEvents++;
                else if(e.getID() == DebuggerEvent.THREAD_HALT)
                    stepEvents++;
                else if(e.getID() == DebuggerEvent.DEBUGGER_STATECHANGED)
                    currentState = e.getNewState();
                
                lastEvent = e;
                
                syncObject.notify();
            }
        }
    }
    
    private class MyEventQueue extends EventQueue
    {
        public void pop()
        {
            super.pop();
        }
    }
}
