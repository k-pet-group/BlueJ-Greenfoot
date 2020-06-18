/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2012,2013,2014,2019,2020  Michael Kolling and John Rosenberg
 
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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.TextAnalyzer.MethodCallDesc;
import bluej.parser.entity.ConstantBoolValue;
import bluej.parser.entity.ConstantFloatValue;
import bluej.parser.entity.ConstantIntValue;
import bluej.parser.entity.ConstantStringEntity;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.ErrorEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.NullEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.SolidTargEntity;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnboundedWildcardEntity;
import bluej.parser.entity.UnresolvedEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.entity.WildcardExtendsEntity;
import bluej.parser.entity.WildcardSuperEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.utility.JavaReflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A parser for the codepad.
 * 
 * @author Davin McCall
 */
public class TextParser extends JavaParser
{
    private EntityResolver resolver;
    private JavaEntity accessType;
    private boolean staticAccess;
    
    protected Stack<JavaEntity> valueStack = new Stack<JavaEntity>();
    private int arrayCount = 0;
    
    /** A class to represent operators, possibly associated with a token */
    protected class Operator
    {
        int type;
        LocatableToken token;
        Operator(int type, LocatableToken token) { this.type = type; this.token = token; }
        int getType() { return type; }
        LocatableToken getToken() { return token; }
    }
    
    protected Stack<Operator> operatorStack = new Stack<Operator>();
    
    /** A stack of type argument lists - one list for each method call on the operator stack */
    protected Stack<List<LocatableToken>> typeArgStack = new Stack<List<LocatableToken>>();
    
    /*
     * Make up some operators - only for cases where there is not a token
     * type we could use, or for where the token is ambiguos:
     */
    
    private static final int CAST_OPERATOR = JavaTokenTypes.INVALID + 1;
    private static final int BAD_CAST_OPERATOR = CAST_OPERATOR + 1;
    private static final int PAREN_OPERATOR = BAD_CAST_OPERATOR + 1;
    private static final int MEMBER_CALL_OP = PAREN_OPERATOR + 1;
    private static final int METHOD_CALL_OP = MEMBER_CALL_OP + 1;
    private static final int CONSTRUCTOR_CALL_OP = METHOD_CALL_OP + 1;
    
    private static final int UNARY_PLUS_OP = CONSTRUCTOR_CALL_OP + 1;
    private static final int UNARY_MINUS_OP = UNARY_PLUS_OP + 1;
    
    /*
     * Parse states - mainly so we know what to do with a type specification
     * when we seen one:
     */
    
    private static final int STATE_NONE = 0;
    private static final int STATE_NEW = 1;  // just saw "new"
    private static final int STATE_NEW_ARGS = 2;  // expecting "new" arguments or array dimensions
    private static final int STATE_INSTANCEOF = 3;  // just saw "instanceof", expecting type
    
    private int state = STATE_NONE;

    /** Arguments for a method or constructor call are added to the list at the top of this stack */
    private Stack<List<JavaEntity>> argumentStack = new Stack<List<JavaEntity>>();
    
    
    /**
     * Construct a text parser for parsing an expression.
     * 
     * @param resolver   Resolver to resolve symbols
     * @param r           Reader to read the expression source
     * @param accessType   The containing type
     * @param staticAccess Whether the expression occurs in a static context
     */
    public TextParser(EntityResolver resolver, Reader r, JavaEntity accessType, boolean staticAccess)
    {
        super(r);
        this.resolver = resolver;
        this.accessType = accessType;
        this.staticAccess = staticAccess;
    }
    
    /**
     * Construct a text parser for parsing an expression, where the expression is located
     * at a particular line and column in the source.
     * 
     * @param resolver   Resolver to resolve symbols
     * @param r           Reader to read the expression source
     * @param accessType   The containing type
     * @param staticAccess Whether the expression occurs in a static context
     * @param line        The line in the source where the expression occurs
     * @param col         The column in the source where the expression occurs
     */
    public TextParser(EntityResolver resolver, Reader r, JavaEntity accessType, boolean staticAccess,
            int line, int col, int pos)
    {
        super(r, line, col, pos);
        this.resolver = resolver;
        this.accessType = accessType;
        this.staticAccess = staticAccess;
    }
    
    /**
     * Construct a text parser for parsing an expression which is represented in a String.
     * 
     * @param resolver   Resolver to resolve symbols
     * @param s           A string containing the expression
     * @param accessType   The containing type
     * @param staticAccess Whether the expression occurs in a static context
     */
    public TextParser(EntityResolver resolver, String s, JavaEntity accessType, boolean staticAccess)
    {
        this(resolver, new StringReader(s), accessType, staticAccess);
    }
    
    /**
     * Check whether the parsed expression ended at the end of the input.
     *  
     * @return  true iff the parsed expression ended at the end of the input (reader or string);
     *          false otherwise.
     */
    public boolean atEnd()
    {
        return tokenStream.LA(1).getType() == JavaTokenTypes.EOF;
    }
    
    /**
     * Get the type of the parsed expression. If the type is unknown or an error occurred,
     * returns {@code null}.
     */
    public JavaEntity getExpressionType()
    {
        processHigherPrecedence(getPrecedence(JavaTokenTypes.EOF) - 1);
        if (valueStack.isEmpty()) {
            return null;
        }
        return valueStack.pop();
    }
    
    /**
     * Pop an item from the value stack. If there are no values to pop, supply an error entity.
     */
    protected JavaEntity popValueStack()
    {
        if (! valueStack.isEmpty()) {
            return valueStack.pop();
        }
        return new ErrorEntity();
    }
    
    /**
     * Get the precedence level for a given operator type.
     */
    private int getPrecedence(int tokenType)
    {
        switch (tokenType) {
        case JavaTokenTypes.LCURLY:
            return -10; // beginning of anon class body
        case PAREN_OPERATOR:
            return -2;
        case JavaTokenTypes.LITERAL_new:
            return -1;
        case JavaTokenTypes.ASSIGN:
        case JavaTokenTypes.BAND_ASSIGN:
        case JavaTokenTypes.BOR_ASSIGN:
        case JavaTokenTypes.PLUS_ASSIGN:
        case JavaTokenTypes.MINUS_ASSIGN:
        case JavaTokenTypes.STAR_ASSIGN:
        case JavaTokenTypes.DIV_ASSIGN:
        case JavaTokenTypes.SL_ASSIGN:
        case JavaTokenTypes.SR_ASSIGN:
        case JavaTokenTypes.BSR_ASSIGN:
        case JavaTokenTypes.MOD_ASSIGN:
        case JavaTokenTypes.BXOR_ASSIGN:
            return 0;
        case JavaTokenTypes.QUESTION:
            return 1;
        case JavaTokenTypes.LT:
        case JavaTokenTypes.LE:
        case JavaTokenTypes.GT:
        case JavaTokenTypes.GE:
        case JavaTokenTypes.LITERAL_instanceof:
            return 8;
        case JavaTokenTypes.EQUAL:
        case JavaTokenTypes.NOT_EQUAL:
            return 9;        
        case JavaTokenTypes.SL:
        case JavaTokenTypes.SR:
        case JavaTokenTypes.BSR:
            return 10;
        case JavaTokenTypes.PLUS:
        case JavaTokenTypes.MINUS:
            return 11;
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
            return 12;
        case JavaTokenTypes.LNOT:
        case JavaTokenTypes.BNOT:
        case JavaTokenTypes.INC:
        case JavaTokenTypes.DEC:
        case UNARY_PLUS_OP:
        case UNARY_MINUS_OP:
        case CAST_OPERATOR:
        case BAD_CAST_OPERATOR:
            return 13;
        case JavaTokenTypes.DOT:
        case MEMBER_CALL_OP:
        case METHOD_CALL_OP:
        case CONSTRUCTOR_CALL_OP:
            return 20;
        default:
        }
        
        return -1;
    }
    
    /** 
     * Process all on-stack operators with a higher precedence than that given
     */
    private void processHigherPrecedence(int precedence)
    {
        while (! operatorStack.isEmpty()) {
            Operator top = operatorStack.peek();
            if (getPrecedence(top.getType()) <= precedence) {
                break;
            }
            operatorStack.pop();
            processOperator(top);
        }
    }
    
