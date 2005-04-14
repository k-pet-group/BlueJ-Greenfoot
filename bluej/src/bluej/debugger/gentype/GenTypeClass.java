package bluej.debugger.gentype;

import java.util.*;

import bluej.utility.JavaNames;

/**
 * Represent a (possibly generic) type. This can include wildcard types,
 * type parameters, etc; ie. anything that JDK 1.5 "Type" can represent. But 
 * this works for java 1.4 as well...
 * 
 * Objects of this type are immutable.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeClass.java 3347 2005-04-14 02:00:15Z davmac $
 */
public class GenTypeClass extends GenTypeSolid {

    // ---------- Instance fields -----------
    
    protected List params = null; // List of GenTypeParameterizable's: type parameters
    protected Reflective reflective = null;
    protected GenTypeClass outer = null; // outer class of this class
    
    // ---------- Constructors -------------
    
    /**
     * Constructor for a non-generic class type.
     * 
     * @param r   The Reflective representing the class.
     */
    public GenTypeClass(Reflective r)
    {
        reflective = r;
    }
    
    /**
     * New GenTypeClass from a reflective and an ordered list of type parameters
     * 
     * @param r  The Reflective representing the class.
     * @param params  A list of GenTypeParameterizables giving the type
     *                  parameters in declaration order
     */
    public GenTypeClass(Reflective r, List params)
    {
        reflective = r;
        if( params != null && ! params.isEmpty() )
            this.params = params;
    }
    
    /**
     * New GenTypeClass from a reflective and an ordered list of type
     * parameters, representing an inner class of a specified class.
     * This should only be used if the outer class is generic AND this inner
     * class is a non-static inner class.<p>
     * 
     * <i>outer</i> may be null, to specify no outer class. In this case this
     * constructor is equivalent to GenTypeClass(r, params). 
     * 
     * @param r  The Reflective representing the class.
     * @param params  A list of GenTypeParameterizables giving the type
     *                  parameters in declaration order
     */
    public GenTypeClass(Reflective r, List params, GenTypeClass outer)
    {
        reflective = r;
        if( params != null && ! params.isEmpty() )
            this.params = params;
        this.outer = outer;
    }
    
    /**
     * New GenTypeClass from a reflective and map of type parameter names to
     * types. For type parameters not in the map, the base type is used (as
     * a wilcard with "extends" clause). If the map is null however, the type
     * is treated as a raw type.
     * 
     * @param r  The Reflective representing the class.
     * @param mparams  A map of String -> GenTypeParameterizable giving the
     *                 type parameters. The map may be modified (if it is not
     *                 empty) by this constructor.
     */
    public GenTypeClass(Reflective r, Map mparams)
    {
        reflective = r;
        
        // if mparams == null, this is a raw type. Nothing more to do.
        if (mparams == null)
            return;
        
        params = new ArrayList();
        Iterator declParmsI = r.getTypeParams().iterator();
        while( declParmsI.hasNext() ) {
            GenTypeDeclTpar next = (GenTypeDeclTpar)declParmsI.next();
            String nextName = next.getTparName();
            if(mparams.get(nextName) == null)
                params.add(new GenTypeExtends(next.getBound()));
            else {
                params.add(mparams.get(nextName));
                mparams.remove(nextName);
            }
        }
        if( params.isEmpty() )
            params = null;
        
        // If there are still entries in the map, they belong to an outer
        // class
        if (! mparams.isEmpty()) {
            String rName = r.getName();
            int p = rName.lastIndexOf('$');
            if (p == -1)
                return;
            String outerName = rName.substring(0, p);
            Reflective outerReflective = r.getRelativeClass(outerName);
            if (outerReflective != null) {
                outer = new GenTypeClass(outerReflective, mparams);
            }
        }
    }
    
    // ---------- instance methods -------------

    public GenTypeClass asClass()
    {
        return this;
    }
    
    public GenType getErasedType()
    {
        return new GenTypeClass(reflective);
    }
    
    /**
     * Get the raw name of the type. The name returned is encoded so that it
     * can be passed to a ClassLoader's "loadClass" method (ie, dots between
     * outer and inner class names are changed to $).
     * 
     * @return the raw name
     */
    public String rawName()
    {
        return reflective.getName();
    }
    
    public String arrayComponentName()
    {
        return "L" + rawName() + ";";
    }
    
