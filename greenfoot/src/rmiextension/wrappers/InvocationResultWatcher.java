package rmiextension.wrappers;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.testmgr.record.InvokerRecord;

/**
 * A result watcher used by the RObjectImpl class.
 * 
 * @author Davin McCall
 * @version $Id: InvocationResultWatcher.java 4274 2006-05-15 21:33:51Z polle $
 */
class InvocationResultWatcher implements ResultWatcher
{
    public String errorMsg = null;
    public DebuggerObject resultObj = null;
    
    public void putError(String error)
    {
        errorMsg = error;
        synchronized (this) {
            notify();
        }
    }
    
    public void putResult(DebuggerObject dObj, String name, InvokerRecord ir)
    {
        resultObj = dObj;
        synchronized (this) {
            notify();
        }
    }
    
    public ExpressionInformation getExpressionInformation()
    {
        return null;
    }

    public void putException(String message)
    {
        errorMsg = message;
        synchronized (this) {
            notify();
        }
    }
    
    public void putVMTerminated()
    {
        synchronized (this) {
            notify();
        }
    }
}
