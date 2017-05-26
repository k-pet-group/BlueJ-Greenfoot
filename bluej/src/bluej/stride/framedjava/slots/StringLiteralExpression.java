/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import bluej.stride.framedjava.slots.InfixStructured.CaretPosMap;
import bluej.stride.framedjava.slots.InfixStructured.IntCounter;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * A component in an expression slot which is a string literal, i.e.
 * double quotes around a single text field.
 */
// Package-visible
class StringLiteralExpression implements StructuredSlotComponent
{
    /** The text field with the string literal content */
    private final StructuredSlotField field;
    /** A permanent reference to the (unchanging) components: opening quote
     *  label, text field, closing quote label */
    private final ObservableList<Node> components = FXCollections.observableArrayList();
    /** The label for the opening quote */
    private final Label openingQuote;
    /** The label for the closing quote */
    private final Label closingQuote;
    /** Either single quote or double quote */
    private final String quote;
    private final String openingSmartQuote;
    private final String closingSmartQuote;

    public StringLiteralExpression(char quoteChar, StructuredSlotField f, InfixStructured parent)
    {
        field = f;
        this.quote = "" + quoteChar;
        // The quotes use proper open/close quote symbols normally, but
        // switch to straight quotes in Java preview mode.
        openingSmartQuote = quoteChar == '"' ? "\u201C" : "\u2018";
        closingSmartQuote = quoteChar == '"' ? "\u201D" : "\u2019";
        openingQuote = StructuredSlot.makeBracket(openingSmartQuote, true, parent);
        closingQuote = StructuredSlot.makeBracket(closingSmartQuote, false, parent);
        components.add(openingQuote);
        components.addAll(field.getComponents());
        components.add(closingQuote);
        // All components should stick together, so we set no-break-before on
        // all except first:
        for (int i = 1; i < components.size(); i++)
            HangingFlowPane.setBreakBefore(components.get(i), false);

        JavaFXUtil.addStyleClass(openingQuote, "expression-string-literal-quote");
        JavaFXUtil.addStyleClass(closingQuote, "expression-string-literal-quote");
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
            b.append(quote);
        b.append(field.getCopyText(from, to));        
        if (to == null)
            b.append(quote);
        
        return b.toString();
    }
    
    @Override
    public String getJavaCode()
    {
        StringBuilder b = new StringBuilder();
        b.append(quote);
        b.append(field.getText());        
        b.append(quote);
        
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
    StructuredSlotField getField()
    {
        return field;
    }

    @Override
    public String testingGetState(CaretPos pos)
    {
        if (pos == null)
            return quote + field.getText() + quote;
        else
        {
            return quote + field.getText().substring(0, pos.index) + "$" + field.getText().substring(pos.index) + quote;
        }
    }

    @Override
    public boolean isFocused()
    {
        return field.isFocused();
    }

    @Override
    public void insertSuggestion(CaretPos p, String name, char opening, List<String> params, StructuredSlot.ModificationToken token)
    {
        if (params != null)
            throw new IllegalArgumentException();
        getField().setText(name, token);
        getField().focusAtPos(new CaretPos(name.length(), null));
    }

    @Override
    public Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end)
    {
        return field.getAllStartEndPositionsBetween(start, end);
    }

    @Override
    public Stream<InfixStructured<?, ?>> getAllExpressions()
    {
        return Stream.empty();
    }

    @Override
    public String getText()
    {
        return quote + field.getText() + quote;
    }

    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        field.setView(oldView, newView, animate);
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, openingQuote, closingQuote);
        openingQuote.setText(newView == View.JAVA_PREVIEW ? quote : openingSmartQuote);
        closingQuote.setText(newView == View.JAVA_PREVIEW ? quote : closingSmartQuote);
    }

    @Override
    public boolean isAlmostBlank()
    {
        return field.isAlmostBlank();
    }

    @Override
    public void notifyLostFocus(StructuredSlotField except)
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

    @Override
    public int calculateEffort()
    {
        return field.calculateEffort();
    }

    @Override
    public Stream<Node> makeDisplayClone(InteractionManager editor)
    {
        return Utility.concat(
            Stream.of(JavaFXUtil.cloneLabel(openingQuote, editor.getFontCSS())),
            field.makeDisplayClone(editor),
            Stream.of(JavaFXUtil.cloneLabel(closingQuote, editor.getFontCSS()))
        );
    }

    //package-visible
    String getQuote()
    {
        return quote;
    }
}
