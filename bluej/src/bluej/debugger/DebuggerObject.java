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
package bluej.debugger;

import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeClass;

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
     *  Get the fully qualified name of the class of this object.
     *
     * @return  the fully qualified class name
     */
    public abstract String getClassName();
    
    /**
     *  Get the name of the class of this object, including generic parameters
     * 
     * @return  the fully qualified class name
     */
    public abstract String getGenClassName();

    /**
     *  Get the name of the class, including generic parameters, with package
     *  prefixes stripped in both the raw name and parameter names.
     * 
     * @return  the stripped class name
     */
    public abstract String getStrippedGenClassName();
    
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
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public abstract int getStaticFieldCount();

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    public abstract int getInstanceFieldCount();

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public abstract String getStaticFieldName(int slot);

    /**
     *  Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public abstract String getInstanceFieldName(int slot);

    /**
     * Return the type of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the field
     */
    public abstract String getInstanceFieldType(int slot);
    
    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The StaticFieldObject value
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     *  Return the object in object field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public abstract DebuggerObject getInstanceFieldObject(int slot);

    /**
     * Return the object, about which some static type information is known,
     * in object field 'slot'.
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return   The value in the field, as a DebuggerObject.
     */
    public abstract DebuggerObject getInstanceFieldObject(int slot, JavaType expectedType);
    
    /**
     *  Return the object in field 'slot' (counting static and object fields).
     *
     *@param  slot  The slot number to be returned
     *@return       The FieldObject value
     */
    public abstract DebuggerObject getFieldObject(int slot);

    /**
     * Return the object, about which some static type information is known,
     * in the field 'slot' (counting static and instance fields).
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return              The field object value (as a DebuggerObject)
     */
    public abstract DebuggerObject getFieldObject(int slot, JavaType expectedType);

    /**
     *  Return the object in field 'slot' (counting static and object fields).
     *
     *@param  name  Description of Parameter
     *@return       The FieldObject value
     */
    public abstract DebuggerObject getFieldObject(String name);

    /**
     * Returns the a string representation of the value in 'slot' (counting static and object fields).
     * @param slot The slot number to be returned
     * @return     The string representation of this value
     */
    public abstract String getFieldValueString(int slot);
    
    /**
     * Returns the a string representation of the type of the value in 'slot' (counting static and object fields).
     * @param slot The slot number to be returned
     * @return     The string representation of this value
     */
    public abstract String getFieldValueTypeString(int slot);
    
    
    /**
     *  Return the jdi object. This exposes the jdi to Inspectors.
     *  If jdi is not being used, it should return null, which is
     *  the default implementation.
     *
     *@return    The ObjectReference value
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
     * A single-item form of getInstanceFields that only gets a single field.
     * All parameters are as in getInstanceFields.  In general, it should be the case that
     * this code:
     * 
     * List<String> list = new LinkedList<String>();
     * for (int i = 0; i < getInstanceFieldCount();i++)
     *     list.add(getInstanceField(i, includeModifiers));
     *     
     * Should produce the same list as:
     * 
     * List<String> list = getInstanceFields(includeModifiers, null);
     * 
     * Array objects should override this to provide a more efficient implementation.
     */
    public String getInstanceField(int slot, boolean includeModifiers)
    {
        return getInstanceFields(includeModifiers).get(slot);
    }
    
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
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsPublic(int slot);


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsObject(int slot);

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsObject(int slot);

    /**
     *  Return true if the field 'slot' is an object (and not
     *  a simple type). Includes static and instance fields.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean fieldIsObject(int slot);
}
