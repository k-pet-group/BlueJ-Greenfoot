package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.extensions.*;

/**
 * This is an invocation event, it gets generated when Objects or methods are
 * invoked on BlueJ objects.
 * The various methods here provided will return you the values of the invocation.
 * 
 * @version $Id: InvocationEvent.java 1719 2003-03-21 09:28:42Z damiano $
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
     * For bluej.extensions use ONLY
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
     * Get the type of event, at the moment it can only be INVOCATION_STARTED.
     * TODO: Differentiate between Constructors and Methods
     * 
     * @return the eventId of this particular event
     */
    public int getEvent()
      {
      return eventId;
      }

    /**
     * Gets the BPackage of this event, from this You can get the project.
     * 
     * @return the BPackage associated with this event.
     */
     
    public BPackage getBPackage()
      {
      return new BPackage (thisPackage);
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
     * Gets the method being called.
     * 
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
     * returns a meaningful version of this object.
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
