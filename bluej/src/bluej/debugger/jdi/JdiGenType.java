package bluej.debugger.jdi;

import java.util.*;

import bluej.utility.Debug;
import bluej.utility.JavaNames;

import com.sun.jdi.*;

/* Represent a generic type (class, and list of type parameters).
 * This class makes heavy use of java 1.5 API.
 * 
 * Objects of this type are immutable.
 * 
 * @author Davin McCall
 * @version $Id: JdiGenType.java 2547 2004-05-26 05:17:29Z davmac $
 */
public class JdiGenType {

    protected final String baseClass; // the raw type. for List<Integer>, this
                                       // is "List".
    protected List params = null; // List of JdiGenType's: type parameters
    
    protected JdiGenType()
    {
        baseClass = null;
        // empty constructor
    }
    
    protected JdiGenType(String baseClass)
    {
        this.baseClass = baseClass;
    }
    
    private JdiGenType(String baseClass, List params)
    {
        this.baseClass = baseClass;
        this.params = params;
    }
    
    public final String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        String baseClass = this.baseClass;
        if( stripPrefix )
            baseClass = JavaNames.stripPrefix(baseClass);
        
        if( params == null )
            return baseClass;
        String r = baseClass + '<';
        for(Iterator i = params.iterator(); i.hasNext(); ) {
            r += ((JdiGenType)i.next()).toString(stripPrefix);
            if( i.hasNext() )
                r += ',';
        }
        r += '>';
        return r;
    }

    /**
     * Factory for producing a JdiGenType object from a field generic
     * signature. It can be safely called for a Field without a generic sig.
     */
    public static JdiGenType fromField(Field f, JdiObject parent)
    {
        final String gensig = f.genericSignature();
        if( gensig == null )
            return new JdiGenType(f.typeName());
        
        // generic version.
        // Get the mapping of type parameter names to actual types. Then,
        // map the names to the class/interface the field is actually defined
        // in.
        
        Map tparams = ((JdiObject15)parent).getGenericParams();
        if( tparams == null )
            tparams = new HashMap();
        addDefaultParamBases(tparams, ((JdiObject15)parent).getClassType());
        mapGenericParamsToBase(tparams,
                        (ClassType)parent.getObjectReference().referenceType(),
                        f.declaringType().name());
        addDefaultParamBases(tparams, (ClassType)f.declaringType());
        StringIterator iterator = new StringIterator(gensig);
        
        return fromSignature(iterator, tparams);
    }
    
    /**
     * Generate a JdiGenType structure from a type specification found in a
     * signature string, optionally mapping type parameter names to their
     * actual types.
     * 
     * @param i        a StringIterator through the signature string
     * @param tparams  a mapping of type parameter names to types (or null)
     * @return
     */
    public static JdiGenType fromSignature(StringIterator i, Map tparams)
    {
        char c = i.next();
        if( c == '*' ) {
            // '*' represents an unbounded '?'. For instance, List<?>
            // has signature:   Ljava/lang/list<*>;
            return new JdiGenTypeUnbounded();
        }
        if( c == '+' ) {
            // ? extends ...
            JdiGenType t = fromSignature(i, tparams);
            return new JdiGenTypeExtends(t);
        }
        if( c == '-' ) {
            // ? super ...
            JdiGenType t = fromSignature(i, tparams);
            return new JdiGenTypeSuper(t);
        }
        if( c == '[' ) {
            // array
            JdiGenType t = fromSignature(i, tparams);
            return new JdiGenTypeArray(t);
        }
        if( c == 'T' ) {
            // type parameter
            String tname = readClassName(i);
            if( tparams != null && tparams.get(tname) != null )
                return (JdiGenType)tparams.get(tname);
            else
                return new JdiGenTypeTpar(tname);
        }
        if( c == 'I') {
            // integer
            return new JdiGenTypeInt();
        }
        if( c == 'C') {
            // character
            return new JdiGenTypeChar();
        }
        if( c == 'Z') {
            // boolean
            return new JdiGenTypeBool();
        }
        if( c == 'B' ) {
            // byte
            return new JdiGenTypeByte();
        }
        if( c == 'S' ) {
            // short
            return new JdiGenTypeShort();
        }
        if( c == 'J' ) {
            // long
            return new JdiGenTypeLong();
        }
        if( c == 'F' ) {
            // float
            return new JdiGenTypeFloat();
        }
        if( c == 'D' ) {
            // double
            return new JdiGenTypeDouble();
        }
        
        if( c != 'L' ) {
            Debug.message("Generic signature begins without 'L'?? (got " + c + ")");
            return null;
        }
        String basename = readClassName(i);
        c = i.current();
        if( c == ';' )
            return new JdiGenType(basename);
        
        List params = new ArrayList();
        if( c != '<' ) {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }
        
        do {
            JdiGenType ptype = fromSignature(i, tparams);
            if( ptype == null )
                return null;
            params.add(ptype);
        } while( i.peek() != '>' );
        i.next(); // fetch the trailing ';'
        
        return new JdiGenType(basename, params);
    }

    /**
     * Factory method to construct a JdiGenType from a class signature and
     * a map of param_name -> type.
     * @param c        The class type
     * @param params   The parameter map
     * @return The constructed JdiGenType
     */
    public static JdiGenType fromClassSignature(ReferenceType c, Map params)
    {
        String gensig = c.genericSignature();
        StringIterator s = new StringIterator(gensig);
        
        if( s.next() != '<' ) {
            Debug.message("JdiGenType.fromClassSignature: no '<' at start of class sig??");
            return null;
        }
        
        // Create a list of type parameters
        List lparams = new LinkedList();
        while( s.current() != '>' ) {
            String tparName = readClassName(s);
            // Double colon means the tpar extends an interface
            if( s.peek() == ':' )
                s.next();
            // Map to the base type unless more specific info exists.
            JdiGenType baseType = fromSignature(s, params);
            if( params.containsKey(tparName))
                baseType = (JdiGenType)params.get(tparName);
            lparams.add(baseType);
            s.next();
        }
            
        return new JdiGenType(c.name(), lparams);
    }
    
    /**
     * Read a class name or type parameter name from a signature string.
     * @param i  An iterator into the string
     * @return   the fully qualified class name
     */
    private static String readClassName(StringIterator i)
    {
        char c = i.next();
        String r = new String();
        while( c != '<' && c != ';' && c != ':' ) {
            if( c == '/' )
                r += '.';
            else
                r += c;
            c = i.next();
        }
        return r;
    }
    
    /**
     * Map the type parameter names from a supertype to the names
     * used in the base type. For instance, if A<T> extends B<U>, Then to map
     * an instance of A<Integer> to B:
     * - pass in A as supertype,
     * - B as base type,
     * - and {T:Integer} as the map;
     * On return the map is {U:Integer}.
     * @param tparams  A Map of String -> JdiGenType
     * @param subType   the supertype to map from
     * @param basename    the fully-qualified name of the base type to map to
     * @return  True if successful, false otherwise (inheritance chain doesn't
     *          exist)
     */
    protected static boolean mapGenericParamsToBase(Map tparams,
            ReferenceType subType, String basename)
    {
        if( subType.name().equals(basename))
            return true;
        
        // the base type could actually be an interface, or a base class. 
        Stack inheritanceStack = getInheritanceChain(subType, basename);
        if( inheritanceStack == null )
            return false;
        
        String bname;
        Iterator i = inheritanceStack.iterator();
        i.next();  // skip the topmost class, we've already got that.
        ReferenceType baseType;
        do {
            baseType = (ReferenceType)i.next();
            bname = baseType.name();
            mapGenericParamsToDirectBase(tparams, subType, baseType);
            subType = baseType;
        } while( ! bname.equals(basename) );
        return true;
    }

    /**
     * Worker function for mapGenericParamsToBase. Maps only to a direct base.
     * @param tparams   a Map of String -> JdiGenType
     * @param subType   the derived type
     * @param baseType  the base type
     */
    private static void mapGenericParamsToDirectBase(Map tparams,
            ReferenceType subType, ReferenceType baseType)
    {
        // A generic signature for a type looks something like:
        //    <..type params..>Lbase/class<...>;...interfaces...
        // First, skip over the type params in the supertype:
        StringIterator s = new StringIterator(subType.genericSignature());        
        if( s.next() != '<' ) {
            Debug.message("mapGenericParamsToBase: didn't see '<' ??");
            return;
        }
        int lbCount = 1;
        while( lbCount > 0 ) {
            char c = s.next();
            if( c == '<') lbCount++;
            else if(c == '>') lbCount--;
        }
        readClassName(s);
        
        HashMap newMapping = new HashMap();
        StringIterator b = new StringIterator(baseType.genericSignature());
        // StringIterator b = new StringIterator(basename);
        // s = new StringIterator(superType.genericSignature());
        char c = b.next();
        
        // Two cases where we can return early: the base isn't a generic type,
        // or the super inherits from the "raw" version of the base.
        if( c != '<')
            return;
        c = s.current();
        if( c == ';' ) {
            tparams.clear();
            return;
        }
        if( c != '<' ) {
            Debug.message("mapGenericParamsToBase: didn't see '<' at end of base-of-super?? (got "+c+")");
            tparams.clear();
            return;
        }
        
        do {
            // Find the name of the type parameter in the base class
            String bParamName = readClassName(b);
            c = b.current();
            if( c != ':') {
                Debug.message("mapGenericParamsToBase: ':' doesn't follow param name in base?? got " + c);
                Debug.message(" signature was :" + baseType.genericSignature());
                tparams.clear();
                tparams.putAll(newMapping);
                return;
            }
            // double colon indicates the lower bound is an interface, not a class
            if( b.peek() == ':' )
                b.next();
            // just skip over the lower bound type
            fromSignature(b, null);
            
            // find the first type parameter to the base class from the super class
            // eg in A<...> extends B<String,Integer>, this is "String".
            JdiGenType sParamType = fromSignature(s, tparams); // super parameter type
            
            if( sParamType == null )
                Debug.message( "sParamType == null !!");
            if( ! (sParamType instanceof JdiGenType) )
                Debug.message( "sParam ! instanceof JdiGenType!!");
            newMapping.put(bParamName, sParamType);
        } while(b.peek() != '>');
        
        tparams.clear();
        tparams.putAll(newMapping);
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
     * @return a Map of String -> JdiGenType
     */
    public Map mapToDerived(ReferenceType derivedType, JdiClassSource cl)
    {        
        // Get a map (parameter name -> type) for this class.
        if( derivedType.genericSignature() == null )
            return new HashMap();
        
        List l = new LinkedList();
        Map r = getMap(cl, l);
        
        // One simple class is when super class = this.
        if( derivedType.name().equals(baseClass))
            return r;
        
        // Construct a list (actually a stack) of classes from the
        // super type down to this type.
        Stack classes = getInheritanceChain(derivedType, baseClass);
        if( classes == null )
            return null;
        
        // This loop continuously pops the next superclass from the stack,
        // maps the parameter types across, and repeat until the stack is
        // empty (and the target superclass has therefore been mapped).
        classes.pop();
        while( ! classes.empty() ) {
            ReferenceType curClass = (ReferenceType)classes.pop();
            String curSig = curClass.genericSignature();
            HashMap newMap = new HashMap();
            List newList = new LinkedList();
           
            // We have a supertype and a basetype. The supertype inherits
            // directly from the basetype.
            //
            // 'l' is a list of type parameter names in the order in which they
            //     are declared in the base type.
            // 'r' is a map of names -> types for the basetype.
            //
            // 'newList' and 'newMap' correspond to 'l' and 'r' but are for the
            // supertype, not the basetype. The body of the loop adds entries to
            // these collections.
            //
            // At the end of this loop we set l = newList and r = newMap.
           
            // First read the type parameters in the current super type.
            // Put the names in "newList".
            StringIterator s = new StringIterator(curSig);
            if( s.next() != '<' )
                return null;
            while( s.current() != '>' ) {
                String paramName = readClassName(s);
                if( s.current() != ':' ) {
                    Debug.message("mapToSuper : no ':' following type parameter name in super signature?? got "+s.current());
                    Debug.message("paramName was:" + paramName);
                    return null;
                }
                // '::' indicates lower bound is an interface. Ignore.
                if( s.peek() == ':' )
                    s.next();
                fromSignature(s, null); // just read the bound and ignore it.
                newList.add(paramName);
                if( s.current() != ';' )
                    Debug.message("mapToSuper: strange character after type parameter??");
                s.next();
            }
           
            // Skip over the base class name
            if( s.next() != 'L' )
                Debug.message( "mapToSuper: no leading L before base class name??");
            readClassName(s);
            
            // Check that the super inherits from the generic version of base
            if( s.current() != '<' )
                return null;
            
            Iterator i = l.iterator();
           
            while( i.hasNext() ) {
                // Get the type of the argument, check if it's usable info.
                //           WARNING WARNING WARNING!!
                // Do not attempt to understand this code unless you have some
                // sort of headache remedy available.
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
                
                JdiGenType argType = fromSignature(s, null);
                JdiGenType srcType = (JdiGenType)r.get(i.next());
                JdiGenType mapType = srcType;

                if( ! (argType instanceof JdiGenTypeTpar)) {
                    if( srcType instanceof JdiGenTypeExtends ) {
                        // Inheritance of the form:
                        //     A<T> extends B<X>
                        // and an object of type:
                        //     B<? extends Y>.
                        // It follows that (if X != Y) X is a superclass or
                        // superinterface of Y. So we can map from base Y to super
                        // X.
                        ReferenceType ct = null;
                        srcType = ((JdiGenTypeExtends)srcType).baseType;
                        ct = cl.classByName(argType.baseClass);
                        Map m = srcType.mapToDerived(ct, cl);
                        mapType = fromClassSignature(ct, m);
                    }
                    else if( srcType instanceof JdiGenTypeSuper ) {
                        // Inheritance of the form:
                        //     A<T> extends B<X>
                        // and an object of type:
                        //     B<? super Y>.
                        // It follows that X is a subclass (or subinterface) of Y.
                        // So we can map from super Y to base X.
                        srcType = ((JdiGenTypeSuper)srcType).baseType;
                        ReferenceType ct = cl.classByName(srcType.baseClass);
                        ReferenceType argRefType = cl.classByName(argType.baseClass);
                        Map m = new HashMap();
                        mapGenericParamsToBase(m, ct, argType.baseClass);
                        mapType = fromClassSignature(argRefType, m);
                    }
                }
                argType.getParamsFromTemplate(newMap, mapType, cl);
            }
           
            r = newMap;
            l = newList;
        }
        
        return r;
    }
    
    /**
     * Retrieve a mapping of type parameter names to their actual types, for
     * this generic type.
     * @param cl  A class source (used to locate the generic signature)
     * @param l   Either null, or an empty list; in the latter case, the list
     *            will be filled with the parameter names, in order.
     * @return A Map of String -> JdiGenType.
     */
    protected Map getMap(JdiClassSource cl, List l)
    {
        HashMap r = new HashMap();
        StringIterator s = new StringIterator(cl.classByName(baseClass).genericSignature());
        Iterator paramIterator = params.iterator();
        
        char c = s.next();
        if( c != '<' ) {
            Debug.message("getMap : no '<' at beginning of this signature ??");
            return null;
        }
        
        // go through each type parameter, assign it the type from our
        // params list.
        while( paramIterator.hasNext() ) {
            String paramName = readClassName(s);
            if( s.current() != ':' ) {
                Debug.message("getMap : no ':' following type parameter name in super signature?? got "+s.current());
                return null;
            }
            // '::' indicates lower bound is an interface. Ignore.
            if( s.peek() == ':' )
                s.next();
            fromSignature(s, null); // just read the bound and ignore it.
            if( l != null )
                l.add(paramName);
            r.put(paramName, paramIterator.next());
        }
        return r;
    }
    
    /**
     * Retrieve a list of all immediate supertypes and superinterfaces of the
     * given reference type. For a class, this is the base class and all
     * directly implemented interfaces. For an interface, this is all the
     * superinterfaces.
     * @param rt   The reference type whose supertypes to retrieve
     * @return     A list of ReferenceTypes which are supertypes
     */
    private static List getSuperTypes(ReferenceType rt)
    {
        if( rt instanceof ClassType ) {
            List l = new LinkedList();
            l.addAll(((ClassType)rt).interfaces());
            if(((ClassType)rt).superclass() != null)
                l.add(((ClassType)rt).superclass());
            return l;
        }
        else if( rt instanceof InterfaceType ){
            // interface
            return ((InterfaceType)rt).superinterfaces();
        }
        else
            return new LinkedList();
    }
    
    /**
     * Determine the inheritance, or implementation chain between two different
     * types. For instance, if A extends B and B extends C, the chain between
     * A and C is "A,B,C". Likewise, if D implements E which extends F, the
     * chain between D and F is "D,E,F".
     */
    private static Stack getInheritanceChain(ReferenceType top, String bottom)
    {
        Stack r = new Stack();
        r.push(top);
        if( top.name().equals(bottom ))
            return r;
        
        // Go through each base/interface and try to discover the hieararchy
        List l = getSuperTypes(top);
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            ReferenceType next = (ReferenceType)i.next();
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
    protected void getParamsFromTemplate(Map r, JdiGenType template, JdiClassSource cl)
    {
        Iterator i = params.iterator();
        Iterator j = template.params.iterator();
        for( ; i.hasNext(); ) {
            ((JdiGenType)i.next()).getParamsFromTemplate(r, (JdiGenType)j.next(), cl);
        }
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
    protected JdiGenType precisify(JdiGenType other, JdiClassSource cl)
    {
        List l = new LinkedList();
        Iterator i = params.iterator();
        Iterator j = other.params.iterator();
        for( ; i.hasNext(); ) {
            l.add(((JdiGenType)i.next()).precisify((JdiGenType)j.next(), cl));
        }
        return new JdiGenType(baseClass,l);
    }
        
    /**
     * For all type parameters which don't already occur in the given Map,
     * add them in as the "default" given by the class definition. For instance
     * with List&lt;T&gt;, add in the mapping "T : ? extends Object".
     * @param tparams       The map (String -> JdiGenType)
     * @param declaringType the type for which to add default mappings
     */
    public static void addDefaultParamBases(Map tparams, ClassType declaringType)
    {
       String genType = declaringType.genericSignature();
       if( genType == null )
           return;
       StringIterator s = new StringIterator(genType);
       
       char c = s.next();
       if( c != '<' ) {
           Debug.message("addDefaultParamBases : no '<' at beginning of this signature ??");
           return;
       }
       
       while( c != '>' ) {
           String paramName = readClassName(s);
           if( s.current() != ':' ) {
               Debug.message("mapToSuper : no ':' following type parameter name in super signature??");
               return;
           }
           // '::' indicates lower bound is an interface. Ignore.
           if( s.peek() == ':' )
               s.next();
           JdiGenType type = fromSignature(s, null); // just read the bound and ignore it.
           type = new JdiGenTypeExtends(type);
           if( ! tparams.containsKey(paramName)) {
               tparams.put(paramName, type);
           }
           if( s.current() != ';' )
               Debug.message("addDefaultParamBases: no ';' following type parameter??");
           c = s.peek();
       }
       return;
    }
    
}

class JdiGenTypeUnbounded extends JdiGenType
{
    private String unboundedString = "?";
    public String toString(boolean stripPrefix)
    {
        return unboundedString;
    }
    protected JdiGenType precisify(JdiGenType other)
    {
        // Anything is more precise than this!
        return other;
    }
}

class JdiGenTypeExtends extends JdiGenType
{
    JdiGenType baseType;
    public JdiGenTypeExtends(JdiGenType baseType)
    {
        super();
        if( baseType instanceof JdiGenTypeExtends ) {
            baseType = ((JdiGenTypeExtends)baseType).baseType;
        }
        this.baseType = baseType;
    }
    public String toString(boolean stripPrefix)
    {
        return "? extends " + baseType.toString(stripPrefix);
    }
    protected JdiGenType precisify(JdiGenType other, JdiClassSource cl)
    {
        // If other is a "super", we have two bounds on the parameter, upper
        // and lower.
        if( other instanceof JdiGenTypeSuper)
            return new JdiGenTypeBounded(baseType, ((JdiGenTypeSuper)other).baseType);
        
        if( ! (other instanceof JdiGenTypeExtends) )
            return other;

        // if the baseType classes are the same, one could still be
        // more specific, for instance ? extends List<? extends Runnable>
        // compared to ? extends List<? extends Thread>.
        if( ((JdiGenTypeExtends)other).baseType.baseClass == baseType.baseClass ) {
            JdiGenType specializedBase = baseType.precisify(((JdiGenTypeExtends)other).baseType, cl);
            if( specializedBase == baseType )
                return this;
            else
                return new JdiGenTypeExtends(specializedBase);
        }
        
        // Here is an interesting situation. One of the baseTypes extends the
        // other. However the parameters for the less specific baseType may
        // actually be more specific. Compare:
        //       List<? extends Thread>      LinkedList<? extends Runnable>
        // First the less specific type's parameters must be mapped to the
        // more specific type, then the parameters must be precisified.
        
        JdiGenType subTypeJdi = ((JdiGenTypeExtends)other).baseType;
        ReferenceType subType = cl.classByName(((JdiGenTypeExtends)other).baseType.baseClass);
        Map mapping = mapToDerived(subType, cl);
        if( mapping == null ) {
            subTypeJdi = baseType;
            subType = cl.classByName(baseType.baseClass);
            mapping = ((JdiGenTypeExtends)other).baseType.mapToDerived(subType, cl);
        }
        
        JdiGenType newJdiType = fromClassSignature(subType, mapping);
        return new JdiGenTypeExtends(newJdiType.precisify(subTypeJdi, cl));
    }
}

class JdiGenTypeSuper extends JdiGenType
{
    JdiGenType baseType;
    public JdiGenTypeSuper(JdiGenType baseType)
    {
        super();
        this.baseType = baseType;
    }
    public String toString(boolean stripPrefix)
    {
        return "? super " + baseType.toString(stripPrefix);
    }
    protected JdiGenType precisify(JdiGenType other, JdiClassSource cl)
    {
        // If other is a "extends", we have two bounds on the parameter, upper
        // and lower.
        if( other instanceof JdiGenTypeExtends)
            return new JdiGenTypeBounded(((JdiGenTypeExtends)other).baseType, baseType);
        
        if( ! (other instanceof JdiGenTypeSuper) )
            return other;

        // if the baseType classes are the same, one could still be
        // more specific, for instance ? super List<? extends Runnable>
        // compared to ? super List<? extends Thread>.
        if( ((JdiGenTypeSuper)other).baseType.baseClass == baseType.baseClass ) {
            JdiGenType specializedBase = baseType.precisify(((JdiGenTypeSuper)other).baseType, cl);
            if( specializedBase == baseType )
                return this;
            else
                return new JdiGenTypeSuper(specializedBase);
        }
        
        // Here is an interesting situation. One of the baseTypes extends the
        // other. However the parameters for the less specific baseType may
        // actually be more specific. Compare:
        //       List<? extends Thread>      LinkedList<? extends Runnable>
        // First the less specific type's parameters must be mapped to the
        // more specific type, then the parameters must be precisified.
        
        JdiGenType superTypeJdi = baseType;
        ReferenceType superType = cl.classByName(baseType.baseClass);
        ReferenceType subType = cl.classByName(((JdiGenTypeSuper)other).baseType.baseClass);
        Map mapping = baseType.getMap(cl, null);
        boolean success = mapGenericParamsToBase(mapping, subType, baseType.baseClass);
                
        // The inheritance hiearchy is the other way around
        if( ! success ) {
            superTypeJdi = ((JdiGenTypeSuper)other).baseType;
            subType = cl.classByName(baseType.baseClass);
            superType = cl.classByName(((JdiGenTypeSuper)other).baseType.baseClass);
            mapping = ((JdiGenTypeSuper)other).baseType.getMap(cl, null);
            mapGenericParamsToBase(mapping, subType, ((JdiGenTypeSuper)other).baseType.baseClass);
        }
        
        JdiGenType newJdiType = fromClassSignature(superType, mapping);
        return new JdiGenTypeSuper(newJdiType.precisify(superTypeJdi, cl));
    }
}

// A type with both an upper and lower bound.
// This type doesn't occur naturally- it can't be specified in the Java
// language. But in some cases we can deduce the type of some object to be
// this.
class JdiGenTypeBounded extends JdiGenType
{
    JdiGenType upperBound;  // ? extends upperBound
    JdiGenType lowerBound;  // ? super lowerBound
    public JdiGenTypeBounded(JdiGenType upper, JdiGenType lower)
    {
        upperBound = upper;
        lowerBound = lower;
    }
    public String toString(boolean stripPrefix)
    {
        return "? extends " + upperBound.toString(stripPrefix) + " super "
                + lowerBound.toString(stripPrefix);
    }
    public JdiGenType precisify(JdiGenType other, JdiClassSource cl)
    {
        JdiGenTypeExtends myUpper = new JdiGenTypeExtends(upperBound);
        JdiGenTypeSuper myLower = new JdiGenTypeSuper(lowerBound);
        
        JdiGenTypeExtends otherUpper = null;
        if( other instanceof JdiGenTypeExtends )
            otherUpper = (JdiGenTypeExtends)other;
        else if( other instanceof JdiGenTypeBounded )
            otherUpper = new JdiGenTypeExtends(((JdiGenTypeBounded)other).upperBound);
        
        if( otherUpper != null )
            myUpper = (JdiGenTypeExtends)myUpper.precisify(otherUpper, cl);
        
        JdiGenTypeSuper otherLower = null;
        if( other instanceof JdiGenTypeSuper )
            otherLower = (JdiGenTypeSuper)other;
        else if( other instanceof JdiGenTypeBounded )
            otherLower = new JdiGenTypeSuper(((JdiGenTypeBounded)other).lowerBound);
        
        if( otherLower != null )
            myLower = (JdiGenTypeSuper)myLower.precisify(otherLower, cl);
        
        if( myUpper.baseType == upperBound && myLower.baseType == lowerBound )
            return this;
        else
            return new JdiGenTypeBounded(myUpper.baseType, myLower.baseType);
    }
}

class JdiGenTypeArray extends JdiGenType
{
    JdiGenType baseType;
    public JdiGenTypeArray(JdiGenType baseType)
    {
        super();
        this.baseType = baseType;
    }
    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    protected JdiGenType precisify(JdiGenType other, JdiClassSource cl)
    {
        // An array is an object. So List<Integer[]> is compatible with
        // List<? extends Object>, for instance. Also, any array extends an
        // array of a subtype.
        
        if( ! (other instanceof JdiGenTypeArray) )
            return this;
        
        JdiGenType newBaseType = baseType.precisify(((JdiGenTypeArray)other).baseType, cl);
        return new JdiGenTypeArray(newBaseType);
    }
}

class JdiGenTypeTpar extends JdiGenType
{
    public JdiGenTypeTpar(String parname)
    {
        super(parname);
    }
    public String getTparName()
    {
        return baseClass;
    }

    protected void getParamsFromTemplate(Map r, JdiGenType template, JdiClassSource cl)
    {
        // This object is a template parameter "T" and the template may
        // contain a more complete type such as Integer. So create the mapping:
        //      T:Integer
        // On the other hand, the template could contain an approximation of
        // a type, such as "? extends Runnable". That's ok in itself, but what
        // if there already exists a better mapping for T (such as
        // "? extends Thread")?? - we shouldn't overwrite it.
        //
        // This could occur when mapping parameters from a template as
        // follows:
        //     Type to map     Template type
        //     AClass<T,T>     AClass<? extends Runnable, ? extends Thread>
        
        JdiGenType existing = (JdiGenType)r.get(baseClass);
        if( existing != null )
            template = template.precisify(existing, cl);
        r.put(baseClass, template);
    }
}

class JdiGenTypeInt extends JdiGenType
{
    public JdiGenTypeInt()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "int";
    }
}

class JdiGenTypeChar extends JdiGenType
{
    public JdiGenTypeChar()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "char";
    }
}

class JdiGenTypeBool extends JdiGenType
{
    public JdiGenTypeBool()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "boolean";
    }
}

class JdiGenTypeByte extends JdiGenType
{
    public JdiGenTypeByte()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "byte";
    }
}

class JdiGenTypeShort extends JdiGenType
{
    public JdiGenTypeShort()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "short";
    }
}

class JdiGenTypeLong extends JdiGenType
{
    public JdiGenTypeLong()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "long";
    }
}

class JdiGenTypeFloat extends JdiGenType
{
    public JdiGenTypeFloat()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "float";
    }
}

class JdiGenTypeDouble extends JdiGenType
{
    public JdiGenTypeDouble()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "double";
    }
}


class StringIterator {
    int i = 0;
    String s;
    
    public StringIterator(String s)
    {
        this.s = s;
        if( s == null )
            Debug.message("StringIterator with null string??");
    }
    
    public char current()
    {
        return s.charAt(i-1);
    }

    public char next()
    {
        return s.charAt(i++);
    }
    
    public char peek()
    {
        return s.charAt(i);
    }

    public boolean hasNext()
    {
        return i < s.length();
    }
};