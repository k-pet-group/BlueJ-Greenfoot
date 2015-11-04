/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import java.util.List;
import java.util.stream.Stream;

import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import bluej.stride.framedjava.slots.InfixExpression.CaretPosMap;
import bluej.stride.framedjava.slots.InfixExpression.IntCounter;
import bluej.stride.generic.Frame.View;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

// Package-visible
class StringLiteralExpression implements ExpressionSlotComponent
{
    private final ExpressionSlotField field;
    private final ObservableList<Node> components = FXCollections.observableArrayList();
    private final StringExpression textProperty;
    private final Label openingQuote;
    private final Label closingQuote;

    public StringLiteralExpression(ExpressionSlotField f, InfixExpression parent)
    {
        field = f;
        openingQuote = ExpressionSlot.makeBracket("\u201C", true, parent);
        closingQuote = ExpressionSlot.makeBracket("\u201D", false, parent);
        components.add(openingQuote);
        components.addAll(field.getComponents());
        components.add(closingQuote);
        // All components should stick together, so we set no-break-before on
        // all except first:
        for (int i = 1; i < components.size(); i++)
            HangingFlowPane.setBreakBefore(components.get(i), false);

        JavaFXUtil.addStyleClass(openingQuote, "expression-string-literal-quote");
        JavaFXUtil.addStyleClass(closingQuote, "expression-string-literal-quote");

        textProperty = new ReadOnlyStringWrapper("\"").concat(f.textProperty()).concat("\"");
    }

    @Override
    public void focusAtStart()
    {
        field.focusAtStart();
    }

    @Override
    public void focusAtEnd()
    {
        field.focusAtEnd();
    }

    @Override
    public Node focusAtPos(CaretPos caretPos)
    {
        return field.focusAtPos(caretPos);
    }

    @Override
    public TextOverlayPosition calculateOverlayPos(CaretPos pos)
    {
        return field.calculateOverlayPos(pos);
    }

    @Override
    public PosAndDist getNearest(double sceneX, double sceneY, boolean allowDescend, boolean anchorInItem)
    {
        if (allowDescend || anchorInItem)
            return field.getNearest(sceneX, sceneY, allowDescend, anchorInItem);
        else
            return new PosAndDist();
    }

    @Override
    public CaretPos getSelectIntoPos(boolean atEnd)
    {
        // Not allowed to select part of field:
        return null;
    }
    
    @Override
    public CaretPos getStartPos()
    {
        return field.getStartPos();
    }
    
    @Override
    public CaretPos getEndPos()
    {
        return field.getEndPos();
    }

    @Override
    public String getCopyText(CaretPos from, CaretPos to)
    {
        // We only add start and end when from/to are null:
        StringBuilder b = new StringBuilder();
        if (from == null)
            b.append("\"");
        b.append(field.getCopyText(from, to));        
        if (to == null)
            b.append("\"");
        
        return b.toString();
    }
    
    @Override
    public String getJavaCode()
    {
        StringBuilder b = new StringBuilder();
        b.append("\"");
        b.append(field.getText());        
        b.append("\"");
        
        return b.toString();
    }

    @Override
    public CaretPos getCurrentPos()
    {
        return field.getCurrentPos();
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return components;
    }

    @Override
    public List<CaretPosMap> mapCaretPosStringPos(IntCounter cur, boolean javaString)
    {
        cur.counter += 1;
        List<CaretPosMap> r = field.mapCaretPosStringPos(cur, false);
        // Need to add one to each index for opening bracket, and one to length for opening bracket
        cur.counter += 1;
        return r;
    }

    @Override
    public Region getNodeForPos(CaretPos subPos)
    {
        return field.getNodeForPos(subPos);
    }

    //Package-visible
    ExpressionSlotField getField()
    {
        return field;
    }

    @Override
    public String testingGetState(CaretPos pos)
    {
        if (pos == null)
            return "\"" + field.getText() + "\"";
        else
        {
            return "\"" + field.getText().substring(0, pos.index) + "$" + field.getText().substring(pos.index) + "\"";
        }
    }

    @Override
    public boolean isFocused()
    {
        return field.isFocused();
    }

    @Override
    public void insertSuggestion(CaretPos p, String name, List<String> params)
    {
        if (params != null)
            throw new IllegalArgumentException();
        getField().setText(name);
        getField().focusAtPos(new CaretPos(name.length(), null));
    }

    @Override
    public Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end)
    {
        return field.getAllStartEndPositionsBetween(start, end);
    }

    @Override
    public Stream<InfixExpression> getAllExpressions()
    {
        return Stream.empty();
    }

    @Override
    public ObservableStringValue textProperty()
    {
        return textProperty;
    }
    
    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        field.setView(oldView, newView, animate);
        // TODO animate size?
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, openingQuote, closingQuote);
        openingQuote.setText(newView == View.JAVA_PREVIEW ? "\"" : "\u201C");
        closingQuote.setText(newView == View.JAVA_PREVIEW ? "\"" : "\u201D");
    }

    @Override
    public boolean isAlmostBlank()
    {
        return field.isAlmostBlank();
    }

    @Override
    public void notifyLostFocus(ExpressionSlotField except)
    {
        if (field != except)
            field.notifyLostFocus(except);
    }

    @Override
    public void setEditable(boolean editable)
    {
        field.setEditable(editable);
    }

    @Override
    public boolean isNumericLiteral()
    {
        // A string can't be a numeric literal:
        return false;
    }
}
