package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.Invoker;
import bluej.debugger.ResultWatcher;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.testmgr.*;

/**
 * Provides a gateway to invoke methods on objects using a specified set of parameters.
 *
 * @author Clive Miller, Damiano Bolla
 * @version $Id: DirectInvoker.java 1655 2003-03-06 09:39:14Z damiano $
 */
class DirectInvoker
{
    private final Package pkg;
    private final CallableView callable;
    private final String instanceName;
    private String error, resultName;

    /**
     * For use by the beluj.extensions
     * The instanceName IS used when an object is created.
     */
    DirectInvoker (Package pkg, CallableView callable, String instanceName)
    {
        this.pkg = pkg;
        this.callable = callable;
        this.instanceName = instanceName;
    }
    
    /**
     * Call this if you want to call a constructor
     */
    DebuggerObject invokeConstructor (String[] args)
        {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
        if ( pmf == null ) return null;
        
        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker (pmf, callable, null, watcher);
        invoker.invokeDirect (instanceName, args);

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        if (result == null)
            {
            error = watcher.getError();
            return null;
            }

        // constructors place the result as the first field on the returned object
        result = result.getInstanceFieldObject(0);
        resultName = watcher.getResultName();
        return result;
        }
    
    /**
     * This if you want a method
     * You need to pass the object where you want it applied.
     */
    DebuggerObject invokeMethod (String onThisObjectInstance, String[] args)
        {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
        if ( pmf == null ) return null;
        
        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker (pmf, callable, onThisObjectInstance, watcher);
        invoker.invokeDirect (instanceName, args);

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        if (result == null)
            {
            error = watcher.getError();
            return null;
            }

        resultName = watcher.getResultName();
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
    

    /**
     * This is used to interface with the core BlueJ
     */
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
            
        public synchronized void putResult(DebuggerObject result, String name, InvokerRecord ir) 
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
