package bluej.stride.framedjava.ast;

import java.util.stream.Stream;

import bluej.stride.framedjava.errors.EmptyError;
import bluej.stride.framedjava.errors.SyntaxCodeError;
import bluej.stride.framedjava.slots.ExpressionSlot;

/**
 * Created by neil on 04/12/2015.
 */
public class CallExpressionSlotFragment extends FilledExpressionSlotFragment
{
    public CallExpressionSlotFragment(String content, String javaCode)
    {
        super(content, javaCode);
    }

    public CallExpressionSlotFragment(String content, String javaCode, ExpressionSlot slot)
    {
        super(content, javaCode, slot);
    }

    @Override
    public Stream<SyntaxCodeError> findEarlyErrors()
    {
        // TODO Also check the call is actually a method call, and not an assignment or other expression
        Stream<SyntaxCodeError> superErrors = super.findEarlyErrors();
        // Look for a blank frame and give an error:
        if (content.equals("()"))
        {
            return Stream.concat(Stream.of(new EmptyError(this, "Method name cannot be blank")), superErrors);
        }
        return superErrors;
    }
}
