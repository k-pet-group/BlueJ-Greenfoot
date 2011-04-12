/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;

import java.util.List;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.jdi.JdiObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Debug;
import bluej.views.FieldView;
import bluej.views.View;

import com.sun.jdi.BooleanValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.Field;
import com.sun.jdi.FloatValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ShortValue;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

/**
 * A wrapper for a field of a BlueJ class.
 * Behaviour is similar to the Reflection API.
 * 
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 * @author Clive Miller, University of Kent at Canterbury, 2002
 */
public class BField
{
    private final FieldView bluej_view;
    private final Identifier parentId;


    /**
     *Constructor for the BField object
     *
     * @param  aParentId     Description of the Parameter
     * @param  i_bluej_view  Description of the Parameter
     */
    BField(Identifier aParentId, FieldView i_bluej_view)
    {
        parentId = aParentId;
        bluej_view = i_bluej_view;
    }


    /**
     * Check to see if the field name matches the given one.
     * 
     * <p>This method is deprecated. Use "getName().equals(fieldName)" instead.
     *
     * @param  fieldName  the field name to compare with
     * @return            true if it does, false otherwise
     */
    @Deprecated
    public boolean matches(String fieldName)
    {
        return getName().equals(fieldName);
    }


    /**
     * Return the name of the field.
     * Similar to reflection API.
     *
     * @return    The name value
     */
    public String getName()
    {
        return bluej_view.getName();
    }


    /**
     * Return the type of the field.
     * Similar to Reflection API.
     *
     * @return    The type value
     */
    public Class<?> getType()
    {
        return bluej_view.getType().getViewClass();
    }


    /**
     * Returns the java Field for inspection.
     * Use this method when you need more information about the Field than
     * is provided by the BField interface. E.g.:
     * What is the declaring class of this Field?
     *
     * Note that this is for information only. If you want to interact with BlueJ you must
     * use the methods provided in BField.
     * 
     * @return    The java.lang.reflect.Field providing extra information about this BField.
     */
    public java.lang.reflect.Field getJavaField()
    {
        return bluej_view.getField();
    }
    
    /**
     * Returns the modifiers of this field.
     * The <code>java.lang.reflect.Modifier</code> class can be used to decode the modifiers.
     * Similar to reflection API
     *
     * @return    The modifiers value
     */
    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     * When you are inspecting a static field use this one.
     *
     * @return                            The staticField value
     * @throws  ProjectNotOpenException   if the project to which this field belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which this field belongs has been deleted by the user.
     */
    private Object getStaticField() throws ProjectNotOpenException, PackageNotFoundException
    {
        Package bluejPkg = parentId.getBluejPackage();
        PkgMgrFrame aFrame = parentId.getPackageFrame();
        String wantFieldName = getName();

        // I need to get the view of the parent of this Field
        // That must be the Class that I want to look after....
        View parentView = bluej_view.getDeclaringView();
        String className = parentView.getQualifiedName();

        DebuggerClass debuggerClass;
        try {
            debuggerClass = bluejPkg.getDebugger().getClass(className, true);
        }
        catch (java.lang.ClassNotFoundException cnfe) {
            // This may not be an error, the class name may be wrong...
            Debug.message("BField.getStaticField: Class=" + className + " Field=" + wantFieldName + " WARNING: cannod get debuggerClass");
            return null;
        }

        // Now I want the Debugger object of that field.
        // I do it this way since there is no way to get it by name...
        DebuggerObject debugObj = null;
        List<DebuggerField> staticFields = debuggerClass.getStaticFields();
        for (DebuggerField field : staticFields) {
            if (wantFieldName.equals(field.getName())) {
                debugObj = field.getValueObject(null);
                break;
            }
        }

        if (debugObj == null) {
            // No need to complain about it it may not be a static field...
            return null;
        }

        ObjectReference objRef = debugObj.getObjectReference();
        if (objRef == null) {
            // Without JDI this cannot work.
            return null;
        }

        return doGetVal(aFrame, wantFieldName, objRef);
    }


    /**
     * Return the value of this field of the given object.
     * This is similar to Reflection API.
     *
     * In the case that the field is of primitive type (<code>int</code> etc.),
     * the return value is of the appropriate Java wrapper type (<code>Integer</code> etc.).
     * In the case that the field contains an object then
     * an appropriate BObject will be returned.
     *
     * The main reason that this method is on a field (derived from a class),
     * rather than directly on an object, is to allow for the retrieval of
     * static field values without having to create an object of the appropriate type.
     *
     * As in the Relection API, in order to get the value of a static field pass
     * null as the parameter to this method.
     *
     * @param  onThis                     Description of the Parameter
     * @return                            The value value
     * @throws  ProjectNotOpenException   if the project to which the field belongs has been closed by the user.
     * @throws  PackageNotFoundException  if the package to which the field belongs has been deleted by the user.
     */
    public Object getValue(BObject onThis)
             throws ProjectNotOpenException, PackageNotFoundException
    {
        // If someone gives me a null it means that he wants a static field
        if (onThis == null) {
            return getStaticField();
        }

        ObjectReference objRef = onThis.getObjectReference();

        ReferenceType type = objRef.referenceType();

        Field thisField = type.fieldByName(bluej_view.getName());
        if (thisField == null) {
            return null;
        }

        PkgMgrFrame aFrame = onThis.getPackageFrame();
        return doGetVal(aFrame, bluej_view.getName(), objRef.getValue(thisField));
    }


    /**
     * Given a Value that comes from the remote debugger machine, converts it into somethig
     * that is usable. The real important thing here is to return a BObject for objects
     * that can be put into the bench.
     *
     * @param  packageFrame  Description of the Parameter
     * @param  instanceName  Description of the Parameter
     * @param  val           Description of the Parameter
     * @return               Description of the Return Value
     */
    static Object doGetVal(PkgMgrFrame packageFrame, String instanceName, Value val)
    {
        if (val == null) {
            return null;
        }

        if (val instanceof StringReference) {
            return ((StringReference) val).value();
        }
        if (val instanceof BooleanValue) {
            return new Boolean(((BooleanValue) val).value());
        }
        if (val instanceof ByteValue) {
            return new Byte(((ByteValue) val).value());
        }
        if (val instanceof CharValue) {
            return new Character(((CharValue) val).value());
        }
        if (val instanceof DoubleValue) {
            return new Double(((DoubleValue) val).value());
        }
        if (val instanceof FloatValue) {
            return new Float(((FloatValue) val).value());
        }
        if (val instanceof IntegerValue) {
            return new Integer(((IntegerValue) val).value());
        }
        if (val instanceof LongValue) {
            return new Long(((LongValue) val).value());
        }
        if (val instanceof ShortValue) {
            return new Short(((ShortValue) val).value());
        }

        if (val instanceof ObjectReference) {
            JdiObject obj = JdiObject.getDebuggerObject((ObjectReference) val);
            ObjectWrapper objWrap = ObjectWrapper.getWrapper(packageFrame, packageFrame.getObjectBench(), obj, obj.getGenType(), instanceName);
            return objWrap.getBObject();
        }

        return val.toString();
    }
}
