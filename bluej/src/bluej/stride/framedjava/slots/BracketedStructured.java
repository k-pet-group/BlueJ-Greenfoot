/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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
import java.util.Optional;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import bluej.stride.framedjava.slots.InfixStructured.CaretPosMap;
import bluej.stride.framedjava.slots.InfixStructured.IntCounter;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.Frame.View;
import bluej.utility.Utility;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.ConcatListBinding;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A bracketed subexpression in an expression slot.  This item will have matching
 * opening/closing brackets (round, square or curly) around an InfixStructured.
 * 
 * Many of its methods simply delegate to the contained InfixStructured.
 */
// Package-visible
class BracketedStructured<INFIX extends InfixStructured<SLOT, INFIX>, SLOT extends StructuredSlot<?, INFIX, ?>> implements StructuredSlotComponent
{
    /** The parent expression this item lives in */
    private final INFIX parent;
    /** The expression directly contained between the brackets */
    private final INFIX content;
    /** The list of GUI components (derived from the sub-expression) */
    private final ObservableList<Node> components = FXCollections.observableArrayList();
    /** The opening bracket character */ 
    private final char opening;
    /** The closing bracket character (must be same bracket type as opening, but cached for convenience */
    private final char closing;
    /** The label which displays the opening bracket */
    private final Label openingLabel;
    /** The label which displays the closing bracket */
    private final Label closingLabel;
    
    public BracketedStructured(InteractionManager editor, INFIX parent, SLOT slot, char opening, String initialContent, StructuredSlot.ModificationToken token)
    {
        this.parent = parent;
        this.opening = opening;
        switch (opening)
        {
        case '(': closing = ')'; break;
        case '[': closing = ']'; break;
        case '{': closing = '}'; break;
        case '<': closing = '>'; break;
        default: throw new IllegalArgumentException("Unrecognised bracket: " + opening);
        }
        content = parent.newInfix(editor, slot, initialContent, this, token, closing);
        openingLabel = StructuredSlot.makeBracket("" + opening, true, content);
        closingLabel = StructuredSlot.makeBracket("" + closing, false, content);
        HangingFlowPane.setBreakBefore(closingLabel, false);
        ConcatListBinding.bind(components, FXCollections.observableArrayList(FXCollections.observableArrayList(openingLabel), content.getComponents(), FXCollections.observableArrayList(closingLabel)));
    }

    @Override
    public String getText()
    {
        return "" + opening + content.getCopyText(null, null) + closing;
    }

    @Override
    public void focusAtStart()
    {
        content.focusAtStart();
    }

    @Override
    public void focusAtEnd()
    {
        content.focusAtEnd();
        
    }

    @Override
    public Node focusAtPos(CaretPos caretPos)
    {
        return content.positionCaret(caretPos);        
    }

    @Override
    public TextOverlayPosition calculateOverlayPos(CaretPos subPos)
    {
        return content.calculateOverlayPos(subPos);
    }

    @Override
    public PosAndDist getNearest(double sceneX, double sceneY, boolean allowDescend, boolean anchorInItem)
    {
        if (allowDescend)
            return content.getNearest(sceneX, sceneY, true, Optional.empty());
        else
            return new PosAndDist();
    }

    @Override
    public CaretPos getSelectIntoPos(boolean atEnd)
    {
        // Compound, so they can't select into us:
        return null;
    }
    
    @Override
    public CaretPos getStartPos()
    {
        return content.getStartPos();
    }
    
    @Override
    public CaretPos getEndPos()
    {
        return content.getEndPos();
    }

    @Override
    public String getCopyText(CaretPos from, CaretPos to)
    {
        // We only add start and end when from/to are null:
        StringBuilder b = new StringBuilder();
        if (from == null)
            b.append(opening);
        b.append(content.getCopyText(from, to));        
        if (to == null)
            b.append(closing);
        
        return b.toString();
    }
    
