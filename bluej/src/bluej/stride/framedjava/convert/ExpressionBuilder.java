package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

/**
 * Created by neil on 03/06/2016.
 */
class ExpressionBuilder
{
    private LocatableToken start;
    // Amount of expressions begun but not ending:
    private int outstanding = 0;
    private final Consumer<Expression> handler;
    private final BiFunction<LocatableToken, LocatableToken, String> getText;
    private LocatableToken assignOp; // may be null
    private List<Integer> incDec = new ArrayList<>();
    private final Consumer<ConversionWarning> addWarning;

    ExpressionBuilder(Consumer<Expression> handler, BiFunction<LocatableToken, LocatableToken, String> getText, Consumer<ConversionWarning> addWarning)
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
                String java = getText.apply(start, end);
                handler.accept(new Expression(java, incDec, addWarning));
            }
            else
            {
                // We override toStatement to give an assignment statement,
                // but if they call toFilled, etc, they'll get the expression as-is,
                // even though Stride won't like it
                // TODO in that case, warn about lack of support 
                String wholeJava = getText.apply(start, end);
                String lhs = getText.apply(start, assignOp).trim();
                String rhs = assignOp.getText().equals("=")
                    ? getText.apply(assignOp, end).substring(assignOp.getText().length())
                    : lhs + " " + assignOp.getText().substring(0, assignOp.getText().length() - 1) + " " + getText.apply(assignOp, end).substring(assignOp.getText().length());
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

    public void unaryOperator(LocatableToken token)
    {

        if (token.getType() == JavaTokenTypes.INC || token.getType() == JavaTokenTypes.DEC)
            incDec.add(token.getType());
    }

    public void postOperator(LocatableToken token)
    {
        unaryOperator(token);
    }
}
