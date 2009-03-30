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
package bluej.parser;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import antlr.ParserSharedInputState;
import antlr.RecognitionException;
import antlr.TokenBuffer;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.AST;
import bluej.Config;
import bluej.debugger.gentype.*;
import bluej.debugmgr.NamedValue;
import bluej.debugmgr.ValueCollection;
import bluej.debugmgr.texteval.WildcardCapture;
import bluej.parser.ast.LocatableAST;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.utility.Debug;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Parsing routines for the code pad.<p>
 * 
 * This is pretty tricky stuff, we try to following the Java Language Specification
 * (JLS) where possible.
 *  
 * @author Davin McCall
 * @version $Id$
 */
public class TextParser
{
    private ClassLoader classLoader;
    private String packageScope;  // evaluation package
    private ValueCollection objectBench;

    private static JavaUtils jutils = JavaUtils.getJavaUtils();
    private static boolean java15 = Config.isJava15();
    
    private List declVars; // variables declared in the parsed statement block
    private String amendedCommand;  // command string amended with initializations for
                                    // all variables
    
    private ImportsCollection imports;
    private String importCandidate; // any import candidates.
    private JavaRecognizer parser;
    
    /**
     * TextParser constructor. Defines the class loader and package scope
     * for evaluation.
     */
    public TextParser(ClassLoader classLoader, String packageScope, ValueCollection ob)
    {
        this.classLoader = classLoader;
        this.packageScope = packageScope;
        this.objectBench = ob;
        imports = new ImportsCollection();
    }
    
    /**
     * Set a new class loader, and clear the imports list.
     */
    public void newClassLoader(ClassLoader newLoader)
    {
        classLoader = newLoader;
        imports.clear();
    }
    
    /**
     * Parse a string entered into the code pad. Return is null if the string
     * is a statement; otherwise the string is an expression and the returned
     * string if the type of the expression (empty if the type cannot be determined).
     * 
     * <p>After calling this method, getDeclaredVars() and getAmendedCommand() can be
     * called - see the documentation for those methods respectively.
     * 
     * <p>If the parsed string is then executed, the confirmCommand() method should
     * subsequently be called.
     */
    public String parseCommand(String command)
    {
        importCandidate = "";
        amendedCommand = command;
        boolean parsedOk = false;
        declVars = Collections.EMPTY_LIST;
        AST rootAST;

        if (parser == null)
            parser = getParser();
        
        // check if it's an import statement
        try {
            parser.setTokenBuffer(new TokenBuffer(getTokenStream(command)));
            parser.getInputState().reset();
            
            parser.importDefinition();
            amendedCommand = "";
            importCandidate = command;
            return null;
        }
        catch (RecognitionException re) { }
        catch (TokenStreamException tse) { }
        
        // start parsing at the compoundStatement rule
        try {
            parser.setTokenBuffer(new TokenBuffer(getTokenStream("{" + command + "};;;;;")));
            parser.getInputState().reset();
            parser.compoundStatement();
            rootAST = parser.getAST();
            parsedOk = true;

            // Extract the declared variables
            AST fcnode = rootAST.getFirstChild();
            checkVars((LocatableAST) fcnode, command);
        }
        catch(RecognitionException re) { }
        catch(TokenStreamException tse) { }
      
        if (! parsedOk) {
            // It might just be an expression. Multiple semi-colon to ensure
            // end-of-file not hit (causes parse failure).
            parser.setTokenBuffer(new TokenBuffer(getTokenStream(command + ";;;;;")));
            parser.getInputState().reset();
            
            try {
                parser.expression();
                rootAST = parser.getAST();
                parsedOk = true; // it parses as an expression.
                
                ExprValue ev = getExpressionType(rootAST);
                JavaType t = ev != null ? ev.getType() : null;

                if (t == null) {
                    return "";
                }
                else if (t.isVoid()) {
                    return null;
                }
                else {
                    // If the result type is a type parameter (a capture),
                    // or an intersection type, extract the bound
                    if (t instanceof GenTypeSolid) {
                        GenTypeSolid st = (GenTypeSolid) t;
                        GenTypeClass [] bounds = st.getReferenceSupertypes();
                        t = bounds[0];
                    }
                    // remove capture/type variables from the type
                    t = t.mapTparsToTypes(null);
                    return t.toString();
                }
            }
            catch(RecognitionException re) { }
            catch(SemanticException se) { }
            catch(TokenStreamException tse) { }
            catch(Exception e) {
                Debug.reportError("TextParser: Unexpected error during parsing:");
                e.printStackTrace(System.out);
            }
            return "";
        }
        return null;
    }
    
    /**
     * Called to confirm that the recently parsed command has successfully
     * executed. This allows TextParser to update internal state to reflect
     * changes caused by the execution of the command.
     */
    public void confirmCommand()
    {
        if (importCandidate.length() != 0) {
            try {
                addImportToCollection(parser.getAST());
            }
            catch (RecognitionException re) { }
            catch (SemanticException se) { }
        }
    }

    /**
     * Add an import (be it regular or wildcard, normal or static) to the collection
     * of imports.
     * 
     * @param importNode  The AST node representing the import statement
     * 
     * @throws SemanticException
     * @throws RecognitionException
     */
    void addImportToCollection(AST importNode)
        throws SemanticException, RecognitionException
    {
        if (importNode.getType() == JavaTokenTypes.IMPORT) {
            // Non-static import
            AST classNode = importNode.getFirstChild();
            AST fpNode = classNode.getFirstChild();
            AST className = fpNode.getNextSibling();
            
            // if className == '*' this is a wildcard
            if (className.getType() == JavaTokenTypes.STAR) {
                PackageOrClass importEntity = getPackageOrType(fpNode, true);
                imports.addWildcardImport(importEntity);
            }
            else {
                // A non-wildcard import.
                PackageOrClass importEntity = getPackageOrType(classNode, true);
                if (importEntity.isClass()) {
                    imports.addNormalImport(className.getText(), importEntity);
                }
            }
        }
        else if (importNode.getType() == JavaTokenTypes.STATIC_IMPORT) {
            // static import
            AST impNode = importNode.getFirstChild().getFirstChild();
            AST impNameNode = impNode.getNextSibling();
            if (impNameNode.getType() == JavaTokenTypes.STAR) {
                ClassEntity importEntity = (ClassEntity) getPackageOrType(impNode, true);
                imports.addStaticWildcardImport(importEntity);
            }
            else {
                String impName = impNameNode.getText();
                ClassEntity importEntity = (ClassEntity) getPackageOrType(impNode, true);
                imports.addStaticImport(impName, importEntity);
            }
        }
    }
    
    /**
     * Check the command string for variable declarations/definitions.
     * @param fcnode  The AST root.
     * 
     * @throws RecognitionException
     */
    private void checkVars(LocatableAST fcnode, String command)
        throws RecognitionException
    {
        try {
            declVars = new ArrayList();
            ArrayList insPoint = new ArrayList(); // list of initializer insertion points
            ArrayList insText = new ArrayList();  // list of initializer insertion texts
            
            while (fcnode != null) {

                // store the end position of the AST
                int ecol = fcnode.getEndColumn() - 2; // column numbers start at 1
                    // additionally, the end column is the column just beyond the
                    // semicolon or comma

                // is a variable declared?
                if (fcnode.getType() == JavaTokenTypes.VARIABLE_DEF) {
                    boolean isFinal = false;

                    // check modifiers for "final"
                    AST modnode = fcnode.getFirstChild(); // modifiers
                    AST firstMod = modnode.getFirstChild();
                    if (firstMod != null && firstMod.getType() == JavaTokenTypes.FINAL)
                        isFinal = true;
                    
                    // get type and name
                    AST typenode = modnode.getNextSibling();
                    JavaType declVarType = getTypeFromTypeNode(typenode);
                    AST namenode = typenode.getNextSibling();
                    String varName = namenode.getText();
                    boolean isVarInit = namenode.getNextSibling() != null;
                    declVars.add(new DeclaredVar(isVarInit, isFinal, declVarType, varName));
                    
                    if (! isVarInit) {
                        insPoint.add(new Integer(ecol - 1));
                        String text;
                        if (declVarType.isPrimitive()) {
                            if (declVarType.isNumeric()) {
                                text = "= 0";
                            }
                            else {
                                text = "= false";
                            }
                        }
                        else {
                            // reference type
                            text = "= null";
                        }
                        insText.add(text);
                    }
                }
                fcnode = (LocatableAST) fcnode.getNextSibling();
            }
            
            // insert the initialization strings
            amendedCommand = command;
            int i = insPoint.size();
            while (i-- > 0) {
                int ipoint = ((Integer) insPoint.get(i)).intValue();
                String itext = (String) insText.get(i);
                amendedCommand = amendedCommand.substring(0, ipoint) + itext + amendedCommand.substring(ipoint);
            }
        }
        catch (SemanticException se) {}
    }
    
    /**
     * Get a list of the variables declared in the recently parsed statement
     * block. The return is a List of TextParser.DeclaredVar
     */
    public List getDeclaredVars()
    {
        return declVars;
    }
    
    /**
     * Get the amended command string, which has initializers inserted for variable
     * declarations which were missing initializers.
     */
    public String getAmendedCommand()
    {
        return amendedCommand;
    }
    
    /**
     * Return the imports collection as a sequence of java import statements.
     */
    public String getImportStatements()
    {
        return imports.toString() + importCandidate;
    }
    
