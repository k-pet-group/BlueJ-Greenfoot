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
 ** Sets up a SecurityManager, terminal window, initial thread state, etc.
 **
 ** This class both holds runtime attibutes (such as the terminal, etc.)
 ** and executes commands. Execution is done through a call to the "main"
 ** method. The main method is executed on the remote machine; its
 ** parameters encode the actual action to be taken. See "main" for more
 ** detail.
 **
 ** @author Michael Kolling
 **/
public class ExecServer
{
    // task type constants:

    public static final int CREATE_LOADER  = 0;
    public static final int REMOVE_LOADER  = 1;
    public static final int START_CLASS	   = 2;
    public static final int LOAD_CLASS	   = 3;
    public static final int ADD_OBJECT     = 4;
    public static final int REMOVE_OBJECT  = 5;
    public static final int TERMINAL_SHOW  = 6;
    public static final int TERMINAL_HIDE  = 7;
    public static final int TERMINAL_CLEAR = 8;


    static ExecServer server = null;
    static TerminalFrame terminal;

    private Hashtable loaders;
    Queue tasks;

    // ==

    private static Hashtable scopes = new Hashtable();
	
    /**
     * Main method.
     *
     */
    public static void main(String[] args)
	throws Throwable
    {
	System.out.println("[VM] creating server");
	server = new ExecServer();
	//server.waitForTask();
	System.out.println("[VM] going to suspend");
	server.suspendExecution();
    }

    // -- instance methods --

    ExecServer()
    {
	System.out.println("[VM] creating server object");

	loaders = new Hashtable();
	tasks = new Queue();

	BlueJSecurityManager manager = new BlueJSecurityManager();
	System.out.println("[VM] security manager created (not installed)");

	//System.setSecurityManager(manager);
	//System.out.println("[VM] security manager installed");
			
	terminal = new TerminalFrame();
	System.out.println("[VM] terminal created");

	//terminal.doShow(); // testing!
	//System.out.println("[VM] terminal shown");

	//System.setIn(terminal.getInputStream());
	//PrintStream out = new PrintStream(terminal.getOutputStream());
	//System.setOut(out);
	// System.setErr(out);

    }


    public void suspendExecution()
    {
	System.out.println("[VM] in suspend");
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
	case START_CLASS:
	    startClass(arg1, arg2);
	    break;
	case LOAD_CLASS:
	    break;
	case ADD_OBJECT:
	    break;
	case REMOVE_OBJECT:
	    break;
	case TERMINAL_SHOW:
	    break;
	case TERMINAL_HIDE:
	    break;
	case TERMINAL_CLEAR:
	    break;
	}
    }

    /**
     * Create a new class loader for a given classpath.
     */
    private void createClassLoader(String loaderId, String classpath)
    {
	System.out.println("[VM] createClassLoader " + loaderId);
	BlueJClassLoader loader = new BlueJClassLoader(classpath);
	loaders.put(loaderId, loader);
    }


    /**
     * Remove a known loader from the table of class loaders.
     */
    private void removeClassLoader(String loaderId)
    {
	System.out.println("[VM] removeLoader");
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
     * Start executing a class in the remote runtime. That is: call its
     * main method with 'null' argument.
     */
    private void startClass(String loaderId, String classname)
	throws Throwable
    {
	System.out.println("[VM] startClass: " + classname);
	Class cl = loadClass(loaderId, classname);
	if(cl == null)
	    Debug.reportError("[VM] Could not load class");

	try {
	    Class[] params = { String[].class };
	    Method m = cl.getMethod("main", params);
			
	    Object[] meth_args = { null };
	    m.invoke(null, meth_args);
	} catch(InvocationTargetException e) {
	    // System.err.println("Exception during invocation " + e);
	    Throwable t = e.getTargetException();
	    throw t;
	} catch(Exception e) {
	    Debug.reportError("Exception while trying to start class " 
			      + classname
			      + ": " + e);
	}
	finally {
	    terminal.activate(false);
	}
    }


    /**
     * Load a class in the remote runtime.
     */
    private Class loadClass(String loaderId, String classname)
	throws Throwable
    {
	System.out.println("[VM] loadClass");
	Class cl = null;

	try {
  	    //System.out.println("loading class " + classname);

	    if(loaderId == null)
		cl = Class.forName(classname);
	    else {
		BlueJClassLoader loader = getLoader(loaderId);
		if(loader != null)
		    cl = loader.loadClass(classname);
		//Field[] f = cl.getFields();	// to force loading of class
		// into remote machine
	    }

	    if(cl == null)
		Debug.reportError("Could not load class for execution");
	    //else 
	    //System.out.println("class loaded");

	} catch(Exception e) {
	    Debug.reportError ("Exception while trying to load class " + 
			       classname + ": " + e);
	}
	return cl;
    }


    // ===

    static Hashtable getScope(String scopeId)
    {
	System.out.println("[VM] getScope");
	Hashtable scope = (Hashtable)scopes.get(scopeId);

	if(scope == null) {
	    scope = new Hashtable();
	    scopes.put(scopeId, scope);
	}
	return scope;
    }
	
    /**
     * Put an object into a package scope (for possible use as parameter later)
     */
    static void putObject(String scopeId, String instanceName, Object value)
    {
	System.out.println("[VM] putObject");
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
	System.out.println("[VM] addObject");
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
	System.out.println("[VM] ");
	Hashtable scope = getScope(scopeId);
	scope.remove(instanceName);

	// debugging
	//  	Enumeration e = scope.keys();
	//  	for (; e.hasMoreElements(); ) {
	//  	    String s = (String)e.nextElement();
	//  	    System.out.println("key: " + s);
	//  	}
    }

    /**
     * Pass a command to the terminal. The command string should be one of
     * the constant strings defined here.
     */
    public static final String TC_SHOW = "show";
    public static final String TC_HIDE = "hide";
    public static final String TC_CLEAR = "clear";

    static void terminalCommand(String command)
    {
	System.out.println("[VM] terminalCommand");
	if(command.equals(TC_SHOW))
	    terminal.doShow();
	else if(command.equals(TC_HIDE))
	    terminal.doClose();
	else if(command.equals(TC_CLEAR))
	    terminal.clear();
    }
}
