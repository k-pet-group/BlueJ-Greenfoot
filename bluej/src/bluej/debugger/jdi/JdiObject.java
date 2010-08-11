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
package bluej.debugger.jdi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Value;

/**
 * Represents an object running on the user (remote) machine, together with an optional generic
 * type of the object.
 *
 * @author  Michael Kolling
 */
public class JdiObject extends DebuggerObject
{
    /**
     *  Factory method that returns instances of JdiObjects.
     *
     *  @param  obj  the remote object this encapsulates.
     *  @return      a new JdiObject or a new JdiArray object if
     *               remote object is an array
     */
    public static JdiObject getDebuggerObject(ObjectReference obj)
    {
        if (obj instanceof ArrayReference)
            return new JdiArray((ArrayReference) obj);
        else
            return new JdiObject(obj);
    }
    
    public static JdiObject getDebuggerObject(ObjectReference obj, JavaType expectedType)
    {
        if( obj instanceof ArrayReference )
            return new JdiArray((ArrayReference) obj, expectedType);
        else {
            if( expectedType instanceof GenTypeClass )
                return new JdiObject(obj, (GenTypeClass)expectedType);
            else
                return new JdiObject(obj);
        }
    }

    /**
     * Get a JdiObject from a field. 
     * @param obj    Represents the value of the field.
     * @param field  The field.
     * @param parent The parent object containing the field.
     * @return
     */
    public static JdiObject getDebuggerObject(ObjectReference obj, Field field, JdiObject parent)
    {
        JavaType expectedType = JdiReflective.fromField(field, parent);
        if (obj instanceof ArrayReference)
            return new JdiArray((ArrayReference) obj, expectedType);
        
        if (expectedType instanceof GenTypeClass)
            return new JdiObject(obj, (GenTypeClass) expectedType);
        
        return new JdiObject(obj);
    }
    
    
    // -- instance methods --

    ObjectReference obj;  // the remote object represented
    GenTypeClass genType = null; // the generic type, if known
    List<Field> fields;
    
    // used by JdiArray.
    protected JdiObject()
    {
    }

    /**
     *  Constructor is private so that instances need to use getJdiObject
     *  factory method.
     *
     *  @param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    private JdiObject(ObjectReference obj)
    {
        this.obj = obj;
        if (obj != null) {
            obj.disableCollection();
            getRemoteFields();
        }
    }

    private JdiObject(ObjectReference obj, GenTypeClass expectedType)
    {
        this.obj = obj;
        if (obj != null) {
            obj.disableCollection();
            getRemoteFields();
            Reflective reflective = new JdiReflective(obj.referenceType());
            if( expectedType.isGeneric() ) {
                genType = expectedType.mapToDerived(reflective);
            }
        }
    }
    
    protected void finalize()
    {
        if (obj != null) {
            obj.enableCollection();
        }
    }
    
    public String toString()
    {
        return JdiUtils.getJdiUtils().getValueString(obj);
    }
    
    /**
     *  Get the (raw) name of the class of this object.
     *
     *  @return    The ClassName value
     */
    public String getClassName()
    {
        if (obj == null)
            return "";
        else
            return obj.referenceType().name();
    }

    /**
     * Get the generic name of the class of the object. All names are fully
     * qualified
     *  (eg. java.util.List&lt;java.lang.Integer&gt;).
     * 
     *  @return    The generic class name
     */
    public String getGenClassName()
    {
        if (obj == null)
            return "";
        if(genType != null)
            return genType.toString();
        else
            return getClassName();
    }
    
    /**
     * Get the generic name of the class of the object. The base names of types
     * are returned. (eg. List&lt;Integer&gt;).
     */
    public String getStrippedGenClassName()
    {
        if(obj == null)
            return "";
        if(genType != null)
            return genType.toString(true);
        else
            return JavaNames.stripPrefix(getClassName());
    }

    /**
     *  Get the class of this object.
     *
     *  @return    The class object.
     */
    public DebuggerClass getClassRef()
    {
        if (obj == null)
            return null;
        else
            return new JdiClass(obj.referenceType());
    }
    
    public GenTypeClass getGenType()
    {
        if(genType != null) {
            return genType;
        }
        else if (obj != null) {
            Reflective r = new JdiReflective(obj.referenceType());
            return new GenTypeClass(r);
        }
        else {
            return null;
        }
    }
    
    /**
     *  Return true if this object is an array. This is always false, since
     *  arrays are wropped in the subclass "JdiArray".
     *
     *@return    The Array value
     */
    public boolean isArray()
    {
        return false;
    }

    public boolean isNullObject()
    {
        return obj == null;
    }

    /**
     *  Return the number of static fields (including inherited fields).
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return getFieldCount(true);
    }

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    public int getInstanceFieldCount()
    {
        return getFieldCount(false);
    }

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        return getField(true, slot).name();
    }

    /**
     *  Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public String getInstanceFieldName(int slot)
    {
        return getField(false, slot).name();
    }

    /**
     * Return the type of the instance field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the field
     */
    @Override
    public String getInstanceFieldType(int slot)
    {    
        return JdiReflective.fromField(getField(false, slot), this).toString(false);
    }

