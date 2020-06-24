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

import java.util.Map;

import bluej.utility.JavaNames;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Interface for a parameterizable type, that is, a type which could have type
 * parameters. This includes classes, arrays, wild card types, and type
 * parameters themselves.
 * 
 * @author Davin McCall
 */
public abstract class GenTypeParameter
{

    private static NameTransform stripPrefixNt = new NameTransform() {
        public String transform(String x) {
            return JavaNames.stripPrefix(x);
        }
    };
    
    private static NameTransform nullTransform = new NameTransform() {
        public String transform(String x) {
            return x;
        }
    };
    
    /**
     * Return an equivalent type where all the type parameters have been mapped
     * to the corresponding types using the given map.
     * 
     * @param tparams
     *            A map of (String name -> GenType type).
     * @return An equivalent type with parameters mapped.
     */
    @OnThread(Tag.FXPlatform)
    abstract public GenTypeParameter mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams);

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public boolean equals(Object other)
    {
        if (other instanceof GenTypeParameter) {
            return equals((GenTypeParameter) other);
        } else {
            return false;
        }
    }

    @OnThread(Tag.FXPlatform)
    abstract public boolean equals(GenTypeParameter other);

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
    @OnThread(Tag.FXPlatform)
    public GenTypeParameter precisify(GenTypeParameter other)
    {
        GenTypeSolid upperBound = getUpperBound().asSolid();
        if (upperBound == null) {
            return this;
        }
        GenTypeSolid lowerBound = getLowerBound();
        
        // Calculate new upper bounds
        GenTypeSolid newUpper = null;
        GenTypeSolid otherUpper = other.getUpperBound().asSolid();
        if (otherUpper == null) {
            newUpper = upperBound;
        }
        else {
            newUpper = IntersectionType.getIntersection(new GenTypeSolid [] {otherUpper, upperBound});
        }
        
        // Calculate new lower bounds
        GenTypeSolid newLower = null;
        GenTypeSolid otherLower = other.getLowerBound();
        if (otherLower == null) {
            newLower = lowerBound;
        }
        else if (lowerBound == null) {
            newLower = otherLower;
        }
        else {
            newLower = GenTypeSolid.lub(new GenTypeSolid [] {otherLower, lowerBound});
        }
        
        // If the upper bounds now equals the lower bounds, we have a solid
        if (newUpper != null && newUpper.equals(newLower)) {
            return newUpper;
        }
        else {
            return new GenTypeWildcard(newUpper, newLower);
        }
    }

    /**
     * Get the upper bound of this type parameter (possibly as an intersection).
     */
    abstract public JavaType getUpperBound();

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
    @OnThread(Tag.FXPlatform)
    public final boolean contains(GenTypeParameter other)
    {
        GenTypeSolid myLower = getLowerBound();
        JavaType myUpper = getUpperBound();
        
        GenTypeSolid otherLower = other.getLowerBound();
        JavaType otherUpper = other.getUpperBound();
        
        if (myUpper != null) {
            if (otherUpper == null) {
                if (myUpper.asClass() == null) {
                    return false;
                }
                else {
                    if (! myUpper.asClass().classloaderName().equals("java.lang.Object")) {
                        return false;
                    }
                }
            }
            else {
                if (! myUpper.isAssignableFrom(otherUpper)) {
                    return false;
                }
            }
        }
        
        if (myLower != null) {
            if (otherLower == null) {
                return false;
            }
            if (! otherLower.isAssignableFrom(myLower)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get a string representation of the type, optionally stripping package prefixes
     */
    @OnThread(Tag.FXPlatform)
    public String toString(boolean stripPrefix)
    {
        if (stripPrefix) {
            return toString(stripPrefixNt);
        }
        else {
            return toString(nullTransform);
        }
    }
    
    /**
     * Returns a string which is a java-source valid type argument,
     * compatible with this actual type. For an intersection type this
     * returns a compatible wildcard.
     * 
     * @param stripPrefix   True if package prefixes should be stripped
     */
    public String toTypeArgString(boolean stripPrefix)
    {
        if (stripPrefix)
            return toString(stripPrefixNt);
        else
            return toString(nullTransform);
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
     * Returns a string which is a java-source valid type argument
     * compatible with this actual type, modified using the given name
     * transform.
     * 
     * For an intersection type this returns a compatible wildcard.
     * 
     * @param nt
     * @return
     */
    abstract public String toTypeArgString(NameTransform nt);
    
    /**
     * Get the erased type of this type.
     */
    @OnThread(Tag.FXPlatform)
    abstract public JavaType getErasedType();
    
    /**
     * Capture this type parameter and return the result.
     */
    abstract public JavaType getTparCapture();
    
    /**
     * Check whether this represents a primitive type.
     */
    public boolean isPrimitive()
    {
        return false;
    }

    public abstract boolean isWildcard();
    
    public GenTypeParameter getArrayComponent()
    {
        return null;
    }
    
    /**
     * If this type parameter represents a "solid" (reference) type, get it. Otherwise return null.
     */
    public GenTypeSolid asSolid()
    {
        return null;
    }
    
    /**
     * If this type parameter is a type, get it. Otherwise return null.
     */
    public JavaType asType()
    {
        return null;
    }
}
