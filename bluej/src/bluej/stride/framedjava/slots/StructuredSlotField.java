/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018,2020 Michael Kölling and John Rosenberg
 
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import bluej.utility.javafx.*;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import bluej.stride.framedjava.slots.InfixStructured.CaretPosMap;
import bluej.stride.framedjava.slots.InfixStructured.IntCounter;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A single text field in an expression slot.  This usually features
 * one alphanumeric identifier or a blank.  If you have say "getWorld().setX(10+3)"
 * then the fields will be square bracketed as follows:
 * [getWorld]([])[].[setX]([10]+[3])[]
 * 
 * This component encapsulates the actual GUI item rather than inheriting from it.
 */
// Package-visible
class StructuredSlotField implements StructuredSlotComponent
{
    /**
     * The actual GUI component.  Delegates most of its behaviour back to this class.
     */
    private final DelegableScalableTextField<StructuredSlotField> field;
    /**
     * The immediate parent expression of this field.  Is often not the
     * same as the top-level expression of the whole slot.  e.g.
     * setFoo(10+3) -- the field for the 10 has as its parent the brackets,
     * not the slot as a whole.
     */
    private final InfixStructured parent;

    /**
     * Creates an StructuredSlotField with the given parent and content
     * @param parent Parent of this field
     * @param content Initial content of this field
     * @param stringLiteral Whether we are the field directly inside a string literal.
     *                      This affects some of the behaviour.
     */
    public StructuredSlotField(InfixStructured parent, String content, boolean stringLiteral)
    {
        this.parent = parent;
        field = new DelegableScalableTextField<>(parent, this, content);
        JavaFXUtil.addStyleClass(field, "expression-slot-field");
        if (stringLiteral)
            JavaFXUtil.addStyleClass(field, "expression-string-literal");
        
        FXPlatformRunnable shrinkGrow = () -> {
            boolean suggesting = parent.suggestingFor(StructuredSlotField.this);
            if (field.isFocused() == false && !suggesting)
            {
                notifyLostFocus(null);
            }
            else
            {
                // If we have focus, don't shrink, stay visible:
                JavaFXUtil.setPseudoclass("bj-transparent", false, field);
            }
        };
        
        field.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends Boolean> observable, Boolean old, Boolean focused)
            {
                shrinkGrow.run();
                // In effect it moved, by gaining or losing focus:
                parent.caretMoved();
                if (focused)
                {
                    parent.getSlot().notifyGainFocus(StructuredSlotField.this);
                }
            }
        });
        
        field.textProperty().addListener(new ChangeListener<String>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)            
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                    String newValue)
            {
                shrinkGrow.run();
                if (!stringLiteral)
                {
                    updateBreaks();
                }
            }
        });

        field.promptTextProperty().addListener(new ChangeListener<String>() {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)            
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                    String newValue)
            {
                shrinkGrow.run();
                if (!stringLiteral)
                {
                    updateBreaks();
                }
            }
        });
        
        JavaFXUtil.initializeCustomHelp(parent.getEditor(), field, this::calculateTooltip, true);

        // Also run it to determine initial size, but must run later after parent has
        // initialised and we are in the scene:
        JavaFXUtil.onceInScene(field, () -> {
            shrinkGrow.run();
            if (parent.getSlot() != null) // Can be null during testing
            {
                field.setContextMenu(AbstractOperation.MenuItems.makeContextMenu(parent.getSlot().getMenuItems(true)));
            }
        });
        if (!stringLiteral)
            updateBreaks();
    }

    /**
     * HangingFlowPane, which will always be our parent container, keeps track
     * of which items it can break before.  This method updates whether you can
     * break before this field, following the rule that: you can break before any
     * field that has non-empty text or non-empty prompt text.
     * 
     * This method should not be called if we are a string literal, because
     * an empty string literal field behaves differently to an empty expression field.
     */
    private void updateBreaks()
    {
        // You can break before any field that has non-empty text or non-empty prompt text:
        HangingFlowPane.setBreakBefore(field, !getText().isEmpty() || !field.getPromptText().isEmpty());
    }

    /** Calculates the scene X position of the given location within the field */
    private double calculateSceneX(CaretPos pos)
    {
        return field.calculateSceneX(pos.index);
    }

    /** Calculates the scene X position of the given Y offset with the field */
    private double calculateSceneY(double y)
    {
        return field.localToScene(new Point2D(0, y)).getY();
    }
    
    @Override
    public TextOverlayPosition calculateOverlayPos(CaretPos pos)
    {
        return TextOverlayPosition.fromScene(calculateSceneX(pos),
                    calculateSceneY(0.0),
                    calculateSceneY(field.getBaselineOffset()),
                    calculateSceneY(field.getHeight()), this);
    }

    /**
     * Calculate the position of the end of the text field.  Note that
     * this may not be the same as calculateOverlayPos applied to
     * the last position with a field.  For example, if the field is empty
     * but has prompt text, calculateOverlayPos would return the left-hand
     * side of the field (being the position for 0, the last position in the field),
     * whereas this method would return the visible right-hand edge.
     */
    public TextOverlayPosition calculateOverlayEnd()
    {
        return TextOverlayPosition.fromScene(field.localToScene(field.getBoundsInLocal()).getMaxX(),
                calculateSceneY(0.0),
                calculateSceneY(field.getBaselineOffset()),
                calculateSceneY(field.getHeight()), this);
    }

    @Override
    public void focusAtStart()
    {
        focusAt(0);
    }

    /** Focus the field, and position cursor at the given position */
    private void focusAt(int i)
    {
        field.requestFocus();
        field.positionCaret(i);        
    }


    @Override
    public void focusAtEnd()
    {
        focusAt(field.getLength());       
    }


    @Override
    public Node focusAtPos(CaretPos caretPos)
    {
        focusAt(caretPos.index);
        return field;
    }

    @Override
    public CaretPos getStartPos()
    {
        return new CaretPos(0, null);
    }

    @Override
    public CaretPos getEndPos()
    {
        return new CaretPos(field.getLength(), null);
    }

    public boolean isEmpty()
    {
        return field.getText().equals("");
    }
    
    public void requestFocus()
    {
        field.requestFocus();        
    }
    
    @Override
    public PosAndDist getNearest(double sceneX, double sceneY, boolean allowDescend, boolean anchorInItem)
    {
        double topYDist = Math.abs(calculateSceneY(0.0) - sceneY);
        // In the case that two slots touch vertically, one's topY is the same as the other's bottomY,
        // which means that the caret position is equally far from each slot.  This minor adjustment
        // of the bottom position (subtracting 1) fixes this issue by favouring the correct slot:
        double bottomYDist = Math.abs(calculateSceneY(field.getHeight() - 1.0) - sceneY);
        PosAndDist nearest = new PosAndDist();
        for (int j = 0; j <= field.getLength(); j++)
        {
            CaretPos pos = new CaretPos(j, null);
            double xDist = calculateSceneX(pos) - sceneX;
            double dist = Math.hypot(xDist, Math.min(topYDist, bottomYDist));
            nearest = PosAndDist.nearest(nearest, new PosAndDist(pos, dist));
        }
        
        // We also check the extremities of the field for their position.
        // In the case where the text field is blank but has prompt text, the right-hand side
        // of the field can be quite different to the final caret position (which is position 0,
        // over on the left-hand side of the slot)
        Bounds b = field.localToScene(field.getBoundsInLocal());
        nearest = PosAndDist.nearest(nearest, new PosAndDist(new CaretPos(0, null), Math.hypot(b.getMinX() - sceneX, Math.min(topYDist, bottomYDist))));
        nearest = PosAndDist.nearest(nearest, new PosAndDist(new CaretPos(field.getLength(), null), Math.hypot(b.getMaxX() - sceneX, Math.min(topYDist, bottomYDist))));
                
        return nearest;
    }


    @Override
    public CaretPos getSelectIntoPos(boolean atEnd)
    {
        return new CaretPos(atEnd ? field.getLength() : 0, null);
    }
    
    public String getText()
    {
        return field.getText();
    }
    
    public void setText(String s, StructuredSlot.ModificationToken token)
    {
        token.check();
        field.setText(s);
    }
    
    @Override
    public String getCopyText(CaretPos from, CaretPos to)
    {
        int start = from == null ? 0 : from.index;
        int end = to == null ? field.getLength() : to.index;
        return field.getText().substring(start, end);
    }
    
    @Override
    public String getJavaCode()
    {
        return field.getText();
    }
    
    @Override
    public CaretPos getCurrentPos()
    {
        if (field.isFocused())
        {
            return new CaretPos(field.getCaretPosition(), null);
        }
        return null;
    }
    
    public void setPromptText(String s)
    {
        field.setPromptText(s);        
    }
    
    @Override
    public ObservableList<Region> getComponents()
    {
        return FXCollections.observableArrayList(field);
    }

    @Override
    public List<CaretPosMap> mapCaretPosStringPos(IntCounter len, boolean javaString)
    {
        String text = getText();
        List<CaretPosMap> r = Collections.singletonList(new CaretPosMap(null, len.counter, len.counter + text.length()));
        len.counter += text.length();
        return r;
    }


    @Override
    public Region getNodeForPos(CaretPos pos)
    {
        return field;
    }


    @Override
    public String testingGetState(CaretPos pos)
    {
        if (pos == null)
            return "{" + field.getText() + "}";
        else
        {
            return "{" + field.getText().substring(0, pos.index) + "$" + field.getText().substring(pos.index) + "}";
        }
    }
    
    @Override
    public boolean isFocused()
    {
        return field.isFocused();
    }
    
    @Override
    public boolean isFieldAndEmpty() {
        return field.getText().isEmpty();
    }
    
    public ObjectProperty<EventHandler<? super KeyEvent>> onKeyPressedProperty() {
        return field.onKeyPressedProperty();
    }
    
    public DoubleExpression heightProperty()
    {
        return field.heightProperty();
    }

    @Override
    public void insertSuggestion(CaretPos p, String name, char opening, List<String> params, StructuredSlot.ModificationToken token)
    {
        if (params != null)
            throw new IllegalArgumentException();
        setText(getText().substring(0, p.index) + name + getText().substring(p.index), token);
    }

    @OnThread(Tag.FXPlatform)
    private void calculateTooltip(FXConsumer<String> tooltipConsumer)
    {
        // We show empty string for tooltip if the slot is not empty and not focused:
        if (!getText().equals("") && !isFocused())
            tooltipConsumer.accept("");
        else
            parent.calculateTooltipFor(this, tooltipConsumer);
    }

    @Override
    public Stream<TextOverlayPosition> getAllStartEndPositionsBetween(CaretPos start, CaretPos end)
    {
        if (start == null)
            start = getStartPos();
        if (end == null)
            end = getEndPos();
        return Stream.of(calculateOverlayPos(start), calculateOverlayPos(end));
    }


    @Override
    public Stream<InfixStructured<?, ?>> getAllExpressions()
    {
        return Stream.empty();
    }

    public void addEventHandler(EventType<MouseEvent> mouseEvent, EventHandler<? super MouseEvent> eventHandler)
    {
        field.addEventHandler(mouseEvent, eventHandler);        
    }

    public ObservableStringValue textProperty()
    {
        return field.textProperty();
    }

    public void setPseudoclass(String name, boolean on)
    {
        JavaFXUtil.setPseudoclass(name, on, field);
    }
    
    @Override
    public void setView(View oldView, View newView, SharedTransition animate)
    {
        field.setEditable(newView == View.NORMAL);
        field.setDisable(newView != View.NORMAL);

        if (newView == Frame.View.JAVA_PREVIEW)
        {
            animate.addOnStopped(() -> {
                JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, field);
            });
        }
        else
        {
            JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, field);
        }
    }
    
    @OnThread(Tag.FXPlatform)
    public void cut()
    {
        field.cut();
    }
    
    @OnThread(Tag.FXPlatform)
    public void copy()
    {
        field.copy();
    }
    
    @OnThread(Tag.FXPlatform)
    public void paste()
    {
        field.paste();
    }

    @Override
    public boolean isAlmostBlank() { return isEmpty(); }

    @Override
    public void notifyLostFocus(StructuredSlotField except)
    {
        // We have lost focus -- are we collapsible?
        boolean collapsible = parent.isCollapsible(StructuredSlotField.this);
        boolean empty = field.getText().isEmpty() && field.getPromptText().isEmpty();
        // TODO allow collapsing if we are only white space
        if (empty && collapsible)
        {
            // We need to become transparent:
            JavaFXUtil.setPseudoclass("bj-transparent", true, field);
        }
        else
        {
            // If are mandatory and empty we stay visible even when unfocused
            // So we go transparent if we are optional, or non empty
            JavaFXUtil.setPseudoclass("bj-transparent", collapsible || !field.getText().isEmpty(), field);
        }
    }

    @Override
    public void setEditable(boolean editable)
    {
        field.setDisable(!editable);
    }

    @OnThread(Tag.FXPlatform)
    public void nextWord()
    {
        field.nextWord();
    }

    @OnThread(Tag.FXPlatform)
    public void previousWord()
    {
        field.previousWord();
    }

    @Override
    public boolean isNumericLiteral()
    {
        return getText().matches("\\A\\d*\\z");
    }

    @Override
    public int calculateEffort()
    {
        return Math.min(3, field.getText().length());
    }

    @Override
    public Stream<Node> makeDisplayClone(InteractionManager editor)
    {
        TextField f = new TextField();
        f.textProperty().bind(field.textProperty());
        f.prefWidthProperty().bind(field.prefWidthProperty());
        JavaFXUtil.bindList(f.getStyleClass(), field.getStyleClass());
        JavaFXUtil.bindPseudoclasses(f, field.getPseudoClassStates());
        JavaFXUtil.setPseudoclass("bj-pinned", true, f);
        f.styleProperty().bind(field.styleProperty().concat(editor.getFontCSS()));
        return Stream.of(f);
    }
}
