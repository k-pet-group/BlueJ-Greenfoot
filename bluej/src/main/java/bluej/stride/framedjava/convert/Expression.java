/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2017 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.convert;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.CallExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.convert.ConversionWarning.UnsupportedFeature;
import bluej.stride.framedjava.elements.AssignElement;
import bluej.stride.framedjava.elements.CallElement;
import bluej.stride.framedjava.elements.CodeElement;

/**
 * An expression.  Because expressions have different text in Stride versus Java
 * (because of instanceof), we must keep track of them seperately.
 */
class Expression
{
    private final String stride;
    private final String java;
    /**
     * A list of JavaTokenTypes.{INC,DEC} found in the expression.  A warning
     * should be generated if any turn out to be unsupported.
     */
    private final List<Integer> incDec;
    /**
     * A callback to add a warning.
     */
    private final Consumer<ConversionWarning> addWarning;

    /**
     * @param src A java expression
     */
    Expression(String src, List<Integer> incDec, Consumer<ConversionWarning> addWarning)
    {
        this.stride = uniformSpacing(src, true);
        this.java = uniformSpacing(src, false);
        this.incDec = new ArrayList<>(incDec);
        this.addWarning = addWarning;
    }

    /**
     * @param expressions A list of expressions to join
     * @param join The string to put between each pair of adjacent expressions
     */
    Expression(List<Expression> expressions, String join, Consumer<ConversionWarning> addWarning)
    {
        this.stride = expressions.stream().map(e -> e.stride).collect(Collectors.joining(join));
        this.java = expressions.stream().map(e -> e.java).collect(Collectors.joining(join));
        this.incDec = new ArrayList<>();
        expressions.forEach(e -> this.incDec.addAll(e.incDec));
        this.addWarning = addWarning;
    }

