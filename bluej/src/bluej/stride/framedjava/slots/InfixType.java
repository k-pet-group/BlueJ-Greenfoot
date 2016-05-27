package bluej.stride.framedjava.slots;

import java.util.Optional;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXBiConsumer;
import bluej.utility.javafx.FXConsumer;

/**
 * Created by neil on 22/05/2016.
 */
public class InfixType extends InfixStructured<TypeSlot, InfixType>
{
    private InfixType(InteractionManager editor, TypeSlot slot, String initialContent, BracketedStructured wrapper, StructuredSlot.ModificationToken token, Character... closingChars)
    {
        super(editor, slot, initialContent, wrapper, token, closingChars);
    }

    InfixType(InteractionManager editor, TypeSlot slot, StructuredSlot.ModificationToken token)
    {
        super(editor, slot, token);
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
    InfixType newInfix(InteractionManager editor, TypeSlot slot, String initialContent, BracketedStructured wrapper, StructuredSlot.ModificationToken token, Character... closingChars)
    {
        return new InfixType(editor, slot, initialContent, wrapper, token, closingChars);
    }

    @Override
    public void calculateTooltipFor(StructuredSlotField expressionSlotField, FXConsumer<String> handler)
    {
        //We could add hints here for inner types, e.g. underscore in ArrayList<_>
    }

    void runIfCommaDirect(FXBiConsumer<String, String> listener)
    {
        Optional<Integer> optIndex = operators.findFirst(op -> op != null && op.get().equals(","));
        optIndex.ifPresent(index -> {
            // We know normal fields must surround operator:
            String before = getCopyText(null, new CaretPos(index, fields.get(index).getEndPos()));
            String after = getCopyText(new CaretPos(index + 1, new CaretPos(0, null)), null);
            listener.accept(before, after);
        });
    }
}
