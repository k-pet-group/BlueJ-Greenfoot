package bluej.debugmgr;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenType;
import bluej.pkgmgr.Package;

/**
 * Event class that holds all the relevant information about
 * an execution.
 *
 * @author  Clive Miller
 * @version $Id: ExecutionEvent.java 3019 2004-09-28 09:51:57Z damiano $
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
    
    private String className, objectName;
    private String methodName;
    private GenType[] signature;
    private String[] parameters;
    private String result;
    private String command;
    private Package pkg;
    private DebuggerObject resultObject;   // If there is a result object it goes here.

    /**
     * Constructs an ExecutionEvent where className and objName are null and only the package is set.
     * @param pkg The package this event is bound to.
     */
    ExecutionEvent ( Package pkg )
    {
        this.pkg = pkg;
    }
    
    /**
     * Constructs an ExecutionEvent given a className and objName.
     * @param className  the className of the event.
     * @param objectName the object name, as in the object bench, of the event, can be null.
     */
    ExecutionEvent (String className, String objectName)
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
    
    void setParameters (GenType[] signature, String[] parameters)
    {
        this.signature = signature;
        this.parameters = parameters;
    }

    void setResult (String result)
    {
        this.result = result;
    }

    /**
     * When an invocation has some valid result it can pass it on using this method.
     */
    void setResultObject (DebuggerObject resultObject)
    {
        this.resultObject = resultObject;
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
    
    public GenType[] getSignature()
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

    /**
     * This is the Object resulting from the invocation.
     */
    public DebuggerObject getResultObject()
    {
        return resultObject;
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
