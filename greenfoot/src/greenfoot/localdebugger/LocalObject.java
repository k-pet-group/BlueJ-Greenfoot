/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.localdebugger;

import greenfoot.util.DebugUtil;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaReflective;

import com.sun.jdi.ObjectReference;

/**
 * A class to represent a local object as a DebuggerObject
 *  
 * @author Davin McCall
 */
public class LocalObject extends DebuggerObject
{
    // static fields
    private static Field [] noFields = new Field[0];
    
    // instance fields
    protected Object object;
    private Map<String,GenTypeParameter> genericParams = null; // Map of parameter names to types 
    
    public static LocalObject getLocalObject(Object o)
    {
        if (o != null && o.getClass().isArray()) {
            if (o instanceof boolean[]) {
                return new LocalBooleanArray((boolean []) o);
            }
            else if (o instanceof byte[]) {
                return new LocalByteArray((byte []) o);
            }
            else if (o instanceof char[]) {
                return new LocalCharArray((char []) o);
            }
            else if (o instanceof int[]) {
                return new LocalIntArray((int []) o);
            }
            else if (o instanceof long[]) {
                return new LocalLongArray((long []) o);
            }
            else if (o instanceof short[]) {
                return new LocalShortArray((short []) o);
            }
            else if (o instanceof float[]) {
                return new LocalFloatArray((float []) o);
            }
            else if (o instanceof double[]) {
                return new LocalDoubleArray((double []) o);
            }
            
            return new LocalArray((Object []) o);
        }
        else {
            return new LocalObject(o);
        }
    }
    
    public static LocalObject getLocalObject(Object o, Map<String,GenTypeParameter> genericParams)
    {
        if (o != null && o.getClass().isArray()) {
            if (o instanceof boolean[]) {
                return new LocalBooleanArray((boolean []) o);
            }
            else if (o instanceof byte[]) {
                return new LocalByteArray((byte []) o);
            }
            else if (o instanceof char[]) {
                return new LocalCharArray((char []) o);
            }
            else if (o instanceof int[]) {
                return new LocalIntArray((int []) o);
            }
            else if (o instanceof long[]) {
                return new LocalLongArray((long []) o);
            }
            else if (o instanceof short[]) {
                return new LocalShortArray((short []) o);
            }
            else if (o instanceof float[]) {
                return new LocalFloatArray((float []) o);
            }
            else if (o instanceof double[]) {
                return new LocalDoubleArray((double []) o);
            }
            
            // TODO generic arrays
            return new LocalArray((Object []) o);
        }
        else {
            return new LocalObject(o, genericParams);
        }
    }
    
    /**
     * Construct a LocalObject to represent a local object as a DebuggerObject.
     * @param o  The local object to represent
     */
    protected LocalObject(Object o)
    {
        object = o;
    }
    
    /**
     * Construct a LocalObject of generic type
     * @param o   The local object to represent
     * @param genericParams  The mapping of type parameter names to types
     *                       (String to GenType).
     */
    protected LocalObject(Object o, Map<String,GenTypeParameter> genericParams)
    {
        object = o;
        this.genericParams = genericParams;
    }
    
    // hash and equality defined in terms of the underlying object
    
    @Override
    public int hashCode()
    {
        return object.hashCode();
    }
    
    @Override
    public boolean equals(Object other)
    {
        if (other instanceof LocalObject) {
            Object otherObj = ((LocalObject) other).object;
            return object.equals(otherObj);
        }
        return false;
    }
    
    
    /*
     * @see bluej.debugger.DebuggerObject#getClassName()
     */
    @Override
    public String getClassName()
    {
        if (object != null) {
            return object.getClass().getName();
        }
        else {
            return "";
        }
    }

    /*
     * @see bluej.debugger.DebuggerObject#getClassRef()
     */
    @Override
    public DebuggerClass getClassRef()
    {
        return new LocalClass(object.getClass());
    }

    /*
     * @see bluej.debugger.DebuggerObject#getGenType()
     */
    @Override
    public GenTypeClass getGenType()
    {
        Reflective r = new JavaReflective(object.getClass());
        if(genericParams != null)
            return new GenTypeClass(r, genericParams);
        else
            return new GenTypeClass(r);
    }

    /*
     * @see bluej.debugger.DebuggerObject#isArray()
     */
    @Override
    public boolean isArray()
    {
        return object.getClass().isArray();
    }

    /*
     * @see bluej.debugger.DebuggerObject#isNullObject()
     */
    @Override
    public boolean isNullObject()
    {
        return object == null;
    }
    
    @Override
    public int getElementCount()
    {
        return -1;
    }
    
    @Override
    public DebuggerObject getElementObject(int index)
    {
        return null;
    }
    
    @Override
    public JavaType getElementType()
    {
        return null;
    }
    
    @Override
    public String getElementValueString(int index)
    {
        return null;
    }
    
    @Override
    public List<DebuggerField> getFields()
    {
        Field [] fields = getAllFields();
        Set<String> usedNames = new HashSet<String>();
        List<DebuggerField> rlist = new ArrayList<DebuggerField>(fields.length);
        
        for (Field field : fields) {
            boolean visible = usedNames.add(field.getName());
            rlist.add(new LocalField(this, field, !visible));
        }
        
        return rlist;
    }

    /**
     * Convenience method to get all fields, instance and static, public
     * and private. Fields are returned in order - first those declared in this
     * class, then the superclass, and so on.
     */
    private Field [] getAllFields()
    {
        if (object == null) {
            return noFields;
        }
        
        ArrayList<Field> allFields = new ArrayList<Field>();
        Class<?> c = object.getClass();
        
        while (c != null) {
            Field [] declFields = c.getDeclaredFields();
            AccessibleObject.setAccessible(declFields, true);
            
            for (int j = 0; j < declFields.length; j++) {
                Field field = declFields[j];
                // Filter out some fields that we want to hide.
                if(keepField(c, field)) {
                    allFields.add(field);
                }
            }
            
            c = c.getSuperclass();
        }

        return allFields.toArray(noFields);
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerObject#getObjectReference()
     */
    @Override
    public ObjectReference getObjectReference()
    {
        // No, this implementation is not Jdi related!
        return null;
    }

    /**
     * Whether a given field should be used.
     * @return True if the field should be used, false if it should be ignored
     */
    private boolean keepField(Class<?> cls, Field field) 
    {
        List<String> fieldWhitelist = DebugUtil.restrictedClasses().get(cls);
        
        if (fieldWhitelist != null) {
            return fieldWhitelist.contains(field.getName());
        } else {
            return true;
        }            
    }

    /**
     * Returns the object that this LocalObject represents.
     */
    public Object getObject()
    {
        return object;
    }
}
