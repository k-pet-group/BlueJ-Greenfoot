package bluej.runtime;

import bluej.utility.Queue;
import bluej.utility.Debug;
import bluej.classmgr.ClassMgr;

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
    public static final int SET_LIBRARIES  = 5;


    static ExecServer server = null;
    static TerminateException terminateExc = new TerminateException("term");

    private RemoteClassMgr classmgr;
    private Hashtable loaders;
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
	System.setSecurityManager(manager);

	classmgr = new RemoteClassMgr();

	// the following causes the class loader mechanism to be initialised:
	// we attempt to load a (non-existant) class

	try {
	    createClassLoader("#dummy", ".");
	    loadClass("#dummy", "Dummy");
	    removeClassLoader("#dummy");
	}
	catch(Exception e) {
	    // ignore - we will get a ClassNotFound exception here
	}
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
     * Perform a task here on the remote VM.
     *
     * This method is called from the main VM to initiate a task here on 
     * this VM.
     */
    public ClassLoader performTask(int taskType, String arg1, 
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
	    }
	}
	catch(Exception e) {
	    Debug.message("Exception while performing task: " + e);
	}
	return null;
    }


    /**
     * Create a new class loader for a given classpath.
     */
    private ClassLoader createClassLoader(String loaderId, 
					       String classpath)
    {
	//Debug.reportError("[VM] createClassLoader " + loaderId);
	ClassLoader loader = classmgr.getLoader(classpath);
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
     * Find and return a class loader in the table of class loaders.
     */
    private ClassLoader getLoader(String loaderId)
    {
	return (ClassLoader)loaders.get(loaderId);
    }


    /**
     * Load a class in the remote runtime.
     */
    private Class loadClass(String loaderId, String classname)
	throws Exception
    {
	Class cl = null;

	//Debug.reportError("loading class " + classname);

	if(loaderId == null)
	    cl = classmgr.getLoader().loadClass(classname);
	else {
	    ClassLoader loader = getLoader(loaderId);
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
     *  later). Used after object creation to addthe newly created object
     *  to the scope.
     */
    static void putObject(String scopeId, String instanceName, Object value)
    {
	//Debug.message("[VM] putObject: " + instanceName);
	Hashtable scope = getScope(scopeId);
	scope.put(instanceName, value);
	// debugging
	//  	Enumeration e = scope.keys();
	// 	for (; e.hasMoreElements(); ) {
	//  	    String s = (String)e.nextElement();
	//  	    System.out.println("key: " + s);
	//  	}
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
	//Debug.message("[VM] addObject: " + newName);
	Hashtable scope = getScope(scopeId);
	Object wrapObject = scope.get(instance);
	try {
	    Field field = wrapObject.getClass().getField(fieldName);
	    Object obj = field.get(wrapObject);
	    scope.put(newName, obj);
	}
	catch (Exception e) {
	    Debug.reportError("Internal BlueJ error: " +
			      "object field not found: " + fieldName +
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
	Hashtable scope = getScope(scopeId);
	scope.remove(instanceName);
    }


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
     *  Update the remote VM with the list of user/system libraries
     *  which the user has created using the ClassMgr.
     */
    private void setLibraries(String libraries)
    {
	classmgr.setLibraries(libraries);
    } 
}
