package bluej.debugmgr.inspector;

import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
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
    private final ObservableDoubleValue lineWidth;

    public ObjectBackground(double cornerSize, ObservableDoubleValue lineWidth)
    {
        this.cornerSize = cornerSize;
        this.lineWidth = lineWidth;
        JavaFXUtil.addChangeListenerPlatform(widthProperty(), w -> redrawContent());
        JavaFXUtil.addChangeListenerPlatform(heightProperty(), h -> redrawContent());
        JavaFXUtil.addChangeListenerPlatform(lineWidth, d -> redrawContent());

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

        final Paint fill = new javafx.scene.paint.Color(227.0 / 255.0, 71.0 / 255.0, 71.0 / 255.0, 1.0);
        gc.setFill(fill);
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(lineWidth.get());
        gc.strokeRoundRect(0, 0, w, h, cornerSize, cornerSize);
    }
}
