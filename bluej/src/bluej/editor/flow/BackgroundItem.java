package bluej.editor.flow;

import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A background item on a TextLine, usually a scope background, but can also be
 * a marker for the current step line or a breakpoint line.
 */
@OnThread(Tag.FXPlatform)
class BackgroundItem extends Region
{
    private final double x;
    private final double width;

    /**
     * Create a background item with the given X position and width, and the given background fills.  This constructor sets the item to be unmanaged.
     */
    BackgroundItem(double x, double width, BackgroundFill... backgroundFills)
    {
        this.x = x;
        this.width = width;
        setManaged(false);
        setBackground(new Background(backgroundFills));
    }

    /**
     * Size the line to have Y position of zero, and the given height.  Must be called whenever the height changes, and before the background is shown for the first time.
     */
    void sizeToHeight(double height)
    {
        resizeRelocate(x, 0, width, height);
    }
}
