package bluej.debugger.gentype;

import java.util.Map;

/**
 * GenType, a tree structure describing a type (including generic types).
 * 
 * Most functionality is in subclasses. Default implementations for many
 * methods are provided.
 * 
 * @author Davin McCall
 * @version $Id: GenType.java 3102 2004-11-18 01:39:18Z davmac $
 */

public abstract class GenType
{
    public static int GT_VOID = 0;
    public static int GT_NULL = 1;
    public static int GT_BOOLEAN = 2;
    public static int GT_CHAR = 3;
    public static int GT_BYTE = 4;
    public static int GT_SHORT = 5;
    public static int GT_INT = 6;
    public static int GT_LONG = 7;
    public static int GT_FLOAT = 8;
    public static int GT_DOUBLE = 9;
    
    public static int GT_MAX = 9;
    
    public static int GT_LOWEST_NUMERIC = GT_CHAR; // all above are numeric
    public static int GT_LOWEST_FLOAT = GT_FLOAT; // all above are float point
    
    /**
     * Get a string representation of a type, optionally stripping pacakge
     * prefixes from all class names.<p>
     * 
     * See toString().
     * 
     * @param stripPrefix  true to strip package prefixes; false otherwise.
     * @return  A string representation of this type.
     */
    public String toString(boolean stripPrefix)
    {
        return toString();
    }
    
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
     * Determine whether the type represents a primitive type such as "int".
     * This includes the null type, void, and numeric constants.
     */
    abstract public boolean isPrimitive();
    
    /**
     * Determine whether the type is a numeric type (char, byte, int, long,
     * float, double, also including numeric literals)
     */
    public boolean isNumeric()
    {
        return false;
    }
    
    public boolean isIntegerLiteral()
    {
        return false;
    }
    
    /**
     * Check whether this type "fits into" the given numeric primitive type.
     * This is mainly useful for numeric literals. For instance the value 4,
     * while nominally an int value, can be assigned to a byte or char
     * variable.<p>
     * 
     * Java's handling of numeric literals is quite bizarre in some respects.
     * For instance it is possible to cast a number to char, then assign it to
     * a byte variable, even though it is not normally possible to assign a
     * char to a byte. On the other hand casting the same number to a long and
     * then assigning to a byte causes a compile-time error.
     */
    public boolean fitsType(int type)
    {
        return false;
    }
    
    /**
     * Determine whether the type represents the void type.
     */
    final public boolean isVoid()
    {
        return typeIs(GT_VOID);
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
     */
    public GenTypeClass asClass()
    {
        return null;
    }
    
    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting etc) a value of the given type.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    abstract public boolean isAssignableFrom(GenType t);
    
    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting etc) a value of the given type, if treating both this
     * and the other as raw types.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    abstract public boolean isAssignableFromRaw(GenType t);
    
    /**
     * Get an equivalent type where the type parameters have been mapped to
     * an actual type.
     * 
     * @param tparams A map (String->GenType) mapping the name of the type
     *                parameter to the corresponding type
     * @return A type with parameters mapped
     */
    abstract public GenType mapTparsToTypes(Map tparams);
    
    /**
     * If this is an array type, get the component type. If this is not an
     * array type, return null.
     */
    public GenType getArrayComponent()
    {
        return null;
    }

}
