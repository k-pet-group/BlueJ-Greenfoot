/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2011  Poul Henriksen and Michael Kolling 
 
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

import java.lang.reflect.Field;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerField;
import bluej.debugger.DebuggerObject;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Implementation of DebuggerField for local objects.
 * 
 * @author Davin McCall
 */
public class LocalField extends DebuggerField
{
    private boolean hidden;
    private Field field;
    private LocalObject parentObject;
    
    public LocalField(LocalObject parentObject, Field field, boolean hidden)
    {
        this.parentObject = parentObject;
        this.field = field;
        this.hidden = hidden;
    }
    
    @Override
    public DebuggerClass getDeclaringClass()
    {
        return new LocalClass(field.getDeclaringClass());
    }
    
    @Override
    public int getModifiers()
    {
        return field.getModifiers();
    }
    
    @Override
    public String getName()
    {
        return field.getName();
    }
    
    @Override
    public JavaType getType()
    {
        try {
            JavaType fieldType = JavaUtils.getJavaUtils().getFieldType(field);
            if (parentObject != null) {
                GenTypeClass parentType = parentObject.getGenType();
                parentType = parentType.mapToSuper(field.getDeclaringClass().getName());
                fieldType = fieldType.mapTparsToTypes(parentType.getMap()).getUpperBound();
            }
            return fieldType;
        }
        catch (ClassNotFoundException cnfe) {
            return new GenTypeClass(new JavaReflective(field.getType()));
        }
    }
    
    @Override
    public DebuggerObject getValueObject(JavaType expectedType)
    {
        try {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Object resultObj = parentObject == null ? field.get(null) : field.get(parentObject.object);
            field.setAccessible(accessible);
            
            // Get type parameters
            GenTypeClass fieldClass = getType().asClass();
            if (resultObj != null && fieldClass != null) {
                GenTypeClass objClass = fieldClass.mapToDerived(new JavaReflective(resultObj.getClass()));
                return LocalObject.getLocalObject(resultObj, objClass.getMap());
            }
            else {
                // Though it's invalid to call getValueObject when the field type isn't a reference
                // type, we may have a primitive array type, for which asClass() can return null.
                return LocalObject.getLocalObject(resultObj);
            }
        }
        catch (IllegalAccessException iae) {
            return null;
        }
    }
    
    @Override
    public String getValueString()
    {
        try {
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Object pObj = (parentObject == null) ? null : parentObject.object;
            Object resultObj = field.get(pObj);
            field.setAccessible(accessible);
            if (field.getType().isPrimitive()) {
                return resultObj.toString();
            }
            else {
                return valueStringForObject(resultObj);
            }
        }
        catch (IllegalAccessException iae) {
            return null;
        }
    }
    
    /**
     * Get the value string representation for some object reference.
     */
    public static String valueStringForObject(Object o)
    {
        if (o instanceof String) {
            return "\"" + JavaUtils.escapeString(o.toString()) + "\"";
        }
        if (o == null) {
            return "null";
        }
        return DebuggerObject.OBJECT_REFERENCE;
    }
    
    @Override
    public boolean isHidden()
    {
        return hidden;
    }
}
