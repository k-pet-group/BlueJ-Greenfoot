/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021  Michael Kolling and John Rosenberg

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

import bluej.editor.flow.FlowEditorPane.LineContainer;
import bluej.editor.base.TextLine.StyledSegment;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A shared editor component to be used as the basis for rich text displays with carets and scrolling.
 * Used by the flow editor and by the terminal.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public abstract class BaseEditorPane extends Region
{
    protected static final Duration SCROLL_DELAY = Duration.millis(50);

    // The manager of the lines of text currently shown on screen in the editor
    protected final LineDisplay lineDisplay;
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


    protected BaseEditorPane(BaseEditorPaneListener listener)
    {
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
        lineDisplay = new LineDisplay(horizontalScroll.valueProperty(), PrefMgr.getEditorFontCSS(true), listener);
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
                    double minScroll = Math.max(0, caretBounds.getCenterX() - (getWidth() - MarginAndTextLine.TEXT_LEFT_EDGE - verticalScroll.prefWidth(-1) - 6));
                    horizontalScroll.setValue(Math.min(maxScroll, Math.max(minScroll, horizontalScroll.getValue())));
                }
                caretShape.translateXProperty().set(MarginAndTextLine.TEXT_LEFT_EDGE - horizontalScroll.getValue());
                caretShape.layoutYProperty().bind(line.layoutYProperty());
                caretShape.setVisible(true);
            }
            else
            {
                caretShape.getElements().clear();
                caretShape.layoutXProperty().unbind();
                caretShape.layoutYProperty().unbind();
                caretShape.setLayoutX(0);
                caretShape.setLayoutY(0);
                caretShape.setVisible(false);
            }
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
            lineDisplay.ensureLineVisible(getCaretEditorPosition().getLine(), lineContainer.getHeight());
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

        prospectiveChildren.addAll(lineDisplay.recalculateVisibleLines(styledLines, this::snapSizeY, - horizontalScroll.getValue(), lineContainer.getWidth(), lineContainer.getHeight(), false));
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
     * Gets the length of the given line, in characters
     * @param lineIndex The index of the line (0 = first line)
     */
    protected abstract int getLineLength(int lineIndex);

    /**
     * Gets the content of the current line (minus newline character) where the caret is
     */
    protected abstract String getLineContentAtCaret();

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
     * Gets the current caret position.
     */
    protected abstract EditorPosition getCaretEditorPosition();

    /**
     * Gets the current anchor position.  If equal to caret position, there is no selection.
     * Otherwise, this is the "other end" of the selection from the caret.
     */    
    protected abstract EditorPosition getAnchorEditorPosition();

    protected final double getLineContainerWidth()
    {
        return lineContainer.getWidth();
    }

    /**
     * A listener for some mouse events that can occur in the editor but need handling elsewhere. 
     */
    public static interface BaseEditorPaneListener
    {
        // The left-hand margin was clicked for (zero-based) lineIndex.
        // Returns true if breakpoint was successfully toggled for that line, false if there was a problem.
        public boolean marginClickedForLine(int lineIndex);

        /**
         * Gets the context menu to show.  If necessary, should be hidden before being returned
         * by this method.
         */
        ContextMenu getContextMenuToShow();

        /**
         * Called when a scroll event has occurred on one of the text lines in the editor
         * @param scrollEvent The scroll event that occurred.
         */
        public void scrollEventOnTextLine(ScrollEvent scrollEvent);
    }
}