    @Override
    public String getJavaCode()
    {
        StringBuilder b = new StringBuilder();
        
        b.append(opening);
        b.append(content.getJavaCode());        
        b.append(closing);
        
        return b.toString();
    }
    
    @Override
    public CaretPos getCurrentPos()
    {
        return content.getCurrentPos();
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return components;
    }

    @Override
    public List<CaretPosMap> mapCaretPosStringPos(IntCounter len, boolean javaString)
    {
        len.counter += 1;
        List<CaretPosMap> r = content.mapCaretPosStringPos(len, javaString);
        // Need to add one to each index for opening bracket, and one to length for closing brackets
        len.counter += 1;
        return r;
    }

    @Override
    public Region getNodeForPos(CaretPos pos)
    {
        return content.getNodeForPos(pos);
    }

    // Package-visible
    INFIX getContent()
    {
        return content;
    }

    @OnThread(Tag.FXPlatform)
    public void insertAfter(String text)
    {
        parent.insertNext(this, text);
    }

    @Override
    public String testingGetState(CaretPos pos)
    {
        return "" + opening + content.testingGetState(pos) + closing;
    }

    public INFIX testingContent()
    {
        return content;
    }

    @OnThread(Tag.FXPlatform)
    public CaretPos flatten(boolean atEnd, StructuredSlot.ModificationToken token)
    {
        return parent.flattenCompound(this, atEnd, token);
    }

    @Override
    public boolean isFocused()
    {
        return content.isFocused();
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void insertSuggestion(CaretPos p, String name, char opening, List<String> params, StructuredSlot.ModificationToken token)
    {
        content.insertSuggestion(p, name, opening, params, token);
    }

    //package-visible
    CaretPos absolutePos(CaretPos p)
    {
        return parent.absolutePos(this, p);
    }

    //package-visible
    char getOpening()
    {
        return opening;
    }

    public void focusBefore()
    {
        parent.backwardAtStart(this);        
    }

    public void focusAfter()
    {
        parent.forwardAtEnd(this);        
    }

    @Override
    public Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end)
    {
        return content.getAllStartEndPositionsBetween(start, end);
    }

    @Override
    public Stream<InfixStructured<?, ?>> getAllExpressions()
    {
        return content.getAllExpressions();
    }

    // package-visible
    void highlightBrackets(boolean on)
    {
        JavaFXUtil.setPseudoclass("bj-bracket-highlight", on, openingLabel, closingLabel);
    }

    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        content.setView(oldView, newView, animate, Optional.empty());
        JavaFXUtil.setPseudoclass("bj-java-preview", newView == View.JAVA_PREVIEW, openingLabel, closingLabel);
    }

    @Override
    public boolean isAlmostBlank()
    {
        return content.isAlmostBlank();
    }

    @Override
    public void notifyLostFocus(StructuredSlotField except)
    {
        content.notifyLostFocus(except);
    }

    // package-visible
    Node positionParentPos(CaretPos pos)
    {
        return parent.positionCaret(pos);
    }

    boolean isInSelection()
    {
        return parent != null && parent.isInSelection();
    }

    @Override
    public void setEditable(boolean editable)
    {
        content.setEditable(editable);
    }

    @Override
    public boolean isNumericLiteral()
    {
        return content.isNumericLiteral();
    }

    // package-visible
    void notifyIsMethodParams(boolean isMethodParams)
    {
        // We don't allow breaks before opening bracket of method params:
        HangingFlowPane.setBreakBefore(openingLabel, !isMethodParams);
    }

    @Override
    public int calculateEffort()
    {
        return content.calculateEffort();
    }

    //package-visible
    INFIX getParent()
    {
        return parent;
    }

    @Override
    public Stream<Node> makeDisplayClone(InteractionManager editor)
    {
        return Utility.concat(
            Stream.of(JavaFXUtil.cloneLabel(openingLabel, editor.getFontCSS())),
            content.makeDisplayClone(editor),
            Stream.of(JavaFXUtil.cloneLabel(closingLabel, editor.getFontCSS()))
        );
    }
}
