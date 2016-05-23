package bluej.stride.framedjava.slots;

import bluej.stride.generic.InteractionManager;

/**
 * Created by neil on 22/05/2016.
 */
public class InfixExpression extends InfixStructured
{
    private InfixExpression(InteractionManager editor, StructuredSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        super(editor, slot, initialContent, wrapper, closingChars);
    }

    InfixExpression(InteractionManager editor, StructuredSlot slot, String stylePrefix)
    {
        super(editor, slot, stylePrefix);
    }
    //package visible for testing
    /** Is this string an operator */
    static boolean isExpressionOperator(String s)
    {
        switch (s)
        {
            case "+": case "-": case "*": case "/":
            case "==": case "!=": case ">": case ">=":
            case "<=": case "<": case "%": case "&":
            case "&&": case "|": case "||": case "^":
            case "~": case "!": case ".": case "..": case "<:": case ",":
            case "<<": case ">>": case ">>>":
            case "->": case "::":
            return true;
            default:
                return false;
        }
    }

    @Override
    boolean isOperator(String s)
    {
        return isExpressionOperator(s);
    }

    /** Does the given character form a one-character operator, or begin a multi-character operator */
    static boolean beginsExpressionOperator(char c)
    {
        switch (c)
        {
            case '+': case '-': case '*': case '/':
            case '=': case '!': case '>': case '<':
            case '%': case '&': case '|': case '^':
            case '~': case '.': case ',': case ':':
            return true;
            default:
                return false;
        }
    }

    @Override
    boolean beginsOperator(char c)
    {
        return beginsExpressionOperator(c);
    }

    @Override
    protected boolean isOpeningBracket(char c)
    {
        return c == '(' || c == '[' || c == '{';
    }

    @Override
    protected boolean isClosingBracket(char c)
    {
        return c == ')' || c == ']' || c == '}';
    }
    
    @Override
    protected boolean isDisallowed(char c)
    {
        return c == ';';
    }

    @Override
    InfixExpression newInfix(InteractionManager editor, StructuredSlot slot, String initialContent, BracketedStructured wrapper, Character... closingChars)
    {
        return new InfixExpression(editor, slot, initialContent, wrapper, closingChars);
    }
}
