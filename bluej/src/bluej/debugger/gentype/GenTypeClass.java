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
 * @version $Id: GenTypeClass.java 3102 2004-11-18 01:39:18Z davmac $
 */
public class GenTypeClass extends GenTypeSolid {

    // ---------- Instance fields -----------
    
    protected List params = null; // List of GenTypeParameterizable's: type parameters
    protected Reflective reflective = null;
    
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
     * New GenTypeClass from a reflective and map of type parameter names to
     * types. For type parameters not in the map, the base type is used (as
     * a wilcard with "extends" clause). If the map is null however, the type
     * is treated as a raw type.
     * 
     * @param r  The Reflective representing the class.
     * @param mparams  A list of GenTypeParameterizables giving the type
     *                  parameters in declaration order
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
            if(mparams.get(next.getTparName()) == null)
                params.add(new GenTypeExtends(next.getBound()));
            else
                params.add(mparams.get(next.getTparName()));
        }
        if( params.isEmpty() )
            params = null;
    }
    
    // ---------- instance methods -------------

    public GenTypeClass asClass()
    {
        return this;
    }
    
    /**
     * Get the raw name of the type.
     * @return the raw name
     */
    public String rawName()
    {
        return reflective.getName();
    }
    
    /**
     * Get the type parameters as a string, for instance:<p>
     *    &lt;? extends java.lang.Object, java.lang.Thread&gt;
     * @return The type parameter string including angle brackets
     *         (empty string if no parameters)
     */
    public String getParamString()
    {
        if( params == null )
            return "";
        
        StringBuffer sb = new StringBuffer();
        sb.append("<");
        
        Iterator i = params.iterator();
        while( i.hasNext() ) {
            GenType next = (GenType)i.next();
            sb.append(next.toString());
            if( i.hasNext() )
                sb.append(",");
        }
        
        sb.append(">");
        return sb.toString();
    }
    
    /**
     * Check whether the type is a generic type (with type parameters).
     * Returns false for parameterless types and raw types.
     * 
     * @return  true if the type has type parameters
     */
    public boolean isGeneric()
    {
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
        return getMap() == null;
    }

    public boolean isInterface()
    {
        return reflective.isInterface();
    }
    
    public String toString(boolean stripPrefix)
    {
        String baseClass = rawName();
        if( stripPrefix )
            baseClass = JavaNames.stripPrefix(baseClass);
        
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
    
    public String toString(NameTransform nt)
    {
        String baseClass = nt.transform(rawName());
        
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
        if (other.getClass() != GenTypeClass.class)
            return false;
        
        GenTypeClass oClass = (GenTypeClass)other;
        
        // the class name must match
        if (! rawName().equals(oClass.rawName()))
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
            Map m = c.mapToSuper(reflective.getName());
            GenTypeClass other = new GenTypeClass(reflective, m);
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
     * Return the corresponding type if all type parameters are replaced with
     * corresponding actual types, as defined by a Map (String -> GenType).
     * 
     * @param tparams  A map definining the translation from type parameter
     *                 name (String) to its actual type (GenType).
     * @return the corresponding type structure, with parameters mapped.
     */
    public GenType mapTparsToTypes(Map tparams)
    {
        // If there are no generic parameters, there's nothing to map...
        if( params == null )
            return this;
        
        // Otherwise map each parameter, return the result.
        Iterator i = params.iterator();
        List retlist = new ArrayList();
        while( i.hasNext() ) {
            retlist.add(((GenTypeParameterizable)i.next()).mapTparsToTypes(tparams));
        }
        return new GenTypeClass(reflective, retlist);
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
    
    /**
     * Get a map of type parameter names to the corresponding types, for this
     * type. The returned map is a mutable copy, that is, it can freely be
     * modified by the caller without affecting this GenTypeClass object. 
     * 
     * Returns null if this represents a raw type.
     * 
     * @return the map (of String -> GenTypeParameterizable).
     */
    public Map getMap()
    {
        List formalParams = reflective.getTypeParams();
        if( params == null && ! formalParams.isEmpty())
            return null;
        HashMap r = new HashMap();
        if( params == null )
            return r;
        Iterator paramIterator = params.iterator();
        Iterator formalIterator = formalParams.iterator();
        
        // go through each type parameter, assign it the type from our
        // params list.
        while( paramIterator.hasNext() ) {
            GenType paramType = (GenType)paramIterator.next();
            GenTypeDeclTpar formalType = (GenTypeDeclTpar)formalIterator.next();
            
            String paramName = formalType.getTparName();
            r.put(paramName, paramType);
        }
        return r;
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
    public void getParamsFromTemplate(Map r, GenTypeParameterizable template)
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
                Iterator i = params.iterator();
                Iterator j = classTemplate.params.iterator();

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
                    Map m = uboundClass.mapToDerived(reflective);
                    getParamsFromTemplate(r, new GenTypeClass(reflective, m));
                }
            }

            for (int i = 0; i < lbounds.length; i++) {
                if (lbounds[i] instanceof GenTypeClass) {
                    GenTypeClass lboundClass = (GenTypeClass) lbounds[i];
                    Map m = lboundClass.mapToSuper(reflective.getName());
                    getParamsFromTemplate(r, new GenTypeClass(reflective, m));
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
    
    /**
     * For all type parameters which don't already occur in the given Map,
     * add them in as the "default" given by the class definition. For instance
     * with List&lt;T&gt;, add in the mapping "T : ? extends Object".
     * @param tparams       The map (String -> GenTypeClass)
     * @param declaringType the type for which to add default mappings
     */
    public static void addDefaultParamBases(Map tparams, Reflective declaringType)
    {
        Iterator i = declaringType.getTypeParams().iterator();
        
        while( i.hasNext() ) {
            GenTypeDeclTpar tpar = (GenTypeDeclTpar)i.next();
            
            String paramName = tpar.getTparName();

            GenTypeSolid bound = tpar.getBound();
            GenTypeWildcard type = new GenTypeExtends(bound);
            if( ! tparams.containsKey(paramName)) {
                tparams.put(paramName, type);
            }
        }
        return;
    }
    
    public GenTypeClass[] getUpperBoundsC()
    {
        return new GenTypeClass [] {this};
    }
    
    public GenTypeSolid[] getLowerBounds()
    {
        return new GenTypeSolid [] {this};
    }

    /**
     * Find the common bases between two classes, and add them into the given
     * list. 
     */
    protected static void getCommonBases(GenTypeClass a, GenTypeClass b, List r)
    {
        Reflective [] ra = new Reflective [] {a.reflective};
        Reflective [] rb = new Reflective [] {b.reflective};
        Set checked = new HashSet(); // keep track of type already added
        
        while (ra.length != 0 && rb.length != 0) {
            
            ArrayList newRa = new ArrayList();
            ArrayList newRb = new ArrayList();

            for (int i = 0; i < ra.length; i++) {
                
                if (checked.contains(ra[i]))
                    continue;
                
                for (int j = 0; j < rb.length; j++) {
                    // skip already checked elements
                    if (checked.contains(rb[i]))
                        continue;
                    
                    if (ra[i].isAssignableFrom(rb[i])) {
                        // TODO are they really common bases? must find the
                        // tpar gcd, but beware of infinite recursion!
                        Map m = a.mapToSuper(ra[i].getName());
                        r.add(new GenTypeClass(ra[i], m));
                        checked.add(ra[i]);

                        if (! rb[i].equals(ra[i]))
                            newRb.addAll(rb[i].getSuperTypesR());
                    }
                    else if (rb[i].isAssignableFrom(ra[i])) {
                        if (! checked.contains(rb[i])) {
                            Map m = a.mapToSuper(rb[i].getName());
                            r.add(new GenTypeClass(rb[i], m));
                            checked.add(rb[i]);
                        }
                        if (! ra[i].equals(rb[i]))
                            newRa.addAll(ra[i].getSuperTypesR());
                    }
                    else {
                        // Add the supertypes of ra[i] into newRa, and the
                        // supertypes of rb[i] into newRb
                        newRa.addAll(ra[i].getSuperTypesR());
                        newRb.addAll(rb[i].getSuperTypesR());
                    }
                }
            }
            
            ra = (Reflective []) newRa.toArray(new Reflective[0]);
            rb = (Reflective []) newRb.toArray(new Reflective[0]);
        }
    }
}
