package bluej.runtime;

import bluej.utility.Queue;
import bluej.utility.Debug;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Enumeration;


/**
 ** Class that controls the runtime of code executed within BlueJ.
 ** Sets up a SecurityManager, initial thread state, etc.
 **
 ** This class both holds runtime attibutes and executes commands. 
 ** Execution is done through a call to the "main" method. The main method
 ** is executed on the remote machine; its parameters encode the actual
 ** action to be taken. See "main" for more detail.
 **
 ** @author Michael Kolling
 **/
public class ExecServer
{
    // task type constants:

    public static final int CREATE_LOADER  = 0;
    public static final int REMOVE_LOADER  = 1;
    public static final int LOAD_CLASS	   = 2;
    public static final int ADD_OBJECT     = 3;
    public static final int REMOVE_OBJECT  = 4;


    static ExecServer server = null;
    private Hashtable loaders;

    // ==

    private static Hashtable scopes = new Hashtable();
	
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

    // -- instance methods --

    ExecServer()
    {
	//Debug.message("[VM] creating server object");

	loaders = new Hashtable();

	BlueJSecurityManager manager = new BlueJSecurityManager();
	//Debug.message("[VM] security manager created (not installed)");

	//System.setSecurityManager(manager);
	//Debug.message("[VM] security manager installed");
    }


    /**
     *  This method is used to suspend the execution of this server thread.
     *  This is done via a breakpoint: a breakpoint is set in this method
     *  so calling this method suspends execution.
     */
    public void suspendExecution()
    {
	// <BREAKPOINT!>
	//Debug.message("[VM] in suspend");
    }


    /**
     * Add a new task to the task queue and signal the server.
     * This method is called from the main VM to initiate a task here on the
     * remote VM.
     *
     * When this task is due to be performed, the server will call
     * "performTask" with this new task as the parameter.
     */
    public void signalStartTask(int taskType, String arg1, String arg2)
	 throws Throwable
    {
	performTask(taskType, arg1, arg2);
    }


    /**
     * Perform a task here on the remote VM. The task is described in the 
     * 'task' object.
     */
    private void performTask(int taskType, String arg1, String arg2)
	throws Throwable
    {
	switch(taskType) {

	case CREATE_LOADER:
	    createClassLoader(arg1, arg2);
	    break;
	case REMOVE_LOADER:
	    removeClassLoader(arg1);
	    break;
	case LOAD_CLASS:
	    loadClass(arg1, arg2);
	    break;
	case ADD_OBJECT:
	    break;
	case REMOVE_OBJECT:
	    break;
	}
    }

    /**
     * Create a new class loader for a given classpath.
     */
    private void createClassLoader(String loaderId, String classpath)
    {
	//Debug.message("[VM] createClassLoader " + loaderId);
	BlueJClassLoader loader = new BlueJClassLoader(classpath);
	loaders.put(loaderId, loader);
    }


    /**
     * Remove a known loader from the table of class loaders.
     */
    private void removeClassLoader(String loaderId)
    {
	//Debug.message("[VM] removeLoader " + loaderId);
	loaders.remove(loaderId);
    }


    /**
     * Find and return a class loader in the table of class loaders.
     */
    private BlueJClassLoader getLoader(String loaderId)
    {
	return (BlueJClassLoader)loaders.get(loaderId);
    }


    /**
     * Load a class in the remote runtime.
     */
    private Class loadClass(String loaderId, String classname)
	throws Throwable
    {
	Class cl = null;

	try {
  	    //Debug.message("loading class " + classname);

	    if(loaderId == null)
		cl = Class.forName(classname);
	    else {
		BlueJClassLoader loader = getLoader(loaderId);
		if(loader != null)
		    cl = loader.loadClass(classname);
	    }

	    if(cl == null)
		Debug.reportError("Could not load class for execution");
	    else
		prepareClass(cl);
	    
	} catch(Exception e) {
	    Debug.reportError ("Exception while trying to load class " + 
			       classname + ": " + e);
	}
	return cl;
    }

    /**
     *  Run the initialisation ("prepare" method) of the new shell class.
     *  This guarantees that the class is properly prepared, as well as
     *  executing some init code in that shell method.
     */
    private void prepareClass(Class cl)
	throws Throwable
    {
	try {
	    Method m = cl.getMethod("prepare", null);
	    m.invoke(null, null);
	} catch(Exception e) {
	    Debug.reportError("Exception while trying to prepare class:" + e);
	}
    }

    // ===

    static Hashtable getScope(String scopeId)
    {
	//Debug.message("[VM] getScope");
	Hashtable scope = (Hashtable)scopes.get(scopeId);

	if(scope == null) {
	    scope = new Hashtable();
	    scopes.put(scopeId, scope);
	}
	return scope;
    }
	
    /**
     * Put an object into a package scope (for possible use as parameter
     * later)
     */
    static void putObject(String scopeId, String instanceName, Object value)
    {
	//Debug.message("[VM] putObject: " + instanceName);
	Hashtable scope = getScope(scopeId);
	scope.put(instanceName, value);
    }


    /**
     * Add an object from to package scope. The object to be added is held
     * in object 'instance', in field 'field'.
     */
    static void addObject(String scopeId, String instance, String fieldName,
			  String newName)
    {
	//Debug.message("[VM] addObject: " + newName);
	Hashtable scope = getScope(scopeId);
	Object wrapObject = scope.get(instance);
	try {
	    Field field = wrapObject.getClass().getField(fieldName);
	    Object obj = field.get(wrapObject);
	    scope.put(newName, obj);
	}
	catch (Exception e) {
	    System.err.println("Internal BlueJ error: " +
			       "object field not found: " + fieldName +
			       " in " + instance);
	    System.err.println("exception: " + e);
	}
    }


    /**
     * Remove an object from a package scope.
     */
    static void removeObject(String scopeId, String instanceName)
    {
	//Debug.message("[VM] removeObject: " + instanceName);
	Hashtable scope = getScope(scopeId);
	scope.remove(instanceName);

	// debugging
	//  	Enumeration e = scope.keys();
	//  	for (; e.hasMoreElements(); ) {
	//  	    String s = (String)e.nextElement();
	//  	    System.out.println("key: " + s);
	//  	}
    }
}
