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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.parser.TextAnalyzer.MethodCallDesc;
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

/**
 * A parser for the codepad.
 * 
 * @author Davin McCall
 */
public class TextParser extends JavaParser
{
    private EntityResolver resolver;
    
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
    
    private static final int CAST_OPERATOR = JavaTokenTypes.INVALID + 1;
    private static final int BAD_CAST_OPERATOR = CAST_OPERATOR + 1;
    private static final int PAREN_OPERATOR = BAD_CAST_OPERATOR + 1;
    private static final int MEMBER_CALL_OP = PAREN_OPERATOR + 1;
    
    private static final int STATE_NONE = 0;
    private static final int STATE_NEW = 1;  // just saw "new"
    private static final int STATE_NEW_ARGS = 2;  // expecting "new" arguments or array dimensions
    
    private int state = STATE_NONE;

    // Arguments for a method or constructor call are added to the list at the top of this stack
    private Stack<List<JavaEntity>> argumentStack = new Stack<List<JavaEntity>>();
    
    public TextParser(EntityResolver resolver, Reader r)
    {
        super(r);
        this.resolver = resolver;
    }
    
    public TextParser(EntityResolver resolver, Reader r, int line, int col)
    {
        super(r, line, col);
        this.resolver = resolver;
    }
    
    public TextParser(EntityResolver resolver, String s)
    {
        this(resolver, new StringReader(s));
    }
    
    public boolean atEnd()
    {
        return tokenStream.LA(1).getType() == JavaTokenTypes.EOF;
    }
    
    public JavaEntity getExpressionType()
    {
        processHigherPrecedence(getPrecedence(JavaTokenTypes.EOF));
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
        case PAREN_OPERATOR:
            return -1;
        case JavaTokenTypes.EQUAL:
        case JavaTokenTypes.NOT_EQUAL:
            return 8;
        case JavaTokenTypes.LT:
        case JavaTokenTypes.LE:
        case JavaTokenTypes.GT:
        case JavaTokenTypes.GE:
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
        case JavaTokenTypes.DOT:
            return 25;
        case CAST_OPERATOR:
        case BAD_CAST_OPERATOR:
            return 50;
        case MEMBER_CALL_OP:
            return 100;
        default:
        }
        
