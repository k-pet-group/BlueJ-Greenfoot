package bluej.debugmgr.objectbench;

import bluej.BlueJEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.views.MethodView;

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
    private PkgMgrFrame pmf;

    public ObjectResultWatcher(DebuggerObject obj, String objInstanceName, Package pkg, PkgMgrFrame pmf, MethodView method)
    {
        this.method = method;
        this.obj = obj;
        this.objInstanceName = objInstanceName;
        this.pkg = pkg;
        this.pmf = pmf;
    }

    @Override
    public void beginCompile()
    {
        pmf.setWaitCursor(true);
    }

    @Override
    public void beginExecution(InvokerRecord ir)
    {
        BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
        pmf.setWaitCursor(false);
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
                ir, expressionInformation, pmf.getFXWindow());
    }

    /**
     * Called to record the result of an interaction, e.g. for unit
     * testing recording or Greenfoot's save-the-world.
     */
    protected abstract void addInteraction(InvokerRecord ir);

    @Override
    public void putError(String msg, InvokerRecord ir)
    {
        pmf.setWaitCursor(false);
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
