/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017,2018  Michael Kolling and John Rosenberg 
 
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
import bluej.views.MethodView;
import javafx.stage.Stage;

/**
 * A result watcher which handles standard invocation of
 * methods on a DebuggerObject.
 */
public abstract class ObjectResultWatcher implements ResultWatcher
{
    private final MethodView method;
    private DebuggerObject obj;
    private String objInstanceName;
    private Package pkg;
    private Stage parentWindow;

    public ObjectResultWatcher(DebuggerObject obj, String objInstanceName, Package pkg, Stage parentWindow, MethodView method)
    {
        this.method = method;
        this.obj = obj;
        this.objInstanceName = objInstanceName;
        this.pkg = pkg;
        this.parentWindow = parentWindow;
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
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), objInstanceName);
        executionEvent.setMethodName(method.getName());
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
        ExpressionInformation expressionInformation = new ExpressionInformation(method, objInstanceName, obj.getGenType());
        expressionInformation.setArgumentValues(ir.getArgumentValues());
        pkg.getProject().getResultInspectorInstance(result, name, pkg,
                ir, expressionInformation, parentWindow);
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
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), objInstanceName);
        executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
        executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
        executionEvent.setException(exception);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

        pkg.getProject().updateInspectors();
        pkg.exceptionMessage(exception);
    }

    @Override
    public void putVMTerminated(InvokerRecord ir)
    {
        ExecutionEvent executionEvent = new ExecutionEvent(pkg, obj.getClassName(), objInstanceName);
        executionEvent.setParameters(method.getParamTypes(false), ir.getArgumentValues());
        executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
        BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
    }
}
