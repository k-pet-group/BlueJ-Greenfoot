/*
 This file is part of the BlueJ program. 
 Copyright (C) 2000-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeArrayClass;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaNames;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;

/**
 * Represents an array object running on the user (remote) machine.
 *
 * @author     Michael Kolling
 * @created    December 26, 2000
 */
public class JdiArray extends JdiObject
{    
    private JavaType componentType; 
    
    protected JdiArray(ArrayReference obj)
    {
        this.obj = obj;
        obj.disableCollection();
    }

    /**
     * Constructor for when the array type is known.
     * @param obj           The reference to the the remote object
     * @param expectedType  The known type of the object
     */
    protected JdiArray(ArrayReference obj, JavaType expectedType)
    {
        this.obj = obj;
        obj.disableCollection();
        // All arrays extend java.lang.Object - so it's possible that the
        // expected type is java.lang.Object and not an array type at all.
        if(expectedType instanceof GenTypeArray) {
            String ctypestr = obj.referenceType().signature();
            JavaType genericType = expectedType;
            int level = 0;
            
            // Go downwards until we find the base component type
            while(genericType instanceof GenTypeArray) {
                GenTypeArray genericArray = (GenTypeArray)genericType;
                genericType = genericArray.getArrayComponent();
                ctypestr = ctypestr.substring(1);
                level++;
            }
            
            // If the arrays are of different depths, no inference is possible
            // (this is possible because all arrays extend Object)
            if(ctypestr.charAt(0) == '[')
                return;

            // The array may be of a primitive type.
            if(genericType.isPrimitive())
                return;

            // It's not really possible for an array to have a component type
            // that is a wildcard, but this type is inferred in some cases so
            // it must be handled here.
            
            JavaType component;
            
            if (genericType instanceof GenTypeClass) {
                // the sig looks like "Lpackage/package/class;". Strip the 'L'
                // and the ';'
                String compName = ctypestr.substring(1, ctypestr.length() - 1);
                compName = compName.replace('/', '.');

                Reflective compReflective = new JdiReflective(compName, obj.referenceType());
                component = ((GenTypeClass) genericType).mapToDerived(compReflective);

                while (level > 1) {
                    component = new GenTypeArray(component);
                    level--;
                }
                componentType = component;
            }
        }            
    }

    /**
     * Get the name of the class of this object.
     * 
     * @return String representing the Class name.
     */
    public String getClassName()
    {
        return obj.referenceType().name();
    }
    
    public String getGenClassName()
    {
        if(componentType == null) {
            return getClassName();
        }
        return componentType.toString() + "[]";
    }
    
    public String getStrippedGenClassName()
    {
        if(componentType == null) {
            return JavaNames.stripPrefix(getClassName());
        }
        return componentType.toString(true) + "[]";
    }

    /**
     *  Get the GenType object representing the type of this array.
     * 
     * @return   GenType representing the type of the array.
     */
    public GenTypeClass getGenType()
    {
        if(componentType != null) {
            Reflective r = new JdiArrayReflective(componentType, obj.referenceType());
            return new GenTypeArrayClass(r, componentType);
        }
        else {
            return super.getGenType();
        }
    }
    
    /**
     * Return true if this object is an array.
     *
     * @return    The Array value
     */
    public boolean isArray()
    {
        return true;
    }

    public boolean isNullObject()
    {
        return obj == null;
    }

    /**
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return 0;
    }

    /*
     * @see bluej.debugger.jdi.JdiObject#getInstanceFieldCount()
     */
    public int getInstanceFieldCount()
    {
        return ((ArrayReference) obj).length();
    }

    @Override
    public List<String> getInstanceFields(boolean includeModifiers,
            Map<String, List<String>> restrictedClasses)
    {
        ArrayReference array = (ArrayReference) obj;
        int len = array.length();
        List<String> r = new ArrayList<String>(len);
        for (int i = 0; i < len; i++) {
            String field = getInstanceFieldName(i) + " = "
                    + JdiUtils.getJdiUtils().getValueString(array.getValue(i));
            r.add(field);
        }
        return r;
    }
    
    @Override
    public String getInstanceField(int slot, boolean includeModifiers)
    {
        ArrayReference array = (ArrayReference) obj;
        String field = getInstanceFieldName(slot) + " = "
            + JdiUtils.getJdiUtils().getValueString(array.getValue(slot));
        return field;
    }
    
    
    
    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldName");
    }

    /**
     * Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public String getInstanceFieldName(int slot)
    {
        return "[" + String.valueOf(slot) + "]";
    }
    
    /**
     * Return the type of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the field
     */
    @Override
    public String getInstanceFieldType(int slot)
    {            
        String arrayType = null;
        if(componentType == null) {
            arrayType = getClassName();
        }
        else {
            arrayType = componentType.toString(false);
        }

        return JavaNames.getArrayElementType(arrayType);
    }

    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot or null if slot does not exist
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     *  Return the object in object field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Value val = ((ArrayReference) obj).getValue(slot);
        if(componentType != null)
            return JdiObject.getDebuggerObject((ObjectReference) val, componentType);
        else
            return JdiObject.getDebuggerObject((ObjectReference) val);
    }


    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List<String> getStaticFields(boolean includeModifiers)
    {
        throw new UnsupportedOperationException("getStaticFields");
        //        return new ArrayList(0);
    }

    /**
     *  Return an array of strings with the description of each field in the
     *  format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The InstanceFields value
     */
    public List<String> getInstanceFields(boolean includeModifiers, List<String> ignoreFieldsFrom)
    {
        List<Value> values;

        if (((ArrayReference) obj).length() > 0) {
            values = ((ArrayReference) obj).getValues();
        } else {
            values = new ArrayList<Value>();
        }
        List<String> fields = new ArrayList<String>(values.size());

        for (int i = 0; i < values.size(); i++) {
            Value val = (Value) values.get(i);
            String valString = JdiUtils.getJdiUtils().getValueString(val);
            fields.add("[" + i + "]" + " = " + valString);
        }
        return fields;
    }

    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  Description of Parameter
     *@return       Description of the Returned Value
     *@arg          slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        return true;
    }


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsObject(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
    }

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param   slot    The slot number to be checked
     * @return          true if the object in slot is an object
     */
    public boolean instanceFieldIsObject(int slot)
    {
        Value val = ((ArrayReference) obj).getValue(slot);

        if (val == null) {
            return false;
        }
        else {
            return (val instanceof ObjectReference);
        }
    }
}
