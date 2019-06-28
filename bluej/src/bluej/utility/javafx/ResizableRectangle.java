package bluej.utility.javafx;

import javafx.scene.shape.Rectangle;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A Rectangle subclass that can be resized to any size during layout.
 */
@OnThread(Tag.FX)
public class ResizableRectangle extends Rectangle
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