    /**
     * Java 1.4 & prior version of trinary "? :" operator. See JLS 2nd ed. 
     * section 15.25.
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    private JavaType questionOperator14(AST node) throws RecognitionException, SemanticException
    {
        AST trueAlt = node.getFirstChild().getNextSibling();
        AST falseAlt = trueAlt.getNextSibling();
        ExprValue trueAltEv = getExpressionType(trueAlt);
        ExprValue falseAltEv = getExpressionType(falseAlt);
        JavaType trueAltType = trueAltEv.getType();
        JavaType falseAltType = falseAltEv.getType();
        
        // if the operands have the same type, that is the result type
        if (trueAltType.equals(falseAltType))
            return trueAltType;
        
        if (trueAltType.isNumeric() && falseAltType.isNumeric()) {
            // if one type is short and the other is byte, result type is short
            if (trueAltType.typeIs(JavaType.JT_SHORT) && falseAltType.typeIs(JavaType.JT_BYTE))
                return JavaPrimitiveType.getShort();
            if (falseAltType.typeIs(JavaType.JT_SHORT) && trueAltType.typeIs(JavaType.JT_BYTE))
                return JavaPrimitiveType.getShort();
            
            // if one type is byte/short/char and the other is a constant of
            // type int whose value fits, the result type is byte/short/char
            if (falseAltType.typeIs(JavaType.JT_INT) && falseAltEv.knownValue()) {
                int fval = falseAltEv.intValue();
                if (isMinorInteger(trueAltType) && trueAltType.couldHold(fval))
                    return trueAltType;
            }
            if (trueAltType.typeIs(JavaType.JT_INT) && trueAltEv.knownValue()) {
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
    private static boolean isMinorInteger(JavaType a)
    {
        return a.typeIs(JavaType.JT_BYTE) || a.typeIs(JavaType.JT_CHAR) || a.typeIs(JavaType.JT_SHORT); 
    }
    
    /**
     * Java 1.5 version of the trinary "? :" operator.
     * See JLS section 15.25. Note that JLS 3rd ed. differs extensively
     * from JLS 2nd edition. The changes are not backwards compatible.
     * 
     * @throws RecognitionException
     * @throws SemanticException
     */
    private JavaType questionOperator15(AST node) throws RecognitionException, SemanticException
    {
        AST trueAlt = node.getFirstChild().getNextSibling();
        AST falseAlt = trueAlt.getNextSibling();
        ExprValue trueAltEv = getExpressionType(trueAlt);
        ExprValue falseAltEv = getExpressionType(falseAlt);
        JavaType trueAltType = trueAltEv.getType();
        JavaType falseAltType = falseAltEv.getType();
        
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
        
        JavaType trueUnboxed = unBox(trueAltType);
        JavaType falseUnboxed = unBox(falseAltType);
        
        // if one the arguments is of type boolean and the other
        // Boolean, the result type is boolean.
        if (trueUnboxed.typeIs(JavaType.JT_BOOLEAN) && falseUnboxed.typeIs(JavaType.JT_BOOLEAN))
            return trueUnboxed;
        
        // if one type is null and the other is a reference type, the
        // return is that reference type.
        //   Also partially handle the final case from the JLS,
        // involving boxing conversion & capture conversion (which
        // is trivial when non-parameterized types such as boxed types
        // are involved)
        // 
        // This precludes either type from being null later on.
        if (trueAltType.typeIs(JavaType.JT_NULL))
            return boxType(falseAltType);
        if (falseAltType.typeIs(JavaType.JT_NULL))
            return boxType(trueAltType);
        
        // if the two alternatives are convertible to numeric types,
        // there are several cases:
        if (trueUnboxed.isNumeric() && falseUnboxed.isNumeric()) {
            // If one is byte/Byte and the other is short/Short, the
            // result type is short.
            if (trueUnboxed.typeIs(JavaType.JT_BYTE) && falseUnboxed.typeIs(JavaType.JT_SHORT))
                return falseUnboxed;
            if (falseUnboxed.typeIs(JavaType.JT_BYTE) && trueUnboxed.typeIs(JavaType.JT_SHORT))
                return trueUnboxed;
            
            // If one type, when unboxed, is byte/short/char, and the
            // other is an integer constant whose value fits in the
            // first, the result type is the (unboxed) former type. (The JLS
            // takes four paragraphs to say this, but the result is the
            // same).
            if (isMinorInteger(trueUnboxed) && falseAltType.typeIs(JavaType.JT_INT) && falseAltEv.knownValue()) {
                int kval = falseAltEv.intValue();
                if (trueUnboxed.couldHold(kval))
                    return trueUnboxed;
            }
            if (isMinorInteger(falseUnboxed) && trueAltType.typeIs(JavaType.JT_INT) && trueAltEv.knownValue()) {
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
        
        if (trueAltType instanceof GenTypeSolid && falseAltType instanceof GenTypeSolid) {
            // apply capture conversion (JLS 5.1.10) to lub() of both
            // alternatives (JLS 15.12.2.7). I have no idea why capture conversion
            // should be performed here, but I follow the spec blindly.
            GenTypeSolid [] lubArgs = new GenTypeSolid[2];
            lubArgs[0] = (GenTypeSolid) trueAltType;
            lubArgs[1] = (GenTypeSolid) falseAltType;
            return captureConversion(GenTypeSolid.lub(lubArgs));
        }
        
        return null;
    }
    
    /**
     * Capture conversion, as in the JLS 5.1.10
     */
    private JavaType captureConversion(JavaType o)
    {
        GenTypeClass c = o.asClass();
        if (c != null)
            return captureConversion(c, new HashMap());
        else
            return o;
    }
    
    /**
     * Capture conversion, storing converted type parameters in the supplied Map so
     * that they are accessible to inner classes.
     *  
     * @param c   The type to perform the conversion on
     * @param tparMap   The map used for storing type parameter conversions
     * @return   The converted type.
     */
    private GenTypeClass captureConversion(GenTypeClass c, Map tparMap)
    {
        // capture the outer type
        GenTypeClass newOuter = null;
        GenTypeClass oldOuter = c.getOuterType();
        if (oldOuter != null)
            newOuter = captureConversion(oldOuter, tparMap);
        
        // capture the arguments
        List oldArgs = c.getTypeParamList();
        List newArgs = new ArrayList(oldArgs.size());
        Iterator i = oldArgs.iterator();
        Iterator boundsIterator = c.getReflective().getTypeParams().iterator();
        while (i.hasNext()) {
            GenTypeParameterizable targ = (GenTypeParameterizable) i.next();
            GenTypeDeclTpar tpar = (GenTypeDeclTpar) boundsIterator.next();
            GenTypeSolid newArg;
            if (targ instanceof GenTypeWildcard) {
                GenTypeWildcard wc = (GenTypeWildcard) targ;
                GenTypeSolid [] ubounds = wc.getUpperBounds();
                GenTypeSolid lbound = wc.getLowerBound();
                GenTypeSolid [] tpbounds = tpar.upperBounds();
                for (int j = 0; j < tpbounds.length; j++) {
                    tpbounds[j] = (GenTypeSolid) tpbounds[j].mapTparsToTypes(tparMap);
                }
                if (lbound != null) {
                    // ? super XX
                    newArg = new WildcardCapture(tpbounds, lbound);
                }
                else {
                    // ? extends ...
                    GenTypeSolid [] newBounds = new GenTypeSolid[ubounds.length + tpbounds.length];
                    System.arraycopy(ubounds, 0, newBounds, 0, ubounds.length);
                    System.arraycopy(tpbounds, 0, newBounds, ubounds.length, tpbounds.length);
                    newArg = new WildcardCapture(newBounds);
                }
            }
            else {
                // The argument is not a wildcard. Capture doesn't affect it.
                newArg = (GenTypeSolid) targ;
            }
            newArgs.add(newArg);
            tparMap.put(tpar.getTparName(), newArg);
        }
        return new GenTypeClass(c.getReflective(), newArgs, newOuter);
    }
    
    /**
     * binary numeric promotion, as defined by JLS section 5.6.2. Both
     * operands must be (possibly boxed) numeric types.
     */
    private JavaType binaryNumericPromotion(JavaType a, JavaType b)
        throws SemanticException
    {
        JavaType ua = unBox(a);
        JavaType ub = unBox(b);

        if (ua.typeIs(JavaType.JT_DOUBLE) || ub.typeIs(JavaType.JT_DOUBLE))
            return JavaPrimitiveType.getDouble();

        if (ua.typeIs(JavaType.JT_FLOAT) || ub.typeIs(JavaType.JT_FLOAT))
            return JavaPrimitiveType.getFloat();

        if (ua.typeIs(JavaType.JT_LONG) || ub.typeIs(JavaType.JT_LONG))
            return JavaPrimitiveType.getLong();

        if (ua.isNumeric() && ub.isNumeric())
            return JavaPrimitiveType.getInt();
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
        
        final JavaType charType = JavaPrimitiveType.getChar();
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
            return new NumValue(JavaPrimitiveType.getInt(), val);
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
            return new NumValue(JavaPrimitiveType.getLong(), val);
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
            return new NumValue(JavaPrimitiveType.getFloat(), val);
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
            return new NumValue(JavaPrimitiveType.getDouble(), val);
        }
        catch (NumberFormatException nfe) {
            throw new RecognitionException();
        }
    }
    
    /**
     * Attempt to load, by its unqualified name,  a class which might be in the
     * current package or which might be in java.lang.
     * 
     * @param className   the name of the class to try to load
     * @param tryWildcardImports    indicates whether the class name can be resolved by
     *                              checking wildcard imports (including java.lang.*)
     * @throws ClassNotFoundException  if the class cannot be resolved/loaded
     */
    private Class loadUnqualifiedClass(String className, boolean tryWildcardImports)
        throws ClassNotFoundException
    {
        // Try singly imported types first
        ClassEntity imported = imports.getTypeImport(className);
        if (imported != null) {
            try {
                String cname = ((GenTypeClass) imported.getType()).rawName();
                return classLoader.loadClass(cname);
            }
            catch (ClassNotFoundException cnfe) { }
        }
        
        // It's an unqualified name - try package scope
        try {
            if (packageScope.length() != 0)
                return classLoader.loadClass(packageScope + "." + className);
            else
                return classLoader.loadClass(className);
        }
        catch(ClassNotFoundException cnfe) {}
        
        // If not trying wildcard imports, bail out now
        if (! tryWildcardImports)
            throw new ClassNotFoundException(className);
        
        // Try wildcard imports
        imported = imports.getTypeImportWC(className);
        if (imported != null) {
            try {
                String cname = ((GenTypeClass) imported.getType()).rawName();
                return classLoader.loadClass(cname);
            }
            catch (ClassNotFoundException cnfe) { }
        }
        
        // Try java.lang
        return classLoader.loadClass("java.lang." + className);
    }
    
    /**
     * Try to find a suitable wildcard import (including the implicit java.lang.*)
     * which can be used to resolve a class name, and load that class.
     * 
     * @param className   The name of the class to resolve and load
     * @return     The loaded Class
     * @throws ClassNotFoundException  if the class cannot be resolved or loaded.
     */
    private Class loadWildcardImportedType(String className)
        throws ClassNotFoundException
    {
        // Try wildcard imports
        ClassEntity imported = imports.getTypeImportWC(className);
        if (imported != null) {
            try {
                String cname = ((GenTypeClass) imported.getType()).rawName();
                return classLoader.loadClass(cname);
            }
            catch (ClassNotFoundException cnfe) { }
        }
        
        // Try java.lang
        return classLoader.loadClass("java.lang." + className);
    }
    
    /**
     * Extract the type from a node. The type is in the form of a qualified
     * or unqualified class name, with optional type parameters. Note, this
     * specifically doesn't handle array types and primitive types - see
     * getTypeFromTypeNode for that.
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
            List params = getTypeArgs(node.getFirstChild());
            
            try {
                Class c = loadUnqualifiedClass(node.getText(), true);
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
            List params = getTypeArgs(classNode.getFirstChild());

            PackageOrClass nodePorC = porc.getPackageOrClassMember(classNode.getText());
            GenTypeClass nodeClass = (GenTypeClass) nodePorC.getType();
            GenTypeClass outer = nodeClass;

            // Don't pass in an outer class if it's not generic anyway 
            if (! nodeClass.isGeneric())
                outer = null;
            return new GenTypeClass(nodeClass.getReflective(), params, outer);
        }
        else {
            throw new RecognitionException();
        }
    }
    
    /**
     * Get a sequence of type arguments.
     * 
     * @param node  The node representing the first type argument (or null).
     * @return      A list of GenType representing the type arguments
     * @throws RecognitionException
     * @throws SemanticException
     */
    private List getTypeArgs(AST node) throws RecognitionException, SemanticException
    {
        List params = new LinkedList();
        
        // get the type parameters
        while(node != null && node.getType() == JavaTokenTypes.TYPE_ARGUMENT) {
            AST childNode = node.getFirstChild();
            JavaType tparType;
            
            if (childNode.getType() == JavaTokenTypes.WILDCARD_TYPE) {
                // wildcard parameter
                AST boundNode = childNode.getFirstChild();
                if (boundNode != null) {
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
                    tparType = new GenTypeUnbounded();
                }
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
    static String combineDotNames(AST node, char seperator) throws RecognitionException
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
    JavaType getInnerType(AST node, GenTypeClass outer) throws SemanticException, RecognitionException
    {
        if (node.getType() == JavaTokenTypes.IDENT) {
            // A simple name<params> expression
            List params = getTypeArgs(node.getFirstChild());
            
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
            List params = getTypeArgs(classNode.getFirstChild());

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
    JavaEntity getEntity(AST node) throws SemanticException, RecognitionException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            
            // Treat it first as a variable...
            String nodeText = node.getText();
            NamedValue nv = objectBench.getNamedValue(nodeText);
            if (nv != null)
                return new ValueEntity(nv.getGenType());
            
            // It's not a codepad or object bench variable, perhaps it's an import
            List l = imports.getStaticImports(nodeText);
            if (l != null) {
                Iterator i = l.iterator();
                while (i.hasNext()) {
                    ClassEntity importEntity = (ClassEntity) i.next();
                    try {
                        JavaEntity fieldEnt = importEntity.getStaticField(nodeText);
                        return fieldEnt;
                    }
                    catch (SemanticException se) { }
                }
            }
            
            // It might be a type
            try {
                Class c = loadUnqualifiedClass(nodeText, false);
                return new TypeEntity(c);
            }
            catch (ClassNotFoundException cnfe) { }
            
            // Wildcard static imports of fields override wildcard
            // imports of types
            l = imports.getStaticWildcardImports();
            Iterator i = l.iterator();
            while (i.hasNext()) {
                ClassEntity importEntity = (ClassEntity) i.next();
                try {
                    JavaEntity fieldEnt = importEntity.getStaticField(nodeText);
                    return fieldEnt;
                }
                catch (SemanticException se) { }
            }
            
            // Finally try wildcard type imports
            try {
                Class c = loadWildcardImportedType(nodeText);
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
                JavaEntity firstpart = getEntity(firstChild);
                return firstpart.getSubentity(secondChild.getText());
            }
            // Don't worry about xxx.super, it shouldn't be used at this
            // level.
        }
        
        // Anything else must be an expression, therefore a value:
        JavaType exprType = getExpressionType(node).getType();
        return new ValueEntity(exprType);
    }
    
    /**
     * Get an entity which by context must be either a package or a (possibly
     * generic) type.
     */
    private PackageOrClass getPackageOrType(AST node)
        throws SemanticException, RecognitionException
    {
        return getPackageOrType(node, false);
    }
    
    /**
     * Get an entity which by context must be either a package or a (possibly
     * generic) type.
     * @param node  The AST node representing the package/type
     * @param fullyQualified   True if the type must be fully qualified
     *            (if false, imports and the current package are checked for
     *            definitions of a class with the initial name)
     * 
     * @throws SemanticException
     */
    private PackageOrClass getPackageOrType(AST node, boolean fullyQualified)
        throws SemanticException, RecognitionException
    {
        // simple case first:
        if (node.getType() == JavaTokenTypes.IDENT) {
            // Treat it first as a type, then as a package.
            String nodeText = node.getText();
            List tparams = getTypeArgs(node.getFirstChild());
            
            try {
                Class c;
                if (fullyQualified) {
                    c = classLoader.loadClass(nodeText);
                }
                else {
                    c = loadUnqualifiedClass(nodeText, true);
                }
                TypeEntity r = new TypeEntity(c, tparams);
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
                List tparams = getTypeArgs(secondChild.getFirstChild());
                PackageOrClass firstpart = getPackageOrType(firstChild, fullyQualified);

                PackageOrClass entity = firstpart.getPackageOrClassMember(secondChild.getText());
                if (! tparams.isEmpty()) {
                    // There are type parmaters, so we must have a type
                    if (entity.isClass()) {
                        entity = ((ClassEntity) entity).setTypeParams(tparams);
                    }
                    else
                        throw new SemanticException();
                }
                
                return entity;
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
    private JavaType [] getExpressionList(AST node) throws SemanticException, RecognitionException
    {
        int num = node.getNumberOfChildren();
        JavaType [] r = new JavaType[num];
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
     *                  invocation of a generic method (list of GenTypeClass)
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private MethodCallDesc isMethodApplicable(GenTypeClass targetType, List tpars, Method m, JavaType [] args)
        throws RecognitionException
    {
        boolean methodIsVarargs = JavaUtils.getJavaUtils().isVarArgs(m);
        MethodCallDesc rdesc = null;
        
        // First try without varargs expansion. If that fails, try with expansion.
        rdesc = isMethodApplicable(targetType, tpars, m, args, false);
        if (rdesc == null && methodIsVarargs) {
            rdesc = isMethodApplicable(targetType, tpars, m, args, true);
        }
        return rdesc;
    }

    /**
     * Check whether a particular method is callable with particular
     * parameters. If so return information about how specific the call is.
     * If the parameters cannot be applied to this method, return null.<p>
     * 
     * Normally this is called by the other variant of this method, which
     * does not take the varargs parameter.
     * 
     * @param targetType   The type of object/class to which the method is
     *                     being applied
     * @param tpars     The explicitly specified type parameters used in the
     *                  invocation of a generic method (list of GenTypeClass)
     * @param m       The method to check
     * @param args    The types of the arguments supplied to the method
     * @param varargs Whether to expand vararg parameters
     * @return   A record with information about the method call
     * @throws RecognitionException
     */
    private MethodCallDesc isMethodApplicable(GenTypeClass targetType, List tpars, Method m, JavaType [] args, boolean varargs)
    throws RecognitionException
    {
        boolean rawTarget = targetType.isRaw();
        boolean boxingRequired = false;
        
        // Check that the number of parameters supplied is allowable. Expand varargs
        // arguments if necessary.
        JavaType [] mparams = JavaUtils.getJavaUtils().getParamGenTypes(m, rawTarget);
        if (varargs) {
            // first basic check. The number of supplied arguments must be at least one less than
            // the number of formal parameters.
            if (mparams.length > args.length + 1)
                return null;

            GenTypeArray lastArgType = (GenTypeArray) mparams[mparams.length - 1];
            JavaType vaType = lastArgType.getArrayComponent();
            JavaType [] expandedParams = new JavaType[args.length];
            System.arraycopy(mparams, 0, expandedParams, 0, mparams.length - 1);
            for (int i = mparams.length; i < args.length; i++) {
                expandedParams[i] = vaType;
            }
            mparams = expandedParams;
        }
        else {
            // Not varargs: supplied arguments must match formal parameters
            if (mparams.length != args.length)
                return null;
        }
        
        // Get type parameters of the method
        List tparams = Collections.EMPTY_LIST;
        if ((! rawTarget) || Modifier.isStatic(m.getModifiers()))
            tparams = JavaUtils.getJavaUtils().getTypeParams(m);
        
        // Number of type parameters supplied must match number declared, unless either
        // is zero. Section 15.12.2 of the JLS, "a non generic method may be applicable
        // to an invocation which supplies type arguments" (in which case the type args
        // are ignored).
        if (! tpars.isEmpty() && ! tparams.isEmpty() && tpars.size() != tparams.size())
            return null;
        
        // Set up a map we can use to put actual/inferred type arguments. Initialise it
        // with the target type's arguments.
        Map tparMap;
        if (rawTarget)
            tparMap = new HashMap();
        else
            tparMap = targetType.getMap();

        // Perform type inference, if necessary
        if (! tparams.isEmpty() && tpars.isEmpty()) {
            // Our initial map has the class type parameters, minus those which are
            // shadowed by the method's type parameters (map to themselves).
            for (Iterator i = tparams.iterator(); i.hasNext(); ) {
                GenTypeDeclTpar tpar = (GenTypeDeclTpar) i.next();
                tparMap.put(tpar.getTparName(), tpar);
            }
            
            Map tlbConstraints = new HashMap(); // multi-map: map -> set -> GenTypeSolid
            Map teqConstraints = new HashMap();
            
            // Time for some type inference
            for (int i = 0; i < mparams.length; i++) {
                if (mparams[i].isPrimitive())
                    continue;
                
                GenTypeSolid mparam = (GenTypeSolid) mparams[i];
                mparam = (GenTypeSolid) mparam.mapTparsToTypes(tparMap);
                processAtoFConstraint(args[i], mparam, tlbConstraints, teqConstraints);
            }
            
            // what we have now is a map with tpar constraints.
            // Some tpars may not have been constrained: these are inferred to be the
            // intersection of their upper bounds.
            tpars = new ArrayList();
            Iterator i = tparams.iterator();
            while (i.hasNext()) {
                GenTypeDeclTpar fTpar = (GenTypeDeclTpar) i.next();
                String tparName = fTpar.getTparName();
                GenTypeSolid eqConstraint = (GenTypeSolid) teqConstraints.get(tparName);
                // If there's no equality constraint, use the lower bound constraints
                if (eqConstraint == null) {
                    Set lbConstraintSet = (Set) tlbConstraints.get(tparName);
                    if (lbConstraintSet != null) {
                        GenTypeSolid [] lbounds = (GenTypeSolid []) lbConstraintSet.toArray(new GenTypeSolid[lbConstraintSet.size()]);
                        eqConstraint = GenTypeSolid.lub(lbounds); 
                    }
                    else {
                        // no equality or lower bound constraints: use the upper
                        // bounds of the tpar
                        eqConstraint = fTpar.getBound();
                    }
                }
                eqConstraint = (GenTypeSolid) eqConstraint.mapTparsToTypes(tparMap);
                tpars.add(eqConstraint);
                tparMap.put(tparName, eqConstraint);
            }
        }
        else {
            // Get a map of type parameter names to types from the target type
            // complete the type parameter map with tpars of the method
            Iterator formalI = tparams.iterator();
            Iterator actualI = tpars.iterator();
            while (formalI.hasNext()) {
                GenTypeDeclTpar formalTpar = (GenTypeDeclTpar) formalI.next();
                GenTypeSolid argTpar = (GenTypeSolid) actualI.next();
                
                // first we check that the argument type is a subtype of the
                // declared type.
                GenTypeSolid [] formalUbounds = formalTpar.upperBounds();
                for (int i = 0; i < formalUbounds.length; i++) {
                    formalUbounds[i] = (GenTypeSolid) formalUbounds[i].mapTparsToTypes(tparMap);
                    if (formalUbounds[i].isAssignableFrom(argTpar))
                        break;
                    if (i == formalUbounds.length - 1)
                        return null;
                }
                
                tparMap.put(formalTpar.getTparName(), argTpar);
            }
        }
        
        // For each argument, must check the compatibility of the supplied
        // parameter type with the argument type; and if neither the formal
        // parameter or supplied argument are raw, then must check generic
        // contract as well.
        
        for (int i = 0; i < args.length; i++) {
            JavaType formalArg = mparams[i];
            JavaType givenParam = args[i];
            
            // Substitute type arguments.
            formalArg = formalArg.mapTparsToTypes(tparMap);
            
            // check if the given parameter doesn't match the formal argument
            if (! formalArg.isAssignableFrom(givenParam)) {
                // a boxing conversion followed by a widening reference conversion
                if (! formalArg.isAssignableFrom(boxType(givenParam))) {
                    // an unboxing conversion followed by a widening primitive conversion
                    if (! formalArg.isAssignableFrom(unBox(givenParam))) {
                        return null;
                    }
                }
                boxingRequired = true;
            }
        }
        
        JavaType rType = jutils.getReturnType(m).mapTparsToTypes(tparMap);
        return new MethodCallDesc(m, Arrays.asList(mparams), varargs, boxingRequired, rType);
    }

    
    
    /**
     * Process a type inference constraint of the form "A is convertible to F".
     * Note F must be a valid formal parameter: it can't be a wildcard with multiple
     * bounds or an intersection type.
     * 
     * @param a  The argument type
     * @param f  The formal parameter type
     * @param tlbConstraints   lower bound constraints (a Map to Set of GenTypeSolid)
     * @param teqConstraints   equality constraints (a Map to GenTypeSolid)
     */
    private void processAtoFConstraint(JavaType a, GenTypeSolid f, Map tlbConstraints, Map teqConstraints)
    {
        a = boxType(a);
        if (a.isPrimitive())
            return; // no constraint
        
        if (f instanceof GenTypeTpar) {
            // The constraint T :> A is implied
            GenTypeTpar t = (GenTypeTpar) f;
            Set constraintsSet = (Set) tlbConstraints.get(t.getTparName());
            if (constraintsSet == null) {
                constraintsSet = new HashSet();
                tlbConstraints.put(t.getTparName(), constraintsSet);
            }
            
            constraintsSet.add(a);
        }
        
        // If F is an array of the form U[], and a is an array of the form V[]...
        else if (f.getArrayComponent() != null) {
            if (a.getArrayComponent() != null) {
                if (f.getArrayComponent() instanceof GenTypeSolid) {
                    a = a.getArrayComponent();
                    f = (GenTypeSolid) f.getArrayComponent();
                    processAtoFConstraint(a, f, tlbConstraints, teqConstraints);
                }
            }
        }
        
        // If F is of the form G<...> and A is convertible to the same form...
        else {
            GenTypeClass cf = (GenTypeClass) f;
            Map fMap = cf.getMap();
            if (fMap != null && a instanceof GenTypeSolid) {
                GenTypeClass [] asts = ((GenTypeSolid) a).getReferenceSupertypes();
                for (int i = 0; i < asts.length; i++) {
                    try {
                        GenTypeClass aMapped = asts[i].mapToSuper(cf.rawName());
                        // Superclass relationship is by capture conversion
                        if (! asts[i].rawName().equals(cf.rawName()))
                            aMapped = (GenTypeClass) captureConversion(aMapped);
                        Map aMap = aMapped.getMap();
                        if (aMap != null) {
                            Iterator j = fMap.keySet().iterator();
                            while (j.hasNext()) {
                                String tpName = (String) j.next();
                                GenTypeParameterizable fPar = (GenTypeParameterizable) fMap.get(tpName);
                                GenTypeParameterizable aPar = (GenTypeParameterizable) aMap.get(tpName);
                                processAtoFtpar(aPar, fPar, tlbConstraints, teqConstraints);
                            }
                        }
                    }
                    catch (BadInheritanceChainException bice) {}
                }
            }
        }
        return;
    }
    
    /**
     * Process type parameters from a type inference constraint A convertible-to F.
     */
    private void processAtoFtpar(GenTypeParameterizable aPar, GenTypeParameterizable fPar, Map tlbConstraints, Map teqConstraints)
    {
        if (fPar instanceof GenTypeSolid) {
            if (aPar instanceof GenTypeSolid) {
                // aPar = fPar
                processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
            }
        } else {
            GenTypeSolid flbound = fPar.getLowerBound();
            if (flbound != null) {
                // F-par is of form "? super ..."
                GenTypeSolid albound = aPar.getLowerBound();
                if (albound != null) {
                    // there should only be one element in albounds
                    // recurse with albounds[0] >> flbound[0]
                    processFtoAConstraint(albound, flbound, tlbConstraints, teqConstraints);
                }
            } else {
                // F-par is of form "? extends ..."
                GenTypeSolid [] fubounds = fPar.getUpperBounds();
                GenTypeSolid [] aubounds = aPar.getUpperBounds();
                if (fubounds.length > 0 && aubounds.length > 0) {
                    // recurse with aubounds << fubounds[0]
                    processAtoFConstraint(IntersectionType.getIntersection(aubounds), fubounds[0], tlbConstraints, teqConstraints);
                }
            }
        }
    }
    
    /**
     * Process a type inference constraint of the form "A is equal to F".
     */
    void processAeqFConstraint(GenTypeSolid a, GenTypeSolid f, Map tlbConstraints, Map teqConstraints)
    {
        if (f instanceof GenTypeTpar) {
            // The constraint T == A is implied.
            GenTypeTpar t = (GenTypeTpar) f;
            teqConstraints.put(t.getTparName(), a);
        }
        
        else if (f.getArrayComponent() instanceof GenTypeSolid) {
            // "If F = U[] ... if A is an array type V[], or a type variable with an
            // upper bound that is an array type V[]..."
            GenTypeSolid [] asts;
            if (a instanceof GenTypeDeclTpar)
                asts = ((GenTypeDeclTpar) a).upperBounds();
            else
                asts = new GenTypeSolid[] {a};
            
            for (int i = 0; i < asts.length; i++) {
                JavaType act = asts[i].getArrayComponent();
                if (act instanceof GenTypeSolid) {
                    processAeqFConstraint((GenTypeSolid) act, (GenTypeSolid) f.getArrayComponent(), tlbConstraints, teqConstraints);
                }
            }
        }
        
        else {
            GenTypeClass cf = f.asClass();
            GenTypeClass af = a.asClass();
            if (af != null && cf != null) {
                if (cf.rawName().equals(af.rawName())) {
                    Map fMap = cf.getMap();
                    Map aMap = af.getMap();
                    if (fMap != null && aMap != null) {
                        Iterator j = fMap.keySet().iterator();
                        while (j.hasNext()) {
                            String tpName = (String) j.next();
                            GenTypeParameterizable fPar = (GenTypeParameterizable) fMap.get(tpName);
                            GenTypeParameterizable aPar = (GenTypeParameterizable) aMap.get(tpName);
                            processAeqFtpar(aPar, fPar, tlbConstraints, teqConstraints);
                        }
                    }
                }
            }
        }
    }

    /**
     * Process type parameters from a type inference constraint A equal-to F.
     */
    private void processAeqFtpar(GenTypeParameterizable aPar, GenTypeParameterizable fPar, Map tlbConstraints, Map teqConstraints)
    {
        if (aPar instanceof GenTypeSolid && fPar instanceof GenTypeSolid) {
            processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
        }
        else if (aPar instanceof GenTypeWildcard && fPar instanceof GenTypeWildcard) {
            GenTypeSolid flBound = fPar.getLowerBound();
            GenTypeSolid [] fuBounds = fPar.getUpperBounds();
            // F = ? super U,  A = ? super V
            if (flBound != null) {
                GenTypeSolid alBound = aPar.getLowerBound();
                if (alBound != null)
                    processAeqFConstraint(alBound, flBound, tlbConstraints, teqConstraints);
            }
            // F = ? extends U, A = ? extends V
            else if (fuBounds.length != 0) {
                GenTypeSolid [] auBounds = aPar.getUpperBounds();
                if (auBounds.length != 0)
                    processAeqFConstraint(IntersectionType.getIntersection(auBounds), fuBounds[0], tlbConstraints, teqConstraints);
            }
        }
    }

    /**
     * Process a type inference constraint of the form "F is convertible to A".
     */
    private void processFtoAConstraint(GenTypeSolid a, GenTypeSolid f, Map tlbConstraints, Map teqConstraints)
    {
        // This is pretty much nothing like what the JLS says it should be. As far as I can
        // make out, the JLS is just plain wrong.
        
        // If F = T, then T <: A is implied: but we cannot make use of such a constraint.
        // If F = U[] ...
        if (f.getArrayComponent() instanceof GenTypeSolid) {
            // "If F = U[] ... if A is an array type V[], or a type variable with an
            // upper bound that is an array type V[]..."
            GenTypeSolid [] asts;
            if (a instanceof GenTypeDeclTpar)
                asts = ((GenTypeDeclTpar) a).upperBounds();
            else
                asts = new GenTypeSolid[] {a};
            
            for (int i = 0; i < asts.length; i++) {
                JavaType act = asts[i].getArrayComponent();
                if (act instanceof GenTypeSolid) {
                    processFtoAConstraint((GenTypeSolid) act, (GenTypeSolid) f.getArrayComponent(), tlbConstraints, teqConstraints);
                }
            }
        }
        
        else if (f.asClass() != null) {
            GenTypeClass cf = f.asClass();
            if (! (a instanceof GenTypeTpar)) {
                GenTypeClass [] asts = a.getReferenceSupertypes();
                for (int i = 0; i < asts.length; i++) {
                    try {
                        GenTypeClass fMapped = cf.mapToSuper(asts[i].rawName());
                        Map aMap = asts[i].getMap();
                        Map fMap = fMapped.getMap();
                        if (aMap != null && fMap != null) {
                            Iterator j = fMap.keySet().iterator();
                            while (j.hasNext()) {
                                String tpName = (String) j.next();
                                GenTypeParameterizable fPar = (GenTypeParameterizable) fMap.get(tpName);
                                GenTypeParameterizable aPar = (GenTypeParameterizable) aMap.get(tpName);
                                processFtoAtpar(aPar, fPar, tlbConstraints, teqConstraints);
                            }
                        }
                    }
                    catch (BadInheritanceChainException bice) {}
                }
            }
        }
    }

    /**
     * Process type parameters from a type inference constraint F convertible-to A.
     */
    private void processFtoAtpar(GenTypeParameterizable aPar, GenTypeParameterizable fPar, Map tlbConstraints, Map teqConstraints)
    {
        if (fPar instanceof GenTypeSolid) {
            if (aPar instanceof GenTypeSolid) {
                processAeqFConstraint((GenTypeSolid) aPar, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
            }
            else {
                GenTypeSolid alBound = aPar.getLowerBound();
                if (alBound != null) {
                    // aPar is of the form "? super ..."
                    processAtoFConstraint(alBound, (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
                }
                else {
                    GenTypeSolid [] auBounds = aPar.getUpperBounds();
                    if (auBounds.length != 0) {
                        processFtoAConstraint(auBounds[0], (GenTypeSolid) fPar, tlbConstraints, teqConstraints);
                    }
                }
            }
        }
        
        else {
            // fPar must be a wildcard
            GenTypeSolid flBound = fPar.getLowerBound();
            if (flBound != null) {
                if (aPar instanceof GenTypeWildcard) {
                    // fPar is ? super ...
                    GenTypeSolid alBound = aPar.getLowerBound();
                    if (alBound != null) {
                        processAtoFConstraint(alBound, flBound, tlbConstraints, teqConstraints);
                    }
                }
                else {
                    // fPar is ? extends ...
                    GenTypeSolid [] fuBounds = fPar.getUpperBounds();
                    GenTypeSolid [] auBounds = aPar.getUpperBounds();
                    if (auBounds.length != 0 && fuBounds.length != 0) {
                        processFtoAConstraint(auBounds[0], fuBounds[0], tlbConstraints, teqConstraints);
                    }
                }
            }
        }
    }

    /**
     * Get the return type of a method call expression. This is quite
     * complicated; see JLS section 15.12
     *
     * @throws RecognitionException
     * @throws SemanticException
     */
    private JavaType getMethodCallReturnType(AST node) throws RecognitionException, SemanticException
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
        
        AST firstArg = node.getFirstChild();
        AST secondArg = firstArg.getNextSibling();
        if (secondArg.getType() != JavaTokenTypes.ELIST)
            throw new RecognitionException();
        
        // we don't handle a variety of cases such
        // as "this.xxx()" or "super.xxxx()".
        
        if (firstArg.getType() == JavaTokenTypes.IDENT) {
            // It's an unqualified method call. In the context of a code pad
            // statement, it can only be an imported static method call.
            
            String mname = firstArg.getText();
            JavaType [] argumentTypes = getExpressionList(secondArg);
            
            List l = imports.getStaticImports(mname);
            MethodCallDesc candidate = findImportedMethod(l, mname, argumentTypes);
            
            if (candidate == null) {
                // There were no non-wildcard static imports. Try wildcard imports.
                l = imports.getStaticWildcardImports();
                candidate = findImportedMethod(l, mname, argumentTypes);
            }
            
            if (candidate != null)
                return captureConversion(candidate.retType);
            
            // no suitable candidates
            throw new SemanticException();
        }
        
        if (firstArg.getType() == JavaTokenTypes.DOT) {
            AST targetNode = firstArg.getFirstChild();
            JavaEntity callTarget = getEntity(targetNode);
            JavaType targetType = callTarget.getType();
                            
            // now get method name, and argument types;
            List typeArgs = new ArrayList(5);
            JavaType [] argumentTypes = getExpressionList(secondArg);
            String methodName = null;
            AST searchNode = targetNode.getNextSibling();
            
            // get the type arguments and method name
            while (searchNode != null) {
                int nodeType = searchNode.getType();
                // type argument?
                if (nodeType == JavaTokenTypes.TYPE_ARGUMENT) {
                    JavaType taType = getType(searchNode.getFirstChild());
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
            
            // getClass() is a special case, it should return Class<? extends
            // basetype>
            if (targetType instanceof GenTypeParameterizable) {
                if (methodName.equals("getClass") && argumentTypes.length == 0) {
                    List paramsl = new ArrayList(1);
                    paramsl.add(new GenTypeExtends((GenTypeSolid) targetType.getErasedType()));
                    return new GenTypeClass(new JavaReflective(Class.class), paramsl);
                }
            }
            
            // apply capture conversion to target
            targetType = captureConversion(targetType);
            
            if (! (targetType instanceof GenTypeSolid))
                throw new SemanticException();
                
            GenTypeSolid targetTypeS = (GenTypeSolid) targetType;
            GenTypeClass [] rsts = targetTypeS.getReferenceSupertypes();
            
            // match the call to a method:
            ArrayList suitableMethods = getSuitableMethods(methodName, rsts, argumentTypes, typeArgs);
            
            if (suitableMethods.size() != 0) {
                MethodCallDesc mcd = (MethodCallDesc) suitableMethods.get(0);
                // JLS 15.12.2.6, we must apply capture conversion
                return captureConversion(mcd.retType);
            }
            // ambiguity
            throw new SemanticException();
        }
        
        // anything else is an unknown
        throw new RecognitionException();
    }
    
    /**
     * Find the most specific imported method with the given name and argument types
     * in the given list of imports.
     * 
     * @param imports         A list of imports (ClassEntity)
     * @param mname           The name of the method to find
     * @param argumentTypes   The type of each supplied argument
     * @return   A descriptor for the most specific method, or null if none found
     */
    private MethodCallDesc findImportedMethod(List imports, String mname, JavaType [] argumentTypes)
        throws RecognitionException
    {
        MethodCallDesc candidate = null;
        
        // Iterate through the imports
        Iterator i = imports.iterator();
        while (i.hasNext()) {
            ClassEntity importEntity = (ClassEntity) i.next();
            List r = importEntity.getStaticMethods(mname);
            Iterator j = r.iterator();
            while (j.hasNext()) {
                // For each matching method, assess its applicability. If applicable,
                // and it is the most specific method yet found, keep it.
                Method m = (Method) j.next();
                MethodCallDesc mcd = isMethodApplicable(importEntity.getClassType(), Collections.EMPTY_LIST, m, argumentTypes);
                if (mcd != null) {
                    if (candidate == null) {
                        candidate = mcd;
                    }
                    else {
                        if (mcd.compareSpecificity(candidate) == 1)
                            candidate = mcd;
                    }
                }
            }
        }
        return candidate;
    }
    
    
    /**
     * Get the candidate list of methods with the given name and argument types.
     * @param methodName    The name of the method
     * @param targetTypes   The types to search for declarations of this method
     * @param argumentTypes The types of the arguments supplied in the method invocation
     * @param typeArgs      The type arguments, if any, supplied in the method invocation
     * @return  an ArrayList of MethodCallDesc - the list of candidate methods
     * @throws RecognitionException
     */
    private ArrayList getSuitableMethods(String methodName, GenTypeClass [] targetTypes, JavaType [] argumentTypes, List typeArgs)
        throws RecognitionException
    {
        ArrayList suitableMethods = new ArrayList();
        for (int k = 0; k < targetTypes.length; k++) {
            GenTypeClass targetClass = targetTypes[k];
            try {
                Class c = classLoader.loadClass(targetClass.rawName());
                Method [] methods = c.getMethods();
                
                // Find methods that are applicable, and
                // accessible. See JLS 15.12.2.1.
                for (int i = 0; i < methods.length; i++) {
                    // ignore bridge methods
                    if (jutils.isSynthetic(methods[i]))
                        continue;
                    
                    // check method name
                    if (methods[i].getName().equals(methodName)) {
                        // check that the method is applicable (and under
                        // what constraints)
                        MethodCallDesc mcd = isMethodApplicable(targetClass, typeArgs, methods[i], argumentTypes);
                        
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

            }
            catch (ClassNotFoundException cnfe) {
                return null; // shouldn't happen
            }
        }
        return suitableMethods;
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
    JavaType getTypeFromTypeNode(AST node) throws RecognitionException, SemanticException
    {
        AST firstChild = node.getFirstChild();
        JavaType baseType;
        
        switch (firstChild.getType()) {
            case JavaTokenTypes.LITERAL_char:
                baseType = JavaPrimitiveType.getChar();
                break;
            case JavaTokenTypes.LITERAL_byte:
                baseType = JavaPrimitiveType.getByte();
                break;
            case JavaTokenTypes.LITERAL_boolean:
                baseType = JavaPrimitiveType.getBoolean();
                break;
            case JavaTokenTypes.LITERAL_short:
                baseType = JavaPrimitiveType.getShort();
                break;
            case JavaTokenTypes.LITERAL_int:
                baseType = JavaPrimitiveType.getInt();
                break;
            case JavaTokenTypes.LITERAL_long:
                baseType = JavaPrimitiveType.getLong();
                break;
            case JavaTokenTypes.LITERAL_float:
                baseType = JavaPrimitiveType.getFloat();
                break;
            case JavaTokenTypes.LITERAL_double:
                baseType = JavaPrimitiveType.getDouble();
                break;
            default:
                // not a primitive type
                baseType = getType(firstChild);
        }
        
        // check for array declarators
        AST arrayNode = firstChild.getNextSibling();
        while (arrayNode != null && arrayNode.getType() == JavaTokenTypes.ARRAY_DECLARATOR) {
            // make a reflective for the array
            // figure out the class name of the array class
            String xName = "[" + baseType.arrayComponentName();
            try {
                // In Java 6, ClassLoader.loadClass() fails to load primitive
                // array classes; must use Class.forName instead.
                Class arrayClass = Class.forName(xName, false, classLoader);
                baseType = new GenTypeArray(baseType, new JavaReflective(arrayClass));
            }
            catch (ClassNotFoundException cnfe) {}
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
    private JavaType unBox(JavaType b)
    {
        if (b instanceof GenTypeClass) {
            GenTypeClass c = (GenTypeClass) b;
            String cName = c.rawName();
            if (cName.equals("java.lang.Integer"))
                return JavaPrimitiveType.getInt();
            else if (cName.equals("java.lang.Long"))
                return JavaPrimitiveType.getLong();
            else if (cName.equals("java.lang.Short"))
                return JavaPrimitiveType.getShort();
            else if (cName.equals("java.lang.Byte"))
                return JavaPrimitiveType.getByte();
            else if (cName.equals("java.lang.Character"))
                return JavaPrimitiveType.getChar();
            else if (cName.equals("java.lang.Float"))
                return JavaPrimitiveType.getFloat();
            else if (cName.equals("java.lang.Double"))
                return JavaPrimitiveType.getDouble();
            else if (cName.equals("java.lang.Boolean"))
                return JavaPrimitiveType.getBoolean();
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
    private JavaType boxType(JavaType u)
    {
        if (u instanceof JavaPrimitiveType) {
            if (u.typeIs(JavaType.JT_INT))
                return new GenTypeClass(new JavaReflective(Integer.class));
            else if (u.typeIs(JavaType.JT_LONG))
                return new GenTypeClass(new JavaReflective(Long.class));
            else if (u.typeIs(JavaType.JT_SHORT))
                return new GenTypeClass(new JavaReflective(Short.class));
            else if (u.typeIs(JavaType.JT_BYTE))
                return new GenTypeClass(new JavaReflective(Byte.class));
            else if (u.typeIs(JavaType.JT_CHAR))
                return new GenTypeClass(new JavaReflective(Character.class));
            else if (u.typeIs(JavaType.JT_FLOAT))
                return new GenTypeClass(new JavaReflective(Float.class));
            else if (u.typeIs(JavaType.JT_DOUBLE))
                return new GenTypeClass(new JavaReflective(Double.class));
            else if (u.typeIs(JavaType.JT_BOOLEAN))
                return new GenTypeClass(new JavaReflective(Boolean.class));
            else
                return u;
        }
        else
            return u;
    }
    
    static public boolean isBoxedBoolean(JavaType t)
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
     * @see TextParser#boxType(JavaType)
     * 
     * @param u    The type to box
     * @param box  The flag indicating whether boxing should occur
     * @return
     */
    private JavaType maybeBox(JavaType u, boolean box)
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
                    JavaType fpType = getExpressionType(firstDotArg).getType();
                    
                    // now evaluate the qualified new in the context of the type
                    // of the expression
                    if (fpType.asClass() != null) {
                        JavaType type = getInnerType(secondDotArg.getFirstChild(), fpType.asClass());
                        return new ExprValue(type);
                    }
                    
                    if (fpType == null)
                        return null;
                    else
                        throw new SemanticException();
                }
                
                // member field/type of some type
                if (secondDotArg.getType() == JavaTokenTypes.IDENT) {
                    
                    JavaEntity entity = getEntity(firstDotArg);
                    entity = entity.getSubentity(secondDotArg.getText());
                    
                    return new ExprValue(entity.getType());
                }
                
                // class literal
                if (secondDotArg.getType() == JavaTokenTypes.LITERAL_class) {
                    if (! java15)
                        return new ExprValue(new GenTypeClass(new JavaReflective(Class.class)));
                    
                    // we want "Class<X>", where X is the boxed type (or Void)
                    int fdType = firstDotArg.getType();
                    JavaReflective classReflective = new JavaReflective(Class.class);
                    List l = new ArrayList();

                    // add appropriate type to the type parameter list
                    if (fdType == JavaTokenTypes.LITERAL_void) {
                        l.add(new GenTypeClass(new JavaReflective(Void.class)));
                    }
                    else {
                        l.add(boxType(getTypeFromTypeNode(fcNode)));
                    }
                    
                    // ... then return Class<X>
                    return new ExprValue(new GenTypeClass(classReflective, l));
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
                JavaEntity entity = getEntity(fcNode);
                return new ExprValue(entity.getType());
            }
            
            // type cast.
            case JavaTokenTypes.TYPECAST:
                return new ExprValue(getTypeFromTypeNode(fcNode.getFirstChild()));
            
            // array element access
            case JavaTokenTypes.INDEX_OP:
            {
                JavaType t = getExpressionType(fcNode.getFirstChild()).getType();
                JavaType componentType = t.getArrayComponent();
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
            
            // true & false literals, "instanceof" expressions
            case JavaTokenTypes.LITERAL_true:
                return BooleanValue.getBooleanValue(true);
            case JavaTokenTypes.LITERAL_false:
                return BooleanValue.getBooleanValue(false);
            case JavaTokenTypes.LITERAL_instanceof:
                return new ExprValue(JavaPrimitiveType.getBoolean());
            
            case JavaTokenTypes.LITERAL_null:
                return new ExprValue(JavaPrimitiveType.getNull());
            
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
                return new ExprValue(JavaPrimitiveType.getBoolean());
            
            // shift operators. The result type is the unary-promoted
            // first operand type.
            // TODO handle operations on numeric constants (result is constant)
            case JavaTokenTypes.SL:
            case JavaTokenTypes.SR:
            case JavaTokenTypes.BSR:
            {
                JavaType rtype = unBox(getExpressionType(fcNode.getFirstChild()).getType());
                if (isMinorInteger(rtype))
                    rtype = JavaPrimitiveType.getInt();
                
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
                JavaType leftType = unBox(getExpressionType(leftNode).getType());
                JavaType rightType = unBox(getExpressionType(rightNode).getType());
                
                if (leftType.typeIs(JavaType.JT_BOOLEAN) && rightType.typeIs(JavaType.JT_BOOLEAN))
                {
                    // TODO handle constants
                    int ntype = fcNode.getType();
                    if (ntype == JavaTokenTypes.BAND || ntype == JavaTokenTypes.BOR || ntype == JavaTokenTypes.BXOR)
                        return new ExprValue(JavaPrimitiveType.getBoolean());
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
                JavaType leftType = unBox(getExpressionType(leftNode).getType());
                JavaType rightType = unBox(getExpressionType(rightNode).getType());
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
                JavaType ntype = unBox(oval.getType());
                if (! ntype.isIntegralType())
                    throw new SemanticException();
                
                if (! oval.knownValue()) {
                    // unary numeric promotion means the result is long/int
                    if (ntype.typeIs(JavaType.JT_LONG))
                        return new ExprValue(ntype);
                    else
                        return new ExprValue(JavaPrimitiveType.getInt());
                }
                
                // handle case where operand (and result) type is "long"
                if (ntype.typeIs(JavaType.JT_LONG)) {
                    long newval = ~ oval.longValue();
                    return new NumValue(ntype, new Long(newval));
                }
                
                // unary numeric promotion means the result must be "int".
                int newval = ~ oval.intValue();
                return new NumValue(JavaPrimitiveType.getInt(), new Integer(newval));
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
    static JavaRecognizer getParser()
    {
        JavaRecognizer jr = new JavaRecognizer(new ParserSharedInputState());
        jr.setASTNodeClass("bluej.parser.ast.LocatableAST");
        
        return jr;
    }
    
    /**
     * Return an appropriate token filter for parsing java command sequences.
     * @param s  The command sequence in question
     * @return   A filter (to remove comments) over the java command sequence
     */
    static TokenStream getTokenStream(String s)
    {
        StringReader r = new StringReader(s);
        
        // We use a lexer pipeline:
        // First, deal with escaped unicode characters:
        EscapedUnicodeReader eur = new EscapedUnicodeReader(r);

        // Next create the initial lexer stage
        JavaLexer lexer = new JavaLexer(eur);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        eur.setAttachedScanner(lexer);
        
        // Finally filter out comments and whitespace
        TokenStream filter = new JavaTokenFilter(lexer);
        
        return filter;
    }

    /**
     * Check if a member of some class is accessible from the context of the given
     * package. This will be the case if the member is public, if the member is
     * protected and declared in the same class, or if the member is package-
     * private and declared in the same class.
     * 
     * @param declaringClass  The class which declares the member
     * @param mods            The member modifier flags (as returned by getModifiers())
     * @param pkg             The package to check access for
     * @return  true if the package has access to the member
     */
    static boolean isAccessible(Class declaringClass, int mods, String pkg)
    {
        if (Modifier.isPrivate(mods))
            return false;
        
        if (Modifier.isPublic(mods))
            return true;
        
        // get the package of the class
        String className = declaringClass.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1)
            lastDot = 0;
        String classPkg = className.substring(0, lastDot);
        
        // it's not private nor public - so it's package private (or protected).
        // It is therefore accessible if the accessing package is the same as
        // the declaring package.
        return classPkg.equals(pkg);
    }
    
    /**
     * Find (if one exists) an accessible field with the given name in the given class (and
     * its supertypes). The "getField(String)" method in java.lang.Class does the same thing,
     * except it doesn't take into account accessible package-private fields which is
     * important.
     * 
     * @param c    The class in which to find the field
     * @param fieldName   The name of the accessible field to find
     * @param pkg         The package context from which the field is accessed
     * @param searchSupertypes  Whether to search in supertypes for the field
     * @return      The field
     * @throws NoSuchFieldException  if no accessible field with the given name exists
     */
    static Field getAccessibleField(Class c, String fieldName, String pkg, boolean searchSupertypes)
        throws NoSuchFieldException
    {
        String className = c.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1)
            lastDot = 0;
        
        String classPkg = className.substring(0, lastDot);
        
        // package private members accessible if the package is the same
        boolean pprivateAccessible = classPkg.equals(pkg);

        try {
            // Try fields declared in this class
            Field [] cfields = c.getDeclaredFields();
            for (int i = 0; i < cfields.length; i++) {
                if (cfields[i].getName().equals(fieldName)) {
                    int mods = cfields[i].getModifiers();
                    // rule out private fields
                    if (! Modifier.isPrivate(mods)) {
                        // now, if the fields is public, or package-private fields are
                        // accessible, then the field is accessible.
                        if (pprivateAccessible || Modifier.isPublic(mods)) {
                            return cfields[i];
                        }
                    }
                }
            }
            
            if (searchSupertypes) {
                // Try fields declared in superinterfaces
                Class [] ifaces = c.getInterfaces();
                for (int i = 0; i < ifaces.length; i++) {
                    try {
                        return getAccessibleField(ifaces[i], fieldName, pkg, true);
                    }
                    catch (NoSuchFieldException nsfe) { }
                }
                
                // Try fields declared in superclass
                Class sclass = c.getSuperclass();
                if (sclass != null)
                    return getAccessibleField(sclass, fieldName, pkg, true);
            }
        }
        catch (LinkageError le) { }
        
        throw new NoSuchFieldException();
    }
    
    /**
     * Get a list of accessible static methods declared in the given class with the
     * given name. The list includes public methods and, if the class is in the designated
     * package, package-private and protected methods.
     * 
     * @param c  The class in which to find the methods
     * @param methodName  The name of the methods to find
     * @param pkg   The accessing package
     * @return  A list of java.lang.reflect.Method
     */
    static List getAccessibleStaticMethods(Class c, String methodName, String pkg)
    {
        String className = c.getName();
        int lastDot = className.lastIndexOf('.');
        if (lastDot == -1)
            lastDot = 0;
        
        String classPkg = className.substring(0, lastDot);
        
        // package private members accessible if the package is the same
        boolean pprivateAccessible = classPkg.equals(pkg);

        try {
            List rlist = new ArrayList();
            
            // Now find methods declared in this class
            Method [] cmethods = c.getDeclaredMethods();
            methodLoop:
            for (int i = 0; i < cmethods.length; i++) {
                if (cmethods[i].getName().equals(methodName)) {
                    int mods = cmethods[i].getModifiers();
                    if (Modifier.isPrivate(mods) || ! Modifier.isStatic(mods))
                        continue methodLoop;
                    
                    if (! Modifier.isPublic(mods) && ! pprivateAccessible)
                        continue methodLoop;
                    
                    if (jutils.isSynthetic(cmethods[i]))
                        continue methodLoop;
                    
                    rlist.add(cmethods[i]);
                }
            }
            return rlist;
        }
        catch (LinkageError le) { }
        
        return Collections.EMPTY_LIST;
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
        public JavaType retType; // effective return type (before capture conversion)
        
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
        public MethodCallDesc(Method m, List argTypes, boolean vararg, boolean autoboxing, JavaType retType)
        {
            this.method = m;
            this.argTypes = argTypes;
            this.vararg = vararg;
            this.autoboxing = autoboxing;
            this.retType = retType;
        }
        
        /**
         * Find out which (if any) method call is strictly more specific than the
         * other. Both calls must be valid calls to the same method with the same
         * number of parameters.
         * 
         * @param other  The method to compare with
         * @return 1 if this method is more specific;
         *         -1 if the other method is more specific;
         *         0 if neither method is more specific than the other.
         * 
         * See JLS 15.12.2.5 (by "more specific", we mean what the JLS calls
         * "strictly more specific", more or less. We also take arity and
         * abstractness into account)
         */
        public int compareSpecificity(MethodCallDesc other)
        {
            if (other.vararg && ! vararg)
                return 1; // we are more specific
            if (! other.vararg && vararg)
                return -1; // we are less specific
            
            // I am reasonably sure this gives the same result as the algorithm
            // described in the JLS section 15.12.2.5, and it has the advantage
            // of being a great deal simpler.
            Iterator i = argTypes.iterator();
            Iterator j = other.argTypes.iterator();
            int upCount = 0;
            int downCount = 0;
            
            while (i.hasNext()) {
                JavaType myArg = (JavaType) i.next();
                JavaType otherArg = (JavaType) j.next();
                
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
            
            // finally, if one method is abstract and the other is not,
            // then the non-abstract method is more specific.
            method.getModifiers();
            boolean isAbstract = Modifier.isAbstract(method.getModifiers());
            boolean otherAbstract = Modifier.isAbstract(other.method.getModifiers());
            if (isAbstract && ! otherAbstract)
                return -1;
            else if (! isAbstract && otherAbstract)
                return 1;
            else
                return 0;
        }
    }
    
    class PackageEntity extends PackageOrClass
    {
        String packageName;
        
        PackageEntity(String pname)
        {
            packageName = pname;
        }
        
        JavaType getType() throws SemanticException
        {
            throw new SemanticException();
        }
        
        void setTypeParams(List tparams) throws SemanticException
        {
            // a package cannot be parameterized!
            throw new SemanticException();
        }
        
        JavaEntity getSubentity(String name) throws SemanticException
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
        
        public String getName()
        {
            return packageName;
        }
        
        public boolean isClass()
        {
            return false;
        }
    }
    
    class TypeEntity extends ClassEntity
    {
        Class thisClass;
        List tparams;
        GenTypeClass outer;
        
        TypeEntity(Class c)
        {
            thisClass = c;
            tparams = Collections.EMPTY_LIST;
        }
        
        TypeEntity(Class c, List tparams)
        {
            thisClass = c;
            this.tparams = tparams;
        }
        
        TypeEntity(Class c, GenTypeClass outer)
        {
            thisClass = c;
            this.outer = outer;
            tparams = Collections.EMPTY_LIST;
        }
        
        TypeEntity(Class c, GenTypeClass outer, List tparams)
        {
            thisClass = c;
            this.outer = outer;
            this.tparams = tparams;
        }

        ClassEntity setTypeParams(List tparams) throws SemanticException
        {
            // this.tparams = tparams;
            return new TypeEntity(thisClass, outer, tparams);
        }

        JavaType getType()
        {
            return getClassType();
        }
        
        GenTypeClass getClassType()
        {
            return new GenTypeClass(new JavaReflective(thisClass), tparams, outer);
        }
        
        JavaEntity getSubentity(String name) throws SemanticException
        {
            // subentity of a class could be a member type or field
            // Is it a field?
            Field f = null;
            try {
                f = getAccessibleField(thisClass, name, packageScope, true);
                JavaType fieldType;
                Map tparmap = getClassType().getMap();
                
                // raw type? (though won't affect static fields)
                if (tparmap == null && ! Modifier.isStatic(f.getModifiers()))
                    fieldType = JavaUtils.getJavaUtils().getRawFieldType(f);
                else {
                    tparmap = captureConversion(getClassType(), new HashMap()).getMap();
                    fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                    if (tparmap != null)
                        fieldType = fieldType.mapTparsToTypes(tparmap);
                    // JLS 15.11.1, field access using a primary, capture conversion must
                    // be applied.
                    fieldType = captureConversion(fieldType);
                }
                
                return new ValueEntity(fieldType, getName() + "." + name);
            }
            catch (NoSuchFieldException nsfe) {}

            // Is it a member type?
            return getPackageOrClassMember(name);
        }
        
        PackageOrClass getPackageOrClassMember(String name) throws SemanticException
        {
            // A class cannot have a package member...
            return new TypeEntity(getMemberClass(name), getClassType());
        }
        
        Class getMemberClass(String name) throws SemanticException
        {
            // Is it a member type?
            Class c;
            try {
                c = classLoader.loadClass(thisClass.getName() + '$' + name);
                return c;
                //return new TypeEntity(c, (GenTypeClass) getType());
            }
            catch (ClassNotFoundException cnfe) {
                // No more options - it must be an error
                throw new SemanticException();
            }
        }
        
        ClassEntity getStaticMemberClass(String name) throws SemanticException
        {
            Class c = getMemberClass(name);
            if (Modifier.isStatic(c.getModifiers()))
                return new TypeEntity(c, (GenTypeClass) getType());
            
            // Not a static member - we fail
            throw new SemanticException();
        }
        
        JavaEntity getStaticField(String name) throws SemanticException
        {
            Field f = null;
            try {
                f = getAccessibleField(thisClass, name, packageScope, false);
                
                if (Modifier.isStatic(f.getModifiers())) {
                    JavaType fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                    // JLS 15.11.1, field access using a primary, capture conversion must
                    // be applied.
                    fieldType = captureConversion(fieldType);

                    // TODO for final fields, return an entity with a value
                    return new ValueEntity(fieldType, getName() + "." + name);
                }
            }
            catch (NoSuchFieldException nsfe) {}
            throw new SemanticException();
        }
        
        List getStaticMethods(String name)
        {
            return getAccessibleStaticMethods(thisClass, name, packageScope);
        }
        
        String getName()
        {
            return getType().toString();
        }
        
        public boolean isClass()
        {
            return true;
        }
    }
    
    class ValueEntity extends JavaEntity
    {
        JavaType type;
        String name;
        
        ValueEntity(JavaType type)
        {
            this.type = type;
        }
        
        ValueEntity(JavaType type, String name)
        {
            this.type = type;
            this.name = name;
        }
        
        JavaType getType()
        {
            return type;
        }
        
        JavaEntity getSubentity(String name)
            throws SemanticException
        {
            // Should be a member field.
            if (!(type instanceof GenTypeClass))
                throw new SemanticException();

            // get the class part of our type
            GenTypeClass thisClass = (GenTypeClass) captureConversion(type);
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
                f = getAccessibleField(c, name, packageScope, true);
                // Map type parameters to declaring class
                Class declarer = f.getDeclaringClass();
                Map tparMap = thisClass.mapToSuper(declarer.getName()).getMap();

                JavaType fieldType;
                if (tparMap != null || Modifier.isStatic(f.getModifiers())) {
                    // Not raw. Apply type parameters to field declaration.
                    fieldType = JavaUtils.getJavaUtils().getFieldType(f);
                    if (tparMap != null)
                        fieldType = fieldType.mapTparsToTypes(tparMap);
                    // JLS 15.11.1 says we must apply capture conversion
                    fieldType = captureConversion(fieldType);
                }
                else
                    fieldType = JavaUtils.getJavaUtils().getRawFieldType(f);

                if (name == null)
                    return new ValueEntity(fieldType);
                else
                    return new ValueEntity(fieldType, this.name + "." + name);
            }
            catch (NoSuchFieldException nsfe) {
                throw new SemanticException();
            }
        }
        
        String getName()
        {
            return name;
        }
        
        public boolean isClass()
        {
            return false;
        }
    }
    
    /**
     * A value (possibly unknown) with assosciated type
     */
    static class ExprValue
    {
        // default implementation has no known value
        public JavaType type;
        
        public ExprValue(JavaType type)
        {
            this.type = type;
        }
        
        public JavaType getType()
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
    
    static class BooleanValue extends ExprValue
    {
        boolean val;

        // constructor is private: use getBooleanValue instead
        private BooleanValue(boolean val)
        {
            super(JavaPrimitiveType.getBoolean());
            this.val = val;
        }
        
        // cache the two values
        public static BooleanValue trueVal = null;
        public static BooleanValue falseVal = null;
        
        /**
         * Get an instance of BooleanValue, representing either true or false.
         */
        public static BooleanValue getBooleanValue(boolean val)
        {
            if (val == true) {
                if (trueVal == null)
                    trueVal = new BooleanValue(true);
                return trueVal;
            }
            else {
                if (falseVal == null)
                    falseVal = new BooleanValue(false);
                return falseVal;
            }
        }
        
        public boolean booleanValue()
        {
            return val;
        }
    }
    
    class NumValue extends ExprValue
    {
        private Number val;
        
        NumValue(JavaType type, Number val)
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
    
    /**
     * A class to represent a variable declared by a statement. This contains
     * the variable name and type, and whether or not it was initialized.
     */
    public class DeclaredVar
    {
        private boolean isVarInit = false;
        private JavaType declVarType;
        private String varName;
        private boolean isFinal = false;
        
        public DeclaredVar(boolean isVarInit, boolean isFinal, JavaType varType, String varName)
        {
            this.isVarInit = isVarInit;
            this.declVarType = varType;
            this.varName = varName;
            this.isFinal = isFinal;
        }
        
        /**
         * Check whether the variable declaration included an initialization.
         */
        public boolean checkVarInit()
        {
            return isVarInit;
        }
        
        /**
         * Get the type of variable which was declared by the recently parsed
         * statement. 
         */
        public JavaType getDeclaredVarType()
        {
            return declVarType;
        }
        
        /**
         * Get the name of the declared variable.
         */
        public String getName()
        {
            return varName;
        }
        
        /**
         * Check whether the variable was declared "final".
         */
        public boolean checkFinal()
        {
            return isFinal;
        }
    }
}
