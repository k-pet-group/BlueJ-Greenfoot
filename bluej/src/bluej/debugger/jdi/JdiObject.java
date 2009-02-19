/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import java.util.*;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.*;

/**
 * Represents an object running on the user (remote) machine.
 *
 * @author  Michael Kolling
 * @version $Id: JdiObject.java 6163 2009-02-19 18:09:55Z polle $
 */
public class JdiObject extends DebuggerObject
{
    
    // boolean - true if our JVM supports generics
    static boolean jvmSupportsGenerics = Config.isJava15();
    
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
        // Optimize the java 1.4 case - no generics.
        if (! jvmSupportsGenerics)
            return getDebuggerObject(obj);
        
        // Handle all cases.
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
        obj.disableCollection();
        getRemoteFields();
    }

    private JdiObject(ObjectReference obj, GenTypeClass expectedType)
    {
        this.obj = obj;
        if (obj != null) {
            obj.disableCollection();
        }
        getRemoteFields();
        if( obj != null ) {
            Reflective reflective = new JdiReflective(obj.referenceType());
            if( expectedType.isGeneric() ) {
                genType = expectedType.mapToDerived(reflective);
            }
        }
    }
    
    protected void finalize()
    {
        obj.enableCollection();
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
     * Determine whether this is a raw object. That is, an object of a class
     * which has formal type parameters, but for which no actual types have
     * been given.
     * @return  true if the object is raw, otherwise false.
     */
 /*   private boolean isRaw()
    {
        if(JdiUtils.getJdiUtils().hasGenericSig(obj)) {
            if (genType == null)
                return true;
            else
                return genType.isRaw();
        }
        else
            return false;
    }*/

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
        if(genType != null)
            return genType;
        else {
            Reflective r = new JdiReflective(obj.referenceType());
            return new GenTypeClass(r);
        }
    }

    /**
     *  Is an object of this class assignable to the given fully qualified type?
     *
     *@param  type  Description of Parameter
     *@return       The AssignableTo value
     */
/*    public boolean isAssignableTo(String type)
    {
        if (obj == null) {
            return false;
        }
        if (obj.referenceType() == null) {
            return false;
        }
        if (obj.referenceType().name() != null
                 && type.equals(obj.referenceType().name())) {
            return true;
        }
        if ((obj.referenceType() instanceof ClassType))
        {
            ClassType clst = ((ClassType) obj.referenceType());
            InterfaceType[] intt = ((InterfaceType[]) clst.allInterfaces().toArray(new InterfaceType[0]));
            for (int i = 0; i < intt.length; i++)
            {
                if (type.equals(intt[i].name()))
                {
                    return true;
                }
            }
            clst = clst.superclass();
            while (clst != null)
            {
                if (clst.name().equals(type))
                {
                    return true;
                }
                clst = clst.superclass();
            }
        }
        else if ((obj.referenceType() instanceof ArrayType))
        {
        }
        return false;
    }
*/
    
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


    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List<String> getStaticFields(boolean includeModifiers)
    {
        return getFields(false, true, includeModifiers);
    }

    /**
     *  Return a list of strings with the description of each instance field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The InstanceFields value
     */
    public List<String> getInstanceFields(boolean includeModifiers)
    {
        return getFields(false, false, includeModifiers);
    }


    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The AllFields value
     */
    public List<String> getAllFields(boolean includeModifiers)
    {
        return getFields(true, true, includeModifiers);
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


    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     *  If 'getAll' is true, both static and instance fields are returned
     *  ('getStatic' is ignored). If 'getAll' is false, then 'getStatic'
     *  determines whether static fields or instance fields are returned.
     *
     *@param  getAll            If true, get static and instance fields
     *@param  getStatic         If 'getAll' is false, determine which fields to get
     *@param  includeModifiers  If true, include the modifier name (public, private)
     *@return                   The Fields value
     */
    private List<String> getFields(boolean getAll, boolean getStatic,
            boolean includeModifiers)
    {
        List<String> fieldStrings = new ArrayList<String>(fields.size());

        if (obj == null)
            return fieldStrings;
            
        ReferenceType cls = obj.referenceType();
        List<Field> visible = cls.visibleFields();

        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field) fields.get(i);

            if (checkIgnoreField(field))
                continue;

            if (getAll || (field.isStatic() == getStatic)) {
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

                if (jvmSupportsGenerics)
                    fieldString += JdiReflective.fromField(field, this).toString(true);
                else
                    fieldString += JavaNames.stripPrefix(field.typeName());

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
