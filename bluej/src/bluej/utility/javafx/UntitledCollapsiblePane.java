/*
 This file is part of the BlueJ program.
 Copyright (C) 2016 Michael Kölling and John Rosenberg

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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Like TitledPane, but does not have a title section, and instead
 * just has an arrow to expand/collapse above the content.
 * 
 * It turned out that styling TitledPane to act like this was nigh-on impossible,
 * so I borrowed some of its workings to make our own simpler version.
 */
public class UntitledCollapsiblePane extends Pane
{
    private final TriangleArrow arrow = new TriangleArrow(Orientation.VERTICAL);
    private final DoubleProperty transitionProperty = new SimpleDoubleProperty(1.0) {
        @Override
        protected void invalidated()
        {
            requestLayout();
        }
    };
    private final Node content;
    private final Rectangle clipRect;
    private final BooleanProperty collapsed = new SimpleBooleanProperty();
    private Animation animation;

    public UntitledCollapsiblePane(Node content, boolean startCollapsed)
    {
        this.content = content;
        this.clipRect = new Rectangle();
        getChildren().addAll(arrow, content);
        content.setClip(clipRect);
        clipRect.widthProperty().bind(widthProperty());
        JavaFXUtil.addStyleClass(this, "untitled-pane");

        collapsed.set(startCollapsed);
        arrow.setOnMouseClicked(e -> {
            toggleCollapsed();
        });
        if (startCollapsed)
        {
            transitionProperty.set(0.0);
            arrow.scaleProperty().set(1.0);
        }
        else
        {
            transitionProperty.set(1.0);
            arrow.scaleProperty().set(-1.0);
        }
    }

    private void toggleCollapsed()
    {
        if (animation != null)
        {
            animation.stop();
        }
        double dest = getTransition() > 0 ? 0 : 1;
        double destScale = getTransition() > 0 ? 1 : -1;
        animation = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(transitionProperty, 1.0 - dest), new KeyValue(arrow.scaleProperty(), -destScale)),
            new KeyFrame(Duration.millis(300), new KeyValue(transitionProperty, dest), new KeyValue(arrow.scaleProperty(), destScale)));
        animation.setOnFinished(ev -> {
            collapsed.set(dest == 0.0);
            animation = null;
        });
        animation.playFromStart();
    }

    public BooleanExpression collapsedProperty()
    {
        return collapsed;
    }


    public void expand()
    {
        if (collapsed.get() && animation == null)
            toggleCollapsed();
    }

    @Override
    protected void layoutChildren()
    {
        this.layoutChildren(0, 0, getWidth(), getHeight());
    }

    private void layoutChildren(final double x, double y,
                                            final double w, final double h) {

        // header
        double headerHeight = snapSize(arrow.TRIANGLE_DEPTH);

        arrow.resize(w, headerHeight);
        positionInArea(arrow, x, y,
            w, headerHeight, 0, HPos.CENTER, VPos.CENTER);

        // content
        double contentHeight = (h - headerHeight) * getTransition();
        contentHeight = snapSize(contentHeight);

        y += snapSize(headerHeight);
        content.resize(w, contentHeight);
        clipRect.setHeight(contentHeight);
        positionInArea(content, x, y,
            w, contentHeight, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return content.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return content.prefHeight(width) * getTransition() + arrow.TRIANGLE_DEPTH;
    }

    @Override
    protected double computeMinWidth(double height)
    {
        return content.minWidth(height);
    }

    @Override
    protected double computeMinHeight(double width)
    {
        return content.minHeight(width) * getTransition() + arrow.TRIANGLE_DEPTH;
    }

    private double getTransition()
    {
        return transitionProperty.get();
    }
}
