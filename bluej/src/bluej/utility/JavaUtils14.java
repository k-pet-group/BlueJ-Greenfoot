/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.utility;

import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;

/**
 * Java 1.4 version of JavaUtils
 * 
 * @author Davin McCall
 */
public class JavaUtils14
{
    /**
     * Gets nicely formatted strings describing the parameter types.
     */
    public static String[] getParameterTypes(Class<?>[] params)
    {
        String[] parameterTypes = new String[params.length];
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            parameterTypes[j] = typeName;
        }
        return parameterTypes;
    }

    /**
     * Get a type name, with prefix stripped. For array types return the base
     * type name plus the appropriate number of "[]" qualifiers.
     */
    public static String getTypeName(Class<?> type)
    {
        return JavaNames.stripPrefix(JavaUtils.getFQTypeName(type));
    }
    
    /**
     * Get a GenType corresponding to the (raw) class c
     */
    public static JavaType genTypeFromClass14(Class<?> c)
    {
        if (c.isPrimitive()) {
            if (c == boolean.class)
                return JavaPrimitiveType.getBoolean();
            if (c == char.class)
                return JavaPrimitiveType.getChar();
            if (c == byte.class)
                return JavaPrimitiveType.getByte();
            if (c == short.class)
                return JavaPrimitiveType.getShort();
            if (c == int.class)
                return JavaPrimitiveType.getInt();
            if (c == long.class)
                return JavaPrimitiveType.getLong();
            if (c == float.class)
                return JavaPrimitiveType.getFloat();
            if (c == double.class)
                return JavaPrimitiveType.getDouble();
            if (c == void.class)
                return JavaPrimitiveType.getVoid();
            Debug.message("getReturnType: Unknown primitive type");
        }
        if (c.isArray()) {
            JavaType componentT = genTypeFromClass14(c.getComponentType());
            return new GenTypeArray(componentT);
        }
        return new GenTypeClass(new JavaReflective(c));
    }
}
