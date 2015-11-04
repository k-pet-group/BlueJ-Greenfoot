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

import bluej.stride.generic.Frame.View;
import bluej.utility.javafx.SharedTransition;

import javafx.beans.value.ObservableStringValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;

// Package-visible
interface ExpressionSlotComponent
{

    default void focusAtStart() { focusAtPos(getStartPos()); }

    default void focusAtEnd() { focusAtPos(getEndPos()); }

    Node focusAtPos(CaretPos caretPos);
    
    boolean isFocused();
    
    // Only returns true if it is a plain field, and is empty
    default boolean isFieldAndEmpty() { return false; }
    
    CaretPos getStartPos();
    CaretPos getEndPos();

    TextOverlayPosition calculateOverlayPos(CaretPos subPos);

    // If allowDescend is false, and this is compound, should return blank
    PosAndDist getNearest(double sceneX, double sceneY, boolean allowDescend, boolean anchorInItem);

    // If compound, should return null.
    CaretPos getSelectIntoPos(boolean atEnd);

    // If from is null, then use start.  If to is null, use end.  Both null thus means whole slot.
    String getCopyText(CaretPos from, CaretPos to);

    // Returns null if doesn't have focus
    CaretPos getCurrentPos();

    List<InfixExpression.CaretPosMap> mapCaretPosStringPos(InfixExpression.IntCounter cur, boolean javaString);

    Region getNodeForPos(CaretPos subPos);

    // For testing purposes:
    String testingGetState(CaretPos pos);

    void insertSuggestion(CaretPos subPos, String name, List<String> params);

    String getJavaCode();

    ObservableList<? extends Node> getComponents();

    // See InfixExpression.getAllStartEndPositionsBetween for an explanation
    Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end);
    
    Stream<InfixExpression> getAllExpressions();
    
    ObservableStringValue textProperty();

    // Toggles between read-only Java preview and normal view 
    void setView(View oldView, View newView, SharedTransition animate);

    boolean isAlmostBlank();

    void notifyLostFocus(ExpressionSlotField except);

    void setEditable(boolean editable);

    boolean isNumericLiteral();
}
