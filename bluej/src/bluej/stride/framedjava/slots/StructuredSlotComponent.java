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

import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.SharedTransition;

import javafx.beans.value.ObservableStringValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An expression slot has a hierarchy of InfixStructured items, starting with
 * one at the top-level.  Each InfixStructured has a list of contained
 * StructuredSlotComponent items, with optional Operator items between each.
 * There are three types of StructuredSlotComponent:
 *  - BracketedStructured, which has brackets (round or square) around an
 *     InfixStructured item (recurse back to the beginning of this comment...)
 *  - StringLiteralExpression, which has inside it an StructuredSlotField
 *  - StructuredSlotField, which is a single text field with text content.
 *  
 *  This interface has all the operations which are common to all its subclasses.
 */
// Package-visible
interface StructuredSlotComponent
{
    /**
     * Place the focus at the first valid position in this component
     * (i.e. request focus and place caret)
     */
    default void focusAtStart() { focusAtPos(getStartPos()); }

    /**
     * Place the focus at the last valid position in this component
     * (i.e. request focus and place caret)
     */
    default void focusAtEnd() { focusAtPos(getEndPos()); }

    /**
     * Request focus and place caret at the given position, traversing
     * down the hierarchy as necessary (@see {@link CaretPos}).  The position
     * should be relative to this component, not to the slot as a whole.  Returns
     * the graphical component which was focused as a result of this call.
     */
    Node focusAtPos(CaretPos caretPos);

    /**
     * Returns true if this component, either directly (if it's an StructuredSlotField)
     * or indirectly via one of its children (in the other cases) currently
     * has GUI focus.
     */
    boolean isFocused();

    /**
     * Only returns true if it is a plain field, and is empty.
     */
    default boolean isFieldAndEmpty() { return false; }

    /**
     * Gets the first caret position which is valid in this component.
     * Position will be relative to this component, not to the slot as a whole.
     */
    CaretPos getStartPos();
    /**
     * Gets the last caret position which is valid in this component.
     * Position will be relative to this component, not to the slot as a whole.
     */
    CaretPos getEndPos();

    /**
     * For the given caret position (relative to this component), returns
     * the @see {@link TextOverlayPosition} for that caret position.
     */
    TextOverlayPosition calculateOverlayPos(CaretPos subPos);

    // If allowDescend is false, and this is compound, should return blank
    PosAndDist getNearest(double sceneX, double sceneY, boolean allowDescend, boolean anchorInItem);

    // If compound item, should return null.
    CaretPos getSelectIntoPos(boolean atEnd);

    // If from is null, then use start.  If to is null, use end.  Both null thus means whole slot.
    String getCopyText(CaretPos from, CaretPos to);

    /*
     * Gets the current caret position relative to this component.
     * Returns null if this component (and its subcomponents) doesn't have focus.
     */
    CaretPos getCurrentPos();
    
    // See InfixStructured.mapCaretPosStringPos for more info
    List<InfixStructured.CaretPosMap> mapCaretPosStringPos(InfixStructured.IntCounter cur, boolean javaString);

    /**
     * Gets the graphical node associated with a particular caret position
     * (relative to this component).
     */
    Region getNodeForPos(CaretPos subPos);

    // For testing purposes:
    String testingGetState(CaretPos pos);

    /**
     * Inserts a suggestion from auto-complete.
     *  @param subPos The caret position (relative to the current
     * component) for insertion.
     * @param name The text to insert
     * @param opening
     * @param params The parameters to insert in round brackets after the text,
     *               or null if none.  (Empty list inserts empty brackets, null
     */
    @OnThread(Tag.FXPlatform)
    void insertSuggestion(CaretPos subPos, String name, char opening, List<String> params, StructuredSlot.ModificationToken token);

    /**
     * Gets the Java code generated by this component.  This will be similar
     * to the current text content, but some items are converted, e.g.
     * <: operator to instanceof, .. to a call to range method.
     */
    String getJavaCode();

    /**
     * Gets the graphical components (all the way down the hierarchy) used
     * to display this list, in order.
     */
    ObservableList<? extends Node> getComponents();

    /**
     * See InfixStructured.getAllStartEndPositionsBetween for an explanation
     */
    Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end);

    /**
     * Gets all the InfixStructured contained in this component, at any
     * level of the hierarchy.
     */
    Stream<InfixStructured<?, ?>> getAllExpressions();

    /**
     * The current text value of the component.  Broadly, this is both the text
     * currently being displayed, and the text you would have to type to
     * re-enter the expression.
     */
    String getText();

    /**
     * Toggles between read-only Java preview and normal view, using the given
     * shared transition.
     */
    void setView(View oldView, View newView, SharedTransition animate);

    /**
     * Checks if the component is "almost-blank".  A component is considered
     * almost-blank if all the individual text fields are blank, ignoring
     * any brackets, string quotes, operators or other structure.
     */
    boolean isAlmostBlank();

    /**
     * Notifies all the components that they do not have the focus,
     * except the given field (if non-null), which does have the focus.
     */
    void notifyLostFocus(StructuredSlotField except);

    /**
     * Sets whether the component (and all subcomponents) are currently editable.
     * If false, they are read-only.
     */
    void setEditable(boolean editable);

    /**
     * Checks if the item is a numeric literal.  Our simple rule is:
     * if all the fields within the component are numeric literals,
     * the whole component must be a numeric literal (regardless of operators).
     */
    boolean isNumericLiteral();

    /**
     * Calculates the effort (roughly, number of keypresses) required
     * to enter this expression (including sub-expressions)
     */
    int calculateEffort();

    /**
     * Makes a display clone (a la CopyableHeaderItem)
     */
    Stream<Node> makeDisplayClone(InteractionManager editor);
}
