/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import java.util.*;

import bluej.debugger.gentype.*;
import bluej.utility.Debug;

import com.sun.jdi.*;

/**
 * A Reflective for Jdi classes.
 * 
 * @see Reflective.
 * 
 * @author Davin McCall
 * @version $Id: JdiReflective.java 6215 2009-03-30 13:28:25Z polle $
 */
public class JdiReflective extends Reflective
{

    // For a loaded type, we have a ReferenceType.
    private ReferenceType rclass = null;

    // If the type has not been loaded, we know only it's name, and the
    // type from which we discovered the reference to it:
    protected String name = null;
    // private ReferenceType sourceType = null;
    private ClassLoaderReference sourceLoader = null;
    private VirtualMachine sourceVM = null;

    /**
     * Constructor - loaded type
     */
    public JdiReflective(ReferenceType rclass)
    {
        super();
        this.rclass = rclass;
        if (rclass == null) {
            Debug.message("JdiReflective: null ReferenceType?");
            throw new NullPointerException();
        }
    }

    /**
     * Constructor - type not loaded yet; source (parent) object available.
     * 
     * @param name
     *            The fully qualified type name
     * @param sourceType
     *            The type from which a reference to this type was obtained
     */
    public JdiReflective(String name, ReferenceType sourceType)
    {
        this.name = name;
        // this.sourceType = sourceType;
        this.sourceLoader = sourceType.classLoader();
        this.sourceVM = sourceType.virtualMachine();
    }

    /**
     * Constructor - type may not yet be loaded; class loader available.
     * 
     * @param name
     *            The fully qualified type name
     * @param classLoader
     *            The class loader used to load this type
     * @param vm
     *            The virtual machine reference
     */
    public JdiReflective(String name, ClassLoaderReference classLoader, VirtualMachine vm)
    {
        this.name = name;
        this.sourceLoader = classLoader;
        this.sourceVM = vm;
    }
    
    public Reflective getRelativeClass(String name)
    {
        if (rclass != null)
            return new JdiReflective(name, rclass);
        else
            return new JdiReflective(name, sourceLoader, sourceVM);
    }

    /**
     * Try to make sure we have a valid reference to the actual type
     */
    protected void checkLoaded()
    {
        if (rclass == null) {
            rclass = findClass(name, sourceLoader, sourceVM);
            outOk:
            if (rclass == null) {
                // Try and load the class.
                VMReference vmr = VMReference.getVmForMachine(sourceVM);
                if (vmr != null) {
                    rclass = vmr.loadClass(name, sourceLoader);
                    if (rclass != null)
                        break outOk;
                }
                Debug.message("Attempt to use unloaded type: " + name);
                Debug.message("  name = " +  name + ", sourceLoader = " + sourceLoader);
                new Exception().printStackTrace(System.out);
                return;
            }
            name = null;
            sourceLoader = null;
            sourceVM = null;
        }
    }

    public String getName()
    {
        if (name != null)
            return name;
        return rclass.name();
    }
    
    public boolean isInterface()
    {
        checkLoaded();
        return rclass instanceof InterfaceType;
    }
    
    public boolean isStatic()
    {
        checkLoaded();
        return rclass.isStatic();
    }

    public Reflective getArrayOf()
    {
        if (rclass != null)
            return new JdiArrayReflective(new GenTypeClass(this), rclass);
        else
            return new JdiArrayReflective(new GenTypeClass(this), sourceLoader, sourceVM);
    }
    
    public List getTypeParams()
    {
        // Make sure we are loaded and a generic signature is present.
        checkLoaded();
        String gensig = JdiUtils.getJdiUtils().genericSignature(rclass);
        if (gensig == null)
            return Collections.EMPTY_LIST;
        
        // Read the type parameters from the generic signature.
        StringIterator s = new StringIterator(gensig);
        return getTypeParams(s);
    }

