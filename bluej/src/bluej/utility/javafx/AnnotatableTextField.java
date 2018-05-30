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
package bluej.utility.javafx;

import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Duration;
import bluej.stride.slots.CompletionCalculator;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Utility;
import bluej.utility.javafx.ErrorUnderlineCanvas.UnderlineInfo;
import threadchecker.OnThread;
import threadchecker.Tag;


/**
 * Uses ScalableHeightTextField because we need it in ImportDialog
 */
public class AnnotatableTextField
{
    private final ScalableHeightTextField field;
    private final StackPane pane;
    private final ErrorUnderlineCanvas errorMarker;
    private final BooleanProperty fakeCaretShowing = new SimpleBooleanProperty(false);
    
    public AnnotatableTextField(String str, ErrorUnderlineCanvas overlay, boolean startHidden)
    {
        field = new ScalableHeightTextField(str, startHidden);
        field.focusedProperty().addListener((a, b, newVal) -> {
            // When focus changes, adjust bj-empty pseudoclass:
            JavaFXUtil.setPseudoclass("bj-empty", newVal.booleanValue() == false && field.getLength() == 0, field);                
        });
        
        if (overlay != null)
        {
            errorMarker = overlay;
            pane = new StackPane(field);
        }
        else
        {
            pane = new StackPane();
            errorMarker = new ErrorUnderlineCanvas(pane);
            pane.getChildren().addAll(field, errorMarker.getNode());
        }
        // Put the handler on the field:
        field.setOnMouseMoved(e -> {
            JavaFXUtil.setPseudoclass("bj-hyperlink", errorMarker.linkFromX(e.getSceneX()) != null, field);
        });
        field.setOnMouseClicked(e -> {
            // check for click on underlined region
            Utility.ifNotNull(errorMarker.linkFromX(e.getSceneX()), FXPlatformRunnable::run);
        });
        
        errorMarker.addExtraRedraw(g -> {
            if (fakeCaretShowing.get())
            {
                double x = errorMarker.sceneToLocal(field.localToScene(calculateCaretPosition(field.getCaretPosition()), 0)).getX();
                Paint p = g.getStroke();
                g.setStroke(Color.BLACK);
                g.strokeLine(x, 0, x, field.getHeight());
                g.setStroke(p);
            }
        });
        JavaFXUtil.addChangeListener(fakeCaretShowing, c -> JavaFXUtil.runNowOrLater(() -> errorMarker.redraw()));
        JavaFXUtil.addChangeListener(field.caretPositionProperty(), p -> { if (fakeCaretShowing.get()) JavaFXUtil.runNowOrLater(() -> errorMarker.redraw()); });
    }
    
    public AnnotatableTextField(ErrorUnderlineCanvas overlay)
    {
        this("", overlay);
    }
    
    public AnnotatableTextField(String str, ErrorUnderlineCanvas overlay)
    {
        this(str, overlay, false);
    }

    public Region getNode()
    {
        return pane;
    }
    
    public Node getFocusableNode()
    {
        return field;
    }

    public final StringProperty textProperty()
    {
        return field.textProperty();
    }

    public final ReadOnlyDoubleProperty widthProperty()
    {
        return field.widthProperty();
    }

    public final ReadOnlyDoubleProperty heightProperty()
    {
        return field.heightProperty();
    }

    public final ReadOnlyBooleanProperty focusedProperty()
    {
        return field.focusedProperty();
    }

    public void positionCaret(int pos)
    {
        field.positionCaret(pos);
    }

    public final StringProperty promptTextProperty()
    {
        return field.promptTextProperty();
    }
    
    public void replaceText(int start, int end, String text)
    {
        field.replaceText(start, end, text);
    }

    public final boolean isFocused()
    {
        return field.isFocused();
    }

    public void requestFocus()
    {
        field.requestFocus();
    }

    public final int getLength()
    {
        return field.getLength();
    }

    public final ReadOnlyIntegerProperty caretPositionProperty()
    {
        return field.caretPositionProperty();
    }

    public final DoubleProperty minWidthProperty()
    {
        return field.minWidthProperty();
    }

    public final ReadOnlyObjectProperty<IndexRange> selectionProperty()
    {
        return field.selectionProperty();
    }

    public final ReadOnlyIntegerProperty anchorProperty()
    {
        return field.anchorProperty();
    }

    public void end()
    {
        field.end();
    }

    public void deselect()
    {
        field.deselect();
    }

    public final DoubleProperty prefWidthProperty()
    {
        return field.prefWidthProperty();
    }

    public final ObjectProperty<Font> fontProperty()
    {
        return field.fontProperty();
    }
    
    public double measureString(String str, boolean includeInsets)
    {
        return JavaFXUtil.measureString(field, str, includeInsets, includeInsets);
    }

    public final IndexRange getSelection()
    {
        return field.getSelection();
    }

    public final ObjectProperty<EventHandler<? super KeyEvent>> onKeyPressedProperty()
    {
        return field.onKeyPressedProperty();
    }

