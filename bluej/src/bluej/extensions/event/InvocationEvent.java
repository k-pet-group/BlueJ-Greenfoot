/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions.event;

import bluej.debugger.*;
import bluej.debugger.gentype.*;
import bluej.debugmgr.*;
import bluej.debugmgr.objectbench.*;
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
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004
 */
public class InvocationEvent implements ExtensionEvent
{
    /**
     *  This event is returned in case of unknown mapping
     */
    public final static int UNKNOWN_EXIT = 0;
    /**
     * The execution finished normally.
     */
    public final static int NORMAL_EXIT = 1;
    /**
     * The execution finished through a call to <code>System.exit()</code>. This is
     * deprecated; it cannot actually occur.
     */
    @Deprecated
    public final static int FORCED_EXIT = 2;
    /**
     * The execution finished due to an exception
     */
    public final static int EXCEPTION_EXIT = 3;
    /**
     * The execution finished because the user forcefully terminated it
     */
    public final static int TERMINATED_EXIT = 4;

    private String className, objectName, methodName;
    private JavaType[] signature;
    private String[] parameters;
    private int invocationStatus;
    private bluej.pkgmgr.Package bluej_pkg;
    private DebuggerObject resultObj;


    /**
     * Constructor for the event.
     *
     * @param  exevent  Description of the Parameter
     */
    public InvocationEvent(ExecutionEvent exevent)
    {
        invocationStatus = UNKNOWN_EXIT;
        String resultType = exevent.getResult();

        if (resultType == ExecutionEvent.NORMAL_EXIT) {
            invocationStatus = NORMAL_EXIT;
        }
        if (resultType == ExecutionEvent.EXCEPTION_EXIT) {
            invocationStatus = EXCEPTION_EXIT;
        }
        if (resultType == ExecutionEvent.TERMINATED_EXIT) {
            invocationStatus = TERMINATED_EXIT;
        }

        bluej_pkg = exevent.getPackage();
        className = exevent.getClassName();
        objectName = exevent.getObjectName();
        methodName = exevent.getMethodName();
        signature = exevent.getSignature();
        parameters = exevent.getParameters();
        resultObj = exevent.getResultObject();
    }


    /**
     * Returns the invocation status. One of the values listed above.
     *
     * @return    The invocationStatus value
     */
    public int getInvocationStatus()
    {
        return invocationStatus;
    }


    /**
     * Returns the package in which this invocation took place.
     * Further information about the context of the event can be retrieved via the package object.
     *
     * @return    The package value
     */
    public BPackage getPackage()
    {
        return bluej_pkg.getBPackage();
    }


    /**
     * Returns the class name on which this invocation took place.
     * If you need further information about this class you can obtain a
     * BClass from <code>BPackage.getBClass()</code> using this name as a reference.
     *
     * @return    The className value
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
     *
     * @return    The objectName value
     */
    public String getObjectName()
    {
        return objectName;
    }


    /**
     * Returns the method name being called.
     * Returns <code>null</code> if this is an invocation of a constructor.
     *
     * @return    The methodName value
     */
    public String getMethodName()
    {
        return methodName;
    }


