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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bluej.debugger.DebuggerObject;
import bluej.utility.JavaNames;

/**
 * A DebuggerObject to represent arrays. This base class is for object arrays;
 * primitive array types are represented in seperate classes which derive from
 * this one.<p>
 * 
 * Subclasses should override:
 * <ul>
 * <li> getValueString(int)
 * <li> instanceFieldIsObject(int)
 * </ul>
 * 
 * @author Davin McCall
 */
public class LocalArray extends LocalObject
{
    private int length;
    
    protected LocalArray(Object [] object)
    {
        super(object);
        length = object.length;
    }
    
    /**
     * Subclasses use this constructor to specify the array length.
     * 
     * @param object  The array object
     * @param length  The array length
     */
    protected LocalArray(Object object, int length)
    {
        super(object);
        this.length = length;
    }
    
    public int getInstanceFieldCount()
    {
        return length;
    }

    @Override
    public boolean isArray()
    {
        return true;
    }
    
    public String getInstanceFieldName(int slot)
    {
        return "[" + String.valueOf(slot) + "]";
    }

    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Object val = ((Object []) object)[slot];
        return getLocalObject(val);
    }
    
    /**
     * Return the type of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The type of the field
     */
    @Override
    public String getInstanceFieldType(int slot)
    {            
        String arrayType = getClassName();
        return JavaNames.getArrayElementType(arrayType);
    }

    @Override
    public List<String> getInstanceFields(boolean includeModifiers,
            Map<String, List<String>> restrictedClasses)
    {
        List<String> fields = new ArrayList<String>(length);

        for (int i = 0; i < length; i++) {
            String valString = getFieldValueString(i);
            fields.add("[" + i + "]" + " = " + valString);
        }
        return fields;
    }
    
    @Override
    public String getInstanceField(int slot, boolean includeModifiers)
    {
        String valString = getFieldValueString(slot);
        String field = "[" + slot + "]" + " = " + valString;
        return field;
    }
    
    @Override
    public String getFieldValueString(int index)
    {
        Object value = ((Object []) object)[index];
        
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        else if (value instanceof Enum<?>) {
            Enum<?> enumv = (Enum<?>) value;
            return enumv.name();
        }
        else {
            return DebuggerObject.OBJECT_REFERENCE;
        }
    }
    
    public boolean instanceFieldIsPublic(int slot)
    {
        return true;
    }

    public boolean instanceFieldIsObject(int slot)
    {
        return ((Object []) object)[slot] != null; 
    }
    

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldName");
    }
    

    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot or null if slot does not exist
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
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
        throw new UnsupportedOperationException("getStaticFields");
        //        return new ArrayList(0);
    }
    


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  Description of Parameter
     *@return       Description of the Returned Value
     *@arg          slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
        throw new UnsupportedOperationException("getStaticFieldObject");
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
        throw new UnsupportedOperationException("getStaticFieldObject");
    }
    
    public int hashCode() 
    {
        return super.hashCode();
    }
    
    public boolean equals(Object other)
    {
        return super.equals(other);
    }
}
