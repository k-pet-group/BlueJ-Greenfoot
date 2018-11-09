/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2017,2018 Michael Kölling and John Rosenberg

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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
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
    public static enum ArrowLocation
    {
        /* Arrow at top, content expands downwards from top */
        TOP,
        /* Arrow at left, content expands rightwards from left */
        LEFT;
    }

    private final ArrowLocation arrowLocation;
    private final TriangleArrow arrow;
    private final BorderPane arrowWrapper;
    protected final double arrowPadding = 1;
    private final DoubleProperty transitionProperty = new SimpleDoubleProperty(1.0) {
        @Override
        protected void invalidated()
        {
            requestLayout();
        }
    };
    private final Node content;
    private final Rectangle clipRect;
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private FXPlatformRunnable cancelHover;
    private Animation animation;

    public UntitledCollapsiblePane(Node content, ArrowLocation arrowLocation, boolean startCollapsed)
    {
        this.content = content;
        this.arrowLocation = arrowLocation;
        this.arrow = new TriangleArrow(isVertical() ? Orientation.VERTICAL : Orientation.HORIZONTAL);
        this.arrowWrapper = new BorderPane(arrow);
        arrowWrapper.setPadding(new Insets(arrowPadding));
        this.clipRect = new Rectangle();
        getChildren().addAll(arrowWrapper, content);
        content.setClip(clipRect);
        if (isVertical())
            clipRect.widthProperty().bind(widthProperty());
        else
            clipRect.heightProperty().bind(heightProperty());
        JavaFXUtil.addStyleClass(this, "untitled-pane");

        expanded.set(!startCollapsed);
        arrowWrapper.setOnMouseClicked(e -> {
            expanded.set(!expanded.get());
        });

        // We use a delay before setting our hover class, to avoid flashes as the user moves their mouse cursor
        // across the screen, to and from the frame catalogue:
        arrowWrapper.setOnMouseEntered(e -> {
            // Shouldn't be non-null, but just in case:
            if (cancelHover != null)
                cancelHover.run();
            cancelHover = JavaFXUtil.runAfter(Duration.millis(200), () -> JavaFXUtil.setPseudoclass("bj-hover-long", true, arrowWrapper));
        });
        arrowWrapper.setOnMouseExited(e -> {
            if (cancelHover != null)
            {
                cancelHover.run();
                cancelHover = null;
            }
            JavaFXUtil.setPseudoclass("bj-hover-long", false, arrowWrapper);
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
        JavaFXUtil.addChangeListener(expanded, this::runAnimation);
    }

    /**
     * A constructor which takes the main parameters needed for the UntitledCollapsiblePane
     * besides a consumer function to be executed when the arrow is clicked.
     *
     * @param content         A node has all contents to be added to the Pane
     * @param arrowLocation   Where to place the arrow (e.g. Top, Left).
     * @param startCollapsed  The initial folding state (true it is collapsed, false expanded)
     * @param listener        A consumer function to be executed when the arrow is clicked
     */
    public UntitledCollapsiblePane(Node content, ArrowLocation arrowLocation, boolean startCollapsed, FXPlatformConsumer<? super Boolean> listener)
    {
        this(content, arrowLocation, startCollapsed);
        arrowWrapper.setOnMouseClicked(e -> {
            expanded.set(!expanded.get());
            listener.accept(expanded.get());
        });
    }

    private boolean isVertical()
    {
        return arrowLocation == ArrowLocation.TOP;
    }

    private void runAnimation(boolean toExpanded)
    {
        if (animation != null)
        {
            animation.stop();
        }
        double dest = toExpanded ? 1 : 0;
        double destScale = toExpanded ? -1 : 1;
        animation = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(transitionProperty, dest), new KeyValue(arrow.scaleProperty(), destScale)));
        animation.setOnFinished(ev -> {
            animation = null;
        });
        animation.playFromStart();
    }

    public BooleanProperty expandedProperty()
    {
        return expanded;
    }

    @Override
    protected void layoutChildren()
    {
        this.layoutChildren(0, 0, getWidth(), getHeight());
    }

    private void layoutChildren(double x, double y,
                                            final double w, final double h)
    {

        // header
        final double arrowSize;

        if (isVertical())
        {
            arrowSize = snapSizeY(arrow.TRIANGLE_DEPTH + 2 * arrowPadding);
            arrowWrapper.resize(w, arrowSize);
        }
        else
        {
            arrowSize = snapSizeX(arrow.TRIANGLE_DEPTH + 2 * arrowPadding);
            arrowWrapper.resize(arrowSize, h);
        }
        positionInArea(arrowWrapper, x, y,
                isVertical() ? w : arrowSize, isVertical() ? arrowSize : h, 0, HPos.CENTER, VPos.CENTER);

        // content size, in the dimension in which we collapse (height if arrow at top, else width)
        final double contentSize;
        if (isVertical())
        {
            contentSize = snapSizeY(h - arrowSize);
        }
        else
        {
            contentSize = snapSizeX(w - arrowSize);
        }

        if (isVertical())
        {
            y += arrowSize;
            content.resize(w, contentSize);
            clipRect.setHeight(contentSize);
        }
        else
        {
            x += arrowSize;
            content.resize(contentSize, h);
            clipRect.setWidth(contentSize);
        }

        positionInArea(content, x, y,
            isVertical() ? w : contentSize, isVertical() ? contentSize : h, /*baseline ignored*/0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return isVertical() ? content.prefWidth(height) : content.prefWidth(height) * getTransition() + arrow.TRIANGLE_DEPTH + 2 * arrowPadding;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return isVertical() ? content.prefHeight(width) * getTransition() + arrow.TRIANGLE_DEPTH + 2 * arrowPadding : content.prefHeight(width);
    }

    @Override
    protected double computeMinWidth(double height)
    {
        return isVertical() ? content.minWidth(height) : content.minWidth(height) * getTransition() + arrow.TRIANGLE_DEPTH + 2 * arrowPadding;
    }

    @Override
    protected double computeMinHeight(double width)
    {
        return isVertical() ? content.minHeight(width) * getTransition() + arrow.TRIANGLE_DEPTH + 2 * arrowPadding : content.minHeight(width);
    }

    private double getTransition()
    {
        return transitionProperty.get();
    }

    public void addArrowWrapperStyleClass(String styleClass)
    {
        arrowWrapper.getStyleClass().add(styleClass);
    }
}
