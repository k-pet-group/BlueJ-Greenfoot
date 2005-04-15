package bluej.debugger.jdi;

import java.awt.EventQueue;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Properties;

import junit.framework.TestCase;
import bluej.Boot;
import bluej.Config;
import bluej.debugger.*;
import bluej.debugger.jdi.JdiDebugger;
import bluej.debugmgr.ExpressionInformation;
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
 * @version $Id: JdiTests.java 3348 2005-04-15 02:36:36Z davmac $
 */
public class JdiTests extends TestCase
{
    public static final String bluejLibDir = calculateBluejLibDir().getPath();
    public static final boolean useClassesDir = true;
    
    protected File javaHomeDir = new File(System.getProperty("java.home"));
    private static String[] bluejJars = { "bluejcore.jar", "bluejeditor.jar", "bluejext.jar",
            "antlr.jar", "MRJ141Stubs.jar" };
    private static String[] bluejUserJars = { "junit.jar" };
    
    // various vars useful for testing
    boolean failed = false;
    boolean flag1 = false;
    
    private static File calculateBluejLibDir()
    {
        File bluejDir = null;
        String bootFullName = JdiTests.class.getResource("JdiTests.class").getFile();

        // Assuming the class is in a jar file, '!' separates the jar file name from the class name.        
        int classIndex = bootFullName.indexOf("!");
        String bootName = null;
        if (classIndex < 0) {
            // Boot.class is not in a jar-file. Find a lib directory somewhere
            // above us to use
            File startingDir = (new File(bootFullName).getParentFile());

            while((startingDir != null) &&
                   !(new File(startingDir.getParentFile(), "lib").isDirectory())) {
                        startingDir = startingDir.getParentFile();
            }
            
            if (startingDir == null)
                bluejDir = null;
            else
                bluejDir = new File(startingDir.getParentFile(), "lib");            
        } else {
            //It was in a jar. Cut of the class name
            bootName = bootFullName.substring(0, classIndex);
            bootName = getURLPath(bootName);

            File finalFile = new File(bootName);
            bluejDir = finalFile.getParentFile();
        }   
        
        return bluejDir;
    }
    
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
        Config.initialise(new File(bluejLibDir), new Properties());
        
