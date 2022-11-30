/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.MethodView;
import bluej.views.View;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

import java.lang.reflect.Modifier;

/**
 * A wrapper for a method of a BlueJ class.
 * Allows an extension to invoke a method on an object that is on the BlueJ object bench.
 * When values representing types are returned, there are two cases:
 * In the case that the returned value is of primitive type (<code>int</code> etc.), 
 * it is represented in the appropriate Java wrapper type (<code>Integer</code> etc.).
 * In the case that the returned value is an object type then an appropriate BObject will 
 * be returned, allowing the returned object itself to be placed on the BlueJ object bench.
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury 2003
 */
public class BMethod
{
    // The same reasoning as of BConstructor applies here.
    
    private Identifier parentId;
    private MethodView bluej_view;

    /**
     * @param  aParentId the {@link Identifier} of the parent class.
     * @param  i_bluej_view the {@link MethodView} of this method.
     */
    BMethod (Identifier aParentId, MethodView i_bluej_view )
    {
        parentId   = aParentId;
        bluej_view = i_bluej_view;
    }

    /**
     * Tests if this method matches against the given signature.
     * Pass a zero length parameter array if the method takes no arguments.
     *
     * @param methodName the name of the method to compare this method with.
     * @param parameter an array of {@link Class} objects representing the signature to compare this method with.
     */
    public boolean matches ( String methodName, Class<?>[] parameter )
    {
        // If someone is crazy enough to do this he deserves it :-)
        if ( methodName == null ) return false;

        // Let me se if the named method is OK
        if ( ! methodName.equals(bluej_view.getName() ) ) return false;

        Class<?>[] thisArgs = bluej_view.getParameters();

        // An empty array is equivalent to a null array
        if (thisArgs  != null && thisArgs.length  <= 0)  thisArgs  = null;
        if (parameter != null && parameter.length <= 0 ) parameter = null;

        // If both are null the we are OK
        if ( thisArgs == null && parameter == null ) return true;

        // If ANY of them is null we are in trouble now. (They MUST be both NOT null)
        if ( thisArgs == null || parameter == null ) return false;

        // Now I know that BOTH are NOT empty. They MUST be the same length
        if ( thisArgs.length != parameter.length ) return false;

        for ( int index=0; index<thisArgs.length; index++ ) {
            if ( ! thisArgs[index].isAssignableFrom(parameter[index]) ) {
                return false;
            }
        }

        return true;
    }

     /**
     * Returns the class's name that declares this method.
     * Similar to Reflection API.
     */
    public String getDeclaringClass()
    {
        return bluej_view.getClassName();
    }

    /**
     * Returns the types of the parameters of this method.
     * Similar to Reflection API.
     *
     * @return An array of {@link Class} objects representing the return type of this method.
     */
    public Class<?>[] getParameterTypes()
    {
        return bluej_view.getParameters();
    }

    /**
     * Returns the name of this method.
     * Similar to Reflection API.
     */
    public String getName()
    {
        return bluej_view.getName();
    }

    /**
     * Returns the return type of this method.
     * Similar to Reflection API.
     *
     * @return A {@link Class} object representing the return type of this method.
     */
    public Class<?> getReturnType()
    {
        View aView = bluej_view.getReturnType();
        return aView.getViewClass();
    }

    /**
     * Returns the modifier of this method. The
     * {@link java.lang.reflect.Modifier} class can be used to decode the
     * modifiers.
     *
     * @return An int value representing the modifiers which can be decoded with <code>java.lang.reflect.Modifier</code>.
     *
     */
    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     * Invokes this method on the given object. Note that this method should
     * not be called from the JavaFX (GUI) thread.
     *
     * <p>The arguments passed in the initargs array may have any type,
     * but the type will determine exactly what is passed to the
     * method:
     *
     * <ul>
     * <li>String - the String will be passed directly to the method
     * <li>BObject - the object will be passed directly to the method,
     *               though it must be on the object bench for this to work
     * <li>Anything else - toString() is called on the object and the
     *               result is treated as a Java expression, which is
     *               evaluated and passed to the method.
     * </ul>
     *
     * <p>An attempt is made to ensure that the argument types are suitable
     * for the method. InvocationArgumentException will be thrown if
     * the arguments are clearly unsuitable, however some cases will
     * generate an InvocationErrorException instead. In such cases no
     * expression arguments will be evaluated.
     *
     * <p>If the method invoked is <code>public static void main(String [] args)</code>, then the invocation will,
     * as a side-effect, reset the VM used by BlueJ to run user code in this project, and clear the object bench. This
     * behaviour matches the effect of invoking a main method through the BlueJ GUI.
     *
     * @param onThis a {@link BObject} object on which the method call should be applied, null if a static method.
     * @param params an array of {@link Object} objects containing the arguments, or null if there are none
     * @return The resulting {@link Object} object. It can be a wrapper for a primitive type or a BObject
     * @throws ProjectNotOpenException if the project to which this object belongs has been closed by the user.
     * @throws PackageNotFoundException if the package to which this object belongs has been deleted by the user.
     * @throws InvocationArgumentException if the <code>params</code> don't match the object's arguments.
     * @throws InvocationErrorException if an error occurs during the invocation.
     */
    public Object invoke (BObject onThis, Object[] params)
        throws ProjectNotOpenException, PackageNotFoundException,
            InvocationArgumentException, InvocationErrorException
    {
        ObjectWrapper instanceWrapper=null;
        // If it is a method call on a live object get the identifier for it.
        if ( onThis != null ) instanceWrapper = onThis.getObjectWrapper();

        PkgMgrFrame  pkgFrame = parentId.getPackageFrame();
        DirectInvoker invoker = new DirectInvoker(pkgFrame);
        DebuggerObject result = invoker.invokeMethod (instanceWrapper, bluej_view, params);

        // We return null if the method is void (as per Reflection), which might 
        // either be a null result object, or a valid result object representing null
        if (result == null || result.isNullObject()) return null;

        String resultName = invoker.getResultName();

        ObjectReference objRef = result.getObjectReference();
        ReferenceType type = objRef.referenceType();

        // It happens that the REAL result is in the result field of this Object...
        Field thisField = type.fieldByName ("result");
        if ( thisField == null ) return null;

        // DOing this is the correct way of returning the right object. Tested 080303, Damiano
        return BField.doGetVal(pkgFrame, resultName, objRef.getValue(thisField));
    }
    
  
    /**
     * Returns a string representing the return type, name and signature of this method
     */
    public String toString()
    {
        Class<?>[] signature = getParameterTypes();
        String sig = "";
        for (int i=0; i<signature.length; i++) {
            sig += signature[i].getName() + (i==signature.length-1?"":", ");
        }
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getReturnType()+" "+getName()+"("+sig+")";
    }
}
