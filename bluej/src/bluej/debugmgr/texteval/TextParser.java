package bluej.debugmgr.texteval;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamHiddenTokenFilter;
import antlr.collections.AST;
import bluej.Config;
import bluej.debugger.gentype.*;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Parsing routines for the code pad.
 *  
 * @author Davin McCall
 * @version $Id: TextParser.java 3327 2005-03-03 01:10:45Z davmac $
 */
public class TextParser
{
    private ClassLoader classLoader;
    private String packageScope;  // evaluation package
    private ObjectBench objectBench;

    private static JavaUtils jutils = JavaUtils.getJavaUtils();
    private static boolean java15 = Config.isJava15();
    
    /**
     * TextParser constructor. Defines the class loader and package scope
     * for evaluation.
     */
    public TextParser(ClassLoader classLoader, String packageScope, ObjectBench ob)
    {
        this.classLoader = classLoader;
        this.packageScope = packageScope;
        this.objectBench = ob;
    }
    
    /**
     * Parse a string entered into the code pad. Return is null if the string
     * is a statement, the empty string if it is an expression whose type cannot
     * be determined, or a string representing the result type (of an
     * expression).
     */
    public String parseCommand(String command)
    {
        boolean parsedOk = false;
        AST rootAST;

        // Multiple semi-colon to avoid hitting "end-of-file" which breaks
        // parse
        JavaRecognizer parser = getParser("{" + command + "};;;;;");
        
        // start parsing at the classBlock rule
        try {
            parser.compoundStatement();
            rootAST = parser.getAST();
            parsedOk = true;
            // Debug.message("It seems to be a statement.");
        }
        catch(RecognitionException re) { }
        catch(TokenStreamException tse) { }
      
        if (! parsedOk) {
            // It might just be an expression. Multiple semi-colon to ensure
            // end-of-file not hit (causes parse failure).
            parser = getParser(command + ";;;;;");
            
            try {
                parser.expression();
                rootAST = parser.getAST();
                parsedOk = true; // it parses as an expression.
                
                ExprValue ev = getExpressionType(rootAST);
                GenType t = ev != null ? ev.getType() : null;

                if (t == null)
                    return "";
                else if (t.isVoid())
                    return null;
                else
                    return t.toString();
            }
            catch(RecognitionException re) { }
            catch(SemanticException se) { }
            catch(TokenStreamException tse) { }
            return "";
        }
        return null;
    }

    /**
     * Java 1.4 & prior version of trinary "? :" operator. See JLS 2nd ed. 
     * section 15.25.
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    private GenType questionOperator14(AST node) throws RecognitionException, SemanticException
    {
        AST trueAlt = node.getFirstChild().getNextSibling();
        AST falseAlt = trueAlt.getNextSibling();
        ExprValue trueAltEv = getExpressionType(trueAlt);
        ExprValue falseAltEv = getExpressionType(falseAlt);
        GenType trueAltType = trueAltEv.getType();
        GenType falseAltType = falseAltEv.getType();
        
        // if the operands have the same type, that is the result type
        if (trueAltType.equals(falseAltType))
            return trueAltType;
        
        if (trueAltType.isNumeric() && falseAltType.isNumeric()) {
            // if one type is short and the other is byte, result type is short
            if (trueAltType.typeIs(GenType.GT_SHORT) && falseAltType.typeIs(GenType.GT_BYTE))
                return GenTypePrimitive.getShort();
            if (falseAltType.typeIs(GenType.GT_SHORT) && trueAltType.typeIs(GenType.GT_BYTE))
                return GenTypePrimitive.getShort();
            
            // if one type is byte/short/char and the other is a constant of
            // type int whose value fits, the result type is byte/short/char
            if (falseAltType.typeIs(GenType.GT_INT) && falseAltEv.knownValue()) {
                int fval = falseAltEv.intValue();
                if (isMinorInteger(trueAltType) && trueAltType.couldHold(fval))
                    return trueAltType;
            }
            if (trueAltType.typeIs(GenType.GT_INT) && trueAltEv.knownValue()) {
                int fval = trueAltEv.intValue();
                if (isMinorInteger(falseAltType) && falseAltType.couldHold(fval))
                    return falseAltType;
            }
            
            // binary numeric promotion is applied
            return binaryNumericPromotion(trueAltType, falseAltType);
        }
        
        // otherwise it must be possible to convert one type to the other by
        // assignment conversion:
        if (trueAltType.isAssignableFrom(falseAltType))
            return trueAltType;
        if (falseAltType.isAssignableFrom(trueAltType))
            return falseAltType;
        
        throw new SemanticException();
    }
    
    /**
     * Test if a given type is one of the "minor" integral types: byte, char
     * or short.
     */
    private static boolean isMinorInteger(GenType a)
    {
        return a.typeIs(GenType.GT_BYTE) || a.typeIs(GenType.GT_CHAR) || a.typeIs(GenType.GT_SHORT); 
    }
    
    /**
     * Java 1.5 version of the trinary "? :" operator.
     * See JLS section 15.24. Note that JLS 3rd ed. differs extensively
     * from JLS 2nd edition. The changes are not backwards compatible.
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    private GenType questionOperator15(AST node) throws RecognitionException, SemanticException
    {
        AST trueAlt = node.getFirstChild().getNextSibling();
        AST falseAlt = trueAlt.getNextSibling();
        ExprValue trueAltEv = getExpressionType(trueAlt);
        ExprValue falseAltEv = getExpressionType(falseAlt);
        GenType trueAltType = trueAltEv.getType();
        GenType falseAltType = falseAltEv.getType();
        
        // if we don't know the type of both alternatives, we don't
        // know the result type:
        if (trueAltType == null || falseAltType == null)
            return null;
        
        // Neither argument can be a void type.
        if (trueAltType.isVoid() || falseAltType.isVoid())
            throw new SemanticException();
        
        // if the second & third arguments have the same type, then
        // that is the result type:
        if (trueAltType.equals(falseAltType))
            return trueAltType;
        
        GenType trueUnboxed = unBox(trueAltType);
        GenType falseUnboxed = unBox(falseAltType);
        
        // if one the arguments is of type boolean and the other
        // Boolean, the result type is boolean.
        if (trueUnboxed.typeIs(GenType.GT_BOOLEAN) && falseUnboxed.typeIs(GenType.GT_BOOLEAN))
            return trueUnboxed;
        
        // if one type is null and the other is a reference type, the
        // return is that reference type.
        //   Also partially handle the final case from the JLS,
        // involving boxing conversion & capture conversion (which
        // is trivial when non-parameterized types such as boxed types
        // are involved)
        // 
        // This precludes either type from being null later on.
        if (trueAltType.typeIs(GenType.GT_NULL))
            return boxType(falseAltType);
        if (falseAltType.typeIs(GenType.GT_NULL))
            return boxType(trueAltType);
        
        // if the two alternatives are convertible to numeric types,
        // there are several cases:
        if (trueUnboxed.isNumeric() && falseUnboxed.isNumeric()) {
            // If one is byte/Byte and the other is short/Short, the
            // result type is short.
            if (trueUnboxed.typeIs(GenType.GT_BYTE) && falseUnboxed.typeIs(GenType.GT_SHORT))
                return falseUnboxed;
            if (falseUnboxed.typeIs(GenType.GT_BYTE) && trueUnboxed.typeIs(GenType.GT_SHORT))
                return trueUnboxed;
            
            // If one type, when unboxed, is byte/short/char, and the
            // other is an integer constant whose value fits in the
            // first, the result type is the (unboxed) former type. (The JLS
            // takes four paragraphs to say this, but the result is the
            // same).
            if (isMinorInteger(trueUnboxed) && falseAltType.typeIs(GenType.GT_INT) && falseAltEv.knownValue()) {
                int kval = falseAltEv.intValue();
                if (trueUnboxed.couldHold(kval))
                    return trueUnboxed;
            }
            if (isMinorInteger(falseUnboxed) && trueAltType.typeIs(GenType.GT_INT) && trueAltEv.knownValue()) {
                int kval = trueAltEv.intValue();
                if (falseUnboxed.couldHold(kval))
                    return falseUnboxed;
            }
            
            // Otherwise apply binary numeric promotion
            return binaryNumericPromotion(trueAltType, falseAltType);
        }
        
        // Box both alternatives:
        trueAltType = boxType(trueAltType);
        falseAltType = boxType(falseAltType);
        
        if (trueAltType instanceof GenTypeParameterizable && falseAltType instanceof GenTypeParameterizable) {
            // apply capture conversion (JLS 5.1.10) to lub() of both
            // alternatives (JLS 15.12.2.7)
            GenTypeSolid [] trueUbounds = ((GenTypeParameterizable) trueAltType).getUpperBounds();
            GenTypeSolid [] falseUbounds = ((GenTypeParameterizable) falseAltType).getUpperBounds();
            GenTypeClass [] ctypes = new GenTypeClass[2];
            ctypes[0] = trueUbounds[0].asClass();
            ctypes[1] = falseUbounds[0].asClass();
            return lub(ctypes).getUpperBounds()[0];
        }
        
        return null;
    }
    
    
    /**
     * Calculate lub, as defined in revised JLS 15.12.2. Essentially this
     * means, calculate the type to which all the given types are
     * convertible.<p>
     * 
     * The JLS specifies lub returns a set of types A & B ...
     * This method actually returns a wildcard "? extends A & B ...".
     */
    private GenTypeParameterizable lub(GenTypeClass [] ubounds)
    {
        Stack btstack = new Stack();
        return lub(ubounds, btstack);
    }
    
