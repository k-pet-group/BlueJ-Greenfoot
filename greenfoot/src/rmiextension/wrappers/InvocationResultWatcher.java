package rmiextension.wrappers;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.testmgr.record.InvokerRecord;

/**
 * A result watcher used by the RObjectImpl class.
 * 
 * @author Davin McCall
 * @version $Id: InvocationResultWatcher.java 3262 2005-01-12 03:30:49Z davmac $
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
}
