package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.extensions.BPackage;

/**
 * This class represents an invocation event.
 * The various methods here provided will return you the values of the invocation.
 * 
 * @version $Id: InvocationEvent.java 1685 2003-03-10 12:49:18Z damiano $
 */
public class InvocationEvent extends ExtEvent
{
    // Occurs when a method call has just begun. This includes constructors, object and static methods.
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
      return new BPackage (thisPackage);
      }

    /**
     * @return the name of the class being operated on, or <code>null</code> if it is an object method
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * @return the name of the object being operated on, or <code>null</code> if it is a static method, or the
     * new instance name if it is a constructor
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
     * returns a meaningful version of this object.
     */
    public String toString() 
      {
      StringBuffer aRisul = new StringBuffer (500);

      aRisul.append("InvocationEvent:");

      if ( className != null ) aRisul.append(" className="+className);
      if ( objectName != null ) aRisul.append(" objectName="+objectName);
      if ( methodName != null ) aRisul.append(" methodName="+methodName);
      
      return aRisul.toString();      
      }


    

}
