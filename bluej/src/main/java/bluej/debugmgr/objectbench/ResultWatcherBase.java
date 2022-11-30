/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.debugmgr.objectbench;

import bluej.BlueJEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.CallableView;
import bluej.views.MethodView;
import javafx.stage.Stage;

/**
 * A result watcher which handles standard invocation of methods and constructors.
 */
public abstract class ResultWatcherBase implements ResultWatcher
{
    private final CallableView method;
    private DebuggerObject obj;  // can be null for static method calls
    private String objInstanceName;  // can be null
    private Package pkg;
    private Stage parentWindow;
    private String className;

    /**
     * Construct a new ResultWatcherBase, for a constructor or static method call.
     * @param pkg            The package in which the call is executed
     * @param parentWindow   The parent window for display
     * @param method         The method/constructor being called
     */
    public ResultWatcherBase(Package pkg, Stage parentWindow, CallableView method)
    {
        this.method = method;
        this.className = method.getClassName();
        this.pkg = pkg;
        this.parentWindow = parentWindow;
    }
    
    /**
     * Construct a new ResultWatcherBase, for an instance method call.
     * @param obj              The receiver of the call
     * @param objInstanceName  The name of the receiver instance (as on the object bench)
     * @param pkg              The package in which the call is executed
     * @param parentWindow     The parent window for display
     * @param method           The method being called
     */
    public ResultWatcherBase(DebuggerObject obj, String objInstanceName, Package pkg, Stage parentWindow, CallableView method)
    {
        this.method = method;
        this.obj = obj;
        this.objInstanceName = objInstanceName;
        this.pkg = pkg;
        this.parentWindow = parentWindow;
        this.className = obj.getClassName();
    }

    @Override
    public void beginCompile()
    {
    }

    @Override
    public void beginExecution(InvokerRecord ir)
    {
    }

    @Override
    public void putResult(DebuggerObject result, String name, InvokerRecord ir)
    {
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, className, objInstanceName);
        if (method instanceof MethodView) {
            MethodView mv = (MethodView) method;
            executionEvent.setMethodName(mv.getName());
        }
        executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
        executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
        executionEvent.setResultObject(result);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

        pkg.getProject().updateInspectors();

        addInteraction(ir);

        // a void result returns a name of null
        if (result != null && ! result.isNullObject()) {
            nonNullResult(result, name, ir);
        }
    }

    /**
     * Handle a non-null object result.  By default, the result is inspected.
     * @param result The result object
     * @param name The suggested name for use in the inspector
     * @param ir Details of the invocation.
     */
    protected void nonNullResult(DebuggerObject result, String name, InvokerRecord ir)
    {
        if (method instanceof MethodView) {
            MethodView mv = (MethodView) method;
            ExpressionInformation expressionInformation;
            if (obj != null)
            {
                expressionInformation = new ExpressionInformation(mv,
                        objInstanceName, obj.getGenType());
            }
            else
            {
                expressionInformation = new ExpressionInformation(mv, objInstanceName);
            }
            expressionInformation.setArgumentValues(ir.getArgumentValues());
            pkg.getProject().getResultInspectorInstance(result, name, pkg,
                    ir, expressionInformation, parentWindow);
        }
    }

    /**
     * Called to record the result of an interaction, e.g. for unit
     * testing recording or Greenfoot's save-the-world.
     */
    protected abstract void addInteraction(InvokerRecord ir);

    @Override
    public void putError(String msg, InvokerRecord ir)
    {
    }

    @Override
    public void putException(ExceptionDescription exception, InvokerRecord ir)
    {
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, className, objInstanceName);
        executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
        executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
        executionEvent.setException(exception);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

        pkg.getProject().updateInspectors();
        pkg.exceptionMessage(exception);
    }

    @Override
    public void putVMTerminated(InvokerRecord ir, boolean terminatedByUserCode)
    {
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, className, objInstanceName);
        executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
        executionEvent.setResult(terminatedByUserCode ? ExecutionEvent.NORMAL_EXIT : ExecutionEvent.TERMINATED_EXIT);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
    }
}
