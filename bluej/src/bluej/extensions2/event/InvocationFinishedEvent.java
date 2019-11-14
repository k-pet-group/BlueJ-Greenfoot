/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.event;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions2.BPackage;
import bluej.extensions2.ExtensionBridge;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.actions.RunTestsAction;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * This class encapsulates events generated when the construction or invocation
 * of a BlueJ object finishes.
 * An invocation may finish in a normal way or it may be interrupted.
 * From this event, an extension can extract the actual result of the invocation, and access the BlueJ
 * classes and objects involved.
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003,2004
 */
@OnThread(Tag.Any)
public class InvocationFinishedEvent implements ExtensionEvent
{
    /**
     * Types of invocation finished events.
     */
    public static enum EventType
    {
        /**
         * Event generated when an invocation finished with unknown event mapping.
         */
        UNKNOWN_EXIT,
        /**
         * Event generated when an invocation finished normally.
         */
        NORMAL_EXIT,

        /**
         * Event generated when an invocation terminated due to an exception.
         */
        EXCEPTION_EXIT,

        /**
         * Event generated when an invocation terminated forcefully by the user.
         */
        TERMINATED_EXIT
    }


    private String className, objectName, methodName;
    private JavaType[] signature;
    private String[] parameters;
    private EventType terminationType;
    private bluej.pkgmgr.Package bluej_pkg;
    private DebuggerObject resultObj;


    /**
     * @param  exevent  an {@link ExecutionEvent} object associated with the invocation.
     */
    public InvocationFinishedEvent(ExecutionEvent exevent)
    {
        terminationType = EventType.UNKNOWN_EXIT;
        String resultType = exevent.getResult();
        switch (resultType)
        {
            case ExecutionEvent.NORMAL_EXIT:
                terminationType = EventType.NORMAL_EXIT;
                break;
            case ExecutionEvent.EXCEPTION_EXIT:
                terminationType = EventType.EXCEPTION_EXIT;
                break;
            case ExecutionEvent.TERMINATED_EXIT:
                terminationType = EventType.TERMINATED_EXIT;
                break;
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
     * Returns the termination type.
     *
     * @return The {@link EventType} value associated with this InvocationFinishedEvent.
     */
    public EventType getEventType()
    {
        return terminationType;
    }


    /**
     * Returns the package in which this invocation took place.
     * Further information about the context of the event can be retrieved via the package object.
     *
     * @return The {@link BPackage} object wrapping the package in which this invocation took place.
     */
    public BPackage getPackage()
    {
        return bluej_pkg.getBPackage();
    }


    /**
     * Returns the class name on which this invocation took place.
     * Further information about this class can be obtained with 
     * {@link BPackage#getBClass(String name)} using this name as a reference.
     */
    public String getClassName()
    {
        return className;
    }


    /**
     * Returns the instance name of the invoked object on the object bench.
     * Further information about this object can be obtained with {@link BPackage#getObject(String)} using this name as a reference.

     * For a static method invocation, this method will return <code>null</code>.
     * For a constructor call it will return the new instance name of the object on the object bench.
     * For a method call it will return the name of the object on which the operation was invoked.
     */
    public String getObjectName()
    {
        return objectName;
    }


    /**
     * Returns the method name being called.
     * Returns <code>null</code> if this is an invocation of a constructor.
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
     * @return  An array of {@link Class} objects corresponding to the static types of the method's parameters.
     */
    @OnThread(Tag.FXPlatform)
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
     * Returns the values of the parameters to the invocation as String objects.
     * If a parameter really was a String, this will be returned either as the
     * name of the string instance, or as a literal string enclosed in double quotes.
     *
     * @return    An array of {@link String} objects containing the values of the parameters.
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
     * If the object is one that can be put on the object bench it will be an instance of {@link bluej.extensions2.BObject}.
     *
     * @return    an Object object of various types, <code>null</code> if the result type is <code>void</code>.
     */

    // TODO: There ought to be a way of retrieving the declared return type of the invoked method.
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
     */
    public String toString()
    {
        StringBuffer aRisul = new StringBuffer(500);

        aRisul.append("ResultEvent: ");
        aRisul.append(terminationType.toString());

        if (className != null) {
            aRisul.append(" BClass=" + className);
        }
        if (objectName != null) {
            aRisul.append(" objectName=" + objectName);
        }
        if (methodName != null) {
            aRisul.append(" methodName=" + methodName);
        }

        if (resultObj != null) {
            Platform.runLater(() ->  aRisul.append(" resultObj=" + getResult()));
        }

        return aRisul.toString();
    }

}
