package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.extensions.*;

import com.sun.jdi.*;
import bluej.pkgmgr.*;

/**
 * This Class represent a Result event, an event generated when the invocation finished.
 * From this event you can extract the actual result of the invocation.
 * 
 * @version $Id: ResultEvent.java 1719 2003-03-21 09:28:42Z damiano $
 */
public class ResultEvent extends BluejEvent
{
    // This event is returned in case of unknown mapping
    public static final int UNKNOWN_EXIT = 0;
    /**
     * The execution has finished normally;
     */
    public static final int NORMAL_EXIT = 1;
    /**
     * The execution has finished through a call to System.exit();
     */
    public static final int FORCED_EXIT = 2;
    /**
     * The execution has finished due to an exception
     */ 
    public static final int EXCEPTION_EXIT = 3;
    /**
     * The execution has finished because the user has forcefully terminated it
     */
    public static final int TERMINATED_EXIT = 4;

    private String className, objectName, methodName;
    private Class[] signature;
    private String[] parameters;
    private int eventId;
    private bluej.pkgmgr.Package bluej_pkg;
    private DebuggerObject resultObj;
    
    
    /** 
     * For bluej.extensions use ONLY
     */
    public ResultEvent ( int BluejEventId, ExecutionEvent exevent )
      {
      eventId = UNKNOWN_EXIT; // a Preset
      String resultType = exevent.getResult();
      
      if ( resultType == ExecutionEvent.NORMAL_EXIT) eventId = NORMAL_EXIT;
      if ( resultType == ExecutionEvent.FORCED_EXIT) eventId = FORCED_EXIT;
      if ( resultType == ExecutionEvent.EXCEPTION_EXIT) eventId = EXCEPTION_EXIT;
      if ( resultType == ExecutionEvent.TERMINATED_EXIT) eventId = TERMINATED_EXIT;

      bluej_pkg   = exevent.getPackage();
      className   = exevent.getClassName();
      objectName  = exevent.getObjectName();
      methodName  = exevent.getMethodName();
      signature   = exevent.getSignature();
      parameters  = exevent.getParameters();
      resultObj   = exevent.getResultObject();
      }
     
    /**
     * @return the eventId of this particular event
     */
    public int getEvent()
      {
      return eventId;
      }

    /**
     * @return the BPackage associated with this event.
     */
     
    public BPackage getBPackage()
      {
      return new BPackage (bluej_pkg);
      }

    /**
     * Gets the Class name on which the event happened.
     * If you need to have further information about this Class you can obtain a 
     * BClass from BPackage using this name as a reference.
     * 
     * @return the Class name associated with this event
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * Gets what is the instanceName, that is the name of the object on the object bench.
     * If you need the BObject you can use the getObject(instanceName) in the BPackage using
     * this name as a key.
     * 
     * In case of a static method this will be null
     * If it is a constructor call it will be the new instance name of the opbject
     * For methods call it will be the name of the object where the operation occourred.
     * 
     * @return the instance name of the object being operated on.
     */
    public String getObjectName()
    {
        return objectName;
    }
    
    /**
     * @return the name of the method being called, or <code>null</code> if it is a constructor
     */
    public String getMethodName()
    {
        return methodName;
    }
    
    /**
     * @return the signature of the called method or constructor
     */
    public Class[] getSignature()
    {
        return signature;
    }
    
    /**
     * @return the parameters in string form. If a parameter really is a string, this should be either the
     * name of a string instance, or a literal string enclosed by double quotes.
     */
    public String[] getParameters()
    {
        return parameters;
    } 

    /**
     * Returns the newly created Object (if any).
     * If the object is one that you can put in the bench it will be a BObject...
     * 
     * @return an Object of various types depending on the type. It can return null if the risul is void.
     */
    public Object getResult ()
      {
      if ( resultObj == null ) return null;

      if ( methodName != null ) return getMethodResult();
      
      // Here I am dealing with a new instance...
      DebuggerObject realResult = resultObj.getInstanceFieldObject(0);
      if ( realResult == null ) return null;

      PkgMgrFrame pmf   = PkgMgrFrame.findFrame(bluej_pkg);

      ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), realResult, objectName);

      return new BObject(wrapper);
      }

    /**
     * Manage the return of a result from a amethod call
     */
    private Object getMethodResult()
      {
      ObjectReference objRef = resultObj.getObjectReference();
      ReferenceType type = objRef.referenceType();

      // It happens that the REAL result is in the result field of this Object...
      Field thisField = type.fieldByName ("result");
      if ( thisField == null ) return null;

      // WARNING: I do not have the newly created name here....
      return BField.getVal(bluej_pkg, "", objRef.getValue(thisField));
      }


    /**
     * returns a meaningful version of this object.
     */
    public String toString() 
      {
      StringBuffer aRisul = new StringBuffer (500);

      aRisul.append("ResultEvent:");

      if ( eventId == NORMAL_EXIT ) aRisul.append(" NORMAL_EXIT");
      if ( eventId == FORCED_EXIT ) aRisul.append(" FORCED_EXIT");
      if ( eventId == EXCEPTION_EXIT ) aRisul.append(" EXCEPTION_EXIT");
      if ( eventId == TERMINATED_EXIT ) aRisul.append(" TERMINATED_EXIT");

      if ( className != null ) aRisul.append(" BClass="+className);
      if ( objectName != null ) aRisul.append(" objectName="+objectName);
      if ( methodName != null ) aRisul.append(" methodName="+methodName);

      Object aResult = getResult();
      if ( resultObj != null ) aRisul.append(" resultObj="+aResult);
      
      return aRisul.toString();      
      }
}
