package greenfoot.localdebugger;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenType;
import bluej.debugger.gentype.GenTypeClass;
import bluej.utility.JavaReflective;

import com.sun.jdi.ObjectReference;

/**
 * A class to represent a local object as a DebuggerObject
 *  
 * @author Davin McCall
 * @version $Id: LocalObject.java 3218 2004-12-06 03:43:52Z davmac $
 */
public class LocalObject extends DebuggerObject
{
    private Object object;
    private static Field [] noFields = new Field[0]; 
    
    public LocalObject(Object o)
    {
        object = o;
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
        // TODO support generics?
        return object.getClass().getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getStrippedGenClassName()
     */
    public String getStrippedGenClassName()
    {
        // TODO support generics?
        return object.getClass().getName();
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
        // TODO support generics?
        return new GenTypeClass(new JavaReflective(object.getClass()));
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getGenericParams()
     */
    public Map getGenericParams()
    {
        // TODO support generics?
        return null;
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
    public DebuggerObject getInstanceFieldObject(int slot, GenType expectedType)
    {
        // TODO generics support
        return getInstanceFieldObject(slot);
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
    public DebuggerObject getFieldObject(int slot, GenType expectedType)
    {
        // TODO generics support
        return getFieldObject(slot);
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
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getFieldValueTypeString(int)
     */
    public String getFieldValueTypeString(int slot)
    {
        // TODO Auto-generated method stub
        return null;
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
     * @see bluej.debugger.DebuggerObject#getAllFields(boolean)
     */
    public List getAllFields(boolean includeModifiers)
    {
        // TODO Auto-generated method stub
        return null;
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
}
