package greenfoot.localdebugger;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;

/**
 * Represent a local class as a DebuggerClass.
 * 
 * @author Davin McCall
 * @version $Id: LocalClass.java 4854 2007-03-20 01:16:35Z davmac $
 */
public class LocalClass extends DebuggerClass
{
    private Class cl;
    private static Field [] noFields = new Field[0];
    
    /**
     * Constructor for LocalClass.
     */
    public LocalClass(Class cl)
    {
        this.cl = cl;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getName()
     */
    public String getName()
    {
        return cl.getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldCount()
     */
    public int getStaticFieldCount()
    {
        Field [] fields = getFields();
        int staticCount = 0;
        for (int i = 0; i < fields.length; i++) {
            if ((fields[i].getModifiers() & Modifier.STATIC) != 0)
                staticCount++;
        }
        
        return staticCount;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldName(int)
     */
    public String getStaticFieldName(int slot)
    {
        Field field = getFields()[slot];
        return field.getName();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFieldObject(int)
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getFields()[slot];
        try {
            return LocalObject.getLocalObject(field.get(null));
        }
        catch (IllegalAccessException iae) {}
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#getStaticFields(boolean)
     */
    public List getStaticFields(boolean includeModifiers)
    {
        List r = new ArrayList();
        
        Field [] fields = getFields();
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
                    desc += fields[i].get(null);
                }
                else {
                    Object fieldval = fields[i].get(null);
                    if (fieldval instanceof String)
                        desc += '\"' + fieldval.toString() + '\"';
                    else if (fieldval == null)
                        desc += Config.getString("debugger.null");
                    else
                        desc += DebuggerObject.OBJECT_REFERENCE;
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
     * @see bluej.debugger.DebuggerClass#staticFieldIsPublic(int)
     */
    public boolean staticFieldIsPublic(int slot)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#staticFieldIsObject(int)
     */
    public boolean staticFieldIsObject(int slot)
    {
        Field field = getFields()[slot];
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#isInterface()
     */
    public boolean isInterface()
    {
        return cl.isInterface();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerClass#isEnum()
     */
    public boolean isEnum()
    {
        // TODO support for enums
        return false;
    }

    /**
     * Convenience method to get all fields, instance and static, public
     * and private.
     */
    private Field [] getFields()
    {
        ArrayList allFields = new ArrayList();
        Class c = cl;
        
        while (c != null) {
            Field [] declFields = c.getDeclaredFields();
            ArrayList sfields = new ArrayList();
            for (int i = 0; i < declFields.length; i++) {
                if ((declFields[i].getModifiers() & Modifier.STATIC) != 0)
                    sfields.add(declFields[i]);
            }
            
            declFields = (Field []) sfields.toArray(noFields);
            AccessibleObject.setAccessible(declFields, true);
            allFields.addAll(Arrays.asList(declFields));
            c = c.getSuperclass();
        }

        return (Field []) allFields.toArray(noFields);
    }
    
    /**
     * Check whether a field in this class contains a null reference.
     */
    public boolean fieldNotNull(Field field)
    {
        try {
            Object v = field.get(null);
            return v != null;
        }
        catch (IllegalAccessException iae) { return false; }
    }

}
