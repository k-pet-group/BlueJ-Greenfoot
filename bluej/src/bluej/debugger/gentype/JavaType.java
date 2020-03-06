/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2020  Michael Kolling and John Rosenberg
 
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
package bluej.debugger.gentype;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Map;

/**
 * JavaType, a tree structure describing a type (including generic types).
 * 
 * Most functionality is in subclasses. Default implementations for many
 * methods are provided.
 * 
 * @author Davin McCall
 */

public abstract class JavaType extends GenTypeParameter
{
    public static int JT_VOID = 0;
    public static int JT_NULL = 1;
    public static int JT_BOOLEAN = 2;
    public static int JT_CHAR = 3;
    public static int JT_BYTE = 4;
    public static int JT_SHORT = 5;
    public static int JT_INT = 6;
    public static int JT_LONG = 7;
    public static int JT_FLOAT = 8;
    public static int JT_DOUBLE = 9;
    
    public static int JT_MAX = 9;
    
    public static int JT_LOWEST_NUMERIC = JT_CHAR; // all above are numeric
    public static int JT_LOWEST_FLOAT = JT_FLOAT; // all above are float point
    
    /**
     * Get a string representation of a type, using the specified name
     * transform on all qualified class names.
     * 
     * @param nt  The name transform to use
     * @return    A string representation of this type.
     */
    public String toString(NameTransform nt)
    {
        return toString();
    }
    
    /**
     * Get a string representation of a type.<p>
     * 
     * Where possible this returns a valid java type, even for cases where
     * type inference has yielded an invalid java type. See GenTypeWildcard.
     * 
     * @return  A string representation of this type.
     */
    abstract public String toString();

    /**
     * Get the name of this type as it must appear in the class name of an
     * array with this type as the component type. The array class name is
     * as defined in documentation for Class.getName().<p>
     * 
     * For instance, primitive types are represented as a single upper case
     * character (boolean = Z, byte = B etc). Reference types are encoded
     * as "Lpkg1.pkg2.classname;" ie the fully qualified name preceded by
     * "L" and with ";" appended.
     */
    @OnThread(Tag.FXPlatform)
    abstract public String arrayComponentName();

    /**
     * Assuming that this is some type which encloses some type parameters by
     * name, and the given template is a similar type but with actual type
     * arguments, obtain a map which maps the name of the argument (in this
     * type) to the actual type (from the template type).<p>
     * 
     * The given map may already contain some mappings. In this case, the
     * existing mappings will be retained or made more specific.
     * 
     * @param map   A map to which mappings should be added
     * @param template   The template to use
     */
    @OnThread(Tag.FXPlatform)
    abstract public void getParamsFromTemplate(Map<String,GenTypeParameter> map, GenTypeParameter template);

    /**
     * Determine whether the type represents a primitive type such as "int".
     * This includes the null type, void, and numeric constants.
     */
    abstract public boolean isPrimitive();
    
    /**
     * Determine whether the type is a numeric type (char, byte, short, int,
     * long, float, double)
     */
    public boolean isNumeric()
    {
        return false;
    }
    
    /**
     * Determine whether the type is a primitive integral type: char, byte,
     * short, int or long (specifically excluding float and double)
     */
    public boolean isIntegralType()
    {
        return false;
    }
    
    /**
     * Determine whether the type represents the void type.
     */
    final public boolean isVoid()
    {
        return typeIs(JT_VOID);
    }
    
    /**
     * Determine whether the type represents the null type. 
     */
    final public boolean isNull()
    {
        return typeIs(JT_NULL);
    }
    
    /**
     * Test whether the type is one of the types represented by the constants
     * GT_VOID, GT_NULL, GT_INT, GT_LONG etc.
     */
    public boolean typeIs(int v)
    {
        return false;
    }
    
    /**
     * If this type represents a class type, get it. Otherwise returns null.
     * Arrays with a non-class component type (including arrays of primitives) may return null.
     */
    public GenTypeClass asClass()
    {
        return null;
    }
    
    @Override
    public JavaType asType()
    {
        return this;
    }

    @OnThread(Tag.FXPlatform)
    abstract public boolean equals(JavaType other);

    @OnThread(Tag.FXPlatform)
    public boolean equals(GenTypeParameter other)
    {
        if (other instanceof JavaType) {
            return equals((JavaType) other);
        }
        return false;
    }

    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting, boxing, unchecked conversion etc) a value of the given type.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    @OnThread(Tag.FXPlatform)
    abstract public boolean isAssignableFrom(JavaType t);
    
    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting etc) a value of the given type, if treating both this
     * and the other as raw types.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    @OnThread(Tag.FXPlatform)
    abstract public boolean isAssignableFromRaw(JavaType t);
    
    /**
     * Get an equivalent type where the type parameters have been mapped to
     * an actual type. Type parameters not present in the map are instead
     * mapped to their bound (as a wildcard, ? extends X).
     * 
     * @param tparams A map (String->JavaType) mapping the name of the type
     *                parameter to the corresponding type. May be null, to map
     *                to the raw type.
     * @return A type with parameters mapped
     */
    @OnThread(Tag.FXPlatform)
    abstract public GenTypeParameter mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams);
    
    /**
     * If this is an array type, get the component type. If this is not an
     * array type, return null.
     */
    public JavaType getArrayComponent()
    {
        return null;
    }
    
    /**
     * Get an array type whose component type is this type.
     */
    @OnThread(Tag.FXPlatform)
    abstract public GenTypeSolid getArray();
    
    /**
     * Get the intersecting types that form this type separately. If this type is not
     * an intersection, the returned array contains only this type.
     */
    public JavaType[] getIntersectionTypes()
    {
        return new JavaType[] {this};
    }
    
    /**
     * Perform capture conversion (JLS 7 chapter 5.1.0) on this type and return the result.
     */
    @OnThread(Tag.FXPlatform)
    public JavaType getCapture()
    {
        return this;
    }
    
    @Override
    public JavaType getTparCapture()
    {
        return this;
    }
}
