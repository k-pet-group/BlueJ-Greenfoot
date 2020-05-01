/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2018,2020 Michael Kölling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.stride.generic;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.utility.Debug;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import bluej.stride.slots.CopyableHeaderItem;
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
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A frame content item with a stack pane containing an overlay and a flow pane.
 * 
 * A frame content row is used for the content of most frames (e.g. frame headers).
 * Essentially, anything except documentation pane and frame canvases sits within
 * a frame content row.  All HeaderItem and EditableSlot things sit inside the
 * HangingFlowPane in a FrameContentRow.
 */
public class FrameContentRow implements FrameContentItem, SlotParent<HeaderItem>
{
    /**
     * The frame that this FrameContentRow lives in
     */
    private final Frame parentFrame;

    /**
     * The flow pane containing all the non-overlay content.  Its content
     * is taken from headerRowComponents
     */
    private final HangingFlowPane headerRow = new HangingFlowPane();

    /**
     * An overlay which spans the whole FrameContentRow, for showing red
     * error underlines.
     */
    private final ErrorUnderlineCanvas headerOverlay;

    /**
     * A list bound to the header items which will then be used as the contents
     * of the headerRow.  The headerRow contains lots of Nodes.  Each HeaderItem
     * may produce many Nodes (e.g. an expression slot is one HeaderItem but usually
     * has many Nodes, e.g. a text field, an operator label, another text field.)
     */
    private final ObservableList<HeaderItem> headerRowComponents = FXCollections.observableArrayList();
    
    /**
     * The stack pane which contains the headerRow (underneath) and headerOverlay (on top).
     */
    private final StackPane stackPane;

    /**
     * Keeps track of whether the mouse is currently hovering over this FrameContentRow or not
     */
    private final BooleanProperty mouseHovering = new SimpleBooleanProperty(false);

    /**
     * Quick constructor to create a FrameContentRow with the given content.
     * Has "anon-" style prefix.
     */
    public FrameContentRow(Frame parentFrame, HeaderItem... items)
    {
        this(parentFrame, "anon-");
        headerRowComponents.setAll(items);
    }

    /**
     * Standard constructor for FrameContentRow.  Creates an empty one, with the
     * given CSS style class prefix.
     * 
     * @param parentFrame The parent of this FrameContentRow
     * @param stylePrefix The style-class prefix.
     */
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

        stackPane.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> mouseHovering.set(true));
        stackPane.addEventFilter(MouseEvent.MOUSE_EXITED, e -> mouseHovering.set(false));
    }

    /**
     * Sets the margin for the headerRow flow pane within the stack pane.
     */
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

    /**
     * Gets all the HeaderItems contained within this FrameContentRow which are
     * instances of EditableSlot (as determined by HeaderItem::asEditable).
     */
    public Stream<EditableSlot> getSlotsDirect()
    {
        return getHeaderItemsDirect().map(HeaderItem::asEditable).filter(x -> x != null);
    }

    /**
     * Gets the overlay which spans this FrameContentRow (and thus also spans all contained
     * HeaderItems).
     */
    public ErrorUnderlineCanvas getOverlay()
    {
        return headerOverlay;
    }

    /**
     * Sets the HeaderItems which are to be contained within this FrameContentRow, replacing
     * all previous content.
     */
    public void setHeaderItems(List<HeaderItem> headerItems)
    {
        headerRowComponents.setAll(headerItems);
    }

    /**
     * Binds the contents of this FrameContentRow to the concatenation of the given
     * list of lists of HeaderItems.
     */
    public void bindContentsConcat(ObservableList<ObservableList<? extends HeaderItem>> src)
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

    /**
     * Gets the next editable slot after the given position (exclusive), or null if there is none.
     */
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

    /**
     * Gets the previous editable slot before the given position (exclusive), or null if there is none.
     */
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
    public boolean backspaceAtStart(HeaderItem src)
    {
        return parentFrame.backspaceAtStart(this, src);
    }

    @Override
    @OnThread(Tag.FXPlatform)
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
        // This is called when they e.g. press up at the beginning of the canvas
        // after us.  In this case we still want to focus the start of the row, not the end:
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

    /**
     * Adds the given overlay to the stack pane, in front of the error underline canvas.
     */
    public void addOverlay(Node item)
    {
        stackPane.getChildren().add(item);
    }

    /**
     * Gets the X coordinate (in scene coordinates) of the first Node in the flow pane.
     */
    public double getLeftFirstItem()
    {
        Node n = headerRow.getChildren().stream().findFirst().get();
        return n.localToScene(n.getBoundsInLocal()).getMinX();
    }

    public DoubleExpression flowPaneHeight()
    {
        return headerRow.heightProperty();
    }

    public void applyCss()
    {
        headerRow.applyCss();
    }

    /**
     * Makes a display clone of this FrameContentRow.  This is used for making a copy of
     * method headers for displaying the pinned method header.
     * @return A StackPane containing a display-identical (but immutable) copy of this FrameContentRow.
     */
    public StackPane makeDisplayClone(InteractionManager editor)
    {
        HangingFlowPane hfpCopy = new HangingFlowPane();
        hfpCopy.getChildren().setAll(headerRowComponents.stream().flatMap(c ->
                ((CopyableHeaderItem)c).makeDisplayClone(editor)
        ).collect(Collectors.toList()));
        hfpCopy.prefWidthProperty().bind(headerRow.widthProperty());
        hfpCopy.alignmentProperty().bind(headerRow.alignmentProperty());
        hfpCopy.hangingIndentProperty().bind(headerRow.hangingIndentProperty());
        JavaFXUtil.bindList(hfpCopy.getStyleClass(), headerRow.getStyleClass().filtered(c -> !c.equals("header-row")));
        StackPane.setMargin(hfpCopy, new Insets(0, 6, 0, 6));
        StackPane paneCopy = new StackPane(hfpCopy);
        return paneCopy;
    }

    public void setVisible(boolean visible)
    {
        stackPane.setVisible(visible);
        stackPane.setManaged(visible);
    }

    public ObservableBooleanValue mouseHoveringProperty()
    {
        return mouseHovering;
    }

    public final List<ExtensionDescription> getExtensions()
    {
        if (parentFrame.getHeaderRow() == this)
            return parentFrame.getAvailableExtensions(null, null);
        else
            return Collections.emptyList();
    }

    @OnThread(Tag.FXPlatform)
    public void notifyModifiedPress(KeyCode c)
    {
        List<ExtensionDescription> possibles = getExtensions().stream().filter(ext -> ext.validFor(ExtensionSource.MODIFIER) && ("" + ext.getShortcutKey()).equals(c.getName().toLowerCase())).collect(Collectors.toList());
        if (possibles.size() == 1)
        {
            possibles.get(0).activate();
        }
        else if (possibles.size() > 1)
        {
            Debug.message("Ambiguous alt keypress for " + parentFrame.getClass() + " for " + c);
        }
    }


    /**
     * Called when the editor font size has changed, to redraw the overlay
     */
    @OnThread(Tag.FXPlatform)
    public void fontSizeChanged()
    {
        headerOverlay.redraw();
    }
}
