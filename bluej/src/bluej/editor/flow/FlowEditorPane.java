/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.flow;

import bluej.Config;
import bluej.editor.flow.Document.Bias;
import bluej.editor.flow.LineDisplay.LineDisplayListener;
import bluej.editor.flow.MarginAndTextLine.MarginDisplay;
import bluej.editor.flow.TextLine.HighlightType;
import bluej.editor.flow.TextLine.StyledSegment;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Stream;

/**
 * A FlowEditorPane is a component with (optional) horizontal and vertical scroll bars.
 * 
 * It displays only the lines that are currently visible on screen, in what is known
 * as a virtualised container.  Scrolling re-renders the currently visible line set.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class FlowEditorPane extends Region implements JavaSyntaxView.Display
{
    private final LineDisplay lineDisplay;
    private final FlowEditorPaneListener listener;

    private final HoleDocument document;
    
    private final TrackedPosition anchor;
    private final TrackedPosition caret;
    // When moving up/down, we keep track of what column we are aiming for so that moving vertically through an empty
    // line doesn't always push you over to the left.  This is automatically reset to -1 by all insertions and movements,
    // if you want it to persist, you will need to set it again manually afterwards.
    // Note: this is 1-based (first column is 1) to fit in more easily with SourceLocation.
    private int targetColumnForVerticalMovement;
    private final Path caretShape;
    
    // Default is to apply no styles:
    private LineStyler lineStyler = (i, s) -> Collections.singletonList(new StyledSegment(Collections.emptyList(), s.toString()));
    
    private ErrorQuery errorQuery = () -> Collections.emptyList();
    
    private final List<IndexRange> errorUnderlines = new ArrayList<>();
    private final LineContainer lineContainer;
    private final ScrollBar verticalScroll;
    private final ScrollBar horizontalScroll;
    private boolean updatingScrollBarDirectly = false;
    // Scroll bars can be turned off for testing and printing:
    private boolean allowScrollBars = true;
    
    private final ArrayList<SelectionListener> selectionListeners = new ArrayList<>();
    private boolean postScrollRenderQueued = false;
    private boolean editable = true;
    private boolean forceCaretShow = false;
    // The pending amount to scroll by (mouse/touch scroll events are batched up)
    private double pendingScrollY;
    // Have we currently scheduled an update of the caret graphics?  If so, no need to schedule another.
    private boolean caretUpdateScheduled;
    // If we have currently scheduled an update of the caret graphics, will we ensure caret is visible?
    private boolean caretUpdateEnsureVisible;

    public FlowEditorPane(String content, FlowEditorPaneListener listener)
    {
        this.listener = listener;
        setSnapToPixel(true);
        document = new HoleDocument();
        document.replaceText(0, 0, content);
        caret = document.trackPosition(0, Bias.FORWARD);
        // Important that the anchor is a different object to the caret, as they will move independently:
        anchor = document.trackPosition(0, Bias.FORWARD);
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
        lineDisplay = new LineDisplay(this::getLineContainerHeight, horizontalScroll.valueProperty(), PrefMgr.getEditorFontCSS(true), listener);

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
        lineContainer = new LineContainer(lineDisplay, false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(lineContainer.widthProperty());
        clip.heightProperty().bind(lineContainer.heightProperty());
        lineContainer.setClip(clip);
        getChildren().setAll(lineContainer, verticalScroll, horizontalScroll);
        updateRender(false);
        JavaFXUtil.addChangeListenerPlatform(lineContainer.heightProperty(), h -> JavaFXUtil.runAfterCurrent(() -> updateRender(false)));

        setAccessibleRole(AccessibleRole.TEXT_AREA);
        selectionListeners.add(new SelectionListener() {
            int oldCaretPos = 0;
            int oldAnchorPos = 0;
            @Override
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
        document.addListener(false, (origStartIncl, replaced, replacement, linesRemoved, linesAdded) -> {
            notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        });

        Nodes.addInputMap(this, InputMap.sequence(
            InputMap.consume(KeyEvent.KEY_TYPED, this::keyTyped),
            InputMap.consume(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            InputMap.consume(MouseEvent.MOUSE_DRAGGED, this::mouseDragged),
            InputMap.consume(MouseEvent.MOUSE_MOVED, this::mouseMoved),
            InputMap.consume(ScrollEvent.SCROLL, this::scroll)
        ));

        JavaFXUtil.addChangeListenerPlatform(widthProperty(), w -> updateRender(false));
        JavaFXUtil.addChangeListenerPlatform(heightProperty(), h -> updateRender(false));
        JavaFXUtil.addChangeListenerPlatform(focusedProperty(), f -> updateCaretVisibility());
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute accessibleAttribute, Object... objects)
    {
        switch (accessibleAttribute)
        {
            case EDITABLE:
                return true;
            case TEXT:
                return getDocument().getFullContent();
            case CARET_OFFSET:
                return caret.position;
            case SELECTION_START:
                return getSelectionStart();
            case SELECTION_END:
                return getSelectionEnd();
            case LINE_FOR_OFFSET:
                return document.getLineFromPosition((Integer)objects[0]);
            case LINE_START:
                return document.getLineStart((Integer)objects[0]);
            case LINE_END:
                return document.getLineEnd((Integer)objects[0]);
            case BOUNDS_FOR_RANGE:
                return lineDisplay.getBoundsForRange(document, (Integer)objects[0], (Integer)objects[1]);
            case OFFSET_AT_POINT:
                Point2D screenPoint = (Point2D)objects[0];
                return getCaretPositionForLocalPoint(screenToLocal(screenPoint));
            case HELP:
                String err = listener.getErrorAtPosition(caret.position);
                if (err != null)
                    return "Error: " + err;
                else
                    break;
        }
        return super.queryAccessibleAttribute(accessibleAttribute, objects);
    }

    private double getLineContainerHeight()
    {
        return lineContainer.getHeight();
    }

    private void keyTyped(KeyEvent event)
    {
        if (!editable)
            return;
        
        /////////////////////////////////////////////////////////
        // This section is adapted from TextInputControlBehavior
        /////////////////////////////////////////////////////////
        // Sometimes we get events with no key character, in which case
        // we need to bail.
        String character = event.getCharacter();
        if (character.length() == 0) return;

        // Filter out control keys except control+Alt on PC or Alt on Mac
        if (event.isControlDown() || event.isAltDown() || (Config.isMacOS() && event.isMetaDown())) {
            if (!((event.isControlDown() || Config.isMacOS()) && event.isAltDown())) return;
        }

        // Ignore characters in the control range and the ASCII delete
        // character as well as meta key presses
        if (character.charAt(0) > 0x1F
            && character.charAt(0) != 0x7F
            && !event.isMetaDown()) { // Not sure about this one -- NCCB note this comment is from the original source
            replaceSelection(character);
            JavaFXUtil.runAfterCurrent(() -> scheduleCaretUpdate(true));
        }
        
    }

    private void mousePressed(MouseEvent e)
    {
        requestFocus();
        if (e.getButton() == MouseButton.PRIMARY)
        {
            // If shift pressed, don't move anchor; form selection instead:
            positionCaretAtDestination(e, !e.isShiftDown());
            updateRender(true);
        }
    }

    private void positionCaretAtDestination(MouseEvent e, boolean setAnchor)
    {
        getCaretPositionForMouseEvent(e).ifPresent(setAnchor ? this::positionCaret : this::moveCaret);
    }
    
    OptionalInt getCaretPositionForMouseEvent(MouseEvent e)
    {
        return getCaretPositionForLocalPoint(new Point2D(e.getX(), e.getY()));
    }

    OptionalInt getCaretPositionForLocalPoint(Point2D localPoint)
    {
        int[] position = lineDisplay.getCaretPositionForLocalPoint(localPoint);
        if (position != null)
        {
            return OptionalInt.of(document.getLineStart(position[0]) + position[1]);
        }
        return OptionalInt.empty();
    }

    private void mouseMoved(MouseEvent event)
    {
        getCaretPositionForMouseEvent(event).ifPresent(pos -> listener.showErrorPopupForCaretPos(pos, true));
    }

    private void mouseDragged(MouseEvent e)
    {
        if (e.getButton() == MouseButton.PRIMARY)
        {
            positionCaretAtDestination(e, false);
            // Don't update the anchor, though
            updateRender(true);
        }
    }

    public void textChanged()
    {
        updateRender(false);
        targetColumnForVerticalMovement = -1;
        callSelectionListeners();
        // FlowEditor is in charge of recording edits
    }

    private void updateRender(boolean ensureCaretVisible)
    {
        if (ensureCaretVisible)
        {
            lineDisplay.ensureLineVisible(caret.getLine());
        }

        // Must calculate horizontal scroll before rendering, in case it updates the horizontal scroll:
        double width = lineDisplay.calculateLineWidth(document.getLongestLine());
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
        // Use an AbstractList rather than pre-calculate, as that means we don't bother
        // styling lines which will not be displayed:
        List<List<StyledSegment>> styledLines = new StyledLines(document, lineStyler);
        
        prospectiveChildren.addAll(lineDisplay.recalculateVisibleLines(styledLines, this::snapSizeY, - horizontalScroll.getValue(), lineContainer.getWidth(), lineContainer.getHeight(), false));
        prospectiveChildren.add(caretShape);
        verticalScroll.setVisible(allowScrollBars && lineDisplay.getVisibleLineCount() < document.getLineCount());
        // Note: we don't use actual line count as that "jiggle" by one line as lines are partially
        // scrolled out of view.  i.e. if you have a window that's tall enough to show 1.8 lines,
        // the number of actual visible lines may be 2 or 3 depending on where you scroll to.
        // A more reliable estimate that doesn't jiggle is to work out the 1.8 part like this:
        double visibleLinesEstimate = getHeight() / lineDisplay.getLineHeight();
        verticalScroll.setMax(document.getLineCount() - visibleLinesEstimate);
        verticalScroll.setVisibleAmount(visibleLinesEstimate / document.getLineCount() * verticalScroll.getMax());
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
            String lineText = getDocument().getLines().get(document.getLineFromPosition(caret.position)).toString();
            setAccessibleText(lineText);
        }
        updateCaretVisibility();

        HashSet<Integer> linesWithSelectionSet = new HashSet<>();
        
        if (caret.position != anchor.position)
        {
            TrackedPosition startPos = caret.position < anchor.position ? caret : anchor;
            TrackedPosition endPos = caret.position < anchor.position ? anchor : caret;
            
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
                        textLine.showSelection(startOnThisLine, document.getLineStart(line + 1) - document.getLineStart(line), true);
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
        
        if (errorQuery != null)
        {
            for (IndexRange indexRange : errorQuery.getErrorUnderlines())
            {
                addErrorUnderline(indexRange.getStart(), indexRange.getEnd());
            }
        }
        
        /* Code for showing error underlines
        for (IndexRange errorUnderline : errorUnderlines)
        {
            Path err = new Path();
            err.setFill(null);
            err.setStroke(new ImagePattern(UNDERLINE_IMAGE, 0, 0, 2, 2, false));
            err.setMouseTransparent(true);
            TextLine errTextLine = lineDisplay.currentlyVisibleLines.get(document.getLineFromPosition(errorUnderline.getStart()));
            err.getElements().setAll(keepBottom(errTextLine.rangeShape(document.getColumnFromPosition(errorUnderline.getStart()), document.getColumnFromPosition(errorUnderline.getEnd()))));
            err.setLayoutX(errTextLine.getLayoutX());
            err.setLayoutY(errTextLine.getLayoutY());
            getChildren().add(err);
        }
        */
        
        // Temporary calculations for box location:
        /*
        String docContent = document.getFullContent();
        int nextRect = 0;
        for (int publicLoc = docContent.indexOf("public"); publicLoc != -1; publicLoc = docContent.indexOf("public", publicLoc + 1))
        {
            int open = 0;
            int closingCurly = publicLoc;
            while (closingCurly < docContent.length())
            {
                closingCurly += 1;
                if (docContent.charAt(closingCurly) == '{')
                    open++;
                if (docContent.charAt(closingCurly) == '}')
                {
                    open--;
                    if (open == 0)
                        break;
                }
            }
            // Now draw a background box the full width of the header line, down to beneath the curly
            double x = getCaretLikeBounds(publicLoc).getMinX();
            double y = getCaretLikeBounds(publicLoc).getMinY();
            double width = lineDisplay.currentlyVisibleLines.get(document.getLineFromPosition(publicLoc)).getWidth() - x;
            double height = getCaretLikeBounds(closingCurly).getMaxY() - y;
            Rectangle r = new Rectangle(x, y, width, height);
            r.setMouseTransparent(true);
            r.setStroke(Color.GRAY);
            r.setFill(docContent.startsWith("public class", publicLoc) ? Color.PALEGREEN : Color.LIGHTYELLOW);
            getChildren().add(nextRect++, r);
        }
        */
        
        lineContainer.requestLayout();        
        requestLayout();
    }

    /**
     * Schedules an update of the caret graphics after the next scene layout.
     * @param ensureCaretVisibleRequestedThisTime True if we want to scroll to make sure the caret is on-screen.
     */
    private void scheduleCaretUpdate(boolean ensureCaretVisibleRequestedThisTime)
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
            if (lineDisplay.isLineVisible(caret.getLine()))
            {
                MarginAndTextLine line = lineDisplay.getVisibleLine(caret.getLine());
                if (line.textLine.isNeedsLayout())
                {
                    // Still need to do more layout; try again after next layout:
                    scheduleCaretUpdate(ensureCaretVisible);
                    return;
                }
                
                caretShape.getElements().setAll(line.textLine.caretShape(caret.getColumn(), true));
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

    private void updateCaretVisibility()
    {
        boolean focused = isFocused();
        boolean lineVisible = lineDisplay.isLineVisible(caret.getLine());
        caretShape.setVisible(lineVisible && (focused || forceCaretShow));
    }

    public boolean isLineVisible(int line)
    {
        return lineDisplay.isLineVisible(line);
    }

    /**
     * Gives back a two-element array, the first element being the inclusive zero-based index of the first line
     * that is visible on screen, and the second element being the inclusive zero-based index of the last line
     * that is visible on screen.
     */
    int[] getLineRangeVisible()
    {
        return lineDisplay.getLineRangeVisible();
    }
    
    double getLineHeight()
    {
        return lineDisplay.getLineHeight();
    }

    // Bounds relative to FlowEditorPane
    /*
    private Bounds getCaretLikeBounds(int pos)
    {
        TextLine textLine = lineDisplay.currentlyVisibleLines.get(document.getLineFromPosition(pos));
        Path path = new Path(textLine.caretShape(document.getColumnFromPosition(pos), true));
        path.setLayoutX(textLine.getLayoutX());
        path.setLayoutY(textLine.getLayoutY());
        return path.getBoundsInParent();
    }
    */

    private PathElement[] keepBottom(PathElement[] rangeShape)
    {
        // This corresponds to the code in PrismTextLayout.range, where
        // the range shapes are constructed using:
        //   MoveTo top-left
        //   LineTo top-right
        //   LineTo bottom-right
        //   LineTo bottom-left ***
        //   LineTo top-left
        //
        // We only want to keep the asterisked line, which is the bottom of the shape.
        // So we convert the others to MoveTo.  If PrismTextLayout.range ever changes
        // its implementation, we will need to change this.
        if (rangeShape.length % 5 == 0)
        {
            for (int i = 0; i < rangeShape.length; i += 5)
            {
                if (rangeShape[0] instanceof MoveTo
                    && rangeShape[1] instanceof LineTo
                    && rangeShape[2] instanceof LineTo
                    && rangeShape[3] instanceof LineTo
                    && rangeShape[4] instanceof LineTo)
                {
                    rangeShape[1] = lineToMove(rangeShape[1]);
                    rangeShape[2] = lineToMove(rangeShape[2]);
                    rangeShape[4] = lineToMove(rangeShape[4]);
                }
            }
        }
        return rangeShape;
    }

    private PathElement lineToMove(PathElement pathElement)
    {
        LineTo lineTo = (LineTo)pathElement;
        return new MoveTo(lineTo.getX(), lineTo.getY());
    }

    /**
     * Used by FlowActions to keep track of target column when going up/down.  Note: first column is one, not zero.
     */
    public int getTargetColumnForVerticalMove()
    {
        return targetColumnForVerticalMovement;
    }

    /**
     * Used by FlowActions to keep track of target column when going up/down.  Note: first column is one, not zero.
     */
    public void setTargetColumnForVerticalMove(int targetColumn)
    {
        this.targetColumnForVerticalMovement = targetColumn;
    }

    private void addErrorUnderline(int startPos, int endPos)
    {
        int lineIndex = document.getLineFromPosition(startPos);
        int startColumn = document.getColumnFromPosition(startPos);
        // Only show error on one line at most:
        int endColumn = Math.min(document.getLineEnd(lineIndex), endPos - document.getLineStart(lineIndex));
        
        if (lineDisplay.isLineVisible(lineIndex))
        {
            lineDisplay.getVisibleLine(lineIndex).textLine.showError(startColumn, endColumn);
        }
    }
    
    // Each item is of size 2, start pos incl and end pos excl, where position is within the whole document
    void showHighlights(HighlightType highlightType, List<int[]> results)
    {
        // Maps line number to [start column incl, end column excl]
        Map<Integer, List<int[]>> resultsByLine = new HashMap<>();

        for (int[] result : results)
        {
            int lineIndex = document.getLineFromPosition(result[0]);
            int startColumn = document.getColumnFromPosition(result[0]);
            // Only show result on one line at most:
            int endColumn = Math.min(document.getLineEnd(lineIndex), result[1] - document.getLineStart(lineIndex));
            resultsByLine.computeIfAbsent(lineIndex, n -> new ArrayList<>()).add(new int[]{startColumn, endColumn});
        }
        int[] visibleLines = lineDisplay.getLineRangeVisible();
        for (int line = visibleLines[0]; line <= visibleLines[1]; line++)
        {
            lineDisplay.getVisibleLine(line).textLine.showHighlight(highlightType, resultsByLine.getOrDefault(line, List.of()));
        }
    }

    public void setErrorQuery(ErrorQuery errorQuery)
    {
        this.errorQuery = errorQuery;
    }

    public void applyScopeBackgrounds(Map<Integer, List<BackgroundItem>> scopeBackgrounds)
    {
        // Important to take a copy so as to not modify the original:
        HashMap<Integer, List<BackgroundItem>> withOverlays = new HashMap<>();
        Set<Integer> breakpointLines = listener.getBreakpointLines();
        int stepLine = listener.getStepLine();
        
        scopeBackgrounds.forEach((line, scopes) -> {
            if (breakpointLines.contains(line) || line == stepLine)
            {
                ArrayList<BackgroundItem> regions = new ArrayList<>(scopes);
                BackgroundItem region = new BackgroundItem(0, getWidth() - MarginAndTextLine.TEXT_LEFT_EDGE, 
                    new BackgroundFill((line == stepLine ? listener.stepMarkOverlayColorProperty() : listener.breakpointOverlayColorProperty()).get(), null, null));
                regions.add(region);
                withOverlays.put(line, regions);
            }
            else
            {
                // Just copy, no need to modify:
                withOverlays.put(line, scopes);
            }
        });
        
        lineDisplay.applyScopeBackgrounds(withOverlays);
    }

    public void ensureCaretShowing()
    {
        updateRender(true);
    }

    public void setLineMarginGraphics(int lineIndex, EnumSet<MarginDisplay> marginDisplays)
    {
        if (lineDisplay.isLineVisible(lineIndex))
        {
            lineDisplay.getVisibleLine(lineIndex).setMarginGraphics(marginDisplays);
        }
    }

    /**
     * Called when the font size has changed; redisplay accordingly.
     */
    public void fontSizeChanged()
    {
        lineDisplay.fontSizeChanged();
        updateRender(false);
    }

    /**
     * Selects the given range, with anchor at the beginning and caret at the end.
     */
    public void select(int start, int end)
    {
        positionAnchor(start);
        moveCaret(end);
    }

    public void setFakeCaret(boolean fakeOn)
    {
        forceCaretShow = fakeOn;
        updateCaretVisibility();
    }

    public boolean isEditable()
    {
        return editable;
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;
    }

    public void write(Writer writer) throws IOException
    {
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        bufferedWriter.write(document.getFullContent());
        // Must flush or else changes don't get written:
        bufferedWriter.flush();
    }

    public void hideAllErrorUnderlines()
    {
        lineDisplay.hideAllErrorUnderlines();
    }

    public double getTextDisplayWidth()
    {
        return lineContainer.getWidth() - MarginAndTextLine.TEXT_LEFT_EDGE;
    }

    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public static class LineContainer extends Region
    {
        private final LineDisplay lineDisplay;
        private final boolean lineWrapping;

        public LineContainer(LineDisplay lineDisplay, boolean lineWrapping)
        {
            this.lineDisplay = lineDisplay;
            this.lineWrapping = lineWrapping;
            JavaFXUtil.addStyleClass(this,"line-container");
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
            }
            else
            {
                for (Node child : getChildren())
                {
                    if (child instanceof MarginAndTextLine)
                    {
                        MarginAndTextLine line = (MarginAndTextLine) child;
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
        protected ObservableList<Node> getChildren()
        {
            return super.getChildren();
        }
    }

    @Override
    protected void layoutChildren()
    {
        double horizScrollHeight = horizontalScroll.isVisible() ? horizontalScroll.prefHeight(-1) : 0;
        horizontalScroll.resizeRelocate(0, getHeight() - horizScrollHeight, getWidth(), horizScrollHeight);
        double vertScrollWidth = verticalScroll.isVisible() ? verticalScroll.prefWidth(-1) : 0;
        verticalScroll.resizeRelocate(getWidth() - vertScrollWidth, 0, vertScrollWidth, getHeight() - horizScrollHeight);
        lineContainer.resizeRelocate(0, 0, getWidth() - vertScrollWidth, getHeight() - horizScrollHeight);
    }

    public HoleDocument getDocument()
    {
        return document;
    }

    /**
     * Sets the given line (zero-based) to be at the top of the window,
     * at zero offset.
     * @param lineIndex
     */
    public void scrollTo(int lineIndex)
    {
        lineDisplay.scrollTo(lineIndex, 0.0);
        updateRender(false);
    }
    
    private void scroll(ScrollEvent scrollEvent)
    {
        updatingScrollBarDirectly = true;
        horizontalScroll.setValue(Math.max(horizontalScroll.getMin(), Math.min(horizontalScroll.getMax(), horizontalScroll.getValue() - scrollEvent.getDeltaX())));
        updatingScrollBarDirectly = false;
        pendingScrollY += scrollEvent.getDeltaY();
        if (!postScrollRenderQueued)
        {
            postScrollRenderQueued = true;
            JavaFXUtil.runAfter(Duration.millis(50), () -> {
                postScrollRenderQueued = false;
                lineDisplay.scrollBy(pendingScrollY, document.getLineCount());
                pendingScrollY = 0;
                updateRender(false);
            });
        }
    }

    /**
     * If given character index within the document is on screen, then returns its X position.
     * If it's not on screen, returns empty.
     * @param leftOfCharIndex
     * @return
     */
    public Optional<Double> getLeftEdgeX(int leftOfCharIndex)
    {
        return getLeftEdgeX(leftOfCharIndex, document, lineDisplay);
    }

    static Optional<Double> getLeftEdgeX(int leftOfCharIndex, Document document, LineDisplay lineDisplay)
    {
        int lineIndex = document.getLineFromPosition(leftOfCharIndex);
        if (lineDisplay.isLineVisible(lineIndex))
        {
            TextLine line = lineDisplay.getVisibleLine(lineIndex).textLine;
            // If the line needs layout, the positions won't be accurate:
            if (line.isNeedsLayout())
                return Optional.empty();
            // Sometimes, it seems that the line can have the CSS for the font,
            // and claim it doesn't need layout, but the font on the Text items
            // has not actually been switched to the right font.  In this case
            // the positions will be inaccurate, so we should not calculate:
            Font curFont = line.getChildren().stream().flatMap(n -> n instanceof Text ? Stream.of(((Text)n).getFont()) : Stream.empty()).findFirst().orElse(null);
            if (curFont != null && !curFont.getFamily().equals(PrefMgr.getEditorFontFamily()))
                return Optional.empty();
            int posInLine = leftOfCharIndex - document.getLineStart(lineIndex);
            PathElement[] elements = line.caretShape(posInLine, true);
            Path path = new Path(elements);
            Bounds bounds = path.getBoundsInLocal();
            // If the bounds are at left edge but char is not, might not have laid out yet:
            if (posInLine > 0 && bounds.getMaxX() < 2.0)
            {
                return Optional.empty();
            }
            return Optional.of((bounds.getMinX() + bounds.getMaxX()) / 2.0);
        }
        return Optional.empty();
    }

    public Optional<Bounds> getCaretBoundsOnScreen(int position)
    {
        int lineIndex = document.getLineFromPosition(position);
        if (lineDisplay.isLineVisible(lineIndex))
        {
            TextLine line = lineDisplay.getVisibleLine(lineIndex).textLine;
            PathElement[] elements = line.caretShape(position - document.getLineStart(lineIndex), true);
            Path path = new Path(elements);
            Bounds bounds = line.localToScreen(path.getBoundsInLocal());
            return Optional.of(bounds);
        }
        return Optional.empty();
    }
    
    public Optional<double[]> getTopAndBottom(int lineIndex)
    {
        if (lineDisplay.isLineVisible(lineIndex))
        {
            MarginAndTextLine line = lineDisplay.getVisibleLine(lineIndex);
            Bounds bounds = line.getLayoutBounds();
            return Optional.of(new double[] {line.getLayoutY() + bounds.getMinY(), line.getLayoutY() + bounds.getMaxY()});
        }
        return Optional.empty();
    }

    /**
     * Set the position of the caret and anchor, and scroll to ensure the caret is on screen.
     */
    public void positionCaret(int position)
    {
        caret.moveTo(position);
        anchor.moveTo(position);
        targetColumnForVerticalMovement = -1;
        updateRender(true);
        callSelectionListeners();
    }

    /**
     * Set the position of the caret and anchor, but do not scroll.
     */
    public void positionCaretWithoutScrolling(int position)
    {
        caret.moveTo(position);
        anchor.moveTo(position);
        targetColumnForVerticalMovement = -1;
        updateRender(false);
        callSelectionListeners();
    }

    /**
     * Set the position of the caret without moving the anchor, and scroll to ensure the caret is on screen.
     */
    public void moveCaret(int position)
    {
        caret.moveTo(position);
        targetColumnForVerticalMovement = -1;
        updateRender(true);
        callSelectionListeners();
    }

    /**
     * Set the position of the anchor without changing the caret or scrolling.
     */
    public void positionAnchor(int position)
    {
        anchor.moveTo(position);
        updateRender(false);
        callSelectionListeners();
    }
    
    public int getCaretPosition()
    {
        return caret.position;
    }
    
    public int getAnchorPosition()
    {
        return anchor.position;
    }

    public void addLineDisplayListener(LineDisplayListener lineDisplayListener)
    {
        lineDisplay.addLineDisplayListener(lineDisplayListener);
    }

    /**
     * Repaint the editor.
     */
    public void repaint()
    {
        updateRender(false);
    }

    public void replaceSelection(String text)
    {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        document.replaceText(start, end, text);
        // This makes sure the anchor is reset, too:
        positionCaret(start + text.length());
    }

    public int getSelectionEnd()
    {
        return Math.max(caret.position, anchor.position);
    }

    public int getSelectionStart()
    {
        return Math.min(caret.position, anchor.position);
    }
    
    public String getSelectedText()
    {
        return document.getContent(getSelectionStart(), getSelectionEnd()).toString();
    }
    
    public static interface ErrorQuery
    {
        public List<IndexRange> getErrorUnderlines();
    }

    @OnThread(Tag.FXPlatform)
    public static interface LineStyler
    {
        /**
         * Get the list of styled segments to display on a particular line.  Note that it is
         * very important to never return an empty list even if the line is blank; 
         * this will effectively hide the line from display.  Instead return a singleton list
         * with an empty text content for the segment.
         * 
         * @param lineIndex The zero-based index of the line.
         * @param lineContent The text content of the line (without trailing newline) 
         * @return The list of styled segments containing the styled content to display.
         */
        public List<StyledSegment> getLineForDisplay(int lineIndex, CharSequence lineContent);
    }

    /**
     * Set the way that lines are styled for this editor.
     */
    public void setLineStyler(LineStyler lineStyler)
    {
        this.lineStyler = lineStyler;
    }

    public void setAllowScrollBars(boolean allowScrollBars)
    {
        this.allowScrollBars = allowScrollBars;
        updateRender(false);
    }
    
    private void callSelectionListeners()
    {
        for (SelectionListener selectionListener : selectionListeners)
        {
            selectionListener.selectionChanged(caret.position, anchor.position);
        }
    }
    
    public void addSelectionListener(SelectionListener selectionListener)
    {
        selectionListeners.add(selectionListener);
    }

    /**
     * Allows tracking of the caret position and anchor position
     * (which together delineate the selection).
     */
    public static interface SelectionListener
    {
        public void selectionChanged(int caretPosition, int anchorPosition);
    }
    
    public static interface FlowEditorPaneListener extends ScopeColors
    {
        // The left-hand margin was clicked for (zero-based) lineIndex.
        // Returns true if breakpoint was successfully toggled for that line, false if there was a problem.
        public boolean marginClickedForLine(int lineIndex);

        public Set<Integer> getBreakpointLines();

        // Returns -1 if no step line
        int getStepLine();

        public void showErrorPopupForCaretPos(int caretPos, boolean mousePosition);

        public String getErrorAtPosition(int caretPos);

        /**
         * Gets the context menu to show.  If necessary, should be hidden before being returned
         * by this method.
         */
        ContextMenu getContextMenuToShow();
    }

    // Use an AbstractList rather than pre-calculate, as that means we don't bother
    // styling lines which will not be displayed:
    @OnThread(value = Tag.FXPlatform ,ignoreParent = true)
    static class StyledLines extends AbstractList<List<StyledSegment>>
    {
        private final LineStyler lineStyler;
        private final List<CharSequence> documentLines;
        
        public StyledLines(Document document, LineStyler lineStyler)
        {
            this.documentLines = document.getLines();
            this.lineStyler = lineStyler;
        }

        @Override
        public int size()
        {
            return documentLines.size();
        }

        @Override
        public List<StyledSegment> get(int lineIndex)
        {
            // Because styling is called on demand, we save styling lines
            // which are never requested for display.
            return lineStyler.getLineForDisplay(lineIndex, documentLines.get(lineIndex));
        }
    }
}
