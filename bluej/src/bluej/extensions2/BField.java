/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2016,2018,2019  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.jdi.JdiObject;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.views.FieldView;
import bluej.views.View;
import com.sun.jdi.*;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

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
     * @param  aParentId the {@link Identifier} of the parent class.
     * @param  i_bluej_view the {@link FieldView} of this field.
     */
    BField(Identifier aParentId, FieldView i_bluej_view)
    {
        parentId = aParentId;
        bluej_view = i_bluej_view;
    }

    /**
     * Returns the name of the field.
     * Similar to reflection API.
     */
    public String getName()
    {
        return bluej_view.getName();
    }


    /**
     * Returns the type of the field.
     * Similar to Reflection API.
     *
     * @return A {@link java.lang.Class} object representing the type of this field.
     */
    public Class<?> getType()
    {
        return bluej_view.getType().getViewClass();
    }


    /**
     * Returns the java Field for inspection.
     * This method can be used when more information is needed about the Field than
     * is provided by the BField interface. E.g.:
     * What is the declaring class of this Field?
     *
     * Note that this is for information only. To interact with BlueJ, an extension must
     * use the methods provided in BField.
     * 
     * @return    The {@link java.lang.reflect.Field} providing extra information about this BField.
     */
    public java.lang.reflect.Field getJavaField()
    {
        return bluej_view.getField();
    }

    /**
     * Returns the modifier of this field. The
     * {@link java.lang.reflect.Modifier} class can be used to decode the
     * modifiers.
     *
     * @return An int value representing the modifiers which can be decoded with <code>java.lang.reflect.Modifier</code>.
     *
     */    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     * Returns the static field.
     *
     * @return An Object representing the static field wrapped by this BField, <code>null</code> if no static field can be retrieved.
     * @throws ProjectNotOpenException   if the project to which this field belongs has been closed by the user.
     * @throws PackageNotFoundException  if the package to which this field belongs has been deleted by the user.
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
            debuggerClass = bluejPkg.getDebugger().getClass(className, true).get();
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
     * Returns the value of this field for a given object.
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
     * As in the Reflection API, in order to get the value of a static field pass
     * null as the parameter to this method.
     *
     * @param  onThis the {@link BObject} object for which this field's value should be retrieved.
     * @return                           An <code>Object</code> object representing the field's value.
     * @throws ProjectNotOpenException   if the project to which the field belongs has been closed by the user.
     * @throws PackageNotFoundException  if the package to which the field belongs has been deleted by the user.
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
     * Given a Value that comes from the remote debugger machine, converts it into somethnig
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
            return Boolean.valueOf(((BooleanValue) val).value());
        }
        if (val instanceof ByteValue) {
            return Byte.valueOf(((ByteValue) val).value());
        }
        if (val instanceof CharValue) {
            return Character.valueOf(((CharValue) val).value());
        }
        if (val instanceof DoubleValue) {
            return Double.valueOf(((DoubleValue) val).value());
        }
        if (val instanceof FloatValue) {
            return Float.valueOf(((FloatValue) val).value());
        }
        if (val instanceof IntegerValue) {
            return Integer.valueOf(((IntegerValue) val).value());
        }
        if (val instanceof LongValue) {
            return Long.valueOf(((LongValue) val).value());
        }
        if (val instanceof ShortValue) {
            return Short.valueOf(((ShortValue) val).value());
        }

        if (val instanceof ObjectReference) {
            JdiObject obj = JdiObject.getDebuggerObject((ObjectReference) val);
            ObjectWrapper objWrap = ObjectWrapper.getWrapper(packageFrame, packageFrame.getObjectBench(), obj, obj.getGenType(), instanceName);
            return objWrap.getBObject();
        }

        return val.toString();
    }
}
