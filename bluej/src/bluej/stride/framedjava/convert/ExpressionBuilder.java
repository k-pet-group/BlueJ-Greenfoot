package bluej.stride.framedjava.convert;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import bluej.parser.lexer.LocatableToken;

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

    ExpressionBuilder(Consumer<Expression> handler, BiFunction<LocatableToken, LocatableToken, String> getText)
    {
        this.handler = handler;
        this.getText = getText;
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
            String java = getText.apply(start, end);
            handler.accept(new Expression(java));
            // Finished now:
            return true;
        }
        else
            // Not finished yet:
            return false;
    }
}
