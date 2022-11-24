/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021,2022  Michael Kolling and John Rosenberg

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
package bluej.editor.base;

import bluej.editor.base.TextLine.StyledSegment;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.fxmisc.wellbehaved.event.InputHandler.Result;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * A shared editor component to be used as the basis for rich text displays with carets and scrolling.
 * Used by the flow editor and by the terminal.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public abstract class BaseEditorPane extends Region
{
    protected static final Duration SCROLL_DELAY = Duration.millis(50);
    
    // The editor pane listener
    private final BaseEditorPaneListener editorPaneListener;
    private final boolean showLeftMargin;

    // The manager of the lines of text currently shown on screen in the editor
    protected final LineDisplay lineDisplay;
    // The listeners for the selection (anchor+caret) to be called when the anchor and/or caret change:
    private final ArrayList<SelectionListener> selectionListeners = new ArrayList<>();
    // The caret (cursor) shape
    private final Path caretShape;
    // The graphical component that contains the lines of text on screen in the editor
    private final LineContainer lineContainer;
    // The two scroll bars for the component (may not always be visible)
    private final ScrollBar verticalScroll;
    private final ScrollBar horizontalScroll;
    // Used to prevent programmatic changes to the scroll triggering listeners
    private boolean updatingScrollBarDirectly = false;

    // Are we queued to render again after scroll has happened?
    private boolean postScrollRenderQueued = false;
    // The pending amount to scroll by (mouse/touch scroll events are batched up)
    private double pendingScrollY;

    // Scroll bars can be turned off for testing and printing:
    private boolean allowScrollBars = true;

    // Forces the caret to show even when not focused (useful for autocomplete and similar)
    private boolean forceCaretShow = false;
    // Have we currently scheduled an update of the caret graphics?  If so, no need to schedule another.
    private boolean caretUpdateScheduled;
    // If we have currently scheduled an update of the caret graphics, will we ensure caret is visible?
    private boolean caretUpdateEnsureVisible;

    // For when the user is dragging the mouse (or just holding the button down with it stationary)
    // and the pointer is out of our bounds, requiring us to scroll:
    private static enum DragScroll { UP_FAST, UP, DOWN, DOWN_FAST }
    private boolean isDragScrollScheduled = false;
    // Null when there's no current drag scroll:
    private DragScroll offScreenDragScroll = null;
    private double offScreenDragX = 0;
    private double offScreenDragY = 0;

    protected BaseEditorPane(boolean showLeftMargin, BaseEditorPaneListener listener)
    {
        this.showLeftMargin = showLeftMargin;
        this.editorPaneListener = listener;
        caretShape = new Path();
        caretShape.getStyleClass().add("flow-caret");
        caretShape.setStroke(Color.RED);
        caretShape.setMouseTransparent(true);
        caretShape.setManaged(false);

        verticalScroll = new ScrollBar();
        verticalScroll.setOrientation(Orientation.VERTICAL);
        verticalScroll.setVisible(false);

        horizontalScroll = new ScrollBar();
        horizontalScroll.setOrientation(Orientation.HORIZONTAL);
        horizontalScroll.setVisible(false);
        lineDisplay = new LineDisplay(horizontalScroll.valueProperty(), PrefMgr.getEditorFontCSS(true), showLeftMargin, listener);
        lineContainer = new LineContainer(lineDisplay, false);


        JavaFXUtil.addChangeListenerPlatform(horizontalScroll.valueProperty(), v -> {
            // Prevent an infinite loop when we update scroll bar ourselves in render method:
            if (!updatingScrollBarDirectly)
            {
                updateRender(false);
            }
        });
        JavaFXUtil.addChangeListenerPlatform(verticalScroll.valueProperty(), v -> {
            // Prevent an infinite loop when we update scroll bar ourselves in render method:
            if (!updatingScrollBarDirectly)
            {
                lineDisplay.scrollTo(v.intValue(), (v.doubleValue() - v.intValue()) * -1 * lineDisplay.getLineHeight());
                updateRender(false);
            }
        });
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(lineContainer.widthProperty());
        clip.heightProperty().bind(lineContainer.heightProperty());
        lineContainer.setClip(clip);
        getChildren().setAll(lineContainer, verticalScroll, horizontalScroll);

        JavaFXUtil.addChangeListenerPlatform(lineContainer.heightProperty(), h -> JavaFXUtil.runAfterCurrent(() -> updateRender(false)));

        JavaFXUtil.addChangeListenerPlatform(widthProperty(), w -> updateRender(false));
        JavaFXUtil.addChangeListenerPlatform(heightProperty(), h -> updateRender(false));
        JavaFXUtil.addChangeListenerPlatform(focusedProperty(), f -> updateCaretVisibility());
        setAccessibleRole(AccessibleRole.TEXT_AREA);


        Nodes.addInputMap(this, InputMap.sequence(
            InputMap.process(KeyEvent.KEY_PRESSED, e -> {keyPressed(e); return Result.PROCEED;}),
            InputMap.consume(KeyEvent.KEY_TYPED, this::keyTyped),
            InputMap.consume(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            InputMap.consume(MouseEvent.MOUSE_DRAGGED, this::mouseDragged),
            InputMap.consume(MouseEvent.MOUSE_RELEASED, this::mouseReleased),
            InputMap.consume(MouseEvent.MOUSE_MOVED, this::mouseMoved)
            // Note: we deliberately do not handle scroll events here, and instead
            // handle them in MarginAndTextLine.  See the comments there for more info.
            // InputMap.consume(ScrollEvent.SCROLL, this::scroll)
        ));

        selectionListeners.add(new SelectionListener() {
            int oldCaretPos = 0;
            int oldAnchorPos = 0;
            @Override
            @OnThread(Tag.FXPlatform)
            public void selectionChanged(int caretPosition, int anchorPosition)
            {
                if (caretPosition != oldCaretPos)
                {
                    notifyAccessibleAttributeChanged(AccessibleAttribute.CARET_OFFSET);
                }
                if (Math.min(caretPosition, anchorPosition) != Math.min(oldCaretPos, oldAnchorPos))
                {
                    notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_START);
                }
                if (Math.max(caretPosition, anchorPosition) != Math.max(oldCaretPos, oldAnchorPos))
                {
                    notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTION_END);
                }
                oldAnchorPos = anchorPosition;
                oldCaretPos = caretPosition;
            }
        });
    }

    // The handler for the KeyEvent.KEY_PRESSED event:
    protected abstract void keyPressed(KeyEvent event);

    /**
     * Shows the context menu at the current position of the caret.
     */
    protected final void showContextMenuAtCaret()
    {
        Bounds sceneBounds = caretShape.localToScene(caretShape.getBoundsInLocal());
        Point2D scenePt = new Point2D(sceneBounds.getMaxX(), sceneBounds.getMaxY());
        Scene scene = getScene();
        Point2D screenPt = scenePt.add(scene.getWindow().getX() + scene.getX(), scene.getWindow().getY() + scene.getY());
        editorPaneListener.getContextMenuToShow(this).show(this, screenPt.getX(), screenPt.getY());
    }

    // The handler for the KeyEvent.KEY_TYPED event:
    protected abstract void keyTyped(KeyEvent event);
    // The handler for the MouseEvent.MOUSE_PRESSED event:
    protected abstract void mousePressed(MouseEvent event);
    // The handler for the MouseEvent.MOUSE_MOVED event:
    protected abstract void mouseMoved(MouseEvent event);

    // The handler for the MouseEvent.MOUSE_DRAGGED event:
    protected void mouseDragged(MouseEvent e)
    {
        if (e.getButton() == MouseButton.PRIMARY)
        {
            double y = e.getY();
            // If the user has the cursor more than this amount of pixels beyond the edge,
            // we speed up the drag:
            int fastDistance = 30;
            if (y > getHeight())
            {
                offScreenDragScroll = y - getHeight() > fastDistance ? DragScroll.DOWN_FAST : DragScroll.DOWN;
                y = getHeight() - 1;
            }
            else if (y < 0)
            {
                offScreenDragScroll = y < -fastDistance ? DragScroll.UP_FAST : DragScroll.UP;
                y = 0;
            }
            else
            {
                // Drag is within pane bounds, no need to scroll:
                offScreenDragScroll = null;
            }
            offScreenDragX = e.getX();
            offScreenDragY = y;
            // Don't update the anchor:
            getCaretPositionForLocalPoint(new Point2D(e.getX(), y)).ifPresent(p -> moveCaret(p, false));

            if (offScreenDragScroll != null && !isDragScrollScheduled)
            {
                JavaFXUtil.runAfter(Duration.millis(50), this::doDragScroll);
                isDragScrollScheduled = true;
            }
        }
    }

    // The handler for the MouseEvent.MOUSE_RELEASED event:
    protected void mouseReleased(MouseEvent e)
    {
        offScreenDragScroll = null;
    }

    /**
     * Moves the caret to the given position without moving the anchor. 
     * @param position The position to move to.  Must refer to this component.
     * @param ensureCaretVisible Whether to scroll to ensure the new caret position is visible.
     */
    protected abstract void moveCaret(EditorPosition position, boolean ensureCaretVisible);
    
    /**
     * Called regularly to continue to scroll up/down the file if the user is dragging and keeping
     * the mouse cursor out of the window.  Will reschedule itself (this is stopped if the user
     * releases the mouse button, in the mouseReleased button).
     */
    private void doDragScroll()
    {
        isDragScrollScheduled = false;
        if (offScreenDragScroll != null)
        {
            int amount = 0;
            switch (offScreenDragScroll)
            {
                case UP_FAST:
                    amount = 50;
                    break;
                case UP:
                    amount = 15;
                    break;
                case DOWN:
                    amount = -15;
                    break;
                case DOWN_FAST:
                    amount = -50;
                    break;
            }
            scroll(0, amount);
            getCaretPositionForLocalPoint(new Point2D(offScreenDragX, offScreenDragY)).ifPresent(p -> moveCaret(p, false));
            JavaFXUtil.runAfter(SCROLL_DELAY, this::doDragScroll);
            isDragScrollScheduled = true;
        }
    }

    /**
     * Get the caret position that corresponds to the XY point featured in the given mouse event.
     */
    public Optional<EditorPosition> getCaretPositionForMouseEvent(MouseEvent e)
    {
        return getCaretPositionForLocalPoint(new Point2D(e.getX(), e.getY()));
    }

    /**
     * Get the caret position that corresponds to the given local XY point (within this component).
     */
    protected Optional<EditorPosition> getCaretPositionForLocalPoint(Point2D localPoint)
    {
        return Optional.ofNullable(lineDisplay.getCaretPositionForLocalPoint(localPoint)).map(p -> makePosition(p[0], p[1]));
    }

    @Override
    protected final void layoutChildren()
    {
        double horizScrollHeight = horizontalScroll.isVisible() ? horizontalScroll.prefHeight(-1) : 0;
        horizontalScroll.resizeRelocate(0, getHeight() - horizScrollHeight, getWidth(), horizScrollHeight);
        double vertScrollWidth = verticalScroll.isVisible() ? verticalScroll.prefWidth(-1) : 0;
        verticalScroll.resizeRelocate(getWidth() - vertScrollWidth, 0, vertScrollWidth, getHeight() - horizScrollHeight);
        lineContainer.resizeRelocate(0, 0, getWidth() - vertScrollWidth, getHeight() - horizScrollHeight);
    }

    protected final void scroll(double deltaX, double deltaY)
    {
        int lineCount = getLineCount();
        updatingScrollBarDirectly = true;
        horizontalScroll.setValue(Math.max(horizontalScroll.getMin(), Math.min(horizontalScroll.getMax(), horizontalScroll.getValue() - deltaX)));
        updatingScrollBarDirectly = false;
        pendingScrollY += deltaY;
        if (!postScrollRenderQueued)
        {
            postScrollRenderQueued = true;
            JavaFXUtil.runAfter(SCROLL_DELAY, () -> {
                postScrollRenderQueued = false;
                lineDisplay.scrollBy(pendingScrollY, lineCount, lineContainer.getHeight());
                pendingScrollY = 0;
                updateRender(false);
            });
        }
    }


    /**
     * Schedules an update of the caret graphics after the next scene layout.
     * @param ensureCaretVisibleRequestedThisTime True if we want to scroll to make sure the caret is on-screen.
     */
    protected void scheduleCaretUpdate(boolean ensureCaretVisibleRequestedThisTime)
    {
        // Passing true overrides false:
        this.caretUpdateEnsureVisible = caretUpdateEnsureVisible || ensureCaretVisibleRequestedThisTime;
        Scene scene = getScene();
        if (scene == null || caretUpdateScheduled)
        {
            return;
        }

        JavaFXUtil.runAfterNextLayout(scene, () -> {
            // Important that we pick up the value from the field, as an intervening request since we were scheduled
            // may have changed the value:
            boolean ensureCaretVisible = caretUpdateEnsureVisible;
            caretUpdateScheduled = false;
            caretUpdateEnsureVisible = false;
            if (lineDisplay.isLineVisible(getCaretEditorPosition().getLine()))
            {
                MarginAndTextLine line = lineDisplay.getVisibleLine(getCaretEditorPosition().getLine());
                if (line.textLine.isNeedsLayout())
                {
                    // Still need to do more layout; try again after next layout:
                    scheduleCaretUpdate(ensureCaretVisible);
                    return;
                }

                caretShape.getElements().setAll(line.textLine.caretShape(getCaretEditorPosition().getColumn(), true));
                caretShape.layoutXProperty().bind(line.layoutXProperty());
                if (ensureCaretVisible)
                {
                    Bounds caretBounds = caretShape.getBoundsInLocal();
                    double maxScroll = Math.max(0, caretBounds.getCenterX() - 8);
                    double minScroll = Math.max(0, caretBounds.getCenterX() - (getWidth() - MarginAndTextLine.textLeftEdge(showLeftMargin) - verticalScroll.prefWidth(-1) - 6));
                    horizontalScroll.setValue(Math.min(maxScroll, Math.max(minScroll, horizontalScroll.getValue())));
                }
                caretShape.translateXProperty().set(MarginAndTextLine.textLeftEdge(showLeftMargin) - horizontalScroll.getValue());
                caretShape.layoutYProperty().bind(line.layoutYProperty());
            }
            else
            {
                caretShape.getElements().clear();
                caretShape.layoutXProperty().unbind();
                caretShape.layoutYProperty().unbind();
                caretShape.setLayoutX(0);
                caretShape.setLayoutY(0);
            }
            updateCaretVisibility();
        });
        caretUpdateScheduled = true;
    }

    // Updates the caret's is-visible status based on focus, whether it's in view, and whether visibility is forced
    private void updateCaretVisibility()
    {
        boolean focused = isFocused();
        // We don't show if the line isn't on screen because this can cause it to flicker on top of other components when scrolling:
        boolean lineVisible = lineDisplay.isLineVisible(getCaretEditorPosition().getLine());
        caretShape.setVisible(lineVisible && (focused || forceCaretShow));
    }

    public void setFakeCaret(boolean fakeOn)
    {
        forceCaretShow = fakeOn;
        updateCaretVisibility();
    }

    public void setAllowScrollBars(boolean allowScrollBars)
    {
        this.allowScrollBars = allowScrollBars;
        updateRender(false);
    }

    /**
     * Updates the scroll bar extents and the set of visible lines.
     * @param ensureCaretVisible If true, scroll horizontally and vertically such that the current caret is visible
     */
    protected void updateRender(boolean ensureCaretVisible)
    {
        if (ensureCaretVisible)
        {
            lineDisplay.ensureLineVisible(getCaretEditorPosition().getLine(), lineContainer.getHeight(), getLineCount());
        }

        // Must calculate horizontal scroll before rendering, in case it updates the horizontal scroll:
        double width = lineDisplay.calculateLineWidth(getLongestLineInWholeDocument());
        // It doesn't look nice if the width the scroll bar shows is exactly the longest line,
        // as then it feels like it shows "too soon".  So we allow an extra 100 pixels before showing:
        int EXTRA_WIDTH = 100;
        horizontalScroll.setMax(width + EXTRA_WIDTH - getWidth());
        if (horizontalScroll.getValue() > horizontalScroll.getMax())
        {
            updatingScrollBarDirectly = true;
            horizontalScroll.setValue(Math.max(Math.min(horizontalScroll.getValue(), horizontalScroll.getMax()), horizontalScroll.getMin()));
            updatingScrollBarDirectly = false;
        }
        horizontalScroll.setVisibleAmount(getWidth() / (horizontalScroll.getMax() + getWidth()) * horizontalScroll.getMax());
        horizontalScroll.setVisible(allowScrollBars && width + EXTRA_WIDTH >= getWidth());

        List<Node> prospectiveChildren = new ArrayList<>();
        
        List<List<StyledSegment>> styledLines = getStyledLines();

        prospectiveChildren.addAll(lineDisplay.recalculateVisibleLines(styledLines, this::snapSizeY, - horizontalScroll.getValue(), lineContainer.getWidth(), lineContainer.getHeight(), false, this));
        prospectiveChildren.add(caretShape);
        int lineCount = getLineCount();
        verticalScroll.setVisible(allowScrollBars && lineDisplay.getVisibleLineCount() < lineCount);
        // Note: we don't use actual line count as that "jiggle" by one line as lines are partially
        // scrolled out of view.  i.e. if you have a window that's tall enough to show 1.8 lines,
        // the number of actual visible lines may be 2 or 3 depending on where you scroll to.
        // A more reliable estimate that doesn't jiggle is to work out the 1.8 part like this:
        double visibleLinesEstimate = getHeight() / lineDisplay.getLineHeight();
        verticalScroll.setMax(lineCount - visibleLinesEstimate);
        verticalScroll.setVisibleAmount(visibleLinesEstimate / lineCount * verticalScroll.getMax());
        updatingScrollBarDirectly = true;
        verticalScroll.setValue(lineDisplay.getLineRangeVisible()[0] - (lineDisplay.getFirstVisibleLineOffset() / lineDisplay.getLineHeight()));
        updatingScrollBarDirectly = false;


        // This will often avoid changing the children, if the window has not been resized:
        boolean needToChangeLinesAndCaret = false;
        for (int i = 0; i < prospectiveChildren.size(); i++)
        {
            // Reference equality is fine here:
            if (i >= lineContainer.getChildren().size() || prospectiveChildren.get(i) != lineContainer.getChildren().get(i))
            {
                needToChangeLinesAndCaret = true;
                break;
            }
        }
        if (needToChangeLinesAndCaret)
        {
            lineContainer.getChildren().setAll(prospectiveChildren);
        }
        else
        {
            // Clear rest after:
            if (lineContainer.getChildren().size() > prospectiveChildren.size())
            {
                lineContainer.getChildren().subList(prospectiveChildren.size(), lineContainer.getChildren().size()).clear();
            }
        }

        if (getScene() != null)
        {
            scheduleCaretUpdate(ensureCaretVisible);
            String lineText = getLineContentAtCaret();
            setAccessibleText(lineText);
        }
        updateCaretVisibility();

        HashSet<Integer> linesWithSelectionSet = new HashSet<>();

        EditorPosition caret = getCaretEditorPosition();
        EditorPosition anchor = getAnchorEditorPosition();
        if (caret.getPosition() != anchor.getPosition())
        {
            EditorPosition startPos = caret.getPosition() < anchor.getPosition() ? caret : anchor;
            EditorPosition endPos = caret.getPosition() < anchor.getPosition() ? anchor : caret;

            // Simple case; one line selection:
            if (startPos.getLine() == endPos.getLine() && lineDisplay.isLineVisible(startPos.getLine()))
            {
                TextLine caretLine = lineDisplay.getVisibleLine(startPos.getLine()).textLine;
                caretLine.showSelection(startPos.getColumn(), endPos.getColumn(), false);
                linesWithSelectionSet.add(startPos.getLine());
            }
            else
            {
                // Need composite of several lines
                // Do all except last line:
                for (int line = startPos.getLine(); line < endPos.getLine(); line++)
                {
                    int startOnThisLine = line == startPos.getLine() ? startPos.getColumn() : 0;
                    if (lineDisplay.isLineVisible(line))
                    {
                        TextLine textLine = lineDisplay.getVisibleLine(line).textLine;
                        textLine.showSelection(startOnThisLine, getLineLength(line), true);
                        linesWithSelectionSet.add(line);
                    }
                }
                // Now do last line:
                if (lineDisplay.isLineVisible(endPos.getLine()))
                {
                    lineDisplay.getVisibleLine(endPos.getLine()).textLine.showSelection(0, endPos.getColumn(), false);
                    linesWithSelectionSet.add(endPos.getLine());
                }
            }
        }

        // Need to clear any stale selection from other lines:
        int[] visibleLineRange = lineDisplay.getLineRangeVisible();
        for (int line = visibleLineRange[0]; line <= visibleLineRange[1]; line++)
        {
            if (!linesWithSelectionSet.contains(line))
            {
                lineDisplay.getVisibleLine(line).textLine.hideSelection();
            }
        }

        lineContainer.requestLayout();
        requestLayout();
    }

    /**
     * Called when a scroll event has occurred on one of the text lines in the editor
     * @param scrollEvent The scroll event that occurred.
     */
    public void scrollEventOnTextLine(ScrollEvent scrollEvent)
    {
        scroll(scrollEvent.getDeltaX(), scrollEvent.getDeltaY());
    }

    /**
     * Gets the length of the given line, in characters
     * @param lineIndex The index of the line (0 = first line)
     */
    protected abstract int getLineLength(int lineIndex);

    /**
     * Gets the content of the current line (minus newline character) where the caret is
     */
    protected abstract String getLineContentAtCaret();

    /**
     * Call all the selection listeners to let them know that the caret and/or anchor position may have changed.
     */
    protected void callSelectionListeners()
    {
        for (SelectionListener selectionListener : selectionListeners)
        {
            selectionListener.selectionChanged(getCaretEditorPosition().getPosition(), getAnchorEditorPosition().getPosition());
        }
    }

    /**
     * Adds a selection listener which will be called back when the selection (i.e. caret and/or anchor pos) might have changed.
     * (That is, it will always be called when it changes, but it may also be called back sometimes when there hasn't been a change.)
     */
    public void addSelectionListener(SelectionListener selectionListener)
    {
        selectionListeners.add(selectionListener);
    }

    /**
     * Gets the content (minus newline character) of the longest line in the document
     * (NOT just the visible lines on screen), as measured by the number of characters in the line.
     * Used to decide the maximum horizontal scroll.
     */
    protected abstract String getLongestLineInWholeDocument();

    /**
     * Gets the number of lines in the whole document.
     * @return
     */
    protected abstract int getLineCount();

    /**
     * Gets the content of the whole document in styled form.
     * The outer list is one entry per line.  Each line may have 0 to unlimited
     * numbers of styled segments (style+text content).  No segment will have an
     * empty string as its content; empty lines are zero-length lists of segments.
     */
    protected abstract List<List<StyledSegment>> getStyledLines();

    /**
     * Make a position corresponding to the given zero-based line and column
     * @param line The zero-based line index (0 = first line)
     * @param column The zero-based column index (0 = before first column)
     * @return An editor position corresponding to that location
     */
    protected abstract EditorPosition makePosition(int line, int column);
    
    /**
     * Gets the current caret position.
     */
    protected abstract EditorPosition getCaretEditorPosition();

    /**
     * Gets the current anchor position.  If equal to caret position, there is no selection.
     * Otherwise, this is the "other end" of the selection from the caret.
     */    
    protected abstract EditorPosition getAnchorEditorPosition();

    public final double getTextDisplayWidth()
    {
        return lineContainer.getTextDisplayWidth();
    }

    /**
     * A listener for some mouse events that can occur in the editor but need handling elsewhere. 
     */
    @OnThread(Tag.FXPlatform)
    public static interface BaseEditorPaneListener
    {
        // The left-hand margin was clicked for (zero-based) lineIndex.
        // Returns true if breakpoint was successfully toggled for that line, false if there was a problem.
        public boolean marginClickedForLine(int lineIndex);

        /**
         * Gets the context menu to show.  If necessary, should be hidden before being returned
         * by this method.
         */
        ContextMenu getContextMenuToShow(BaseEditorPane editorPane);

        /**
         * Called when a scroll event has occurred on one of the text lines in the editor
         * @param scrollEvent The scroll event that occurred.
         */
        public void scrollEventOnTextLine(ScrollEvent scrollEvent, BaseEditorPane editorPane);
    }

    /**
     * Allows tracking of the caret position and anchor position
     * (which together delineate the selection).
     */
    public static interface SelectionListener
    {
        @OnThread(Tag.FXPlatform)
        public void selectionChanged(int caretPosition, int anchorPosition);
    }
}
