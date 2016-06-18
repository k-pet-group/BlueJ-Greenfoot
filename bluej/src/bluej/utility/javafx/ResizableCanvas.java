package bluej.utility.javafx;

import javafx.scene.canvas.Canvas;

/**
 * Created by neil on 17/06/2016.
 */
public class ResizableCanvas extends Canvas
{
    @Override
    public boolean isResizable()
    {
        return true;
    }

    @Override
    public void resize(double width, double height)
    {
        setWidth(width);
        setHeight(height);
    }

    @Override
    public double minWidth(double height)
    {
        return 0;
    }

    @Override
    public double minHeight(double width)
    {
        return 0;
    }

    @Override
    public double prefWidth(double height)
    {
        return 0;
    }

    @Override
    public double prefHeight(double width)
    {
        return 0;
    }

    @Override
    public double maxWidth(double height)
    {
        return Double.MAX_VALUE;
    }

    @Override
    public double maxHeight(double width)
    {
        return Double.MAX_VALUE;
    }
}
