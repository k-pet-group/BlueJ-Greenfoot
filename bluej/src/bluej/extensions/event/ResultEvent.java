package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.extensions.BPackage;

/**
 * <pre>This Class represent a Result event, it is mostly similar to an Invocation now
 * but it will be different as soon as I am able to get the returnin values.
 * Damiano
 * </pre>
 * @version $Id: ResultEvent.java 1682 2003-03-10 11:58:52Z damiano $
 */
public class ResultEvent extends ExtEvent
{
    // This event is returned in case of unknown mapping
    public static final int UNKNOWN_EXIT = 0;
    // The execution has finished normally;
    public static final int NORMAL_EXIT = 1;
    // The execution has finished through a call to System.exit();
    public static final int FORCED_EXIT = 2;
    // The execution has finished due to an exception
    public static final int EXCEPTION_EXIT = 3;
    // The execution has finished because the user has forcefully terminated it
    public static final int TERMINATED_EXIT = 4;

    private String className, objectName, methodName;
    private Class[] signature;
    private String[] parameters;
    private int eventId;
    private bluej.pkgmgr.Package thisPackage;
    private Object resultObj;
    
    
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

      thisPackage = exevent.getPackage();
      className   = exevent.getClassName();
      objectName  = exevent.getObjectName();
      methodName  = exevent.getMethodName();
      signature   = exevent.getSignature();
      parameters  = exevent.getParameters();
      resultObj   = exevent.getResult();
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
     * The resulting object should be one of the primitive wrappers one (like Integer, Long, etc)
     * or a BObject that you can manage.
     * 
     * @return an Object of various types depending on the type. It can return null if the risul is void.
     */
    public Object getResult ()
      {
      return resultObj;
      }
}