    /**
     * Check whether the type is a generic type (with type parameters).
     * Returns false for parameterless types and raw types.
     * 
     * @return  true if the type has type parameters
     */
    public boolean isGeneric()
    {
        if (outer != null)
            return true;
        else
            return (params != null);
    }
    
    /**
     * Check whether this is a raw type, ie. a generic class type with no
     * type parameter substitutions supplied. (Return is false for a non-
     * generic class type).
     * 
     * @return  true if the type is a raw type
     */
    public boolean isRaw()
    {
        // Outer class should only have been specified if it is generic -
        // and it is not possible for the inner class to be raw if the outer
        // is not also raw (or, if it is, the outer should be treated as
        // raw anyway).
        if (outer != null)
            return false;
        List formalParams = reflective.getTypeParams();
        return params == null && ! formalParams.isEmpty();
    }

    public boolean isInterface()
    {
        return reflective.isInterface();
    }
    
    public String toString(boolean stripPrefix)
    {
        String baseClass = rawName();
        
        if (outer != null) {
            int i = baseClass.lastIndexOf('$');
            baseClass = outer.toString(stripPrefix) + '.' + baseClass.substring(i + 1);
        }
        else if( stripPrefix )
            baseClass = JavaNames.stripPrefix(baseClass);
        
        // Append type parameters, if any
        if( params == null )
            return baseClass;
        String r = baseClass + '<';
        for(Iterator i = params.iterator(); i.hasNext(); ) {
            r += ((GenTypeParameterizable)i.next()).toString(stripPrefix);
            if( i.hasNext() )
                r += ',';
        }
        r += '>';
        return r;
    }
    
    // transform is only applied to outermost class
    public String toString(NameTransform nt)
    {
        String baseClass = rawName();

        if (outer != null) {
            int i = baseClass.lastIndexOf('$');
            baseClass = outer.toString(nt) + '.' + baseClass.substring(i + 1);
        }
        else
            baseClass = nt.transform(baseClass);

        // Append type parameters, if any
        if(params == null)
            return baseClass;
        String r = baseClass + '<';
        for(Iterator i = params.iterator(); i.hasNext(); ) {
            r += ((GenTypeParameterizable)i.next()).toString(nt);
            if( i.hasNext() )
                r += ',';
        }
        r += '>';
        return r;
    }
    
    
    public boolean equals(GenTypeParameterizable other)
    {
        if (other == this)
            return true;
        if (other == null)
            return false;
        if (other.getClass() != GenTypeClass.class)
            return false;
        
        GenTypeClass oClass = (GenTypeClass)other;
        
        // the class name must match
        if (! rawName().equals(oClass.rawName()))
            return false;
        
        // outer class (if any) must match
        if (outer == null && oClass.outer != null)
            return false;
        if (outer != null)
            if (! outer.equals(oClass.outer))
                return false;
        
        if (params == null && oClass.params == null)
            return true;
        if (params == null && oClass.params != null)
            return false;
        if (params != null && oClass.params == null)
            return false;
        
        Iterator i = params.iterator();
        Iterator j = oClass.params.iterator();
        
        // All the parameter types must match...
        while( i.hasNext() ) {
            if( ! j.hasNext() )
                return false;
            GenTypeParameterizable iNext = (GenTypeParameterizable)i.next();
            GenTypeParameterizable jNext = (GenTypeParameterizable)j.next();
            if( ! iNext.equals(jNext) )
                return false;
        }
        
        // and there must be the same number of parameters
        if( j.hasNext() )
            return false;

        return true;
    }
    
    public Reflective getReflective()
    {
        return reflective;
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (! (t instanceof GenTypeClass))
            return false;
        
        GenTypeClass c = (GenTypeClass) t;
        Reflective r = c.reflective;

        // check the inheritance hierarchy
        if( getInheritanceChain(r, reflective.getName()) != null) {
            if (isRaw() || c.isRaw())
                return true;
            
            // need to check type parameters
            GenTypeClass other = c.mapToSuper2(reflective.getName());
            GenTypeClass precise = (GenTypeClass) precisify(other);
            
            if (precise == null)
                return false;
            
            // If after precisifying precise == other, then this type imposes
            // no additional restrictions not already part of other. So it's
            // a match.
            if (other.equals(precise))
                return true;
        }

        return false;
    }
    