    /**
     * Get the type parameters of this class, as a list of GenTypeDeclTpar,
     * using a pre-existing string iterator to process the generic signature.
     *  
     * @param s  A string iterator, currently at the beginning of the generic
     *           signature for this class. On return, the iterator will be
     *           positioned after the type parameters in the signature, at the
     *           beginning of the class name.
     * 
     * @return  A list of GenTypeDeclTpar, representing the type parameters of
     *          this class.
     */
    private List getTypeParams(StringIterator s)
    {
        List rlist = new ArrayList();

        char c = s.peek();
        if (c != '<')
            return rlist;

        // go through each type parameter, assign it the type from our
        // params list.
        s.next();
        while (c != '>') {
            String paramName = readClassName(s);
            if (s.current() != ':') {
                Debug.message("getTypeParams : no ':' following type parameter name in super signature?? got "
                        + s.current());
                Debug.message("signature was: " + s.getString());
                return null;
            }
            // '::' indicates lower bound is an interface. Ignore.
            if (s.peek() == ':')
                s.next();

            // multiple bounds appear as T:bound1;:bound2; ... etc
            ArrayList bounds = new ArrayList(3);
            while (s.current() == ':') {
                bounds.add(fromSignature(s, null, rclass));
                
                //we don't want the next char to be eaten...
                if (s.peek() == ':')
                    s.next();
            }
            rlist.add(new GenTypeDeclTpar(paramName, (GenTypeSolid []) bounds.toArray(new GenTypeSolid [0])));
            c = s.peek();
        }
        s.next();
        return rlist;
    }
    
    public List getSuperTypesR()
    {
        checkLoaded();
        if (rclass instanceof ClassType) {
            List l = new LinkedList();
            Iterator i = ((ClassType) rclass).interfaces().iterator();
            while (i.hasNext())
                l.add(new JdiReflective((ReferenceType) i.next()));
            if (((ClassType) rclass).superclass() != null)
                l.add(new JdiReflective(((ClassType) rclass).superclass()));
            return l;
        }
        else if (rclass instanceof InterfaceType) {
            // interface
            List l = new LinkedList();
            Iterator i = ((InterfaceType) rclass).superinterfaces().iterator();
            while (i.hasNext())
                l.add(new JdiReflective((ReferenceType) i.next()));
            
            // interfaces with no direct superinterfaces have a supertype of Object
            if (l.isEmpty()) {
                l.add(new JdiReflective("java.lang.Object", this.rclass));
            }
            
            return l;
        }
        else
            return new LinkedList();
    }

    public List getSuperTypes()
    {
        checkLoaded();
        List rlist = new ArrayList();

        if (JdiUtils.getJdiUtils().genericSignature(rclass) == null) {
            if (rclass instanceof ClassType) {
                ClassType ctClass = (ClassType) rclass;

                // superclass
                Reflective r = new JdiReflective(ctClass.superclass());
                rlist.add(new GenTypeClass(r));

                // interfaces
                List interfaces = ctClass.interfaces();
                for (Iterator i = interfaces.iterator(); i.hasNext();) {
                    r = new JdiReflective((InterfaceType) i.next());
                    rlist.add(new GenTypeClass(r));
                }
                return rlist;
            }
            else {
                // rclass must be an InterfaceType
                InterfaceType itClass = (InterfaceType) rclass;

                List interfaces = itClass.superinterfaces();
                for (Iterator i = interfaces.iterator(); i.hasNext();) {
                    Reflective r = new JdiReflective((InterfaceType) i.next());
                    rlist.add(new GenTypeClass(r));
                }
                
                // interfaces with no direct superinterfaces have a supertype of Object
                if (rlist.isEmpty()) {
                    rlist.add(new GenTypeClass(new JdiReflective("java.lang.Object", this.rclass)));
                }

                return rlist;
            }
        }

        // A generic signature for a type looks something like:
        //    <..type params..>Lbase/class<...>;...interfaces...
        // First, skip over the type params in the supertype:

        StringIterator s = new StringIterator(JdiUtils.getJdiUtils().genericSignature(rclass));
        List l = getTypeParams(s);
        Map declTpars = new HashMap();
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar declTpar = (GenTypeDeclTpar) i.next();
            declTpars.put(declTpar.getTparName(), declTpar); 
        }
        
