package bluej.debugger.gentype;

import java.util.*;

/**
 * A wildcard type with an upper and/or lower bound.<p>
 * 
 * Note that both an upper and lower bound is allowed. This type doesn't occur
 * naturally- it can't be specified in the Java language. But in some cases we
 * can deduce the type of some object to be this.<p>
 *
 * This is an Immutable type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeWildcard.java 3063 2004-10-25 02:37:00Z davmac $
 */
public class GenTypeWildcard extends GenTypeParameterizable
{
    protected static final GenTypeSolid [] noBounds = new GenTypeSolid[0];
    
    GenTypeSolid upperBounds[];  // ? extends upperBound
    GenTypeSolid lowerBounds[];  // ? super lowerBound
    
    public GenTypeWildcard(GenTypeSolid upper, GenTypeSolid lower)
    {
        if (upper != null)
            upperBounds = new GenTypeSolid[] {upper};
        else
            upperBounds = noBounds;
        
        if (lower != null)
            lowerBounds = new GenTypeSolid[] {lower};
        else
            lowerBounds = noBounds;
    }
    
    public GenTypeWildcard(GenTypeSolid [] uppers, GenTypeSolid [] lowers)
    {
        upperBounds = uppers;
        lowerBounds = lowers;
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        // return only a legal java type string
        if (lowerBounds.length != 0)
            return "? super " + lowerBounds[0].toString(stripPrefix);
        else if (upperBounds.length != 0)
            return "? extends " + upperBounds[0].toString(stripPrefix);
        else
            return "?";
    }
    
