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
    }

    private void redrawContent()
    {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        gc.clearRect(0, 0, w, h);
        
        final Paint fill = new javafx.scene.paint.Color(227.0 / 255.0, 71.0 / 255.0, 71.0 / 255.0, 1.0);
        gc.setFill(fill);
        double l = lineWidth.get();
        // Need a slightly increased corner size for the fill so that it doesn't show up outside the stroke:
        gc.fillRoundRect(l, l, w-2*l, h-2*l, cornerSize*1.1, cornerSize*1.1);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(lineWidth.get());
        gc.strokeRoundRect(l, l, w-2*l, h-2*l, cornerSize, cornerSize);
    }
}
