package bluej.extensions.event;

import bluej.debugger.ExecutionEvent;
import bluej.extensions.BPackage;

/**
 * This class represents an invocation event. It is provided to a <code>BJEventListener</code> 
 * that has registered an interest in receiving such events with 
 * a <CODE>BlueJ</CODE> object by using its <CODE>addBJEventListener</CODE> method.
 * <p>The parameters provided indicated the method that has occurred.
 * <table border><tr><td><th>className<th>objectName<th>methodName</tr>
 * <tr><th>Constructor<td>class name<td>new instance name<td><code>null</code></tr>
 * <tr><th>Static method<td>class name<td><code>null</code><td>method name</tr>
 * <tr><th>Object method<td><code>null</code><td>object name<td>method name</tr>
 * </table>
 * @version $Id: InvocationEvent.java 1671 2003-03-10 08:58:32Z damiano $
 */
public class InvocationEvent extends ExtEvent
{
    // Occurs when a method call has just begun. This includes constructors, object and static methods.
    public  final int INVOCATION_STARTED = 4;

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
     * Gets the result when an invocation has finished.
     * @return one of NORMAL_EXIT, FORCED_EXIT, EXCEPTION_EXIT or TERMINATED_EXIT
     */
    public String getResult()
    {
        return result;
    }
}