    private GenTypeParameterizable lub(GenTypeClass [] ubounds, Stack lubBt)
    {
        // "lowest(/least) upper bound"?
        
        // first check for infinite recursion:
        Iterator si = lubBt.iterator();
        while (si.hasNext()) {
            GenTypeClass [] sbounds = (GenTypeClass []) si.next();
            int i;
            for (i = 0; i < sbounds.length; i++) {
                if (! sbounds[i].equals(ubounds[i]))
                    break;
            }
            if (i == sbounds.length)
                return new GenTypeUnbounded();
            // TODO this is really supposed to result in a recursively-
            // defined type.
        }

        lubBt.push(ubounds);
        List l = new ArrayList();
        Reflective [] mec = MinimalErasedCandidateSet(ubounds);
        for (int i = 0; i < mec.length; i++) {
            l.add(Candidate(mec[i], ubounds, lubBt));
        }
        lubBt.pop();
        
        GenTypeSolid [] lbounds = new GenTypeSolid[0];
        GenTypeSolid [] rubounds = (GenTypeSolid []) l.toArray(lbounds);
        return new GenTypeWildcard(rubounds, lbounds);
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
    private GenTypeClass Candidate(Reflective t, GenTypeClass [] ubounds, Stack lubBt)
    {
        if (t.getTypeParams().isEmpty())
            return new GenTypeClass(t);
        else {
            // CandidateInvocation(G) = lci(Inv(G))
            GenTypeClass [] ri = relevantInvocations(t, ubounds);
            return leastContainingInvocation(ri, lubBt);
        }
    }
    
    /**
     * Find the least containing invocation from a set of invocations. The
     * invocations a, b, ... are types based on the same class G. The return is
     * a generic type G<...> such that all  a, b, ... are convertible to the
     * return type.<p>
     * 
     * This is "lci" as defined in the proposed JLS section 15.12.2.7 
     * 
     * @param types   The invocations
     * @param lubBt   A backtrace used to avoid infinite recursion
     * @return   The least containing type
     */
    private GenTypeClass leastContainingInvocation(GenTypeClass [] types, Stack lubBt)
    {
        GenTypeClass rtype = types[0];
        for (int i = 1; i < types.length; i++) {
            rtype = leastContainingInvocation(rtype, types[i], lubBt);
        }
        return rtype;
    }
    
    /**
     * Find the least containing invocation from two invocations.
     */
    private GenTypeClass leastContainingInvocation(GenTypeClass a, GenTypeClass b, Stack lubBt)
    {
        if (! a.getReflective().getName().equals(b.getReflective().getName()))
            throw new IllegalArgumentException("Class types must be the same.");
        
        Map ma = a.getMap();
        Map mb = b.getMap();
        if (ma == null || mb == null)
            return new GenTypeClass(a.getReflective());
        
        Map rmap = new HashMap();
        Iterator i = ma.entrySet().iterator();

        // lci(G<X1,...,Xn>, G<Y1,...,Yn>) =
        //       G<lcta(X1,Y1), ..., lcta(Xn,Yn)>
        while (i.hasNext()) {
            Map.Entry mentry = (Map.Entry) i.next();
            GenTypeParameterizable atype = (GenTypeParameterizable) mentry.getValue();
            GenTypeParameterizable btype = (GenTypeParameterizable) mb.get(mentry.getKey());
            GenTypeParameterizable rtype = leastContainingTypeArgument(atype, btype, lubBt);
            rmap.put(mentry.getKey(), rtype);
        }
        return new GenTypeClass(a.getReflective(), rmap);
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
    private GenTypeParameterizable leastContainingTypeArgument(GenTypeParameterizable a, GenTypeParameterizable b, Stack lubBt)
    {
        GenTypeClass ac = a.asClass();
        GenTypeClass bc = b.asClass();
        
        // Both arguments are of solid type
        if (ac != null && bc != null) {
            if (ac.equals(bc))
                return ac;
            else
                return lub(new GenTypeClass [] {ac, bc}, lubBt);
        }
        
        
        if (ac != null || bc != null) {
            // One is a solid type and the other is a wilcard type. Ensure
            // that ac is the solid and b is the wildcard:
            if (ac == null) {
                ac = bc;
                b = a;
            }

            GenTypeSolid [] lbounds = b.getLowerBounds();
            if (lbounds.length != 0) {
                // lcta(U, ? super V) = ? super glb(U,V)
                GenTypeSolid [] newlbounds = new GenTypeSolid[lbounds.length + 1];
                System.arraycopy(lbounds, 0, newlbounds, 1, lbounds.length);
                newlbounds[0] = ac;
                return new GenTypeWildcard(new GenTypeSolid[0], newlbounds);
            }
        }
        
        GenTypeSolid [] lboundsa = a.getLowerBounds();
        GenTypeSolid [] lboundsb = b.getLowerBounds();
        if (lboundsa != null && lboundsb != null) {
            // lcta(? super U, ? super V = ? super glb(U,V)
            GenTypeSolid [] newlbounds = new GenTypeSolid[lboundsa.length + lboundsb.length];
            System.arraycopy(lboundsa, 0, newlbounds, 0, lboundsa.length);
            System.arraycopy(lboundsb, 0, newlbounds, lboundsa.length, lboundsb.length);
            return new GenTypeWildcard(new GenTypeSolid[0], newlbounds);
        }
        
        if (lboundsa != null || lboundsb != null) {
            // lcta(? super U, ? extends V)
            GenTypeSolid [] ubounds;
            if (lboundsa == null) {
                lboundsa = lboundsb;
                ubounds = a.getUpperBounds();
            }
            else
                ubounds = b.getUpperBounds();
            
            // we'll check if any upper bounds matches any lower bounds. This
            // is not exactly as in the JLS, which doesn't really specify what
            // to do in the case of multiple types
            for (int i = 0; i < lboundsa.length; i++) {
                for (int j = 0; j < ubounds.length; j++) {
                    if (lboundsa[i].equals(ubounds[j]))
                        return lboundsa[i];
                }
            }
            
            // otherwise return good old '?'.
            return new GenTypeUnbounded();
        }
        
        // The only option left is lcta(? extends U, ? extends V)
        GenTypeSolid [] uboundsa = a.getUpperBounds();
        GenTypeSolid [] uboundsb = b.getUpperBounds();
        GenTypeClass [] args = new GenTypeClass[uboundsa.length + uboundsb.length];
        System.arraycopy(uboundsa, 0, args, 0, uboundsa.length);
        System.arraycopy(uboundsb, 0, args, uboundsa.length, uboundsb.length);
        return lub(args);
    }
    
    /**
     * Aggregate types, checking for consistency. This is "glb" as defined
     * in the JLS section 5.1.10
     * 
     * @throws SemanticException
     */
    private GenTypeSolid [] glb(GenTypeSolid [] args) throws SemanticException
    {
        for (int i = 0; i < args.length; i++) {

            // if i is an interface (not a class), continue
            if (args[i].isInterface())
                continue;
            
            for (int j = 0; j < args.length; j++) {
                if (i == j || args[j].isInterface())
                    continue;
                
                // if both are class types, and neither is assignable to the
                // other, it is a "compile time" error.
                if (! (args[i].isAssignableFrom(args[j]) || args[j].isAssignableFrom(args[i])))
                    throw new SemanticException();
            }
        }
        return args;
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
    private GenTypeClass [] relevantInvocations(Reflective r, GenTypeClass [] ubounds)
    {
        GenTypeClass [] rlist = new GenTypeClass[ubounds.length];
        for (int i = 0; i < ubounds.length; i++) {
            rlist[i] = ubounds[i].mapToSuper2(r.getName());
        }
        
        return rlist;
    }
    
    /**
     * Get the erased (raw) super types of some type, put them in the given
     * map. The given type itself is also stored in the map. This is a
     * recursive method which also uses the map to avoid processing types
     * more than once.<P>
     * 
     * This is "EST" as defined in the proposed JLS section 15.12.2.7
     * 
     * @param r    The type whose supertypes to find
     * @param rmap The map (String -&gt; Reflective) in which to store the
     *             supertypes.
     */
    private void ErasedSuperTypes(Reflective r, Map rmap)
    {
        String rname = r.getName();
        if (! rmap.containsKey(rname)) {
            rmap.put(rname, r);
            List supertypes = r.getSuperTypesR();
            Iterator i = supertypes.iterator();
            while (i.hasNext()) {
                ErasedSuperTypes((Reflective) i.next(), rmap);
            }
        }
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
    private Reflective [] MinimalErasedCandidateSet(GenTypeClass [] types)
    {
        // have to find *intersection* of all sets and remove redundant types
        
        Map rmap = new HashMap();
        ErasedSuperTypes(types[0].getReflective(), rmap);
        
        for (int i = 1; i < types.length; i++) {
            Map rmap2 = new HashMap();
            ErasedSuperTypes(types[i].getReflective(), rmap2);
            
            // find the intersection incrementally
            Iterator j = rmap2.keySet().iterator();
            while (j.hasNext()) {
                if( ! rmap.containsKey(j.next()))
                    j.remove();
            }
            rmap = rmap2;
        }
        
        // Now remove redundant types
        Set entryset = rmap.entrySet();
        Iterator i = entryset.iterator();
        while (i.hasNext()) {
            Iterator j = entryset.iterator();
            Map.Entry ielem = (Map.Entry) i.next();
            
            while (j.hasNext()) {
                Map.Entry jelem = (Map.Entry) j.next();
                if (ielem == jelem)
                    continue;
                
                Reflective ri = (Reflective) ielem.getValue();
                Reflective ji = (Reflective) jelem.getValue();
                if (ri.isAssignableFrom(ji)) {
                    i.remove();
                    break;
                }
            }
        }
        
        Reflective [] rval = new Reflective[rmap.values().size()];
        rmap.values().toArray(rval);
        
        return rval;
    }
    
    /**
     * binary numeric promotion, as defined by JLS section 5.6.2. Both
     * operands must be (possibly boxed) numeric types.
     */
    private GenType binaryNumericPromotion(GenType a, GenType b)
        throws SemanticException
    {
        GenType ua = unBox(a);
        GenType ub = unBox(b);

        if (a.typeIs(GenType.GT_DOUBLE) || b.typeIs(GenType.GT_DOUBLE))
            return GenTypePrimitive.getDouble();

        if (a.typeIs(GenType.GT_FLOAT) || b.typeIs(GenType.GT_FLOAT))
            return GenTypePrimitive.getFloat();

        if (a.typeIs(GenType.GT_LONG) || b.typeIs(GenType.GT_LONG))
            return GenTypePrimitive.getLong();

        if (a.isNumeric() && b.isNumeric())
            return GenTypePrimitive.getInt();
        else
            throw new SemanticException();
    }
    
    /**
     * Get the GenType of a character literal node.
     * 
     * @throws RecognitionException
     */
    private ExprValue getCharLiteral(AST node) throws RecognitionException
    {
        // char literal is either 'x', or '\\uXXXX' notation, or '\t' etc.
        String x = node.getText();
        x = x.substring(1, x.length() - 1); // strip single quotes
        
        final GenType charType = GenTypePrimitive.getChar();
        if (! x.startsWith("\\")) {
            // This is the normal case
            if (x.length() != 1)
                throw new RecognitionException();
            else
                return new NumValue(charType, new Integer(x.charAt(0)));
        }
        else if (x.equals("\\b"))
            return new NumValue(charType, new Integer('\b'));
        else if (x.equals("\\t"))
            return new NumValue(charType, new Integer('\t'));
        else if (x.equals("\\n"))
            return new NumValue(charType, new Integer('\n'));
        else if (x.equals("\\f"))
            return new NumValue(charType, new Integer('\f'));
        else if (x.equals("\\r"))
            return new NumValue(charType, new Integer('\r'));
        else if (x.equals("\\\""))
            return new NumValue(charType, new Integer('"'));
        else if (x.equals("\\'"))
            return new NumValue(charType, new Integer('\''));
        else if (x.equals("\\\\"))
            return new NumValue(charType, new Integer('\\'));
        else if (x.startsWith("\\u")) {
            // unicode escape, as a 4-digit hexadecimal
            if (x.length() != 6)
                throw new RecognitionException();
            
            char val = 0;
            for (int i = 0; i < 4; i++) {
                char digit = x.charAt(i + 2);
                int digVal = Character.digit(digit, 16);
                if (digVal == -1)
                    throw new RecognitionException();
                val = (char)(val * 16 + digVal);
            }
            return new NumValue(charType, new Integer(val));
        }
        else {
            // octal escape, up to three digits
            int xlen = x.length();
            if (xlen < 2 || xlen > 4)
                throw new RecognitionException();
            
            char val = 0;
            for (int i = 0; i < xlen - 1; i++) {
                char digit = x.charAt(i+1);
                int digVal = Character.digit(digit, 8);
                if (digVal == -1) {
                        throw new RecognitionException();
                }
                val = (char)(val * 8 + digVal);
            }
            return new NumValue(charType, new Integer(val));
        }
    }
    
    /**
     * Get the GenType corresponding to an integer literal node.
     * @throws RecognitionException
     */
    private ExprValue getIntLiteral(AST node, boolean negative) throws RecognitionException
    {
        String x = node.getText();
        if (negative)
            x = "-" + x;
        
        try {
            Integer val = Integer.decode(x);
            return new NumValue(GenTypePrimitive.getInt(), val);
        }
        catch (NumberFormatException nfe) {
            throw new RecognitionException();
        }
    }
    
    /**
     * Ge the GenType corresponding to a long literal node.
     * @throws RecognitionException
     */
    private ExprValue getLongLiteral(AST node, boolean negative) throws RecognitionException
    {
        String x = node.getText();
        if (negative)
            x = "-" + x;
        
        try {
            Long val = Long.decode(x);
            return new NumValue(GenTypePrimitive.getLong(), val);
        }
        catch (NumberFormatException nfe) {
            throw new RecognitionException();
        }
    }
    
    /**
     * Get the GenType corresponding to a float literal.
     * @throws RecognitionException
     */
    private ExprValue getFloatLiteral(AST node, boolean negative) throws RecognitionException
    {
        String x = node.getText();
        if (negative)
            x = "-" + x;
        
        try {
            Float val = Float.valueOf(x);
            return new NumValue(GenTypePrimitive.getFloat(), val);
        }
        catch (NumberFormatException nfe) {
            throw new RecognitionException();
        }
    }
    
    /**
     * Get the GenType corresponding to a double literal.
     * @throws RecognitionException
     */
    private ExprValue getDoubleLiteral(AST node, boolean negative) throws RecognitionException
    {
        String x = node.getText();
        if (negative)
            x = "-" + x;
        
        try {
            Double val = Double.valueOf(x);
            return new NumValue(GenTypePrimitive.getDouble(), val);
        }
        catch (NumberFormatException nfe) {
            throw new RecognitionException();
        }
    }
    
    
    /**
     * Attempt to load, by its unqualified name,  a class which might be in the
     * current package or which might be in java.lang.
     */
    private Class loadUnqualifiedClass(String className) throws ClassNotFoundException
    {
        boolean qualified = false;
        int index = 0;
        
        // It's an unqualified name - try package scope
        try {
            if (packageScope.length() != 0)
                return classLoader.loadClass(packageScope + "." + className);
            else
                return classLoader.loadClass(className);
        }
        catch(ClassNotFoundException cnfe) {}
        
        // Finally, try java.lang
        return classLoader.loadClass("java.lang." + className);
    }
    
    /**
     * Extract the type from a node. The type is in the form of a qualified
     * or unqualified class name, with optional type parameters.
     * 
     * @param node   The node representing the type
     * 
     * @throws RecognitionException, SemanticException
     */
    private GenTypeSolid getType(AST node) throws RecognitionException, SemanticException
    {        
        if (node.getType() == JavaTokenTypes.IDENT) {
            // A class name
            // the children are the type parameters
            List params = getTParams(node.getFirstChild());
            
            try {
                Class c = loadUnqualifiedClass(node.getText());
                Reflective r = new JavaReflective(c);
                return new GenTypeClass(r, params);
            }
            catch(ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
        }
        else if (node.getType() == JavaTokenTypes.DOT) {
            // The children nodes are: the qualified class name, and then
            // the type arguments
            AST packageNode = node.getFirstChild();
            PackageOrClass porc = getPackageOrType(packageNode);
            
            // String packagen = combineDotNames(packageNode, '.');

            AST classNode = packageNode.getNextSibling();
            List params = getTParams(classNode.getNextSibling());

            PackageOrClass nodePorC = porc.getPackageOrClassMember(classNode.getText());
            GenTypeClass nodeClass = (GenTypeClass) nodePorC.getType();

            // Don't pass in an outer class if it's not generic anyway 
            if (! nodeClass.isGeneric())
                nodeClass = null;
            return new GenTypeClass(nodeClass.getReflective(), params, nodeClass);
        }
        else {
            throw new RecognitionException();
        }
    }
    
    /**
     * Get a sequence of type parameters.
     * 
     * @param node  The node representing the first tpar (or null).
     * @return      A list of GenType representing the type parameters
     * @throws RecognitionException
     * @throws SemanticException
     */
    private List getTParams(AST node) throws RecognitionException, SemanticException
    {
        List params = new LinkedList();
        
        // get the type parameters
        while(node != null && node.getType() == JavaTokenTypes.TYPE_ARGUMENT) {
            AST childNode = node.getFirstChild();
            GenType tparType;
            
            if (childNode.getType() == JavaTokenTypes.WILDCARD_TYPE) {
                // wildcard parameter
                AST boundNode = childNode.getNextSibling();
                int boundType = boundNode.getType();
                
                // it's either an upper or lower bound
                if (boundType == JavaTokenTypes.TYPE_UPPER_BOUNDS) {
                    tparType = new GenTypeExtends(getType(boundNode.getFirstChild()));
                }
                else if (boundType == JavaTokenTypes.TYPE_LOWER_BOUNDS) {
                    tparType = new GenTypeSuper(getType(boundNode.getFirstChild()));
                }
                else
                    throw new RecognitionException();
            }
            else {
                // "solid" parameter
                tparType = getType(node.getFirstChild());
            }
            params.add(tparType);
            node = node.getNextSibling();
        }
        
        return params;
    }
    
    /**
     * Return a string represenetation of a node which represents a dot-name
     * (eg "a", "a.b", "a.b.c" etc). A custom seperator can be used.
     * 
     * @param node    The node representing the dot-name
     * @param seperator  The seperator to use in the returned string
     * @return        The string representation of the node
     * @throws RecognitionException
     */
    String combineDotNames(AST node, char seperator) throws RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT)
            return node.getText();
        else if (node.getType() == JavaTokenTypes.DOT) {
            AST fchild = node.getFirstChild();
            AST nchild = fchild.getNextSibling();
            if (nchild.getType() != JavaTokenTypes.IDENT)
                throw new RecognitionException();
            return combineDotNames(fchild, seperator) + seperator + nchild.getText(); 
        }
        else
            throw new RecognitionException();
    }
    
    /**
     * Get the type from a node which must by context be an inner class type.
     * @param node   The node representing the type
     * @param outer  The containing type
     * *
     * @throws SemanticException
     * @throws RecognitionException
     */
    GenType getInnerType(AST node, GenTypeClass outer) throws SemanticException, RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT) {
            // A simple name<params> expression
            List params = getTParams(node.getFirstChild());
            
            String name = outer.rawName() + '$' + node.getText();
            try {
                Class theClass = classLoader.loadClass(name);
                Reflective r = new JavaReflective(theClass);
                return new GenTypeClass(r, params, outer);
            }
            catch (ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
        }
        else if (node.getType() == JavaTokenTypes.DOT) {
            // A name.name<params> style expression
            // The children nodes are: the qualified class name, and then
            // the type arguments
            AST packageNode = node.getFirstChild();
            String dotnames = combineDotNames(packageNode, '$');

            AST classNode = packageNode.getNextSibling();
            List params = getTParams(classNode.getNextSibling());

            String name = outer.rawName() + '$' + dotnames + '$' + node.getText();
            try {
                Class c = classLoader.loadClass(name);
                Reflective r = new JavaReflective(c);
                return new GenTypeClass(r, params);
            }
            catch(ClassNotFoundException cnfe) {
                throw new SemanticException();
            }
            
        }
        else
            throw new RecognitionException();
    }
    
    /**
     * Parse a node as an entity (which could be a package, class or value).
     * @throws SemanticException
     * @throws RecognitionException
     */
    Entity getEntity(AST node) throws SemanticException, RecognitionException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            // Treat it first as a variable, then a type, then as a package.
            String nodeText = node.getText();
            ObjectWrapper ow = objectBench.getObject(nodeText);
            if (ow != null)
                return new ValueEntity(ow.getGenType());
            
            try {
                Class c = loadUnqualifiedClass(nodeText);
                return new TypeEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                return new PackageEntity(nodeText);
            }
        }
        
        // A dot-node in the form xxx.identifier:
        if (node.getType() == JavaTokenTypes.DOT) {
            AST firstChild = node.getFirstChild();
            AST secondChild = firstChild.getNextSibling();
            if (secondChild.getType() == JavaTokenTypes.IDENT) {
                Entity firstpart = getEntity(firstChild);
                return firstpart.getSubentity(secondChild.getText());
            }
            // Don't worry about xxx.super, it shouldn't be used at this
            // level.
        }
        
        // Anything else must be an expression, therefore a value:
        GenType exprType = getExpressionType(node).getType();
        return new ValueEntity(exprType);
    }
    