    public boolean isAssignableFromRaw(GenType t)
    {
        if (! (t instanceof GenTypeClass))
            return false;
        
        GenTypeClass c = (GenTypeClass) t;
        Reflective r = c.reflective;

        // check the inheritance hierarchy
        if( getInheritanceChain(r, reflective.getName()) != null)
            return true;
        else
            return false;
    }
    
    /**
     * Map the type parameter in a base type to the types
     * used in the super type. For instance, if A<T> extends B<U>, Then to map
     * an instance of A<Integer> to B, pass "B" as the base type;
     * on return the map is {U:Integer}.
     * 
     * @param subType   the supertype to map from
     * @param basename    the fully-qualified name of the base type to map to
     * @return  A map of (String -> GenType)
     * 
     * @throws BadInheritanceChainException
     */
    public Map mapToSuper(String basename)
    {
        Map tparams = getMap();
        if (tparams == null)
            return null;
        if( rawName().equals(basename))
            return tparams;
        
        // the base type could actually be an interface, or a base class. 
        Stack inheritanceStack = getInheritanceChain(reflective, basename);
        if( inheritanceStack == null )
            throw new BadInheritanceChainException();
        
        String bname;
        Iterator i = inheritanceStack.iterator();
        i.next();  // skip the topmost class, we've already got that.
        Reflective baseType;
        Reflective subType = reflective;
        
        do {
            baseType = (Reflective)i.next();
            bname = baseType.getName();
            tparams = mapGenericParamsToDirectBase(tparams, subType, baseType);
            subType = baseType;
        } while( tparams != null && ! bname.equals(basename) );
        return tparams;
    }
    
    /**
     * Map the type parameters in a base type to a super type, apply the
     * resulting type parameters to the super type, and return the result.
     * 
     * For instance, if A&lt;T&gt; extends B&lt;U&gt;, Then to map an instance
     * of  A&lt;Integer&gt; to B, pass "B" as the super type; The return is
     * then B&lt;Integer&gt;.
     * 
     * @param subType   the supertype to map from
     * @param basename    the fully-qualified name of the base type to map to
     * 
     * @throws BadInheritanceChainException
     */
    public GenTypeClass mapToSuper2(String basename)
    {
        if( rawName().equals(basename))
            return this;
        
        // the base type could actually be an interface, or a base class. 
        Stack inheritanceStack = getInheritanceChain(reflective, basename);
        if( inheritanceStack == null )
            throw new BadInheritanceChainException();
        
        String bname;
        Iterator i = inheritanceStack.iterator();
        i.next();  // skip the topmost class, we've already got that.
        Reflective baseType;
        Reflective subType = reflective;
        GenTypeClass ccc = this;
        
        do {
            baseType = (Reflective)i.next();
            bname = baseType.getName();
            Map tparams = ccc.getMap();
            ccc = mapGenericParamsToDirectBase2(tparams, subType, baseType);
            subType = baseType;
        } while( ! bname.equals(basename) );
        return ccc;
    }

    /**
     * Worker function for mapGenericParamsToBase. Maps only to a direct base.
     * @param tparams   a Map of String -> GenTypeClass
     * @param subType   the derived type
     * @param baseType  the base type
     */
    private static Map mapGenericParamsToDirectBase(Map tparams,
            Reflective subType, Reflective baseType)
    {
        GenTypeClass baseClass = subType.superTypeByName(baseType.getName());
        if (baseClass.isRaw())
            return null;

        // The derived type inherits from the non-raw version of the base type.
        // This should usually be the case.
        baseClass = (GenTypeClass) baseClass.mapTparsToTypes(tparams);
        tparams.clear();
        tparams.putAll(baseClass.getMap());
        return tparams;
    }

    /**
     * Worker function for mapGenericParamsToBase. Maps only to a direct base.
     * @param tparams   a Map of String -> GenTypeClass
     * @param subType   the derived type
     * @param baseType  the base type
     */
    private static GenTypeClass mapGenericParamsToDirectBase2(Map tparams,
            Reflective subType, Reflective baseType)
    {
        GenTypeClass baseClass = subType.superTypeByName(baseType.getName());
        baseClass = (GenTypeClass) baseClass.mapTparsToTypes(tparams);
        return baseClass;
    }
    