    /**
     * Process an operator, take the operands from the value stack and leave the result on the
     * stack.
     */
    private void processOperator(Operator operator)
    {
        int tokenType = operator.getType();
        
        JavaEntity arg1;
        JavaEntity arg2;
        
        switch (tokenType) {
        case JavaTokenTypes.PLUS:
        case JavaTokenTypes.MINUS:
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
        case JavaTokenTypes.MOD:
        case JavaTokenTypes.SL:
        case JavaTokenTypes.SR:
        case JavaTokenTypes.BSR:
        case JavaTokenTypes.LT:
        case JavaTokenTypes.LE:
        case JavaTokenTypes.GT:
        case JavaTokenTypes.GE:
        case JavaTokenTypes.EQUAL:
        case JavaTokenTypes.NOT_EQUAL:
        case JavaTokenTypes.ASSIGN:
        case JavaTokenTypes.BAND_ASSIGN:
        case JavaTokenTypes.BOR_ASSIGN:
        case JavaTokenTypes.PLUS_ASSIGN:
        case JavaTokenTypes.MINUS_ASSIGN:
        case JavaTokenTypes.STAR_ASSIGN:
        case JavaTokenTypes.DIV_ASSIGN:
        case JavaTokenTypes.SL_ASSIGN:
        case JavaTokenTypes.SR_ASSIGN:
        case JavaTokenTypes.BSR_ASSIGN:
        case JavaTokenTypes.MOD_ASSIGN:
        case JavaTokenTypes.BXOR_ASSIGN:
        case JavaTokenTypes.BAND:
        case JavaTokenTypes.BOR:
        case JavaTokenTypes.BXOR:
        case JavaTokenTypes.LOR:
        case JavaTokenTypes.LAND:
            arg2 = popValueStack();
            arg1 = popValueStack();
            doBinaryOp(arg1, arg2, operator);
            break;
        case JavaTokenTypes.LNOT:
        case JavaTokenTypes.BNOT:
        case JavaTokenTypes.INC:
        case JavaTokenTypes.DEC:
        case UNARY_MINUS_OP:
        case UNARY_PLUS_OP:
            arg1 = popValueStack();
            checkArg(arg1, operator);
            break;
        case CAST_OPERATOR:
            doCast();
            break;
        case BAD_CAST_OPERATOR:
            popValueStack(); // remove the value being cast
            valueStack.push(new ErrorEntity());
            break;
        case JavaTokenTypes.QUESTION:
            processQuestionOperator();
            break;
        }
    }

