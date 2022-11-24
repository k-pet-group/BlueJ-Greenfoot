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
package bluej.stride.framedjava.slots;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import bluej.stride.framedjava.ast.links.PossibleTypeLink;
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

    public List<PossibleTypeLink> findTypeLinks()
    {
        List<PossibleTypeLink> links = new ArrayList<>();

        int startField = 0;
        StringBuilder type = new StringBuilder(fields.get(0).getCopyText(null, null));
        
        // We do a final iteration for i == fields.size() to process the content of the final field.
        for (int i = 1; i <= fields.size(); i++)
        {
            if (i - 1 >= operators.size() || operators.get(i - 1) == null || !operators.get(i - 1).get().equals("."))
            {
                int start = slot.getTopLevel().caretPosToStringPos(absolutePos(new CaretPos(startField, fields.get(startField).getStartPos())), false);
                int end = slot.getTopLevel().caretPosToStringPos(absolutePos(new CaretPos(i - 1, fields.get(i - 1).getEndPos())), false);
                if (start != end && type.length() > 0)
                    links.add(new PossibleTypeLink(type.toString(), start, end, slot));
                type = new StringBuilder();
                startField = i;
            }
            else
            {
                type.append(operators.get(i - 1).get());
            }
            if (i >= fields.size())
                break;
            
            if (fields.get(i) instanceof BracketedStructured)
            {
                links.addAll(((BracketedStructured<InfixType, TypeSlot>)fields.get(i)).getContent().findTypeLinks());
            }
            else
            {
                type.append(fields.get(i).getCopyText(null, null));
            }
        }

        return links;
    }

    @Override
    protected boolean supportsFloatLiterals()
    {
        return false;
    }
}