    /**
     * Return the corresponding type if all type parameters are replaced with
     * corresponding actual types, as defined by a Map (String -> GenType).
     * 
     * @param tparams  A map definining the translation from type parameter
     *                 name (String) to its actual type (GenType). The map
     *                 can be null to return the raw type.
     * @return the corresponding type structure, with parameters mapped.
     */
    public GenType mapTparsToTypes(Map tparams)
    {
        // If tparams is null, return the erased type
        if (tparams == null)
            return getErasedType();
        
        // If there are no generic parameters, there's nothing to map...
        if( params == null && outer == null )
            return this;
        
        // Otherwise map each parameter, return the result.
        List retlist = new ArrayList();
        if (params != null) {
            Iterator i = params.iterator();
            while( i.hasNext() ) {
                retlist.add(((GenTypeParameterizable)i.next()).mapTparsToTypes(tparams));
            }
        }
        
        GenTypeClass newOuter = null;
        if (outer != null)
            newOuter = (GenTypeClass) outer.mapTparsToTypes(tparams);
        
        return new GenTypeClass(reflective, retlist, newOuter);
    }
    
    /**
     * Get a map of template parameter names to types, if this type was mapped
     * to the given supertype. For example, if A&lt;T&gt; extends B&lt;T&gt;, and this type
     * is B&lt;Integer&gt; and the given derivedType is A, then return the map:
     * {T:Integer}.<p>
     * 
     * A may extend B directly, indirectly (A extends C, C extends B) 
     * or by virtue of A == B.<p>
     * 
     * The returned Map might not contain an entry for every type parameter in
     * the supertype. The return is null if A does not extend/implement B.<p>
     * 
     * @param derivedType  the super type; must be a generic type
     * @return a Map of String -> GenTypeClass
     */
    public Map mapToDerived(Reflective derivedType)
    {        
        // Get a map (parameter name -> type) for this class.
        if( derivedType.getTypeParams().isEmpty() || ! isGeneric() )
            return new HashMap();
        
        Map r = getMap();
        if (r == null)
            r = new HashMap();
        List l = new LinkedList();
        for( Iterator i = reflective.getTypeParams().iterator(); i.hasNext(); ) {
            String paramName = ((GenTypeTpar)i.next()).getTparName();
            l.add(paramName);
        }
        
        // One simple class is when super class = this.
        if( derivedType.getName().equals(rawName()))
            return r;
        
        // Construct a list (actually a stack) of classes from the
        // super type down to this type.
        Stack classes = getInheritanceChain(derivedType, rawName());
        if( classes == null )
            return null;
        
        // This loop continuously pops the next superclass from the stack,
        // maps the parameter types across, and repeat until the stack is
        // empty (and the target superclass has therefore been mapped).
        Reflective curBase = (Reflective)classes.pop();
        while( ! classes.empty() ) {
            Reflective curSubtype = (Reflective)classes.pop();
            HashMap newMap = new HashMap();
            List newList = new LinkedList();
           
            // We have a subtype and a basetype. The subtype inherits
            // directly from the basetype.
            //
            // 'l' is a list of type parameter names in the order in which they
            //     are declared in the base type.
            // 'r' is a map of names -> types for the basetype.
            //
            // 'newList' and 'newMap' correspond to 'l' and 'r' but are for the
            // subtype, not the basetype. The body of the loop adds entries to
            // these collections.
            //
            // At the end of this loop we set l = newList and r = newMap.
           
            // First read the type parameters in the current sub type.
            // Put the names in "newList".

            for( Iterator i = curSubtype.getTypeParams().iterator(); i.hasNext(); ) {
                String paramName = ((GenTypeTpar)i.next()).getTparName();
                newList.add(paramName);
            }
           
            // Check that the super inherits from the generic version of base
            GenTypeClass baseDecl = curSubtype.superTypeByName(curBase.getName());
            if( baseDecl.params == null )
                // Note, must return a mutable map - not Collections.EMPTY_MAP
                return new HashMap();
            
            Iterator i = l.iterator();
            Iterator baseDeclI = baseDecl.params.iterator();
           
            while( i.hasNext() ) {
                // Get the type of the argument, check if it's usable info.
                //
                // We have some object which is of type B<X>, but we know that
                // that the concrete type is really A (a subtype of B). So,
                // we want to take the information we have about X and apply it
                // to A.
                //
                // Consider these cases:
                //   A<T> extends B<T>;     // nice and easy
                //   A<T> extends B<C<T>>;  // not so easy
                // Just to complicate matters, we may not know exactly what X
                // is. It could be "? extends Y" or "? super Y" for instance.
                //
                // In the first example argType = T, srcType = X.
                // In the second, argType = C<T>, srcType = X. X is then
                //                 implied to be of type C<?>.
                
                GenTypeParameterizable argType = (GenTypeParameterizable)baseDeclI.next();
                GenTypeParameterizable srcType = (GenTypeParameterizable)r.get(i.next());
                if( srcType != null )
                    argType.getParamsFromTemplate(newMap, srcType);
            }
           
            r = newMap;
            l = newList;
            curBase = curSubtype;
        }
        
        return r;
    }
    
