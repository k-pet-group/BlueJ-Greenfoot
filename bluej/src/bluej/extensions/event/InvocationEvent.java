package bluej.extensions.event;

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
 * @author Clive Miller
 * @version $Id: InvocationEvent.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class InvocationEvent extends BJEvent
{
    /**
     * Event id: Occurs when a method call has just begun. This includes constructors, object and static methods.
     */
    public static final int INVOCATION_STARTED = 4;

   /**
    * Event id: Occurs when a method call has finished its execution.
    */
    public static final int INVOCATION_FINISHED = 8;
    
    /**
     * Event id: A bitwise combination of INVOCATION_STARTED and INVOCATION_FINISHED
     */
    public static final int INVOCATION_EVENT = INVOCATION_STARTED | INVOCATION_FINISHED;
    
    /**
     * The execution has finished normally;
     */
    public static final String NORMAL_EXIT = "Normal exit";

    /**
     * The execution has finished through a call to System.exit();
     */
    public static final String FORCED_EXIT = "Forced exit";

    /**
     * The execution has finished due to an exception
     */
    public static final String EXCEPTION_EXIT = "An exception occurred";

    /**
     * The execution has finished because the user has forcefully terminated it
     */
    public static final String TERMINATED_EXIT = "User terminated";

    private String className, objectName, methodName, result;
    private Class[] signature;
    private String[] parameters;
    
    /**
     * Constructs an INVOCATION_STARTED event.
     * @param className the name of the class being operated on, or <code>null</code> if it is an object method
     * @param objectName the name of the object being operated on, or <code>null</code> if it is a static method, or the
     * new instance name if it is a constructor
     * @param methodName the name of the method being called, or <code>null</code> if it is a constructor
     * @param signature the signature of the called method or constructor
     * @param parameters the parameters in string form. If a parameter really is a string, this should be either the
     * name of a string instance, or a literal string enclosed by double quotes.
     */
    public InvocationEvent (BPackage pkg, String className, String objectName, String methodName, Class[] signature, String[] parameters)
    {
        super (INVOCATION_STARTED, pkg);
        this.className = className;
        this.objectName = objectName;
        this.methodName = methodName;
        this.signature = signature;
        this.parameters = parameters;
    }
    
    /**
     * Constructs an INVOCATION_FINISHED event.
     * @param result one of NORMAL_EXIT, FORCED_EXIT, EXCEPTION_EXIT or TERMINATED_EXIT
     */
    public InvocationEvent (BPackage pkg, String result)
    {
        super (INVOCATION_FINISHED, pkg);
        this.result = result;
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