    /**
     * Returns the signature of the invoked method or constructor. 
     *
     * This is an array of Class objects representing the static types of 
     * the parameters to the method or constructor, in order. In the case of 
     * parameterised types, only the base type (e.g. List, not 
     * List<String>) is returned.
     *
     * @return    An array of Classes corresponding to the static types of the method's parameters.
     */
    public Class<?>[] getSignature()
    {
        if (signature == null) {
            return new Class[0];
        }

        Class<?>[] risul = new Class[signature.length];
        for (int index = 0; index < signature.length; index++) {
            JavaType sig = signature[index];
            if (sig.isPrimitive()) {
                // Map the primitive bluej types to java types
                if(sig == JavaPrimitiveType.getBoolean()) {
                    risul[index] = boolean.class;
                } 
                else if(sig == JavaPrimitiveType.getByte()) {
                    risul[index] = byte.class;
                }  
                else if(sig == JavaPrimitiveType.getChar()) {
                    risul[index] = char.class;
                }  
                else if(sig == JavaPrimitiveType.getDouble()) {
                    risul[index] = double.class;
                }  
                else if(sig == JavaPrimitiveType.getFloat()) {
                    risul[index] = float.class;
                }  
                else if(sig == JavaPrimitiveType.getInt()) {
                    risul[index] = int.class;
                }  
                else if(sig == JavaPrimitiveType.getLong()) {
                    risul[index] = long.class;
                }  
                else if(sig == JavaPrimitiveType.getShort()) {
                    risul[index] = short.class;
                }  
            }
            else {
                // It's a non-primitive class. Until we abandon Java 1.4
                // support, we can't use java.lang.reflect.Type, and we
                // don't want to reveal the JavaType hierarchy, so we use 
                // the "raw" class name provided by the JavaType

                String className = sig.asClass().classloaderName();
                risul[index] = bluej_pkg.getProject().loadClass(className);
            }
        }
        return risul;
    }


    /**
     * Returns the values of the parameters to the invocation as strings.
     * If a parameter really was a String, this will be returned either as the
     * name of the string instance, or as a literal string enclosed in double quotes.
     *
     * @return    The values of the parameters
     */
    public String[] getParameters()
    {
        if (parameters == null) {
            return new String[0];
        }
        return parameters;
    }


    /**
     * Returns the newly created object (if any).
     * If the object is one that can be put on the object bench it will be an instance of BObject.
     *
     * @return    an Object of various types or <code>null</code> if the result type is <code>void</code>.
     */

    // TODO: There ought to be a way of retrieving the declared return type of the invoked method.

    public Object getResult()
    {
        if (resultObj == null) {
            return null;
        }

        if (methodName != null) {
            return getMethodResult();
        }

        // Here I am dealing with a new instance...
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(bluej_pkg);
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), resultObj, resultObj.getGenType(), objectName);

        return ExtensionBridge.newBObject(wrapper);
    }


    /**
     * Returns of a result from a method call
     *
     * @return    The methodResult value
     */
    private Object getMethodResult()
    {
        ObjectReference objRef = resultObj.getObjectReference();
        ReferenceType type = objRef.referenceType();

        // It happens that the REAL result is in the result field of this Object...
        Field thisField = type.fieldByName("result");
        if (thisField == null) {
            return null;
        }

        // WARNING: I do not have the newly created name here....
        PkgMgrFrame aFrame = PkgMgrFrame.findFrame(bluej_pkg);
        return ExtensionBridge.getVal(aFrame, "", objRef.getValue(thisField));
    }


    /**
     * Returns a meaningful description of this Event.
     *
     * @return    Description of the Return Value
     */
    public String toString()
    {
        StringBuffer aRisul = new StringBuffer(500);

        aRisul.append("ResultEvent:");

        if (invocationStatus == NORMAL_EXIT) {
            aRisul.append(" NORMAL_EXIT");
        }
        if (invocationStatus == FORCED_EXIT) {
            aRisul.append(" FORCED_EXIT");
        }
        if (invocationStatus == EXCEPTION_EXIT) {
            aRisul.append(" EXCEPTION_EXIT");
        }
        if (invocationStatus == TERMINATED_EXIT) {
            aRisul.append(" TERMINATED_EXIT");
        }

        if (className != null) {
            aRisul.append(" BClass=" + className);
        }
        if (objectName != null) {
            aRisul.append(" objectName=" + objectName);
        }
        if (methodName != null) {
            aRisul.append(" methodName=" + methodName);
        }

        Object aResult = getResult();
        if (resultObj != null) {
            aRisul.append(" resultObj=" + aResult);
        }

        return aRisul.toString();
    }

}
