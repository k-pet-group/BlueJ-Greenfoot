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
package bluej.debugger;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;

/**
 * A class representing an object, and its type, in the debugged VM. The "null" value
 * can also be represented.
 *
 * @author     Michael Kolling
 */
public abstract class DebuggerObject
{
    public static final String OBJECT_REFERENCE = "<object reference>";
    
    /**
     * Get the fully qualified name of the class of this object.
     *
     * @return  the fully qualified class name
     */
    public abstract String getClassName();
    
    /**
     *  Get the class of this object.
     *
     *  @return    The class object.
     */
    public abstract DebuggerClass getClassRef();
    
    /**
     * Get the complete generic type of this object, if known. If not known, the raw
     * dynamic type of the object is returned, or null if this is the null object.
     * 
     * @return    The object type (or null for the null object).
     */
    public abstract GenTypeClass getGenType();

    /**
     *  Return true if this object is an array.
     *
     *@return    The Array value
     */
    public abstract boolean isArray();

    /**
     * Return true if this object has a null value
     */
    public abstract boolean isNullObject();

    /**
     * Get all field/value pairs for the object.
     */
    public abstract List<DebuggerField> getFields();
    
    /**
     * Get a field/value pair, specified by index. 
     */
    public DebuggerField getField(int slot)
    {
        return getFields().get(slot);
    }
    
    /**
     * Get an instance field/value pair, specified by index.
     */
    public DebuggerField getInstanceField(int slot)
    {
        for (DebuggerField field : getFields()) {
            if (! Modifier.isStatic(field.getModifiers())) {
                if (slot == 0) {
                    return field;
                }
                slot--;
            }
        }
        return null;
    }
    
    /**
     * Return the number of array elements. Returns -1 if the object is not an array.
     */
    public abstract int getElementCount();

    /**
     * Return the array element type. Returns null if the object is not an array.
     */
    public abstract JavaType getElementType();
    
    /**
     * Return the array element object for the specified index.
     */
    public abstract DebuggerObject getElementObject(int index);
    
    /**
     * Return a string representation of the array element at the specified index.
     * For null, the string "null" will be returned.
     * For a primitive, a string representation of the value will be returned.
     * For a string, the return will be a quoted Java literal string expression.
     * For any other reference type, the return will be DebuggerObject.OBJECT_REFERENCE.
     */
    public abstract String getElementValueString(int index);

    /**
     * Return the JDI object. This exposes the JDI to Inspectors.
     * If JDI is not being used, it should return null.
     *
     * @return    The ObjectReference value
     */
    public abstract com.sun.jdi.ObjectReference getObjectReference();

    /**
     * Return a list of strings with the description of each instance field
     * in the format "&lt;modifier&gt; &lt;type&gt; &lt;name&gt; [(hidden)] =
     * &lt;value&gt;" or "&lt;type&gt; &lt;name&gt; = &lt;value&gt;", depending
     * on the parameter.<p>
     *
     * "&lt;modifier&gt;" if present is "public", "private", or "protected".
     * Modifiers such as "final"/"volatile" are not included.<p>
     * 
     * "&lt;type&gt;" is the simple name of the type (i.e. it is not fully
     * qualified).<p>
     *
     * "(hidden)" means that the field is declared in a superclass of the
     * object class and shadowed by a field with the same name declared in
     * a descendant class.
     * 
     * <p>Values are represented differently depending on their type:
     * <ul>
     * <li>A String value is represented as a valid Java string expression.
     * <li>A null value is represented as "null".
     * <li>An Enum value is represented as the name of the Enum constant.
     * <li>Any other object reference is represented as "&lt;object reference&gt;".
     * <li>A primitive value is represented as the value itself.
     * </ul>
     *
     * @param  includeModifiers  Whether to include the access modifier
     * @param restrictedClasses  a map of class names for which the field should be filtered;
     *                           the class name maps to list of fields which should not be
     *                           filtered (i.e. a whitelist). 
     */
    public abstract List<String> getInstanceFields(boolean includeModifiers, Map<String, List<String>> restrictedClasses);
    
    /**
     * Get a list of the instance fields of this object.
     * @param includeModifiers  Whether to include modifiers ("private" etc).
     * @see #getInstanceFields(boolean, java.util.Map)
     */
    public final List<String> getInstanceFields(boolean includeModifiers)
    {
        return getInstanceFields(includeModifiers, null);
    }

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsPublic(int slot);

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsObject(int slot);
}
