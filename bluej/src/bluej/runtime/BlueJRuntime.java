package bluej.runtime;

import bluej.utility.Debug;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Enumeration;

import sun.tools.debug.SunHooks;

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
 ** @author Michael Cahill
 **/
public class BlueJRuntime
{
    public static final String INIT = "init";
    public static final String CREATE_LOADER = "createLoader";
    public static final String REMOVE_LOADER = "removeLoader";
    public static final String ADD_OBJECT = "addObject";
    public static final String REMOVE_OBJECT = "removeObject";
    public static final String START_CLASS = "startClass";
    public static final String LOAD_CLASS = "loadClass";
    public static final String TERM_COMMAND = "terminal";
	
    static TerminalFrame terminal;
    private static Hashtable loaders = new Hashtable();
    private static Hashtable scopes = new Hashtable();
	
    static boolean initialised = false;

    BlueJRuntime()
    {
	if(!initialised) {
	    BlueJSecurityManager manager = new BlueJSecurityManager();
	    System.setSecurityManager(manager);
			
	    terminal = new TerminalFrame();
	    System.setIn(terminal.getInputStream());
	    PrintStream out = new PrintStream(terminal.getOutputStream());
	    System.setOut(out);
	    // System.setErr(out);
			
	    initialised = true;
	}
    }

    static Hashtable getScope(String scopeId)
    {
	Hashtable scope = (Hashtable)scopes.get(scopeId);

	if(scope == null) {
	    scope = new Hashtable();
	    scopes.put(scopeId, scope);
	}
	return scope;
    }
	
    /**
    ** Main method.
    ** Invoked in the remote runtime by the BlueJ debugger to perform
    ** certain actions. This ugliness is forced by the inflexibility of
    ** Sun's debugger interface.
    **
    ** The actions being performed through this method are:
    **
    **	INIT		- initialise the runtime
    **	CREATE_LOADER	- create the class loader
    **	REMOVE_LOADER	- remove the class loader
    **	ADD_OBJECT	- add an object to a package scope
    **	REMOVE_OBJECT	- remove an object from a package scope
    **	START_CLASS	- start a class (run its main method)
    **	LOAD_CLASS	- load a class (but don't run it)
    **	TERM_COMMAND	- pass a command on to the terminal object
    **
    ** START_CLASS is used to run Shell classes to execute interactive
    ** calls.
    **
    ** The action itself is passed in as the first argument; each action
    ** may have additional arguments.
    **/
    public static void main(String[] args)
	 throws Throwable
    {
	if(args.length == 0)		// no args - do nothing
	    return;

	if(INIT.equals(args[0]))		// no further arguments
	    new BlueJRuntime();

	else if(CREATE_LOADER.equals(args[0]))	// arg[1] == scopeID
	    createLoader(args[1], args[2]);	// arg[2] == classpath

	else if(REMOVE_LOADER.equals(args[0]))	// arg[1] == scopeID
	    removeLoader(args[1]);

	else if(ADD_OBJECT.equals(args[0]))	// arg[1] == scopeID
	    addObject(args[1], args[2], args[3], args[4]);
						// arg[2] == instanceName
						// arg[3] == fieldName
						// arg[4] == newName

	else if(REMOVE_OBJECT.equals(args[0]))	// arg[1] == scopeID
	    removeObject(args[1], args[2]);	// arg[2] == instancename

	else if(START_CLASS.equals(args[0])) {	// arg[1] == scopeID/loaderID
						// arg[2] == classname
						// arg[3..n] == method args

	    String[] tail_args = new String[args.length - 3];
	    System.arraycopy(args, 3, tail_args, 0, tail_args.length);
	    startClass(args[1], args[2], tail_args);
	}

	else if(LOAD_CLASS.equals(args[0]))	// arg[1] == scopeID/loaderID
	    loadClass(args[1], args[2]);	// arg[2] == classname

	else if(TERM_COMMAND.equals(args[0]))	// arg[1] == command
	    terminalCommand(args[1]);

	else
	    System.err.println("Unknown runtime command " + args[0]);

	SunHooks.die();
    }

    static void createLoader(String loaderId, String classpath)
    {
	BlueJClassLoader loader = new BlueJClassLoader(classpath);
	loaders.put(loaderId, loader);
    }

    static BlueJClassLoader getLoader(String loaderId)
    {
	return (BlueJClassLoader)loaders.get(loaderId);
    }

    static void removeLoader(String loaderId)
    {
	loaders.remove(loaderId);
    }

    /**
     * Put an object into a package scope (for possible use as parameter later)
     */
    static void putObject(String scopeId, String instanceName, Object value)
    {
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
     * Start executing a class in the remote runtime.
     */
    static void startClass(String loaderId, String classname, String[] args)
	 throws Throwable
    {
	Class cl = loadClass(loaderId, classname);

	try {
	    Class[] params = { String[].class };
	    Method m = cl.getMethod("main", params);
			
	    Object[] meth_args = { args };
	    m.invoke(null, meth_args);
	} catch(InvocationTargetException e) {
	    // System.err.println("Exception during invocation " + e);
	    Throwable t = e.getTargetException();
	    throw t;
	} catch(Exception e) {
	    Debug.reportError("Exception " + e + 
				" while trying to start class " + classname);
	}
	finally {
	    terminal.activate(false);
	}
    }

    /**
     * Load a class in the remote runtime.
     */
    static Class loadClass(String loaderId, String classname)
	 throws Throwable
    {
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

    /**
     * Pass a command to the terminal. The command string should be one of
     * the constant strings defined here.
     */
    public static final String TC_SHOW = "show";
    public static final String TC_HIDE = "hide";
    public static final String TC_CLEAR = "clear";

    static void terminalCommand(String command)
    {
	if(command.equals(TC_SHOW))
	    terminal.doShow();
	else if(command.equals(TC_HIDE))
	    terminal.doClose();
	else if(command.equals(TC_CLEAR))
	    terminal.clear();
    }
}
