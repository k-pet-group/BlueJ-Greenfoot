package bluej.debugger.gentype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Interface for a parameterizable type, that is, a type which could have type
 * parameters. This includes classes, arrays, wild card types, and type
 * parameters themselves.
 * 
 * @author Davin McCall
 */
public abstract class GenTypeParameterizable
    implements GenType
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
    abstract public GenType mapTparsToTypes(Map tparams);

    abstract public boolean equals(GenTypeParameterizable other);
    
    public boolean equals(Object other)
    {
        if (other instanceof GenTypeParameterizable)
            return equals((GenTypeParameterizable) other);
        else
            return false;
    }

    /**
     * Assuming that this is some type which encloses some type parameters by
     * name, and the given template is a similar type but with actual type
     * arguments, obtain a map which maps the name of the argument (in this
     * type) to the actual type (from the template type).<p>
     * 
     * The given map may already contain some mappings. In this case, the
     * existing mappings will be retained or made more specific.
     * 
     * @param map   A map (String -> GenTypeSolid) to which mappings should
     *              be added
     * @param template   The template to use
     */
    abstract protected void getParamsFromTemplate(Map map, GenTypeParameterizable template);
    
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
    abstract public GenTypeParameterizable precisify(GenTypeParameterizable other);

    abstract public GenTypeSolid [] getUpperBounds();
    
    abstract public GenTypeSolid [] getLowerBounds();
    
    abstract public String toString(NameTransform nt);
    
    /**
     * If this is an array type, get the component type. If this is not an
     * array type, return null.<p>
     */
    public GenType getArrayComponent()
    {
        return null;
    }
    
    /**
     * Find the "greatest common denominator" between this type and the given
     * type. The GCD is the most specific type to which both this and the other
     * type are assignable, assuming that both types are type parameters.<p>
     * 
     * For example,
     *   tpargcd(Thread,String) = ?.
     *   tpargcd(? extends Thread, ? extends Runnable) = ? extends Runnable.
     * 
     * @param other   The type to find the GCD with
     * @return    the type which is the GCD of both types
     */
    public static GenTypeParameterizable getTparGcd(GenTypeParameterizable a, GenTypeParameterizable b)
    {
        GenTypeSolid [] upperBounds = a.getUpperBounds();
        GenTypeSolid [] lowerBounds = a.getLowerBounds();
        GenTypeSolid [] otherUpper = b.getUpperBounds();
        GenTypeSolid [] otherLower = b.getLowerBounds();
        
        ArrayList newUbounds = new ArrayList();
        ArrayList newLbounds = new ArrayList();
        
        for (int i = 0; i < upperBounds.length; i++) {
            // for each upper bound, must find a match or at least near
            // as we can get.
            for (int j = 0; j < otherUpper.length; j++) {
                // get common upper bounds
                // TODO avoid duplicates
                GenTypeClass commonUpper [] = GenTypeSolid.commonBases(upperBounds[i], otherUpper[j]);
                newUbounds.addAll(Arrays.asList(commonUpper));
            }
        }
        
        // also do lower bounds.
        for (int i = 0; i < lowerBounds.length; i++) {
            for (int j = 0; j < otherLower.length; j++) {
                // TODO probably should do some kind of mapping here
                if (lowerBounds[i].isAssignableFromRaw(otherLower[j])) {
                    newLbounds.add(otherLower[j]);
                }
                else if (otherLower[j].isAssignableFromRaw(lowerBounds[i])) {
                    newLbounds.add(lowerBounds[i]);
                }
            }
        }
        
        return optimize(newUbounds, newLbounds);
    }

    // TODO  refactoring potential here is enormous
    private static GenTypeParameterizable optimize(ArrayList ubounds, ArrayList lbounds)
    {
        // first optimize the upper bounds
        for (int i = 0; i < ubounds.size() - 1; i++) {
            for (int j = i + 1; j < ubounds.size(); j++) {
                GenTypeSolid a = (GenTypeSolid) ubounds.get(i);
                GenTypeSolid b = (GenTypeSolid) ubounds.get(j);
                
                // If one of the bounds types is assignable to the other, one
                // of them is redundant. Find out which, and remove it.
                if (a.isAssignableFromRaw(b)) {
                    if (a instanceof GenTypeClass && b instanceof GenTypeClass) {
                        GenTypeClass aClass = (GenTypeClass) a;
                        GenTypeClass bClass = (GenTypeClass) b;
                        Map m = aClass.mapToDerived(bClass.getReflective());
                        GenTypeClass mapped = new GenTypeClass(bClass.getReflective(), m);
                        mapped = (GenTypeClass) mapped.precisify(bClass);
                        ubounds.set(i, mapped);
                    }
                    else {
                        ubounds.set(i, b);
                    }
                    ubounds.remove(j);
                    j--;
                }
                else if (b.isAssignableFromRaw(a)) {
                    if (a instanceof GenTypeClass && b instanceof GenTypeClass) {
                        GenTypeClass aClass = (GenTypeClass) a;
                        GenTypeClass bClass = (GenTypeClass) b;
                        Map m = bClass.mapToDerived(aClass.getReflective());
                        GenTypeClass mapped = new GenTypeClass(aClass.getReflective(), m);
                        mapped = (GenTypeClass) mapped.precisify(aClass);
                        ubounds.set(i, mapped);
                    }
                    ubounds.remove(j);
                    j--;
                }
            }
        }

        // Now process the lower bounds in a similar fashion.
        for (int i = 0; i < lbounds.size() - 1; i++) {
            for (int j = i + 1; j < ubounds.size(); j++) {
                GenTypeSolid a = (GenTypeSolid) lbounds.get(i);
                GenTypeSolid b = (GenTypeSolid) lbounds.get(j);
                
                // If one of the bounds types is assignable to the other, one
                // of them is redundant. Find out which, and remove it.
                if (a.isAssignableFromRaw(b)) {
                    if (a instanceof GenTypeClass && b instanceof GenTypeClass) {
                        GenTypeClass aClass = (GenTypeClass) a;
                        GenTypeClass bClass = (GenTypeClass) b;
                        Map m = bClass.mapToSuper(aClass.rawName());
                        GenTypeClass mapped = new GenTypeClass(aClass.getReflective(), m);
                        mapped = (GenTypeClass) mapped.precisify(aClass);
                        lbounds.set(i, mapped);
                    }
                    lbounds.remove(j);
                    j--;
                }
                else if (b.isAssignableFrom(a)) {
                    if (a instanceof GenTypeClass && b instanceof GenTypeClass) {
                        GenTypeClass aClass = (GenTypeClass) a;
                        GenTypeClass bClass = (GenTypeClass) b;
                        Map m = aClass.mapToSuper(bClass.rawName());
                        GenTypeClass mapped = new GenTypeClass(bClass.getReflective(), m);
                        mapped = (GenTypeClass) mapped.precisify(bClass);
                        lbounds.set(i, mapped);
                    }
                    else {
                        lbounds.set(i, b);
                    }
                    lbounds.remove(j);
                    j--;
                }
            }
        }

        // Now check to see if any upper bounds are equal to the lower bound
        for (int i = 0; i < ubounds.size(); i++) {
            for (int j = 0; j < lbounds.size(); j++) {
                GenTypeSolid u = (GenTypeSolid) ubounds.get(i);
                GenTypeSolid l = (GenTypeSolid) lbounds.get(j);
                
                // first a simple equality check, which also works for tpars
                if (u.equals(l))
                    return u;
                
                // otherwise might be the same class, with different type
                // parameters
                if (u instanceof GenTypeClass && l instanceof GenTypeClass) {
                    GenTypeClass uClass = (GenTypeClass) u;
                    GenTypeClass lClass = (GenTypeClass) l;
                    
                    if (uClass.rawName().equals(lClass.rawName()))
                        return uClass.precisify(lClass);
                }
            }
        }
        
        GenTypeSolid [] uboundsA = (GenTypeSolid []) ubounds.toArray(noBounds);
        GenTypeSolid [] lboundsA = (GenTypeSolid []) lbounds.toArray(noBounds);
        return new GenTypeWildcard(uboundsA, lboundsA);
    }

    
}