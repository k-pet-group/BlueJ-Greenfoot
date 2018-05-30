/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2018 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.Platform;

import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.ast.Parser;
import bluej.stride.framedjava.ast.links.PossibleLink;
import bluej.stride.framedjava.ast.links.PossibleMethodUseLink;
import bluej.stride.framedjava.ast.links.PossibleTypeLink;
import bluej.stride.framedjava.ast.links.PossibleVarLink;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
    private void setPromptsFromParamNames(List<List<String>> possibilities)
    {
        List<StructuredSlotField> curParams = getSimpleParameters();
        int curArity;
        // There is a special case if we have a single empty parameter; this looks
        // like arity 1, but actually because it's empty, it's arity 0:
        if (curParams.size() == 1 && curParams.get(0) != null && curParams.get(0).isEmpty())
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
                modificationPlatform(token -> {
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
    @OnThread(Tag.FXPlatform)
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
                fields.get(i + 1) instanceof BracketedStructured &&
                ((BracketedStructured)fields.get(i+1)).getOpening() == '(' )
            {
                // Text, non-empty, followed by round brackets.  Must be a method call:
                BracketedStructured bracketedParams = (BracketedStructured) fields.get(i + 1);
                CaretPos absPos = absolutePos(new CaretPos(i, new CaretPos(0, null)));
                ((InfixExpression)bracketedParams.getContent()).treatAsParams_updatePrompts(fields.get(i).getCopyText(null, null), absPos);
            }
        }

    }


    /**
     * Queues a call to update prompts in method calls after modification has finished.  Will not
     * queue more than one such call at any time.
     */
    void queueUpdatePromptsInMethodCalls()
    {
        if (!queuedUpdatePrompts && slot != null && Platform.isFxApplicationThread())
        {
            queuedUpdatePrompts = true;
            slot.afterCurrentModification(this::updatePromptsInMethodCalls);
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
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

    @OnThread(Tag.FXPlatform)
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

    @Override
    protected StructuredSlotField makeNewField(String content, boolean stringLiteral)
    {
        StructuredSlotField field = super.makeNewField(content, stringLiteral);
        JavaFXUtil.addChangeListener(field.textProperty(), t -> queueUpdatePromptsInMethodCalls());
        return field;
    }

    public List<? extends PossibleLink> findLinks(Optional<Character> surroundingBracket, Map<String, CodeElement> vars, Function<Integer, JavaFragment.PosInSourceDoc> posCalculator, int offset)
    {
        final List<PossibleLink> r = new ArrayList<>();

        // Consume next compound identifier:
        int cur = 0;
        int beginningSlot = cur;
        int endSlot = cur;
        int endLength = -1;
        String curOperand = "";
        while (cur < fields.size())
        {
            if (fields.get(cur) instanceof StructuredSlotField)
            {
                final StructuredSlotField expressionSlotField = (StructuredSlotField) fields.get(cur);

                if (expressionSlotField.getText().equals("class") && curOperand.endsWith("."))
                {
                    // What went before is assumed to be a type:
                    r.add(new PossibleTypeLink(curOperand.substring(0, curOperand.length() - 1),
                        offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                        offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                }

                if (curOperand.equals(""))
                    beginningSlot = cur;
                curOperand += expressionSlotField.getText();
                endSlot = cur;
                endLength = expressionSlotField.getText().length();

                if (cur < operators.size() && operators.get(cur) == null)
                {
                    // Must be a bracket next.  If round, this will be a method call, but handled below.
                }
                else if (cur < operators.size() && operators.get(cur).get().equals("."))
                {
                    // Fine, carry on building up the identifier
                    curOperand += ".";
                }
                else
                {
                    // end of operand, File it away:

                    if (cur == operators.size() && beginningSlot == 0 && surroundingBracket.isPresent() && surroundingBracket.get() == '(')
                    {
                        // Item took up whole length of bracket; may well be a type:
                        r.add(new PossibleTypeLink(curOperand,
                            offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                            offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                    }

                    if (cur == beginningSlot)
                    {
                        // Plain (no dots):
                        if (vars != null)
                        {
                            CodeElement el = vars.get(curOperand);
                            if (el != null)
                                r.add(new PossibleVarLink(curOperand, el,
                                    offset+caretPosToStringPos(new CaretPos(beginningSlot, new CaretPos(0, null)), false),
                                    offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                        }
                    }

                    curOperand = "";
                }
            }
            else if (fields.get(cur) instanceof BracketedStructured)
            {
                StructuredSlotField prev = (StructuredSlotField) fields.get(cur - 1);
                int innerOffset = 1 + offset + caretPosToStringPos(new CaretPos(cur - 1, new CaretPos(prev.getText().length(), null)), false);
                BracketedStructured<InfixExpression, ExpressionSlot<?>> be = (BracketedStructured<InfixExpression, ExpressionSlot<?>>)fields.get(cur);
                r.addAll(be.getContent().findLinks(Optional.of(be.getOpening()), vars, posCalculator, innerOffset));

                if (!curOperand.equals("") && be.getOpening() == '(')
                {
                    // curOperand is assumed to have been a method call:
                    final int endSlotFinal = endSlot;
                    r.add(new PossibleMethodUseLink(curOperand.substring(curOperand.indexOf(".") + 1), be.getContent().getSimpleParameters().size(), () -> posCalculator.apply(offset+caretPosToStringPos(new CaretPos(endSlotFinal, new CaretPos(0, null)), true)),
                        offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(0, null)), false),
                        offset+caretPosToStringPos(new CaretPos(endSlot, new CaretPos(endLength, null)), false), getSlot()));
                }
            }

            cur += 1;
        }

        return r;
    }

    @Override
    protected boolean supportsFloatLiterals()
    {
        return true;
    }
}
