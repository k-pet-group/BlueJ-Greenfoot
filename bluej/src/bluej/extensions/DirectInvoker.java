package bluej.extensions;

import bluej.debugger.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.testmgr.*;
import bluej.views.*;

/**
 * Provides a gateway to invoke methods on objects using a specified set of parameters.
 *
 * @author Clive Miller, Damiano Bolla
 * @version $Id: DirectInvoker.java 1949 2003-05-13 14:37:36Z damiano $
 */
class DirectInvoker
{
    private final Package pkg;
    private final CallableView callable;
    private String errorMsg=null;
    private String resultName;

    /**
     * For use by the bluej.extensions
     */
    DirectInvoker (Package i_pkg, CallableView i_callable )
    {
        pkg = i_pkg;
        callable = i_callable;
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
        invoker.invokeDirect ( convObjToString(args) );

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        if (watcher.isFailed()) 
          {
          // If the invoke did fail miserably this is what I can do now...
          errorMsg = "invokeConstructor: Error="+watcher.getError();
          return null;
          }

        if ( result == null )
          {
          // This is most likely an error, but not of the sort above. Unlikely to happen 
          errorMsg = "DirectInvoker.invokeConstructor: ERROR: result==null";
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
        invoker.invokeDirect ( convObjToString(args));

        // this will wait() on the invoke to finish
        DebuggerObject result = watcher.getResult();

        if (watcher.isFailed()) 
          {
          // If the invoke did fail miserably this is what I can do now...
          errorMsg = "invokeMethod: Error="+watcher.getError();
          return null;
          }

        if (result == null) 
          {
          // This is most likely an error, but not of the sort above. Unlikely to happen 
          errorMsg = "DirectInvoker.invokeMethod: ERROR: result==null";
          return null;
          }

        // The "real" object is the first Field in this object.. BUT it is not always
        // an Object, it may be a primitive one...
        resultName = watcher.getResultName();
        return result;
        }


    /**
     * If the returned error message is != null then there has been a serious error
     */
    public String getError()
      {
      return errorMsg;
      }
    
    public String getResultName()
      {
      return resultName;
      }
    
// ====================== UTILITY CLASS aligned left =========================

/**
 * This is used to interface with the core BlueJ
 * This new version does return when there is an INTERRUPT
 */
class DirectResultWatcher implements ResultWatcher
  {
  private boolean resultReady;
  private boolean isFailed;
  
  private DebuggerObject result;
  private String errorMsg;         // When there is a fail this is the reason.
  private String resultName;
        
  public DirectResultWatcher ()
      {
      resultReady = false;
      isFailed    = false;
      result      = null;
      errorMsg    = null;
      }
        
  /**
   * This will try to get the result of an invocation.
   * null can be returned if the thread is interrupted !!!
   */
  public synchronized DebuggerObject getResult()
    {
    while (!resultReady) 
      {
      try 
        {
        wait();
        } 
      catch (InterruptedException exc) 
        {
        // This is correct, if someone wants to get me out of this I should
        // obey to the oreder !
        isFailed=true;
        errorMsg="getResult: Interrupt: Exception="+exc.getMessage();
        return null;
        }
      }

    return result;
    }

  /**
   * I need a way to reliably detect if there is an error or not.
   * Careful... should I look for resultReady too ?
   */
  public synchronized boolean isFailed ()
    {
    return isFailed;
    }

  /**
   * Used to return a result. We know that it is a good one.
   */
  public synchronized void putResult(DebuggerObject aResult, String anObjectName, InvokerRecord ir) 
    {
    result = aResult;
    resultName = anObjectName;
    resultReady = true;
    notifyAll();
    }
        
  /**
   * This is used to return an error. We know it is an error here !
   */
  public synchronized void putError (String error) 
    {
    errorMsg    = "Invocation: Error="+error;
    isFailed    = true;
    resultReady = true;
    notifyAll();
    }
        
  public String getError()
    {
    return errorMsg;
    }
        
  public String getResultName()
    {
    return resultName;
    }
  }


// ================== End of utility class =====================================
}            
