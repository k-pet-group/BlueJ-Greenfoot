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
// import bluej.tester.*;

import java.awt.Component;
import java.awt.Cursor;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Debugger class that arranges invocation of constructors or methods.
 * This class constructs a "shell" java source file, compiles it,
 * then loads the resulting class file and executes a method in a new thread.
 *
 * @author  Michael Kolling
 * @version $Id: Invoker.java 1371 2002-10-14 08:26:48Z mik $
 */

public class Invoker extends Thread
    implements CompileObserver, CallDialogWatcher
{
    private static final String creating = Config.getString("pkgmgr.creating");
    private static final String createDone = Config.getString("pkgmgr.createDone");

    public static final int OBJ_NAME_LENGTH = 8;
    public static final String SHELLNAME = "__SHELL";
    private static int shellNumber = 0;


    private static final synchronized String getShellName() {
        return SHELLNAME + (shellNumber++);
    }

    private static final synchronized String getUniqueResultName() {
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
    private CallDialog dialog;
    private boolean constructing;
    private String resultName;

    /**
     * Create an invoker for a free form statement or expression.
     */
    public Invoker(PkgMgrFrame pmf, FreeFormCallDialog callDialog, ResultWatcher watcher)
    {
        if (pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.watcher = watcher;
        this.member = null;
        this.shellName = getShellName();
        this.objName = null;
        this.instanceName = null;

        constructing = false;
        doFreeForm(callDialog);
    }
        
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
            Debug.reportError("illegal member type in invocation");

        invoke();
    }

    /**
     * Execute the invocation. The details of the invocation
     * (object, method, etc) have been defined on creation of
     * this invoker object.
     */
    private void invoke()
    {
        MethodDialog mDialog = null;
 
        // check for a method call with no parameter
        // if so, just do it
        if(!constructing && !member.hasParameters()) {
            doInvocation(null, null);
        }
        else {
            mDialog = (MethodDialog)methods.get(member);

            if(mDialog == null) {
                mDialog = new MethodDialog(pmf,
                                          member.getClassName(),
                                          objName,
                                          member);
                methods.put(member, mDialog);
            }
            else {
                if (constructing)
                    mDialog.setNewInstanceName(objName);
                else
                    mDialog.setInstanceName(objName);
            }
        }

        if(mDialog != null) {
            LabelPrintWriter writer = new LabelPrintWriter();
            member.print(writer);
            mDialog.setDescription(writer.getLabel());
            mDialog.setWatcher(this);
            mDialog.setVisible(true);
        }
        dialog = mDialog;
    }

    /* Start a free form invocation. That is: Show the free form
     * evaluation dialog, then sit back and wait for a callback.
     */
    private void doFreeForm(FreeFormCallDialog callDialog)
    {
        dialog = callDialog;
        dialog.setWatcher(this);
        dialog.setVisible(true);
        // after this, wait for callDialogEvent
    }

    // -- CallDialogWatcher interface --

    /**
     * The call dialog notified of an event. If it
     * is an OK, start doing the call.
     */
    public void callDialogEvent(CallDialog dlg, int event)
    {
        if(event == CallDialog.CANCEL) {

            dialog.setVisible(false);
        }
        else if(event == CallDialog.OK) {

            if(dlg instanceof MethodDialog) {
                MethodDialog mDialog = (MethodDialog)dlg;
                instanceName = mDialog.getNewInstanceName();
                doInvocation(mDialog.getArgs(), mDialog.getArgTypes());
                pmf.setWaitCursor(true);
                if (constructing)
                    pkg.setStatus(creating);
            }
            else if(dlg instanceof FreeFormCallDialog) {
                FreeFormCallDialog ffDialog = (FreeFormCallDialog)dlg;
                doFreeFormInvocation(ffDialog.getExpression(), ffDialog.getHasResult());
            }
        }
        else
            Debug.reportError("Invoker: Unknown CallDialog event");
    }

    // -- end of CallDialogWatcher interface --

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
        int numArgs = (args==null ? 0 : args.length);
        String className = member.getClassName();

        // prepare variables (assigned with actual values) for each parameter
        
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < numArgs; i++) {
            buffer.append("\t\t" + cleverQualifyTypeName(pkg, argTypes[i].getName()));
            buffer.append(" __bluej_param" + i);
            buffer.append(" = " + args[i]);
            buffer.append(";" + Config.nl);
        }
        String paramInit = buffer.toString();

        // Build two strings with  parameter lists: one using the variable names
        // "(__bluej_param0,__bluej_param1,...)" for internal use, one using the 
        // actual values for interface display.

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

        // build the invocation string

        buffer = new StringBuffer();
        String command;             // the interactive command in text form
        boolean isVoid = false;
        
        if(constructing) {
            command = "new " + cleverQualifyTypeName(pkg, className);
            resultName = instanceName;
            //             CallRecord.getCallRecord(instanceName, member, args);
        }
        else {  // it's a method call
            MethodView method = (MethodView)member;
            isVoid = method.isVoid();

            if(method.isStatic())
                command = cleverQualifyTypeName(pkg, className) + "." + method.getName();
            else {
                command = objName + "." + method.getName();

                //                CallRecord.addMethodCallRecord(objName, method.getName(),
                //                                                member, args);
            }

            // generate and store unique ID for result object
            resultName = getUniqueResultName();
        }

        File shell = writeInvocationFile(pkg, paramInit, 
                            command + argString, resultName, constructing, isVoid);

        compileInvocationFile(shell);
        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, command + actualArgString);
    }

    /**
     * Arrange to execute a free form (text) invocation.
     *
     * Invocation here means: construct shell class and compile.
     * The execution is done once we return from compilation (in
     * method "endCompile").
     * Compilation is done asynchronously by the CompilerThread.
     *
     * This method is still executed in the interface thread,
     * while "endCompile" will be executed by the CompilerThread.
     */
    protected void doFreeFormInvocation(String executionString, boolean hasResult)
    {
        // generate and store unique ID for result object
        resultName = getUniqueResultName();
        
        File shell = writeInvocationFile(pkg, "", executionString, 
                                         resultName, false, !hasResult);
                                         //constructing, isVoid);

        compileInvocationFile(shell);
        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, executionString);
    }

    /**
     * Write a source file for a class to do the interactive invocation.
     */
    private File writeInvocationFile(Package pkg, 
                                     String paramInit, String callString, String resultName,
                                     boolean constructing, boolean isVoid)
    {
        // PENDING: this should be changed to write directly to file.
        // The hashtable mechanism doesn't make so much sense anymore
        // since most of it gets constructed here anyway.

        Hashtable trans = new Hashtable();

        // Create package specification line ("package xyz")

        String packageLine;
        if(pkg.isUnnamedPackage())
            packageLine = "";
        else
            packageLine = "package " + pkg.getQualifiedName() + ";";

        trans.put("PKGLINE", packageLine);
        trans.put("CLASSNAME", shellName);
        
        // add variable declaration for a (possible) result

        StringBuffer buffer = new StringBuffer();
        if(constructing)
            buffer.append("public static bluej.runtime.ObjectResultWrapper");
        else
            buffer.append("public static Object");
        buffer.append(" __bluej_runtime_result;" + Config.nl);

        trans.put("VARDECL", buffer.toString());

        // Build scope, ie. add one line for every object on the object
        // bench that gets the object and makes it available for use as
        // a parameter. Then add one line for each parameter setting the
        // parameter value.

        buffer = new StringBuffer();
        String scopeId = Utility.quoteSloshes(pkg.getId());
        Component[] wrappers = pmf.getObjectBench().getComponents();

        if(wrappers.length > 0)
            buffer.append("java.util.Map __bluej_runtime_scope = getScope(\""
                          + scopeId + "\");" + Config.nl);
        for(int i = 0; i < wrappers.length; i++) {
            ObjectWrapper wrapper = (ObjectWrapper)wrappers[i];
            String type = cleverQualifyTypeName(pkg, wrapper.className);
            String instname = wrapper.instanceName;

            buffer.append("\t\t" + type + " " + instname + " = ");
            buffer.append("(" + type + ")__bluej_runtime_scope.get(\"");
            buffer.append(instname + "\");" + Config.nl);
        }
        
        trans.put("SCOPEINIT", buffer.toString());
        trans.put("PARAMINIT", paramInit);

        // build the invocation string

        buffer = new StringBuffer();

        if(constructing) {
            buffer.append("__bluej_runtime_result = makeObj((Object)");
            buffer.append(callString + ");" + Config.nl);
            buffer.append("\t\tputObject(\"" + scopeId + "\", \"");
            buffer.append(resultName);
            buffer.append("\", __bluej_runtime_result.result);");
        }
        else {  // it's a method call
            if(!isVoid)
                buffer.append("__bluej_runtime_result = makeObj(");
            buffer.append(callString);
            if(!isVoid)
                buffer.append(")");
            buffer.append(";" + Config.nl);

            if(!isVoid) {
                buffer.append("\t\tputObject(\"" + scopeId + "\", \"");
                buffer.append(resultName);
                buffer.append("\", __bluej_runtime_result);");
            }
        }
        trans.put("INVOCATION", buffer.toString());

        File templateFile = Config.getTemplateFile("shell");
        File shellFile = new File(pkg.getPath(), shellName + ".java");

        try {
            BlueJFileReader.translateFile(templateFile, shellFile,
                                          trans);
        } catch(IOException e) {
            e.printStackTrace();
        }
        return shellFile;
    }

    /**
     * Start the compilation of a shell fine and register us as a watcher.
     * After this, we just wait for the callback from the compiler.
     */    
    private void compileInvocationFile(File shellFile)
    {
        String[] files = { shellFile.getPath() };
        JobQueue.getJobQueue().addJob(files, this, pkg.getProject().getClassPath(),
                                      pkg.getProject().getProjectDir().getPath());
    }

    // -- CompileObserver interface --

    // not interested in these events:
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
            dialog.setErrorMessage("Error: " + message);
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
                if(dialog instanceof MethodDialog)
                    ((MethodDialog)dialog).updateParameters();
            }
        }

        pmf.setWaitCursor(false);

        if(successful)
            startClass();

        File srcFile = new File(pkg.getPath(), shellName + ".java");
        srcFile.delete();

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
                    watcher.putResult(result, resultName);
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
                String text = exc.getClassName();
                if(text != null) {
                    text = JavaNames.stripPrefix(text) + ":\n" + exc.getText();
                    pkg.exceptionMessage(exc.getStack(), text, false);
                } else {
                    pkg.reportException(exc.getText());
                }
                break;

            case Debugger.TERMINATED:  // terminated by user
                // nothing to do
                break;

            } // switch
        } catch(Throwable e) {
            e.printStackTrace(System.err);
        }
    }
    
    private String cleverQualifyTypeName(Package p, String typeName)
    {
        // if we happen to have a class in this package with the
        // same name as the start of the package, then fully qualifying names
        // does not work ie if the package is Test.Sub and we have
        // a class called Test, then all fully qualified names
        // fail ie new Test.Sub.Test() and new Test.Sub.Foo()
        // in the package where the class Test exists.
        // so in this case we need to use the unqualified name
        // ie new Test() and new Foo()

        // note we need to retain the fully qualified case for when
        // we add library classes etc which may involve constructing
        // objects of types which are not in the current package

        if (!p.isUnnamedPackage()) {
            String pkgName = p.getQualifiedName();
            int lastDot = pkgName.indexOf(".");

            if (lastDot >= 0)
                pkgName = pkgName.substring(0, lastDot);

            // if the first part of the package name exists as a target
            // lets unqualify the typeName
            if (p.getTarget(pkgName) != null)
                typeName = JavaNames.getBase(typeName);
        }

        // now we need to deal with nested types
        // these types will have a $ in them but depending on whether
        // they are anonymous classes or member classes will change how
        // we want to refer to them
        // an anonymous class we can refer to as MyClass$1 and the
        // compiler is ok with that
        // a member class, despite being given a type name of
        // MyClass$MemberClass, must be referred to as MyClass.MemberClass
        // in the source code. Here we make this change to the typeName
        // based entirely on whether the character following the $ is
        // a numeral or not (which just happens to be the way it is at
        // the moment but I don't think its written down in the JLS or
        // anything so this may break)

        int firstDollar;

        if ((firstDollar = typeName.indexOf('$')) != -1) {
            StringBuffer sb = new StringBuffer(typeName);

            // go to length - 1 only so we always have an i+1 character
            // to check. What this means is that if the last character
            // in the typeName is a $, it won't be converted but I don't
            // think a type name with a $ as the last character is valid
            // anyway
            for(int i=firstDollar; i<sb.length()-1; i++) {
                if (sb.charAt(i) == '$' &&
                    !Character.isDigit(sb.charAt(i+1)))
                    sb.setCharAt(i, '.');
            }

            typeName = sb.toString();
        }

        return JavaNames.typeName(typeName);
    }
}
