/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2016  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.DebuggerObject;
import bluej.utility.JavaUtils;

import com.sun.jdi.CharValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

import java.lang.reflect.InvocationTargetException;

/**
 * Utility methods for Jdi. Used to abstract away differences between java
 * 1.4 and 1.5
 * 
 * @author Davin McCall
 */
public abstract class JdiUtils
{
    private static JdiUtils jutils = null;
    private static final String nullLabel = "null";
    
    /**
     * Factory method. Returns a JdiUtils object.
     * @return an object supporting the approriate feature set
     */
    public static JdiUtils getJdiUtils()
    {
        if (jutils == null)
        {
            jutils = new JdiUtils15();
        }
        return jutils;
    }

    abstract public boolean hasGenericSig(ObjectReference obj);
    
    abstract public String genericSignature(Field f);
    
    abstract public String genericSignature(ReferenceType rt);
    
    abstract public String genericSignature(LocalVariable lv);
    
    abstract public boolean isEnum(ClassType ct);
    
    /**
     * Return the value of a field as as string.
     * 
     * <p>Values are represented differently depending on their type:
     * <ul>
     * <li>A String value is represented as a valid Java string expression.
     * <li>A null value is represented as "null".
     * <li>An Enum value is represented as the name of the Enum constant.
     * <li>Any other object reference is represented as "&lt;object reference&gt;".
     * <li>A primitive value is represented as the value itself.
     * </ul>
     *
     * @see bluej.debugger.DebuggerObject#getInstanceFields(boolean, java.util.Map)
     */
    public String getValueString(Value val)
    {
        if (val == null) {
            return nullLabel;
        }
        else if (val instanceof StringReference) {
            return "\"" + JavaUtils.escapeString(((StringReference) val).value()) + "\"";
        }
        else if (val.type() instanceof ClassType && isEnum((ClassType) val.type())) {
            ClassType type = (ClassType) val.type();
            Field nameField = type.fieldByName("name");
            String name = ((StringReference) ((ObjectReference) val).getValue(nameField)).value();
            return name;
        }
        else if (val instanceof ObjectReference) {
            return DebuggerObject.OBJECT_REFERENCE;
        }
        else if (val instanceof CharValue) {
            return "\'" + JavaUtils.escapeString(val.toString()) + "\'";
        }
        return val.toString();
    }
}
