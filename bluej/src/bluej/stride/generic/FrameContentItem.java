package bluej.stride.generic;

import java.util.Optional;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;

import bluej.stride.slots.HeaderItem;
import bluej.utility.javafx.SharedTransition;

/**
 * An interface for an item within a Frame; either a FrameContentRow or FrameContentCanvas
 */
public interface FrameContentItem
{
    /**
     * Gets all header items, all the way down in the children
     */
    public Stream<HeaderItem> getHeaderItemsDeep();
    /**
     * Gets header items that are directly in this item
     */
    public Stream<HeaderItem> getHeaderItemsDirect();

    /**
     * Gets bounds in terms of the scene
     */
    public Bounds getSceneBounds();

    /**
     * Gets the canvas within this item, if any
     */
    public Optional<FrameCanvas> getCanvas();

    public boolean focusLeftEndFromPrev();
    public boolean focusRightEndFromNext();
    public boolean focusTopEndFromPrev();
    public boolean focusBottomEndFromNext();

    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animation);

    public Node getNode();
}
