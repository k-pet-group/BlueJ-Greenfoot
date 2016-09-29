package bluej.debugmgr.inspector;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableCanvas;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 29/09/2016.
 */
@OnThread(Tag.FXPlatform)
public class ObjectBackground extends ResizableCanvas
{
    private final double cornerSize;
    private final double lineWidth;

    public ObjectBackground(double cornerSize, double lineWidth)
    {
        this.cornerSize = cornerSize;
        this.lineWidth = lineWidth;
        JavaFXUtil.addChangeListenerPlatform(widthProperty(), w -> redrawContent());
        JavaFXUtil.addChangeListenerPlatform(heightProperty(), h -> redrawContent());

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(10.0);
        clip.setArcHeight(10.0);
        setClip(clip);
    }

    private void redrawContent()
    {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        final Paint bottomColor =
            new LinearGradient(w / 2, h / 2, w / 2, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, new javafx.scene.paint.Color(227.0 / 255.0, 71.0 / 255.0, 71.0 / 255.0, 1.0)),
                new Stop(1.0, new javafx.scene.paint.Color(205.0 / 255.0, 39.0 / 255.0, 39.0 / 255.0, 1.0)));
        final Paint topColor =
            new LinearGradient(w / 2, 0, w / 2, h / 2, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, new javafx.scene.paint.Color(248.0 / 255.0, 120.0 / 255.0, 120.0 / 255.0, 1.0)),
                new Stop(1.0, new javafx.scene.paint.Color(231.0 / 255.0, 96.0 / 255.0, 96.0 / 255.0, 1.0)));

        gc.setFill(bottomColor);
        gc.fillRect(0, 0, w, h);
        gc.setFill(topColor);
        gc.fillOval(-2.0 * w, -2.5 * h, 5.0 * w, 3.0 * h);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(lineWidth);
        gc.strokeRoundRect(0, 0, w, h, cornerSize, cornerSize);
    }
}
