package bluej.extensions.event;

import bluej.debugger.DebuggerObject;
import bluej.debugger.ExecutionEvent;
import bluej.debugger.ObjectWrapper;
import bluej.extensions.*;
import bluej.pkgmgr.*;
import com.sun.jdi.*;



/**
 * This class encapsulates events generated when the construction or invocation 
 * of a BlueJ object finishes.
 * An invocation may finish in a normal way or it may be interrupted.
 * From this event you can extract the actual result of the invocation, and access the BlueJ
 * classes and objects involved.
 * 
 * @version $Id: InvocationResultEvent.java 1869 2003-04-21 11:04:44Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
 
public class InvocationResultEvent implements BlueJExtensionEvent
{
    // This event is returned in case of unknown mapping
    public static final int UNKNOWN_EXIT = 0;
    /**
     * The execution finished normally.
     */
    public static final int NORMAL_EXIT = 1;
    /**
     * The execution finished through a call to <code>System.exit()</code>
     */
    public static final int FORCED_EXIT = 2;
    /**
     * The execution finished due to an exception
     */ 
    public static final int EXCEPTION_EXIT = 3;
    /**
     * The execution finished because the user forcefully terminated it
     */
    public static final int TERMINATED_EXIT = 4;

    private String className, objectName, methodName;
    private Class[] signature;
    private String[] parameters;
    private int eventId;
    private bluej.pkgmgr.Package bluej_pkg;
    private DebuggerObject resultObj;
    
    
    /** 
     * Constructor for an InvocationResultEvent
     */
    public InvocationResultEvent ( ExecutionEvent exevent )
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
     * Returns the eventId of this event.
     */
    public int getEvent()
      {
      return eventId;
      }

    /**
     * Returns the package in which this invocation took place.
     * Using this BPackage you can retrieve further information about this Event.
     */
    public BPackage getBPackage()
      {
      return ExtensionBridge.newBPackage (bluej_pkg);
      }

    /**
     * Returns the class name on which this invocation took place.
     * If you need further information about this class you can obtain a 
     * BClass from <code>BPackage.getBClass()</code> using this name as a reference.
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * Returns the instance name of the invoked object on the object bench.
     * If you need further information about this object you can obtain a BObject using
     * <code>BPackage.getObject()</code> using this name as a reference.
     * 
     * For a static method invocation, this method will return <code>null</code>.
     * For a constructor call it will return the new instance name of the object on the object bench.
     * For a method call it will return the name of the object on which the operation was invoked.
     */
    public String getObjectName()
    {
        return objectName;
    }
    
    /**
     * Returns the method name being called.
     * Returns <code>null</code> if this is an invocation of a constructor.
     */
    public String getMethodName()
    {
        return methodName;
    }
    
    /**
     * Returns the signature of the invoked method or constructor.
     */
    public Class[] getSignature()
    {
        return signature;
    }
    
    /**
     * Returns the parameters of the invocation in string form. 
     * If a parameter really is a string, this will be either the
     * name of the string instance, or a literal string enclosed in double quotes.
     */
    public String[] getParameters()
    {
        return parameters;
    } 

    /**
     * Returns the newly created object (if any).
     * If the object is one that can be put on the object bench it will be an instance of BObject.
     * 
     * @return an Object of various types or <code>null</code> if the result type is <code>void</code>.
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

      return ExtensionBridge.newBObject(wrapper);
      }

    /**
     * Manage the return of a result from a method call
     */
    private Object getMethodResult()
      {
      ObjectReference objRef = resultObj.getObjectReference();
      ReferenceType type = objRef.referenceType();

      // It happens that the REAL result is in the result field of this Object...
      Field thisField = type.fieldByName ("result");
      if ( thisField == null ) return null;

      // WARNING: I do not have the newly created name here....
      return ExtensionBridge.getVal(bluej_pkg, "", objRef.getValue(thisField));
      }


    /**
     * Returns a meaningful description of this Event.
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
