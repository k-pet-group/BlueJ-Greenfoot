package bluej.stride.framedjava.slots;

import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 22/05/2016.
 */
public class InfixType extends InfixStructured
{
    private InfixType(InteractionManager editor, StructuredSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        super(editor, slot, initialContent, wrapper, closingChars);
    }

    InfixType(InteractionManager editor, StructuredSlot slot, String stylePrefix)
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
    InfixStructured newInfix(InteractionManager editor, StructuredSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        return new InfixType(editor, slot, initialContent, wrapper, closingChars);
    }
}
