package bluej.debugger;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.BlueJFileReader;
import bluej.compiler.CompileObserver;
import bluej.compiler.JobQueue;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.ConstructorView;
import bluej.views.LabelPrintWriter;
import bluej.views.MemberView;
import bluej.views.CallableView;
import bluej.views.MethodView;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Debugger class that constructs a "shell" java source file, compiles it,
 * then loads the resulting class file and executes a method in a new thread.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Invoker.java 811 2001-03-25 23:11:51Z mik $
 */

public class Invoker extends Thread
	implements CompileObserver, MethodDialogWatcher
{
    private static final String creating = Config.getString("pkgmgr.creating");
    private static final String createDone = Config.getString("pkgmgr.createDone");

    public static final int OBJ_NAME_LENGTH = 8;
    public static final String SHELLNAME = "__SHELL";
    private static int shellNumber = 0;


    private static final synchronized String getShellName() {
        return SHELLNAME + (shellNumber++);
    }

    private static final synchronized String getResultId() {
        return "#result" + shellNumber;
    }

    /**
     * To allow each method/constructor dialog to have a call history we need
     * to cache the dialogs we create. We store the mapping from method to
     * dialog in this hashtable.
     */
    private static Map methods = new HashMap();

    private PkgMgrFrame pmf;
    private Package pkg;
    private ResultWatcher watcher;
    private CallableView member;
    private String shellName;
    private String objName;
    private String instanceName;
    private MethodDialog dialog;
    private boolean constructing;
    private String resultId;

    /**
     * Call a class's constructor, then create an ObjectWrapper for the
     * resulting object
     *
     * @param pmf       the frame of the package we are working on
     * @param member    the member to invoke
     * @param objName   the name of the object on which the method is called (has no
     *                  relevance when we are calling a constructor or static method)
     * @param watcher   an object interested in the result of the invocation
     */
    public Invoker(PkgMgrFrame pmf, CallableView member, String objName,
                   ResultWatcher watcher)
    {
        if (pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.member = member;
        this.watcher = watcher;

        this.shellName = getShellName();

        // in the case of a constructor, we need to construct an object name
        if (member instanceof ConstructorView) {

            String baseName = member.getClassName();
            int dot_index = baseName.lastIndexOf('.');
            if(dot_index >= 0)
                baseName = baseName.substring(dot_index + 1);

            // truncate long names to  OBJ_NAME_LENGTH plus _instanceNum
            int stringEndIndex =
                baseName.length() > OBJ_NAME_LENGTH ? OBJ_NAME_LENGTH : baseName.length();

            String instanceName = Character.toLowerCase(baseName.charAt(0)) +
                baseName.substring(1, stringEndIndex);

            this.objName = instanceName + "_" +
                                    member.getDeclaringView().getInstanceNum();

             constructing = true;
        }
        else if(member instanceof MethodView) {

            // in the case of a static method call, we use the class name as an
            // object name
            if(((MethodView)member).isStatic()) {
                this.objName = JavaNames.stripPrefix(member.getClassName());
            } else {
                this.objName = objName;
            }

            constructing = false;
        }
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
        // check for a method call with no parameter
        // if so, just do it
        if(!constructing && !member.hasParameters()) {
            doInvocation(null, null);
        }
        else {
            dialog = (MethodDialog)methods.get(member);

            if(dialog == null) {
                dialog = new MethodDialog(pmf,
                                          member.getClassName(),
                                          objName,
                                          member);
                methods.put(member, dialog);
            }
            else {
                if (constructing)
                    dialog.setNewInstanceName(objName);
                else
                    dialog.setInstanceName(objName);
            }
        }

        if(dialog != null) {
            LabelPrintWriter writer = new LabelPrintWriter();
            member.print(writer);
            dialog.setDescription(writer.getLabel());
            dialog.addWatcher(this);
            dialog.setVisible(true);
        }
    }

    // -- MethodDialogWatcher interface --

    /**
     * The method call dialog notified of an event. If it
     * is an OK, start doing the call.
     */
    public void methodDialogEvent(MethodDialog dlg, int event)
    {
        if(event == MethodDialog.CANCEL) {

            dialog.setVisible(false);
        }
        else if(event == MethodDialog.OK) {

            instanceName = dlg.getNewInstanceName();
            doInvocation(dlg.getArgs(), dlg.getArgTypes());
            pmf.setWaitCursor(true);
            if (constructing)
                pkg.setStatus(creating);
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
     * while "endCompile" will be executed by the CompilerThread.
     */
    protected void doInvocation(String[] args, Class[] argTypes)
    {
        Component[] wrappers;
        boolean dontQualify = false;

        // PENDING: this should be changed to write directly to file.
        // The hashtable mechanism doesn't make so much sense anymore
        // since most of it gets constructed here anyway.

        int numArgs = (args==null ? 0 : args.length);
        String className = member.getClassName();

        // if we happen to have a class in this package with the
        // same name as the start of the package, then fully qualifying names
        // does not work ie if the package is Test.Sub and we have
        // a class called Test, then all fully qualified names
        // fail ie new Test.Sub.Test() and new Test.Sub.Foo()
        // in the package where the class Test exists.
        // so in this case we need to use the unqualified name

        // note we need to retain the fully qualified case for when
        // we add library classes etc which may involve constructing
        // objects of types which are not in the current package
        if (!pkg.isUnnamedPackage()) {
            String pkgName = pkg.getQualifiedName();
            int lastDot = pkgName.indexOf(".");

            if (lastDot >= 0)
                pkgName = pkgName.substring(0, lastDot);

            // the first part of the package name exists as a target
            // then we must not qualify names
            if (pkg.getTarget(pkgName) != null)
                dontQualify = true;
        }

        // Create package specification line ("package xyz")

        Hashtable trans = new Hashtable();

        if(pkg.isUnnamedPackage())
            trans.put("PKGLINE", "");
        else
            trans.put("PKGLINE", "package " + pkg.getQualifiedName() + ";");

          // Create class name

        trans.put("CLASSNAME", shellName);

          // add variable declarations: one for a (possible) result, and one
          // for each parameter

        StringBuffer buffer = new StringBuffer();
        if(constructing)
            buffer.append("public static bluej.runtime.ObjectResultWrapper");
        else
            buffer.append("public static Object");
        buffer.append(" __bluej_runtime_result;" + Config.nl);
        trans.put("VARDECL", buffer.toString());

        // Build a string with parameter list: "(param0,param1,...)"

        buffer = new StringBuffer("(");
        StringBuffer argBuffer = new StringBuffer("(");
        if(numArgs>0) {
            buffer.append("__bluej_param0");
            argBuffer.append(args[0]);
        }
        for(int i = 1; i < numArgs; i++) {
            buffer.append(",__bluej_param" + i);
            argBuffer.append(", ");
            argBuffer.append(args[i]);
        }
        buffer.append(")");
        argBuffer.append(")");
        String argString = buffer.toString();
        String actualArgString = argBuffer.toString();

          // Build scope, ie. add one line for every object on the object
          // bench that gets the object and makes it available for use as
          // a parameter. Then add one line for each parameter setting the
          // parameter value.

        wrappers = pmf.getObjectBench().getComponents();
        buffer = new StringBuffer();
        String scopeId = Utility.quoteSloshes(pkg.getId());
        if(wrappers.length > 0)
            buffer.append("java.util.Map __bluej_runtime_scope = getScope(\""
        		  + scopeId + "\");" + Config.nl);
        for(int i = 0; i < wrappers.length; i++) {
            ObjectWrapper wrapper = (ObjectWrapper)wrappers[i];
            String type = (dontQualify ?
                                JavaNames.getBase(wrapper.className) :
                                wrapper.className);
            String instname = wrapper.instanceName;
            buffer.append("\t\t" + type + " " + instname + " = ");
            buffer.append("(" + type + ")__bluej_runtime_scope.get(\"");
            buffer.append(instname + "\");" + Config.nl);
        }
        for(int i = 0; i < numArgs; i++) {
            buffer.append("\t\t" +
                            (dontQualify ?
                              JavaNames.getBase(JavaNames.typeName(argTypes[i].getName())) :
                              JavaNames.typeName(argTypes[i].getName())));
            buffer.append(" __bluej_param" + i);
            buffer.append(" = " + args[i]);
            buffer.append(";" + Config.nl);
        }
        trans.put("SCOPEINIT", buffer.toString());

        buffer = new StringBuffer();
        String command;  // the interactive command in text form

        if(constructing) {
            command = "new " + (dontQualify ? JavaNames.getBase(className) : className);

            buffer.append("__bluej_runtime_result = makeObj(");
            buffer.append(command + argString + ");" + Config.nl);
            buffer.append("\t\tputObject(\"" + scopeId + "\", \"");
            buffer.append(instanceName);
            buffer.append("\", __bluej_runtime_result.result);");
        }
        else {	// it's a method call
            MethodView method = (MethodView)member;
            boolean isVoid = method.isVoid();

            if(method.isStatic())
                command = (dontQualify ?
                             JavaNames.getBase(className) :
                             className) + "." + method.getName();
            else
                command = objName + "." + method.getName();

            if(!isVoid)
                buffer.append("__bluej_runtime_result = makeObj(");
            buffer.append(command + argString);
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
        String shellFileName = new File(pkg.getPath(),shellName + ".java").getPath();

        try {
            BlueJFileReader.translateFile(templateFileName, shellFileName,
        				  trans);
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, command + actualArgString);

        String[] files = { shellFileName };
        JobQueue.getJobQueue().addJob(files, this, pkg.getProject().getClassPath(),
                                        pkg.getProject().getProjectDir().getPath());
    }

    // -- CompileObserver interface --

    public void startCompile(String[] sources) {}
    public void checkTarget(String sources) {}

    /**
     * An error was detected during compilation of the shell
     * class.
     */
    public void errorMessage(String filename, int lineNo, String message,
                                boolean invalidate)
    {
    	if(dialog != null) {
    	    dialog.setMessage("Error: " + message);
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

        pmf.setWaitCursor(false);

        if(successful)
            startClass();

        File srcFile = new File(pkg.getPath(), shellName + ".java");
        //srcFile.delete();

        File classFile = new File(pkg.getPath(), shellName + ".class");
        classFile.delete();

        if (constructing)
            pkg.setStatus(createDone);
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
            BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_STARTED, null);
            DebuggerClassLoader loader = pkg.getRemoteClassLoader();
            String shellClassName = pkg.getQualifiedName(shellName);
            Debugger.debugger.startClass(loader, shellClassName,
                                         pkg.getProject());
            BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_FINISHED, null);

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
                  // possible change: currently, exits don't get reported for
                  // void methods. maybe: exits should get reported if return
                  // value != 0
		  if(watcher != null) {
		      ExceptionDescription exc =
			  Debugger.debugger.getException();
		      pkg.reportExit(exc.getText());
		  }
		  break;

	      case Debugger.EXCEPTION:
		  ExceptionDescription exc = Debugger.debugger.getException();
		  String text =
		      JavaNames.stripPrefix(exc.getClassName());
		  if(exc.getText() != null)
		      text += ":\n" + exc.getText();

		  if(exc.getSourceFile() == null)
		      pkg.reportException(text);
		  else
		      pkg.exceptionMessage(
			   new File(pkg.getPath(), exc.getSourceFile()).getPath(),
			   exc.getLineNumber(), text, false);
		  break;

	      case Debugger.TERMINATED:  // terminated by user
		  // nothing to do
		  break;

	    } // switch
	} catch(Throwable e) {
	    e.printStackTrace(System.err);
	}
    }
}
