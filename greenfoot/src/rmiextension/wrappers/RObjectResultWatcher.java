package rmiextension.wrappers;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.ResultWatcher;
import bluej.testmgr.record.InvokerRecord;

/**
 * A result watcher used by the RObjectImpl class.
 * 
 * @author Davin McCall
 * @version $Id: RObjectResultWatcher.java 3227 2004-12-08 04:04:58Z davmac $
 */
class RObjectResultWatcher implements ResultWatcher
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