        return -1;
    }
    
    // Process all on-stack operators with a equal-or-higher precedence than that given
    private void processHigherPrecedence(int precedence)
    {
        while (! operatorStack.isEmpty()) {
            Operator top = operatorStack.peek();
            if (getPrecedence(top.getType()) < precedence) {
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
    private void processOperator(Operator token)
    {
        int tokenType = token.getType();
        
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
            arg2 = popValueStack();
            arg1 = popValueStack();
            checkArgs(arg1, arg2, token);
            break;
        case CAST_OPERATOR:
            popValueStack(); // remove the value being cast, leave the cast-to type.
            JavaEntity castType = popValueStack();
            castType = castType.resolveAsType();
            if (castType != null) {
                // TODO check operand can be cast to this type
                valueStack.push(new ValueEntity(castType.getType().getCapture()));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            break;
        case BAD_CAST_OPERATOR:
            popValueStack(); // remove the value being cast
            valueStack.push(new ErrorEntity());
        }
        // TODO
    }
    
    private void processNewOperator(Operator token)
    {
        if (! argumentStack.isEmpty()) {
            argumentStack.pop(); // constructor arguments
            // TODO check argument validity
            // Don't pop the type off the stack: we would just have to push it back anyway
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
        JavaEntity target = popValueStack();
        JavaEntity vtarget = target.resolveAsValue();
        GenTypeClass [] bounds;
        if (vtarget != null) {
            GenTypeSolid stype = vtarget.getType().asSolid();
            if (stype == null) {
                // call on a primitive
                valueStack.push(new ErrorEntity());
                return;
            }
            bounds = stype.getRealTypes();
        }
        else {
            // entity may be a type rather than a value
            target = target.resolveAsType();
            if (target == null) {
                valueStack.push(new ErrorEntity());
                return;
            }
            GenTypeSolid stype = target.getType().asSolid();
            if (stype == null) {
                // call on a primitive
                valueStack.push(new ErrorEntity());
                return;
            }
            bounds = stype.getRealTypes();
        }
            
        List<JavaEntity> argList = argumentStack.pop();
        JavaType [] argTypes = new JavaType[argList.size()];
        for (int i = 0; i < argTypes.length; i++) {
            JavaEntity cent = argList.get(i).resolveAsValue();
            if (cent == null) {
                valueStack.push(new ErrorEntity());
                return;
            }
            argTypes[i] = cent.getType().getCapture();
        }

        List<GenTypeClass> typeArgs = Collections.emptyList(); // TODO!

        ArrayList<MethodCallDesc> suitable = TextAnalyzer.getSuitableMethods(op.getToken().getText(), bounds, argTypes, typeArgs);
        // DAV fix
        // assume for now all candidates have override-equivalent signatures
        if (suitable.size() == 0) {
            valueStack.push(new ErrorEntity());
            return;
        }
        
        valueStack.push(new ValueEntity(suitable.get(0).retType));
    }
    
    private void checkArgs(JavaEntity arg1, JavaEntity arg2, Operator op)
    {
        JavaEntity rarg1 = arg1.resolveAsValue();
        JavaEntity rarg2 = arg2.resolveAsValue();
        if (rarg1 == null || rarg2 == null) {
            valueStack.push(rarg1 == null ? arg1 : arg2);
            return;
        }
        
        doBinaryOp(arg1, arg2, op);
    }
    
    /**
     * Process a binary operator. Arguments have been resolved as values. Result is left of stack.
     */
    private void doBinaryOp(JavaEntity arg1, JavaEntity arg2, Operator op)
    {
        JavaType a1type = arg1.getType().getCapture();
        JavaType a2type = arg2.getType().getCapture();
        
        int ttype = op.getType();
        switch (ttype) {
        // TODO and remember unboxing conversion
        case JavaTokenTypes.PLUS:
            if (! a1type.isNumeric() && !(TextAnalyzer.unBox(a1type).isNumeric())) {
                if (a1type.asClass().getReflective().getName().equals("java.lang.String")) {
                    valueStack.push(new ValueEntity(a1type));
                    return;
                }
                valueStack.push(new ErrorEntity());
                return;
            }
        case JavaTokenTypes.MINUS:
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
        case JavaTokenTypes.MOD:
            JavaType resultType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
            if (resultType == null) {
                valueStack.push(new ErrorEntity());
            }
            else {
                valueStack.push(new ValueEntity("", resultType));
            }
            break;
        case JavaTokenTypes.SL:
        case JavaTokenTypes.SR:
        case JavaTokenTypes.BSR:
            JavaType a1typeP = TextAnalyzer.unaryNumericPromotion(a1type);
            JavaType a2typeP = TextAnalyzer.unaryNumericPromotion(a1type);
            if (a1typeP == null || a2typeP == null) {
                valueStack.push(new ErrorEntity());
            }
            else {
                // The result is the type of the LHS
                // (see http://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.19) 
                valueStack.push(new ValueEntity("", a1typeP));
            }
            break;
        case JavaTokenTypes.LT:
        case JavaTokenTypes.LE:
        case JavaTokenTypes.GT:
        case JavaTokenTypes.GE:
            {
                JavaType promotedType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
                if (promotedType == null) {
                    valueStack.push(new ErrorEntity());
                }
                else {
                    valueStack.push(new ValueEntity("", JavaPrimitiveType.getBoolean()));
                }
            }
            break;
        case JavaTokenTypes.EQUAL:
        case JavaTokenTypes.NOT_EQUAL:
            if (a1type.isNumeric() || a2type.isNumeric()) {
                JavaType promotedType = TextAnalyzer.binaryNumericPromotion(a1type, a2type);
                if (promotedType == null) {
                    valueStack.push(new ErrorEntity());
                }
                else {
                    valueStack.push(new ValueEntity("", JavaPrimitiveType.getBoolean()));
                }
            }
            //TODO other cases (check same type hierarchy, or null-ness)
            break;
        case JavaTokenTypes.DOT:
            // This is handled elsewhere
            valueStack.push(new ErrorEntity());
        default:
        }
    }
    
    @Override
    protected void beginExpression(LocatableToken token)
    {
        operatorStack.push(new Operator(PAREN_OPERATOR, token));
    }
    
    @Override
    protected void endExpression(LocatableToken token)
    {
        processHigherPrecedence(getPrecedence(PAREN_OPERATOR) + 1);
        operatorStack.pop();
    }
    
    @Override
    protected void gotLiteral(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.CHAR_LITERAL) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getChar()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_INT) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getInt()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_LONG) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getLong()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_FLOAT) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getFloat()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_DOUBLE) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getDouble()));
        }
        else if (token.getType() == JavaTokenTypes.LITERAL_null) {
            valueStack.push(new NullEntity());
        }
        else if (token.getType() == JavaTokenTypes.STRING_LITERAL) {
            ValueEntity ent = new ValueEntity(new GenTypeClass(new JavaReflective(String.class)));
            valueStack.push(ent);
        }
        else {
            // LITERAL_this or LITERAL_super
            valueStack.push(new ErrorEntity());
        }
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token)
    {
        String ident = token.getText();
        valueStack.push(UnresolvedEntity.getEntity(resolver, ident, null)); // DAV access source
        arrayCount = 0;
    }
    
    @Override
    protected void gotMemberAccess(LocatableToken token)
    {
        String ident = token.getText();
        JavaEntity top = valueStack.pop();
        JavaEntity newTop = top.getSubentity(ident, null); // DAV access source
        if (newTop != null) {
            valueStack.push(newTop);
        }
        else {
            valueStack.push(new ErrorEntity());
        }
    }
    
    @Override
    protected void gotMemberCall(LocatableToken token)
    {
        //valueStack.push(UnresolvedEntity.getEntity(resolver, token.getText(), ""));
        operatorStack.push(new Operator(MEMBER_CALL_OP, token));
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token)
    {
        processHigherPrecedence(getPrecedence(token.getType()));
        operatorStack.push(new Operator(token.getType(), token));
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        if (state == STATE_NEW) {
            JavaEntity entity = resolveTypeSpec(tokens);
            
            if (entity != null) {
                valueStack.push(new ValueEntity(entity.getType().getCapture()));
                state = STATE_NEW_ARGS;
            }
            else {
                state = STATE_NONE;
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
    protected void gotPrimitiveTypeLiteral(LocatableToken token)
    {
        List<LocatableToken> ltokens = new ArrayList<LocatableToken>(1);
        ltokens.add(token);
        TypeEntity tent = resolveTypeSpec(ltokens);
        valueStack.push(tent);
        arrayCount = 0;
    }
    
    @Override
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
            GenTypeClass ctype = ttype.asClass();
            if (ctype != null) {
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
        // DAV use correct request source:
        PackageOrClass poc = resolver.resolvePackageOrClass(text, null);
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
                    poc = new TypeEntity(poc.getType().getCapture().getArray());
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
        int startDepth = depthRef.depth;
        List<TypeArgumentEntity> taList = new LinkedList<TypeArgumentEntity>();
        depthRef.depth++;
        
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
                    taList.add(new UnboundedWildcardEntity());
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
        // TODO check the type arguments are actually valid
        return base.setTypeArgs(taList);
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
        // processHigherPrecedence(getPrecedence(JavaTokenTypes.COMMA));
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

}
