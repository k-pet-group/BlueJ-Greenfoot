/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2014,2015,2016,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileObserver;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.compiler.EventqueueCompileObserverAdapter;
import bluej.compiler.FXCompileObserver;
import bluej.compiler.JobQueue;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.NameTransform;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageListener;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.runtime.Shell;
import bluej.testmgr.record.ConstructionInvokerRecord;
import bluej.testmgr.record.ExpressionInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.MethodInvokerRecord;
import bluej.testmgr.record.StatementInvokerRecord;
import bluej.testmgr.record.VoidMethodInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import javafx.application.Platform;
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Debugger class that arranges invocation of constructors or methods. This
 * class constructs a "shell" java source file, compiles it, then loads the
 * resulting class file and executes a method in a new thread.
 * 
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public class Invoker
    implements FXCompileObserver, PackageListener
{
    public static final int OBJ_NAME_LENGTH = 8;
    public static final String SHELLNAME = "__SHELL";
    private static int shellNumber = 0;

    private static final synchronized String getShellName()
    {
        return SHELLNAME + (shellNumber++);
    }

    @OnThread(Tag.FXPlatform)
    private Stage parent;
    private Package pkg; //For data collection purposes
    private boolean codepad; //Used to decide whether to do data collection (don't record if for codepad)
    private File pkgPath;
    private String pkgName;
    private String pkgScopeId;
    private final CallHistory callHistory;
    private ResultWatcher watcher;
    private final CallableView member;
    private String shellName;
    /** Name of the result object */
    @OnThread(Tag.Any)
    private String objName;
    /** The name that the object will have on the object bench.
        Used by data collection. */
    private String benchName;
    @OnThread(Tag.Any)
    private final Map<String,GenTypeParameter> typeMap; // map type parameter names to types
    private ValueCollection localVars;
    private ValueCollection objectBenchVars;
    private final ObjectBenchInterface objectBench;
    private Debugger debugger;
    private String imports; // import statements to include in shell file
    private NameTransform nameTransform;
    private InvokerCompiler compiler;
    private Charset sourceCharset;
    
    /** Name of the target object to which the call is applied */
    private final String instanceName;

    @OnThread(Tag.FXPlatform)
    private CallDialog dialog;
    private boolean constructing;

    private String commandString;
    private InvokerRecord ir;
    
    /** Whether we've already seen an error from the compiler */
    private boolean gotError;

    /**
     * Construct an invoker, specifying most attributes manually.
     */
    public Invoker(Stage frame, Package pkg, CallableView member, ResultWatcher watcher,
            CallHistory callHistory, ValueCollection objectBenchVars, ObjectBenchInterface objectBench,
            Debugger debugger, String instanceName)
    {
        this.pkg = pkg;
        this.parent = frame;
        this.member = member;
        this.watcher = watcher;
        if (member instanceof ConstructorView)
        {
            this.objName = debugger.guessNewName(member.getClassName());
            constructing = true;
        }
        else if (member instanceof MethodView) {
            constructing = false;
        }
        
        this.instanceName = instanceName;
        this.pkgPath = pkg.getPath();
        this.pkgName = pkg.getQualifiedName();
        this.pkgScopeId = pkg.getId();
        this.callHistory = callHistory;
        this.objectBenchVars = objectBenchVars;
        this.objectBench = objectBench;
        this.debugger = debugger;
        this.nameTransform = new NameTransform() {
            @OnThread(Tag.Any)
            public String transform(String typeName)
            {
                return typeName;
            }
        };
        compiler = new InvokerCompiler() {
            public void compile(File[] files, CompileObserver observer)
            {
                Project project = pkg.getProject();
                List<CompileInputFile> wrapped = Utility.mapList(Arrays.asList(files), f -> new CompileInputFile(f, f));
                JobQueue.getJobQueue().addJob(wrapped.toArray(new CompileInputFile[0]), observer, project.getClassLoader(),
                        project.getProjectDir(), true, project.getProjectCharset(), CompileReason.INVOKE, CompileType.INTERNAL_COMPILE);
            }
        };
        this.shellName = getShellName();
        this.sourceCharset = pkg.getProject().getProjectCharset();
        this.typeMap = null;
    }

    /**
     * Create an invoker for a free form statement or expression. After using this
     * constructor, optionally call setImports(), then call doFreeFormInvocation()
     * to perform compilation and execution.
     */
    public Invoker(PkgMgrFrame pmf, ValueCollection localVars, String command, ResultWatcher watcher)
    {
        this(pmf, (MethodView)null, (String)null, null);
        
        this.watcher = watcher;
        this.shellName = getShellName();
        this.objName = null;
        this.localVars = localVars;

        constructing = false;
        codepad = true;
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
        this(pmf.getWindow(), pmf.getPackage(), member, watcher, pmf.getPackage().getCallHistory(), pmf.getObjectBench(),
                pmf.getObjectBench(), pmf.getProject().getDebugger(), null);

        codepad = false;

        // in the case of a constructor, we need to construct an object name
        if (member instanceof ConstructorView) {
            this.objName = pmf.getProject().getDebugger().guessNewName(member.getClassName());
            benchName = objName;
            constructing = true;
        }
        else if (member instanceof MethodView) {
            constructing = false;
        }
        else {
            Debug.reportError("illegal member type in invocation");
            throw new IllegalArgumentException("Unknown callable type");
        }
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
    public Invoker(PkgMgrFrame pmf, MethodView member, String objName, DebuggerObject debuggerObject, ResultWatcher watcher)
    {
        // We want a map of all the type parameters that may appear in the
        // method signature to the corresponding instantiation types from the
        // object to which the method is being applied.
        //
        // Tpar names in the method signature however correspond to names from
        // the class in which the method was declared. So we need to map tpars
        // from the object's class to that class.
        this(pmf, member, objName, debuggerObject.getGenType().mapToSuper(member.getClassName()).getMap());
        
        this.watcher = watcher;
        this.shellName = getShellName();
        codepad = false;

        constructing = false;
    }

    /**
     * Initialize most of the invoker's necessary fields via a PkgMgrFrame reference.
     */
    private Invoker(final PkgMgrFrame pmf, CallableView member, String instanceName, Map<String, GenTypeParameter> typeMap)
    {
        this.member = member;
        this.instanceName = instanceName;
        this.typeMap = typeMap;
        this.parent = pmf.getWindow();
        this.pkg = pmf.getPackage();
        final Package pkg = pmf.getPackage();
        this.pkgPath = pkg.getPath();
        this.pkgName = pkg.getQualifiedName();
        this.pkgScopeId = pkg.getId();
        this.callHistory = pkg.getCallHistory();
        this.objectBenchVars = pmf.getObjectBench();
        this.objectBench = pmf.getObjectBench();
        this.debugger = pkg.getProject().getDebugger();
        this.nameTransform = new CleverQualifyTypeNameTransform(pkg);
        compiler = new InvokerCompiler() {
            public void compile(File[] files, CompileObserver observer)
            {
                Project project = pkg.getProject();
                List<CompileInputFile> wrapped = Utility.mapList(Arrays.asList(files), f -> new CompileInputFile(f, f));
                JobQueue.getJobQueue().addJob(wrapped.toArray(new CompileInputFile[0]), observer, project.getClassLoader(),
                        project.getProjectDir(), true, project.getProjectCharset(), CompileReason.INVOKE, CompileType.INTERNAL_COMPILE);
            }
        };
        this.sourceCharset = pmf.getProject().getProjectCharset();
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
        gotError = false;
        // Here are the different cases for when we do/don't show an invoker dialog:
        // - If there's parameters needed, we must always show the dialog
        // - If there's no parameters and a void return, we never show a dialog as there's nothing to ask.
        // - If there's no parameters to a constructor, we still show the dialog in BlueJ (we need to ask for an object name for the object bench) but not in Greenfoot
        // - If there's no parameters and a non-void return, we show the dialog if we're in BlueJ in testing mode because they may want to add an assertion for the result
        if ((!constructing || Config.isGreenfoot()) && !member.hasParameters() && (!inTestMode() || isVoidReturn()))
        {
            doInvocation(null, (JavaType[]) null, null);
        }
        else {

            CallDialog cDialog;
            if (member instanceof MethodView)
            {
                // Method requires a method dialog
                MethodView mmember = (MethodView)member;
                MethodDialog mDialog = new MethodDialog(parent, objectBench, callHistory, instanceName, mmember, typeMap, this);
                cDialog = mDialog;
            }
            else
            {
                // Constructor
                ConstructorView cmember = (ConstructorView)member;
                ConstructorDialog conDialog = new ConstructorDialog(parent, objectBench, callHistory, objName, cmember, this);
                cDialog = conDialog;
            }

            cDialog.show();
            //org.scenicview.ScenicView.show(cDialog.getDialogPane());

            dialog = cDialog;
            if (pkg != null)
            {
                pkg.addListener(this);
            }
        }
    }

    /**
     * The call dialog had OK clicked.
     */
    @OnThread(Tag.FXPlatform)
    public void callDialogOK()
    {
        dialog.setOKEnabled(false);
        String[] actualTypeParams = dialog.getTypeParams();
        String newInstanceName = dialog.getNewInstanceName();
        String[] args = dialog.getArgs();
        JavaType[] argGenTypes = dialog.getArgGenTypes(true);
        gotError = false;
        objName = newInstanceName;
        benchName = objName;
        doInvocation(args, argGenTypes, actualTypeParams);
    }

    // -- end of CallDialogWatcher interface --

    /**
     * Invokes a constructor or method with the given parameters.
     * 
     * @param params The arguments to the method/constructor (Java expressions)
     */
    public void invokeDirect(String[] params)
    {
        gotError = false;
        final JavaType[] argTypes = member.getParamTypes(false);
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = argTypes[i].mapTparsToTypes(typeMap).getUpperBound();
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
        gotError = false;
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
                argTypeStrings[i] = argType.toString(nameTransform);
            }
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
            constype = nameTransform.transform(className);
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
            ir = new ConstructionInvokerRecord(constype, objName, command + actualArgString, args);
        }
        else { // it's a method call
            MethodView method = (MethodView) member;
            isVoid = method.isVoid();

            if (method.isStatic())
                command = nameTransform.transform(className) + "." + method.getName();
            else {
                command = instanceName + "." + method.getName();
            }

            if (isVoid) {
                ir = new VoidMethodInvokerRecord(command + actualArgString, args);
                objName = null;
            }
            else {
                ir = new MethodInvokerRecord(method.getGenericReturnType(), command + actualArgString, args);
                objName = "__bluej__result__";
            }
        }

        if (constructing && member.getParameterCount() == 0 && (typeParams == null || typeParams.length == 0)) {
            // Special case for construction of a class using the default constructor.
            // We can do this without writing and compiling a shell file.
            
            commandString = command + actualArgString;
            watcher.beginCompile(); // there is no compile step, really
            watcher.beginExecution(ir);
            
            // We must however do so in a seperate thread. Otherwise a constructor which
            // goes into an infinite loop can hang BlueJ.
            new Thread() {
                @OnThread(Tag.Worker)
                public void run() {
                    Platform.runLater(Invoker.this::closeCallDialog);
                    
                    DebuggerResult result = debugger.instantiateClass(className);

                    Platform.runLater(() -> {
                        // the execution is completed, get the result if there was one
                        // (this could be either a construction or a function result)

                        handleResult(result, false); // handles error situations
                    });
                }
            }.start();
        }
        else {
            if (isVoid)
                argString += ';';
            
            watcher.beginCompile();
            File shell = writeInvocationFile(paramInit, command + argString, isVoid, constype);
            if (shell != null) {
                commandString = command + actualArgString;
                compileInvocationFile(shell);
            }
            else {
                endCompile(new CompileInputFile[0], false, CompileType.INTERNAL_COMPILE, -1);
            }
        }
    }

    /**
     * Set the assertion statement to the associated invocation record.
     */
    public void setAssertionStatement(String statement)
    {
        ir.addAssertion(statement);
    }

    /**
     * Get the Unique ID of the associated invocation record.
     */
    public int getUniqueIRIdentifier()
    {
        return ir.getUniqueIdentifier();
    }

    /**
     * Get the associated package.
     */
    public Package getPackage()
    {
        return pkg;
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
     * <p>Invocation here means: construct shell class and compile. The execution
     * is done once we return from compilation (in method "endCompile").
     * Compilation is done asynchronously by the CompilerThread.
     * 
     * <p>This method is still executed in the interface thread, while "endCompile"
     * will be executed by the CompilerThread.
     * 
     * @param resultType   the type of the result expressed in Java (eg "int",
     *                     "java.util.ArrayList<String>"). An empty string means
     *                     the type is not known. A null value indicates that there
     *                     is no result (the invocation is a statement).
     * 
     * @return true if successful, or false if there was a problem (the shell
     * file couldn't be written). In case of failure, a dialog is displayed to
     * alert the user.
     */
    public boolean doFreeFormInvocation(String resultType)
    {
        gotError = false;
        boolean hasResult = resultType != null;
        if (hasResult) {
            if (resultType.equals(""))
                resultType = null;
            objName = "__bluej__result__";
            ir = new ExpressionInvokerRecord(commandString);
        }
        else {
            objName = null;
            // this is a statement, treat as a void method result
            ir = new StatementInvokerRecord(commandString);
        }

        File shell = writeInvocationFile("", commandString, !hasResult, resultType);
        if (shell != null) {
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
     * <p>A shell file has, very roughly, the following form:
     * 
     * <p><pre>
     * $PKGLINE
     * $IMPORTS
     * public class $CLASSNAME extends bluej.runtime.Shell
     * {
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
     * SCOPEINIT declares a Map, __bluej_runtime_scope, which maps object
     * names to their values (allowing objects from the object bench to be
     * accessed).
     * 
     *  
     * @param paramInit  java code which initializes parameter variables
     * @param callString java code which executes requested method/code
     * @param isVoid   true if no result is returned. The callString parameter
     *                 should contain a complete statement (including terminating
     *                 semicolon).
     * @param constype  the exact type of the object being constructed. Only
     *                  needed if 'constructing' is true, but can be supplied in other
     *                  cases to yield a more accurate result type (when generic types
     *                  are involved).
     */
    private File writeInvocationFile(String paramInit, String callString,
            boolean isVoid, String constype)
    {
        // Create package specification line ("package xyz")
        String packageLine;
        if (pkgName.length() == 0) {
            packageLine = "";
        }
        else {
            packageLine = "package " + pkgName + ";";
        }

        StringBuffer buffer = new StringBuffer();
        
        // Build scope, i.e. add one line for every object on the object
        // bench that gets the object and makes it available for use as
        // a parameter.
        
        // A sample of the code generated
        //  java.util.Map __bluej_runtime_scope = getScope("BJIDC:\\aproject");
        //  JavaType instnameA = (JavaType) __bluej_runtime_scope.get("instnameA");
        //  OtherJavaType instnameB = (OtherJavaType) __bluej_runtime_scope.get("instnameB");

        String scopeId = Utility.quoteString(pkgScopeId);
        Iterator<? extends NamedValue> wrappers = objectBenchVars.getValueIterator();

        Map<String, String> objBenchVarsMap = new HashMap<String, String>();
        
        if (wrappers.hasNext() || localVars != null) {
            buffer.append("final bluej.runtime.BJMap __bluej_runtime_scope = getScope(\"" + scopeId + "\");" + Config.nl);
            while (wrappers.hasNext()) {
                NamedValue objBenchVar = wrappers.next();
                objBenchVarsMap.put(objBenchVar.getName(), getVarDeclString("", false, objBenchVar, nameTransform));
            }
        }
        
        // put the local variables here if we don't know the result type. If we do know
        // the result type, we put the local variables inside the result wrapper object
        // later on.
        if (localVars != null && constype == null) {
            Iterator<? extends NamedValue> i = localVars.getValueIterator();
            while (i.hasNext()) {
                NamedValue localVar = i.next();
                objBenchVarsMap.put(localVar.getName(), getVarDeclString("lv:", false, localVar, nameTransform));
            }
        }
        
        Iterator<String> obVarsIterator = objBenchVarsMap.values().iterator();
        while (obVarsIterator.hasNext()) {
            buffer.append(obVarsIterator.next().toString());
        }
        
        String vardecl = buffer.toString();
        buffer.setLength(0);

        // build the invocation string
        
        // A sample of the code generated:
        //
        // Result type not known:
        //    try {
        //      return makeObj(2+new String("ap").length());
        //    }
        //    finally {
        //    }
        //
        // Result type known:
        //    return new java.lang.Object() {
        //       int result;
        //       try {
        //           result = 2+new String("ap").length();
        //       }
        //       finally {
        //       }
        //    }
        //
        // Note that codepad local variable values, if any, are saved in the finally block.

        if (!isVoid) {
            if (constype == null) {
                buffer.append(paramInit);
                buffer.append("try {" + Config.nl);
                buffer.append("return makeObj(");
            }
            else {
                buffer.append("return new java.lang.Object() { ");
                buffer.append(constype + " __bluej__result__;" + Config.nl);
                buffer.append("{ ");
                buffer.append(paramInit);
                if (localVars != null) {
                    writeVariables("lv:", buffer, false, localVars.getValueIterator(), nameTransform);
                }
                buffer.append("try {" + Config.nl);
                buffer.append("__bluej__result__=(");
            }
            buffer.append(callString);
            // Append a new line, as the call string may end with a //-style comment
            buffer.append(Config.nl);
            buffer.append(");}");
            buffer.append(Config.nl);
            buffer.append("finally {" + Config.nl);
        }
        else {
            buffer.append(paramInit);
            buffer.append(callString);
            // Append a new line, as the call string may end with a //-style comment
            buffer.append(Config.nl);
        }

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

        File shellFile = new File(pkgPath, shellName + ".java");
        BufferedWriter shell = null;
        try {
            FileOutputStream fos = new FileOutputStream(shellFile);
            shell = new BufferedWriter(new OutputStreamWriter(fos, sourceCharset));

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
            shell.write("public static ");
            if (isVoid) {
                shell.write("void");
            }
            else {
                shell.write("java.lang.Object");
            }
            shell.write(" run() throws Throwable {");
            shell.newLine();
            shell.write(vardecl);
            shell.newLine();
            shell.write(invocation);
            shell.write(scopeSave);
            if (! isVoid) {
                shell.write("}"); // end finally block
                if (constype != null) {
                    shell.write("} };"); // end block, anonymous inner object
                }
            }
            shell.newLine();
            shell.write("}}"); // end method, class
            shell.newLine();
            shell.close();
        }
        catch (IOException e) {
            DialogManager.showErrorFX(parent, "could-not-write-shell-file");
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
                
                if (wrapper.isFinal()) {
                    buffer.append("final ");
                }
                if (isStatic) {
                    buffer.append("static ");
                }
                
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
            
            if (wrapper.isFinal()) {
                buffer.append("final ");
            }
            if (isStatic) {
                buffer.append("static ");
            }
            
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
        compiler.compile(files, new EventqueueCompileObserverAdapter(this));
    }

    // -- CompileObserver interface --

    // not interested in these events:
    @Override
    public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence) { }

    /*
     * @see bluej.compiler.CompileObserver#compilerMessage(bluej.compiler.Diagnostic)
     */
    @Override
    public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
    {
        if (diagnostic.getType() == Diagnostic.ERROR) {
            if (! gotError) {
                gotError = true;
                errorMessage(diagnostic.getFileName(), diagnostic.getStartLine(), diagnostic.getMessage());
                return true;
            }
        }
        // We ignore warnings for shell classes
        return false;
    }
    
    /**
     * An error was detected during compilation of the shell class.
     */
    private void errorMessage(String filename, long lineNo, String message)
    {
        DataCollector.invokeCompileError(pkg, commandString, message);
        
        if (dialog != null)
        {
            dialog.setErrorMessage("Error: " + message);
        }
        watcher.putError(message, ir);
    }
    
    /**
     * The compilation of the shell class has ended. If all went well, execute
     * now. Then clean up.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public synchronized void endCompile(CompileInputFile[] sources, boolean successful, CompileType type, int compilationSequence)
    {
        if (dialog != null)
        {
            dialog.setWaitCursor(false);
            if (successful)
            {
                closeCallDialog();
            }
        }

        if (successful) {
            watcher.beginExecution(ir);
            startClass();
        }
        else {
            finishCall(false);
        }
    }

    /**
     * Clean up after an invocation or attempted invocation.
     * @param successful  Whether the invocation compilation was successful
     */
    private void finishCall(boolean successful)
    {
        deleteShellFiles();

        if (!successful && dialog != null)
        {
            // Re-enable call dialog: use can try again with
            // different parameters.
            dialog.setOKEnabled(true);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void closeCallDialog()
    {
        if (dialog != null) {
            dialog.setWaitCursor(false);
            dialog.close();
            dialog.saveCallHistory();
            dialog = null;
        }
        if (pkg != null)
        {
            pkg.addListener(this);
        }
    }

    /**
     * Remove the shell files that we created for this invocation.
     */
    private void deleteShellFiles()
    {
        File srcFile = new File(pkgPath, shellName + ".java");
        srcFile.delete();

        File classFile = new File(pkgPath, shellName + ".class");
        classFile.delete();
        
        // Remove any inner class files
        String [] innerClassFiles = pkgPath.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name)
            {
                return (name.startsWith(shellName + "$"));
            }
        });
        
        for (String innerClassFile : innerClassFiles) {
            new File(pkgPath, innerClassFile).delete();
        }
    }

    // -- end of CompileObserver interface --

    /**
     * Execute an interactive method call. At this point, the shell class has
     * been compiled and we are ready to go.
     */
    private void startClass()
    {
        final String shellClassName = JavaNames.combineNames(pkgName, shellName);
        
        new Thread() {
            public void run() {
                try {
                    DebuggerResult result = debugger.runClassMain(shellClassName);
                    
                    Platform.runLater(new Runnable() {
                        public void run() {
                            // the execution is completed, get the result if there was one
                            // (this could be either a construction or a function result)
                            
                            handleResult(result, constructing);
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
     * <p>"exitStatus" and "__bluej__result__" fields should be set with appropriate values before
     * calling this.
     */
    @OnThread(Tag.FXPlatform)
    public void handleResult(DebuggerResult result, boolean unwrap)
    {
        try {
            // first, check whether we had an unexpected exit
            int status = result.getExitStatus();
            switch(status) {
                case Debugger.NORMAL_EXIT :
                    // resultObj will be the null object representation (isNullObject() == true) for a void call
                    DebuggerObject resultObj = result.getResultObject();
                    if (unwrap) {
                        // For constructor calls, the result is expected to be the created object.
                        resultObj = resultObj.getInstanceField(0).getValueObject(null);
                    }
                    if (!codepad)
                    {
                        String resultType;
                        //Only record this if it wasn't on behalf of the codepad (codepad records separately):
                        if (resultObj.getClassName().startsWith(Shell.class.getCanonicalName()))
                        {
                            //Wrapped by Shell class, grab from first field.
                            if (resultObj.getInstanceField(0).getType().isPrimitive())
                            {
                                // If it's a field with primitive type, use type of field:
                                resultType = resultObj.getInstanceField(0).getType().toString();  
                            }
                            else
                            {
                                // Take type from resulting object:
                                resultType = resultObj.getInstanceField(0).getValueObject(null).getClassName();
                            }
                        }
                        else
                        {
                            resultType = resultObj.getClassName();
                            if (resultType.equals(""))
                            {
                                resultType = "void";
                            }
                        }
                        
                        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
                        
                        DataCollector.invokeMethodSuccess(pkg, commandString, benchName, resultType, pmf == null ? -1 : pmf.getTestIdentifier(), ir.getUniqueIdentifier());
                    }
                    
                    ir.setResultObject(resultObj);
                    watcher.putResult(resultObj, objName, ir);
                    break;

                case Debugger.EXCEPTION :
                    ExceptionDescription exc = result.getException();
                    if (!codepad)
                    {
                        //Only record this if it wasn't on behalf of the codepad (codepad records separately):
                        DataCollector.invokeMethodException(pkg, commandString, exc);
                    }
                    watcher.putException(exc, ir);
                    break;

                case Debugger.TERMINATED_BY_USER_SYSTEM_EXIT : // terminated by user
                case Debugger.TERMINATED_BY_BLUEJ:
                    if (!codepad)
                    {
                        //Only record this if it wasn't on behalf of the codepad (codepad records separately):
                        DataCollector.invokeMethodTerminated(pkg, commandString);
                    }
                    watcher.putVMTerminated(ir, status == Debugger.TERMINATED_BY_USER_SYSTEM_EXIT);
                    break;

            } // switch
        }
        catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void graphClosed()
    {
        closeCallDialog();
    }
    
    @Override
    public void graphChanged()
    {
        // Nothing needs doing.
    }

    public boolean isVoidReturn()
    {
        return member.isVoid();
    }

    static class CleverQualifyTypeNameTransform
        implements NameTransform
    {
        Package mypackage;

        public CleverQualifyTypeNameTransform(Package p)
        {
            mypackage = p;
        }

        @OnThread(Tag.Any)
        public String transform(String n)
        {
            return cleverQualifyTypeName(mypackage, n);
        }
    }

    @OnThread(Tag.Any)
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
            String pName = p.getQualifiedName();
            int firstDot = pName.indexOf(".");

            if (firstDot >= 0)
                pName = pName.substring(0, firstDot);

            // if the first part of the package name exists as a target
            // lets unqualify the typeName
            if (p.getTarget(pName) != null)
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

    public boolean inTestMode()
    {
        return pkg.getProject().inTestMode();
    }
}
