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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import bluej.debugger.gentype.GenTypeExtends;
import bluej.debugger.gentype.GenTypeParameterizable;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.GenTypeSuper;
import bluej.debugger.gentype.GenTypeUnbounded;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.entity.ClassEntity;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.ErrorEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.NullEntity;
import bluej.parser.entity.PackageOrClass;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnresolvedEntity;
import bluej.parser.entity.ValueEntity;

public class TextParser extends JavaParser
{
    private EntityResolver resolver;
    
    private Stack<JavaEntity> valueStack = new Stack<JavaEntity>();
    private Stack<LocatableToken> operatorStack = new Stack<LocatableToken>();
    
    private static final int CAST_OPERATOR = JavaTokenTypes.INVALID + 1;
    
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
        processHigherPrecedence(JavaTokenTypes.EOF);
        if (valueStack.isEmpty()) {
            return null;
        }
        return valueStack.pop();
    }
    
    /**
     * Pop an item from the value stack. If there are no values to pop, supply an error entity.
     */
    public JavaEntity popValueStack()
    {
        if (! valueStack.isEmpty()) {
            return valueStack.pop();
        }
        return new ErrorEntity();
    }
        
    // Process all on-stack operators with a equal-or-higher precedence than that given
    private void processHigherPrecedence(int tokenType)
    {
        int precedence = getPrecedence(tokenType);
        while (! operatorStack.isEmpty()) {
            LocatableToken top = operatorStack.peek();
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
    private void processOperator(LocatableToken token)
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
                valueStack.push(new ValueEntity(castType.getType()));
            }
            else {
                valueStack.push(new ErrorEntity());
            }
            break;
        }
        // TODO
    }
    
    private void processNewOperator(LocatableToken token)
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
    
    private void checkArgs(JavaEntity arg1, JavaEntity arg2, LocatableToken op)
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
    private void doBinaryOp(JavaEntity arg1, JavaEntity arg2, LocatableToken op)
    {
        JavaType a1type = arg1.getType();
        JavaType a2type = arg2.getType();
        
        int ttype = op.getType();
        switch (ttype) {
        case JavaTokenTypes.PLUS:
            if (! a1type.isNumeric() || ! a2type.isNumeric()) {
                // TODO can add any type to a java.lang.String
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
        case JavaTokenTypes.DOT:
            // This is handled elsewhere
            valueStack.push(new ErrorEntity());
        default:
        }
    }
    
    /**
     * Get the precedence level for a given operator type.
     */
    private int getPrecedence(int tokenType)
    {
        switch (tokenType) {
        case JavaTokenTypes.PLUS:
        case JavaTokenTypes.MINUS:
            return 0;
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
            return 1;
        case JavaTokenTypes.DOT:
            return 25;
        case CAST_OPERATOR:
            return 50;
        case JavaTokenTypes.RBRACK:
            return 100;
        default:
        }
        
        return -1;
    }

    @Override
    protected void gotLiteral(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.CHAR_LITERAL) {
            valueStack.push(new ValueEntity(JavaPrimitiveType.getChar()));
        }
        if (token.getType() == JavaTokenTypes.NUM_INT) {
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
    }
    
    @Override
    protected void gotIdentifier(LocatableToken token)
    {
        // Process any dot operator immediately
        String ident = token.getText();
        if (! operatorStack.isEmpty() && operatorStack.peek().getType() == JavaTokenTypes.DOT) {
            JavaEntity top = valueStack.pop();
            JavaEntity newTop = top.getSubentity(ident);
            if (newTop != null) {
                valueStack.push(newTop);
            }
            else {
                valueStack.push(new ErrorEntity());
            }
        }
        else {
            valueStack.push(UnresolvedEntity.getEntity(resolver, ident));
        }
    }
    
    @Override
    protected void gotBinaryOperator(LocatableToken token)
    {
        processHigherPrecedence(token.getType());
        operatorStack.push(token);
    }
    
    @Override
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        if (state == STATE_NEW) {
            JavaEntity entity = resolveTypeSpec(tokens);
            
            if (entity != null) {
                valueStack.push(new ValueEntity(entity.getType()));
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
            operatorStack.push(new LocatableToken(CAST_OPERATOR, null));
        }
    }
    
    private class DepthRef
    {
        int depth = 0;
    }
    
    /**
     * Resolve a type specification. Returns null if the type couldn't be resolved.
     * TODO handle generics, arrays.
     */
    private ClassEntity resolveTypeSpec(List<LocatableToken> tokens)
    {
        DepthRef dr = new DepthRef();
        return resolveTypeSpec(tokens.listIterator(), dr);
    }
    
    /**
     * Resolve a type specification. Returns null if the type couldn't be resolved.
     */
    private ClassEntity resolveTypeSpec(ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        LocatableToken token = i.next();
        String text = token.getText();
        
        PackageOrClass poc = resolver.resolvePackageOrClass(text, null);
        while (poc != null && i.hasNext()) {
            token = i.next();
            if (token.getType() == JavaTokenTypes.LT) {
                // Type arguments
                ClassEntity classEnt = poc.resolveAsType();
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
    private ClassEntity processTypeArgs(ClassEntity base, ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        int startDepth = depthRef.depth;
        List<GenTypeParameterizable> taList = new LinkedList<GenTypeParameterizable>();
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
                    ClassEntity taEnt = resolveTypeSpec(i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    JavaType taType = taEnt.getType();
                    if (taType instanceof GenTypeSolid) {
                        GenTypeSuper stype = new GenTypeSuper((GenTypeSolid) taType);
                        taList.add(stype);
                    }
                    else {
                        return null;
                    }
                }
                else if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                    ClassEntity taEnt = resolveTypeSpec(i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    JavaType taType = taEnt.getType();
                    if (taType instanceof GenTypeSolid) {
                        GenTypeExtends stype = new GenTypeExtends((GenTypeSolid) taType);
                        taList.add(stype);
                    }
                    else {
                        return null;
                    }
                }
                else {
                    taList.add(new GenTypeUnbounded());
                    i.previous();
                }
            }
            else {
                i.previous();
                ClassEntity taEnt = resolveTypeSpec(i, depthRef);
                if (taEnt == null) {
                    return null;
                }
                JavaType taType = taEnt.getType();
                if (taType instanceof GenTypeParameterizable) {
                    taList.add((GenTypeParameterizable) taType);
                }
                else {
                    return null;
                }
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
        return base.setTypeParams(taList);
    }
    
    @Override
    protected void beginArgumentList(LocatableToken token)
    {
        if (state == STATE_NEW_ARGS) {
            state = STATE_NONE;
            argumentStack.push(new ArrayList<JavaEntity>());
        }
    }
    
    @Override
    protected void endArgument()
    {
        processHigherPrecedence(JavaTokenTypes.COMMA);
        if (! valueStack.isEmpty()) {
            argumentStack.peek().add(valueStack.pop());
        }
    }
    
    @Override
    protected void endArgumentList(LocatableToken token)
    {
        if (! operatorStack.isEmpty()) {
            LocatableToken top = operatorStack.pop();
            if (top.getType() == JavaTokenTypes.LITERAL_new) {
                processNewOperator(top);
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
        operatorStack.push(token);
    }

}