        try {
            
            // Create a new instance of "Boot" and set the static instance
            // field as a reference to it

            Class bootClass = Class.forName("bluej.Boot");
            
            Constructor bootConstructor = bootClass.getDeclaredConstructor(new Class[] { String[].class });
            bootConstructor.setAccessible(true);
            Boot bootInstance = (Boot) bootConstructor.newInstance(new Object[] { new String[0] });

            Field [] fields =  bootClass.getDeclaredFields();
            setStaticField(fields, "instance", bootInstance);
            ClassLoader bootLoader = bootClass.getClassLoader();
            setInstanceField(fields, bootInstance, "bootLoader", bootLoader);
            setInstanceField(fields, bootInstance, "javaHomeDir", javaHomeDir);
            File bluejLibDirFile = new File(bluejLibDir);
            setInstanceField(fields, bootInstance, "bluejLibDir", bluejLibDirFile);
            
            //runtimeClassPath = getKnownJars(bluejLibDir, bluejJars, true);
            //runtimeUserClassPath = getKnownJars(bluejLibDir, bluejUserJars, false);
            //runtimeLoader = new URLClassLoader(runtimeClassPath, bootLoader);
            //userLibClassPath = getUserExtLibItems();
            URL [] runtimeClassPath = getKnownJars(bluejLibDirFile, bluejJars, true);
            setInstanceField(fields, bootInstance, "runtimeClassPath", runtimeClassPath);
            setInstanceField(fields, bootInstance, "runtimeUserClassPath", getKnownJars(bluejLibDirFile, bluejUserJars, false));
            setInstanceField(fields, bootInstance, "runtimeLoader", new URLClassLoader(runtimeClassPath, bootLoader));
            setInstanceField(fields, bootInstance, "userLibClassPath", new URL[0]);
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

    protected void setInstanceField(Field [] fields, Object instance, String fieldName, Object value) throws IllegalAccessException
    {
        int i = 0;
        for (i = 0; i < fields.length; i++) {
            if (fields[i].getName() == fieldName) {
                fields[i].setAccessible(true);
                fields[i].set(instance, value);
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
    
    /**
     * This is borrowed from the Boot class. Should really use the version
     * there rather than maintain this private copy.
     * 
     * @param libDir
     * @param jars
     * @param isSystem
     * @return
     * @throws MalformedURLException
     */
    private URL[] getKnownJars(File libDir, String[] jars, boolean isSystem) 
        throws MalformedURLException
    {
        // by default, we require all our known jars to be present
        int startJar = 0;
        ArrayList urlList = new ArrayList();
        
        // a hack to let BlueJ run from within Eclipse.
        // If specified on command line, lets add a ../classes
        // directory to the classpath (where Eclipse stores the
        // .class files)
        if (isSystem && useClassesDir) {
            File classesDir = new File(libDir.getParentFile(), "classes");
            
            if (classesDir.isDirectory()) {
                urlList.add(classesDir.toURL());
                // skip over requiring bluejcore.jar, bluejeditor.jar etc.
                startJar = 3;
            }
        }
        
        for (int i=startJar; i < jars.length; i++) {
            File toAdd = new File(libDir, jars[i]);
            
            if (!toAdd.canRead())
                throw new IllegalStateException("required jar is missing or unreadable: " + toAdd);
            
            urlList.add(toAdd.toURL());
        }
        
        if (isSystem) {
            // We also need to add tools.jar on some systems
            URL toolsURL = getToolsURL();
            if(toolsURL != null)
                urlList.add(toolsURL);
        }
        return (URL[]) urlList.toArray(new URL[0]);
    }

    /**
     * Also borrowed from Boot class. Should use that version rather than
     * maintain this private copy.
     * 
     * @return
     * @throws MalformedURLException
     */
    private URL getToolsURL() 
        throws MalformedURLException
    {
        String osname = System.getProperty("os.name", "");
        if(osname.startsWith("Mac"))     // we know it does not exist on a Mac...
            return null;
        
        File toolsFile = new File(javaHomeDir, "lib/tools.jar");
        if (toolsFile.canRead())
            return toolsFile.toURL();
        
        File parentDir = javaHomeDir.getParentFile();
        toolsFile = new File(parentDir, "lib/tools.jar");
        if (toolsFile.canRead())
            return toolsFile.toURL();
        else {
            // on other systems where we don't find it, we just warn. We don't expect it
            // to happen, but you never know...
            System.err.println("class Boot: tools.jar not found. Potential problem for execution.");
            return null;
        }
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
        File launchDir = new File(bluejLibDir);
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        
        // launch the debugger
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        
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
        
        // wait til the debugger process has actually started
        debugger.getClass("shellInfiniteLoop");
        
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
    }
    
    /**
     * This tests that restarting the debug vm when an object on the bench
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
        File launchDir = new File(bluejLibDir);
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

                            pmf.getPackage().getDebugger().addObject(wrapper.getName(), result);

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

                    /**
                     * We have no use of this information when using the constructor
                     */
                    public ExpressionInformation getExpressionInformation()
                    {
                        return null;
                    }
                };
                
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
        File launchDir = new File(bluejLibDir);
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        
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
        File launchDir = new File(bluejLibDir);
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);
        debugger.launch();
        
        TestDebuggerListener tdl = new TestDebuggerListener(this);
        
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
        File launchDir = new File(bluejLibDir);
        launchDir = launchDir.getParentFile();
        launchDir = new File(launchDir, "test");
        launchDir = new File(launchDir, "testprojects");
        launchDir = new File(launchDir, "test1");
        DebuggerTerminal term = new TestTerminal();
        final JdiDebugger debugger = new JdiDebugger(launchDir,term);

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
                }
                catch (ClassNotFoundException cnfe) {}
                flag1 = true;
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
}
