package bluej.debugger.gentype;

import java.util.*;

import bluej.utility.JavaNames;

/* Represent a (possibly generic) type. This can include wildcard types,
 * type parameters, etc; ie. anything that JDK 1.5 "Type" can represent. But 
 * this works for java 1.4 as well...
 * 
 * Objects of this type are immutable.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeClass.java 2640 2004-06-21 05:08:18Z davmac $
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
     * types.
     * 
     * @param r  The Reflective representing the class.
     * @param mparams  A list of GenTypeParameterizables giving the type
     *                  parameters in declaration order
     */
    public GenTypeClass(Reflective r, Map mparams)
    {
        reflective = r;
        params = new ArrayList();
        Iterator declParmsI = r.getTypeParams().iterator();
        while( declParmsI.hasNext() ) {
            GenTypeDeclTpar next = (GenTypeDeclTpar)declParmsI.next();
            params.add(mparams.get(next.getTparName()));
        }
        if( params.isEmpty() )
            params = null;
    }
    
    // ---------- instance methods -------------

    public boolean isPrimitive()
    {
        return false;
    }
    
    public final String toString()
    {
        return toString(false);
    }

    /**
     * Get the raw name of the type.
     * @return the raw name
     */
    public String rawName()
    {
        return reflective.getName();
    }
    
    protected boolean isGeneric()
    {
        return (params == null);
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
    
    public boolean equals(GenTypeParameterizable other)
    {
        if( ! (other.getClass() != GenTypeClass.class))
            return false;
        
        GenTypeClass oClass = (GenTypeClass)other;
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
    
    /**
     * Map the type parameter names from a supertype to the names
     * used in the base type. For instance, if A<T> extends B<U>, Then to map
     * an instance of A<Integer> to B, pass "B" as the base type.
     * On return the map is {U:Integer}.
     * @param subType   the supertype to map from
     * @param basename    the fully-qualified name of the base type to map to
     * @return  A map of (String -> GenType), or null if the inheritance
     *              chain doesn't exist
     */
    public Map mapToSuper(String basename)
    {
        Map tparams = getMap();
        if( rawName().equals(basename))
            return tparams;
        
        // the base type could actually be an interface, or a base class. 
        Stack inheritanceStack = getInheritanceChain(reflective, basename);
        if( inheritanceStack == null )
            return null;
        
        String bname;
        Iterator i = inheritanceStack.iterator();
        i.next();  // skip the topmost class, we've already got that.
        Reflective baseType;
        Reflective subType = reflective;
        
        do {
            baseType = (Reflective)i.next();
            bname = baseType.getName();
            mapGenericParamsToDirectBase(tparams, subType, baseType);
            subType = baseType;
        } while( ! bname.equals(basename) );
        return tparams;
    }

    /**
     * Worker function for mapGenericParamsToBase. Maps only to a direct base.
     * @param tparams   a Map of String -> GenTypeClass
     * @param subType   the derived type
     * @param baseType  the base type
     */
    private static void mapGenericParamsToDirectBase(Map tparams,
            Reflective subType, Reflective baseType)
    {
        
        HashMap newMapping = new HashMap();
        
        // Cases where we can return early: the base isn't a generic type,
        // or the super inherits from the "raw" version of the base. Actually
        // only one test is needed for both cases.

        List baseParams = baseType.getTypeParams();
        if(baseParams.isEmpty())
            return;
        Iterator baseParamsI = baseParams.iterator();
        
        GenTypeClass baseClass = subType.superTypeByName(baseType.getName());
        baseClass = (GenTypeClass)baseClass.mapTparsToTypes(tparams);
        tparams.clear();
        tparams.putAll(baseClass.getMap());
    }
    
    /**
     * Return the corresponding type if all type parameters are replaced with
     * corresponding actual types, as defined by a Map (String -> GenType).
     * 
     * @param tparams  A map definining the translation from type parameter
     *                 name (String) to its actual type (GenType).
     * @return the corresponding type structure, with parameters mapped.
     */
    public GenTypeParameterizable mapTparsToTypes(Map tparams)
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
     * The returned Map may not contain an entry for every type parameter in
     * the supertype. The return is null if A does not extend/implement B.<p>
     * 
     * @param derivedType  the super type; must be a generic type
     * @return a Map of String -> GenTypeClass
     */
    public Map mapToDerived(Reflective derivedType)
    {        
        // Get a map (parameter name -> type) for this class.
        if( derivedType.getTypeParams().isEmpty() )
            return new HashMap();
        
        Map r = getMap();
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
                
                GenTypeParameterizable argType = (GenTypeParameterizable)baseDeclI.next();
                GenTypeParameterizable srcType = (GenTypeParameterizable)r.get(i.next());
                argType.getParamsFromTemplate(newMap, srcType);
            }
           
            r = newMap;
            l = newList;
        }
        
        return r;
    }
    
    /**
     * Get a map of type parameter names to the corresponding types, for this
     * type.
     * @return the map (of String -> GenTypeParameterizable).
     */
    protected Map getMap()
    {
        HashMap r = new HashMap();
        if( params == null )
            return r;
        List formalParams = reflective.getTypeParams();
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
     */
    protected void getParamsFromTemplate(Map r, GenTypeParameterizable template)
    {
        // TODO. Too complicated for now.
        return;
    }
    
    /**
     * "precisify". Return a type using the most specific information from
     * this type and the given template in each case.
     * For instance if:
     *     this  = Map<? extends Runnable, ? extends Thread>
     *     other = Map<? extends Thread, ? extends Runnable>
     * then the result is:
     *     Map<? extends Thread, ? extends Thread>
     */
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // If "other" is not a GenTypeClass, let it do the work. (It's
        // probably a wildcard).
        if( ! (other instanceof GenTypeClass) )
            return other.precisify(this);
        
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
            GenTypeExtends type = new GenTypeExtends(bound);
            if( ! tparams.containsKey(paramName)) {
                tparams.put(paramName, type);
            }
        }
        return;
    }
}