    public GenTypeParameterizable mapToDerived2(Reflective derivedType)
    {        
        // Get a map (parameter name -> type) for this class.
        if(! isGeneric() )
            return new GenTypeClass(derivedType);
        
        // One simple class is when super class = this.
        // TODO don't use class names as equality test
        if( derivedType.getName().equals(rawName()))
            return this;
        
        // Construct a list (actually a stack) of classes from the
        // super type down to this type.
        Stack classes = getInheritanceChain(derivedType, rawName());
        if( classes == null )
            return null;
        
        // This loop continuously pops the next superclass from the stack,
        // maps the parameter types across, and repeat until the stack is
        // empty (and the target superclass has therefore been mapped).
        // Reflective curBase = (Reflective)classes.pop();
        GenTypeClass curBaseC = this;
        classes.pop();
        
        while( ! classes.empty() ) {
            Reflective curSubtype = (Reflective)classes.pop();
            HashMap newMap = new HashMap();
           
            // Check that the super inherits from the generic version of base
            GenTypeClass baseDecl = curSubtype.superTypeByName(curBaseC.rawName());
            if (baseDecl.isRaw())
                return new GenTypeClass(derivedType);
            
            // Get the mapping of tpar names to types
            baseDecl.getParamsFromTemplate(newMap, curBaseC);
            
            // Apply the mapping to the subtype
            curBaseC = new GenTypeClass(curSubtype, newMap);
        }
        
        return curBaseC;
    }

    /**
     * Get a map of type parameter names to the corresponding types, for this
     * type. The returned map is a mutable copy, that is, it can freely be
     * modified by the caller without affecting this GenTypeClass object. 
     * 
     * Returns null if this represents a raw type.
     * 
     * Note that a map is not enough to completely describe a type, due to
     * the possibility of inner/outer types having type parameters with the
     * same name.
     * 
     * @return the map (of String -> GenTypeParameterizable).
     */
    public Map getMap()
    {
        if (isRaw())
            return null;
        
        HashMap r = new HashMap();
        mergeMap(r);
        return r;
    }
    
    /**
     * Get a map of type parameter names to the corresponding types, for this
     * type. Existing entries in the map will be overwritten. 
     * 
     * The returned does not indicate if this type is a raw type.
     */
    public void mergeMap(Map m)
    {
        if (outer != null)
            outer.mergeMap(m);

        List formalParams = reflective.getTypeParams();
        if( params == null )
            return;
        Iterator paramIterator = params.iterator();
        Iterator formalIterator = formalParams.iterator();
        
        // go through each type parameter, assign it the type from our
        // params list.
        while( paramIterator.hasNext() ) {
            GenType paramType = (GenType)paramIterator.next();
            GenTypeDeclTpar formalType = (GenTypeDeclTpar)formalIterator.next();
            
            String paramName = formalType.getTparName();
            m.put(paramName, paramType);
        }
    }
    
