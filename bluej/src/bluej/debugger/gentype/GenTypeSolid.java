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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


/**
 * A "solid" type is a non-primitive, non-wildcard type. This includes arrays,
 * classes, and type parameters. Basically, a "solid" is anything that can be
 * a component type for a wildcard clause.
 * 
 * @author Davin McCall
 */
public abstract class GenTypeSolid extends JavaType
{
    // force toString(NameTransform) to be reimplemented
    public abstract String toString(NameTransform nt);
    
    // provide a default implementation for toString().
    public String toString()
    {
        return toString(false);
    }
    
    public boolean isPrimitive()
    {
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public abstract boolean isInterface();
    
    /**
     * Get the erased supertypes of this type, as defined in the JLS 3.0
     * section 15.12.2.7.
     * 
     * @param s  The set into which to store the reflectives
     */
    @OnThread(Tag.FXPlatform)
    public abstract void erasedSuperTypes(Set<Reflective> s);
    
    /**
     * Find the minimal set of supertypes of this type which are reference types. For tpars
     * this is the bounds. For a class/interface this is the type itself. 
     */
    @OnThread(Tag.FXPlatform)
    public abstract GenTypeClass [] getReferenceSupertypes();
    
    /**
     * Assuming that this is some solid type that is either a tpar or a
     * class whose type arguments also match this definition (recursively),
     * and the given template is a similar type (a constraint if this is
     * a tpar, or the same class with type arguments acting as constraints).<p>
     * 
     * For example, if this type is LinkedList&lt;T&gt;, and the template
     * is LinkedList&lt;Thread&gt;, then this method returns a map containing
     * the mapping "T -&gt; Thread".<p> 
     * 
     * The given map may already contain some mappings. In this case, the
     * existing mappings will be retained or made more specific.
     * 
     * @param map   A map to which mappings should be added
     * @param template   The template to use
     */
    @OnThread(Tag.FXPlatform)
    abstract public void getParamsFromTemplate(Map<String,GenTypeParameter> map, GenTypeParameter template);
    
    /*
     *  Implement methods from GenTypeParameterizable
     */
    
    public GenTypeSolid [] getUpperBounds()
    {
        return getIntersectionTypes();
    }
    
    @Override
    public GenTypeSolid getUpperBound()
    {
        return this;
    }
    
    @Override
    public GenTypeSolid getLowerBound()
    {
        return this;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getCapture()
    {
        return this;
    }
    
    @Override
    public GenTypeSolid asSolid()
    {
        return this;
    }
    
    @Override
    public boolean isWildcard()
    {
        return false;
    }
    
    /*
     * Static methods
     */
    
    /**
     * Calculate lub, as defined in revised JLS 15.12.2. Essentially this
     * means, calculate the most specific type to which all the given types are
     * convertible.<p>
     */
    @OnThread(Tag.FXPlatform)
    public static GenTypeSolid lub(GenTypeSolid [] ubounds)
    {
        Stack<GenTypeClass[]> btstack = new Stack<GenTypeClass[]>();
        return lub(ubounds, btstack);
    }
    
    /*
     * Private static methods
     */
    
    /**
     * lub workhorse method, uses a stack backtrace to avoid infinite recursion.
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeSolid lub(GenTypeSolid [] ubounds, Stack<GenTypeClass[]> lubBt)
    {
        // "lowest(/least) upper bound"?
        
        List<GenTypeSolid> l = new ArrayList<GenTypeSolid>();
        Reflective [] mec = minimalErasedCandidateSet(ubounds);
        for (int i = 0; i < mec.length; i++) {
            l.add(Candidate(mec[i], ubounds, lubBt));
        }
        
        GenTypeSolid [] intersecting = l.toArray(new GenTypeSolid[l.size()]);
        return IntersectionType.getIntersection(intersecting);
    }
    
    /**
     * This is the "Candidate" (and "CandidateInvocation") function as defined
     * in the proposed JLS, section 15.12.2.7
     * 
     * @param t        The class type to find the candidate type for
     * @param ubounds  The complete set of bounding types (see lub())
     * @param lubBt    A backtrace used to avoid infinite recursion
     * @return  The candidate type
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeClass Candidate(Reflective t, GenTypeSolid [] ubounds, Stack<GenTypeClass[]> lubBt)
    {
        GenTypeClass [] ri = relevantInvocations(t, ubounds);
        return leastContainingInvocation(ri, lubBt);
    }
    
    /**
     * Find the least containing invocation from a set of invocations. The
     * invocations a, b, ... are types based on the same class G. The return is
     * a generic type G<...> such that all  a, b, ... are convertible to the
     * return type.<p>
     * 
     * This is "lci" as defined in the JLS 3rd edition section 15.12.2.7 
     * 
     * @param types   The invocations
     * @param lubBt   A backtrace used to avoid infinite recursion
     * @return   The least containing type
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeClass leastContainingInvocation(GenTypeClass [] types, Stack<GenTypeClass[]> lubBt)
    {
        // first check for infinite recursion:
        boolean breakRecursion = false;
        Iterator<GenTypeClass[]> si = lubBt.iterator();
        while (si.hasNext()) {
            GenTypeSolid [] sbounds = si.next();
            int i;
            for (i = 0; i < sbounds.length; i++) {
                if (! sbounds[i].equals(types[i])) {
                    break;
                }
            }
            breakRecursion = (i == sbounds.length);
            // TODO this is really supposed to result in a recursively-
            // defined type.
        }
        
        lubBt.push(types);
        GenTypeClass rtype = types[0];
        for (int i = 1; i < types.length; i++) {
            rtype = leastContainingInvocation(rtype, types[i], lubBt, breakRecursion);
        }
        lubBt.pop();
        return rtype;
    }
    
    /**
     * Find the least containing invocation from two invocations.
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeClass leastContainingInvocation(GenTypeClass a, GenTypeClass b, Stack<GenTypeClass[]> lubBt, boolean breakRecursion)
    {
        if (! a.getReflective().getName().equals(b.getReflective().getName()))
            throw new IllegalArgumentException("Class types must be the same.");
        
        if (a.isRaw() || b.isRaw())
            return (a.isRaw()) ? a : b;
        
        // Handle arrays - apply against component type
        int arrCount = 0; // number of array dimensions
        GenTypeClass origA = a;
        while (a.getArrayComponent() != null) {
            a = a.getArrayComponent().asClass();
            b = b.getArrayComponent().asClass();
            if (a == null) {
                // if a is now null, the array is of primitive type
                return origA;
            }
            arrCount++;
        }
        
        List<GenTypeParameter> lc = new ArrayList<GenTypeParameter>();
        Iterator<? extends GenTypeParameter> i = a.getTypeParamList().iterator();
        Iterator<? extends GenTypeParameter> j = b.getTypeParamList().iterator();
        
        GenTypeClass oa = a.getOuterType();
        GenTypeClass ob = b.getOuterType();
        GenTypeClass oc = null;
        if (oa != null && ob != null)
            oc = leastContainingInvocation(oa, ob, lubBt, breakRecursion);

        // lci(G<X1,...,Xn>, G<Y1,...,Yn>) =
        //       G<lcta(X1,Y1), ..., lcta(Xn,Yn)>
        while (i.hasNext()) {
            GenTypeParameter atype = (GenTypeParameter) i.next();
            GenTypeParameter btype = (GenTypeParameter) j.next();
            GenTypeParameter rtype;
            if (! breakRecursion)
                rtype = leastContainingTypeArgument(atype, btype, lubBt);
            else
                rtype = new GenTypeUnbounded();
            lc.add(rtype);
        }
        
        // re-instate array dimensions
        GenTypeClass rval = new GenTypeClass(a.getReflective(), lc, oc);
        while (arrCount-- > 0) {
            rval = rval.getArray();
        }
        return rval;
    }
    
    /**
     * Find the "least containing" type of two type parameters. This is "lcta"
     * as defined in the JLS section 15.12.2.7 
     * 
     * @param a      The first type parameter
     * @param b      The second type parameter
     * @param lubBt  The backtrace for avoiding infinite recursion
     * @return   The least containing type
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeParameter leastContainingTypeArgument(GenTypeParameter a, GenTypeParameter b, Stack<GenTypeClass[]> lubBt)
    {
        GenTypeSolid ac = a.asSolid();
        GenTypeSolid bc = b.asSolid();
        
        // Both arguments are of solid type?
        if (ac != null && bc != null) {
            if (ac.equals(bc))
                return ac;
            else
                return new GenTypeWildcard(lub(new GenTypeSolid [] {ac, bc}, lubBt), null);
        }
        
        if (ac != null || bc != null) {
            // One is a solid type and the other is a wildcard type. Ensure
            // that ac is the solid and b is the wildcard:
            if (ac == null) {
                ac = bc;
                b = a;
            }

            GenTypeSolid lbound = b.getLowerBound();
            if (lbound != null) {
                return new GenTypeWildcard(null, IntersectionType.getIntersection(lbound, ac));
            }
        }
        
        GenTypeSolid lboundsa = a.getLowerBound();
        GenTypeSolid lboundsb = b.getLowerBound();
        if (lboundsa != null && lboundsb != null) {
            return new GenTypeWildcard(null, IntersectionType.getIntersection(lboundsa, lboundsb));
        }
        
        if (lboundsa != null || lboundsb != null) {
            // lcta(? super U, ? extends V)
            if (a.equals(b))
                return a;
            
            // otherwise return good old '?'.
            return new GenTypeUnbounded();
        }
        
        // The only option left is lcta(? extends U, ? extends V)
        GenTypeSolid uboundsa = a.getUpperBound().asSolid();
        GenTypeSolid uboundsb = b.getUpperBound().asSolid();
        GenTypeSolid [] args = new GenTypeSolid[2];
        args[0] = uboundsa;
        args[1] = uboundsb;
        return lub(args);
    }
    
    /**
     * Find the "minimal erased candidate set" of a set of types (MEC as
     * defined in the JLS, section 15.12.2.7. This is the set of all (raw)
     * supertypes common to each type in the given set, with no duplicates or
     * redundant types (types whose presence is dictated by the presence of a
     * subtype).
     * 
     * @param types   The types for which to find the MEC.
     * @return        The MEC as an array of Reflective.
     */
    @OnThread(Tag.FXPlatform)
    private static Reflective [] minimalErasedCandidateSet(GenTypeSolid [] types)
    {
        // have to find *intersection* of all sets and remove redundant types
        
        Set<Reflective> rset = new HashSet<Reflective>();
        types[0].erasedSuperTypes(rset);
        
        for (int i = 1; i < types.length; i++) {
            Set<Reflective> rset2 = new HashSet<Reflective>();
            types[i].erasedSuperTypes(rset2);
            
            // find the intersection incrementally
            Iterator<Reflective> j = rset2.iterator();
            while (j.hasNext()) {
                if( ! rset.contains(j.next()))
                    j.remove();
            }
            rset = rset2;
        }
        
        // Now remove redundant types
        Iterator<Reflective> i = rset.iterator();
        while (i.hasNext()) {
            Iterator<Reflective> j = rset.iterator();
            Reflective ri = (Reflective) i.next();
            
            while (j.hasNext()) {
                Reflective ji = (Reflective) j.next();
                if (ri == ji)
                    continue;
                
                if (ri.isAssignableFrom(ji)) {
                    i.remove();
                    break;
                }
            }
        }
        
        Reflective [] rval = new Reflective[rset.size()];
        rset.toArray(rval);
        
        return rval;
    }

    /**
     * Find the "relevant invocations" of some class. That is, given the class,
     * find the generic types corresponding to that class which occur in the
     * given parameter list.<P>
     * 
     * This is "Inv" described in the JLS section 15.12.2.7
     *  
     * @param r       The class whose invocations to find
     * @param ubounds The parameter list to search
     * @return        A list of generic types all based on the class r
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeClass [] relevantInvocations(Reflective r, GenTypeSolid [] ubounds)
    {
        ArrayList<GenTypeClass> rlist = new ArrayList<GenTypeClass>();
        for (int i = 0; i < ubounds.length; i++) {
            GenTypeClass [] blist = ubounds[i].getReferenceSupertypes();
            for (int j = 0; j < blist.length; j++) {
                rlist.add(blist[j].mapToSuper(r.getName()));
            }
        }
        return rlist.toArray(new GenTypeClass[rlist.size()]);
    }
    
    @Override
    public GenTypeSolid[] getIntersectionTypes()
    {
        return new GenTypeSolid[] {this};
    }
}
