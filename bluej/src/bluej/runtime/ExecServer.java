package bluej.runtime;

import bluej.utility.Queue;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.SwingUtilities;

/**
 * Class that controls the runtime of code executed within BlueJ.
 * Sets up a SecurityManager, initial thread state, etc.
 *
 * This class both holds runtime attibutes and executes commands.
 * Execution is done through a call to the "performTask" method. The
 * performTask method is executed on the remote machine; its parameters
 * encode the actual action to be taken. See "performTask" for more detail.
 *
 * @author Michael Kolling
 */
public class ExecServer
{
    // task type constants:

    public static final int CREATE_LOADER  = 0;
    public static final int REMOVE_LOADER  = 1;
    public static final int LOAD_CLASS	   = 2;
    public static final int ADD_OBJECT     = 3;
    public static final int REMOVE_OBJECT  = 4;
    public static final int SET_LIBRARIES  = 5;
    public static final int SET_DIRECTORY  = 6;
    public static final int SERIALIZE_OBJECT = 7;
    public static final int DESERIALIZE_OBJECT = 8;
    public static final int SUPRESS_OUTPUT = 9;
    public static final int RESTORE_OUTPUT = 10;
    public static final int DISPOSE_WINDOWS = 11;


    /*package*/ static ExecServer server = null;
    /*package*/ static TerminateException terminateExc = new TerminateException("term");
    /*package*/ static ExitException exitExc = new ExitException("0");

