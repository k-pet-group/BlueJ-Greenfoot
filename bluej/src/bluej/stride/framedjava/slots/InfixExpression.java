package bluej.stride.framedjava.slots;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javafx.application.Platform;

import bluej.stride.framedjava.ast.Parser;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXConsumer;

/**
 * Created by neil on 22/05/2016.
 */
public class InfixExpression extends InfixStructured<ExpressionSlot<?>, InfixExpression>
{
    /**
     * Keeps track of whether we have queued a task to update the prompts:
     */
    private boolean queuedUpdatePrompts;
    
    private InfixExpression(InteractionManager editor, ExpressionSlot<?> slot, String initialContent, BracketedStructured wrapper, StructuredSlot.ModificationToken token, Character... closingChars)
    {
        super(editor, slot, initialContent, wrapper, token, closingChars);
    }

    InfixExpression(InteractionManager editor, ExpressionSlot<?> slot, StructuredSlot.ModificationToken token)
    {
        super(editor, slot, token);
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
    
    /** Can this operator be unary */
    @Override
    boolean canBeUnary(String s)
    {
        if (s == null)
            return false;

        switch (s)
        {
            case "+": case "-": case "~": case "!": case "new ":
            return true;
            default:
                return false;
        }
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
    InfixExpression newInfix(InteractionManager editor, ExpressionSlot<?> slot, String initialContent, BracketedStructured<?, ExpressionSlot<?>> wrapper, StructuredSlot.ModificationToken token, Character... closingChars)
    {
        return new InfixExpression(editor, slot, initialContent, wrapper, token, closingChars);
    }


    // package-visible
    // Called to notify us that we are a set of parameters, and we should update our prompts for those parameters
    void treatAsConstructorParams_updatePrompts()
    {
        List<StructuredSlotField> params = getSimpleParameters();

        if (params.stream().allMatch(p -> p == null))
            return; // Nothing needs prompts

        slot.withParamNamesForConstructor(
            poss -> setPromptsFromParamNames(poss));
    }

    // package-visible
    // Called to notify us that we are a set of parameters, and we should update our prompts for those parameters
    void treatAsParams_updatePrompts(String methodName, CaretPos absPosOfMethodName)
    {
        List<StructuredSlotField> params = getSimpleParameters();

        if (params.stream().allMatch(p -> p == null))
            return; // Nothing needs prompts

        if (slot == null) // Can happen during testing
            return;

        slot.withParamNamesForPos(absPosOfMethodName, methodName,
            poss -> setPromptsFromParamNames(poss));
    }

    /**
     * A callback called when we have fetched information on parameter names, and want
     * to use it to update the prompts for method parameters.
     *
     * @param possibilities  This is the list of possible parameters.  If there is a single
     *                       method of that name, possibilities will be a singleton list, with
     *                       the content being parameter names, e.g.
     *                       Arrays.asList(Arrays.asList("x", "y")) for setLocation(int x, int y)
     *                       If possibilities is empty, there are no methods found.
     *                       If possibilities is not size 1, there are multiple overloads for that name.
     */
    private void setPromptsFromParamNames(List<List<String>> possibilities)
    {
        List<StructuredSlotField> curParams = getSimpleParameters();
        int curArity;
        // There is a special case if we have a single empty parameter; this looks
        // like arity 1, but actually because it's empty, it's arity 0:
        if (curParams.size() == 1 && curParams.get(0).isEmpty())
            curArity = 0;
        else
            curArity = curParams.size();
        // Arity is fixed if any params are non-empty (i.e. null or !isEmpty())
        boolean arityFlexible = curParams.stream().allMatch(f -> f != null && f.isEmpty());

        List<List<String>> matchedPoss = possibilities.stream()
            .filter(ps -> arityFlexible || ps.size() == curArity)
            .sorted(Comparator.comparing(List::size)) // Put shortest ones first
            .collect(Collectors.toList());

        if (matchedPoss.size() != 1)
        {
            // No possibilities, remove all commas if empty:
            if (arityFlexible && !isEmpty())
            {
                modification(this::blank);
            }
            curParams.stream().filter(f -> f != null).forEach(f -> f.setPromptText(""));
        }
        else
        {
            // Exactly one option; give prompts:
            List<String> match = matchedPoss.get(0);

            if (arityFlexible && match.size() != curArity)
            {
                // No fixed arity; we know field must be near-blank, so just
                // replace it with the right number of commas (may be zero):
                boolean wasFocused = isFocused();
                modification(token -> {
                    blank(token);
                    for (int i = 0; i < match.size() - 1; i++)
                    {
                        // We add at end to avoid the overtyping logic:
                        insertChar(getEndPos(), ',', false, token);
                    }
                });
                curParams = getSimpleParameters();
                if (wasFocused)
                    getFirstField().requestFocus();
            }

            for (int i = 0; i < match.size(); i++)
            {
                String prompt = match.get(i);
                if (prompt == null || Parser.isDummyName(prompt))
                    prompt = "";
                // Due to the delay in calculating prompts, we may be trying to set a parameter
                // at an outdated index, so protect against that:
                if (i < curParams.size() && curParams.get(i) != null)
                    curParams.get(i).setPromptText(prompt);
            }
        }
    }

    //package-visible
    private void updatePromptsInMethodCalls()
    {
        queuedUpdatePrompts = false;

        // We look for method calls, which means we need to look for brackets preceded by non-empty fields:
        // We look from the first field onwards, because e.g. getWorl().addObject() should update
        // the addObject call if "getWorl" gets editing to "getWorld".
        // However, we only check at the current level; a method can't affect the prompts
        // for further calls inside its parameters.
        // TODO: updating later calls doesn't seem to work right, for some reason?
        for (int i = 0; i < fields.size(); i++)
        {
            if (i < fields.size() - 1 &&
                fields.get(i) instanceof StructuredSlotField &&
                !fields.get(i).isFieldAndEmpty() &&
                fields.get(i + 1) instanceof BracketedStructured)
            {
                // Text, non-empty, followed by brackets.  Must be a method call:
                BracketedStructured bracketedParams = (BracketedStructured) fields.get(i + 1);
                CaretPos absPos = absolutePos(new CaretPos(i, new CaretPos(0, null)));
                ((InfixExpression)bracketedParams.getContent()).treatAsParams_updatePrompts(fields.get(i).getCopyText(null, null), absPos);
            }
        }

    }


    /**
     * Queues a call to update prompts in method calls.  Will not
     * queue more than one such call at any time.
     */
    void queueUpdatePromptsInMethodCalls()
    {
        if (!queuedUpdatePrompts)
        {
            queuedUpdatePrompts = true;
            Platform.runLater(this::updatePromptsInMethodCalls);
        }
    }
    
    @Override
    public void calculateTooltipFor(StructuredSlotField expressionSlotField, FXConsumer<String> handler)
    {
        int slotIndex = fields.indexOf(expressionSlotField);

        // It can also have a tooltip if it is a method name, which means
        // it has a bracketedexpression immediately afterwards.
        if (!expressionSlotField.getText().equals("") &&
            slotIndex + 1 < fields.size() && fields.get(slotIndex + 1) instanceof BracketedStructured)
        {
            CaretPos relPos = new CaretPos(slotIndex, new CaretPos(0, null));
            slot.withMethodHint(absolutePos(relPos), fields.get(slotIndex).getCopyText(null, null),
                methodHints -> {
                    if (methodHints.size() == 1)
                    {
                        handler.accept(methodHints.get(0));
                    } else
                    {
                        // More than one set of hints (method is overloaded), play simple and give no tip:
                        handler.accept("");
                    }
                });
        }
        // It can have a tooltip if is a parameter, which means it is
        // inside a bracketedexpression, with a non-blank before it
        else if (parent != null || slot.isConstructorParams())
        {
            int paramIndex = 0;
            int totalParams = 1;

            for (int i = 0; i < operators.size(); i++)
            {
                if (operators.get(i) != null && operators.get(i).getCopyText().equals(","))
                {
                    totalParams += 1;
                    if (i < slotIndex)
                        paramIndex += 1;
                }
            }
            if (parent != null)
                parent.getParent().withTooltipForParam(parent, paramIndex, handler);
            else
            {
                final int finalParamIndex = paramIndex;
                // We are the top-level and we are constuctor params
                slot.withParamHintsForConstructor(totalParams, conHints -> {
                    // Only show if there's one overload of that arity:
                    if (conHints.size() == 1 && finalParamIndex < conHints.get(0).size())
                    {
                        handler.accept(conHints.get(0).get(finalParamIndex));
                    }
                });
            }

        }
        else
        {
            handler.accept("");
        }
    }


    public void withTooltipForParam(BracketedStructured bracketedExpression, int paramPos, FXConsumer<String> handler)
    {
        int expIndex = fields.indexOf(bracketedExpression);
        if (fields.get(expIndex - 1).getCopyText(null, null).equals(""))
        {
            // No text before bracket; not parameter so no valid tooltip:
            handler.accept("");
        }
        else
        {
            CaretPos relPos = new CaretPos(expIndex - 1, new CaretPos(0, null));
            slot.withParamHintsForPos(absolutePos(relPos), fields.get(expIndex - 1).getCopyText(null, null),
                paramHints -> {
                    if (paramHints.size() == 1)
                    {
                        if (paramPos < paramHints.get(0).size())
                        {
                            handler.accept(paramHints.get(0).get(paramPos));
                            return;
                        }
                    }
                    // More than one set of hints (method is overloaded), play simple and give no tip:
                    handler.accept("");
                });
        }
    }

}