    /**
     *  Return the object in static field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getField(true, slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }

    /**
     *  Return the object in object field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Field field = getField(false, slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }
    
    /**
     * Return the object, about which some static type information is known,
     * in object field 'slot'.
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return   The value in the field, as a DebuggerObject.
     */
    public DebuggerObject getInstanceFieldObject(int slot, JavaType expectedType)
    {
        Field field = getField(false, slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, expectedType);
    }


    /**
     *  Return the object in field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       The FieldObject value
     */
    public DebuggerObject getFieldObject(int slot)
    {
        Field field = (Field) fields.get(slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }
    
    /**
     * Return the object, about which some static type information is known,
     * in the field 'slot'.
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return              The field object value (as a DebuggerObject)
     */
    public DebuggerObject getFieldObject(int slot, JavaType expectedType)
    {
        Field field = (Field) fields.get(slot);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, expectedType);
    }

    public DebuggerObject getFieldObject(String name)
    {
        Field field = obj.referenceType().fieldByName(name);
        ObjectReference val = (ObjectReference) obj.getValue(field);
        return getDebuggerObject(val, field, this);
    }
    
    public String getFieldValueString(int slot) 
    {
        Field field = (Field) fields.get(slot);
        Value val = obj.getValue(field);
        return JdiUtils.getJdiUtils().getValueString(val); 
    }
    
    public String getFieldValueTypeString(int slot) 
    {
        Field field = (Field) fields.get(slot);
        Value val = obj.getValue(field);
        return val.type().name();  
    }

    public ObjectReference getObjectReference()
    {
        return obj;
    }

    /*
     * @see bluej.debugger.DebuggerObject#getInstanceFields(boolean, java.util.Map)
     */
    public List<String> getInstanceFields(boolean includeModifiers, Map<String, List<String>> restrictedClasses)
    {
        List<String> fieldStrings = new ArrayList<String>(obj == null ? 0 : fields.size());
        
        if (obj == null)
            return fieldStrings;
            
        ReferenceType cls = obj.referenceType();
        List<Field> visible = cls.visibleFields();
        
        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);
        
            if (checkIgnoreField(field))
                continue;
            
            if (restrictedClasses != null) {
                List<String> fieldWhitelist = restrictedClasses.get(field.declaringType().name());
                if (fieldWhitelist != null && !fieldWhitelist.contains(field.name())) 
                    continue; // ignore this one
            }            
        
            if (field.isStatic() == false) {
                Value val = obj.getValue(field);
        
                String valString = JdiUtils.getJdiUtils().getValueString(val);
                String fieldString = "";
        
                if (includeModifiers) {
                    if (field.isPrivate()) {
                        fieldString = "private ";
                    }
                    if (field.isProtected()) {
                        fieldString = "protected ";
                    }
                    if (field.isPublic()) {
                        fieldString = "public ";
                    }
                }
        
                fieldString += JdiReflective.fromField(field, this).toString(true);
        
                if (!visible.contains(field)) {
                    fieldString += " (hidden)";
                }
                
                fieldString += " " + field.name() + " = " +valString;
                
                // the following code adds the word "inherited" to inherited
                // fields - currently unused
                //else if (!field.declaringType().equals(cls)) {
                //    fieldString += " (inherited)";
                //}
                fieldStrings.add(fieldString);
            }
        }
        return fieldStrings;
    }


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsPublic(int slot)
    {
        return getField(true, slot).isPublic();
    }

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        return getField(false, slot).isPublic();
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
        return checkFieldForObject(true, slot);
    }

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean instanceFieldIsObject(int slot)
    {
        return checkFieldForObject(false, slot);
    }

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean fieldIsObject(int slot)
    {
        Field field = fields.get(slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }

    private int getFieldCount(boolean getStatic)
    {
        int count = 0;

        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (field.isStatic() == getStatic) {
                count++;
            }
        }
        return count;
    }


    private Field getField(boolean getStatic, int slot)
    {
        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (field.isStatic() == getStatic) {
                if (slot == 0) {
                    return field;
                }
                else {
                    slot--;
                }
            }
        }
        Debug.reportError("invalid slot in remote object");
        return null;
    }

    private boolean checkIgnoreField(Field f)
    {
        if (f.name().indexOf('$') >= 0)
            return true;
        else
            return false;
    }

    /**
     *  Get the list of fields for this object.
     */
    protected void getRemoteFields()
    {
        if (obj != null) {
        ReferenceType cls = obj.referenceType();

            if (cls != null) {
                fields = cls.allFields();
                return;
            }
        }
        // either null object or unavailable fields
        // lets give them an empty list of fields
        fields = new ArrayList<Field>();
    }

    private boolean checkFieldForObject(boolean getStatic, int slot)
    {
        Field field = getField(getStatic, slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }  // list of fields of the object

    /**
     * Base our object equality on the object that we are referring
     * to in the remote VM.
     */
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if((o == null) || (o.getClass() != this.getClass()))
            return false;

        // object must be JdiObject at this point
        JdiObject test = (JdiObject)o;
        return this.obj.equals(test.obj);
    }
		
    /**
     * Base our hashcode on the hashcode of the object that we are
     * referring to in the remote VM.
     */
    public int hashCode()
    {
        return obj.hashCode();
    }
}
