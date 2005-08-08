package bluej.debugger.gentype;

import java.util.Map;

/**
 * Interface for a parameterizable type, that is, a type which could have type
 * parameters. This includes classes, arrays, wild card types, and type
 * parameters themselves.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeParameterizable.java 3508 2005-08-08 04:18:26Z davmac $
 */
public abstract class GenTypeParameterizable
    extends JavaType
{

    private static GenTypeSolid [] noBounds = new GenTypeSolid[0];
    
    /**
     * Return an equivalent type where all the type parameters have been mapped
     * to the corresponding types using the given map.
     * 
     * @param tparams
     *            A map of (String name -> GenType type).
     * @return An equivalent type with parameters mapped.
     */
    abstract public JavaType mapTparsToTypes(Map tparams);

    abstract public boolean equals(GenTypeParameterizable other);
    
    public boolean equals(Object other)
    {
        if (other instanceof GenTypeParameterizable)
            return equals((GenTypeParameterizable) other);
        else
            return false;
    }

    /**
     * Find the most precise type that can be determined by taking into account
     * commonalities between this type and the given type. For instance if the
     * other class is a subtype of this type, return the subtype. Also, if
     * the types have wildcard type parameters, combine them to form a more
     * specific parameter.
     * 
     * @param other  The other type to precisify against
     * @return  The most precise determinable type, or null if this comparison
     *          is meaningless for the given type (incompatible types).
     */
    public GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        GenTypeSolid upperBound = getUpperBound();
        GenTypeSolid lowerBound = getLowerBound();
        
        // Calculate new upper bounds
        GenTypeSolid newUpper = null;
        GenTypeSolid otherUpper = IntersectionType.getIntersection(other.getUpperBounds());
        if (otherUpper == null)
            newUpper = upperBound;
        else if (upperBound == null)
            newUpper = otherUpper;
        else
            newUpper = IntersectionType.getIntersection(new GenTypeSolid [] {otherUpper, upperBound});
        
        // Calculate new lower bounds
        GenTypeSolid newLower = null;
        GenTypeSolid otherLower = other.getLowerBound();
        if (otherLower == null)
            newLower = lowerBound;
        else if (lowerBound == null)
            newLower = otherLower;
        else
            newLower = GenTypeSolid.lub(new GenTypeSolid [] {otherLower, lowerBound});
        
        // If the upper bounds now equals the lower bounds, we have a solid
        if (newUpper != null && newUpper.equals(newLower))
            return newUpper;
        else
            return new GenTypeWildcard(newUpper, newLower);
    }


    /**
     * Get the upper bounds of this type. For a solid type the upper bounds are the
     * type itself, except for an intersection type, where the bounds are the aggregated
     * bounds of the components of the intersection.
     */
    abstract public GenTypeSolid [] getUpperBounds();
    
    /**
     * Get the upper bounds (possibly as an intersection).
     */
    abstract public GenTypeSolid getUpperBound();

    /**
     * Get the lower bounds of this type. For a solid type the lower bounds are the
     * type itself.
     */
    abstract public GenTypeSolid getLowerBound();
    
    /**
     * Return true if this type "contains" the other type. That is, if this type as a
     * type argument imposes less or equal constraints than the other type in the same
     * place.
     * 
     * @param other  The other type to test against
     * @return True if this type contains the other type
     */
    abstract public boolean contains(GenTypeParameterizable other);
}