    /**
     * Determine the inheritance, or implementation chain between two different
     * types. For instance, if A extends B and B extends C, the chain between
     * A and C is "A,B,C". Likewise, if D implements E which extends F, the
     * chain between D and F is "D,E,F".
     * 
     * returns a Stack of ReflectiveType.
     */
    private static Stack getInheritanceChain(Reflective top, String bottom)
    {
        Stack r = new Stack();
        r.push(top);
        if( top.getName().equals(bottom ))
            return r;
        
        // Go through each base/interface and try to discover the hieararchy
        List l = top.getSuperTypesR();
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            Reflective next = (Reflective)i.next();
            Stack r2 = getInheritanceChain(next, bottom);
            if( r2 != null ) {
                r.addAll(r2);
                return r;
            }
        }
        return null;
    }
    
    /**
     * This determines the mappings from type parameter names to their actual
     * types, using the provided template. For instance, to map
     *   List&lt;T&gt;     via template      List&lt;Integer&gt;
     * would yield "T:Integer".
     * 
     * This method is overridden in subclasses.
     * 
     * @param r  the Map to put the entries in. 
     * @see bluej.debugger.gentype.GenTypeParameterizable#getParamsFromTemplate(java.util.Map, bluej.debugger.gentype.GenTypeParameterizable)
     */
    protected void getParamsFromTemplate(Map r, GenTypeParameterizable template)
    {
        // We are classA<...>, template could be anything.
        // possibilities for template:
        //      classA<...>  - fine, just map the parameters seperately
        //      ?            - can't map anything
        //      ? extends classB<...> - first map classB to classA, then
        //                        map the parameters
        //           - same with super clause
        //           - with multiple bounds, map seperately and then precisify
        
        if (template instanceof GenTypeClass) {
            GenTypeClass classTemplate = (GenTypeClass) template;
            if (classTemplate.rawName().equals(rawName())) {
                if (params == null || classTemplate.params == null)
                    return;
                Iterator i = params.iterator();
                Iterator j = classTemplate.params.iterator();

                // Handle case that this is an inner class
                if (outer != null)
                    outer.getParamsFromTemplate(r, classTemplate.outer);
                
                // loop through each parameter
                while (i.hasNext() && j.hasNext()) {
                    GenTypeParameterizable ip = (GenTypeParameterizable) i.next();
                    GenTypeParameterizable jp = (GenTypeParameterizable) j.next();

                    ip.getParamsFromTemplate(r, jp);
                }
            }
        }
        else if (template instanceof GenTypeWildcard) {
            GenTypeWildcard wildcardTemplate = (GenTypeWildcard) template;

            // wildcard. Map each of the upper and lower bounds to this type,
            // and use the mapped type as a template in each case.

            GenTypeSolid[] ubounds = wildcardTemplate.getUpperBounds();
            GenTypeSolid[] lbounds = wildcardTemplate.getLowerBounds();

            for (int i = 0; i < ubounds.length; i++) {
                if (ubounds[i] instanceof GenTypeClass) {
                    GenTypeClass uboundClass = (GenTypeClass) ubounds[i];
                    //Map m = uboundClass.mapToDerived(reflective);
                    GenTypeClass uboundMapped = (GenTypeClass) uboundClass.mapToDerived2(reflective);
                    getParamsFromTemplate(r, uboundMapped);
                }
            }

            for (int i = 0; i < lbounds.length; i++) {
                if (lbounds[i] instanceof GenTypeClass) {
                    GenTypeClass lboundClass = (GenTypeClass) lbounds[i];
                    GenTypeClass m = lboundClass.mapToSuper2(reflective.getName());
                    getParamsFromTemplate(r, m);
                }
            }
        }

        return;
    }
    
    /**
     * "precisify". Return a type using the most specific information from this
     * type and the given template in each case. For instance if: this = Map <?
     * extends Runnable, ? extends Thread> other = Map <? extends Thread, ?
     * extends Runnable> then the result is: Map <? extends Thread, ? extends
     * Thread>
     */
    public GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // If "other" is not a GenTypeClass, let it do the work. (It's
        // probably a wildcard).
        if( ! (other instanceof GenTypeClass) )
            return other.precisify(this);
        
        // handle raw types gracefully
        if (params == null)
            return other;
        
        List l = new LinkedList();
        Iterator i = params.iterator();
        Iterator j = ((GenTypeClass)other).params.iterator();
        for( ; i.hasNext(); ) {
            l.add(((GenTypeParameterizable)i.next()).precisify((GenTypeParameterizable)j.next()));
        }
        return new GenTypeClass(reflective,l);
    }
        
    public GenTypeClass[] getUpperBoundsC()
    {
        return new GenTypeClass [] {this};
    }
    
    public GenTypeSolid[] getLowerBounds()
    {
        return new GenTypeSolid [] {this};
    }
}
