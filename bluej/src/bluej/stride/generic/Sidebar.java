/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2021 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.generic;

import java.util.List;

import bluej.utility.javafx.FXConsumer;
import javafx.animation.FadeTransition;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import bluej.utility.javafx.JavaFXUtil;

public class Sidebar
{
    private final Node node;
    private final Label label;
    // This is an unused field but it stops the bindings being garbage collected:
    private final Binding[] bindingsToKeepHoldOf;

    public Sidebar(Label label, Node node, Binding... bindingsToKeepHoldOf)
    {
        this.label = label;
        this.node = node;
        this.bindingsToKeepHoldOf = bindingsToKeepHoldOf;
    }

    public Node getNode()
    {
        return node;
    }

    public StringProperty textProperty()
    {
        return label.textProperty();
    }

    public void setText(String value)
    {
        label.setText(value);
    }

    public Node getStyleable()
    {
        return label;
    }

    private static class SidebarLabel extends Label
    {
        private final SimpleStyleableDoubleProperty leftMarginProperty = new SimpleStyleableDoubleProperty(SIDEBAR_LABEL_LEFT_MARGIN_META_DATA);
        private final SimpleStyleableDoubleProperty leftMarginProperty() { return leftMarginProperty; }

        private final SimpleStyleableDoubleProperty topMarginProperty = new SimpleStyleableDoubleProperty(SIDEBAR_LABEL_TOP_MARGIN_META_DATA);
        private final SimpleStyleableDoubleProperty topMarginProperty() { return topMarginProperty; }

        private static final CssMetaData<SidebarLabel, Number> SIDEBAR_LABEL_LEFT_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-left-margin", SidebarLabel::leftMarginProperty);
        private static final CssMetaData<SidebarLabel, Number> SIDEBAR_LABEL_TOP_MARGIN_META_DATA =
            JavaFXUtil.cssSize("-bj-top-margin", SidebarLabel::topMarginProperty);
    
        private static final List<CssMetaData <? extends Styleable, ? > > cssMetaDataList =
            JavaFXUtil.extendCss(Label.getClassCssMetaData())
                    .add(SIDEBAR_LABEL_LEFT_MARGIN_META_DATA)
                    .add(SIDEBAR_LABEL_TOP_MARGIN_META_DATA)
                    .build();

        public static List <CssMetaData <? extends Styleable, ? > > getClassCssMetaData() { return cssMetaDataList; }
        @Override public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() { return getClassCssMetaData(); }

    }