    /**
     * Package-visible for testing
     * 
     * @param src Java source code
     * @param replaceInstanceof True to replace instanceof with <:
     * @return A version of src with a space between each consecutive token
     */
    static String uniformSpacing(String src, boolean replaceInstanceof)
    {
        // It is a bit inefficient to re-lex the string, but
        // it's easiest this way and conversion is not particularly time sensitive:
        JavaLexer lexer = new JavaLexer(new StringReader(src));
        StringBuilder r = new StringBuilder();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == JavaTokenTypes.EOF)
                return r.toString();
            if (r.length() != 0)
                r.append(" ");
            if (replaceInstanceof && token.getType() == JavaTokenTypes.LITERAL_instanceof)
                r.append("<:");
            else
                r.append(token.getText());
        }
    }

    FilledExpressionSlotFragment toFilled()
    {
        warnIncDec();
        return new FilledExpressionSlotFragment(stride, java);
    }
    
    OptionalExpressionSlotFragment toOptional()
    {
        warnIncDec();
        return new OptionalExpressionSlotFragment(stride, java);
    }

    SuperThisParamsExpressionFragment toSuperThis()
    {
        warnIncDec();
        return new SuperThisParamsExpressionFragment(stride, java);
    }

    public CodeElement toStatement()
    {
        // We look for increment and decrement at the beginning/end
        // Then if there are any other inc/dec, we warn that they are unsupported.
        boolean startInc = java.startsWith("++ ");
        boolean startDec = java.startsWith("-- ");
        boolean endInc = java.endsWith(" ++");
        boolean endDec = java.endsWith(" --");
        if (startInc || startDec)
        {
            incDec.remove((Integer)(startInc ? JavaTokenTypes.INC : JavaTokenTypes.DEC));
            warnIncDec();
            return new AssignElement(null, new FilledExpressionSlotFragment(stride.substring(3), java.substring(3)), new FilledExpressionSlotFragment(stride.substring(3) + " " + stride.substring(0, 1) + " 1", java.substring(3) + " " + java.substring(0, 1) + " 1"), true);
        }
        else if (endInc || endDec)
        {
            incDec.remove((Integer)(endInc ? JavaTokenTypes.INC : JavaTokenTypes.DEC));
            warnIncDec();
            String choppedStride = stride.substring(0, stride.length() - 3);
            String choppedJava = java.substring(0, java.length() - 3);
            return new AssignElement(null, new FilledExpressionSlotFragment(choppedStride, choppedJava), new FilledExpressionSlotFragment(choppedStride + " " + stride.substring(stride.length() - 1, stride.length()) + " 1", choppedJava + " " + java.substring(java.length() - 1, java.length()) + " 1"), true);
        }
        else
        {
            warnIncDec();
            return new CallElement(null, new CallExpressionSlotFragment(stride, java), true);
        }
    }

    private void warnIncDec()
    {
        if (!incDec.isEmpty())
            addWarning.accept(new UnsupportedFeature("++/-- in expression"));
    }

    @Override
    public String toString()
    {
        // If we need to display to the user, display the original Java:
        return java;
    }
    
    public String getJava()
    {
        return java;
    }

    /**
     * Is the expression an integer literal?  (Long literal will give false)
     */
    public boolean isIntegerLiteral()
    {
        JavaLexer lexer = new JavaLexer(new StringReader(java));
        if (lexer.nextToken().getType() != JavaTokenTypes.NUM_INT)
            return false;
        return lexer.nextToken().getType() == JavaTokenTypes.EOF;
    }

    /**
     * Given varName, is the expression of the exact form "varName < integer_literal"
     * or "varName <= integer_literal" ?
     */
    public boolean lessThanIntegerLiteral(String varName)
    {
        JavaLexer lexer = new JavaLexer(new StringReader(java));
        LocatableToken token = lexer.nextToken();
        if (token.getType() != JavaTokenTypes.IDENT || !token.getText().equals(varName))
            return false;
        token = lexer.nextToken();
        if (token.getType() != JavaTokenTypes.LT && token.getType() != JavaTokenTypes.LE)
            return false;
        if (lexer.nextToken().getType() != JavaTokenTypes.NUM_INT)
            return false;
        return lexer.nextToken().getType() == JavaTokenTypes.EOF;
    }

    /**
     * Assuming lessThanIntegerLiteral has already returned true,
     * what is the inclusive upper bound on the condition?
     */
    public String getUpperBound()
    {
        JavaLexer lexer = new JavaLexer(new StringReader(java));
        LocatableToken token = lexer.nextToken();
        if (token.getType() != JavaTokenTypes.IDENT)
            return "";
        LocatableToken comparisonToken = lexer.nextToken();
        if (comparisonToken.getType() != JavaTokenTypes.LT && comparisonToken.getType() != JavaTokenTypes.LE)
            return "";
        token = lexer.nextToken();
        if (token.getType() != JavaTokenTypes.NUM_INT)
            return "";
        // If it was < 10, we have to subtract one to get inclusive bound of 9:
        return Integer.toString(Integer.decode(token.getText()) + (comparisonToken.getType() == JavaTokenTypes.LT ? -1 : 0)); 
    }
    
    public boolean isIncrementByOne(String varName)
    {
        // Four different possibilities:
        // varName++
        // ++varName
        // varName += 1
        // varName = varName + 1
        JavaLexer lexer = new JavaLexer(new StringReader(java));
        LocatableToken token = lexer.nextToken();
        // First token, can be varName, or ++
        if (token.getType() == JavaTokenTypes.INC)
        {
            token = lexer.nextToken();
            if (token.getType() != JavaTokenTypes.IDENT || !token.getText().equals(varName))
                return false;
            // Fall through to EOF check
        }
        else if (token.getType() == JavaTokenTypes.IDENT && token.getText().equals(varName))
        {
            // Was varName
            token = lexer.nextToken();
            if (token.getType() == JavaTokenTypes.INC)
            {
                // Increment; fall through to EOF check
            }
            else if (token.getType() == JavaTokenTypes.PLUS_ASSIGN)
            {
                // +=.  Needs to be 1 on RHS:
                token = lexer.nextToken();
                if (token.getType() != JavaTokenTypes.NUM_INT || !token.getText().equals("1"))
                    return false;
                // Fall through to EOF check
            }
            else if (token.getType() == JavaTokenTypes.ASSIGN)
            {
                // =.  Look for varName + 1 on RHS:
                // (We could look for 1 + varName, etc, but we don't bother:
                token = lexer.nextToken();
                if (token.getType() != JavaTokenTypes.IDENT || !token.getText().equals(varName))
                    return false;
                token = lexer.nextToken();
                if (token.getType() != JavaTokenTypes.PLUS)
                    return false;
                token = lexer.nextToken();
                if (token.getType() != JavaTokenTypes.NUM_INT || !token.getText().equals("1"))
                    return false;
                // Fall through to EOF check
            }
            else
                return false;
                    
        }
        else
            return false;

        return lexer.nextToken().getType() == JavaTokenTypes.EOF;
    }
}
