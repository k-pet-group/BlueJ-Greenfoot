/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
import javafx.animation.FadeTransition;
import javafx.beans.Observable;
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

    public Sidebar(Label label, Node node)
    {
        this.label = label;
        this.node = node;
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
            DoubleBinding sidebarOffset = Bindings.max(0.0, containedPaneOffset).add(sidebar.topMarginProperty());
            
            // Make the width of the text (which after rotate, will be height of sideways text)
            // be linked to the height of the whole children-block area:
            sidebar.maxWidthProperty().bind(
                    Bindings.min(
                      containedPane.heightProperty().subtract(sidebarOffset)
                      , editor.getObservableViewportHeight().subtract(sidebar.topMarginProperty())
                      ).subtract(25.0) /* bottom margin */);
            // Rotate the text:
            {            
                Rotate r = new Rotate(-90.0, 0.0, 0.0);
                Translate t = new Translate();
                // Then move down so right edge is at top:
                t.xProperty().bind(sidebar.widthProperty().add(sidebarOffset).negate());
                t.yProperty().bind(sidebar.leftMarginProperty());
                sidebar.getTransforms().addAll(r, t);
            }
            
            // Even when the max width of the sidebar is zero, JavaFX still displays
            // the ellipsis (on Mac OS X, at least), so make it invisible when it's too small:
            sidebar.visibleProperty().bind(sidebar.maxWidthProperty().greaterThanOrEqualTo(10.0));
        }
        return new Sidebar(sidebar, g);
    }
}
