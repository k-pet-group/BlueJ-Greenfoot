package bluej.stride.framedjava.slots;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXConsumer;

/**
 * Created by neil on 22/05/2016.
 */
public class InfixType extends InfixStructured<TypeSlot, InfixType>
{
    private InfixType(InteractionManager editor, TypeSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        super(editor, slot, initialContent, wrapper, closingChars);
    }

    InfixType(InteractionManager editor, TypeSlot slot, String stylePrefix)
    {
        super(editor, slot, stylePrefix);
    }
    //package visible for testing
    /** Is this string an operator */
    @Override
    boolean isOperator(String s)
    {
        switch (s)
        {
            case ".": case ",":
            return true;
            default:
                return false;
        }
    }

    /** Does the given character form a one-character operator, or begin a multi-character operator */
    @Override
    boolean beginsOperator(char c)
    {
        switch (c)
        {
            case ',': case '.':
            return true;
            default:
                return false;
        }
    }

    @Override
    boolean canBeUnary(String s)
    {
        //No unary operators in types:
        return false;
    }

    @Override
    protected boolean isOpeningBracket(char c)
    {
        return c == '<' || c == '[';
    }

    @Override
    protected boolean isClosingBracket(char c)
    {
        return c == '>' || c == ']';
    }

    @Override
    protected boolean isDisallowed(char c)
    {
        return !(Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c)
            || c == ',' || c == '.' || c == '<' || c == '>' || c == '?'
            || c == '[' || c == ']' || c == '$');
    }

    @Override
    InfixType newInfix(InteractionManager editor, TypeSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        return new InfixType(editor, slot, initialContent, wrapper, closingChars);
    }

    @Override
    public void withTooltipFor(StructuredSlotField expressionSlotField, FXConsumer<String> handler)
    {
        //TODOTYPESLOT
    }
}
