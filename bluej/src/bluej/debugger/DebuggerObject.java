/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2018,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import threadchecker.OnThread;
import threadchecker.Tag;

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
     * If this object is the "null object", return an empty string.
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
    @OnThread(Tag.FXPlatform)
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
     * Subclasses should implement equals and hashCode.
     */
    public abstract int hashCode();

    /**
     * Subclasses should implement equals and hashCode.
     */
    public abstract boolean equals(Object obj);
}
