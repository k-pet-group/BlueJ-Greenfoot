package bluej.editor.base;

import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import threadchecker.OnThread;
import threadchecker.Tag;

@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class LineContainer extends Region
{
    private final LineDisplay lineDisplay;
    private final boolean lineWrapping;

    public LineContainer(LineDisplay lineDisplay, boolean lineWrapping)
    {
        this.lineDisplay = lineDisplay;
        this.lineWrapping = lineWrapping;
        JavaFXUtil.addStyleClass(this, "line-container");
    }

    @Override
    protected void layoutChildren()
    {
        double y = snapPositionY(lineDisplay.getFirstVisibleLineOffset());
        if (!lineWrapping)
        {
            double height = snapSizeY(lineDisplay.calculateLineHeight());
            for (Node child : getChildren())
            {
                if (child instanceof MarginAndTextLine)
                {
                    double nextY = snapPositionY(y + height);
                    child.resizeRelocate(0, y, Math.max(getWidth(), child.prefWidth(-1.0)), nextY - y);
                    y = nextY;
                }
            }
        } else
        {
            for (Node child : getChildren())
            {
                if (child instanceof MarginAndTextLine)
                {
                    double height = snapSizeY(child.prefHeight(getWidth()));
                    double nextY = snapPositionY(y + height);
                    child.resizeRelocate(0, y, getWidth(), nextY - y);
                    y = nextY;
                }
            }
        }
    }

    @Override
    @OnThread(Tag.FX)
    public ObservableList<Node> getChildren()
    {
        return super.getChildren();
    }

    public double getTextDisplayWidth()
    {
        return getWidth() - lineDisplay.textLeftEdge();
    }
}