    /**
     * Get an entity which by context must be either a package or a (possibly
     * generic) type.
     * *
     * @throws SemanticException
     */
    private PackageOrClass getPackageOrType(AST node) throws SemanticException, RecognitionException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            // Treat it first as a type, then as a package.
            String nodeText = node.getText();
            List tparams = getTParams(node.getFirstChild());
            
            try {
                Class c = loadUnqualifiedClass(nodeText);
                TypeEntity r = new TypeEntity(c);
                r.setTypeParams(tparams);
                return r;
            }
            catch (ClassNotFoundException cnfe) {
                // Could not be loaded as a class, so it must be a package.
                if (! tparams.isEmpty())
                    throw new SemanticException();
                return new PackageEntity(nodeText);
            }
        }
        
        // A dot-node in the form xxx.identifier:
        if (node.getType() == JavaTokenTypes.DOT) {
            AST firstChild = node.getFirstChild();
            AST secondChild = firstChild.getNextSibling();
            if (secondChild.getType() == JavaTokenTypes.IDENT) {
                List tparams = getTParams(secondChild.getFirstChild());
                PackageOrClass firstpart = getPackageOrType(firstChild);

                PackageOrClass entity = firstpart.getPackageOrClassMember(secondChild.getText());
                entity.setTypeParams(tparams);
            }
        }
        
        throw new SemanticException();
    }
    
    /**
     * Get an expression list node as an array of GenType
     * 
     * @throws SemanticException
     * @throws RecognitionException
     */
    private GenType [] getExpressionList(AST node) throws SemanticException, RecognitionException
    {
        int num = node.getNumberOfChildren();
        GenType [] r = new GenType[num];
        AST child = node.getFirstChild();
        
        // loop through the child nodes
        for (int i = 0; i < num; i++) {
            r[i] = getExpressionType(child).getType();
            child = child.getNextSibling();
        }
        return r;
    }
    
    /**
     * Check whether a particular method is callable with particular
     * parameters. If so return information about how specific the call is.
     * If the parameters cannot be applied to this method, return null.
     * 
     * @param targetType   The type of object/class to which the method is
     *                     being applied
     * @param tpars     The explicitly specified type parameters used in the
     *                  invocation of a generic method
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private MethodCallDesc isMethodApplicable(GenTypeClass targetType, Map tpars, Method m, GenType [] args)
        throws RecognitionException
    {
        // TODO support varargs, autoboxing/unboxing, generic methods
        // the rule for autoboxing seems to be, if two methods are applicable
        // but one requires autoboxing and the other doesn't, choose the one
        // that doesn't. Otherwise amibiguity.
        
        // First check that at least the number of parameters is correct. If
        // type parameters are explicitly stated, also check that their number
        // is correct.
        GenType [] mparams = JavaUtils.getJavaUtils().getParamGenTypes(m, targetType.isRaw());
        if (mparams.length != args.length)
            return null;
        List tparams = JavaUtils.getJavaUtils().getTypeParams(m);
        if (! tpars.isEmpty() && tpars.size() != tparams.size())
            return null;
        // Map methodTPars = JavaUtils.TParamsToMap(tparams);
        
        // at the moment, doesn't handle generic methods.
        if (! tparams.isEmpty())
            throw new RecognitionException();
        
        // Get a map of type parameter names to types from the target type,
        // but remove mappings for type parameter names which also occur as
        // part of the generic method declaration (as these override those
        // from the class declaration)
        Map targetTpars = targetType.getMap();
        if (targetTpars != null) {
            Iterator i = tparams.iterator();
            while (i.hasNext())
                targetTpars.remove(((GenTypeDeclTpar) i.next()).getTparName());
        }
        
        // For each argument, must check the compatibility of the supplied
        // parameter type with the argument type; and if neither the formal
        // parameter or supplied argument are raw, then must check generic
        // contract as well.
        
        for (int i = 0; i < args.length; i++) {
            GenType formalArg = mparams[i];
            GenType givenParam = args[i];
            
            // If type arguments specified, use those. Also bring in class
            // type parameters.
            formalArg = formalArg.mapTparsToTypes(targetTpars);
            formalArg = formalArg.mapTparsToTypes(tpars);
            
            // check if the given parameter doesn't match the formal argument
            if (! formalArg.isAssignableFrom(givenParam))
                return null;
        }
        
        GenType rType = jutils.getReturnType(m).mapTparsToTypes(targetType.getMap());
        return new MethodCallDesc(m, Arrays.asList(mparams), false, false, rType);
    }
    
    /**
     * Get the return type of a method call expression. This is quite
     * complicated; see JLS section 15.12
     *
     * @throws RecognitionException
     * @throws SemanticException
     */
    private GenType getMethodCallReturnType(AST node) throws RecognitionException, SemanticException
    {
        // For a method call node, the first child is the method name
        // (possibly a dot-name) and the second is an ELIST.
        //
        // In the case of the method name being a dot-name, it may also
        // be a generic type. In this case the children of the dot-node
        // are:
        //
        // <object-expression> <type-arg-1> <type-arg-2> .... <methodname>
        //
        // Where <object-expression> may actually be a type (ie. invoking
        // a static method).
        // TODO getClass() is a special case, it should return Class<? extends
        // basetype>
        
        JavaUtils jutils = JavaUtils.getJavaUtils();
        
        AST firstArg = node.getFirstChild();
        AST secondArg = firstArg.getNextSibling();
        if (secondArg.getType() != JavaTokenTypes.ELIST)
            throw new RecognitionException();
        
        // we don't handle method calls where the object/class is not
        // specified...
        // that would be calling a shell class method. The user shouldn't
        // do that.
        
        if (firstArg.getType() == JavaTokenTypes.DOT) {
            AST targetNode = firstArg.getFirstChild();
            Entity callTarget = getEntity(targetNode);
            GenType targetType = callTarget.getType();
                            
            // now get method name, and argument types;
            List typeArgs = new ArrayList(5);
            GenType [] argumentTypes = getExpressionList(secondArg);
            String methodName = null;
            AST searchNode = targetNode.getNextSibling();
            
            // get the type arguments and method name
            while (searchNode != null) {
                int nodeType = searchNode.getType();
                // type argument?
                if (nodeType == JavaTokenTypes.TYPE_ARGUMENT) {
                    GenType taType = getType(searchNode.getFirstChild());
                    typeArgs.add(taType);
                }
                // method name?
                else if (nodeType == JavaTokenTypes.IDENT) {
                    methodName = searchNode.getText();
                    break;
                }
                else
                    break;
                
                searchNode = searchNode.getNextSibling();
            }
            
            // If no method name, this doesn't seem to be valid grammar
            if (methodName == null)
                throw new RecognitionException();
            
            // match the call to a method:
            if (targetType instanceof GenTypeClass) {
                GenTypeClass targetClass = (GenTypeClass) targetType;
                try {
                    Class c = classLoader.loadClass(targetClass.rawName());
                    Method [] methods = c.getMethods();
                        
                    ArrayList suitableMethods = new ArrayList();
                    
                    // Find methods that are applicable, and
                    // accessible. See JLS 15.12.2.1.
                    // The JLS doesn't cover some 1.5 features though,
                    // such as varargs, generic methods/parameters, etc,
                    // so we take a best-guess approach.
                        
                    for (int i = 0; i < methods.length; i++) {
                        // ignore bridge methods
                        if (jutils.isBridge(methods[i]))
                            continue;
                        
                        // check method name
                        if (methods[i].getName().equals(methodName)) {
                            // map target type to declaring type
                            Class declClass = methods[i].getDeclaringClass();
                            //Map m = targetClass.mapToSuper(declClass.getName());
                            
                            // check that the method is applicable (and under
                            // what constraints)
                            
                            // TODO should pass a map constructed from typeArgs
                            // as the second parameter here, when support for
                            // generic methods is added.
                            MethodCallDesc mcd = isMethodApplicable(targetClass, new HashMap(), methods[i], argumentTypes);
                            
                            // Iterate through the current candidates, and:
                            // - replace one or more of them with this one
                            //   (this one is more precise)
                            //   OR
                            // - add this one (no more or less precise than
                            //   any other candidates)
                            //   OR
                            // - discard this one (less precise than another)

                            if (mcd != null) {
                                boolean replaced = false;
                                for (int j = 0; j < suitableMethods.size(); j++) {
                                    //suitableMethods.add(methods[i]);
                                    MethodCallDesc mc = (MethodCallDesc) suitableMethods.get(j);
                                    int compare = mcd.compareSpecificity(mc);
                                    if (compare == 1) {
                                        // this method is more specific
                                        suitableMethods.remove(j);
                                        j--;
                                    }
                                    else if (compare == -1) {
                                        // other method is more specific
                                        replaced = true;
                                        break;
                                    }
                                }
                                
                                if (! replaced)
                                    suitableMethods.add(mcd);
                            }
                        }
                    }

                    if (suitableMethods.size() == 1) {
                        MethodCallDesc mcd = (MethodCallDesc) suitableMethods.get(0);
                        return mcd.retType;
                    }
                    else
                        throw new SemanticException();
                }
                catch (ClassNotFoundException cnfe) {
                    return null; // shouldn't happen
                }
            }
            
            // method call on a non-class type
            throw new SemanticException();
        }
        
        // don't try and handle unqualified method calls
        return null;
    }
    
    /**
     * Get the type from a TYPE node (normally found in a typecast). The node
     * need not actually be a TYPE node, but must conform to the same
     * structure.
     * 
     * @param node the node containing the type
     * @return
     * @throws RecognitionException
     * @throws SemanticException
     */
    GenType getTypeFromTypeNode(AST node) throws RecognitionException, SemanticException
    {
        AST firstChild = node.getFirstChild();
        GenType baseType;
        
        switch (firstChild.getType()) {
            case JavaTokenTypes.LITERAL_char:
                baseType = GenTypePrimitive.getChar();
                break;
            case JavaTokenTypes.LITERAL_byte:
                baseType = GenTypePrimitive.getByte();
                break;
            case JavaTokenTypes.LITERAL_boolean:
                baseType = GenTypePrimitive.getBoolean();
                break;
            case JavaTokenTypes.LITERAL_short:
                baseType = GenTypePrimitive.getShort();
                break;
            case JavaTokenTypes.LITERAL_int:
                baseType = GenTypePrimitive.getInt();
                break;
            case JavaTokenTypes.LITERAL_long:
                baseType = GenTypePrimitive.getLong();
                break;
            case JavaTokenTypes.LITERAL_float:
                baseType = GenTypePrimitive.getFloat();
                break;
            case JavaTokenTypes.LITERAL_double:
                baseType = GenTypePrimitive.getDouble();
                break;
            default:
                // not a primitive type
                baseType = getType(firstChild);
        }
        
        // check for array declarators
        AST arrayNode = firstChild.getNextSibling();
        while (arrayNode != null && arrayNode.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
            baseType = new GenArray(baseType);
            arrayNode = arrayNode.getFirstChild();
        }
        
        return baseType;
    }
    
    /**
     * Unbox a type, if it is a class type which represents a primitive type
     * in object form (eg. java.lang.Integer).<p>
     * 
     * Other class types are returned unchanged.<p>
     * 
     * To determine whether unboxing occurred, compare the result with the
     * object which was passed in. (The same object will be returned if no
     * unboxing took place).
     * 
     * @param b  The type to unbox
     * @return  The unboxed type
     */
    private GenType unBox(GenType b)
    {
        if (b instanceof GenTypeClass) {
            GenTypeClass c = (GenTypeClass) b;
            String cName = c.rawName();
            if (c.equals("java.lang.Integer"))
                return GenTypePrimitive.getInt();
            else if (c.equals("java.lang.Long"))
                return GenTypePrimitive.getLong();
            else if (c.equals("java.lang.Short"))
                return GenTypePrimitive.getShort();
            else if (c.equals("java.lang.Byte"))
                return GenTypePrimitive.getByte();
            else if (c.equals("java.lang.Character"))
                return GenTypePrimitive.getChar();
            else if (c.equals("java.lang.Float"))
                return GenTypePrimitive.getFloat();
            else if (c.equals("java.lang.Double"))
                return GenTypePrimitive.getDouble();
            else if (c.equals("java.lang.Boolean"))
                return GenTypePrimitive.getBoolean();
            else
                return b;
        }
        else
            return b;
    }
    
    /**
     * Box a type, if it is a primitive type such as "int".<p>
     * 
     * Other types are returned unchanged.<p>
     * 
     * To determine whether boxing occurred, compare the result with the
     * object which was passed in. (The same object will be returned if no
     * boxing took place).
     * 
     * @param b  The type to box
     * @return  The boxed type
     */
    private GenType boxType(GenType u)
    {
        if (u instanceof GenTypePrimitive) {
            if (u.typeIs(GenType.GT_INT))
                return new GenTypeClass(new JavaReflective(Integer.class));
            else if (u.typeIs(GenType.GT_LONG))
                return new GenTypeClass(new JavaReflective(Long.class));
            else if (u.typeIs(GenType.GT_SHORT))
                return new GenTypeClass(new JavaReflective(Short.class));
            else if (u.typeIs(GenType.GT_BYTE))
                return new GenTypeClass(new JavaReflective(Byte.class));
            else if (u.typeIs(GenType.GT_CHAR))
                return new GenTypeClass(new JavaReflective(Character.class));
            else if (u.typeIs(GenType.GT_FLOAT))
                return new GenTypeClass(new JavaReflective(Float.class));
            else if (u.typeIs(GenType.GT_DOUBLE))
                return new GenTypeClass(new JavaReflective(Double.class));
            else if (u.typeIs(GenType.GT_BOOLEAN))
                return new GenTypeClass(new JavaReflective(Boolean.class));
            else
                return u;
        }
        else
            return u;
    }
    
    static public boolean isBoxedBoolean(GenType t)
    {
        GenTypeClass ct = t.asClass();
        if (ct != null) {
            return ct.rawName().equals("java.lang.Boolean");
        }
        else
            return false;
    }
    
    /**
     * Conditionally box a type. The type is only boxed if the boolean flag
     * passed in the second parameter is true.<p>
     * 
     * This is a helper method to improve readability.<p>
     * 
     * @see TextParser#boxType(GenType)
     * 
     * @param u    The type to box
     * @param box  The flag indicating whether boxing should occur
     * @return
     */
    private GenType maybeBox(GenType u, boolean box)
    {
        if (box)
            return boxType(u);
        else
            return u;
    }
    
    /**
     * Get the (static) result type of some expression.
     * 
     * @param node  The node representing the expression
     * @return The result type of the expression
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    ExprValue getExpressionType(AST node) throws RecognitionException, SemanticException
    {        
        // Expressions are represented as an EXPR node but sometimes part of
        // an expression can consist of a complete expression which is not
        // itself headed by an EXPR node. This method can handle both cases.
        AST fcNode;
        if (node.getType() == JavaTokenTypes.EXPR)
            fcNode = node.getFirstChild();
        else
            fcNode = node;
        
        switch (fcNode.getType()) {
            // "new xxxxx<>()"
            case JavaTokenTypes.LITERAL_new:
                return new ExprValue(getTypeFromTypeNode(fcNode));
            
            // dot node
            case JavaTokenTypes.DOT:
            {
                AST firstDotArg = fcNode.getFirstChild();
                AST secondDotArg = firstDotArg.getNextSibling();
                
                // qualified new:  expression.new xxxxx<>()
                if (secondDotArg.getType() == JavaTokenTypes.LITERAL_new) {
                    // The class being instantiated needs to be resolved in terms
                    // of the type of the expression to the left.
                    GenType fpType = getExpressionType(firstDotArg).getType();
                    
                    // now evaluate the qualified new in the context of the type
                    // of the expression
                    if (fpType.asClass() != null) {
                        GenType type = getInnerType(secondDotArg.getFirstChild(), fpType.asClass());
                        return new ExprValue(type);
                    }
                    
                    if (fpType == null)
                        return null;
                    else
                        throw new SemanticException();
                }
                
                // member field/type of some type
                if (secondDotArg.getType() == JavaTokenTypes.IDENT) {
                    
                    Entity entity = getEntity(firstDotArg);
                    entity = entity.getSubentity(secondDotArg.getText());
                    
                    return new ExprValue(entity.getType());
                }
                
                // class literal
                if (secondDotArg.getType() == JavaTokenTypes.LITERAL_class) {
                    if (! Config.isJava15())
                        return new ExprValue(new GenTypeClass(new JavaReflective(Class.class)));
                    // TODO return "Class<X>", not just "Class"
                    return new ExprValue(new GenTypeClass(new JavaReflective(Class.class)));
                }
                
                // not worrying about - .this, .super etc. They can't be used in
                // the code pad context.
            }
            
            // method call
            case JavaTokenTypes.METHOD_CALL:
                return new ExprValue(getMethodCallReturnType(fcNode));
            
            // Object bench object
            case JavaTokenTypes.IDENT:
            {
                String objectName = fcNode.getText();
                ObjectWrapper ow = objectBench.getObject(objectName);
                
                if (ow == null)
                    throw new SemanticException();
                
                return new ExprValue(ow.getGenType());
            }
            
            // type cast.
            case JavaTokenTypes.TYPECAST:
                return new ExprValue(getTypeFromTypeNode(fcNode.getFirstChild()));
            
            // array element access
            case JavaTokenTypes.INDEX_OP:
            {
                GenType t = getExpressionType(fcNode.getFirstChild()).getType();
                GenType componentType = t.getArrayComponent();
                if (componentType != null)
                    return new ExprValue(componentType);
                else
                    throw new SemanticException();
            }
            
            // various literal types.
            
            case JavaTokenTypes.STRING_LITERAL:
                return new ExprValue(new GenTypeClass(new JavaReflective(String.class)));
            
            case JavaTokenTypes.CHAR_LITERAL:
                return getCharLiteral(fcNode);
            
            case JavaTokenTypes.NUM_INT:
                return getIntLiteral(fcNode, false);
            
            case JavaTokenTypes.NUM_LONG:
                return getLongLiteral(fcNode, false);
            
            case JavaTokenTypes.NUM_FLOAT:
                return getFloatLiteral(fcNode, false);
            
            case JavaTokenTypes.NUM_DOUBLE:
                return getDoubleLiteral(fcNode, false);
            
            // TODO these should return an ExprValue with assosciated values
            case JavaTokenTypes.LITERAL_true:
            case JavaTokenTypes.LITERAL_false:
            case JavaTokenTypes.LITERAL_instanceof:
                return new ExprValue(GenTypePrimitive.getBoolean());
            
            case JavaTokenTypes.LITERAL_null:
                return new ExprValue(GenTypePrimitive.getNull());
            
            // unary operators
            case JavaTokenTypes.UNARY_PLUS:
                return getExpressionType(fcNode.getFirstChild());
            case JavaTokenTypes.UNARY_MINUS:
            {
                AST negExpr = fcNode.getFirstChild();
                
                switch (negExpr.getType()) {
                    case JavaTokenTypes.NUM_INT:
                        return getIntLiteral(negExpr, true);
                    case JavaTokenTypes.NUM_LONG:
                        return getLongLiteral(negExpr, true);
                    case JavaTokenTypes.NUM_FLOAT:
                        return getFloatLiteral(negExpr, true);
                    case JavaTokenTypes.NUM_DOUBLE:
                        return getFloatLiteral(negExpr, true);
                    default:
                        return getExpressionType(negExpr);
                }
            }
                
            // boolean operators
            // TODO operations on constant values have a constant result
            
            case JavaTokenTypes.LT:
            case JavaTokenTypes.LE:
            case JavaTokenTypes.GT:
            case JavaTokenTypes.GE:
            case JavaTokenTypes.EQUAL:
            case JavaTokenTypes.NOT_EQUAL:
            case JavaTokenTypes.LNOT:
            case JavaTokenTypes.LAND:
            case JavaTokenTypes.LOR:
                return new ExprValue(GenTypePrimitive.getBoolean());
            
            // shift operators. The result type is the unary-promoted
            // first operand type.
            // TODO handle operations on numeric constants (result is constant)
            case JavaTokenTypes.SL:
            case JavaTokenTypes.SR:
            case JavaTokenTypes.BSR:
            {
                GenType rtype = unBox(getExpressionType(fcNode.getFirstChild()).getType());
                if (isMinorInteger(rtype))
                    rtype = GenTypePrimitive.getInt();
                
                return new ExprValue(rtype);
            }
            
            // assignment operators. The result type is the same as the first
            // operand type.
            case JavaTokenTypes.SL_ASSIGN:
            case JavaTokenTypes.SR_ASSIGN:
            case JavaTokenTypes.BSR_ASSIGN:
            case JavaTokenTypes.PLUS_ASSIGN:
            case JavaTokenTypes.MINUS_ASSIGN:
            case JavaTokenTypes.STAR_ASSIGN:
            case JavaTokenTypes.DIV_ASSIGN:
            case JavaTokenTypes.MOD_ASSIGN:
            case JavaTokenTypes.BAND_ASSIGN:
            case JavaTokenTypes.BOR_ASSIGN:
            case JavaTokenTypes.BXOR_ASSIGN:
                return getExpressionType(fcNode.getFirstChild());

            // arithmetic operations, other binary ops
            // TODO operations on numeric constants result in a constant
            case JavaTokenTypes.MINUS:
            case JavaTokenTypes.STAR:
            case JavaTokenTypes.DIV:
            case JavaTokenTypes.MOD:
            case JavaTokenTypes.BAND:
            case JavaTokenTypes.BOR:
            case JavaTokenTypes.BXOR:
            {
                AST leftNode = fcNode.getFirstChild();
                AST rightNode = leftNode.getNextSibling();
                GenType leftType = unBox(getExpressionType(leftNode).getType());
                GenType rightType = unBox(getExpressionType(rightNode).getType());
                
                if (leftType.typeIs(GenType.GT_BOOLEAN) && rightType.typeIs(GenType.GT_BOOLEAN))
                {
                    // TODO handle constants
                    int ntype = fcNode.getType();
                    if (ntype == JavaTokenTypes.BAND || ntype == JavaTokenTypes.BOR || ntype == JavaTokenTypes.BXOR)
                        return new ExprValue(GenTypePrimitive.getBoolean());
                }
                
                return new ExprValue(binaryNumericPromotion(leftType, rightType));
            }

            // plus needs special handling, as it is also the string
            // concatenation operator
            // TODO handle operations on numeric constants
            case JavaTokenTypes.PLUS:
            {
                AST leftNode = fcNode.getFirstChild();
                AST rightNode = leftNode.getNextSibling();
                GenType leftType = unBox(getExpressionType(leftNode).getType());
                GenType rightType = unBox(getExpressionType(rightNode).getType());
                if (leftType == null || rightType == null)
                    return null;
                
                if (leftType.toString().equals("java.lang.String"))
                    return new ExprValue(leftType);
                if (rightType.toString().equals("java.lang.String"))
                    return new ExprValue(rightType);
                
                return new ExprValue(binaryNumericPromotion(leftType, rightType));
            }
            
            // increment/decrement, binary not - unary operators
            case JavaTokenTypes.POST_INC:
            case JavaTokenTypes.POST_DEC:
            case JavaTokenTypes.INC:
            case JavaTokenTypes.DEC:
                return new ExprValue(unBox(getExpressionType(fcNode.getFirstChild()).getType()));

            case JavaTokenTypes.BNOT:
            {
                ExprValue oval = getExpressionType(fcNode.getFirstChild());
                GenType ntype = unBox(oval.getType());
                if (! ntype.isIntegralType())
                    throw new SemanticException();
                
                if (! oval.knownValue()) {
                    // unary numeric promotion means the result is long/int
                    if (ntype.typeIs(GenType.GT_LONG))
                        return new ExprValue(ntype);
                    else
                        return new ExprValue(GenTypePrimitive.getInt());
                }
                
                // handle case where operand (and result) type is "long"
                if (ntype.typeIs(GenType.GT_LONG)) {
                    long newval = ~ oval.longValue();
                    return new NumValue(ntype, new Long(newval));
                }
                
                // unary numeric promotion means the result must be "int".
                int newval = ~ oval.intValue();
                return new NumValue(GenTypePrimitive.getInt(), new Integer(newval));
            }
            
            // ? : operator  (trinary operator)
            case JavaTokenTypes.QUESTION:
            {
                // TODO handle constant expressions
                if (Config.usingJava15())
                    return new ExprValue(questionOperator15(fcNode));
                else
                    return new ExprValue(questionOperator14(fcNode));
            }
            
            default:
        }
        
        // bluej.utility.Debug.message("Unknown type: " + fcNode.getType());
        return null;
    }
    
    /**
     * Obtain a parser which can be used to parse a command string.
     * @param s  the string to parse
     */
    static JavaRecognizer getParser(String s)
    {
        StringReader r = new StringReader(s);
        
        // create a scanner that reads from the input stream passed to us
        JavaLexer lexer = new JavaLexer(r);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");

        // with a tab size of one, the rows and column numbers that
        // locatable token returns are model coordinates in the editor
        // (not view coordinates)
        // ie a keyword may appear to start at column 14 because of tabs
        // but in the actual document model its really at column 4
        // so we set our tabsize to 1 so that it maps directly to the
        // document model
        lexer.setTabSize(1);

        // create a filter to handle our comments
        TokenStreamHiddenTokenFilter filter;
        filter = new TokenStreamHiddenTokenFilter(lexer);
        filter.hide(JavaRecognizer.SL_COMMENT);
        filter.hide(JavaRecognizer.ML_COMMENT);

        // create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");
        
        return parser;
    }

    /**
     * A simple structure to hold various information about a method call.
     * 
     * @author Davin McCall
     */
    private class MethodCallDesc
    {
        public Method method;
        public List argTypes; // list of GenType
        public boolean vararg;   // is a vararg call
        public boolean autoboxing; // requires autoboxing
        public GenType retType; // effective return type
        
        /**
         * Constructor for MethodCallDesc.
         * 
         * @param m   The method being called
         * @param argTypes   The effective types of the arguments, as an
         *                   ordered list
         * @param vararg      Whether the method is being called as a vararg
         *                    method
         * @param autoboxing  Whether autoboxing is required for parameters
         * @param retType     The effective return type
         */
        public MethodCallDesc(Method m, List argTypes, boolean vararg, boolean autoboxing, GenType retType)
        {
            this.method = m;
            this.argTypes = argTypes;
            this.vararg = vararg;
            this.autoboxing = autoboxing;
            this.retType = retType;
        }
        
        /**
         * Find out which (if any) method call is more specific than the other.
         * Both calls must be valid calls to the same method with the same
         * number of parameters.
         * 
         * @param other  The method to compare with
         * @return 1 if this method is more specific;
         *         -1 if the other method is more specific;
         *         0 if neither method is more specific than the other.
         * 
         * See JLS 15.12.2.2
         */
        public int compareSpecificity(MethodCallDesc other)
        {
            if (other.vararg && ! vararg)
                return 1; // we are more specific
            if (! other.vararg && vararg)
                return -1; // we are less specific
            
            Iterator i = argTypes.iterator();
            Iterator j = other.argTypes.iterator();
            int upCount = 0;
            int downCount = 0;
            
            while (i.hasNext()) {
                GenType myArg = (GenType) i.next();
                GenType otherArg = (GenType) j.next();
                
                if (myArg.isAssignableFrom(otherArg)) {
                    if (! otherArg.isAssignableFrom(myArg))
                        upCount++;
                }
                else if (otherArg.isAssignableFrom(myArg))
                    downCount++;
            }
            
            if (upCount > 0 && downCount == 0)
                return -1; // other is more specific
            else if (downCount > 0 && upCount == 0)
                return 1;  // other is less specific
            
            // finally, if one method is declared in an interface and the
            // other in a class, the one from the class is "more specific".
            // TODO this should only be true if the method signatures are
            // actually the same. Is it even necessary?
            if (method.getDeclaringClass().isInterface() && ! other.method.getDeclaringClass().isInterface())
                return -1;
            else if (! method.getDeclaringClass().isInterface() && other.method.getDeclaringClass().isInterface())
                return 1;
            else
                return 0;
        }
    }
    
    /**
     * An exception class used to indicate that a semantic occur was detected
     * in the code being parsed.
     * 
     * @author Davin McCall
     */
    class SemanticException extends Exception
    {
        // Nothing to see here.
    }
    
    /**
     * An entity which occurs in the source in a place where context alone
     * cannot determine whether the entity is a package, class or value.
     * 
     * @author Davin McCall
     */
    abstract class Entity
    {
        //static final int ENTITY_PACKAGE = 0;
        //static final int ENTITY_CLASS = 1;
        //static final int ENTITY_VALUE = 2;
        
        // abstract int entityType();
        
        // getType throws SemanticException if the entity doesn't have an
        // assosciated type (for instance a package)
        abstract GenType getType() throws SemanticException;
        
        abstract Entity getSubentity(String name) throws SemanticException;
    }
    
    abstract class PackageOrClass extends Entity
    {
        // same as getSubentity, but cannot yield a value
        abstract PackageOrClass getPackageOrClassMember(String name) throws SemanticException;
        
        abstract void setTypeParams(List tparams) throws SemanticException;
    }
    
    class PackageEntity extends PackageOrClass
    {
        String packageName;
        
        PackageEntity(String pname)
        {
            packageName = pname;
        }
        
        GenType getType() throws SemanticException
        {
            throw new SemanticException();
        }
        
        void setTypeParams(List tparams) throws SemanticException
        {
            // a package cannot be parameterized!
            throw new SemanticException();
        }
        
        Entity getSubentity(String name) throws SemanticException
        {
            Class c;
            try {
                c = classLoader.loadClass(packageName + '.' + name);
                return new TypeEntity(c);
            }
            catch (ClassNotFoundException cnfe) {
                return new PackageEntity(packageName + '.' + name);
            }
        }
        
        PackageOrClass getPackageOrClassMember(String name) throws SemanticException
        {
            return (PackageOrClass) getSubentity(name);
        }
        
        public String toString()
        {
            return "package: " + packageName;
        }
    }
    
    class TypeEntity extends PackageOrClass
    {
        Class thisClass;
        List tparams;
        GenTypeClass outer;
        
        TypeEntity(Class c)
        {
            thisClass = c;
            tparams = Collections.EMPTY_LIST;
        }
        
        TypeEntity(Class c, GenTypeClass outer)
        {
            thisClass = c;
            this.outer = outer;
            tparams = Collections.EMPTY_LIST;
        }

        void setTypeParams(List tparams) throws SemanticException
        {
            this.tparams = tparams;
        }

        GenType getType()
        {
            return getClassType();
        }
        
        GenTypeClass getClassType()
        {
            return new GenTypeClass(new JavaReflective(thisClass), tparams, outer);
        }
        
        Entity getSubentity(String name) throws SemanticException
        {
            // subentity of a class could be a member type or field
            // Is it a field?
            Field f = null;
            try {
                f = thisClass.getField(name);
                GenType fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                Map tparmap = getClassType().getMap();
                if (tparmap != null)
                    fieldType.mapTparsToTypes(tparmap);
                return new ValueEntity(fieldType);
            }
            catch (NoSuchFieldException nsfe) {}

            // Is it a member type?
            return getPackageOrClassMember(name);
        }
        
        PackageOrClass getPackageOrClassMember(String name) throws SemanticException
        {
            // Is it a member type?
            Class c;
            try {
                c = classLoader.loadClass(thisClass.getName() + '$' + name);
                return new TypeEntity(c, (GenTypeClass) getType());
            }
            catch (ClassNotFoundException cnfe) {
                // No more options - it must be an error
                throw new SemanticException();
            }
        }
    }
    
    class ValueEntity extends Entity
    {
        GenType type;
        
        ValueEntity(GenType type)
        {
            this.type = type;
        }
        
        GenType getType()
        {
            return type;
        }
        
        Entity getSubentity(String name)
            throws SemanticException
        {
            // Should be a member field.
            if (!(type instanceof GenTypeClass))
                throw new SemanticException();

            // get the class part of our type
            GenTypeClass thisClass = (GenTypeClass) type;
            Reflective r = thisClass.getReflective();
            Class c;
            try {
                c = classLoader.loadClass(r.getName());
            }
            catch (ClassNotFoundException cnfe) {
                // shouldn't happen
                throw new SemanticException();
            }

            //  Try and find the field
            Field f = null;
            try {
                f = c.getField(name);
                // Map type parameters to declaring class
                Class declarer = f.getDeclaringClass();
                Map tparMap = thisClass.mapToSuper2(declarer.getName()).getMap();

                GenType fieldType;
                if (tparMap != null) {
                    // Not raw. Apply type parameters to field declaration.
                    fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                    fieldType = fieldType.mapTparsToTypes(tparMap);
                }
                else
                    fieldType = JavaUtils.getJavaUtils().getRawFieldType(f);

                return new ValueEntity(fieldType);
            }
            catch (NoSuchFieldException nsfe) {
                throw new SemanticException();
            }
        }
    }
    
    /**
     * A value (possibly unknown) with assosciated type
     */
    class ExprValue
    {
        // default implementation has no known value
        public GenType type;
        
        public ExprValue(GenType type)
        {
            this.type = type;
        }
        
        public GenType getType()
        {
            return type;
        }
        
        public boolean knownValue()
        {
            return false;
        }
        
        public int intValue()
        {
            throw new UnsupportedOperationException();
        }
        
        public long longValue()
        {
            throw new UnsupportedOperationException();
        }
        
        public float floatValue()
        {
            throw new UnsupportedOperationException();
        }
        
        public double doubleValue()
        {
            throw new UnsupportedOperationException();
        }
        
        public boolean booleanValue()
        {
            throw new UnsupportedOperationException();
        }
    }
    
    class NumValue extends ExprValue
    {
        private Number val;
        
        NumValue(GenType type, Number val)
        {
            super(type);
            this.val = val;
        }
        
        public boolean knownValue()
        {
            return true;
        }
        
        public int intValue()
        {
            return val.intValue();
        }
        
        public long longValue()
        {
            return val.longValue();
        }
        
        public float floatValue()
        {
            return val.floatValue();
        }
        
        public double doubleValue()
        {
            return val.doubleValue();
        }
    }
}
