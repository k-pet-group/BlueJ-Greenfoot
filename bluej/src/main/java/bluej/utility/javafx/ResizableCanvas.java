package bluej.utility.javafx;

import javafx.scene.canvas.Canvas;

/**
 * A simple extenstion of the JavaFX Canvas class that allows the Canvas
 * to be resized.  You can also specify an action to be run on resize
 * (typically, a redraw action).
 */
public class ResizableCanvas extends Canvas
{
    protected FXRunnable onResize;

    public ResizableCanvas()
    {
        this(null);
    }

    /**
     * Supply an optional action to be run when the canvas is resized.
     *
     * @param onResize If not null, will be runs whenever the canvas is resized.
     */
    public ResizableCanvas(FXRunnable onResize)
    {
        this.onResize = onResize;
    }

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
        if (onResize != null)
            onResize.run();
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