    private RemoteClassMgr classmgr;
    private Map loaders;
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
        server = new ExecServer();
        server.suspendExecution();
    }

    /**
     *  This method is used to generate an event which is recorded
     *  by the local VM when handling System.exit(). See RemoteSecurityManager
     *  for details.
     */
    public static void exitMarker()
    {
        // <NON SUSPENDING BREAKPOINT!>
    }

    // -- instance methods --

    /**
     * Contructor: Initialise the execution server.
     */
    private ExecServer()
    {
        //Debug.message("[VM] creating server object");

        loaders = new HashMap();
        classmgr = new RemoteClassMgr();

        // the following causes the class loader mechanism to be initialised:
        // we attempt to load a (non-existent) class

        try {
            createClassLoader("#dummy", ".");
            loadClass("#dummy", "Dummy");
            removeClassLoader("#dummy");
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
    }

    /**
     *  This method is used to suspend the execution of this server thread.
     *  This is done via a breakpoint: a breakpoint is set in this method
     *  so calling this method suspends execution.
     */
    public void suspendExecution()
    {
        // <BREAKPOINT!>
        Debug.message("[VM] woke up from suspend");
    }



    /**
     * Perform a task here on the remote VM.
     *
     * This method is called from the main VM to initiate a task here on
     * this VM.
     */
    public Object performTask(int taskType, String arg1,
                               String arg2, String arg3, String arg4)
        throws Throwable
    {
        try {
            switch(taskType) {
            case CREATE_LOADER:
                return createClassLoader(arg1, arg2);
            case REMOVE_LOADER:
                removeClassLoader(arg1);
                return null;
            case LOAD_CLASS:
                loadClass(arg1, arg2);
                return null;
            case ADD_OBJECT:
                addObject(arg1, arg2, arg3, arg4);
                return null;
            case REMOVE_OBJECT:
                removeObject(arg1, arg2);
                return null;
            case SET_LIBRARIES:
                setLibraries(arg1);
                return null;
            case SET_DIRECTORY:
                setDirectory(arg1);
                return null;
            case SERIALIZE_OBJECT:
                serializeObject(arg1, arg2, arg3);
                return null;
            case DESERIALIZE_OBJECT:
                return deserializeObject(arg1, arg2, arg3, arg4);
            case SUPRESS_OUTPUT:
                supressErrorOutput();
                return null;
            case RESTORE_OUTPUT:
                restoreErrorOutput();
                return null;
            case DISPOSE_WINDOWS:
                disposeWindows();
                return null;
            }
        }
        catch(Exception e) {
            Debug.reportError("Exception while performing task: " + e);
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Create a new class loader for a given classpath.
     */
    private ClassLoader createClassLoader(String loaderId,
                                            String classPath)
    {
        //Debug.reportError("[VM] createClassLoader " + loaderId);
        ClassLoader loader = classmgr.getLoader(classPath);
        loaders.put(loaderId, loader);
        return loader;
    }


    /**
     * Remove a known loader from the table of class loaders.
     */
    private void removeClassLoader(String loaderId)
    {
        //Debug.reportError("[VM] removeLoader " + loaderId);
        loaders.remove(loaderId);
    }

    /**
     * Load a class in the remote runtime.
     */
    private Class loadClass(String loaderId, String classname)
        throws Exception
    {
        Class cl = null;

        if(loaderId == null)
            cl = classmgr.getLoader().loadClass(classname);
        else {
            ClassLoader loader = (ClassLoader)loaders.get(loaderId);
            if(loader != null)
                cl = loader.loadClass(classname);
    	}

        //Debug.reportError("   loaded.");
        if(cl == null)
            Debug.reportError("Could not load class for execution");
        else
            prepareClass(cl);

        return cl;
    }

    /**
     *  Run the initialisation ("prepare" method) of the new shell class.
     *  This guarantees that the class is properly prepared, as well as
     *  executing some init code in that shell method.
     */
    private void prepareClass(Class cl)
    {
        try {
            Method m = cl.getMethod("prepare", null);
            m.invoke(null, null);
        } catch(Exception e) {
            // ignore - some classes don't have prepare method. attempt to
            // call will still prepare the class
        }
    }

    /**
     *  Put an object into a package scope (for possible use as parameter
     *  later). Used after object creation to add the newly created object
     *  to the scope.
     */
    static void putObject(String scopeId, String instanceName, Object value)
    {
        //Debug.message("[VM] putObject: " + instanceName);
        Map scope = getScope(scopeId);
        scope.put(instanceName, value);

        // debugging
        // 	for (Iterator it = scope.keys(); it.hasNext(); ) {
        //  	    String s = (String)it.next();
        //  	    System.out.println("key: " + s);
        //  	}
    }

    /**
     */
    static void serializeObject(String scopeId, String instanceName, String fileName)
    {
        //Debug.message("[VM] serializeObject: " + instanceName);
        Map scope = getScope(scopeId);
        Object wrapObject = scope.get(instanceName);

        if (wrapObject == null) {
            System.out.println("wrap object was null for scope " + scopeId);
            System.out.println(scopes.toString());
        }

        try {
            FileOutputStream fo = new FileOutputStream(fileName);
            ObjectOutputStream so = new ObjectOutputStream(fo);
            so.writeObject(wrapObject);
            so.flush();
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }

    private Object deserializeObject(String loaderId, String scopeId, String newInstanceName,
                                     String fileName)
    {
        //Debug.message("[VM] deserializeObject: " + newInstanceName);
        Map scope = getScope(scopeId);
        Object obj = null;

        try {
            ClassLoader loader = (ClassLoader)loaders.get(loaderId);

            FileInputStream fi = new FileInputStream(fileName);
            ObjectInputStream si = new RemoteObjectInputStream(fi, loader);

            obj = si.readObject();

            scope.put(newInstanceName, obj);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
/*        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        catch(ClassNotFoundException cfne)
        {
            cfne.printStackTrace();
        } */

        System.out.println(obj.toString());

        return obj;
    }

    /**
     *  Add an object to package scope. The object to be added is held
     *  in the object 'instance', in field 'field'. (This is used when
     *  "Get" is selected in the object inspection to pull out the requested
     *  object and add it to the scope.)
     */
    static void addObject(String scopeId, String instance, String fieldName,
                            String newName)
    {
        //Debug.message("[VM] addObject: " + instance + ", " + fieldName + ", " + newName);

        Map scope = getScope(scopeId);
        Object wrapObject = scope.get(instance);
        try {
            Object obj;

            if (wrapObject.getClass().isArray()) {
                int slot = Integer.valueOf(fieldName.substring(5)).intValue();
                obj = Array.get(wrapObject, slot);
            } else {
                Field field = wrapObject.getClass().getField(fieldName);
                obj = field.get(wrapObject);
            }

            scope.put(newName, obj);
        }
        catch (Exception e) {
            e.printStackTrace();
            Debug.reportError("object field not found: " + fieldName +
                              " in " + instance);
            Debug.reportError("exception: " + e);
        }
    }

    /**
     *  Remove an object from a package scope. This has to be done tolerantly:
     *  If the named instance is not in the scope, we just quetly return.
     */
    static void removeObject(String scopeId, String instanceName)
    {
        //Debug.message("[VM] removeObject: " + instanceName);
        Map scope = getScope(scopeId);
        scope.remove(instanceName);
    }


    static Map getScope(String scopeId)
    {
        //Debug.message("[VM] getScope" + scopeId);
        Map scope = (Map)scopes.get(scopeId);

        if(scope == null) {
            scope = new HashMap();
            scopes.put(scopeId, scope);
        }
        return scope;
    }

    /**
     * Add the object to our list of open windows
     *
     * @param   o   a window object which has just been opened
     */
    private static void addWindow(final Object o)
    {
        openWindows.add(o);
        // experiment to try to fix windows bug where window
        // is hidden behind bluej window
/*        if (o instanceof Window) {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((Window)o).toFront();
                    }
                });
        } */
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
     * Redirect System.err to an invisible sink.
     */
    static void supressErrorOutput()
    {
        throwawayErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(throwawayErr));
    }

    /**
     * Restore the standard System.err
     */
    static void restoreErrorOutput()
    {
        System.setErr(systemErr);
    }

    /**
     * Dispose of all the top level windows we think are open
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
     *  Update the remote VM with the list of user/system libraries
     *  which the user has created using the ClassMgr.
     */
    private void setLibraries(String libraries)
    {
        classmgr.setLibraries(libraries);
    }

    /**
     *  Set the current working directory for this virtual machine.
     */
    private void setDirectory(String dir)
    {
        System.setProperty("user.dir", dir);
    }
}
