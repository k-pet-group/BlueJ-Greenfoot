package greenfoot.localdebugger;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaNames;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

import com.sun.jdi.ObjectReference;

/**
 * A class to represent a local object as a DebuggerObject
 *  
 * @author Davin McCall
 * @version $Id: LocalObject.java 4740 2006-12-05 16:34:14Z polle $
 */
public class LocalObject extends DebuggerObject
{
    // static fields
    private static Field [] noFields = new Field[0];
    
    // instance fields
    private Object object;
    private Map genericParams = null; // Map of parameter names to types
    
    /**
     * Construct a LocalObject to represent a local object as a DebuggerObject.
     * @param o  The local object to represent
     */
    public LocalObject(Object o)
    {
        object = o;
    }
    
    /**
     * Construct a LocalObject of generic type
     * @param o   The local object to represent
     * @param genericParams  The mapping of type parameter names to types
     *                       (String to GenType).
     */
    public LocalObject(Object o, Map genericParams)
    {
        object = o;
        this.genericParams = genericParams;
    }
    
    // hash and equality defined in terms of the underlying object
    
    public int hashCode()
    {
        return object.hashCode();
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof LocalObject) {
            Object otherObj = ((LocalObject) other).object;
            return object.equals(otherObj);
        }
        return false;
    }
    
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getClassName()
     */
    public String getClassName()
    {
        return object.getClass().getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getGenClassName()
     */
    public String getGenClassName()
    {
        if (object == null)
            return "";
        if(genericParams != null)
            return new GenTypeClass(new JavaReflective(object.getClass()),
                    genericParams).toString();
        else
            return getClassName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStrippedGenClassName()
     */
    public String getStrippedGenClassName()
    {
        if(object == null)
            return "";
        if(genericParams != null)
            return new GenTypeClass(new JavaReflective(object.getClass()),
                    genericParams).toString(true);
        else
            return JavaNames.stripPrefix(getClassName());
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getClassRef()
     */
    public DebuggerClass getClassRef()
    {
        return new LocalClass(object.getClass());
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getGenType()
     */
    public GenTypeClass getGenType()
    {
        Reflective r = new JavaReflective(object.getClass());
        if(genericParams != null)
            return new GenTypeClass(r, genericParams);
        else
            return new GenTypeClass(r);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getGenericParams()
     */
    public Map getGenericParams()
    {
        Map r = null;
        if( genericParams != null ) {
            r = new HashMap();
            r.putAll(genericParams);
        }
        else if (! isRaw())
            r = new HashMap();
        return r;
    }

    /**
     * Determine whether this is a raw object. That is, an object of a class
     * which has formal type parameters, but for which no actual types have
     * been given.
     * @return  true if the object is raw, otherwise false.
     */
    private boolean isRaw()
    {
        if ((! JavaUtils.getJavaUtils().getTypeParams(object.getClass()).isEmpty()) && genericParams == null)
            return true;
        else
            return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#isArray()
     */
    public boolean isArray()
    {
        return object.getClass().isArray();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#isNullObject()
     */
    public boolean isNullObject()
    {
        return object == null;
    }

    /**
     * Convenience method to get all fields, instance and static, public
     * and private.
     */
    private Field [] getAllFields()
    {
        if (object == null)
            return noFields;
        
        ArrayList allFields = new ArrayList();
        Class c = object.getClass();
        
        while (c != null) {
            Field [] declFields = c.getDeclaredFields();
            AccessibleObject.setAccessible(declFields, true);
            allFields.addAll(Arrays.asList(declFields));
            c = c.getSuperclass();
        }

        return (Field []) allFields.toArray(noFields);
    }
    
    /**
     * Convenience method to get a reference to a static field by its slot
     * number
     * 
     * @param slot   The slot number of the field to retrieve
     * @return       The requested Field
     */
    private Field getStaticFieldSlot(int slot)
    {
        Field [] fields = getAllFields();
        int staticCount = -1;
        int index = 0;
        while (staticCount != slot) {
            if ((fields[index].getModifiers() & Modifier.STATIC) != 0)
                staticCount++;
            index++;
        }
        
        return fields[index - 1];
    }
    
    /**
     * Convenience method to get a reference to an instance field by its slot
     * number
     * 
     * @param slot   The slot number of the field to retrieve
     * @return       The requested Field
     */
    private Field getInstanceFieldSlot(int slot)
    {
        Field [] fields = getAllFields();
        int instanceCount = -1;
        int index = 0;
        while (instanceCount != slot) {
            if ((fields[index].getModifiers() & Modifier.STATIC) == 0)
                instanceCount++;
            index++;
        }
        
        return fields[index - 1];
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStaticFieldCount()
     */
    public int getStaticFieldCount()
    {
        Field [] fields = getAllFields();
        int staticCount = 0;
        for (int i = 0; i < fields.length; i++) {
            if ((fields[i].getModifiers() & Modifier.STATIC) != 0)
                staticCount++;
        }
        
        return staticCount;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getInstanceFieldCount()
     */
    public int getInstanceFieldCount()
    {
        Field [] fields = getAllFields();
        int instanceCount = 0;
        for (int i = 0; i < fields.length; i++) {
            if ((fields[i].getModifiers() & Modifier.STATIC) == 0)
                instanceCount++;
        }
        
        return instanceCount;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStaticFieldName(int)
     */
    public String getStaticFieldName(int slot)
    {
        return getStaticFieldSlot(slot).getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getInstanceFieldName(int)
     */
    public String getInstanceFieldName(int slot)
    {
        return getInstanceFieldSlot(slot).getName();
    }

    /**
     * Get an object with expected type from a field. This is used when the
     * type of the field is known to a greater extent than is represented by
     * the static type of the field.
     * 
     * @param field         The field
     * @param expectedType  The expected tyoe
     * @return a DebuggerObject representing the value and type of the field
     */
    private LocalObject getFieldObject(Field field, JavaType expectedType)
    {
        GenTypeClass expectedCtype = expectedType.asClass();
        try {
            if (expectedCtype != null && !isRaw()) {
                Object o = field.get(object);
                if (o != null) { // The return value might be null
                    Class c = o.getClass();
                    if (genericParams != null)
                        expectedType.mapTparsToTypes(genericParams);
                    GenTypeClass g = expectedCtype.mapToDerived(new JavaReflective(c));
                    Map m = g.getMap();
                    return new LocalObject(o, m);
                }
            }

            // raw
            Object o = field.get(object);
            return new LocalObject(o);
        }
        catch (IllegalAccessException iae) {
            return null;
        }
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStaticFieldObject(int)
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getStaticFieldSlot(slot);
        try {
            return new LocalObject(field.get(object));
        }
        catch (IllegalAccessException iae) {}
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getInstanceFieldObject(int)
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Field field = getInstanceFieldSlot(slot);
        try {
            return new LocalObject(field.get(object));
        }
        catch (IllegalAccessException iae) {}
        return null;
    }
 
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getInstanceFieldObject(int, bluej.debugger.gentype.GenType)
     */
    public DebuggerObject getInstanceFieldObject(int slot, JavaType expectedType)
    {
        Field field = getInstanceFieldSlot(slot);
        return getFieldObject(field, expectedType);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldObject(int)
     */
    public DebuggerObject getFieldObject(int slot)
    {
        Field field = getAllFields()[slot];
        try {
            return new LocalObject(field.get(object));
        }
        catch (IllegalAccessException iae) {}
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldObject(int, bluej.debugger.gentype.GenType)
     */
    public DebuggerObject getFieldObject(int slot, JavaType expectedType)
    {
        Field field = getAllFields()[slot];
        return getFieldObject(field, expectedType);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldObject(java.lang.String)
     */
    public DebuggerObject getFieldObject(String name)
    {
        try {
            Field field = object.getClass().getField(name);
            return new LocalObject(field.get(object));
        }
        catch (Exception exc) {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldValueString(int)
     */
    public String getFieldValueString(int slot)
    {
        Field f = getAllFields()[slot];
        Object v = null;
        try {
            v = f.get(object);
            // Reference types are handled specially
            if (! f.getType().isPrimitive()) {
                if (v == null)
                    v = Config.getString("debugger.null");
                else
                    v = OBJECT_REFERENCE;
            }
        }
        catch (IllegalAccessException iae) {}
        return v.toString();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldValueTypeString(int)
     */
    public String getFieldValueTypeString(int slot)
    {
        Field f = getAllFields()[slot];
        JavaUtils.getJavaUtils().getFieldType(f);
        Class c = f.getType();
        
        String tname = "";
        while (c.isArray()) {
            tname += "[]";
            c = c.getComponentType();
        }
        return c.getName() + tname;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getObjectReference()
     */
    public ObjectReference getObjectReference()
    {
        // No, this implementation is not Jdi related!
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStaticFields(boolean)
     */
    public List getStaticFields(boolean includeModifiers)
    {
        if (object == null)
            return Collections.EMPTY_LIST;
        
        return getClassRef().getStaticFields(includeModifiers);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getInstanceFields(boolean)
     */
    public List getInstanceFields(boolean includeModifiers)
    {
        List r = new ArrayList();
        
        Field [] fields = getAllFields();
        for (int i = 0; i < fields.length; i++) {
            // skip non-instance fields
            int mods = fields[i].getModifiers();
            if ((mods & Modifier.STATIC) != 0)
                continue;
            
            String desc = "";
            if (includeModifiers) {
                desc = Modifier.toString(mods) + " ";
            }
            
            desc += fields[i].getName() + " = ";
            try {
                if (fields[i].getType().isPrimitive()) {
                    desc += fields[i].get(object);
                }
                else {
                    Object fieldval = fields[i].get(object);
                    if (fieldval instanceof String)
                        desc += '\"' + fieldval.toString() + '\"';
                    else if (fieldval == null)
                        desc += Config.getString("debugger.null");
                    else
                        desc += OBJECT_REFERENCE;
                }
            }
            catch (IllegalAccessException iae) {
                desc += "?";
            }
            
            r.add(desc);
        }
        
        return r;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#staticFieldIsPublic(int)
     */
    public boolean staticFieldIsPublic(int slot)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#instanceFieldIsPublic(int)
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#staticFieldIsObject(int)
     */
    public boolean staticFieldIsObject(int slot)
    {
        Field field = getStaticFieldSlot(slot);
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#instanceFieldIsObject(int)
     */
    public boolean instanceFieldIsObject(int slot)
    {
        Field field = getInstanceFieldSlot(slot);
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#fieldIsObject(int)
     */
    public boolean fieldIsObject(int slot)
    {
        Field field = getAllFields()[slot];
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    public boolean fieldNotNull(Field field)
    {
        try {
            Object v = field.get(object);
            return v != null;
        }
        catch (IllegalAccessException iae) { return false; }
    }

    public List getAllFields(boolean includeModifiers)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
