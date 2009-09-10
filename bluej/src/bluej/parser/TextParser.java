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
import java.util.List;
import java.util.Stack;

import antlr.TokenStreamException;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.entity.JavaEntity;

public class TextParser extends NewParser
{
    private Stack<JavaEntity> valueStack = new Stack<JavaEntity>();
    private Stack<LocatableToken> operatorStack = new Stack<LocatableToken>();
    
    public TextParser(Reader r)
    {
        super(r);
    }
    
    public TextParser(String s)
    {
        this(new StringReader(s));
    }
    
    public boolean atEnd()
    {
        try {
            return tokenStream.LA(1).getType() == JavaTokenTypes.EOF;
        } catch (TokenStreamException e) {
            return true;
        }
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
    
    @Override
    protected void gotLiteral(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.CHAR_LITERAL) {
            valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getChar()));
        }
        if (token.getType() == JavaTokenTypes.NUM_INT) {
            valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getInt()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_LONG) {
            valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getLong()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_FLOAT) {
            valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getFloat()));
        }
        else if (token.getType() == JavaTokenTypes.NUM_DOUBLE) {
            valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getDouble()));
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
        // TODO Auto-generated method stub
        super.gotTypeSpec(tokens);
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
     *  Process an operator, take the operands from the value stack and leave the result on the
     *  stack.
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
        }
        // TODO
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
        int ttype = op.getType();
        switch (ttype) {
        case JavaTokenTypes.PLUS:
            // TODO
            if (arg1.getType().typeIs(JavaType.JT_DOUBLE) || arg2.getType().typeIs(JavaType.JT_DOUBLE)) {
                valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getDouble()));
            }
            else if (arg1.getType().typeIs(JavaType.JT_FLOAT) || arg2.getType().typeIs(JavaType.JT_FLOAT)) {
                valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getFloat()));
            }
            else if (arg1.getType().typeIs(JavaType.JT_LONG) || arg2.getType().typeIs(JavaType.JT_LONG)) {
                valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getLong()));
            }
            else {
                valueStack.push(new PrimitiveValueEntity(JavaPrimitiveType.getInt()));
            }
        }
    }
    
    private int getPrecedence(int tokenType)
    {
        switch (tokenType) {
        case JavaTokenTypes.PLUS:
        case JavaTokenTypes.MINUS:
            return 0;
        case JavaTokenTypes.STAR:
        case JavaTokenTypes.DIV:
            return 1;
        default:
        }
        
        return -1;
    }
    
}

class PrimitiveValueEntity extends JavaEntity
{
    JavaType type;
    String name;
    
    PrimitiveValueEntity(JavaType type)
    {
        this.type = type;
    }
    
    PrimitiveValueEntity(JavaType type, String name)
    {
        this.type = type;
        this.name = name;
    }
    
    @Override
    public JavaEntity resolveAsValue()
    {
        return this;
    }
    
    @Override
    public JavaEntity resolveAsValOrType() throws SemanticException
    {
        return this;
    }
    
    public JavaType getType()
    {
        return type;
    }
    
    public JavaEntity getSubentity(String name)
        throws SemanticException
    {
        throw new SemanticException();
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isClass()
    {
        return false;
    }
}

class ErrorEntity extends JavaEntity
{
    @Override
    public String getName()
    {
        return "** error **";
    }
    
    @Override
    public JavaEntity getSubentity(String name) throws SemanticException
    {
        return this;
    }
    
    @Override
    public JavaType getType()
    {
        return null;
    }
    
    @Override
    public boolean isClass()
    {
        return false;
    }
}
