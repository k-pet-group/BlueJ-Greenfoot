package bluej.stride.generic;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.beans.binding.DoubleExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.Border;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotParent;
import bluej.utility.Utility;
import bluej.utility.javafx.ErrorUnderlineCanvas;
import bluej.utility.javafx.HangingFlowPane;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import bluej.utility.javafx.binding.ConcatListBinding;
import bluej.utility.javafx.binding.ConcatMapListBinding;

/**
 * A frame content item with a stack pane containing an overlay, and a border pane containing a flow pane as its center.
 */
public class FrameContentRow implements FrameContentItem, SlotParent<HeaderItem>
{
    private final Frame parentFrame;
    private final HangingFlowPane headerRow = new HangingFlowPane();
    private final ErrorUnderlineCanvas headerOverlay;
    private final ObservableList<HeaderItem> headerRowComponents = FXCollections.observableArrayList();
    private final StackPane stackPane;

    public FrameContentRow(Frame parentFrame, HeaderItem... items)
    {
        this(parentFrame, "anon-");
        headerRowComponents.setAll(items);
    }

    public FrameContentRow(Frame parentFrame, String stylePrefix)
    {
        this.parentFrame = parentFrame;

        stackPane = new StackPane();
        headerOverlay = new ErrorUnderlineCanvas(stackPane);
        stackPane.getChildren().addAll(headerRow, headerOverlay.getNode());

        headerRow.setMinWidth(200.0);

        headerRow.getStyleClass().addAll("header-row", stylePrefix + "header-row");
        headerRow.setAlignment(Pos.CENTER_LEFT);
        ConcatMapListBinding.bind(headerRow.getChildren(), headerRowComponents, HeaderItem::getComponents);

        StackPane.setMargin(headerRow, new Insets(0, 6, 0, 6));
    }
    
    public void setMargin(Insets insets)
    {
        StackPane.setMargin(headerRow, insets);
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDeep()
    {
        return getHeaderItemsDirect(); // HeaderItems cannot nest
    }

    @Override
    public Stream<HeaderItem> getHeaderItemsDirect()
    {
        return headerRowComponents.stream();
    }

    public Stream<EditableSlot> getSlotsDirect()
    {
        return getHeaderItemsDirect().map(HeaderItem::asEditable).filter(x -> x != null);
    }

    public ErrorUnderlineCanvas getOverlay()
    {
        return headerOverlay;
    }



    public void setHeaderItems(List<HeaderItem> headerItems)
    {
        headerRowComponents.setAll(headerItems);
    }

    public void bindContentsConcat(ObservableList<ObservableList<HeaderItem>> src)
    {
        ConcatListBinding.bind(headerRowComponents, src);
    }

    @Override
    public Bounds getSceneBounds()
    {
        return stackPane.localToScene(stackPane.getBoundsInLocal());
    }

    @Override
    public void focusLeft(HeaderItem src)
    {
        int index = headerRowComponents.indexOf(src);
        if (index < 0) {
            throw new IllegalStateException("Child slot not found in slot parent");
        }

        EditableSlot s = prevFocusableBefore(index);
        if (s != null) {
            s.requestFocus(Focus.RIGHT);
        }
        else {
            parentFrame.focusLeft(this);
        }
    }

    @Override
    public void focusRight(HeaderItem src)
    {
        int index = headerRowComponents.indexOf(src);
        if (index < 0) {
            throw new IllegalStateException("Child slot not found in slot parent");
        }

        EditableSlot s = nextFocusableAfter(index);
        if (s != null) {
            s.requestFocus(Focus.LEFT);
        }
        else {
            parentFrame.focusRight(this);
        }
    }


    private EditableSlot nextFocusableAfter(int curSlot)
    {
        for (int i = curSlot + 1; i < headerRowComponents.size(); i++) {
            EditableSlot s = headerRowComponents.get(i).asEditable();
            if (s != null && s.isEditable()) {
                return s;
            }
        }
        return null;
    }

    private EditableSlot prevFocusableBefore(int curSlot)
    {
        for (int i = curSlot - 1; i >= 0; i--) {
            EditableSlot s = headerRowComponents.get(i).asEditable();
            if (s != null && s.isEditable()) {
                return s;
            }
        }
        return null;
    }

    @Override
    public void focusEnter(HeaderItem src)
    {
        parentFrame.focusEnter(this);
    }

    @Override
    public void escape(HeaderItem src)
    {
        parentFrame.escape(this, src);
    }

    @Override
    public void focusDown(HeaderItem src)
    {
        parentFrame.focusDown(this);
    }

    @Override
    public void focusUp(HeaderItem src, boolean cursorToEnd)
    {
        parentFrame.focusUp(this, cursorToEnd);
    }

    @Override
    public boolean backspaceAtStart(HeaderItem src)
    {
        return parentFrame.backspaceAtStart(this, src);
    }

    @Override
    public boolean deleteAtEnd(HeaderItem src)
    {
        return parentFrame.deleteAtEnd(this, src);
    }


    @Override
    public void setView(Frame.View oldView, Frame.View newView, SharedTransition animation)
    {
        getHeaderItemsDirect().forEach(item -> item.setView(oldView, newView, animation));
        animation.addOnStopped(() -> JavaFXUtil.runAfter(Duration.millis(100), headerOverlay::redraw));
    }

    @Override
    public boolean focusBottomEndFromNext()
    {
        // Still focus start of row:
        return focusLeftEndFromPrev();
    }

    @Override
    public boolean focusLeftEndFromPrev()
    {
        Optional<EditableSlot> last = getSlotsDirect().filter(EditableSlot::isEditable).findFirst();
        if (last.isPresent())
        {
            last.get().requestFocus(Focus.LEFT);
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean focusRightEndFromNext()
    {
        Optional<EditableSlot> last = Utility.findLast(getSlotsDirect().filter(EditableSlot::isEditable));
        if (last.isPresent())
        {
            last.get().requestFocus(Focus.RIGHT);
            return true;
        }
        else
            return false;
    }

    @Override
    public boolean focusTopEndFromPrev()
    {
        // Focus start of row:
        return focusLeftEndFromPrev();
    }

    @Override
    public Optional<FrameCanvas> getCanvas()
    {
        return Optional.empty();
    }

    public final DoubleExpression flowPaneWidth()
    {
        return headerRow.widthProperty();
    }

    @Override
    public Region getNode()
    {
        return stackPane;
    }

    public void setSnapToPixel(boolean b)
    {
        stackPane.setSnapToPixel(b);
        headerRow.setSnapToPixel(b);
    }

    public void addOverlay(Node item)
    {
        stackPane.getChildren().add(item);
    }


    public double getLeftFirstItem()
    {
        Node n = headerRow.getChildren().stream().findFirst().get();
        return n.localToScene(n.getBoundsInLocal()).getMinX();
    }

    public DoubleExpression flowPaneHeight()
    {
        return headerRow.heightProperty();
    }

    public Border getBorder()
    {
        return headerRow.getBorder();
    }

    public void applyCss()
    {
        headerRow.applyCss();
    }
}
