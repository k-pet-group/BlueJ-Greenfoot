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
 * @version $Id: JdiReflective.java 2816 2004-07-26 00:10:16Z davmac $
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

    /**
     * Try to make sure we have a valid reference to the actual type
     */
    protected void checkLoaded()
    {
        if (rclass == null) {
            rclass = findClass(name, sourceLoader, sourceVM);
            if (rclass == null)
                Debug.message("Attempt to use unloaded type: " + name);
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

    public List getTypeParams()
    {
        checkLoaded();
        List rlist = new ArrayList();
        String gensig = JdiUtils.getJdiUtils().genericSignature(rclass);
        if (gensig == null)
            return rlist;
        StringIterator s = new StringIterator(gensig);

        char c = s.next();
        if (c != '<')
            return rlist;

        // go through each type parameter, assign it the type from our
        // params list.
        while (c != '>') {
            String paramName = readClassName(s);
            if (s.current() != ':') {
                Debug.message("getTypeParams : no ':' following type parameter name in super signature?? got "
                        + s.current());
                Debug.message("signature was: " + gensig);
                return null;
            }
            // '::' indicates lower bound is an interface. Ignore.
            if (s.peek() == ':')
                s.next();
            // multiple bounds appear as T:bound1;:bound2; ... etc
            boolean firstBound = true;

            //TODO refactor this messy while loop...
            while (s.current() == ':') {
                GenTypeSolid bound = (GenTypeSolid) fromSignature(s, null, rclass);
                // TODO properly support multiple bounds. At the moment we'll
                // just throw the subsequent bounds away.
                if (firstBound) {
                    rlist.add(new GenTypeDeclTpar(paramName, bound));
                    firstBound = false;
                }
                //we don't want the next char to be eaten...
                if (s.peek() == ':') {

                    s.next();
                }
                else {
                    break;
                }
            }
            c = s.peek();
        }
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
                return rlist;
            }
        }

        // A generic signature for a type looks something like:
        //    <..type params..>Lbase/class<...>;...interfaces...
        // First, skip over the type params in the supertype:

        StringIterator s = new StringIterator(JdiUtils.getJdiUtils().genericSignature(rclass));
        if (s.peek() == '<') {
            s.next();
            int lbCount = 1;
            while (lbCount > 0) {
                char c = s.next();
                if (c == '<')
                    lbCount++;
                else if (c == '>')
                    lbCount--;
            }
        }

        while (s.hasNext()) {

            // We now have a base.
            if (s.next() != 'L') {
                Debug.message("getSuperTypes: inherit from non reference type?");
                return rlist;
            }
            String bName = readClassName(s);
            ReferenceType bType = findClass(bName, rclass.classLoader(), rclass.virtualMachine());
            if (bType == null)
                Debug.message("getSuperTypes: Couldn't find type: " + bName);

            JdiReflective bReflective = new JdiReflective(bType);

            char c = s.current();
            if (c == ';') {
                rlist.add(new GenTypeClass(bReflective, (List) null));
            }
            else {

                if (c != '<') {
                    Debug.message("mapGenericParamsToBase: didn't see '<' at end of base-of-super?? (got " + c + ")");
                    return rlist;
                }

                List bParams = new ArrayList();

                while (c != '>') {
                    // find the first type parameter to the base class from the
                    // super class
                    // eg in A<...> extends B<String,Integer>, this is "String".
                    GenType sParamType = fromSignature(s, null, rclass); // super
                                                                         // parameter
                                                                         // type
                    bParams.add(sParamType);
                    c = s.peek();
                }

                s.next(); // read terminating '>'
                s.next(); // read ';'
                rlist.add(new GenTypeClass(bReflective, bParams));
            }
        }
        return rlist;
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

        // If not, it may have been initated by a parent class loader.
        // TODO. Technically what we do here is incorrect. We should walk up
        // the chain of classloaders and check each in turn until we reach
        // the system clas loader.
        i = vm.classesByName(name).iterator();
        while (i.hasNext()) {
            ReferenceType ct = (ReferenceType) i.next();
            if (ct.name().equals(name))
                return ct;
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
     * Generate a JdiGenType structure from a type specification found in a
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
    private static GenType fromSignature(StringIterator i, Map tparams, ReferenceType parent)
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
            GenType t = fromSignature(i, tparams, parent);
            t = new GenTypeArray(t, new JdiArrayReflective(t, parent));
            return t;
        }
        if (c == 'T') {
            // type parameter
            String tname = readClassName(i);
            if (tparams != null && tparams.get(tname) != null)
                return (GenType) tparams.get(tname);
            else
                return new GenTypeTpar(tname);
        }
        if (c == 'I') {
            // integer
            return new GenTypeInt();
        }
        if (c == 'C') {
            // character
            return new GenTypeChar();
        }
        if (c == 'Z') {
            // boolean
            return new GenTypeBool();
        }
        if (c == 'B') {
            // byte
            return new GenTypeByte();
        }
        if (c == 'S') {
            // short
            return new GenTypeShort();
        }
        if (c == 'J') {
            // long
            return new GenTypeLong();
        }
        if (c == 'F') {
            // float
            return new GenTypeFloat();
        }
        if (c == 'D') {
            // double
            return new GenTypeDouble();
        }

        if (c != 'L')
            Debug.message("Generic signature begins without 'L'?? (got " + c + ")");

        String basename = readClassName(i);
        Reflective reflective = new JdiReflective(basename, parent);
        c = i.current();
        if (c == ';')
            return new GenTypeClass(reflective, (List) null);

        List params = new ArrayList();
        if (c != '<') {
            Debug.message("Generic signature: expected '<', got '" + c + "' ??");
            return null;
        }

        do {
            GenType ptype = fromSignature(i, tparams, parent);
            if (ptype == null)
                return null;
            params.add(ptype);
        } while (i.peek() != '>');
        i.next(); // fetch the '>'
        i.next(); // fetch the trailing ';'

        return new GenTypeClass(reflective, params);
    }

    private static GenType getNonGenericType(String typeName, Type t, ClassLoaderReference clr, VirtualMachine vm)
    {
        if (t instanceof BooleanType)
            return new GenTypeBool();
        else if (t instanceof ByteType)
            return new GenTypeByte();
        else if (t instanceof CharType)
            return new GenTypeChar();
        else if (t instanceof DoubleType)
            return new GenTypeDouble();
        else if (t instanceof FloatType)
            return new GenTypeFloat();
        else if (t instanceof IntegerType)
            return new GenTypeInt();
        else if (t instanceof LongType)
            return new GenTypeLong();
        else if (t instanceof ShortType)
            return new GenTypeShort();
        else {
            // The class may or may not be loaded.
            Reflective ref;
            if (t == null)
                ref = new JdiReflective(typeName, clr, vm);
            else {
                ReferenceType rt = findClass(t.name(), clr, vm);
                ref = new JdiReflective(rt);
            }
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
    public static GenType fromField(Field f, JdiObject parent)
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
                                          // loaded
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

        if (gensig == null)
            return getNonGenericType(f.typeName(), t, parent.obj.referenceType().classLoader(), parent.obj
                    .virtualMachine());

        // generic version.
        // Get the mapping of type parameter names to actual types. Then,
        // map the names to the class/interface the field is actually defined
        // in.

        Map tparams = parent.getGenericParams();
        Reflective r = new JdiReflective(parent.obj.referenceType());

        // For any parameters that we don't have explicit information for,
        // substitute an appropriate wildcard type.
        if (tparams == null)
            tparams = new HashMap();
        GenTypeClass.addDefaultParamBases(tparams, r);
        GenTypeClass genType = new GenTypeClass(r, tparams);

        // genType is now the actual type of the parent object. Map the
        // parameters to the declaring type:
        tparams = genType.mapToSuper(f.declaringType().name());
        StringIterator iterator = new StringIterator(gensig);
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
    public static GenType fromField(Field f, ReferenceType parent)
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

    public static GenType fromLocalVar(StackFrame sf, LocalVariable var)
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
        // Map tparams = null;
        Map tparams = new HashMap();
        GenTypeClass.addDefaultParamBases(tparams, new JdiReflective(declType));
        return fromSignature(iterator, tparams, declType);
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
    };
}