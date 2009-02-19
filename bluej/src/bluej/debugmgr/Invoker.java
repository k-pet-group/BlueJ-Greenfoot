/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugmgr;

import java.awt.EventQueue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bluej.BlueJEvent;
import bluej.Config;
import bluej.compiler.CompileObserver;
import bluej.compiler.EventqueueCompileObserver;
import bluej.compiler.JobQueue;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.GenTypeWildcard;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.NameTransform;
import bluej.debugger.gentype.TextType;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.*;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * Debugger class that arranges invocation of constructors or methods. This
 * class constructs a "shell" java source file, compiles it, then loads the
 * resulting class file and executes a method in a new thread.
 * 
 * @author Michael Kolling
 * @version $Id: Invoker.java 6163 2009-02-19 18:09:55Z polle $
 */

public class Invoker
    implements CompileObserver, CallDialogWatcher
{
    private static final String creating = Config.getString("pkgmgr.creating");
    private static final String createDone = Config.getString("pkgmgr.createDone");

    public static final int OBJ_NAME_LENGTH = 8;
    public static final String SHELLNAME = "__SHELL";
    private static int shellNumber = 0;

    private static final synchronized String getShellName()
    {
        return SHELLNAME + (shellNumber++);
    }

    /**
     * To allow each method/constructor dialog to have a call history we need to
     * cache the dialogs we create. We store the mapping from method to dialog
     * in this hashtable.
     */
    private static Map<CallableView, MethodDialog> methods = new HashMap<CallableView, MethodDialog>();

    private PkgMgrFrame pmf;
    private Package pkg;
    private ResultWatcher watcher;
    private CallableView member;
    private String shellName;
    private String objName;
    private Map<?, ?> typeMap; // map type parameter names to types
    private ValueCollection localVars;
    private String imports; // import statements to include in shell file
    
    /**
     * The instance name for any object we create. For a constructed object the
     * user sets it in the dialog. For a method call with result we set this to
     * "result". For a void method we set this to null.
     */
    private String instanceName;

    private CallDialog dialog;
    private boolean constructing;

    private String commandString;
    private ExecutionEvent executionEvent;
    private InvokerRecord ir;

    /**
     * Create an invoker for a free form statement or expression. After using this
     * constructor, optionally call setImports(), then call doFreeFormInvocation()
     * to perform compilation and execution.
     */
    public Invoker(PkgMgrFrame pmf, ValueCollection localVars, String command, ResultWatcher watcher)
    {
        if (pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        if (watcher == null)
            throw new NullPointerException("Invoker: watcher == null");

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.watcher = watcher;
        this.member = null;
        this.shellName = getShellName();
        this.objName = null;
        this.instanceName = null;
        this.localVars = localVars;

        constructing = false;
        executionEvent = new ExecutionEvent(this.pkg);
        commandString = command;
    }

    /**
     * Call a class's constructor OR call a static method and create an
     * ObjectWrapper for the resulting object
     * 
     * @param pmf
     *            the frame of the package we are working on
     * @param member
     *            the member to invoke
     * @param watcher
     *            an object interested in the result of the invocation
     */
    public Invoker(PkgMgrFrame pmf, CallableView member, ResultWatcher watcher)
    {
        if (pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        if (watcher == null)
            throw new NullPointerException("Invoker: watcher == null");

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.member = member;
        this.watcher = watcher;

        this.shellName = getShellName();

        // in the case of a constructor, we need to construct an object name
        if (member instanceof ConstructorView) {

            this.objName = pmf.getProject().getDebugger().guessNewName(member.getClassName());

            constructing = true;
            executionEvent = new ExecutionEvent(member.getClassName(),null);
        }
        else if (member instanceof MethodView) {

            // in the case of a static method call, we use the class name as an
            // object name
            if (((MethodView) member).isStatic()) {
                this.objName = JavaNames.stripPrefix(member.getClassName());
                executionEvent = new ExecutionEvent(member.getClassName(), null );
            }
            else {
                executionEvent = new ExecutionEvent(member.getClassName(), objName);
            }

            constructing = false;
        }
        else
            Debug.reportError("illegal member type in invocation");
        executionEvent.setPackage(pkg);
    }

    /**
     * Call an instance method on an object
     * 
     * @param pmf
     *            the frame of the package we are working on
     * @param member
     *            the member to invoke
     * @param objWrapper
     *            the object to invoke the method on
     * @param watcher
     *            an object interested in the result of the invocation
     */
    public Invoker(PkgMgrFrame pmf, MethodView member, ObjectWrapper objWrapper, ResultWatcher watcher)
    {
        if (pmf.isEmptyFrame())
            throw new IllegalArgumentException();

        if (watcher == null)
            throw new NullPointerException("Invoker: watcher == null");

        this.pmf = pmf;
        this.pkg = pmf.getPackage();
        this.member = member;
        this.watcher = watcher;

        this.shellName = getShellName();

        // We want a map of all the type parameters that may appear in the
        // method signature to the corresponding instantiation types from the
        // object to which the method is being applied.
        //
        // Tpar names in the method signature however correspond to names from
        // the class in which the method was declared. So we need to map tpars
        // from the object's class to that class.
        this.objName = objWrapper.getName();
        this.typeMap = objWrapper.getObject().getGenType().mapToSuper(member.getClassName()).getMap();

        executionEvent = new ExecutionEvent(member.getClassName(), objName);
        
        constructing = false;
        executionEvent.setPackage(pkg);
    }

    /**
     * Set the import statements that should be in effect when this invocation
     * is performed.
     * 
     * @param importStatements   The import statements in complete and valid java syntax
     */
    public void setImports(String importStatements)
    {
        imports = importStatements;
    }
    
    /**
     * Open a dialog to get further information about the requested invocation, or
     * if no information is needed (ie. no parameters) then just proceed with the
     * invocation.
     * 
     * When the dialog is complete, it will call proceed with the invocation
     * (see callDialogEvent).
     */
    public void invokeInteractive()
    {
        //in greenfoot mode we don't ever want to ask for instance name
        if(constructing && Config.isGreenfoot()) {     
            instanceName = objName;
        }
        // check for a method call with no parameter
        // if so, just do it
        if ((!constructing || Config.isGreenfoot()) && !member.hasParameters()) {
            dialog = null;
            doInvocation(null, (JavaType []) null, null);
        }
        else {
            MethodDialog mDialog = methods.get(member);

            if (mDialog == null) {
                mDialog = new MethodDialog(pmf, objName, member, typeMap);
                methods.put(member, mDialog);
                mDialog.setVisible(true);
            }
            else {
                mDialog.setInstanceInfo(objName, typeMap);
            }

            mDialog.setEnabled(true);
            mDialog.setWatcher(this);
            dialog = mDialog;
        }
    }

    /**
     * After attempting a free form invocation, and getting an error, we try
     * again. First time round, we tried interpreting the input as an
     * expression, now we try as a statement.
     */
    public synchronized void tryAgain()
    {
        doFreeFormInvocation(null);
    }

    // -- CallDialogWatcher interface --

    /**
     * The call dialog notified of an event. If it is an OK, start doing the
     * call.
     */
    public void callDialogEvent(CallDialog dlg, int event)
    {
        if (event == CallDialog.CANCEL) {

            dlg.setVisible(false);
        }
        else if (event == CallDialog.OK) {

            if (dlg instanceof MethodDialog) {
                MethodDialog mDialog = (MethodDialog) dlg;
                mDialog.setEnabled(false);
                instanceName = mDialog.getNewInstanceName();                
                String[] actualTypeParams = mDialog.getTypeParams();
                
                pmf.setWaitCursor(true);
                doInvocation(mDialog.getArgs(), mDialog.getArgGenTypes(true), actualTypeParams);
                
                if (constructing)
                    pkg.setStatus(creating);
            }
        }
        else
            Debug.reportError("Invoker: Unknown CallDialog event");
    }

    // -- end of CallDialogWatcher interface --

    /**
     * Invokes a constructor or method with the given parameters.
     */
    public void invokeDirect(String[] params)
    {
        if (instanceName == null)
            instanceName = objName;

        final JavaType[] argTypes = member.getParamTypes(false);
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = argTypes[i].mapTparsToTypes(typeMap);
        }
        
        doInvocation(params, argTypes, null);
    }

    /**
     * After all the interactive stuff is finished, finally do the invocation of
     * the method. (This can be a constructor call or a normal method call.)
     * 
     * Invocation here means: construct shell class and start compiling it.
     * 
     * The "endCompile" method is called when the compilation has completed. If
     * successful, the shell class will then be executed.
     *  
     * @param args  The arguments to the method/constructor as they will appear
     *              in the generated source
     * @param argTypes  The argument types (ignored for generic callables);
     *              type parameters have been mapped to actual types
     * @param typeParams  Specifies the type parameters as supplied by the
     *                    user
     */
    protected void doInvocation(String[] args, JavaType[] argTypes, String[] typeParams)
    {
        int numArgs = (args == null ? 0 : args.length);

        // prepare variables (assigned with actual values) for each parameter
        String [] argTypeStrings;
        if (argTypes != null)
            argTypeStrings = new String[argTypes.length];
        else
            argTypeStrings = null;
        
        if (! member.isGeneric() || member.isConstructor()) {
            for (int i = 0; i < numArgs; i++) {
                JavaType argType = argTypes[i];
                
                if (argType instanceof GenTypeWildcard) {
                    GenTypeSolid [] ubounds = ((GenTypeWildcard) argType).getUpperBounds();
                    
                    if (ubounds.length != 0)
                        argType = ubounds[0];
                    else
                        argType = new TextType("java.lang.Object");
                }
                
                argTypeStrings[i] = argType.toString(new CleverQualifyTypeNameTransform(pkg));
            }
        }

        executionEvent.setParameters(argTypes, args);
        if (constructing) {
            executionEvent.setObjectName(instanceName);
        }
        else {
            executionEvent.setMethodName(((MethodView) member).getName());
        }

        doInvocation(args, argTypeStrings, typeParams);
    }

    /**
     * Workhorse doInvocation method which takes a string array for the
     * argument types instead of a GenType array. This constructs the code strings,
     * writes the invocation file, compiles it and eventually executes it.
     */
    private void doInvocation(String[] args, String[] argTypes, String[] typeParams)
    {
        int numArgs = (args == null ? 0 : args.length);
        final String className = member.getClassName();

        // Generic methods currently require special handling
        boolean isGenericMethod = member.isGeneric() && ! member.isConstructor();
        
        // prepare variables (assigned with actual values) for each parameter
        StringBuffer buffer = new StringBuffer();
        if (! isGenericMethod) {
            for (int i = 0; i < numArgs; i++) {
                buffer.append(argTypes[i]);
                buffer.append(" __bluej_param" + i);
                buffer.append(" = " + args[i]);
                buffer.append(";" + Config.nl);
            }
        }
        String paramInit = buffer.toString();

        // Build two strings with parameter lists: one using the variable names
        // "(__bluej_param0,__bluej_param1,...)" for internal use, one using the
        // actual values for interface display.

        buffer.setLength(0);
        StringBuffer argBuffer = new StringBuffer();
        buildArgStrings(buffer, argBuffer, args);
        String argString = buffer.toString();
        String actualArgString = argBuffer.toString();
        if (isGenericMethod)
            argString = actualArgString; 
        
        // build the invocation string

        buffer.setLength(0);
        String command; // the interactive command in text form
        boolean isVoid = false;

        String constype = null;
        if (constructing) {
            constype = cleverQualifyTypeName(pkg, className);
            if (typeParams != null && typeParams.length > 0) {
                constype += "<";
                for (int i = 0; i < typeParams.length; i++) {
                    String typeParam = typeParams[i];
                    constype += typeParam;
                    if (i < (typeParams.length - 1)) {
                        constype += ",";
                    }
                }
                constype += ">";
            }
            command = "new " + constype;
            ir = new ConstructionInvokerRecord(constype, instanceName, command + actualArgString, args);

            //          BeanShell
            //commandAsString = command + actualArgString;
        }
        else { // it's a method call
            MethodView method = (MethodView) member;
            isVoid = method.isVoid();

            if (method.isStatic())
                command = cleverQualifyTypeName(pkg, className) + "." + method.getName();
            else {
                command = objName + "." + method.getName();
            }

            if (isVoid) {
                if (method.isMain()) {
                    // if we are calling a main method then we want to simulate a
                    // new launch of an application, so first of all we unload all our
                    // classes (prevents problems with static variables not being
                    // reinitialised because the class hangs around from a previous
                    // call)
                    if (! Config.isGreenfoot()) {
                        pmf.getProject().removeClassLoader();
                        pmf.getProject().newRemoteClassLoaderLeavingBreakpoints();
                    }

                    ir = new StaticVoidMainMethodInvokerRecord();
                } 
                else {
                    ir = new VoidMethodInvokerRecord(command + actualArgString, args);
                }
                instanceName = null;
            }
            else {
                ir = new MethodInvokerRecord(method.getGenericReturnType(), command + actualArgString, args, pmf);
                instanceName = "result";
            }

            //          BeanShell
            //commandAsString = "bluej.runtime.Shell.makeObj(" + command +
            // actualArgString + ");";
        }

        if (constructing && member.getParameterCount() == 0 && (typeParams == null || typeParams.length == 0)) {
            // Special case for construction of a class using the default constructor.
            // We can do this without writing and compiling a shell file.
            
            commandString = command + actualArgString;
            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, commandString);
            
            // We must however do so in a seperate thread. Otherwise a constructor which
            // goes into an infinite loop can hang BlueJ.
            new Thread() {
                public void run() {
                    final DebuggerResult result = pkg.getProject().getDebugger().instantiateClass(className);
                    
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            // the execution is completed, get the result if there was one
                            // (this could be either a construction or a function result)
                            
                            handleResult(result); // handles error situations
                                
                            closeCallDialog();
                            pmf.setWaitCursor(false);
                            
                            // update all open inspect windows
                            pkg.getProject().updateInspectors();
                        }
                    });
                }
            }.start();
        }
        else {
            if (isVoid)
                argString += ';';
            
            File shell = writeInvocationFile(paramInit, command + argString, isVoid, constype);
            if (shell != null) {
                commandString = command + actualArgString;
                compileInvocationFile(shell);
            }
            else {
                endCompile(new File[0], false);
            }
        }
    }

    /**
     * Build up two strings representing the arguments to a method/constructor
     * call as a comma-seperated list enclosed in braces ie. (x, y, z)<p>
     * 
     * The first buffer gets the form (__bluej_param0, __bluej_param1 ...)
     * while the second gets the arguments as supplied by the user.<p>
     * 
     * @param buffer    The first buffer
     * @param argBuffer The second buffer
     * @param args      The arguments supplied by the user
     */
    protected void buildArgStrings(StringBuffer buffer, StringBuffer argBuffer, String[] args)
    {
        int numArgs = args == null ? 0 : args.length;
        
        buffer.append("(");
        argBuffer.append("(");
        if (numArgs > 0) {
            buffer.append("__bluej_param0");
            argBuffer.append(args[0]);
        }
        for (int i = 1; i < numArgs; i++) {
            buffer.append(",__bluej_param" + i);
            argBuffer.append(", ");
            argBuffer.append(args[i]);
        }
        buffer.append(")");
        argBuffer.append(")");
    }
    
    /**
     * Arrange to execute a free form (text) invocation.
     * 
     * Invocation here means: construct shell class and compile. The execution
     * is done once we return from compilation (in method "endCompile").
     * Compilation is done asynchronously by the CompilerThread.
     * 
     * This method is still executed in the interface thread, while "endCompile"
     * will be executed by the CompilerThread.
     * 
     * Returns true if successful, or false if there was a problem (the shell
     * file couldn't be written). In case of failure, a dialog is displayed to
     * alert the user.
     */
    public boolean doFreeFormInvocation(String resultType)
    {
        boolean hasResult = resultType != null;
        if (hasResult) {
            if (resultType.equals(""))
                resultType = null;
            instanceName = "result";
            ir = new ExpressionInvokerRecord(commandString);
        }
        else {
            instanceName = null;
            // this is a statement, treat as a void method result
            ir = new VoidMethodInvokerRecord(commandString, null);
        }

        File shell = writeInvocationFile("", commandString, !hasResult, resultType);
        if (shell != null) {
            executionEvent.setCommand(commandString);
            compileInvocationFile(shell);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Write a source file for a class (the 'shell file') to do the interactive
     * invocation. Returns the written file, or null if the file cannot be written
     * (an error dialog will be shown in this case).
     * 
     * <p>A shell file has the following form:
     * 
     * <p><pre>
     * $PKGLINE
     * $IMPORTS
     * public class $CLASSNAME extends bluej.runtime.Shell
     * {
     *   $VARDECL
     * 
     *   public static void run() throws Throwable
     *   {
     *     $SCOPEINIT
     *     $PARAMINIT
     *     $INVOCATION
     *   }
     * }
     * </pre><p>
     * 
     * PARAMINIT and INVOCATION correspond directly to the parameters
     * 'paramInit' and 'callString' as passed to this method.<p>
     * 
     * VARDECL declares a static member variable, __bluej_runtime_result,
     * whose type depends on 'constructing' and 'constype' parameters. Only
     * present if 'isVoid' is false.<p>
     * 
     * SCOPEINIT declares a Map, __bluej_runtime_scope, which maps object
     * names to their values (allowing objects from the object bench to be
     * accessed).
     * 
     *  
     * @param pkg   the Package in which scope to execute
     * @param paramInit  java code which initializes parameter variables
     * @param callString java code which executes requested method/code
     * @param constructing  true to store the result directly, false to wrap
     *                      it in an ObjectResultWrapper
     * @param isVoid   true if no result is returned
     * @param constype  the exact type of the object being constructed. Only
     *                  needed if 'constructing' is true.
     */
    private File writeInvocationFile(String paramInit, String callString,
            boolean isVoid, String constype)
    {
        // Create package specification line ("package xyz")
        String packageLine;
        if (pkg.isUnnamedPackage())
            packageLine = "";
        else
            packageLine = "package " + pkg.getQualifiedName() + ";";

        // add variable declaration for a (possible) result

        StringBuffer buffer = new StringBuffer();
        if (!isVoid) {
            if (constructing) {
                buffer.append("public static ");
                buffer.append(constype);
            }
            else
                buffer.append("public static java.lang.Object");
            buffer.append(" __bluej_runtime_result;");
            buffer.append(Config.nl);
        }
        String resultDecl = buffer.toString();
        buffer.setLength(0);
        
        // Build scope, ie. add one line for every object on the object
        // bench that gets the object and makes it available for use as
        // a parameter. Then add one line for each parameter setting the
        // parameter value.

        // A sample of the code generated
        //  java.util.Map __bluej_runtime_scope = getScope("BJIDC:\\aproject");
        //  JavaType instnameA = (JavaType) __bluej_runtime_scope("instnameA");
        //  OtherJavaType instnameB = (OtherJavaType)
        // __bluej_runtime_scope("instnameB");

        String scopeId = Utility.quoteString(pkg.getId());
        Iterator<ObjectWrapper> wrappers = pmf.getObjectBench().getValueIterator();
        NameTransform cqtTransform = new CleverQualifyTypeNameTransform(pkg);

        Map<String, String> objBenchVarsMap = new HashMap<String, String>();
        
        if (wrappers.hasNext() || localVars != null) {
            buffer.append("final java.util.Map __bluej_runtime_scope = getScope(\"" + scopeId + "\");" + Config.nl);
        
            // writeVariables("", buffer, false, wrappers, cqtTransform);
            while (wrappers.hasNext()) {
                NamedValue objBenchVar = (NamedValue) wrappers.next();
                objBenchVarsMap.put(objBenchVar.getName(), getVarDeclString("", false, objBenchVar, cqtTransform));
            }
        }
        
        // put the local variables here if we don't know the result type. If we do know
        // the result type, we put the local variables inside the result wrapper object
        // later on.
        if (localVars != null && constype == null) {
            // writeVariables("lv:", buffer, false, localVars.getValueIterator(), cqtTransform);
            Iterator<?> i = localVars.getValueIterator();
            while (i.hasNext()) {
                NamedValue localVar = (NamedValue) i.next();
                objBenchVarsMap.put(localVar.getName(), getVarDeclString("lv:", false, localVar, cqtTransform));
            }
        }
        
        Iterator<String> obVarsIterator = objBenchVarsMap.values().iterator();
        while (obVarsIterator.hasNext()) {
            buffer.append(obVarsIterator.next().toString());
        }
        
        String vardecl = buffer.toString();
        buffer.setLength(0);

        // build the invocation string
        
        if (constructing) {
            // A sample of the code generated (for a constructor)
            //  __bluej_runtime_result = new SomeType(2,"adb");

            buffer.append(shellName);
            buffer.append(".__bluej_runtime_result = ");
            buffer.append(callString);
        }
        else {
            // A sample of the code generated (for a method call)
            //  __bluej_runtime_result = makeObj(2+new String("ap").length());

            if (!isVoid) {
                buffer.append(shellName);
                if (constype == null) {
                    buffer.append(".__bluej_runtime_result = makeObj(");
                }
                else {
                    buffer.append(".__bluej_runtime_result = new java.lang.Object() { ");
                    buffer.append(constype + " result;");
                    if (localVars != null) {
                        buffer.append("{ ");
                        writeVariables("lv:", buffer, false, localVars.getValueIterator(), cqtTransform);
                        buffer.append("this.result = ");
                    }
                }
            }
            buffer.append(callString);
            // Append a new line, as the call string may end with a //-style comment
            buffer.append(Config.nl);
            if (!isVoid && constype == null) {
                buffer.append(")");
            }
        }

        if(! isVoid && ! callString.endsWith(";")) {
            buffer.append(";");
        }
        buffer.append(Config.nl);

        String invocation = buffer.toString();
        
        // save altered local variable values
        buffer = new StringBuffer();
        if (localVars != null) {
            for (Iterator<?> i = localVars.getValueIterator(); i.hasNext();) {
                NamedValue wrapper = (NamedValue) i.next();
                if (! wrapper.isFinal() || ! wrapper.isInitialized()) {
                    String instname = wrapper.getName();
                    
                    buffer.append("__bluej_runtime_scope.put(\"lv:" + instname + "\", "); 
                    wrapValue(buffer, instname, wrapper.getGenType());
                    buffer.append(");" + Config.nl);
                }
            }
        }
        String scopeSave = buffer.toString();

        File shellFile = new File(pkg.getPath(), shellName + ".java");
        BufferedWriter shell = null;
        try {
            shell = new BufferedWriter(new FileWriter(shellFile));

            shell.write(packageLine);
            shell.newLine();
            if (imports != null) {
                shell.write(imports);
                shell.newLine();
            }
            shell.write("public class ");
            shell.write(shellName);
            shell.write(" extends bluej.runtime.Shell {");
            shell.newLine();
            shell.write(resultDecl);
            shell.newLine();
            shell.write("public static void run() throws Throwable {");
            shell.newLine();
            shell.write(vardecl);
            shell.newLine();
            shell.write(paramInit);
            shell.write(invocation);
            shell.write(scopeSave);
            if (! isVoid && constype != null && ! constructing) {
                shell.write("} };");
            }
            shell.newLine();
            shell.write("}}");
            shell.close();
        }
        catch (IOException e) {
            DialogManager.showError(pmf, "could-not-write-shell-file");
            if (shell != null) {
                try {
                    shell.close();
                }
                catch (IOException ioe) {}
            }
            shellFile.delete();
            return null;
        }
        return shellFile;
    }
    
    /**
     * Write out shell code to retrieve the values of variables or bench objects.
     * 
     * @param scopePx  The scope prefix ("lv:" for local variables)
     * @param buffer   The string buffer to write the code to
     * @param isStatic  True if the variables should be declared static
     * @param i        An iterator through the variables to write
     * @param nt       The name transform to use
     */
    private void writeVariables(String scopePx, StringBuffer buffer, boolean isStatic, Iterator<?> i, NameTransform nt)
    {
        for (; i.hasNext();) {
            NamedValue wrapper = (NamedValue) i.next();
            if (wrapper.isInitialized()) {
                String type = wrapper.getGenType().toString(nt);
                String instname = wrapper.getName();
                
                if (wrapper.isFinal())
                    buffer.append("final ");
                if (isStatic)
                    buffer.append("static ");
                
                buffer.append(type);
                
                buffer.append(" " + instname + " = ");
                extractValue(buffer, scopePx, instname, wrapper.getGenType(), type);
                buffer.append(Config.nl);
            }
        }
    }
    
    /**
     * Get the string to declare a variable.
     * @param scopePx   The scope prefix for the value map
     * @param isStatic  True if the variable should be declared static
     * @param wrapper   The NamedValue representing the variable (and its name)
     * @param nt        The name transform to use for class names
     * 
     * @return the string to declared the variable
     */
    private String getVarDeclString(String scopePx, boolean isStatic, NamedValue wrapper, NameTransform nt)
    {
        if (wrapper.isInitialized()) {
            String type = wrapper.getGenType().toString(nt);
            String instname = wrapper.getName();
            StringBuffer buffer = new StringBuffer();
            
            if (wrapper.isFinal())
                buffer.append("final ");
            if (isStatic)
                buffer.append("static ");
            
            buffer.append(type);
            
            buffer.append(" " + instname + " = ");
            extractValue(buffer, scopePx, instname, wrapper.getGenType(), type);
            buffer.append(Config.nl);
            
            return buffer.toString();
        }
        else {
            return "";
        }
    }

    /**
     * Write code to extract a value of a given type.
     * @param buffer    The buffer in which the expression is stored
     * @param scopePx   The scope prefix ("" for object bench, "lv:" for codepad)
     * @param instname  The name of the value (used as map key)
     * @param type      The type of the value
     */
    private void extractValue(StringBuffer buffer, String scopePx, String instname, JavaType type, String typeStr)
    {
        if (type.isPrimitive()) {
            // primitive type. Must pull a wrapped object out, and then
            // unwrap it.
            
            String castType;
            String extractMethod;
            
            if (type.typeIs(JavaType.JT_BOOLEAN)) {
                castType = "java.lang.Boolean";
                extractMethod = "booleanValue";
            }
            else if (type.typeIs(JavaType.JT_CHAR)) {
                castType = "java.lang.Character";
                extractMethod = "charValue";
            }
            else if (type.typeIs(JavaType.JT_BYTE)) {
                castType = "java.lang.Byte";
                extractMethod = "byteValue";
            }
            else if (type.typeIs(JavaType.JT_SHORT)) {
                castType = "java.lang.Short";
                extractMethod = "shortValue";
            }
            else if (type.typeIs(JavaType.JT_INT)) {
                castType = "java.lang.Integer";
                extractMethod = "intValue";
            }
            else if (type.typeIs(JavaType.JT_LONG)) {
                castType = "java.lang.Long";
                extractMethod = "longValue";
            }
            else if (type.typeIs(JavaType.JT_FLOAT)) {
                castType = "java.lang.Float";
                extractMethod = "floatValue";
            }
            else if (type.typeIs(JavaType.JT_DOUBLE)) {
                castType = "java.lang.Double";
                extractMethod = "doubleValue";
            }
            else {
                throw new UnsupportedOperationException("unhandled primitive type");
            }
            
            buffer.append("((" + castType + ")__bluej_runtime_scope.get(\"");
            buffer.append(scopePx + instname + "\"))." + extractMethod + "();");
        }
        else {
            // reference (object) type. Much easier.
            buffer.append("(" + typeStr);
            buffer.append(")__bluej_runtime_scope.get(\"");
            buffer.append(scopePx + instname + "\");" + Config.nl);
        }
    }
    
    /**
     * Wrap a value, if necessary, as an appropriate object type.
     * @param buffer  The resulting expression is written to this buffer
     * @param name    The name of the variable holding the value
     * @param type    The type of the value
     */
    private void wrapValue(StringBuffer buffer, String name, JavaType type)
    {
        if (type.isPrimitive()) {
            if (type.typeIs(JavaType.JT_BOOLEAN))
                buffer.append("java.lang.Boolean.valueOf(" + name + ")");
            else if (type.typeIs(JavaType.JT_BYTE))
                buffer.append("new java.lang.Byte(" + name + ")");
            else if (type.typeIs(JavaType.JT_CHAR))
                buffer.append("new java.lang.Character(" + name + ")");
            else if (type.typeIs(JavaType.JT_DOUBLE))
                buffer.append("new java.lang.Double(" + name + ")");
            else if (type.typeIs(JavaType.JT_FLOAT))
                buffer.append("new java.lang.Float(" + name + ")");
            else if (type.typeIs(JavaType.JT_LONG))
                buffer.append("new java.lang.Long(" + name + ")");
            else if (type.typeIs(JavaType.JT_INT))
                buffer.append("new java.lang.Integer(" + name + ")");
            else if (type.typeIs(JavaType.JT_SHORT))
                buffer.append("new java.lang.Short(" + name + ")");
            else {
                throw new UnsupportedOperationException("unhandled primitive type.");
            }
        }
        else {
            buffer.append(name);
        }
    }

    /**
     * Start the compilation of a shell fine and register us as a watcher. After
     * this, we just wait for the callback from the compiler.
     */
    private void compileInvocationFile(File shellFile)
    {
        File[] files = {shellFile};
        JobQueue.getJobQueue().addJob(files, new EventqueueCompileObserver(this), pkg.getProject().getClassLoader(), pkg.getProject().getProjectDir(),true);
    }

    // -- CompileObserver interface --

    // not interested in these events:
    public void startCompile(File[] sources)
    {}

    /**
     * An error was detected during compilation of the shell class.
     */
    public void errorMessage(String filename, int lineNo, String message)
    {
        if (dialog != null) {
            dialog.setErrorMessage("Error: " + message);
        }
        watcher.putError(message);
    }
    
    /**
     * A warning was detected during compilation of the shell class.
     * For the shell class, we just ignore warnings.
     */
    public void warningMessage(String filename, int lineNo, String message) 
    {
    }

    /**
     * The compilation of the shell class has ended. If all went well, execute
     * now. Then clean up.
     */
    public synchronized void endCompile(File[] sources, boolean successful)
    {
        if (dialog != null) {
            dialog.setWaitCursor(false);
            if (successful) {
                closeCallDialog();
            }
        }

        pmf.setWaitCursor(false);

        if (successful) {
            startClass();
        }
        else {
            finishCall(false);
        }
    }

    /**
     * Clean up after an invocation or attempted invocation.
     * @param successful  Whether the invocation was successful
     */
    private void finishCall(boolean successful)
    {
        if (constructing && successful) {
            pkg.setStatus(createDone);
        }
        else {
            pkg.setStatus(" ");
        }

        deleteShellFiles();
        
        if (! successful && dialog != null) {
            // Re-enable call dialog: use can try again with
            // different parameters.
            dialog.setEnabled(true);
        }
    }
    
    private void closeCallDialog()
    {
        if (dialog != null) {
            dialog.setWaitCursor(false);
            dialog.setVisible(false);
            // JDK on Windows has a bug. If the PkgMgrFrame is
            // minimised while the call dialog is still showing, the
            // call dialog re-appears when the PkgMgrFrame is restored.
            // Calling dispose() as a workaround.
            // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5005454
            if (Config.isWinOS()) {
                dialog.dispose();
            }
            if (dialog instanceof MethodDialog) {
                ((MethodDialog) dialog).updateParameters();
            }
        }
    }

    /**
     * Remove the shell files that we created for this invocation.
     */
    private void deleteShellFiles()
    {
        File srcFile = new File(pkg.getPath(), shellName + ".java");
        srcFile.delete();

        File classFile = new File(pkg.getPath(), shellName + ".class");
        classFile.delete();
    }

    // -- end of CompileObserver interface --

    /**
     * Execute an interactive method call. At this point, the shell class has
     * been compiled and we are ready to go.
     */
    public void startClass()
    {
        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, commandString);
        final String shellClassName = pkg.getQualifiedName(shellName);
        
        new Thread() {
            public void run() {
                try {
                    final DebuggerResult result = pkg.getProject().getDebugger().runClassMain(shellClassName);
                    
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            // the execution is completed, get the result if there was one
                            // (this could be either a construction or a function result)
                            
                            handleResult(result);
                            
                            // update all open inspect windows
                            pkg.getProject().updateInspectors();
                            
                            finishCall(true);
                        }
                    });
                    
                }
                catch (Throwable e) {
                    e.printStackTrace(System.err);
                }
            }
        }.start();
    }
    
    /**
     * After an execution has finished, check whether there is a result (such as
     * a freshly created object, a function result or an exception) and make
     * sure that it gets processed appropriately.
     * 
     * "exitStatus" and "result" fields should be set with appropriate values before
     * calling this.
     */
    public void handleResult(DebuggerResult result)
    {
        try {
            // first, check whether we had an unexpected exit
            int status = result.getExitStatus();
            switch(status) {
                case Debugger.NORMAL_EXIT :
                    // result will be null here for a void call
                    watcher.putResult(result.getResultObject(), instanceName, ir);
                    ir.setResultObject(result.getResultObject());   
                    executionEvent.setResultObject(result.getResultObject());
                    executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                    break;

                case Debugger.FORCED_EXIT : // exit through System.exit()
                    String excMsg = result.getException().getText();
                    if (instanceName != null) {
                        // always report System.exit for non-void calls
                        pkg.reportExit(excMsg);
                        watcher.putException(excMsg);
                    }
                    else {
                        // for void calls, only report non-zero exits
                        if (!"0".equals(excMsg))
                            pkg.reportExit(excMsg);
                    }
                    executionEvent.setResult(ExecutionEvent.FORCED_EXIT);
                    break;

                case Debugger.EXCEPTION :
                    ExceptionDescription exc = result.getException();
                    String msg = exc.getText();
                    String text = exc.getClassName();
                    if (text != null) {
                        text = JavaNames.stripPrefix(text) + ":\n" + msg;
                        pkg.exceptionMessage(exc.getStack(), text);
                        watcher.putException(text);
                    }
                    else {
                        pkg.reportException(msg);
                        watcher.putException(msg);
                    }
                    executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                    break;

                case Debugger.TERMINATED : // terminated by user
                    watcher.putVMTerminated();
                    executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                    break;

            } // switch
            BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
        }
        catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    static class CleverQualifyTypeNameTransform
        implements NameTransform
    {
        Package mypackage;

        public CleverQualifyTypeNameTransform(Package p)
        {
            mypackage = p;
        }

        public String transform(String n)
        {
            return cleverQualifyTypeName(mypackage, n);
        }
    }

    static private String cleverQualifyTypeName(Package p, String typeName)
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
            int firstDot = pkgName.indexOf(".");

            if (firstDot >= 0)
                pkgName = pkgName.substring(0, firstDot);

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

        int firstDollar = typeName.indexOf('$');

        if (firstDollar != -1) {
            StringBuffer sb = new StringBuffer(typeName);

            // go to length - 1 only so we always have an i+1 character
            // to check. What this means is that if the last character
            // in the typeName is a $, it won't be converted but I don't
            // think a type name with a $ as the last character is valid
            // anyway
            for (int i = firstDollar; i < sb.length() - 1; i++) {
                if (sb.charAt(i) == '$' && !Character.isDigit(sb.charAt(i + 1)))
                    sb.setCharAt(i, '.');
            }

            typeName = sb.toString();
        }

        return JavaNames.typeName(typeName);
    }
}
