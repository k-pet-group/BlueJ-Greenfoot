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
package bluej.stride.slots;

import java.util.List;
import java.util.stream.Stream;

import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.ScalableHeightLabel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import bluej.stride.generic.Frame;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;

/**
 * A SlotLabel is a wrapper class for a ScalableHeightLabel (i.e. our subclass
 * of JavaFX's Label, which allows growing and shrinking the height)
 * which can be used as a HeaderItem in FrameContentRow.
 * 
 * The actual label is kept private, and this class is mainly filled with delegates
 * which expose the necessary methods of Label.
 */
public class SlotLabel implements HeaderItem, Styleable, CopyableHeaderItem
{
    private final ScalableHeightLabel l;
    // It's important that the same list is returned every time from getComponents:
    private final ObservableList<Node> list;
    private boolean animateCaption;

    public SlotLabel(String content, String... styleClasses)
    {
        l = new ScalableHeightLabel(content, false);
        JavaFXUtil.addStyleClass(l, "slot-label");
        JavaFXUtil.addStyleClass(l, styleClasses);
        list = FXCollections.observableArrayList(l);
    }

    @Override
    public ObservableList<Node> getComponents()
    {
        return list;
    }

    @Override
    public EditableSlot asEditable()
    {
        return null;
    }

    @Override
    public String getTypeSelector()
    {
        return l.getTypeSelector();
    }

    @Override
    public String getId()
    {
        return l.getId();
    }

    @Override
    public ObservableList<String> getStyleClass()
    {
        return l.getStyleClass();
    }

    @Override
    public String getStyle()
    {
        return l.getStyle();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData()
    {
        return l.getCssMetaData();
    }

    @Override
    public Styleable getStyleableParent()
    {
        return l.getStyleableParent();
    }

    @Override
    public ObservableSet<PseudoClass> getPseudoClassStates()
    {
        return l.getPseudoClassStates();
    }

    public void setStyle(String style)
    {
        l.setStyle(style);
    }

    public StringProperty textProperty()
    {
        return l.textProperty();
    }

    public BooleanProperty disableProperty()
    {
        return l.disableProperty();
    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> h)
    {
        l.setOnMouseClicked(h);        
    }

    public void setOnMouseDragged(EventHandler<? super MouseEvent> value)
    {
        l.setOnMouseDragged(value);
    }

    public void setCursor(Cursor c)
    {
        l.setCursor(c);
    }

    public void setOpacity(double d)
    {
        l.setOpacity(0.0);
    }
    
    public Node getNode()
    {
        return l;
    }

    public void setText(String string)
    {
        l.setText(string);
    }

    public DoubleProperty rotateProperty()
    {
        return l.rotateProperty();
    }

    public DoubleProperty prefWidthProperty()
    {
        return l.prefWidthProperty();
    }

    public DoubleProperty minWidthProperty()
    {
        return l.minWidthProperty();
    }

    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animate)
    {
        if (!animateCaption)
            return;

        if (newView == Frame.View.JAVA_PREVIEW)
        {
            animate.addOnStopped(() -> {
                JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, l);
            });
        }
        else
        {
            JavaFXUtil.setPseudoclass("bj-java-preview", newView == Frame.View.JAVA_PREVIEW, l);
            JavaFXUtil.setPseudoclass("bj-birdseye", newView.isBirdseye(), l);
            animate.addOnStopped(() -> {
                l.minWidthProperty().unbind();
                l.setMinWidth(0);
            });
        }

    }
    
    public void shrinkHorizontally(SharedTransition animate)
    {
        l.setTextOverrun(OverrunStyle.CLIP);
        final double cur = l.getWidth();
        l.minWidthProperty().set(0.0);
        l.maxWidthProperty().unbind();
        l.maxWidthProperty().bind(animate.getProgress().negate().add(1.0).multiply(cur));
    }
    
    public void growHorizontally(SharedTransition animate)
    {
        l.maxWidthProperty().unbind();
        l.maxWidthProperty().bind(animate.getProgress().multiply(JavaFXUtil.measureString(l, l.getText())));
        animate.addOnStopped(() -> {
            l.setMinWidth(Region.USE_COMPUTED_SIZE);
            l.maxWidthProperty().unbind();
            l.maxWidthProperty().set(Region.USE_COMPUTED_SIZE);
        });
    }
    
    public void shrinkVertically(SharedTransition animate)
    {
        l.setTextOverrun(OverrunStyle.CLIP);
        l.shrinkToNothingWith(animate, true);
    }

    public void shrinkInstantly()
    {
        l.setTextOverrun(OverrunStyle.CLIP);
        l.setToNothing();
    }


    public void growVertically(SharedTransition animate)
    {
        l.growToFullHeightWith(animate, true);
    }

    public String getText()
    {
        return l.getText();
    }

    public ObjectProperty<Font> fontProperty()
    {
        return l.fontProperty();
    }

    public double measureString(String text)
    {
        return JavaFXUtil.measureString(l, text);
    }

    public void setOnDragDetected(EventHandler<? super MouseEvent> value)
    {
        l.setOnDragDetected(value);
    }

    public void setOnMousePressed(EventHandler<? super MouseEvent> value)
    {
        l.setOnMousePressed(value);
    }

    public void setOnMouseReleased(EventHandler<? super MouseEvent> value)
    {
        l.setOnMouseReleased(value);
    }

    public void setLeftPadding(double amount)
    {
        Insets p = l.getPadding();
        l.setPadding(new Insets(p.getTop(), p.getRight(), p.getBottom(), amount));
    }

    public void setAnimateCaption(boolean animateCaption)
    {
        this.animateCaption = animateCaption;
    }

    @Override
    public Stream<Label> makeDisplayClone(InteractionManager editor)
    {
        Label copy = JavaFXUtil.cloneLabel(l, editor.getFontCSS());
        return Stream.of(copy);
    }

}
