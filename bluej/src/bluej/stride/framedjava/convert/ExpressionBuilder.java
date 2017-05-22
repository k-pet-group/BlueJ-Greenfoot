/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.OptionalExpressionSlotFragment;
import bluej.stride.framedjava.ast.SuperThisParamsExpressionFragment;
import bluej.stride.framedjava.convert.ConversionWarning.UnsupportedFeature;
import bluej.stride.framedjava.elements.AssignElement;
import bluej.stride.framedjava.elements.CodeElement;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Class in charge of building expressions.
 * 
 * Because expressions can nest, beginExpression/endExpression will likely be called
 * multiple times on the same handler.  We pair them off, and only finish when
 * the outermost expression is completed.
 */
class ExpressionBuilder
{
    // The start of the outermost expression
    private LocatableToken start;
    // Amount of expressions begun but not ending:
    private int outstanding = 0;
    // The handler to call at the end
    private final Consumer<Expression> handler;
    // The function to get the text between a start token (incl) and
    // end token (excl), excluding a series of masked sections (where the delimiteds
    // are both included in the masking)
    private final TriFunction<LocatableToken, LocatableToken, List<Mask>, String> getText;
    // The top-level assignment operator (e.g. =, +=, >>=), if any
    private LocatableToken assignOp; // may be null
    // The list of JavaTokenTypes.INC,DEC found in the expression
    private List<Integer> incDec = new ArrayList<>();
    // The function to record a conversion warning
    private final Consumer<ConversionWarning> addWarning;
    // The stack of currently open masks.  We only need to record
    // the outermost because that will mask all inner masks.
    private final Stack<Mask> curMasks =  new Stack<>();
    // The list of completed, non-overlapping masks.  Masks are used
    // to remove e.g. the bodies of anonymous inner classes.
    private final List<Mask> completeMasks = new ArrayList<>();

    // Create expression builder, passing all the callbacks we need.
    ExpressionBuilder(Consumer<Expression> handler, TriFunction<LocatableToken, LocatableToken, List<Mask>, String> getText, Consumer<ConversionWarning> addWarning)
    {
        this.handler = handler;
        this.getText = getText;
        this.addWarning = addWarning;
    }

    void expressionBegun(LocatableToken start)
    {
        // Only record first begin:
        if (outstanding == 0)
            this.start = start;
        outstanding += 1;
    }

    // Return true if we should be removed from the stack
    boolean expressionEnd(LocatableToken end)
    {
        outstanding -= 1;
        // If the outermost has finished, pass it to handler:
        if (outstanding == 0)
        {
            if (assignOp == null)
            {
                // No top-level assignment, nothing special to do:
                String java = getText.apply(start, end, completeMasks);
                handler.accept(new Expression(java, incDec, addWarning));
            }
            else
            {
                // We override toStatement to give an assignment statement,
                // but if they call toFilled, etc, they'll get the expression as-is,
                // even though Stride won't like it
                String wholeJava = getText.apply(start, end, completeMasks);
                String lhs = getText.apply(start, assignOp, completeMasks).trim();
                String rhs = assignOp.getText().equals("=")
                    ? getText.apply(assignOp, end, completeMasks).substring(assignOp.getText().length())
                    : lhs + " " + assignOp.getText().substring(0, assignOp.getText().length() - 1) + " " + getText.apply(assignOp, end, completeMasks).substring(assignOp.getText().length());
                handler.accept(new Expression(wholeJava, incDec, addWarning) {
                    @Override
                    FilledExpressionSlotFragment toFilled()
                    {
                        addWarning.accept(new UnsupportedFeature(assignOp.getText() + " in expression"));
                        return super.toFilled();
                    }

                    @Override
                    OptionalExpressionSlotFragment toOptional()
                    {
                        addWarning.accept(new UnsupportedFeature(assignOp.getText() + " in expression"));
                        return super.toOptional();
                    }

                    @Override
                    SuperThisParamsExpressionFragment toSuperThis()
                    {
                        addWarning.accept(new UnsupportedFeature(assignOp.getText() + " in expression"));
                        return super.toSuperThis();
                    }

                    @Override
                    public CodeElement toStatement()
                    {
                        return new AssignElement(null, new Expression(lhs, incDec, addWarning).toFilled(), new Expression(rhs, Collections.emptyList(), addWarning).toFilled(), true);
                    }
                });
            }
            
            // Finished now:
            return true;
        }
        else
            // Not finished yet:
            return false;
    }

    // Called when a binary operator is seen
    public void binaryOperator(LocatableToken opToken)
    {
        switch (opToken.getType())
        {
            case JavaTokenTypes.BAND_ASSIGN:
            case JavaTokenTypes.BOR_ASSIGN:
            case JavaTokenTypes.BSR_ASSIGN:
            case JavaTokenTypes.BXOR_ASSIGN:
            case JavaTokenTypes.DIV_ASSIGN:
            case JavaTokenTypes.MINUS_ASSIGN:
            case JavaTokenTypes.MOD_ASSIGN:
            case JavaTokenTypes.PLUS_ASSIGN:
            case JavaTokenTypes.SL_ASSIGN:
            case JavaTokenTypes.SR_ASSIGN:
            case JavaTokenTypes.STAR_ASSIGN:
            case JavaTokenTypes.ASSIGN:
                if (outstanding == 1)
                    assignOp = opToken;
                else
                    addWarning.accept(new UnsupportedFeature(opToken.getText() + " in expression"));
                break;
            default:
                return;
        } 
    }

    // Called when a unary (prefix) operator is seen
    public void unaryOperator(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.INC || token.getType() == JavaTokenTypes.DEC)
            incDec.add(token.getType());
    }

    // Called when a unary postfix operator is seen
    public void postOperator(LocatableToken token)
    {
        unaryOperator(token);
    }

    // Called to begin masking a section from the given token (incl)
    public void beginMask(LocatableToken from)
    {
        curMasks.push(new Mask(from));
    }

    // Called to finish masking a section up to the given token (incl)
    public void endMask(LocatableToken to)
    {
        Mask mask = curMasks.pop();
        // Only store outermost mask:
        if (curMasks.isEmpty())
        {
            mask.setEnd(to);
            completeMasks.add(mask);
        }
    }

    // When BiFunction isn't enough...
    public static interface TriFunction<T1, T2, T3, R>
    {
        public R apply(T1 t1, T2 t2, T3 t3);
    }
}
