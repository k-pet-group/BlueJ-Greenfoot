package bluej.utility.javafx;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
    private static double ARROW_WIDTH = 14;
    private static double ARROW_HEIGHT = 10;
    private final Canvas arrow = new Canvas(ARROW_WIDTH, ARROW_HEIGHT); 
    private final DoubleProperty transitionProperty = new SimpleDoubleProperty(1.0) {
        @Override
        protected void invalidated()
        {
            requestLayout();
        }
    };
    private final Node content;
    private final Rectangle clipRect;
    private Animation animation;

    public UntitledCollapsiblePane(Node content, boolean startCollapsed)
    {
        this.content = content;
        this.clipRect = new Rectangle();
        getChildren().addAll(arrow, content);
        content.setClip(clipRect);
        clipRect.widthProperty().bind(widthProperty());
        JavaFXUtil.addStyleClass(this, "untitled-pane");

        arrow.setCursor(Cursor.HAND);
        GraphicsContext gc = arrow.getGraphicsContext2D();
        gc.setFill(Color.DARKGRAY);
        gc.fillPolygon(new double[] { 1, ARROW_WIDTH * 0.5, ARROW_WIDTH - 1, 1}, new double[] {ARROW_HEIGHT - 1, 1, ARROW_HEIGHT - 1, ARROW_HEIGHT - 1}, 4);
        Scale scale = new Scale(1.0, -1.0, ARROW_WIDTH / 2.0, ARROW_HEIGHT / 2.0);
        arrow.getTransforms().add(scale);
        arrow.setOnMouseClicked(e -> {
            if (animation != null)
            {
                animation.stop();
            }
            double dest = getTransition() > 0 ? 0 : 1;
            double destScale = getTransition() > 0 ? 1 : -1;
            animation = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(transitionProperty, 1.0 - dest), new KeyValue(scale.yProperty(), -destScale)),
                new KeyFrame(Duration.millis(300), new KeyValue(transitionProperty, dest), new KeyValue(scale.yProperty(), destScale)));
            animation.playFromStart();
        });
        if (startCollapsed)
        {
            transitionProperty.set(0.0);
            scale.yProperty().set(1.0);
        }
    }

    @Override
    protected void layoutChildren()
    {
        this.layoutChildren(0, 0, getWidth(), getHeight());
    }

    private void layoutChildren(final double x, double y,
                                            final double w, final double h) {

        // header
        double headerHeight = snapSize(ARROW_HEIGHT);

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
        return content.prefHeight(width) * getTransition() + ARROW_HEIGHT;
    }

    @Override
    protected double computeMinWidth(double height)
    {
        return content.minWidth(height);
    }

    @Override
    protected double computeMinHeight(double width)
    {
        return content.minHeight(width) * getTransition() + ARROW_HEIGHT;
    }

    private double getTransition()
    {
        return transitionProperty.get();
    }
}
