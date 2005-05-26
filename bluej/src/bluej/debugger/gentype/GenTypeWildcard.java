package bluej.debugger.gentype;

import java.util.*;

import bluej.utility.Debug;

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
 * @version $Id: GenTypeWildcard.java 3386 2005-05-26 01:28:52Z davmac $
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
    
    /**
     * Constructor with a given range of upper and lower bounds. The arrays
     * used should not be modified afterwards.
     * 
     * @param uppers  The upper bounds
     * @param lowers  The lower bounds
     */
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
        if (lowerBounds.length != 0) {
            return "? super " + lowerBounds[0].toString(stripPrefix);
        }
        else if (upperBounds.length != 0) {
            if (upperBounds[0] instanceof GenTypeClass) {
                if (((GenTypeClass) upperBounds[0]).rawName().equals("java.lang.Object"))
                    return "?";
            }
            return "? extends " + upperBounds[0].toString(stripPrefix);
        }
        else
            return "?";
    }
    
    public String toString(NameTransform nt)
    {
        // return only a legal java type string
        if (lowerBounds.length != 0) {
            return "? super " + lowerBounds[0].toString(nt);
        }
        else if (upperBounds.length != 0) {
            if (upperBounds[0] instanceof GenTypeClass) {
                if (((GenTypeClass) upperBounds[0]).rawName().equals("java.lang.Object"))
                    return "?";
            }
            return "? extends " + upperBounds[0].toString(nt);
        }
        else
            return "?";
    }
    
    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }
    
    // TODO refactor.
    public GenTypeParameterizable precisify(GenTypeParameterizable other)
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
                    GenTypeParameterizable precis = otherwc.lowerBounds[i].precisify((GenTypeParameterizable) lbounds.get(j));
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
                    GenTypeParameterizable precis = otherwc.upperBounds[i].precisify((GenTypeParameterizable) ubounds.get(j));
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
            
            // Check for merging of upper & lower bounds.
            // ie. if upper == lower then return GenTypeClass.
            for (Iterator i = ubounds.iterator(); i.hasNext(); ) {
                for (Iterator j = lbounds.iterator(); j.hasNext(); ) {
                    GenTypeParameterizable ubound = (GenTypeParameterizable) i.next();
                    GenTypeParameterizable lbound = (GenTypeParameterizable) j.next();
                    
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
                    
                    GenTypeClass mapped = (GenTypeClass) bound.mapToDerived(bound.getReflective());
                    otherClass = (GenTypeClass) mapped.precisify(otherClass);
                    if (otherClass == null)
                        return null;
                }
            }
            
            for (int i = 0; i < lowerBounds.length; i++) {
                // Only interested in class types.
                if (lowerBounds[i] instanceof GenTypeClass) {
                    GenTypeClass bound = (GenTypeClass) lowerBounds[i];
                    
                    otherClass = (GenTypeClass) bound.mapToSuper(otherClass.getReflective().getName()).precisify(otherClass);
                    if (otherClass == null)
                        return null;
                }
            }

            return otherClass;
        }
        
        return null;
    }
    
    /*
     * Do not create abominations such as "? extends ? extends ...".
     *   "? extends ? super ..."    => "?" (ie. the bounds is eliminated).
     *   "? extends ? extends X"    => "? extends X".
     *   "? super ? super X"        => "? super X".
     */
    public GenType mapTparsToTypes(Map tparams)
    {
        ArrayList newUpper = new ArrayList();
        ArrayList newLower = new ArrayList();
        
        // find the new upper bounds
        for (int i = 0; i < upperBounds.length; i++) {
            GenTypeParameterizable newBound = (GenTypeParameterizable) upperBounds[i].mapTparsToTypes(tparams);
            if (newBound instanceof GenTypeWildcard) {
                GenTypeWildcard newWcBound = (GenTypeWildcard) newBound;
                for (int j = 0; j < newWcBound.upperBounds.length; j++)
                    newUpper.add(newWcBound.upperBounds[j]);
            }
            else
                newUpper.add(newBound);
        }
        
        // find the new lower bounds
        for (int i = 0; i < lowerBounds.length; i++) {
            GenTypeParameterizable newBound = (GenTypeParameterizable) lowerBounds[i].mapTparsToTypes(tparams);
            if (newBound instanceof GenTypeWildcard) {
                GenTypeWildcard newWcBound = (GenTypeWildcard) newBound;
                for (int j = 0; j < newWcBound.lowerBounds.length; j++)
                    newLower.add(newWcBound.lowerBounds[j]);
            }
        }
            
        // above may yield redundant bounds. Optimize.
        return optimize(newUpper, newLower);
    }
    
    // TODO  refactoring potential here is enormous
    private GenTypeParameterizable optimize(ArrayList ubounds, ArrayList lbounds)
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
                        GenTypeClass mapped = (GenTypeClass) aClass.mapToDerived(bClass.getReflective());
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
                        GenTypeClass mapped = (GenTypeClass) bClass.mapToDerived(aClass.getReflective());
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
                        GenTypeClass mapped = bClass.mapToSuper(aClass.rawName());
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
                        GenTypeClass mapped = aClass.mapToSuper(bClass.rawName());
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
                GenTypeParameterizable x = (GenTypeParameterizable) j.next();
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
                GenTypeParameterizable x = (GenTypeParameterizable) j.next();
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
        // TODO fix. Actually it probably can get called. When it is called, it
        // should only be on an actual java type (not a type with multiple
        // bounds), and it may match against a wildcard or a class.
        Debug.reportError("getParamsFromTemplate called on GenTypeWildcard.");
        return;
    }
    
    public GenType getErasedType()
    {
        return upperBounds[0].getErasedType();
    }
    
    public boolean isPrimitive()
    {
        return false;
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        // TODO fix
        return false;
    }
    
    public boolean isAssignableFromRaw(GenType t)
    {
        // TODO fix
        return false;
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