    @OnThread(Tag.FXPlatform)
    private strictfp void doCast()
    {
        // Conversions allowed are specified in JLS 3rd ed. 5.5.
        // But see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7029688
        
        ValueEntity varg1 = popValueStack().resolveAsValue(); // value being cast
        TypeEntity castType = popValueStack().resolveAsType();  // cast-to type
        if (varg1 == null || castType == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        if (ValueEntity.isConstant(varg1)) {
            JavaType jctype = castType.getType();
            if (jctype.isIntegralType()) {
                long ival;
                if (varg1.hasConstantIntValue()) {
                    ival = varg1.getConstantIntValue();
                    if (jctype.typeIs(JavaType.JT_BYTE)) {
                        ival = (byte) ival;
                    }
                    else if (jctype.typeIs(JavaType.JT_CHAR)) {
                        ival = (char) ival;
                    }
                    else if (jctype.typeIs(JavaType.JT_INT)) {
                        ival = (int) ival;
                    }
                    else if (jctype.typeIs(JavaType.JT_SHORT)) {
                        ival = (short) ival;
                    }
                }
                else if (varg1.hasConstantFloatValue()) {
                    if (jctype.typeIs(JavaType.JT_BYTE)) {
                        ival = (byte) varg1.getConstantFloatValue();
                    }
                    else if (jctype.typeIs(JavaType.JT_CHAR)) {
                        ival = (char) varg1.getConstantFloatValue();
                    }
                    else if (jctype.typeIs(JavaType.JT_INT)) {
                        ival = (int) varg1.getConstantFloatValue();
                    }
                    else if (jctype.typeIs(JavaType.JT_SHORT)) {
                        ival = (short) varg1.getConstantFloatValue();
                    }
                    else {
                        ival = (long) varg1.getConstantFloatValue();
                    }
                }
                else {
                    // string constant
                    valueStack.push(new ErrorEntity());
                    return;
                }
                
                valueStack.push(new ConstantIntValue(null, jctype, ival));
                return;
            }
            else if (jctype.typeIs(JavaType.JT_FLOAT) || jctype.typeIs(JavaType.JT_DOUBLE)) {
                double dval;
                if (varg1.hasConstantIntValue()) {
                    dval = varg1.getConstantIntValue();
                }
                else if (varg1.hasConstantFloatValue()) {
                    dval = varg1.getConstantFloatValue();
                }
                else {
                    valueStack.push(new ErrorEntity());
                    return;
                }
                
                if (jctype.typeIs(JavaType.JT_FLOAT)) {
                    dval = (float) dval;
                }
                
                valueStack.push(new ConstantFloatValue(jctype, dval));
                return;
            }
            else if (jctype.toString().equals("java.lang.String")) {
                // Argument is constant: it must be a constant string.
                if (varg1.isConstantString()) {
                    valueStack.push(varg1);
                }
                else {
                    valueStack.push(new ErrorEntity());
                }
                return;
            }
        }
        
        // Argument is not a constant, or cast is to a type that won't result in
        // a constant.
        
        JavaType argType = varg1.getType();
        JavaType jctype = castType.getType();
        
        if (argType.isPrimitive() && ! jctype.isPrimitive()) {
            if (argType.typeIs(JavaType.JT_NULL)) {
                valueStack.push(new ValueEntity(jctype));
                return;
            }
            
            // The only remaining allowable case is a boxing conversion.
            
            String jctypeStr = jctype.toString();
            
            if (argType.typeIs(JavaType.JT_BOOLEAN) && jctypeStr.equals("java.lang.Boolean")
                    || argType.typeIs(JavaType.JT_CHAR) && jctypeStr.equals("java.lang.Character")
                    || argType.typeIs(JavaType.JT_BYTE) && jctypeStr.equals("java.lang.Byte")
                    || argType.typeIs(JavaType.JT_SHORT) && jctypeStr.equals("java.lang.Short")
                    || argType.typeIs(JavaType.JT_INT) && jctypeStr.equals("java.lang.Integer")
                    || argType.typeIs(JavaType.JT_LONG) && jctypeStr.equals("java.lang.Long")
                    || argType.typeIs(JavaType.JT_FLOAT) && jctypeStr.equals("java.lang.Float")
                    || argType.typeIs(JavaType.JT_DOUBLE) && jctypeStr.equals("java.lang.Double")) {
                valueStack.push(new ValueEntity(jctype));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            return;
        }
        
        JavaType argUnboxed = TextAnalyzer.unBox(argType);
        
        if (argUnboxed.isNumeric() && jctype.isNumeric()) {
            // Widening or narrowing primitive conversion - allowed.
            // NOTE: The JLS doesn't actually allow the case where the argument type is a
            //       boxed type, but the javac compiler *does*, as does the Eclipse compiler.
            // NOTE: Also, the JLS doesn't allow a cast from byte to char, because (JLS 5.1.4)
            //       that requires a widening conversion *followed by* a narrowing conversion,
            //       which isn't one of the options allowed for a cast.
            valueStack.push(new ValueEntity(jctype));
            return;
        }
        
        if (jctype.isPrimitive()) {
            // Note the numeric cases are handled above.
            if (jctype.typeIs(JavaType.JT_BOOLEAN) && argUnboxed.typeIs(JavaType.JT_BOOLEAN)) {
                valueStack.push(new ValueEntity(jctype));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            return;
        }
        
        // TODO check operand can be cast to this type - widening or narrowing
        //      reference conversion.
        valueStack.push(new ValueEntity(jctype));
    }
    
    @Override
    protected void gotArrayElementAccess()
    {
        JavaEntity index = popValueStack();
        processHigherPrecedence(getPrecedence(JavaTokenTypes.DOT) - 1); // Process DOT precedence and higher
        JavaEntity array = popValueStack();
        
        index = index.resolveAsValue();
        array = array.resolveAsValue();
        if (index == null || array == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        JavaType componentType = array.getType().getArrayComponent();
        if (componentType == null) {
            valueStack.push(new ErrorEntity());
        }
        else {
            valueStack.push(new ValueEntity(componentType));
        }
    }
    
    private void processNewOperator(Operator token)
    {
        if (! argumentStack.isEmpty()) {
            argumentStack.pop(); // constructor arguments
            // TODO check argument validity
        }
        else {
            popValueStack();
            valueStack.push(new ErrorEntity());
        }
    }
    
    /**
     * Process the "member call operator".
     */
    private void processMemberCall(Operator op)
    {
        // See JLS 15.12
        // step 1 - determine type to search
        //  Seeing as this is a member call, that's already mostly done.
        JavaEntity target = popValueStack();
        JavaEntity vtarget = target.resolveAsValue();
        GenTypeSolid targetType;
        
        if (vtarget != null) {
            GenTypeSolid stype = vtarget.getType().asSolid();
            if (stype == null) {
                // call on a primitive
                valueStack.push(new ErrorEntity());
                return;
            }
            targetType = stype;
        }
        else {
            // entity may be a type rather than a value
            target = target.resolveAsType();
            if (target == null) {
                valueStack.push(new ErrorEntity());
                return;
            }
            GenTypeSolid stype = target.getType().getCapture().asSolid();
            if (stype == null) {
                // call on a primitive
                valueStack.push(new ErrorEntity());
                return;
            }
            targetType = stype;
        }
        
        if (op.getToken().getType() == JavaTokenTypes.IDENT) {
            processMethodCall(targetType, op.getToken().getText(), typeArgStack.pop());
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    private void processMethodCall(Operator op)
    {
        if (op.getToken().getType() == JavaTokenTypes.IDENT) {
            processMethodCall(accessType.getType().asClass(), op.getToken().getText(),
                    Collections.<LocatableToken>emptyList());
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    private void processMethodCall(GenTypeSolid targetType, String methodName, List<LocatableToken> typeArgTokens)
    {
        GenTypeClass accessClass = accessType.getType().asClass();
        if (accessClass == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        // Gather the argument types.
        List<JavaEntity> argList = argumentStack.pop();
        JavaType [] argTypes = new JavaType[argList.size()];
        for (int i = 0; i < argTypes.length; i++) {
            JavaEntity cent = argList.get(i).resolveAsValue();
            if (cent == null) {
                valueStack.push(new ErrorEntity());
                return;
            }
            argTypes[i] = cent.getType();
        }

        // Determine type arguments to method invocation
        List<GenTypeParameter> typeArgs;
        if (! typeArgTokens.isEmpty()) {
            DepthRef depthRef = new DepthRef();
            ListIterator<LocatableToken> i = typeArgTokens.listIterator(1); // skip '<'
            List<TypeArgumentEntity> typeArgEnts = readTypeArguments(i, depthRef);
            if (typeArgEnts == null) {
                valueStack.push(new ErrorEntity());
                return;
            }
            
            typeArgs = new ArrayList<GenTypeParameter>(typeArgEnts.size());
            for (TypeArgumentEntity typeArgEnt : typeArgEnts) {
                GenTypeParameter targType = typeArgEnt.getType();
                if (targType == null || targType.isPrimitive() || targType.isWildcard()) {
                    valueStack.push(new ErrorEntity());
                    return;
                }
                typeArgs.add(targType);
            }
        }
        else {
            typeArgs = Collections.emptyList();
        }

        ArrayList<MethodCallDesc> suitable = TextAnalyzer.getSuitableMethods(methodName,
                targetType, argTypes, typeArgs, accessClass.getReflective());
        if (suitable.size() == 0) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        // JLS 3rd ed. 15.12.2.5. We already have a list of the maximally specific methods:
        // - if one or more methods have non override-equivalent signatures, the call is ambiguous
        // - otherwise, if there is exactly one non-abstract method, choose that one;
        // - otherwise, arbitrarily choose one from the set with the most specific return type.

        int nonAbstractCount = 0;
        MethodCallDesc nonAbstractMethod = null;
        MethodCallDesc mostSpecificMethod = null;

        Iterator<MethodCallDesc> i = suitable.iterator();
        MethodCallDesc first = i.next();
        if (! first.method.isAbstract()) {
            nonAbstractCount++;
            nonAbstractMethod = first;
        }
        mostSpecificMethod = first;
        
        if (suitable.size() > 1) {
            List<GenTypeDeclTpar> tpars = first.method.getTparTypes();
            List<JavaType> paramTypes = first.method.getParamTypes();
            while (i.hasNext()) {
                MethodCallDesc next = i.next();
                if (!checkOverrideEquivalence(tpars, paramTypes, next.method.getTparTypes(), next.method.getParamTypes())) {
                    // Non override equivalent signatures: ambiguous call
                    valueStack.push(new ErrorEntity());
                    return;
                }
                if (! next.method.isAbstract()) {
                    nonAbstractCount++;
                    nonAbstractMethod = next;
                }
                
                if (mostSpecificMethod.retType.isAssignableFrom(next.retType)) {
                    mostSpecificMethod = next;
                }
            }
        }
        
        MethodCallDesc chosenMethod = nonAbstractMethod;
        if (nonAbstractCount != 1) {
            chosenMethod = mostSpecificMethod;
        }
        
        // TODO check applicability of chosen method as per JLS 3rd ed. 15.12.3

        valueStack.push(new ValueEntity(chosenMethod.retType));
    }

    @OnThread(Tag.FXPlatform)
    private static boolean checkOverrideEquivalence(List<GenTypeDeclTpar> firstTpars, List<JavaType> firstParamTypes,
            List<GenTypeDeclTpar> secondTpars, List<JavaType> secondParamTypes)
    {
        if (firstTpars.size() != 0 && secondTpars.size() != 0) {
            // Type parameters must be matchable
            if (firstTpars.size() != secondTpars.size()) {
                return false;
            }
            
            // Create a map from second method tpar name to first method tpar
            Map<String,GenTypeParameter> tparMap = new HashMap<String,GenTypeParameter>();
            Iterator<GenTypeDeclTpar> i = firstTpars.iterator();
            Iterator<GenTypeDeclTpar> j = secondTpars.iterator();
            while (i.hasNext()) {
                GenTypeDeclTpar firstTpar = i.next();
                GenTypeDeclTpar secondTpar = j.next();
                tparMap.put(secondTpar.getTparName(), firstTpar);
                if (!secondTpar.getUpperBound().mapTparsToTypes(tparMap).equals(firstTpar.getUpperBound())) {
                    return false;
                }
            }
            
            // Check argument types
            Iterator<JavaType> k = firstParamTypes.iterator();
            Iterator<JavaType> l = secondParamTypes.iterator();
            while (k.hasNext()) {
                if (!l.next().mapTparsToTypes(tparMap).equals(k.next())) {
                    return false;
                }
            }
            
            return true;
        }
        
        if (firstTpars.isEmpty() && secondTpars.isEmpty()) {
            // The parameter types might match exactly
            boolean doMatch = true;
            Iterator<JavaType> k = firstParamTypes.iterator();
            Iterator<JavaType> l = secondParamTypes.iterator();
            while (k.hasNext()) {
                if (!l.next().equals(k.next())) {
                    doMatch = false;
                    break;
                }
            }
            if (doMatch) {
                return true;
            }
        }
        
        if (firstTpars.isEmpty()) {
            // The first signature might be the erasure of the second
            boolean doMatch = true;
            Iterator<JavaType> k = firstParamTypes.iterator();
            Iterator<JavaType> l = secondParamTypes.iterator();
            while (k.hasNext()) {
                if (!l.next().getErasedType().equals(k.next())) {
                    doMatch = false;
                    break;
                }
            }
            if (doMatch) {
                return true;
            }
        }
        
        if (secondTpars.isEmpty()) {
            // The second signature might be the erasure of the first
            boolean doMatch = true;
            Iterator<JavaType> k = firstParamTypes.iterator();
            Iterator<JavaType> l = secondParamTypes.iterator();
            while (k.hasNext()) {
                if (!l.next().equals(k.next().getErasedType())) {
                    doMatch = false;
                    break;
                }
            }
            return doMatch;
        }
        
        return false;
    }

    /**
     * For a unary operator, check that the argument is valid,
     * then process the operator.
     */
    private void checkArg(JavaEntity arg1, Operator op)
    {
        JavaEntity rarg1 = arg1.resolveAsValue();
        if (rarg1 == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        doUnaryOp(rarg1, op);
    }
    
    /**
     * Do a unary plus operation (JLS 3rd ed. 15.15.3)
     */
    private void doUnaryPlus(ValueEntity arg)
    {
        JavaType argType = arg.getType();
        if (arg.hasConstantIntValue()) {
            long value = arg.getConstantIntValue();
            JavaType rtype = TextAnalyzer.unaryNumericPromotion(argType);
            valueStack.push(new ConstantIntValue(null, rtype, value));
        }
        else if (arg.hasConstantFloatValue()) {
            double value = arg.getConstantFloatValue();
            valueStack.push(new ConstantFloatValue(argType, value));
        }
        else {
            JavaType rtype = TextAnalyzer.unaryNumericPromotion(argType);
            if (rtype != null) {
                valueStack.push(new ValueEntity(rtype));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
        }
    }
    
    /**
     * Do a unary minus operation (JLS 3rd ed. 15.15.4)
     */
    private void doUnaryMinus(ValueEntity arg)
    {
        JavaType argType = arg.getType();
        if (arg.hasConstantIntValue()) {
            long value = -arg.getConstantIntValue();
            JavaType rtype = TextAnalyzer.unaryNumericPromotion(argType);
            valueStack.push(new ConstantIntValue(null, rtype, value));
        }
        else if (arg.hasConstantFloatValue()) {
            double value = -arg.getConstantFloatValue();
            valueStack.push(new ConstantFloatValue(argType, value));
        }
        else {
            JavaType rtype = TextAnalyzer.unaryNumericPromotion(argType);
            if (rtype != null) {
                valueStack.push(new ValueEntity(rtype));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
        }
    }
    
    /**
     * Process a unary operator.
     */
    private void doUnaryOp(JavaEntity arg, Operator op)
    {
        ValueEntity varg = arg.resolveAsValue();
        if (varg == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        JavaType argType = varg.getType();

        int ttype = op.getType();
        switch (ttype) {
        case JavaTokenTypes.LNOT:
            if (varg.hasConstantBooleanValue()) {
                boolean rval = ! varg.getConstantBooleanValue();
                valueStack.push(new ConstantBoolValue(rval));
            }
            else if (argType.typeIs(JavaType.JT_BOOLEAN) || argType.toString().equals("java.lang.Boolean")) {
                valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            break;
        case JavaTokenTypes.BNOT:
            if (varg.hasConstantIntValue()) {
                long rval = ~ varg.getConstantIntValue();
                JavaType rtype = TextAnalyzer.unaryNumericPromotion(argType);
                valueStack.push(new ConstantIntValue(null, rtype, rval));
            }
            else {
                JavaType argTypeUnboxed = TextAnalyzer.unBox(argType);
                if (argTypeUnboxed.isIntegralType()) {
                    valueStack.push(new ValueEntity(TextAnalyzer.unaryNumericPromotion(argTypeUnboxed)));
                }
                else {
                    valueStack.push(new ErrorEntity());
                }
            }
            break;
        case JavaTokenTypes.INC:
        case JavaTokenTypes.DEC:
            // TODO: check that the argument is a variable (an "lvalue")
            JavaType argTypeUnboxed = TextAnalyzer.unBox(argType);
            if (argTypeUnboxed.isIntegralType()) {
                // Note the value has the type of the variable, not the unboxed type
                valueStack.push(new ValueEntity(argType));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            break;
        case UNARY_PLUS_OP:
            doUnaryPlus(varg);
            break;
        case UNARY_MINUS_OP:
            doUnaryMinus(varg);
            break;
        }
    }
    
    /**
     * Cast an integer value to the given integer type, and return the resulting value.
     * The returned value will be within the range of values representable by the type. 
     */
    private long limitResult(JavaType type, long value)
    {
        if (type.typeIs(JavaType.JT_BYTE)) {
            return (byte) value;
        }
        else if (type.typeIs(JavaType.JT_CHAR)) {
            return (char) value;
        }
        else if (type.typeIs(JavaType.JT_SHORT)) {
            return (short) value;
        }
        else if (type.typeIs(JavaType.JT_INT)) {
            return (int) value;
        }
        return value;
    }
    
    /**
     * Promote an integer or float to a float, return the result
     */
    private float promoteToFloat(ValueEntity arg1)
    {
        if (arg1.hasConstantFloatValue()) {
            return (float) arg1.getConstantFloatValue();
        }
        else {
            return arg1.getConstantIntValue();
        }
    }
    
    /**
     * Promote an integer, float or double to a double, return the result.
     */
    private double promoteToDouble(ValueEntity arg1)
    {
        if (arg1.hasConstantFloatValue()) {
            return (float) arg1.getConstantFloatValue();
        }
        else {
            return arg1.getConstantIntValue();
        }
    }
    
    /**
     * Process the '+' operator
     */
    private void doOpPlus(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        JavaType a2type = arg2.getType();
        
        // either the first or second argument might be a String,
        // in which case the result will be a String also.
        GenTypeSolid a1solid = a1type.asSolid();
        GenTypeSolid a2solid = a2type.asSolid();
        GenTypeClass a1class = null;
        GenTypeClass a2class = null;
        
        // The JLS 3rd edition conflicts with actual compiler behaviour.
        // JLS says that String concatenation is only the case if the type of
        // either argument is String. However, if the type is a type parameter
        // with a bound type of String, the compiler still applies concatenation.
        if (a1solid != null) {
            GenTypeClass [] stypes = a1solid.getReferenceSupertypes();
            if (stypes.length > 0) {
                a1class = a1solid.getReferenceSupertypes()[0];
            }
        }
        if (a2solid != null) {
            GenTypeClass [] stypes = a2solid.getReferenceSupertypes();
            if (stypes.length > 0) {
                a2class = a2solid.getReferenceSupertypes()[0];
            }
        }
        
        if (a1class != null && a1class.toString().equals("java.lang.String")) {
            // TODO concatenation of constant string with a constant should yield a constant string
            valueStack.push(new ValueEntity(a1class));
            return;
        }
        if (a2class != null && a2class.toString().equals("java.lang.String")) {
            // TODO concatenation of constant string with a constant should yield a constant string
            valueStack.push(new ValueEntity(a2class));
            return;
        }
        
        doBnpOp(op, arg1, arg2);
    }
    
    /**
     * Process an operator which performs binary numeric promotion and which allows
     * the result to be a constant expression.
     */
    private strictfp void doBnpOp(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        JavaType a2type = arg2.getType();
        JavaType resultType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
        
        if (resultType == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        // Handle the case where the arguments are constant. We must do three cases
        // differently: integral types, "float" type (arguments promoted to "float")
        // and "double" type (arguments promoted to "double").
        if ((arg1.hasConstantIntValue() || arg1.hasConstantFloatValue())
                && (arg2.hasConstantIntValue() || arg2.hasConstantFloatValue())) {
            if (resultType.isIntegralType()) {
                long a1 = arg1.getConstantIntValue();
                long a2 = arg2.getConstantIntValue();
                long rval;
                switch (op.type) {
                case JavaTokenTypes.PLUS:
                    rval = a1 + a2; break;
                case JavaTokenTypes.MINUS:
                    rval = a1 - a2; break;
                case JavaTokenTypes.STAR:
                    rval = a1 * a2; break;
                case JavaTokenTypes.DIV:
                    if (a2 == 0) {
                        valueStack.push(new ValueEntity(resultType));
                        return;
                    }
                    rval = a1 / a2; break;
                case JavaTokenTypes.MOD:
                    if (a2 == 0) {
                        valueStack.push(new ValueEntity(resultType));
                        return;
                    }
                    rval = a1 % a2; break;
                case JavaTokenTypes.BAND:
                    rval = a1 & a2; break;
                case JavaTokenTypes.BOR:
                    rval = a1 | a2; break;
                case JavaTokenTypes.BXOR:
                    rval = a1 ^ a2; break;
                default:
                    rval = 0; break;
                }
                rval = limitResult(resultType, rval);
                valueStack.push(new ConstantIntValue(null, resultType, rval));
            }
            else if (resultType.typeIs(JavaType.JT_FLOAT)) {
                float a1, a2;
                a1 = promoteToFloat(arg1);
                a2 = promoteToFloat(arg2);
                float rval;
                switch (op.type) {
                case JavaTokenTypes.PLUS:
                    rval = a1 + a2; break;
                case JavaTokenTypes.MINUS:
                    rval = a1 - a2; break;
                case JavaTokenTypes.STAR:
                    rval = a1 * a2; break;
                case JavaTokenTypes.DIV:
                    rval = a1 / a2; break;
                case JavaTokenTypes.MOD:
                    rval = a1 % a2; break;
                case JavaTokenTypes.BAND:
                case JavaTokenTypes.BOR:
                case JavaTokenTypes.BXOR:
                default:
                    valueStack.push(new ErrorEntity());
                    return;
                }
                valueStack.push(new ConstantFloatValue(resultType, rval));
            }
            else {
                // Result type is double; one argument might still be integer.
                double a1 = promoteToDouble(arg1);
                double a2 = promoteToDouble(arg2);
                double rval;
                switch (op.type) {
                case JavaTokenTypes.PLUS:
                    rval = a1 + a2; break;
                case JavaTokenTypes.MINUS:
                    rval = a1 - a2; break;
                case JavaTokenTypes.STAR:
                    rval = a1 * a2; break;
                case JavaTokenTypes.DIV:
                    rval = a1 / a2; break;
                case JavaTokenTypes.MOD:
                    rval = a1 % a2; break;
                case JavaTokenTypes.BAND:
                case JavaTokenTypes.BOR:
                case JavaTokenTypes.BXOR:
                default:
                    valueStack.push(new ErrorEntity());
                    return;
                }
                valueStack.push(new ConstantFloatValue(resultType, rval));
            }
            return;
        }

        valueStack.push(new ValueEntity(null, resultType));
    }
    
    /**
     * Process equality operators '==' and '!='
     */
    private void doEqualityOp(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        JavaType a2type = arg2.getType();
        
        if (ValueEntity.isConstant(arg1) && ValueEntity.isConstant(arg2)) {
            if (a1type.isNumeric()) {
                if (! a2type.isNumeric()) {
                    valueStack.push(new ErrorEntity());
                    return;
                }

                JavaType promotedType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
                if (promotedType.isIntegralType()) {
                    long a1 = arg1.getConstantIntValue();
                    long a2 = arg2.getConstantIntValue();
                    boolean rval= (a1 == a2) ^ (op.type != JavaTokenTypes.EQUAL);
                    valueStack.push(new ConstantBoolValue(rval));
                }
                else if (promotedType.typeIs(JavaType.JT_FLOAT)) {
                    float a1 = promoteToFloat(arg1);
                    float a2 = promoteToFloat(arg2);
                    boolean rval= (a1 == a2) ^ (op.type != JavaTokenTypes.EQUAL);
                    valueStack.push(new ConstantBoolValue(rval));                    
                }
                else {
                    // JT_DOUBLE
                    double a1 = promoteToDouble(arg1);
                    double a2 = promoteToDouble(arg2);
                    boolean rval= (a1 == a2) ^ (op.type != JavaTokenTypes.EQUAL);
                    valueStack.push(new ConstantBoolValue(rval));                    
                }
                return;
            }
            
            // Constants but not numeric...
            if (arg1.hasConstantBooleanValue() && arg2.hasConstantBooleanValue()) {
                boolean a1 = arg1.getConstantBooleanValue();
                boolean a2 = arg2.getConstantBooleanValue();
                boolean rval = op.type == JavaTokenTypes.EQUAL ? a1 == a2 : a1 != a2;
                valueStack.push(new ConstantBoolValue(rval));
                return;
            }
            
            if (arg1.isConstantString() && arg2.isConstantString()) {
                String a1 = arg1.getConstantString();
                String a2 = arg2.getConstantString();
                boolean rval = (op.type != JavaTokenTypes.EQUAL) ^ a1.equals(a2);
                valueStack.push(new ConstantBoolValue(rval));
                return;
            }
            
            valueStack.push(new ErrorEntity());
        }
        
        if (a1type.isNumeric() || a2type.isNumeric()) {
            // Note we only perform binary numeric promotion if one of the arguments
            // is already (primitive) numeric, as per the JLS 3rd ed.
            JavaType promotedType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
            if (promotedType == null) {
                valueStack.push(new ErrorEntity());
            }
            else {
                valueStack.push(new ValueEntity("", JavaPrimitiveType.getBoolean()));
            }
        }
        else if (a1type.isNull() && a2type.isNull()
                || a1type.isNull() && a2type.asSolid() != null
                || a1type.asSolid() != null && a2type.isNull()) {
            // Null compared to itself, or to a reference type
            valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
        }
        else if (a1type.asSolid() != null && a2type.asSolid() != null) {
            // Reference comparison
            // TODO identify comparisons which are invalid due to divergent
            // inheritance hierarchies
            valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    /**
     * Process a relationship operator - '&lt;', '&gt;', '&lt;=', '&gt;='
     */
    private void doRelationshipOp(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        JavaType a2type = arg2.getType();
        
        JavaType promotedType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
        if (promotedType == null) {
            valueStack.push(new ErrorEntity());
        }
        else {
            if (ValueEntity.isConstant(arg1) && ValueEntity.isConstant(arg2)) {
                if (promotedType.isIntegralType()) {
                    long a1 = arg1.getConstantIntValue();
                    long a2 = arg2.getConstantIntValue();
                    boolean rval;
                    
                    switch (op.type) {
                    case JavaTokenTypes.LT:
                        rval = a1 < a2; break;
                    case JavaTokenTypes.LE:
                        rval = a1 <= a2; break;
                    case JavaTokenTypes.GT:
                        rval = a1 > a2; break;
                    default:
                    // case JavaTokenTypes.GE:
                        rval = a1 >= a2;
                    }
                    
                    valueStack.push(new ConstantBoolValue(rval));
                }
                else if (promotedType.typeIs(JavaType.JT_FLOAT)) {
                    float a1 = promoteToFloat(arg1);
                    float a2 = promoteToFloat(arg2);
                    boolean rval;
                    
                    switch (op.type) {
                    case JavaTokenTypes.LT:
                        rval = a1 < a2; break;
                    case JavaTokenTypes.LE:
                        rval = a1 <= a2; break;
                    case JavaTokenTypes.GT:
                        rval = a1 > a2; break;
                    default:
                    // case JavaTokenTypes.GE:
                        rval = a1 >= a2;
                    }
                    
                    valueStack.push(new ConstantBoolValue(rval));
                }
                else { // JT_DOUBLE
                    double a1 = promoteToDouble(arg1);
                    double a2 = promoteToDouble(arg2);
                    boolean rval;
                    
                    switch (op.type) {
                    case JavaTokenTypes.LT:
                        rval = a1 < a2; break;
                    case JavaTokenTypes.LE:
                        rval = a1 <= a2; break;
                    case JavaTokenTypes.GT:
                        rval = a1 > a2; break;
                    default:
                    // case JavaTokenTypes.GE:
                        rval = a1 >= a2;
                    }
                    
                    valueStack.push(new ConstantBoolValue(rval));
                }
                return;
            }
            
            valueStack.push(new ValueEntity("", JavaPrimitiveType.getBoolean()));
        }
    }
    
    /**
     * Do a bitwise operation - '&amp;', '|' or '^'
     */
    private void doBitwiseOp(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        if (a1type.typeIs(JavaType.JT_BOOLEAN)
                || a1type.toString().equals("java.lang.Boolean")) {
            JavaType a2type = arg2.getType();
            if (a2type.typeIs(JavaType.JT_BOOLEAN)
                    || a2type.toString().equals("java.lang.Boolean")) {
                // Both arguments are (convertible to) boolean
                if (arg1.hasConstantBooleanValue() && arg2.hasConstantBooleanValue()) {
                    boolean a1 = arg1.getConstantBooleanValue();
                    boolean a2 = arg2.getConstantBooleanValue();
                    boolean rval;
                    switch (op.type) {
                    case JavaTokenTypes.BAND:
                        rval = a1 & a2;
                        break;
                    case JavaTokenTypes.BOR:
                        rval = a1 | a2;
                        break;
                    default:
                        // JavaTokenTypes.BXOR:
                        rval = a1 ^ a2;
                    }
                    valueStack.push(new ConstantBoolValue(rval));
                }
                else {
                    valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
                }
                return;
            }
        }
        
        doBnpOp(op, arg1, arg2);
    }
    
    /**
     * Do a logical operation - '&amp;&amp;' or '||'
     * @param op
     * @param arg1
     * @param arg2
     */
    private void doLogicalOp(Operator op, ValueEntity arg1, ValueEntity arg2)
    {
        JavaType a1type = arg1.getType();
        if (a1type.typeIs(JavaType.JT_BOOLEAN)
                || a1type.toString().equals("java.lang.Boolean")) {
            JavaType a2type = arg2.getType();
            if (a2type.typeIs(JavaType.JT_BOOLEAN)
                    || a2type.toString().equals("java.lang.Boolean")) {
                // Both arguments are (convertible to) boolean
                if (arg1.hasConstantBooleanValue() && arg2.hasConstantBooleanValue()) {
                    boolean a1 = arg1.getConstantBooleanValue();
                    boolean a2 = arg2.getConstantBooleanValue();
                    boolean rval;
                    switch (op.type) {
                    case JavaTokenTypes.LAND:
                        rval = a1 && a2;
                    default:
                        // JavaTokenTypes.LOR:
                        rval = a1 || a2;
                    }
                    valueStack.push(new ConstantBoolValue(rval));
                }
                else {
                    valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
                }
                return;
            }
        }
        
        valueStack.push(new ErrorEntity());
    }
    
    /**
     * Process a binary operator. Arguments have been resolved as values.
     * The result is pushed back onto the value stack.
     */
    private void doBinaryOp(JavaEntity uarg1, JavaEntity uarg2, Operator op)
    {
        // Check that arguments resolve to values
        ValueEntity arg1 = uarg1.resolveAsValue();
        ValueEntity arg2 = uarg2.resolveAsValue();
        if (arg1 == null || arg2 == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        JavaType a1type = arg1.getType().getCapture();
        
        int ttype = op.getType();
        switch (ttype) {
        case JavaTokenTypes.PLUS:
            doOpPlus(op, arg1, arg2);
            return;
        case JavaTokenTypes.MINUS:
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
        case JavaTokenTypes.MOD:
            doBnpOp(op, arg1, arg2);
            break;
        case JavaTokenTypes.BAND:
        case JavaTokenTypes.BOR:
        case JavaTokenTypes.BXOR:
            doBitwiseOp(op, arg1, arg2);
            break;
        case JavaTokenTypes.LOR:
        case JavaTokenTypes.LAND:
            doLogicalOp(op, arg1, arg2);
            break;
        case JavaTokenTypes.SL:
        case JavaTokenTypes.SR:
        case JavaTokenTypes.BSR:
            JavaType a1typeP = TextAnalyzer.unaryNumericPromotion(a1type);
            JavaType a2typeP = TextAnalyzer.unaryNumericPromotion(a1type);
            if (a1typeP == null || a2typeP == null || !a1typeP.isIntegralType()
                    || ! a2typeP.isIntegralType()) {
                valueStack.push(new ErrorEntity());
            }
            else {
                // The result type is the type of the LHS
                // See JLS 3rd ed 15.19
                if (arg1.hasConstantIntValue() && arg2.hasConstantIntValue()) {
                    long a1 = arg1.getConstantIntValue();
                    long a2 = arg2.getConstantIntValue();
                    long rval;
                    if (ttype == JavaTokenTypes.SL) {
                        rval = a1 << a2;
                    }
                    else if (ttype == JavaTokenTypes.SR) {
                        rval = a1 >> a2;
                    }
                    else {
                        // ttype == JavaTokenTypes.BSR
                        rval = a1 >>> a2;
                    }
                    rval = limitResult(a1typeP, rval);
                    valueStack.push(new ConstantIntValue(null, a1typeP, rval));
                }
                else {
                    valueStack.push(new ValueEntity("", a1typeP));
                }
            }
            break;
        case JavaTokenTypes.LT:
        case JavaTokenTypes.LE:
        case JavaTokenTypes.GT:
        case JavaTokenTypes.GE:
            doRelationshipOp(op, arg1, arg2);
            break;
        case JavaTokenTypes.EQUAL:
        case JavaTokenTypes.NOT_EQUAL:
            doEqualityOp(op, arg1, arg2);
            break;
        case JavaTokenTypes.ASSIGN:
        case JavaTokenTypes.BAND_ASSIGN:
        case JavaTokenTypes.BOR_ASSIGN:
        case JavaTokenTypes.PLUS_ASSIGN:
        case JavaTokenTypes.MINUS_ASSIGN:
        case JavaTokenTypes.STAR_ASSIGN:
        case JavaTokenTypes.DIV_ASSIGN:
        case JavaTokenTypes.SL_ASSIGN:
        case JavaTokenTypes.SR_ASSIGN:
        case JavaTokenTypes.BSR_ASSIGN:
        case JavaTokenTypes.MOD_ASSIGN:
        case JavaTokenTypes.BXOR_ASSIGN:
            // TODO check valid assignment
            valueStack.push(arg1);
            break;
        case JavaTokenTypes.DOT:
            // This is handled elsewhere
            valueStack.push(new ErrorEntity());
        default:
        }
    }
    
    private void processQuestionOperator()
    {
        // JLS 15.25
        JavaEntity rhs = popValueStack();
        JavaEntity lhs = popValueStack();
        JavaEntity condition = popValueStack();
        
        ValueEntity conditionv = condition.resolveAsValue();
        if (conditionv == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        JavaType ctype = conditionv.getType();
        if (!ctype.typeIs(JavaType.JT_BOOLEAN) && !ctype.toString().equals("java.lang.Boolean")) {
            // Condition is not a boolean
            valueStack.push(new ErrorEntity());
            return;
        }
        
        ValueEntity rhsv = rhs.resolveAsValue();
        ValueEntity lhsv = lhs.resolveAsValue();
        if (rhsv == null || lhsv == null) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        ValueEntity rent = TextAnalyzer.questionOperator15(conditionv, lhsv, rhsv);
        valueStack.push(rent);
    }
    
    @Override
    protected void beginExpression(LocatableToken token)
    {
        operatorStack.push(new Operator(PAREN_OPERATOR, token));
    }
    
    @Override
    protected void endExpression(LocatableToken token, boolean isEmpty)
    {
        // An "empty" expression hasn't yet generated a value:
        if (isEmpty) {
            valueStack.push(new ErrorEntity());
        }
        
        // This should leave the expression value on top of the value stack:
        processHigherPrecedence(getPrecedence(PAREN_OPERATOR));
        operatorStack.pop(); // pop expression beginning operator
        
        if (!operatorStack.isEmpty()) {
            if (operatorStack.peek().type == JavaTokenTypes.LCURLY) {
                // The value generated by this expression can be discarded:
                popValueStack();
            }
        }
    }
    
    private void gotStringLiteral(LocatableToken token)
    {
        String ctext = token.getText();
        StringBuffer sb = new StringBuffer(ctext.length());
        for (int pos = 1; pos < ctext.length(); pos++) {
            char c = ctext.charAt(pos); // just after "'"
            if (c == '\"') {
                break; // end of string
            }
            if (c == '\\') {
                c = ctext.charAt(2);
                if (c == 'b') {
                    c = '\b';
                }
                else if (c == 't') {
                    c = '\t';
                }
                else if (c == 'n') {
                    c = '\n';
                }
                else if (c == 'f') {
                    c = '\f';
                }
                else if (c == 'r') {
                    c = '\r';
                }
                else if (c == '\'') {
                    c = '\'';
                }
                else if (c == '\"') {
                    c = '\"';
                }
                else if (c == '\\') {
                    c = '\\';
                }
                else if (c >= '0' && c <= '7') {
                    // Octal escape
                    int val = c - '0';
                    while (++pos < ctext.length()) {
                        char d = ctext.charAt(pos);
                        if (d == '\'') break;
                        if (d < '0' || d > '7') {
                            valueStack.push(new ErrorEntity());
                            return;
                        }
                        val = val * 8 + (d - '0');
                        if (val > 0377) {
                            valueStack.push(new ErrorEntity());
                            return;
                        }
                    }
                    c = (char) val;
                }
                else {
                    valueStack.push(new ErrorEntity());
                    return;
                }
            }
            sb.append(c);
        }
        JavaType stringType = new GenTypeClass(new JavaReflective(String.class));
        ValueEntity ent = new ConstantStringEntity(stringType, sb.toString());
        valueStack.push(ent);
    }
    
    @Override
    protected void gotLiteral(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.CHAR_LITERAL) {
            String ctext = token.getText();
            char c = ctext.charAt(1); // just after "'"
            if (c == '\\') {
                c = ctext.charAt(2);
                if (c == 'b') {
                    c = '\b';
                }
                else if (c == 't') {
                    c = '\t';
                }
                else if (c == 'n') {
                    c = '\n';
                }
                else if (c == 'f') {
                    c = '\f';
                }
                else if (c == 'r') {
                    c = '\r';
                }
                else if (c == '\'') {
                    c = '\'';
                }
                else if (c == '\"') {
                    c = '\"';
                }
                else if (c == '\\') {
                    c = '\\';
                }
                else if (c >= '0' && c <= '7') {
                    // Octal escape
                    int val = c - '0';
                    int pos = 3;
                    while (pos < ctext.length()) {
                        char d = ctext.charAt(pos++);
                        if (d == '\'') break;
                        if (d < '0' || d > '7') {
                            valueStack.push(new ErrorEntity());
                            return;
                        }
                        val = val * 8 + (d - '0');
                        if (val > 0377) {
                            valueStack.push(new ErrorEntity());
                            return;
                        }
                    }
                    c = (char) val;
                }
                else {
                    valueStack.push(new ErrorEntity());
                    return;
                }
            }
            valueStack.push(new ConstantIntValue(null, JavaPrimitiveType.getChar(), c));
        }
        else if (token.getType() == JavaTokenTypes.NUM_INT) {
            try {
                valueStack.push(new ConstantIntValue(null, JavaPrimitiveType.getInt(), Integer.decode(token.getText())));
            }
            catch (NumberFormatException nfe) {
                valueStack.push(new ErrorEntity());
            }
        }
        else if (token.getType() == JavaTokenTypes.NUM_LONG) {
            try {
                String text = token.getText();
                text = text.substring(0, text.length() - 1); // remove 'l' or 'L' suffix
                valueStack.push(new ConstantIntValue(null, JavaPrimitiveType.getLong(), Long.decode(text)));
            }
            catch (NumberFormatException nfe) {
                valueStack.push(new ErrorEntity());
            }
        }
        else if (token.getType() == JavaTokenTypes.NUM_FLOAT) {
            try {
                valueStack.push(new ConstantFloatValue(JavaPrimitiveType.getFloat(), Float.parseFloat(token.getText())));
            }
            catch (NumberFormatException nfe) {
                valueStack.push(new ErrorEntity());
            }
        }
        else if (token.getType() == JavaTokenTypes.NUM_DOUBLE) {
            try {
                valueStack.push(new ConstantFloatValue(JavaPrimitiveType.getDouble(), Double.parseDouble(token.getText())));
            }
            catch (NumberFormatException nfe) {
                valueStack.push(new ErrorEntity());
            }
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_null) {
            valueStack.push(new NullEntity());
        }
        else if (token.getType() == JavaTokenTypes.STRING_LITERAL) {
            gotStringLiteral(token);
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_true
                || token.getType() == JavaTokenTypes.LITERAL_false) {
            valueStack.push(new ConstantBoolValue(token.getType() == JavaTokenTypes.LITERAL_true));
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_this) {
            if (staticAccess) {
                valueStack.push(new ErrorEntity());
            }
            else {
                JavaType type = accessType.getType();
                if (type != null) {
                    valueStack.push(new ValueEntity(type));
                }
                else {
                    valueStack.push(new ErrorEntity());
                }
            }
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_super) 
        {
            for (JavaType type : accessType.getType().asClass().getReflective().getSuperTypes()) 
            {
                if (type != null) 
                {
                    valueStack.push(new ValueEntity(type));
                } 
                else 
                {
                    valueStack.push(new ErrorEntity());
                }
            }
        }   
        else 
        {
            valueStack.push(new ErrorEntity());
        }
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token)
    {
        String ident = token.getText();
        Reflective accessSource = getAccessSource();
        valueStack.push(UnresolvedEntity.getEntity(resolver, ident, accessSource));
        arrayCount = 0;
    }
    
    /**
     * Get the access type as a reflective.
     */
    private Reflective getAccessSource()
    {
        if (accessType != null) {
            GenTypeClass accessClass = accessType.getType().asClass();
            if (accessClass != null) {
                return accessClass.getReflective();
            }
        }
        return null;
    }
    
    @Override
    protected void gotMemberAccess(LocatableToken token)
    {
        String ident = token.getText();
        JavaEntity top = valueStack.pop();
        
        // handle array "length" member
        if (token.getText().equals("length")) {
            JavaEntity topVal = top.resolveAsValue();
            if (topVal != null) {
                if (topVal.getType().getArrayComponent() != null) {
                    // This is an array
                    valueStack.push(new ValueEntity(JavaPrimitiveType.getInt()));
                    return;
                }
            }
        }
        
        JavaEntity newTop = top.getSubentity(ident, getAccessSource());
        if (newTop != null) {
            valueStack.push(newTop);
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    @Override
    protected void gotMemberCall(LocatableToken token, List<LocatableToken> typeArgs)
    {
        operatorStack.push(new Operator(MEMBER_CALL_OP, token));
        typeArgStack.push(typeArgs);
    }
    
    @Override
    protected void gotMethodCall(LocatableToken token)
    {
        operatorStack.push(new Operator(METHOD_CALL_OP, token));
    }
    
    @Override
    protected void gotConstructorCall(LocatableToken token)
    {
        operatorStack.push(new Operator(METHOD_CALL_OP, token));
    }
    
    @Override
    protected void gotUnaryOperator(LocatableToken token)
    {
        int ttype = token.getType();
        if (ttype == JavaTokenTypes.PLUS) {
            ttype = UNARY_PLUS_OP;
        }
        else if (ttype == JavaTokenTypes.MINUS) {
            ttype = UNARY_MINUS_OP;
        }
        processHigherPrecedence(getPrecedence(ttype)); // right associative
        operatorStack.push(new Operator(ttype, token));
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token)
    {
        processHigherPrecedence(getPrecedence(token.getType()) - 1);
        operatorStack.push(new Operator(token.getType(), token));
    }
    
    @Override
    protected void gotInstanceOfOperator(LocatableToken token)
    {
        processHigherPrecedence(getPrecedence(token.getType()) - 1);
        operatorStack.push(new Operator(token.getType(), token));
        state = STATE_INSTANCEOF;
    }
    
    @Override
    protected void gotQuestionOperator(LocatableToken token)
    {
        processHigherPrecedence(getPrecedence(token.getType()) - 1);
        operatorStack.push(new Operator(token.getType(), token));
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        if (state == STATE_NEW) {
            JavaEntity entity = resolveTypeSpec(tokens);
            state = STATE_NEW_ARGS;
            
            if (entity != null) {
                valueStack.push(new ValueEntity(entity.getType()));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
        }
        else if (state == STATE_INSTANCEOF) {
            TypeEntity entity = resolveTypeSpec(tokens);
            if (entity != null) {
                // TODO: check validity of instanceof check
                popValueStack();
                valueStack.push(new ValueEntity(JavaPrimitiveType.getBoolean()));
            }
            else {
                popValueStack();
                valueStack.push(new ErrorEntity());
            }
        }
    }
    
    @Override
    protected void gotTypeCast(List<LocatableToken> tokens)
    {
        JavaEntity entity = resolveTypeSpec(tokens);
        
        if (entity != null) {
            valueStack.push(entity);
            operatorStack.push(new Operator(CAST_OPERATOR, null));
        }
        else {
            operatorStack.push(new Operator(BAD_CAST_OPERATOR, null));
        }
    }
    
    @Override
    protected void gotArrayDeclarator()
    {
        arrayCount++;
    }
    
    @Override
    protected void gotNewArrayDeclarator(boolean withDimension)
    {
        if (state != STATE_NONE) {
            state = STATE_NONE; // Don't expect constructor arguments!
            operatorStack.pop(); // remove new operator from stack
        }
        
        if (withDimension) {
            valueStack.pop(); // pull the dimension off the value stack
        }
        
        JavaEntity top = valueStack.pop();
        JavaEntity ttop = top.resolveAsValue();
        if (ttop != null) {
            valueStack.push(new ValueEntity(ttop.getType().getArray()));
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    @Override
    protected void gotPrimitiveTypeLiteral(LocatableToken token)
    {
        List<LocatableToken> ltokens = new ArrayList<LocatableToken>(1);
        ltokens.add(token);
        TypeEntity tent = resolveTypeSpec(ltokens);
        valueStack.push(tent);
        arrayCount = 0;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    protected void gotClassLiteral(LocatableToken token)
    {
        JavaEntity ent = popValueStack();
        TypeEntity tent = ent.resolveAsType();
        if (tent != null) {
            JavaType ttype = tent.getType();
            if (arrayCount > 0) {
                while (arrayCount-- > 0) {
                    ttype = ttype.getArray();
                }
                tent = new TypeEntity(ttype);
            }
            if (! ttype.isPrimitive()) {
                TypeEntity jlcEnt = resolver.resolveQualifiedClass("java.lang.Class");
                if (jlcEnt != null) {
                    List<TypeArgumentEntity> targs = new ArrayList<TypeArgumentEntity>(1);
                    targs.add(new SolidTargEntity(tent));
                    jlcEnt = jlcEnt.setTypeArgs(targs);
                    if (jlcEnt != null) {
                        valueStack.push(new ValueEntity(jlcEnt.getType()));
                        return;
                    }
                }
            }
            else {
                String repClass = null;
                if (ttype.typeIs(JavaType.JT_BOOLEAN)) {
                    repClass = "java.lang.Boolean";
                }
                else if (ttype.typeIs(JavaType.JT_BYTE)) {
                    repClass = "java.lang.Byte";
                }
                else if (ttype.typeIs(JavaType.JT_CHAR)) {
                    repClass = "java.lang.Char";
                }
                else if (ttype.typeIs(JavaType.JT_DOUBLE)) {
                    repClass = "java.lang.Double";
                }
                else if (ttype.typeIs(JavaType.JT_FLOAT)) {
                    repClass = "java.lang.Float";
                }
                else if (ttype.typeIs(JavaType.JT_INT)) {
                    repClass = "java.lang.Integer";
                }
                else if (ttype.typeIs(JavaType.JT_LONG)) {
                    repClass = "java.lang.Long";
                }
                else if (ttype.typeIs(JavaType.JT_VOID)) {
                    repClass = "java.lang.Void";
                }
                if (repClass != null) {
                    TypeEntity jlcEnt = resolver.resolveQualifiedClass("java.lang.Class");
                    TypeEntity repEnt = resolver.resolveQualifiedClass(repClass);
                    if (jlcEnt != null && repEnt != null) {
                        List<TypeArgumentEntity> targs = new ArrayList<TypeArgumentEntity>(1);
                        targs.add(new SolidTargEntity(repEnt));
                        jlcEnt = jlcEnt.setTypeArgs(targs);
                        if (jlcEnt != null) {
                            valueStack.push(new ValueEntity(jlcEnt.getType()));
                            return;
                        }
                    }
                }
            }
        }

        valueStack.push(new ErrorEntity());
    }
    
    private class DepthRef
    {
        int depth = 0;
    }
    
    /**
     * Resolve a type specification. Returns null if the type couldn't be resolved.
     */
    private TypeEntity resolveTypeSpec(List<LocatableToken> tokens)
    {
        DepthRef dr = new DepthRef();
        return resolveTypeSpec(tokens.listIterator(), dr);
    }
    
    /**
     * Resolve a type specification. Returns null if the type couldn't be resolved.
     */
    @OnThread(Tag.FXPlatform)
    private TypeEntity resolveTypeSpec(ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        LocatableToken token = i.next();
        
        if (isPrimitiveType(token)) {
            if (token.getType() == JavaTokenTypes.LITERAL_void) {
                return new TypeEntity(JavaPrimitiveType.getVoid());
            }
            
            JavaType type = null;
            switch (token.getType()) {
            case JavaTokenTypes.LITERAL_int:
                type = JavaPrimitiveType.getInt();
                break;
            case JavaTokenTypes.LITERAL_short:
                type = JavaPrimitiveType.getShort();
                break;
            case JavaTokenTypes.LITERAL_char:
                type = JavaPrimitiveType.getChar();
                break;
            case JavaTokenTypes.LITERAL_byte:
                type = JavaPrimitiveType.getByte();
                break;
            case JavaTokenTypes.LITERAL_boolean:
                type = JavaPrimitiveType.getBoolean();
                break;
            case JavaTokenTypes.LITERAL_long:
                type = JavaPrimitiveType.getLong();
                break;
            case JavaTokenTypes.LITERAL_double:
                type = JavaPrimitiveType.getDouble();
                break;
            case JavaTokenTypes.LITERAL_float:
                type = JavaPrimitiveType.getFloat();
            }
            
            while (i.hasNext()) {
                token = i.next();
                if (token.getType() == JavaTokenTypes.LBRACK) {
                    type = type.getArray();
                    i.next();  // RBRACK
                }
                else {
                    return null;
                }
            }
            
            return new TypeEntity(type);
        }

        String text = token.getText();
        PackageOrClass poc = resolver.resolvePackageOrClass(text, getAccessSource());
        while (poc != null && i.hasNext()) {
            token = i.next();
            if (token.getType() == JavaTokenTypes.LT) {
                // Type arguments
                TypeEntity classEnt = poc.resolveAsType();
                if (classEnt != null) {
                    classEnt = processTypeArgs(classEnt, i, depthRef);
                }
                poc = classEnt;
                if (poc == null) {
                    return null;
                }
                if (! i.hasNext()) {
                    return classEnt;
                }
                token = i.next();
            }
            if (token.getType() != JavaTokenTypes.DOT) {
                poc = poc.resolveAsType();
                if (poc == null) {
                    return null;
                }
                
                while (token.getType() == JavaTokenTypes.LBRACK) {
                    poc = new TypeEntity(poc.getType().getArray());
                    if (i.hasNext()) {
                        token = i.next(); // RBRACK
                    }
                    if (! i.hasNext()) {
                        return poc.resolveAsType();
                    }
                    token = i.next();
                }
                
                i.previous(); // allow token to be re-read by caller
                return poc.resolveAsType();
            }
            token = i.next();            
            if (token.getType() != JavaTokenTypes.IDENT) {
                break;
            }
            poc = poc.getPackageOrClassMember(token.getText());
        }
                
        if (poc != null) {
            return poc.resolveAsType();
        }
        else {
            return null;
        }
    }
    
    /**
     * Process tokens as type arguments
     * @param base  The base type, i.e. the type to which the arguments are applied
     * @param i     A ListIterator to iterate through the tokens
     * @param depthRef  The argument depth
     * @return   A ClassEntity representing the type with type arguments applied (or null)
     */
    private TypeEntity processTypeArgs(TypeEntity base, ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        List<TypeArgumentEntity> taList = readTypeArguments(i, depthRef);
        if (taList == null) {
            return null;
        }
        
        // TODO check the type arguments are actually valid
        return base.setTypeArgs(taList);
    }
    
    /**
     * Read a list of type arguments (the opening angle-bracket has already been read) from a list of tokens.
     * Returns null if there is an error.
     */
    private List<TypeArgumentEntity> readTypeArguments(ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        int startDepth = depthRef.depth;
        List<TypeArgumentEntity> taList = new LinkedList<TypeArgumentEntity>();
        depthRef.depth++; // initial '<' already skipped
        
        mainLoop:
        while (i.hasNext() && depthRef.depth > startDepth) {
            LocatableToken token = i.next();
            if (token.getType() == JavaTokenTypes.QUESTION) {
                if (! i.hasNext()) {
                    return null;
                }
                token = i.next();
                if (token.getType() == JavaTokenTypes.LITERAL_super) {
                    TypeEntity taEnt = resolveTypeSpec(i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    taList.add(new WildcardSuperEntity(taEnt));
                }
                else if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                    TypeEntity taEnt = resolveTypeSpec(i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    taList.add(new WildcardExtendsEntity(taEnt));
                }
                else {
                    taList.add(new UnboundedWildcardEntity(resolver));
                    i.previous();
                }
            }
            else {
                i.previous();
                TypeEntity taEnt = resolveTypeSpec(i, depthRef);
                if (taEnt == null) {
                    return null;
                }
                JavaType taType = taEnt.getType();
                if (taType.isPrimitive()) {
                    return null;
                }
                taList.add(new SolidTargEntity(new TypeEntity(taType)));
            }

            if (! i.hasNext()) {
                return null;
            }
            token = i.next();
            int ttype = token.getType();
            while (ttype == JavaTokenTypes.GT || ttype == JavaTokenTypes.SR || ttype == JavaTokenTypes.BSR) {
                switch (ttype) {
                case JavaTokenTypes.BSR:
                    depthRef.depth--;
                case JavaTokenTypes.SR:
                    depthRef.depth--;
                default:
                    depthRef.depth--;
                }
                if (! i.hasNext()) {
                    break mainLoop;
                }
                token = i.next();
                ttype = token.getType();
            }

            if (ttype != JavaTokenTypes.COMMA) {
                i.previous();
                break;
            }
        }
        
        return taList;
    }
    
    @Override
    protected void beginArgumentList(LocatableToken token)
    {
        state = STATE_NONE;
        argumentStack.push(new ArrayList<JavaEntity>());
    }
    
    @Override
    protected void endArgument()
    {
        // Each argument is an expression delimited by beginExpression()/
        // endExpression(), so it should leave just a single value on the
        // stack.
        if (! valueStack.isEmpty()) {
            argumentStack.peek().add(valueStack.pop());
        }
    }
    
    @Override
    protected void endArgumentList(LocatableToken token)
    {
        if (! operatorStack.isEmpty()) {
            Operator top = operatorStack.pop();
            if (top.getType() == JavaTokenTypes.LITERAL_new) {
                processNewOperator(top);
            }
            else if (top.getType() == MEMBER_CALL_OP) {
                processMemberCall(top);
            }
            else if (top.getType() == METHOD_CALL_OP) {
                processMethodCall(top);
            }
            else {
                // ??!!
                argumentStack.pop();
            }
        }
    }
    
    @Override
    protected void gotExprNew(LocatableToken token)
    {
        state = STATE_NEW;
        operatorStack.push(new Operator(token.getType(), token));
    }
    
    @Override
    protected void endExprNew(LocatableToken token, boolean included)
    {
        if (state != STATE_NONE) {
            if (state == STATE_NEW_ARGS) {
                // We got a type spec: remove it
                popValueStack();
            }
            operatorStack.pop(); // Remove "new" operator from stack
            valueStack.push(new ErrorEntity());
            state = STATE_NONE;
        }
    }
    
    @Override
    protected void beginAnonClassBody(LocatableToken token, boolean isEnumMember)
    {
        operatorStack.push(new Operator(token.getType(), token));
    }
    
    @Override
    protected void beginArrayInitList(LocatableToken token)
    {
        operatorStack.push(new Operator(token.getType(), token));
    }
}
