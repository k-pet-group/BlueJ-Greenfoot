package bluej.debugger;

import bluej.pkgmgr.Package;

/**
 * Event class that holds all the relevant information about
 * an execution.
 *
 * @author  Clive Miller
 * @version $Id: ExecutionEvent.java 1459 2002-10-23 12:13:12Z jckm $
 */

public class ExecutionEvent
{
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


    public static ExecutionEvent createConstructor (String className)
    {
        return new ExecutionEvent (className, null);
    }
    
    public static ExecutionEvent createStaticMethod (String objName)
    {
        return new ExecutionEvent (objName, null);
    }
    
    public static ExecutionEvent createObjectMethod (String objName)
    {
        return new ExecutionEvent (null, objName);
    }
    
    public static ExecutionEvent createFreeForm (Package pkg)
    {
        ExecutionEvent event = new ExecutionEvent (null, null);
        event.pkg = pkg;
        return event;
    }
    
    private String className, objectName;
    private String methodName;
    private Class[] signature;
    private String[] parameters;
    private String result;
    private String command;
    private Package pkg;

    private ExecutionEvent (String className, String objectName)
    {
        this.className = className;
        this.objectName = objectName;
    }

    void setObjectName (String objectName)
    {
        this.objectName = objectName;
    }
    
    void setMethodName (String methodName)
    {
        this.methodName = methodName;
    }
    
    void setParameters (Class[] signature, String[] parameters)
    {
        this.methodName = methodName;
        this.signature = signature;
        this.parameters = parameters;
    }

    void setResult (String result)
    {
        this.result = result;
    }
    
    void setPackage (Package pkg)
    {
        this.pkg = pkg;
    }
    
    void setCommand (String cmd)
    {
        this.command = cmd;
    }
    
    public String getClassName()
    {
        return className;
    }
    
    public String getObjectName()
    {
        return objectName;
    }
    
    public String getMethodName()
    {
        return methodName;
    }
    
    public Class[] getSignature()
    {
        return signature;
    }
    
    public String[] getParameters()
    {
        return parameters;
    }
    
    public String getResult()
    {
        return result;
    }
    
    public Package getPackage()
    {
        return pkg;
    }
    
    public String getCommand()
    {
        return command;
    }
}