    public String toString(NameTransform nt)
    {
        // return only a legal java type string
        if (lowerBounds.length != 0)
            return "? super " + lowerBounds[0].toString(nt);
        else if (upperBounds.length != 0)
            return "? extends " + upperBounds[0].toString(nt);
        else
            return "?";
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // create a copy of our current lower bounds, as an arrayList
        List lbounds = new ArrayList(lowerBounds.length);
        lbounds.addAll(Arrays.asList(lowerBounds));
        
        List ubounds = new ArrayList(upperBounds.length);
        ubounds.addAll(Arrays.asList(upperBounds));
        
        if (other instanceof GenTypeWildcard) {
            GenTypeWildcard otherwc = (GenTypeWildcard) other;
            
            // precisify lower bounds
            for (int i = 0; i < otherwc.lowerBounds.length; i++) {
                int j;
                for (j = 0; j < lowerBounds.length; j++) {
                    GenTypeParameterizable precis = otherwc.lowerBounds[i].precisify((GenTypeSolid) lbounds.get(j));
                    if (precis != null) {
                        lbounds.set(j, precis);
                        break;
                    }
                }
                
                // If the lower bound in the other didn't merge with one of
                // ours, add it as a new one.
                if (j == lowerBounds.length)
                    lbounds.add(otherwc.lowerBounds[i]);
            }
            
            // precisify upper bounds
            for (int i = 0; i < otherwc.upperBounds.length; i++) {
                int j;
                for (j = 0; j < upperBounds.length; j++) {
                    GenTypeParameterizable precis = otherwc.upperBounds[i].precisify((GenTypeSolid) ubounds.get(j));
                    if (precis != null) {
                        ubounds.set(j, precis);
                        break;
                    }
                }
                
                // If the bound in the other didn't merge with one of
                // ours, add it as a new one.
                if (j == upperBounds.length)
                    ubounds.add(otherwc.lowerBounds[i]);
            }
            
            // Ceck for merging of upper & lower bounds.
            // ie. if upper == lower then return GenTypeClass.
            for (Iterator i = ubounds.iterator(); i.hasNext(); ) {
                for (Iterator j = lbounds.iterator(); j.hasNext(); ) {
                    GenTypeSolid ubound = (GenTypeSolid) i.next();
                    GenTypeSolid lbound = (GenTypeSolid) j.next();
                    
                    if (ubound.equals(lbound))
                        return ubound;
                }
            }
            
            return new GenTypeWildcard((GenTypeSolid [])ubounds.toArray(noBounds), (GenTypeSolid [])lbounds.toArray(noBounds));
            
        }
        
        if (other instanceof GenTypeClass) {
            // map each bound to the class type, and precisify results
            
            GenTypeClass otherClass = (GenTypeClass) other;
            
            for (int i = 0; i < upperBounds.length; i++) {
                // Only interested in class types.
                if (upperBounds[i] instanceof GenTypeClass) {
                    GenTypeClass bound = (GenTypeClass) upperBounds[i];
                    
                    Map m = bound.mapToDerived(bound.getReflective());
                    otherClass = (GenTypeClass) new GenTypeClass(bound.getReflective(), m).precisify(otherClass);
                    if (otherClass == null)
                        return null;
                }
            }
            
            for (int i = 0; i < lowerBounds.length; i++) {
                // Only interested in class types.
                if (lowerBounds[i] instanceof GenTypeClass) {
                    GenTypeClass bound = (GenTypeClass) lowerBounds[i];
                    
                    Map m = bound.mapToSuper(otherClass.getReflective().getName());
                    
                    otherClass = (GenTypeClass) new GenTypeClass(bound.getReflective(), m).precisify(otherClass);
                    if (otherClass == null)
                        return null;
                }
            }

            return otherClass;
        }
        
        return null;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        GenTypeSolid [] newUpper = new GenTypeSolid[upperBounds.length];
        GenTypeSolid [] newLower = new GenTypeSolid[lowerBounds.length];
        
        for (int i = 0; i < upperBounds.length; i++)
            newUpper[i] = (GenTypeSolid) upperBounds[i].mapTparsToTypes(tparams);
        
        for (int i = 0; i < lowerBounds.length; i++)
            newLower[i] = (GenTypeSolid) lowerBounds[i].mapTparsToTypes(tparams);

        return new GenTypeWildcard(newUpper, newLower);
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if (this == other)
            return true;
        if( ! (other instanceof GenTypeWildcard) )
            return false;
        
        GenTypeWildcard bOther = (GenTypeWildcard)other;
        if (bOther.lowerBounds.length != lowerBounds.length
                || bOther.upperBounds.length != upperBounds.length)
            return false;

        List oLowerBounds = new LinkedList(Arrays.asList(bOther.lowerBounds));
        List oUpperBounds = new LinkedList(Arrays.asList(bOther.upperBounds));

        // Compare each of the lower bounds
        for (int i = 0; i < lowerBounds.length; i++) {
            boolean matched = false;
            ListIterator j = oLowerBounds.listIterator();
            while (j.hasNext()) {
                GenTypeSolid x = (GenTypeSolid) j.next();
                if (x.equals(lowerBounds[i])) {
                    matched = true;
                    j.remove();
                    break;
                }
            }
            if (! matched)
                return false;
        }
            
        // Compare each of the upper bounds
        for (int i = 0; i < upperBounds.length; i++) {
            boolean matched = false;
            ListIterator j = oUpperBounds.listIterator();
            while (j.hasNext()) {
                GenTypeSolid x = (GenTypeSolid) j.next();
                if (x.equals(upperBounds[i])) {
                    matched = true;
                    j.remove();
                    break;
                }
            }
            if (! matched)
                return false;
        }
        
        return true;
    }
    
    protected void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        // This should never actually be called on a wildcard type (I think).
        return;
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    /**
     * Get the upper bounds of this wildcard type, as an array. The upper
     * bounds are those occurring in "extends" clauses.
     * 
     * @return A copy of the upper bounds.
     */
    public GenTypeSolid[] getUpperBounds()
    {
        GenTypeSolid [] r = new GenTypeSolid[upperBounds.length];
        System.arraycopy(upperBounds, 0, r, 0, r.length);
        return r;
    }
    
    /**
     * Get the lower bounds of this wildcard type, as an array. The lower
     * bounds are those occurring in "super" clauses.
     * 
     * @return A copy of the lower bounds.
     */
    public GenTypeSolid[] getLowerBounds()
    {
        GenTypeSolid [] r = new GenTypeSolid[lowerBounds.length];
        System.arraycopy(lowerBounds, 0, r, 0, r.length);
        return r;
    }
}
