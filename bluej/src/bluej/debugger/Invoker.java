package bluej.debugger;

import bluej.Config;
import bluej.utility.Debug;
import bluej.compiler.CompileObserver;
import bluej.compiler.JobQueue;
import bluej.pkgmgr.Package;
import bluej.runtime.Shell;
import bluej.utility.Utility;
import bluej.views.ConstructorView;
import bluej.views.LabelPrintWriter;
import bluej.views.MemberView;
import bluej.views.MethodView;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Hashtable;
import sun.tools.java.ClassDeclaration;
import sun.tools.javac.SourceClass;
import sun.tools.javac.BatchEnvironment;

/**
 ** @version $Id: Invoker.java 104 1999-06-02 03:56:24Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** Debugger class that constructs a "shell" java source file, compiles it,
 ** then loads the resulting class file and executes a method in a new thread.
 **/

public class Invoker extends Thread 

	implements CompileObserver, MethodDialogWatcher, ObjectBenchWatcher
{
    private static String creating = Config.getString("pkgmgr.creating");
    private static String createDone = Config.getString("pkgmgr.createDone");

    private static final String SHELLNAME = "__SHELL";
    private static int shellNumber = 0;

    private static final synchronized String getShellName() {
	return SHELLNAME + (shellNumber++);
    }

    private static final synchronized String getResultId() {
	return "#result" + shellNumber;
    }
	
    private static Hashtable methods = new Hashtable();
	
    private Package pkg;
    private ResultWatcher watcher;
    private MemberView member;
    private String shellName;
    private String objName;
    private String instanceName;
    private boolean constructing;
    private MethodDialog dialog;
    private String resultId;

    /**
     * Call a class's constructor, then create an ObjectWrapper for the
     * resulting object
     *
     * @arg pkg - the Package we are working on
     * @arg member - the member to invoke
     * @arg objName - the name of the object on which the method is called
     * @arg watcher - an object interested in the result of the invocation
     */
    public Invoker(Package pkg, MemberView member, String objName, ResultWatcher watcher)
    {
	this.pkg = pkg;
	this.member = member;
	this.objName = objName;
	this.watcher = watcher;
		
	this.shellName = getShellName();
	if (member instanceof ConstructorView)
	    constructing = true;
	else if(member instanceof MethodView)
	    constructing = false;
	else
	    Debug.reportError("illegal member type in invokation");
	invoke();
    }
	
    /**
     * Execute the invokation. The details of the invokation
     * (object, method, etc) have been defined on creation of
     * this invoker object.
     */
    private void invoke()
    {
	String className = member.getClassName();
		
	if(constructing) {
	    ConstructorView cons = (ConstructorView)member;
			
	    dialog = (MethodDialog)methods.get(cons);

	    String baseName = className;
	    int dot_index = baseName.lastIndexOf('.');
	    if(dot_index >= 0)
		baseName = baseName.substring(dot_index + 1);

	    String objectName = Character.toLowerCase(baseName.charAt(0)) +
				baseName.substring(1) + "_" + 
				cons.getDeclaringView().getInstanceNum();
	    if(dialog == null) {
		dialog = new MethodDialog(pkg,
					  className,
					  objectName,
					  cons);
		methods.put(cons, dialog);
	    }
	    else
		dialog.setNewInstanceName(objectName);
	}
	else if(member instanceof MethodView) {
	    MethodView meth = (MethodView)member;
			
	    if(meth.hasParameters()) {
		dialog = (MethodDialog)methods.get(meth);
				
		if(dialog == null) {
		    dialog = new MethodDialog(pkg,
					      className,
					      objName,
					      meth);

		    methods.put(meth, dialog);
		}
		else
		    dialog.setInstanceName(objName);
	    }
	    else // no parameters - don't need dialog
		doInvocation(null, null);
	}
		
	if(dialog != null) {
	    LabelPrintWriter writer = new LabelPrintWriter();
	    member.print(writer);
	    dialog.setDescription(writer.getLabel());
	    dialog.addWatcher(this);
	    dialog.setVisible(true);
	    pkg.getBench().addWatcher(this);
	}
    }
	
    // -- ObjectBenchWatcher interface --

    /**
     * The object was selected interactively (by clicking
     * on it with the mouse pointer).
     */
    public void objectSelected(ObjectWrapper wrapper)
    {
	if(dialog != null)
	    dialog.insertText(wrapper.instanceName);
    }
	
    // -- MethodDialogWatcher interface --

    /**
     * The method call dialog notified of an event. If it
     * is an OK, start doing the call.
     */
    public void methodDialogEvent(MethodDialog dlg, int event)
    {
	if(event == MethodDialog.CANCEL){
	    pkg.getBench().removeWatcher(this);
	}
	else if(event == MethodDialog.OK) {
	    instanceName = dlg.getNewInstanceName();
	    pkg.getBench().removeWatcher(this);
	    doInvocation(dlg.getArgs(), dlg.getArgTypes());
	    pkg.getFrame().setWaitCursor(true);
	    if (constructing)
		pkg.getFrame().setStatus(creating);
	}
	else
	    Debug.reportError("Invoker: Unknown MethodDialog event");
    }

    // -- end of MethodDialogWatcher interface --

    /**
     * After all the interactive stuff is finished, finally do
     * the invocation of the method. (This can be a constructor
     * call or a normal method call.)
     *
     * Invocation here means: construct shell class and compile.
     * The execution is done once we return from compilation (in
     * method "endCompile").
     * Compilation is done asynchronously by the CompilerThread.
     *
     * This method is still executed in the interface thread,
     * while "endCompile" will be executed by the ComilerThread.
     */
    protected void doInvocation(String[] args, Class[] argTypes)
    {
	Component[] wrappers;

	// PENDING: this should be changed to write directly to file.
	// The hashtable mechanism doesn't make so much sense anymore
	// since most of it gets constructed here anyway.

	int numArgs = (args==null ? 0 : args.length);
	String className = member.getClassName();

	  // Create package specification line ("package xyz")

	Hashtable trans = new Hashtable();
	String pkgname = pkg.getName();
	if((pkgname == Package.noPackage))
	    trans.put("PKGLINE", "");
	else
	    trans.put("PKGLINE", "package " + pkgname + ";");

	  // Create class name

	trans.put("CLASSNAME", shellName);

	  // add variable declarations: one for a (possible) result, and one
	  // for each parameter

	StringBuffer buffer = new StringBuffer();
	if(constructing)
	    buffer.append("public static ObjectResultWrapper");
	else
	    buffer.append("public static Object");
	buffer.append(" __bluej_runtime_result;" + Config.nl);
	trans.put("VARDECL", buffer.toString());

	  // Build a string with parameter list: "(param0,param1,...)"

	buffer = new StringBuffer("(");
	if(numArgs>0)
	    buffer.append("__bluej_param0");
	for(int i = 1; i < numArgs; i++)
	    buffer.append(",__bluej_param" + i);
	buffer.append(")");
	String argString = buffer.toString();
 
	  // Build scope, ie. add one line for every object on the object
	  // bench that gets the object and makes it available for use as
	  // a parameter. Then add one line for each parameter setting the
	  // parameter value.

	wrappers = pkg.getBench().getComponents();
	buffer = new StringBuffer();
	String scopeId = Utility.quoteSloshes(pkg.getId());
	if(wrappers.length > 0)
	    buffer.append("Hashtable __bluej_runtime_scope = getScope(\"" 
			  + scopeId + "\");" + Config.nl);
	for(int i = 0; i < wrappers.length; i++) {
	    ObjectWrapper wrapper = (ObjectWrapper)wrappers[i];
	    String type = wrapper.className;
	    String instname = wrapper.instanceName;
	    buffer.append("\t\t" + type + " " + instname + " = "); 
	    buffer.append("(" + type + ")__bluej_runtime_scope.get(\"");
	    buffer.append(instname + "\");" + Config.nl);
	}
	for(int i = 0; i < numArgs; i++) {
	    buffer.append("\t\t" + Utility.typeName(argTypes[i].getName()));
	    buffer.append(" __bluej_param" + i);
	    buffer.append(" = " + args[i]);
	    buffer.append(";" + Config.nl);
	}
	trans.put("SCOPEINIT", buffer.toString());

	buffer = new StringBuffer();
	if(constructing) {
	    buffer.append("__bluej_runtime_result = makeObj(new "); 
	    buffer.append(className + argString + ");" + Config.nl);
	    buffer.append("\t\tputObject(\"" + scopeId + "\", \"");
	    buffer.append(instanceName);
	    buffer.append("\", __bluej_runtime_result.result);");
	}
	else {	// it's a method call
	    MethodView method = (MethodView)member;
	    boolean isVoid = method.isVoid();

	    if(!isVoid)
		buffer.append("__bluej_runtime_result = makeObj(");
	    if(method.isStatic())
		buffer.append(className + "." + method.getName() + argString);
	    else
		buffer.append(objName + "." + method.getName() + argString);
	    if(!isVoid)
		buffer.append(")");
	    buffer.append(";" + Config.nl);

	    if(!isVoid) {
		// generate and store unique ID for result object
		resultId = getResultId();
		buffer.append("\t\tputObject(\"" + scopeId + "\", \"");
		buffer.append(resultId);
		buffer.append("\", __bluej_runtime_result);");
	    }
	}
	trans.put("INVOCATION", buffer.toString());

	String templateFileName = Config.getLibFilename("template.shell");
	String shellFileName = pkg.getFileName(shellName) + ".java";
		
	try {
	    Utility.translateFile(templateFileName, shellFileName, trans);
	} catch(IOException e) {
	    e.printStackTrace();
	    return;
	}
		
	String[] files = { shellFileName };
	JobQueue.getJobQueue().addJob(files, this, pkg.getClasspath(), 
				      pkg.getClassDir());
    }

    // -- CompileObserver interface --

    public void startCompile(String[] sources) {}

    /**
     * An error was detected during compilation of the shell
     * class.
     */
    public void errorMessage(String filename, int lineNo, String message,
			     boolean invalidate)
    {
	if(dialog != null) {
	    dialog.setMessage("Error: " + message);
	    //added so that dialog can still insert from object bench after
	    // error
	    pkg.getBench().addWatcher(this);
	}
    }

    /**
     * The compilation of the shell class has ended. This method
     * is called by the CompilerThread after compilation. If
     * all went well, execute now. Then clean up.
     */
    public void endCompile(String[] sources, boolean successful)
    {
	if(dialog != null) {
	    dialog.setWaitCursor(false);
	    if(successful) {
		dialog.setVisible(false);
		dialog.updateParameters();
	    }
	}
	pkg.getFrame().setWaitCursor(false);

	if(successful)
	    startClass();

	File srcFile = new File(pkg.getFileName(shellName) + ".java");
	//srcFile.delete();

	File classFile = new File(pkg.getClassFileName(shellName) + ".class");
	classFile.delete();

	if (constructing)
	    pkg.getFrame().setStatus(createDone);
    }

    // -- end of CompileObserver interface --

    /**
     * Execute an interactive method call. At this point, the shell
     * class has been compiled and we are ready to go. If you want to
     * extend this to support concurrency (executing in a separate
     * thread), this is the place to do it: This could be done in 
     * another thread, which you could construct here. Careful,
     * though: you have to make sure that the clean-up (deleting
     * the class file) does not happen too early.
     *
     * This method is executed by the CompilerThread after
     * compilation.
     */
    public void startClass()
    {
	try {
	    pkg.getFrame().startExecution();
	    DebuggerClassLoader loader = pkg.getRemoteClassLoader();
	    String shellClassName = pkg.getQualifiedName(shellName);
	    Debugger.debugger.startClass(loader, shellClassName, pkg);
	    pkg.getFrame().stopExecution();

	    // the execution is completed, get the result if there was one
	    // (this could be either a construction of a function result)

	    handleResult(shellClassName);
	    
	    // update all open inspect windows
	    ObjectViewer.updateViewers();

	} catch(Throwable e) {
	    e.printStackTrace(System.err);
	}
    }

    /**
     * After an execution has finished, check whether there is a result
     * (such as a freshly created object, a function result or an exception)
     * and make sure that it gets processed appropriately.
     */
    public void handleResult(String shellClassName)
    {
	try {
	    // first, check whether we had an unexpected exit
	    int status = Debugger.debugger.getExitStatus();
	    switch(status) {

	      case Debugger.NORMAL_EXIT:
		  if(watcher != null) {
		      DebuggerObject result = Debugger.debugger.getStaticValue(
						shellClassName, 
						"__bluej_runtime_result");
		      if(constructing)
			  watcher.putResult(result, instanceName);
		      else
			  watcher.putResult(result, resultId);
		  }
		  break;

	      case Debugger.FORCED_EXIT:  // exit through System.exit()
		  if(watcher != null) {
		      ExceptionDescription exc = 
			  Debugger.debugger.getException();
		      pkg.reportExit(exc.getText());
		  }
		  break;

	      case Debugger.EXCEPTION:
		  ExceptionDescription exc = Debugger.debugger.getException();
		  String text = 
		      Utility.stripPackagePrefix(exc.getClassName());
		  if(exc.getText() != null)
		      text += ":\n" + exc.getText();

		  if(exc.getSourceFile() == null)
		      pkg.reportException(text);
		  else
		      pkg.errorMessage(
			   pkg.getFileName(exc.getSourceFile()),
			   exc.getLineNumber(), text, false);
		  break;

	    } // switch
	} catch(Throwable e) {
	    e.printStackTrace(System.err);
	}
    }

    public void notifyParsed(ClassDeclaration decl, SourceClass src, BatchEnvironment env) {}
    public void notifyCompiled(SourceClass src, BatchEnvironment env) {}
}