    static Sidebar addSidebar(final InteractionManager editor,
            final Pane containedPane,
            final Observable observableBounds,
            String stylePrefix) {
        final SidebarLabel sidebar = new SidebarLabel();
        // To avoid sidebar getting in the way of other layout calculations
        // (such as min height during birdseye view), we put it in an
        // unmanaged group inside the AnchorPane:
        Group g = new Group(sidebar);
        g.setManaged(false);
        sidebar.getStyleClass().addAll("sidelabel", stylePrefix + "sidelabel");
        sidebar.setOpacity(0.0);
        containedPane.getChildren().add(g);
        if (editor == null || editor.getWindowOverlayPane() == null)
        {
            sidebar.setVisible(false);
            return new Sidebar(sidebar, g);
        }
        else
        {
            DoubleBinding containedPaneOffset = new DoubleBinding() {
                { super.bind(editor.getObservableScroll()); 
                  super.bind(editor.getObservableViewportHeight());
                  super.bind(observableBounds);
                  super.bind(containedPane.localToSceneTransformProperty()); }
                
                @Override
                protected double computeValue()
                {
                    // Positive if need to scroll sidebar downwards to stay on screen
                    return (int) -(editor.getWindowOverlayPane().sceneYToWindowOverlayY(containedPane.localToScene(0, 0).getY()));
                }
            };
            
            containedPaneOffset.addListener(new ChangeListener<Number>() {
    
                @Override
                public void changed(ObservableValue<? extends Number> arg0,
                        Number oldVal, Number newVal)
                {
                    // Play a fade if it has crossed zero:
                    if ((oldVal.doubleValue() <= 0 && newVal.doubleValue() > 0) || (oldVal.doubleValue() > 0 && newVal.doubleValue() <= 0))
                    {
                        FadeTransition ft = new FadeTransition(Duration.millis(200), sidebar);
                        ft.setToValue(newVal.doubleValue() <= 0 ? 0.0 : 1.0);
                        ft.play();
                    }
                }
            });
            
            sidebar.setEllipsisString("\u22EF");
            // Trim out the middle because trimming right looks weird
            // on a right-aligned text as you scroll, and trimming left
            // is a bit odd because you miss out the if part:
            sidebar.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            
            // We have a minimum offset of zero, so that when you scroll up quickly, it doesn't
            // sink into the header bar.  It will be turning invisible at that point
            // anyway            
            DoubleBinding containedPaneOffsetMax = Bindings.max(0.0, containedPaneOffset);
            DoubleBinding sidebarOffset = containedPaneOffsetMax.add(sidebar.topMarginProperty());

            // Make the width of the text (which after rotate, will be height of sideways text)
            // be linked to the height of the whole children-block area:
            DoubleBinding sidebarMaxWidth = new DoubleBinding()
            {
                {
                    super.bind(containedPane.heightProperty());
                    super.bind(sidebarOffset);
                    super.bind(editor.getObservableViewportHeight());
                    super.bind(sidebar.topMarginProperty());
                }
                @Override
                protected double computeValue()
                {
                    return Math.min(containedPane.getHeight() - sidebarOffset.get(),
                        editor.getObservableViewportHeight().get() - sidebar.topMarginProperty().get()
                        ) - 25 /* bottom margin */;
                }
            };
            sidebar.maxWidthProperty().bind(sidebarMaxWidth);
            // Rotate the text:
            {            
                Rotate r = new Rotate(-90.0, 0.0, 0.0);
                Translate t = new Translate();
                // Then move down so right edge is at top:
                
                // This is the original binding code, but there was a problem with it (read on): 
                // t.xProperty().bind(sidebar.widthProperty().add(sidebarOffset).negate());
                
                // First, you need to understand that there are two parts of JavaFX bindings.  There
                // is invalidation, which is used by bindings.  An invalidation causes all dependent bindings
                // to be updated, and is a sort of "I might have changed, recalculate in case" message.  Whereas
                // a change listener is only triggered if the value has actually changed.                
                
                // sidebarOffset is bound indirectly to an object property that involves a BoundingBox object.
                // This object is changed during layout to one with an identical value, but a new object
                // nonetheless.  This causes the property to be invalidated, even if the change listener does not fire.
                
                // (See javafx.beans.property.ObjectPropertyBase.set -- the comparison is newValue != oldValue,
                // not !Objects.equals(newValue, oldValue) as you might think. This causes an invalidation.
                // In contrast, com.sun.javafx.binding.ExpressionHelper.SingleChange.fireValueChangedEvent
                // uses Object.equals to check the values rather than the object identity.)
                
                // This invalidation chains all the way through the bindings until our Translate property,
                // which was then updated, even though nothing in the whole chain of bindings had actually
                // changed its value.  This causes an indirect infinite loop as it triggers another layout
                // which invalidates the binding and so on.
                
                // As it happens, the solution to this is the one we need to avoid the GC problem which was
                // also present in the original code.  By using change listeners, rather than bindings, we
                // depend on checking the value (which has not changed) and thus we avoid the loop:
                FXConsumer<Object> update = a -> t.setX(-(sidebar.getWidth() + sidebarOffset.get()));
                JavaFXUtil.addChangeListenerAndCallNow(sidebar.widthProperty(), update);
                JavaFXUtil.addChangeListenerAndCallNow(sidebarOffset, update);
                
                t.yProperty().bind(sidebar.leftMarginProperty());
                sidebar.getTransforms().addAll(r, t);
            }
            
            // Even when the max width of the sidebar is zero, JavaFX still displays
            // the ellipsis (on Mac OS X, at least), so make it invisible when it's too small:
            sidebar.visibleProperty().bind(sidebar.maxWidthProperty().greaterThanOrEqualTo(10.0));

            return new Sidebar(sidebar, g, containedPaneOffset, containedPaneOffsetMax, sidebarOffset, sidebarMaxWidth);
        }
    }
}