    public final BooleanProperty editableProperty()
    {
        return field.editableProperty();
    }

    public final BooleanProperty disableProperty()
    {
        return field.disableProperty();
    }

    public Timeline getGrowToFullHeightTimeline(Duration dur)
    {
        return field.getGrowToFullHeightTimeline(dur);
    }

    public Timeline getShrinkToNothingTimeline(Duration dur)
    {
        return field.getShrinkToNothingTimeline(dur);
    }
    
    @OnThread(Tag.FXPlatform)
    public boolean executeCompletion(CompletionCalculator cc, int highlighted, int startOfCurWord)
    {
        return cc.execute(field, highlighted, startOfCurWord);
    }

    /**
     * Calculates the position of the caret when it is before the given index
     * in the text field, so passing 0 (before first char) will generally return
     * 0 (far left of field), passing 1 will return X position at end of first char
     * (before second char) and so on.
     * 
     * Ideally this would be calculated using model-to-view or font metrics or similar,
     * but since JavaFX doesn't seem to support these yet, we use the hack of
     * adjusting the text content of a hidden text field and measuring its width.
     * 
     * @param beforeIndex Character position, will calculate caret position before (to left) of char
     * @return The X coordinate (0 being far left) of the caret, our best guess.
     */
    protected double calculateCaretPosition(int beforeIndex)
    {
        if (beforeIndex == Integer.MAX_VALUE)
            return field.getWidth();
        
        double paddingLeft = field.getPadding().getLeft();
        double borderLeft = 0;
        if (field.getBorder() != null && field.getBorder().getInsets() != null)
            borderLeft = field.getBorder().getInsets().getLeft();
        // It seems that beforeIndex can report beyond the length of the text, so take the min of them:
        int index = Math.min(beforeIndex, field.getText().length());
        return paddingLeft + borderLeft + measureString(field.getText().substring(0, index), false) + 1 /* fudge factor*/;
    }

    protected double getBaseline()
    {
        // 3 is a hack/guess at the baseline
        double height = field.getHeight() - 3 - field.getPadding().getBottom();
        if (field.getBorder() != null && field.getBorder().getInsets() != null)
            height -= field.getBorder().getInsets().getBottom();
        return height;
    }

    public final ObjectProperty<Tooltip> tooltipProperty()
    {
        return field.tooltipProperty();
    }

    @OnThread(Tag.FXPlatform)
    public void clearUnderlines()
    {
        errorMarker.clearUnderlines();
        field.setCursor(null);
    }

    @OnThread(Tag.FXPlatform)
    public void drawUnderline(UnderlineInfo s, int startPosition, int endPosition, FXPlatformRunnable onClick)
    {
        errorMarker.addUnderline(s, startPosition, endPosition, onClick);
    }
    
    protected void addStyleClasses(String... styleClasses)
    {
        JavaFXUtil.addStyleClass(field, styleClasses);
    }
    
    protected void setPseudoclass(String pseudoClass, boolean on)
    {
        JavaFXUtil.setPseudoclass(pseudoClass, on, field);
    }

    @OnThread(Tag.FXPlatform)
    public void drawErrorMarker(EditableSlot s, int startPos, int endPos, boolean javaPos, FXPlatformConsumer<Boolean> onHover, ObservableBooleanValue visible)
    {
        // If we are trying to highlight an empty slot, highlight whole width
        if ((startPos == 0 && endPos == 0) || getLength() == 0)
            errorMarker.addErrorMarker(s, 0, Integer.MAX_VALUE, false, onHover, visible);
        else
            errorMarker.addErrorMarker(s, startPos, endPos, javaPos, onHover, visible);
    }

    @OnThread(Tag.FXPlatform)
    public void clearErrorMarkers(EditableSlot s)
    {
        errorMarker.clearErrorMarkers(s);
    }
    
    public void selectAll()
    {
        field.selectAll();
    }
    
    public void cut()
    {
        field.cut();
    }
    
    public void copy()
    {
        field.copy();
    }
    
    public void paste()
    {
        field.paste();
    }

    @OnThread(Tag.FXPlatform)
    public void backspace()
    {
        field.deletePreviousChar();
    }
    
    public void setContextMenu(ContextMenu menu)
    {
        field.setContextMenu(menu);
    }
    
    public void injectEvent(KeyEvent e)
    {
        field.fireEvent(e.copyFor(null, field));
    }
    
    public void setFakeCaretShowing(boolean showing)
    {
        fakeCaretShowing.set(showing);
    }

    public Font getFont()
    {
        return field.getFont();
    }

    public double measureString(String text, Font font)
    {
        return JavaFXUtil.measureString(field, text, font, true, true);
    }


    public boolean hasSelection()
    {
        return field.getAnchor() != field.getCaretPosition();
    }

    public ObservableList<String> getStyleClass()
    {
        return field.getStyleClass();
    }

    public ObservableSet<PseudoClass> getPseudoClassStates()
    {
        return field.getPseudoClassStates();
    }
    
    public StringProperty styleProperty()
    {
        return field.styleProperty();
    }
}
