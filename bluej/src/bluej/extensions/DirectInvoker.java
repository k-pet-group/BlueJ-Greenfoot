package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.Invoker;
import bluej.debugger.ResultWatcher;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.CallableView;

/**
 * Provides a gateway to invoke methods on objects using a specified
 * set of parameters.
 *
 * @author Clive Miller
 * @version $Id: DirectInvoker.java 1459 2002-10-23 12:13:12Z jckm $
 */
class DirectInvoker
{
    private final Package pkg;
    private final CallableView callable;
    private final String instanceName;
    private String error, resultName;
    
    DirectInvoker (Package pkg, CallableView callable, String instanceName)
    {
        this.pkg = pkg;
        this.callable = callable;
        this.instanceName = instanceName;
    }
    
    /*
     * @param methodName <CODE>null</CODE> implies a constructor.
     */
    DebuggerObject invoke (String[] args)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker (pmf, callable, instanceName, watcher);
        invoker.invokeDirect (null, args);
        DebuggerObject result = watcher.getResult();
        if (result == null) error = watcher.getError();
        else resultName = watcher.getResultName();
        return result;
    }
    
    public String getError()
    {
        return error;
    }
    
    public String getResultName()
    {
        return resultName;
    }
    
    public class DirectResultWatcher implements ResultWatcher
    {
        private boolean resultReady;
        private DebuggerObject result;
        private String errorString;
        private String resultName;
        
        public DirectResultWatcher ()
        {
            resultReady = false;
            result = null;
            errorString = null;
        }
        
        public synchronized DebuggerObject getResult()
        {
            while (!resultReady) {
                try {
                    wait();
                } catch (InterruptedException e) {}
            }
            return result;
        }
            
        public synchronized void putResult(DebuggerObject result, String name) 
        {
            this.result = result;
            this.resultName = name;
            resultReady = true;
            notifyAll();
        }
        
        public synchronized void putError (String error) 
        {
            errorString = "Invocation Error: "+error;
            this.result = null;
            resultReady = true;
            notifyAll();
        }
        
        public String getError()
        {
            return errorString;
        }
        
        public String getResultName()
        {
            return resultName;
        }
    }
}            
