package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.extensions.*;

/**
 * An invocation event, it gets generated when an Object constructor is called or
 * a method is called.
 * 
 * @version $Id: InvocationEvent.java 1726 2003-03-24 13:33:06Z damiano $
 */
public class InvocationEvent extends BluejEvent
{
    /**
     * Occurs when a method call has just begun. This includes constructors, object and static methods.
     */
    public static final int INVOCATION_STARTED = 4;

    private String className, objectName, methodName, result;
    private Class[] signature;
    private String[] parameters;
    private int eventId;
    private bluej.pkgmgr.Package thisPackage;
    
    /** 
     * NOT to be used by Extension writer.
     */
    public InvocationEvent ( int BluejEventId, ExecutionEvent exevent )
      {
      eventId     = INVOCATION_STARTED;
      thisPackage = exevent.getPackage();
      className   = exevent.getClassName();
      objectName  = exevent.getObjectName();
      methodName  = exevent.getMethodName();
      signature   = exevent.getSignature();
      parameters  = exevent.getParameters();
      }
     
    /**
     * Returns the type of event.
     */
    public int getEvent()
      {
      return eventId;
      }

    /**
     * Returns the BPackage of this event.
     * Using a BPackege it is possible to obtain further information on what is being invoked.
     */
     
    public BPackage getBPackage()
      {
      return new BPackage (thisPackage);
      }

    /**
     * Returns the Class name on which the event happened.
     * If you need to have further information about this Class you can obtain a 
     * BClass from BPackage using this name as a reference.
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * Returns the instanceName, that is the name of the object on the object bench.
     * If you need the BObject you can use the getObject(instanceName) in the BPackage using
     * this name as a key.
     * 
     * In case of a static method this will be null
     * If it is a constructor call it will be the new instance name of the opbject
     * For methods call it will be the name of the object where the operation occourred.
     */
    public String getObjectName()
    {
        return objectName;
    }
    
    /**
     * Returns the method name being called.
     * It can be null if this is an invocation of a constructor.
     */
    public String getMethodName()
    {
        return methodName;
    }
    
    /**
     * Returns the signature of the called method or the one of the constructor.
     */
    public Class[] getSignature()
    {
        return signature;
    }
    
    /**
     * Returns the parameters in string form. 
     * If a parameter really is a string, this should be either the
     * name of a string instance, or a literal string enclosed by double quotes.
     */
    public String[] getParameters()
    {
        return parameters;
    } 

    /**
     * Returns a meaningful description of this Event.
     */
    public String toString() 
      {
      StringBuffer aRisul = new StringBuffer (500);

      aRisul.append("InvocationEvent:");

      if ( className != null ) aRisul.append(" BClass="+className);
      if ( objectName != null ) aRisul.append(" objectName="+objectName);
      if ( methodName != null ) aRisul.append(" methodName="+methodName);
      
      return aRisul.toString();      
      }

}
