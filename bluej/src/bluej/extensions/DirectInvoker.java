package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.debugger.Invoker;
import bluej.debugger.ResultWatcher;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.testmgr.*;
import bluej.utility.*;

/**
 * Provides a gateway to invoke methods on objects using a specified set of parameters.
 *
 * @author Clive Miller, Damiano Bolla
 * @version $Id: DirectInvoker.java 1667 2003-03-08 12:16:42Z damiano $
 */
class DirectInvoker
{
    private final Package pkg;
    private final CallableView callable;
    private String error, resultName;

    /**
     * For use by the bluej.extensions
     */
    DirectInvoker (Package pkg, CallableView callable )
    {
        this.pkg = pkg;
        this.callable = callable;
    }


    /**
     * As from reflection standard the user will give me Objects to be given to the
     * constructors or methods. BUT bluej wants strings... so I am here converting
     * the objects into strings...
     */
    private String[] convObjToString ( Object[] i_array )
      {
      if ( i_array == null ) return null;
      if ( i_array.length <= 0 ) return new String[0];

      String [] o_array = new String [i_array.length];
      for ( int index=0; index<i_array.length; index ++ )
        {
//        Debug.message("convert="+convOneObj ( i_array[index] ));
        o_array[index] = convOneObj ( i_array[index] );
        }

      return o_array;
      }

    /**
     * Does one conversion. WARNING
     * WERIFY with MIchael or somebody else that this IS indeed correct !!!
     */
    String convOneObj ( Object i_obj )
      {
      // A string should be quoted by a couple of "".
      if ( i_obj instanceof String )     return "\""+i_obj+"\"";
      // An object reference is just the object instance name
      if ( i_obj instanceof BObject )    return ((BObject)i_obj).getInstanceName();
      // All the rest should be done by standard conversion...
      return i_obj.toString();
      }
    
    /**
     * Call this if you want to call a constructor
     */
    DebuggerObject invokeConstructor (Object[] args)
        {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
        if ( pmf == null ) return null;
        
        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker (pmf, callable, null, watcher);
        // Setting the instanceName here works but it is simpler to do it when we
        // put it into the bench, maybe it should be removed from the params... Damiano
        invoker.invokeDirect (null, convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();
        // Let me get back a possible error
        error = watcher.getError();

        // No result... possible
        if (result == null) 
          {
          Debug.message("DirectInvoker.invokeConstructor: ERROR="+error);
          return null;
          }

        // Result is ALWAYS an Object and it is the first field on the returned object
        result = result.getInstanceFieldObject(0);
        resultName = watcher.getResultName();
        return result;
        }
    
    /**
     * This if you want a method
     * You need to pass the object where you want it applied.
     */
    DebuggerObject invokeMethod (String onThisObjectInstance, Object[] args)
        {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(pkg);
        if ( pmf == null ) return null;
        
        DirectResultWatcher watcher = new DirectResultWatcher();
        Invoker invoker = new Invoker (pmf, callable, onThisObjectInstance, watcher);
        // Setting the instanceName here does NOT work at all.
        invoker.invokeDirect (null, convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        // Let me get back a possible error
        error = watcher.getError();

        // No result... possible
        if (result == null) 
          {
          Debug.message("DirectInvoker.invokeMethod: ERROR="+error);
          return null;
          }

        // The "real" object is the first Field in this object.. BUT it is not always
        // an Object, it may be a primitive one...
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
