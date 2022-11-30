/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2017,2018,2020  Michael Kolling and John Rosenberg 
 
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

import java.util.*;

/**
 * Represent a (possibly generic) type. This can include wildcard types,
 * type parameters, etc; ie. anything that JDK 1.5 "Type" can represent. 
 * 
 * Objects of this type are immutable.
 * 
 * @author Davin McCall
 */
public class GenTypeClass extends GenTypeSolid
{
    // ---------- Instance fields -----------
    
    /** List of type parameters; null if none (there may be an outer class with parameters) */
    protected List<? extends GenTypeParameter> params = null;
    protected Reflective reflective = null;
    /** Outer class, *iff* an outer class has type parameters; otherwise null */
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
    public GenTypeClass(Reflective r, List<GenTypeParameter> params)
    {
        this(r, params, null);
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
     * @param params  A list of GenTypeParameter giving the type
     *                  parameters in declaration order
     */
    public GenTypeClass(Reflective r, List<? extends GenTypeParameter> params, GenTypeClass outer)
    {
        reflective = r;
        if (params != null && ! params.isEmpty()) {
            this.params = params;
        }
        this.outer = outer;
    }
    
    /**
     * New GenTypeClass from a reflective and map of type parameter names to
     * types. For type parameters not in the map, the base type is used (as
     * a wilcard with "extends" clause). If the map is null however, the type
     * is treated as a raw type.
     * 
     * @param r  The Reflective representing the class.
     * @param mparams  A map of String -> GenTypeParameter giving the
     *                 type parameters. The map may be modified (if it is not
     *                 empty) by this constructor.
     */
    @OnThread(Tag.FXPlatform)
    public GenTypeClass(Reflective r, Map<String,GenTypeParameter> mparams)
    {
        reflective = r;
        
        // if mparams == null, this is a raw type. Nothing more to do.
        if (mparams == null) {
            return;
        }
        
        List<GenTypeParameter> params = new ArrayList<GenTypeParameter>();
        
        Iterator<GenTypeDeclTpar> declParmsI = r.getTypeParams().iterator();
        while( declParmsI.hasNext() ) {
            GenTypeDeclTpar next = declParmsI.next();
            String nextName = next.getTparName();
            if(mparams.get(nextName) == null)
                params.add(new GenTypeExtends(next.getBound()));
            else {
                params.add((GenTypeParameter) mparams.get(nextName));
                mparams.remove(nextName);
            }
        }
        if (params.isEmpty()) {
            params = null;
        }

        this.params = params;
        
        // If there are still entries in the map, they may belong to an outer class
        if (! mparams.isEmpty() && ! r.isStatic()) {
            Reflective outerReflective = r.getOuterClass();
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

    @OnThread(Tag.FXPlatform)
    public GenTypeClass getErasedType()
    {
        return new GenTypeClass(reflective);
    }
        
    /**
     * Get the name of the type as known to the classloader. The name returned is
     * encoded so that it can be passed to a ClassLoader's "loadClass" method (dots
     * between outer and inner class names are changed to '$', and arrays are
     * encoded).
     */
    public String classloaderName()
    {
        return reflective.getName();
    }
    
    public String arrayComponentName()
    {
        return "L" + classloaderName() + ";";
    }
    
    /**
     * Return an unmodifiable list of the type parameters applied to the
     * innermost class in this generic type. 
     */
    public List<? extends GenTypeParameter> getTypeParamList()
    {
        if (params == null)
            return Collections.emptyList();
        else
            return params;
    }
    
    /**
     * Get the containing type. If this is a generic type whose class is an
     * inner class, and an outer class is also generic, this will return the
     * outer class of the class type. NOTE, it will not necessarily work if
     * no outer class is generic, or if this is a raw type!
     */
    public GenTypeClass getOuterType()
    {
        return outer;
    }
    
    /**
     * Determine whether this class is an inner type. This works regardless
     * of whether outer types are generic etc.
     */
    public boolean isInnerType()
    {
        return reflective.getName().indexOf('$') != -1;
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
    @OnThread(Tag.FXPlatform)
    public boolean isRaw()
    {
        // Outer class should only have been specified if it is generic -
        // and it is not possible for the inner class to be raw if the outer
        // is not also raw (or, if it is, the outer should be treated as
        // raw anyway).
        if (outer != null || params != null) {
            return false;
        }
        
        Reflective r = reflective;
        List<?> formalParams = reflective.getTypeParams();
        while (formalParams.isEmpty()) {
            if (r.isStatic()) {
                return false;
            }
            r = r.getOuterClass();
            if (r == null) {
                return false;
            }
            formalParams = r.getTypeParams();
        }
        
        return true;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isInterface()
    {
        return reflective.isInterface();
    }
    
    // transform is only applied to outermost class
    public String toString(NameTransform nt)
    {
        String baseClass = classloaderName();

        if (outer != null) {
            int i = baseClass.lastIndexOf('$');
            baseClass = outer.toString(nt) + '.' + baseClass.substring(i + 1);
        }
        else {
            baseClass = nt.transform(baseClass);
            baseClass = baseClass.replace('$', '.');
        }

        // Append type parameters, if any
        if(params == null)
            return baseClass;
        String r = baseClass + '<';
        for(Iterator<? extends GenTypeParameter> i = params.iterator(); i.hasNext(); ) {
            r += i.next().toTypeArgString(nt);
            if( i.hasNext() )
                r += ',';
        }
        r += '>';
        return r;
    }
    
    public String toTypeArgString(NameTransform nt)
    {
        return toString(nt);
    }

    @OnThread(Tag.FXPlatform)
    public boolean equals(JavaType other)
    {
        if (other == this)
            return true;
        if (other == null)
            return false;
        
        GenTypeClass oClass = other.asClass();
        if (oClass == null) {
            return false;
        }
        
        JavaType arrayComponent = getArrayComponent();
        JavaType oarrayComponent = oClass.getArrayComponent();
        if (arrayComponent != null) {
            return arrayComponent.equals(oarrayComponent);
        }
        else if (oarrayComponent != null) {
            return false;
        }
        
        // the class name must match
        if (! classloaderName().equals(oClass.classloaderName()))
            return false;
        
        // outer class (if any) must match
        if (outer == null && oClass.outer != null)
            return false;
        if (outer != null)
            if (! outer.equals(oClass.outer))
                return false;
        
        if (params == null) {
            return oClass.params == null;
        } else if (oClass.params == null) {
            return false;
        }
        
        Iterator<? extends GenTypeParameter> i = params.iterator();
        Iterator<? extends GenTypeParameter> j = oClass.params.iterator();
        
        // All the parameter types must match...
        while( i.hasNext() ) {
            if( ! j.hasNext() || ! i.next().equals(j.next())) {
                return false;
            }
        }
        
        // and there must be the same number of parameters
        if( j.hasNext() )
            return false;

        return true;
    }
    
    /**
     * Get the reflective represented by this GenTypeClass. This can return null
     * if the GenTypeClass represents an array.
     */
    public Reflective getReflective()
    {
        return reflective;
    }
    
    /*
     * Note this does not handle boxing/unboxing conversions.
     * 
     *  (non-Javadoc)
     * @see bluej.debugger.gentype.JavaType#isAssignableFrom(bluej.debugger.gentype.JavaType)
     */
    @OnThread(Tag.FXPlatform)
    public boolean isAssignableFrom(JavaType t)
    {
        if (t.isNull())
            return true;
        
        if (! (t instanceof GenTypeSolid) )
            return false;
        
        GenTypeClass tc = t.asClass();
        if (tc != null) {
            // If this type is raw, no need to bother with type parameters
            if (isRaw()) {
                return reflective.isAssignableFrom(tc.reflective);
            }
            
            // If all the type arguments contain those from the other class, after
            // mapping the base type to this class type, then the other type is
            // assignable to this - otherwise it's not. Also check outer classes.
            GenTypeClass cclass = this;
            GenTypeClass tclass;
            try {
                tclass = tc.mapToSuper(reflective.getName());
            }
            catch (BadInheritanceChainException bice) {
                return false;
            }
            
            if (tclass.isRaw())
                return false;
            while (cclass != null) {
                if (cclass.params != null) {
                    Iterator<? extends GenTypeParameter> i = cclass.params.iterator();
                    Iterator<? extends GenTypeParameter> j = tclass.params.iterator();
                    while (i.hasNext()) {
                        GenTypeParameter cpar = i.next();
                        GenTypeParameter tpar = j.next();
                        if (! cpar.contains(tpar))
                            return false;
                    }
                }
                cclass = cclass.outer;
                tclass = tclass.outer;
            }
            
            return true;
        }
        
        // Check each of the reference supertypes
        GenTypeClass [] stypes = ((GenTypeSolid) t).getReferenceSupertypes();
        for (int i = 0; i < stypes.length; i++) {
            if (isAssignableFrom(stypes[i]))
                return true;
        }
        
        return false;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isAssignableFrom(GenTypeClass c)
    {
        Reflective r = c.reflective;

        // check the inheritance hierarchy
        if( getInheritanceChain(r, reflective.getName()) != null) {

            GenTypeClass other = c.mapToSuper(reflective.getName());
            if (isRaw() || other.isRaw())
                return true;
            
            if (outer != null) {
                if (! outer.isAssignableFrom(other.outer))
                    return false;
            }
            
            if (params != null) {
                Iterator<? extends GenTypeParameter> i = params.iterator();
                Iterator<? extends GenTypeParameter> j = other.params.iterator();
                while (i.hasNext()) {
                    GenTypeParameter myParam = i.next();
                    GenTypeParameter oParam = j.next();
                    if (! myParam.contains(oParam))
                        return false;
                }
            }
            
            return true;
        }

        return false;
    }

    @OnThread(Tag.FXPlatform)
    public boolean isAssignableFromRaw(JavaType t)
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
    @OnThread(Tag.FXPlatform)
    public GenTypeClass mapToSuper(String basename)
    {
        if( classloaderName().equals(basename))
            return this;
        
        // the base type could actually be an interface, or a base class. 
        Stack<Reflective> inheritanceStack = getInheritanceChain(reflective, basename);
        if( inheritanceStack == null ) {
            throw new BadInheritanceChainException();
        }
        
        String bname;
        Iterator<Reflective> i = inheritanceStack.iterator();
        i.next();  // skip the topmost class, we've already got that.
        Reflective baseType;
        Reflective subType = reflective;
        GenTypeClass ccc = this;
        
        do {
            baseType = (Reflective)i.next();
            bname = baseType.getName();
            Map<String,GenTypeParameter> tparams = ccc.getMap();
            ccc = mapGenericParamsToDirectBase(tparams, subType, baseType);
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
    @OnThread(Tag.FXPlatform)
    private static GenTypeClass mapGenericParamsToDirectBase(Map<String,? extends GenTypeParameter> tparams,
            Reflective subType, Reflective baseType)
    {
        GenTypeClass baseClass = subType.superTypeByName(baseType.getName());
        if (tparams == null) {
            // sub-type inherits from the raw base class
            return new GenTypeClass(baseClass.reflective);
        }
        baseClass = (GenTypeClass) baseClass.mapTparsToTypes(tparams);
        return baseClass;
    }
    
    /**
     * Return the corresponding type if all type parameters are replaced with
     * corresponding actual types, as defined by a Map (String -> GenType).
     * 
     * @param tparams  A map defining the translation from type parameter
     *                 name (String) to its actual type (GenType). The map
     *                 can be null to return the raw type.
     * @return the corresponding type structure, with parameters mapped.
     */
    @OnThread(Tag.FXPlatform)
    public GenTypeClass mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        // If there are no generic parameters, there's nothing to map...
        if( params == null && outer == null )
            return this;
        
        if (tparams == null) {
            return new GenTypeClass(reflective);
        }
        
        // Otherwise map each parameter, return the result.
        List<GenTypeParameter> retlist = new ArrayList<GenTypeParameter>();
        if (params != null) {
            Iterator<? extends GenTypeParameter> i = params.iterator();
            while( i.hasNext() ) {
                retlist.add(i.next().mapTparsToTypes(tparams));
            }
        }
        
        GenTypeClass newOuter = null;
        if (outer != null) {
            newOuter = (GenTypeClass) outer.mapTparsToTypes(tparams);
        }
        
        return new GenTypeClass(reflective, retlist, newOuter);
    }

    @OnThread(Tag.FXPlatform)
    public GenTypeClass mapToDerived(Reflective derivedType)
    {        
        // Get a map (parameter name -> type) for this class.
        if(! isGeneric() )
            return new GenTypeClass(derivedType);
        
        // One simple class is when super class = this.
        // TODO don't use class names as equality test
        if( derivedType.getName().equals(classloaderName()))
            return this;
        
        // Construct a list (actually a stack) of classes from the
        // super type down to this type.
        Stack<Reflective> classes = getInheritanceChain(derivedType, classloaderName());
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
            HashMap<String,GenTypeParameter> newMap = new HashMap<String,GenTypeParameter>();
           
            // Check that the super inherits from the generic version of base
            GenTypeClass baseDecl = curSubtype.superTypeByName(curBaseC.classloaderName());
            if (baseDecl.isRaw()) {
                return new GenTypeClass(derivedType);
            }
            
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
     * <p>Returns null if this represents a raw type, or an empty map if the
     * type is not a generic type.
     * 
     * <p>Note that a map is not enough to completely describe a type, due to
     * the possibility of inner/outer types having type parameters with the
     * same name.
     * 
     * @return the map (of String -> GenTypeParameterizable).
     */
    @OnThread(Tag.FXPlatform)
    public HashMap<String,GenTypeParameter> getMap()
    {
        if (isRaw()) {
            return null;
        }
        
        HashMap<String,GenTypeParameter> r = new HashMap<String,GenTypeParameter>();
        mergeMap(r);
        return r;
    }
    
    /**
     * Get a map of type parameter names to the corresponding types, for this
     * type. Existing entries in the map will be overwritten. 
     * 
     * The returned does not indicate if this type is a raw type.
     */
    @OnThread(Tag.FXPlatform)
    public void mergeMap(Map<String, GenTypeParameter> m)
    {
        if (outer != null)
        {
            outer.mergeMap(m);
        }

        List<GenTypeDeclTpar> formalParams = reflective.getTypeParams();
        if( params == null )
        {
            return;
        }
        
        Iterator<? extends GenTypeParameter> paramIterator = params.iterator();
        Iterator<GenTypeDeclTpar> formalIterator = formalParams.iterator();
        
        // go through each type parameter, assign it the type from our
        // params list.
        while (paramIterator.hasNext() && formalIterator.hasNext())
        {
            GenTypeParameter paramType = paramIterator.next();
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
     * returns a Stack of Reflective.
     */
    @OnThread(Tag.FXPlatform)
    private static Stack<Reflective> getInheritanceChain(Reflective top, String bottom)
    {
        Stack<Reflective> r = new Stack<Reflective>();
        r.push(top);
        if( top.getName().equals(bottom )) {
            return r;
        }
        
        // Go through each base/interface and try to discover the hieararchy
        List<Reflective> l = top.getSuperTypesR();
        for(Iterator<Reflective> i = l.iterator(); i.hasNext(); ) {
            Reflective next = i.next();
            Stack<Reflective> r2 = getInheritanceChain(next, bottom);
            if( r2 != null ) {
                r.addAll(r2);
                return r;
            }
        }
        return null;
    }
    
    /*
     * see bluej.debugger.gentype.GenTypeSolid#getParamsFromTemplate(java.util.Map, bluej.debugger.gentype.GenTypeParameterizable)
     */
    @OnThread(Tag.FXPlatform)
    public void getParamsFromTemplate(Map<String,GenTypeParameter> r, GenTypeParameter template)
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
            if (classTemplate.classloaderName().equals(classloaderName())) {
                if (params == null || classTemplate.params == null)
                    return;
                Iterator<? extends GenTypeParameter> i = params.iterator();
                Iterator<? extends GenTypeParameter> j = classTemplate.params.iterator();

                // Handle case that this is an inner class
                if (outer != null)
                    outer.getParamsFromTemplate(r, classTemplate.outer);
                
                // loop through each parameter
                while (i.hasNext() && j.hasNext()) {
                    GenTypeSolid ip = (GenTypeSolid) i.next();
                    GenTypeParameter jp = j.next();

                    ip.getParamsFromTemplate(r, jp);
                }
            }
        }

        return;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void erasedSuperTypes(Set<Reflective> s)
    {
        Stack<Reflective> refs = new Stack<Reflective>();
        refs.push(reflective);
        
        while(! refs.empty()) {
            Reflective r = (Reflective) refs.pop();
            if (! s.contains(r)) {
                // The reflective is not already in the set, so
                // add it and queue its supertypes
                s.add(r);
                refs.addAll(r.getSuperTypesR());
            }
        }
    }
    
    public GenTypeClass [] getReferenceSupertypes()
    {
        return new GenTypeClass[] {this};
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeClass getArray()
    {
        return new GenTypeArrayClass(reflective.getArrayOf(), this);
    }
    
    @Override
    public GenTypeClass getCapture()
    {
        if (outer == null && params == null) {
            return this;
        }
        
        GenTypeClass outerCapture = outer == null ? null : outer.getCapture();
        
        boolean isDifferent = outerCapture != outer;
        
        List<JavaType> capturedParams = null;
        if (params != null) {
            capturedParams = new ArrayList<JavaType>(params.size());
            for (GenTypeParameter param : params) {
                JavaType captured = param.getTparCapture();
                isDifferent |= captured != param;
                capturedParams.add(captured);
            }
        }
        
        if (isDifferent) {
            return new GenTypeClass(reflective, capturedParams, outerCapture);
        }
        
        return this;
    }
}
