package bluej.debugger.gentype;

import java.util.Map;

/**
 * GenType, a tree structure describing a type (including generic types).
 * 
 * Most functionality is in subclasses.
 * 
 * @author Davin McCall
 * @version $Id: GenType.java 3075 2004-11-09 00:10:18Z davmac $
 */

public interface GenType {

    /**
     * Get a string representation of a type.<p>
     * 
     * Where possible this returns a valid java type, even for cases where
     * type inference has yielded an invalid java type. See GenTypeWildcard.
     * 
     * @return  A string representation of this type.
     */
    public String toString();
    
    /**
     * Get a string representation of a type, optionally stripping pacakge
     * prefixes from all class names.<p>
     * 
     * See toString().
     * 
     * @param stripPrefix  true to strip package prefixes; false otherwise.
     * @return  A string representation of this type.
     */
    public String toString(boolean stripPrefix);

    /**
     * Determine whether the type represents a primitive type such as "int".
     */
    public boolean isPrimitive();
    
    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting etc) a value of the given type.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    public boolean isAssignableFrom(GenType t);
    
    /**
     * Determine whether a variable of this type could legally be assigned
     * (without casting etc) a value of the given type, if treating both this
     * and the other as raw types.
     * 
     * @param t  The type to check against
     * @return   true if the type is assignable to this type
     */
    public boolean isAssignableFromRaw(GenType t);
    
    /**
     * Get an equivalent type where the type parameters have been mapped to
     * an actual type.
     * 
     * @param tparams A map (String->GenType) mapping the name of the type
     *                parameter to the corresponding type
     * @return A type with parameters mapped
     */
    public GenType mapTparsToTypes(Map tparams);
    
    /**
     * If this is an array type, get the component type. If this is not an
     * array type, return null.
     */
    public GenType getArrayComponent();

}