        // go through each base type in turn.
        while (s.hasNext()) {
            // We now have a base.
            GenTypeClass t = (GenTypeClass) fromSignature(s, declTpars, rclass);
            rlist.add(t);
        }
        return rlist;
    }
    
    public boolean isAssignableFrom(Reflective r)
    {
        if (this.equals(r))
            return true;
        
        // Any reference type, including arrays, can be assigned to Object
        if (getName().equals("java.lang.Object"))
            return true;
        
        if (r instanceof JdiReflective) {
            JdiReflective jr = (JdiReflective) r;
            
            jr.checkLoaded();
            return checkAssignability(rclass, jr.rclass);
        }
        else
            return false;
    }
    
    /**
     * Check that type b is assingable to a variable of type a.
     */
    private static boolean checkAssignability(ReferenceType a, ReferenceType b)
    {
        while (true)
        {
            if (a instanceof ClassType) {
                if (b instanceof InterfaceType) {
                    List l = ((ClassType)a).allInterfaces();
                    return l.contains(b);
                }
                else if (b instanceof ClassType) {
                    ClassType classType = (ClassType) a;
                    
                    while (classType != null) {
                        if (b.equals(classType))
                            return true;
                        classType = classType.superclass();
                    }
                }
                return false;
            }
            else if (a instanceof InterfaceType) {
                if (! (b instanceof InterfaceType))
                    return false;
                
                List l = new LinkedList(); 
                l.addAll(((InterfaceType) a).superinterfaces());
                while (! l.isEmpty()) {
                    // get the first superinterface in the list
                    InterfaceType it = (InterfaceType) l.get(0);
                    if (a.equals(it))
                        return true;
                    
                    l.addAll(it.superinterfaces());
                    l.remove(0);
                }
                return false;
            }
            else if (a instanceof ArrayType) {
                if (! (b instanceof ArrayType))
                    return false;
                
                try {
                    Type an = ((ArrayType) a).componentType();
                    Type bn = ((ArrayType) b).componentType();
                
                    if (an instanceof ReferenceType && bn instanceof ReferenceType) {
                        a = (ReferenceType) an;
                        b = (ReferenceType) bn;
                    }
                    else
                        return false;
                }
                catch (ClassNotLoadedException cnle) {
                    return false;
                }
            }
            else
                // unknown type
                return false;
        }
    }

    /**
     * Find a class by name, using the given class loader.
     * 
     * @param name
     *            The name of the class to find
     * @param lclass
     *            The loading class. The class loader of this class is used to
     *            locate the desired class.
     * @return A ClassType object representing the found class (or null)
     */
    private static ReferenceType findClass(String name, ClassLoaderReference cl, VirtualMachine vm)
    {
        Iterator i;

        if (cl != null) {
            // See if the desired class was initiated by our class loader.
            i = cl.visibleClasses().iterator();
            while (i.hasNext()) {
                ReferenceType ct = (ReferenceType) i.next();
                if (ct.name().equals(name))
                    return ct;
            }
        }
        else {
            // A null classloader means the bootstrap class loader was used.
            // So look for the class with the correct name which was loaded
            // by the bootstrap loader.
            i = vm.classesByName(name).iterator();
            while (i.hasNext()) {
                ReferenceType ct = (ReferenceType) i.next();
                if (ct.classLoader() == null)
                    return ct;
            }
        }
        
        return null;
    }

    /**
     * Read a class name or type parameter name from a signature string.
     * 
     * @param i
     *            An iterator into the string
     * @return the fully qualified class name
     */
    private static String readClassName(StringIterator i)
    {
        char c = i.next();
        String r = new String();
        while (c != '<' && c != ';' && c != ':') {
            if (c == '/')
                r += '.';
            else
                r += c;
            c = i.next();
        }
        return r;
    }

    /**
     * Generate a GenType structure from a type specification found in a
     * signature string, optionally mapping type parameter names to their actual
     * types.
     * 
     * @param i
     *            a StringIterator through the signature string
     * @param tparams
     *            a mapping of type parameter names to types (or null)
     * @param parent
     *            the type in which the signature string is embedded. The class
     *            loader of this type is used to locate embedded types.
     * @return The GenType structure determined from the signature.
     */
    private static JavaType fromSignature(StringIterator i, Map tparams, ReferenceType parent)
    {
        char c = i.next();
        if (c == '*') {
            // '*' represents an unbounded '?'. For instance, List<?>
            // has signature: Ljava/lang/list<*>;
            return new GenTypeUnbounded();
        }
        if (c == '+') {
            // ? extends ...
            // The bound must be a solid, but it's possible that tparams map
            // a type parameter (solid) to a wildcard (non-solid).
            GenTypeSolid t = (GenTypeSolid) fromSignature(i, null, parent);
            if (tparams != null)
                return new GenTypeExtends(t).mapTparsToTypes(tparams);
            else
                return new GenTypeExtends(t);
        }
        if (c == '-') {
            // ? super ...
            // Likewise as for extends.
            GenTypeSolid t = (GenTypeSolid) fromSignature(i, null, parent);
            if (tparams != null)
                return new GenTypeSuper(t).mapTparsToTypes(tparams);
            else
                return new GenTypeSuper(t);
        }
        if (c == '[') {
            // array
            JavaType t = fromSignature(i, tparams, parent);
            
            // figure out the class name of the array class
            String xName = "[" + t.arrayComponentName();
            
            // return the array
            Reflective areflective = new JdiReflective(xName, parent); 
            t = new GenTypeArray(t, areflective);
            return t;
        }
        if (c == 'T') {
            // type parameter
            String tname = readClassName(i);
            if (tparams != null && tparams.get(tname) != null)
                return (JavaType) tparams.get(tname);
            else
                return new GenTypeTpar(tname);
        }
        if (c == 'I') {
            // integer
            return JavaPrimitiveType.getInt();
        }
        if (c == 'C') {
            // character
            return JavaPrimitiveType.getChar();
        }
        if (c == 'Z') {
            // boolean
            return JavaPrimitiveType.getBoolean();
        }
        if (c == 'B') {
            // byte
            return JavaPrimitiveType.getByte();
        }
        if (c == 'S') {
            // short
            return JavaPrimitiveType.getShort();
        }
        if (c == 'J') {
            // long
            return JavaPrimitiveType.getLong();
        }
        if (c == 'F') {
            // float
            return JavaPrimitiveType.getFloat();
        }
        if (c == 'D') {
            // double
            return JavaPrimitiveType.getDouble();
        }

        if (c != 'L')
            Debug.message("Generic signature begins without 'L'?? (got " + c + ")");

        String basename = readClassName(i);
        Reflective reflective = new JdiReflective(basename, parent);
        c = i.current();
        if (c == ';')
            return new GenTypeClass(reflective, (List) null);

        if (c != '<') {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }

        List params = new ArrayList();
        do {
            JavaType ptype = fromSignature(i, tparams, parent);
            if (ptype == null)
                return null;
            params.add(ptype);
        } while (i.peek() != '>');
        i.next(); // fetch the '>'
        c = i.next(); // fetch the trailing ';' or '.'
        
        GenTypeClass result = new GenTypeClass(reflective, params);

        // if c is now '.', we have an inner class
        if (c == '.')
            return innerFromSignature(i, basename, result, tparams, parent);
        
        // otherwise assume we have ';'
        return result;
    }
    
    private static GenTypeClass innerFromSignature(StringIterator i, String outerName, GenTypeClass outer, Map tparams, ReferenceType parent)
    {
        String basename = readClassName(i);
        String innerName = outerName + '$' + basename;
        Reflective reflective = new JdiReflective(innerName, parent);
            
        char c = i.current();
        if (c == ';')
            return new GenTypeClass(reflective, (List) null, outer);
        
        if (c == '<') {
            List params = new ArrayList();
            do {
                JavaType ptype = fromSignature(i, tparams, parent);
                if (ptype == null)
                    return null;
                params.add(ptype);
            } while (i.peek() != '>');
            i.next(); // fetch the '>'
            c = i.next(); // fetch the trailing ';' or '.'
            
            GenTypeClass result = new GenTypeClass(reflective, params, outer);
            
            // if c is now '.', we have an inner class
            if (c == '.')
                return innerFromSignature(i, innerName, result, tparams, parent);

            return result;
        }
        else {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }
    }

    private static JavaType getNonGenericType(String typeName, Type t, ClassLoaderReference clr, VirtualMachine vm)
    {
        if (t instanceof BooleanType)
            return JavaPrimitiveType.getBoolean();
        else if (t instanceof ByteType)
            return JavaPrimitiveType.getByte();
        else if (t instanceof CharType)
            return JavaPrimitiveType.getChar();
        else if (t instanceof DoubleType)
            return JavaPrimitiveType.getDouble();
        else if (t instanceof FloatType)
            return JavaPrimitiveType.getFloat();
        else if (t instanceof IntegerType)
            return JavaPrimitiveType.getInt();
        else if (t instanceof LongType)
            return JavaPrimitiveType.getLong();
        else if (t instanceof ShortType)
            return JavaPrimitiveType.getShort();
        else {
            // The class may or may not be loaded.
            Reflective ref;
            ref = new JdiReflective(typeName, clr, vm);
            return new GenTypeClass(ref, (List) null);
        }
    }

    /**
     * Determine the complete type of an instance field.
     * 
     * @param f
     *            The field
     * @param parent
     *            The object in which the field is located
     * @return The type of the field value
     */
    public static JavaType fromField(Field f, JdiObject parent)
    {
        Type t = null;

        // For a field whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "f.type()" throws a
        // ClassNotLoadedException.
        //
        // In the case of string literals, however, it's possible that trying
        // to get a reference to "java.lang.String" throws
        // ClassNotLoadedException even when the value isn't null. In this
        // case, findClass() and v.type() both successfully get a reference
        // to the type. Perhaps it's a VM bug.

        Value v = parent.obj.getValue(f); // cheap way to make sure class is
                                          // loaded (if value not null)
        try {
            t = f.type();
        }
        catch (ClassNotLoadedException cnle) {
            // Debug.message("ClassNotLoadedException, name = " + f.typeName());
            t = findClass(f.typeName(), parent.obj.referenceType().classLoader(), parent.obj.virtualMachine());
            if (t == null && v != null)
                t = v.type();
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(f);

        // check for primitive type, or raw type
        //if (gensig == null && (rt == null || rt.genericSignature() != null))
        if (gensig == null)
            return getNonGenericType(f.typeName(), t, parent.obj.referenceType().classLoader(), parent.obj
                    .virtualMachine());

        // generic version.
        GenTypeClass genType = parent.getGenType();
        
        // Map from containing object type to the type in which the field was
        // declared. Then extract the type parameter mappings.
        Map tparams = genType.mapToSuper(f.declaringType().name()).getMap();
        if (tparams == null) {
            // raw parent
            Reflective r = new JdiReflective(f.typeName(), parent.obj.referenceType());
            return new GenTypeClass(r);
        }
        
        StringIterator iterator = new StringIterator(gensig);

        // Parse the signature, using the determined tpar mappings.
        return fromSignature(iterator, tparams, parent.obj.referenceType());
    }

    /**
     * Determine the complete type of a class field.
     * 
     * @param f
     *            The field
     * @param parent
     *            The object in which the field is located
     * @return The type of the field value
     */
    public static JavaType fromField(Field f, ReferenceType parent)
    {
        Type t = null;

        // For a field whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "f.type()" throws a
        // ClassNotLoadedException.
        //
        // In the case of string literals, however, it's possible that trying
        // to get a reference to "java.lang.String" throws
        // ClassNotLoadedException even when the value isn't null. In this
        // case, findClass() and v.type() both successfully get a reference
        // to the type. Perhaps it's a VM bug.

        Value v = parent.getValue(f); // cheap way to make sure class is loaded
        try {
            t = f.type();
        }
        catch (ClassNotLoadedException cnle) {
            // Debug.message("ClassNotLoadedException, name = " + f.typeName());
            t = findClass(f.typeName(), parent.classLoader(), parent.virtualMachine());
            if (t == null && v != null)
                t = v.type();
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(f);

        if (gensig == null)
            return getNonGenericType(f.typeName(), t, parent.classLoader(), parent.virtualMachine());

        // if the generic signature wasn't null, get the type from it.
        StringIterator iterator = new StringIterator(gensig);
        return fromSignature(iterator, null, parent);
    }

    public static JavaType fromLocalVar(StackFrame sf, LocalVariable var)
    {
        Type t = null;
        // For a variable whose value is unset/null, the corresponding class
        // type may not have been loaded. In this case "var.type()" throws a
        // ClassNotLoadedException.
        //
        // In the case of string literals, however, it's possible that trying
        // to get a reference to "java.lang.String" throws
        // ClassNotLoadedException even when the value isn't null. In this
        // case, findClass() and v.type() both successfully get a reference
        // to the type. Perhaps it's a VM bug.

        Value v = sf.getValue(var);

        Location l = sf.location();
        ReferenceType declType = l.declaringType();

        try {
            t = var.type();
        }
        catch (ClassNotLoadedException cnle) {
            // Debug.message("ClassNotLoadedException, name = " + f.typeName());
            t = findClass(var.typeName(), declType.classLoader(), declType.virtualMachine());
            if (t == null && v != null)
                t = v.type();
        }

        final String gensig = JdiUtils.getJdiUtils().genericSignature(var);

        if (gensig == null)
            return getNonGenericType(var.typeName(), t, declType.classLoader(), declType.virtualMachine());

        // if the generic signature wasn't null, get the type from it.
        StringIterator iterator = new StringIterator(gensig);
        Map tparams = new HashMap();
        addDefaultParamBases(tparams, new JdiReflective(declType));
        return fromSignature(iterator, tparams, declType);
    }

    /**
     * For all type parameters which don't already occur in the given Map,
     * add them in as the "default" given by the class definition. For instance
     * with List&lt;T&gt;, add in the mapping "T : ? extends Object".
     * @param tparams       The map (String -> GenTypeClass)
     * @param declaringType the type for which to add default mappings
     */
    private static void addDefaultParamBases(Map tparams, JdiReflective declaringType)
    {
        while (declaringType != null) {
            Iterator i = declaringType.getTypeParams().iterator();
            
            while( i.hasNext() ) {
                GenTypeDeclTpar tpar = (GenTypeDeclTpar) i.next();
                
                String paramName = tpar.getTparName();
                
                GenTypeSolid [] ubounds = tpar.upperBounds();
                GenTypeWildcard type = new GenTypeWildcard(ubounds, new GenTypeSolid[0]);
                if( ! tparams.containsKey(paramName)) {
                    tparams.put(paramName, type);
                }
            }
            declaringType = declaringType.getOuterType();
        }
    }
    
    /**
     * Get the reflective representing the outer class of this reflective,
     * or null if none.
     */
    private JdiReflective getOuterType()
    {
        checkLoaded();
        String myName = getName();
        int x = myName.indexOf('$');
        if (x != -1 && ! rclass.isStatic()) {
            String outerName = myName.substring(0, x);
            return (JdiReflective) getRelativeClass(outerName);
        }
        else
            return null;
    }
    
    static class StringIterator
    {
        int i = 0;
        String s;

        public StringIterator(String s)
        {
            this.s = s;
            if (s == null)
                Debug.message("StringIterator with null string??");
        }

        public char current()
        {
            return s.charAt(i - 1);
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
        
        public String getString()
        {
            return s;
        }
    };
}
