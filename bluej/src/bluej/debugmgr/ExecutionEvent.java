/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugmgr;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.JavaType;
import bluej.pkgmgr.Package;

/**
 * Event class that holds all the relevant information about
 * an execution.
 *
 * @author  Clive Miller
 */
@OnThread(Tag.Any)
public class ExecutionEvent
{
    /**
     * The execution has finished normally;
     */
    public static final String NORMAL_EXIT = "Normal exit";

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
    private JavaType[] signature;
    private String[] parameters;
    private String result;
    private String command;
    private Package pkg;
    private DebuggerObject resultObject;   // If there is a result object it goes here.
    private ExceptionDescription exception;  // If an exception occurred, this is it.

    /**
     * Constructs an ExecutionEvent where className and objName are null and only the package is set.
     * @param pkg The package this event is bound to.
     */
    public ExecutionEvent(Package pkg)
    {
        this.pkg = pkg;
    }
    
    /**
     * Constructs an ExecutionEvent given a className and objName.
     * @param pkg        The package this event is bound to.
     * @param className  The className of the event.
     * @param objectName The object name, as in the object bench, of the event, can be null.
     */
    public ExecutionEvent(Package pkg, String className, String objectName)
    {
        this.pkg = pkg;
        this.className = className;
        this.objectName = objectName;
    }

    public void setObjectName (String objectName)
    {
        this.objectName = objectName;
    }
    
    /**
     * Set the name of the called method. For a constructor call, this should be left null.
     */
    public void setMethodName (String methodName)
    {
        this.methodName = methodName;
    }
    
    /**
     * Set the types and values (Java expressions) of the method/constructor arguments.
     */
    public void setParameters (JavaType[] signature, String[] parameters)
    {
        this.signature = signature;
        this.parameters = parameters;
    }

    /**
     * Set the result of the execution. This should be one of:
     * NORMAL_EXIT - the execution terminated successfully
     * FORCED_EXIT - System.exit() was called
     * EXCEPTION_EXIT - the execution failed due to an exception
     * TERMINATED_EXIT - the user terminated the VM before execution completed
     */
    public void setResult (String result)
    {
        this.result = result;
    }

    /**
     * When an invocation has some valid result it can pass it on using this method.
     * 
     * <p>For a constructor call (class name is set but method name is not), the created
     * instance itself should be set as the result object. For any other invocation, the
     * result should be a wrapper with a single "result" field containing the actual result.
     */
    public void setResultObject (DebuggerObject resultObject)
    {
        this.resultObject = resultObject;
    }
    
    /**
     * Set the exception which occurred (if one did).
     */
    public void setException(ExceptionDescription exception)
    {
        this.exception = exception;
    }
    
    public void setCommand (String cmd)
    {
        this.command = cmd;
    }
    
    /**
     * Get the name of the class on which the invocation occurred.
     * For an instance method invocation, this returns the class of the object on
     *   which the method was invoked.
     * For a static method or constructor call, this returns the class name.
     * For a free-form invocation, this returns null.
     */
    public String getClassName()
    {
        return className;
    }
    
    /**
     * Get the name of the object on which the invocation occurred.
     * For constructor calls, returns the name of the constructed object.
     * For static method calls or free-form invocations returns null.
     */
    public String getObjectName()
    {
        return objectName;
    }
    
    /**
     * Get the name of the method which was invoked.
     * For a constructor or free-form invocation, this returns null.
     */
    public String getMethodName()
    {
        return methodName;
    }
    
    /**
     * Get the method/constructor parameter types.
     * For a free-form invocation this returns null. 
     */
    public JavaType[] getSignature()
    {
        return signature;
    }
    
    /**
     * Gets the arguments to the method/constructor (as java expressions).
     * For a free-form invocation this returns null.
     */
    public String[] getParameters()
    {
        return parameters;
    }
    
    /**
     * Get the result of the execution. This will be one of:
     * NORMAL_EXIT - the execution terminated successfully
     * EXCEPTION_EXIT - the execution failed due to an exception
     * TERMINATED_EXIT - the user terminated the VM before execution completed
     */
    public String getResult()
    {
        return result;
    }

    /**
     * This is the Object resulting from the invocation.
     * 
     * <p>For a constructor call (class name is set but method name is not), the created
     * instance itself is the result object. For any other invocation, the result object is 
     * a wrapper with a single "result" field containing the actual invocation result.
     */
    public DebuggerObject getResultObject()
    {
        return resultObject;
    }

    /**
     * Get the exception which occurred (valid if the result is EXCEPTION_EXIT).
     */
    public ExceptionDescription getException()
    {
        return exception;
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
