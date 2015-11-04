package bluej.utility.javafx.binding;

import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Bounds;
import javafx.scene.control.ScrollPane;

/**
 * An observable binding to the height of a ScrollPane's viewport height.
 */
public class ViewportHeightBinding extends DoubleBinding
{
    private ScrollPane scroll;

    public ViewportHeightBinding(ScrollPane scroll)
    {
        this.scroll = scroll;
        bind(scroll.viewportBoundsProperty());
    }

    @Override
    protected double computeValue()
    {
        Bounds viewBound = scroll.getViewportBounds();
        return viewBound == null ? 0.0 : viewBound.getHeight();
    }
}
