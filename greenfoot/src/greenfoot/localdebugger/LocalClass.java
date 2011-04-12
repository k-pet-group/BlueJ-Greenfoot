/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.Actor;
import greenfoot.World;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;

/**
 * Represent a local class as a DebuggerClass.
 * 
 * @author Davin McCall
 */
public class LocalClass extends DebuggerClass
{
    private Class<?> cl;
    private static Field [] noFields = new Field[0];
     
    /**
     * Constructor for LocalClass.
     */
    public LocalClass(Class<?> cl)
    {
        this.cl = cl;
    }
    
    /*
     * @see bluej.debugger.DebuggerClass#getName()
     */
    public String getName()
    {
        return cl.getName();
    }
    
    @Override
    public List<DebuggerField> getStaticFields()
    {
        Field [] fields = getFields();
        Set<String> usedNames = new HashSet<String>();
        List<DebuggerField> rlist = new ArrayList<DebuggerField>(fields.length);
        
        for (Field field : fields) {
            boolean visible = usedNames.add(field.getName());
            rlist.add(new LocalField(null, field, !visible));
        }
        
        return rlist;
    }
    
    /*
     * @see bluej.debugger.DebuggerClass#getStaticFieldCount()
     */
    public int getStaticFieldCount()
    {        
        return getFields().length;
    }

    /*
     * @see bluej.debugger.DebuggerClass#getStaticFieldName(int)
     */
    public String getStaticFieldName(int slot)
    {
        Field field = getFields()[slot];
        return field.getName();
    }

    /*
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

    /*
     * @see bluej.debugger.DebuggerClass#getStaticFields(boolean)
     */
    public List<String> getStaticFields(boolean includeModifiers, Map<String, List<String>> restrictedClasses)
    {
        List<String> r = new ArrayList<String>();
        
        Field [] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            // skip non-instance fields
            int mods = fields[i].getModifiers();
            
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
                    if (fieldval instanceof String) {
                        desc += '\"' + fieldval.toString() + '\"';
                    }
                    else if (fieldval == null) {
                        desc += "null";
                    }
                    else {
                        desc += DebuggerObject.OBJECT_REFERENCE;
                    }
                }
            }
            catch (IllegalAccessException iae) {
                desc += "?";
            }
            
            r.add(desc);
        }
        
        return r;
    }

    /*
     * @see bluej.debugger.DebuggerClass#staticFieldIsPublic(int)
     */
    public boolean staticFieldIsPublic(int slot)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * @see bluej.debugger.DebuggerClass#staticFieldIsObject(int)
     */
    public boolean staticFieldIsObject(int slot)
    {
        Field field = getFields()[slot];
        return ! field.getType().isPrimitive()
            && fieldNotNull(field);
    }

    /*
     * @see bluej.debugger.DebuggerClass#isInterface()
     */
    public boolean isInterface()
    {
        return cl.isInterface();
    }

    /*
     * @see bluej.debugger.DebuggerClass#isEnum()
     */
    public boolean isEnum()
    {
        // TODO support for enums
        return false;
    }

    /**
     * Convenience method to get static fields, public
     * and private. Except the ones that should be filtered out.
     */
    private Field [] getFields()
    {
        ArrayList<Field> allFields = new ArrayList<Field>();
        Class<?> c = cl;
        
        while (c != null) {
            Field [] declFields = c.getDeclaredFields();
            ArrayList<Field> sfields = new ArrayList<Field>();
            for (int i = 0; i < declFields.length; i++) {
                Field field = declFields[i];
                if ((field.getModifiers() & Modifier.STATIC) != 0 && keepField(c, field)) {
                    sfields.add(field);
                }
            }
            
            declFields = sfields.toArray(noFields);
            AccessibleObject.setAccessible(declFields, true);
            allFields.addAll(Arrays.asList(declFields));
            c = c.getSuperclass();

        }

        return allFields.toArray(noFields);
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

    /**
     * Whether a given field should be used.
     * @return True if the field should be used, false if it should be ignored
     */
    private boolean keepField(Class<?> cls, Field field) 
    {
        if(cls.equals(World.class)) {
            return false;
        }
        else if(cls.equals(Actor.class)) {
            return false;
        }
        return true;            
    }
}
