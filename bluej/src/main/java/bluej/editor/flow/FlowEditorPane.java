/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019,2020,2021,2022,2024,2025  Michael Kolling and John Rosenberg

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
import bluej.editor.base.BackgroundItem;
import bluej.editor.base.BaseEditorPane;
import bluej.editor.base.EditorPosition;
import bluej.editor.base.LineDisplay;
import bluej.editor.base.MarginAndTextLine;
import bluej.editor.base.TextLine;
import bluej.editor.flow.Document.Bias;
import bluej.editor.base.LineDisplay.LineDisplayListener;
import bluej.editor.base.MarginAndTextLine.MarginDisplay;
import bluej.editor.base.TextLine.HighlightType;
import bluej.editor.base.TextLine.StyledSegment;
import bluej.utility.javafx.JavaFXUtil;
import com.sun.javafx.scene.input.ExtendedInputMethodRequests;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Optional;
import java.util.Set;

/**
 * A FlowEditorPane is a component with (optional) horizontal and vertical scroll bars.
 * 
 * It displays only the lines that are currently visible on screen, in what is known
 * as a virtualised container.  Scrolling re-renders the currently visible line set.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class FlowEditorPane extends BaseEditorPane implements JavaSyntaxView.Display
{
    private final FlowEditorPaneListener listener;

    private final HoleDocument document;
    
    private final TrackedPosition anchor;
    private final TrackedPosition caret;
    // When moving up/down, we keep track of what column we are aiming for so that moving vertically through an empty
    // line doesn't always push you over to the left.  This is automatically reset to -1 by all insertions and movements,
    // if you want it to persist, you will need to set it again manually afterwards.
    // Note: this is 1-based (first column is 1) to fit in more easily with SourceLocation.
    private int targetColumnForVerticalMovement;
    
    
    // Default is to apply no styles:
    private LineStyler lineStyler = (i, s) -> Collections.singletonList(new StyledSegment(Collections.emptyList(), s.toString()));
    
    private ErrorQuery errorQuery = () -> Collections.emptyList();

    private boolean editable = true;
    
    // Tracked for the purposes of smart bracket adding.  We set this to true when
    // the user types an '{'.  We set it to false when they either:
    //   - type anything else
    //   - move the caret around
    // Thus we can determine the pattern "Typed '{', then pressed enter" from other
    // situations (like: "typed '{', pasted some content" or "typed '{' then went up a line and pressed enter").
    private boolean justAddedOpeningCurlyBracket;

    // Keep track of InputMethod (IM) character positions within this field,
    // e.g. for entering Chinese with the on-screen IME keyboard.
    // imStart is the start of the current sequence within this document of
    // "composing" characters that could end up staying as English or transformed into
    // a related Chinese (or other language) string.  imLength is the length (starting at
    // imStart) of this portion.  When there is no current IM entry, imStart is set to -1
    // and imLength is set to 0.
    private int imStart = -1;
    private int imLength;

    public FlowEditorPane(String content, FlowEditorPaneListener listener)
    {
        super(true, listener);
        this.listener = listener;
        setSnapToPixel(true);
        document = new HoleDocument();
        document.replaceText(0, 0, content);
        caret = document.trackPosition(0, Bias.FORWARD);
        // Important that the anchor is a different object to the caret, as they will move independently:
        anchor = document.trackPosition(0, Bias.FORWARD);
        
        
        updateRender(false);
        
        document.addListener(false, (origStartIncl, replaced, replacement, linesRemoved, linesAdded) -> {
            notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        });

        setOnInputMethodTextChanged(this::handleInputMethodEvent);
        // This implementation has been copied from TextInputControlSkin but it's important
        // to copy it because it uses our imStart/imLength fields.
        setInputMethodRequests(new ExtendedInputMethodRequests() {
            @Override public Point2D getTextLocation(int offset) {
                Scene scene = getScene();
                Window window = scene != null ? scene.getWindow() : null;
                if (window == null) {
                    return new Point2D(0, 0);
                }
                // Don't use imstart here because it isn't initialized yet.
                Bounds characterBounds = lineDisplay.getBoundsForRange(document, getSelectionStart(), getSelectionEnd())[0];
                Point2D p = localToScene(characterBounds.getMinX(), characterBounds.getMaxY());
                Point2D location = new Point2D(window.getX() + scene.getX() + p.getX(),
                        window.getY() + scene.getY() + p.getY());
                return location;
            }

            @Override public int getLocationOffset(int x, int y) {
                return FlowEditorPane.this.getCaretPositionForLocalPoint(FlowEditorPane.this.screenToLocal(x, y)).map(p -> p.getPosition()).orElse(Integer.MAX_VALUE) - imStart;
            }

            @Override public void cancelLatestCommittedText() {
                nonIMEdit();
            }

            @Override public String getSelectedText() {
                return FlowEditorPane.this.getSelectedText();
            }

            @Override public int getInsertPositionOffset() {
                int caretPosition = getCaretPosition();
                if (caretPosition < imStart) {
                    return caretPosition;
                } else if (caretPosition < imStart + imLength) {
                    return imStart;
                } else {
                    return caretPosition - imLength;
                }
            }

            @Override public String getCommittedText(int begin, int end) {
                if (begin < imStart) {
                    if (end <= imStart) {
                        return document.getContent(begin, end).toString();
                    } else {
                        return document.getContent(begin, imStart) + document.getContent(imStart + imLength, end + imLength).toString();
                    }
                } else {
                    return document.getContent(begin + imLength, end + imLength).toString();
                }
            }

            @Override public int getCommittedTextLength() {
                return document.getLength() - imLength;
            }
        });
    }

    /**
     * Called whenever the content of the field, or the position of the caret, has been changed
     * by an action *other* than an InputMethodEvent.  So we need to call this method at the start of
     * just about every other method in this class.
     */
    private void nonIMEdit()
    {
        imStart = -1;
        imLength = 0;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
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
                return getCaretPositionForLocalPoint(screenToLocal(screenPoint)).map(p -> p.getPosition()).orElse(0);
            case HELP:
                String err = listener.getErrorAtPosition(caret.position);
                if (err != null)
                    return "Error: " + err;
                else
                    break;
        }
        return super.queryAccessibleAttribute(accessibleAttribute, objects);
    }


    @OnThread(Tag.FXPlatform)
    private void handleInputMethodEvent(InputMethodEvent event)
    {
        // This is adapted from the default handler in TextInputControlSkin.  That handler
        // uses selection to keep track of some things.  We keep track manually here but
        // there is not currently a visual representation of the highlight.

        // Check we're in an editable state:
        if (this.isEditable() && !this.isDisabled())
        {

            // Committed text is the final bit that the user might select from the on-screen IME keyboard,
            // or the plain English bit by pressing a key.
            // Insert committed text
            if (event.getCommitted().length() != 0)
            {
                final int start = imStart;
                final int end = imStart + imLength;
                // This call will also position the caret at the end of the inserted text:
                FlowEditorPane.this.replaceText(start, end, event.getCommitted());
                // Set imstart and imlength back to having no selection:
                imStart = -1;
                imLength = 0;
                showHighlights(HighlightType.IME_INPUT, getImeInput());
                // I don't think committed and composed can both be there in one event, so return:
                return;
            }

            // If this is the first part of a composed text, set imstart,
            // and delete any existing selection:
            if (imStart == -1)
            {
                FlowEditorPane.this.replaceSelection("");
                imStart = this.getCaretPosition();
            }
            // Work out the full composed text:
            StringBuilder composed = new StringBuilder();
            for (InputMethodTextRun run : event.getComposed()) {
                composed.append(run.getText());
            }
            // Replace the IM section with the latest text:
            FlowEditorPane.this.replaceText(imStart, imStart + imLength, composed.toString());
            // Update imlength to the new composed length:
            imLength = composed.length();
            positionCaret(imStart + imLength);
            showHighlights(HighlightType.IME_INPUT, getImeInput());
        }
    }

    public List<int[]> getImeInput()
    {
        if (imStart != -1)
            return List.of(new int[]{imStart, imStart + imLength});
        else
            return List.of();
    }

    @Override
    protected EditorPosition makePosition(int line, int column)
    {
        return new TrackedPosition(document, document.getLineStart(line) + column, Bias.NONE);
    }

    @Override
    protected void keyPressed(KeyEvent event)
    {
        // All the key press events are handled by the editor actions system, except the context menu key:
        if (event.getCode() == KeyCode.CONTEXT_MENU)
        {
            showContextMenuAtCaret();
            nonIMEdit();
            event.consume();
        }
    }

    @Override
    protected void keyTyped(KeyEvent event)
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
            // Replace selection will call nonIMEdit for us:
            replaceSelection(character);
            JavaFXUtil.runAfterCurrent(() -> scheduleCaretUpdate(true));
            // Must do this last, to avoid the cursor movement in textChanged()
            // from cancelling our memory that they added a curly bracket without moving:
            justAddedOpeningCurlyBracket = character.equals("{");
        }
        
    }

    @Override
    protected void mousePressed(MouseEvent e)
    {
        requestFocus();
        if (e.getButton() == MouseButton.PRIMARY)
        {
            // If shift pressed, don't move anchor; form selection instead:
            boolean setAnchor = !e.isShiftDown();
            getCaretPositionForMouseEvent(e).ifPresent(p -> {
                if (setAnchor)
                    positionCaret(p.getPosition());
                else
                    moveCaret(p, true);
            });
            updateRender(true);
        }
    }

    @Override
    protected void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);
        if (e.isStillSincePress() && e.getButton() == MouseButton.MIDDLE)
        {
            getCaretPositionForMouseEvent(e).ifPresent(p -> {
                // If the clicked position is not in the current selection,
                // move the caret to that position first:
                if (p.getPosition() < getSelectionStart() || p.getPosition() > getSelectionEnd())
                {
                    positionCaret(p.getPosition());
                }
                listener.middleClickedAtPosition(e.getScreenX(), e.getScreenY());
            });
        }
    }

    @Override
    protected void mouseMoved(MouseEvent event)
    {
        getCaretPositionForMouseEvent(event).ifPresent(pos -> listener.showErrorPopupForCaretPos(pos.getPosition(), true));
    }

    // Make method public:
    @Override
    public Optional<EditorPosition> getCaretPositionForLocalPoint(Point2D localPoint)
    {
        return super.getCaretPositionForLocalPoint(localPoint);
    }

    public void textChanged()
    {
        updateRender(false);
        targetColumnForVerticalMovement = -1;
        callSelectionListeners();
        // FlowEditor is in charge of recording edits
    }

    @Override
    protected void updateRender(boolean ensureCaretVisible)
    {
        super.updateRender(ensureCaretVisible);

        if (errorQuery != null)
        {
            for (IndexRange indexRange : errorQuery.getErrorUnderlines())
            {
                addErrorUnderline(indexRange.getStart(), indexRange.getEnd());
            }
        }
    }

    @Override
    protected int getLineLength(int lineIndex)
    {
        return document.getLineLength(lineIndex);
    }

    @Override
    protected String getLineContentAtCaret()
    {
        return document.getLines().get(caret.getLine()).toString();
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
                BackgroundItem region = new BackgroundItem(0, getWidth() - MarginAndTextLine.textLeftEdge(true), 
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
        int posInLine = leftOfCharIndex - document.getLineStart(lineIndex);
        return lineDisplay.calculateLeftEdgeX(lineIndex, posInLine);
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

    protected Bounds getLineBoundsInScene(int line, boolean inclMargin)
    {
        if (lineDisplay.isLineVisible(line))
        {
            MarginAndTextLine marginAndTextLine = lineDisplay.getVisibleLine(line);
            Node node = inclMargin ? marginAndTextLine : marginAndTextLine.textLine;
            return node.localToScene(node.getBoundsInLocal());
        }
        else
        {
            return null;
        }
    }
    
    public Rectangle2D getLineBoundsOnScreen(int line, Point2D windowPos, double renderScaleX, double renderScaleY)
    {
        if (lineDisplay.isLineVisible(line))
        {
            // This is the reverse of the computation in FlowEditor.getTextPositionForScreenPos;
            // see the comment there for more info.
            MarginAndTextLine marginAndTextLine = lineDisplay.getVisibleLine(line);
            Bounds bounds = marginAndTextLine.textLine.localToScene(marginAndTextLine.textLine.getBoundsInLocal());
            double sceneX = marginAndTextLine.getScene().getX();
            double sceneY = marginAndTextLine.getScene().getY();
            double windowX = windowPos.getX();
            double windowY = windowPos.getY();
            return new Rectangle2D(
                (bounds.getMinX() + sceneX + windowX) * renderScaleX,
                (bounds.getMinY() + sceneY + windowY) * renderScaleY,
                bounds.getWidth() * renderScaleX,
                bounds.getHeight() * renderScaleY);
        }
        return null;
    }

    
    public double getFontSizeInPixels()
    {
        return lineDisplay.getFontSizeInPixels();
    }

    /**
     * Set the position of the caret and anchor, and scroll to ensure the caret is on screen.
     */
    public void positionCaret(int position)
    {
        caret.moveTo(position);
        anchor.moveTo(position);
        targetColumnForVerticalMovement = -1;
        justAddedOpeningCurlyBracket = false;
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
        justAddedOpeningCurlyBracket = false;
        updateRender(false);
        callSelectionListeners();
    }

    /**
     * Set the position of the caret without moving the anchor, and scroll to ensure the caret is on screen.
     */
    public void moveCaret(int position)
    {
        moveCaret(new TrackedPosition(document, position, Bias.NONE), true);
    }

    @Override
    protected void moveCaret(EditorPosition position, boolean ensureCaretVisible)
    {
        nonIMEdit();
        caret.moveTo(position.getPosition());
        targetColumnForVerticalMovement = -1;
        justAddedOpeningCurlyBracket = false;
        updateRender(ensureCaretVisible);
        callSelectionListeners();
    }

    /**
     * Set the position of the anchor without changing the caret or scrolling.
     */
    public void positionAnchor(int position)
    {
        anchor.moveTo(position);
        justAddedOpeningCurlyBracket = false;
        updateRender(false);
        callSelectionListeners();
    }
    
    @Override
    public EditorPosition getCaretEditorPosition()
    {
        return caret;
    }
    
    @Override
    public EditorPosition getAnchorEditorPosition()
    {
        return anchor;
    }

    public int getCaretPosition()
    {
        return caret.getPosition();
    }

    public int getAnchorPosition()
    {
        return anchor.getPosition();
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
        nonIMEdit(); // IME calls replaceText directly, not this method
        replaceText(getSelectionStart(), getSelectionEnd(), text);
    }

    // Looks at the current selection,
    // or otherwise the text around the given caret pos
    // If it is a valid Java identifier, returns it.
    // Returns null if the cursor is not in a valid Java identifier
    String getCurrentJavaIdentifier()
    {
        String sel = getSelectedText();
        if (sel.isEmpty())
        {
            // Need to find limits from where we are.
            int curPos = getCaretPosition();
            // Get line as a String, as that will help us with unicode:
            int lineNo = document.getLineFromPosition(curPos);
            int lineStart = document.getLineStart(lineNo);
            String line = document.getContent(lineStart, document.getLineEnd(lineNo)).toString();
            // Make pos relative to line:
            curPos -= lineStart;
            int left = curPos;
            // Keep going left to find identifier start:
            while (left > 0 && Character.isJavaIdentifierPart(line.codePointBefore(left)))
                left -= 1;
            // Check that the very first one can start:
            if (left < line.length() && !Character.isJavaIdentifierStart(line.codePointAt(left)))
                left = curPos;

            // Go right to find end:
            int right = curPos;
            while (right < line.length() && Character.isJavaIdentifierPart(line.codePointAt(right)))
                right += 1;
            return left == right ? null : line.substring(left, right);
        }
        else if (sel.length() < 255){
            // Only bother checking if the selection is small enough
            int[] codepoints = sel.codePoints().toArray();
            if (!Character.isJavaIdentifierStart(codepoints[0]))
                return null;
            for (int i = 1; i < codepoints.length; i++)
            {
                if (!Character.isJavaIdentifierPart(codepoints[i]))
                    return null;
            }
            return sel;
        }
        return null;
    }

    @Override
    protected void setLineDisplayPseudoclass(String pseudoclass, boolean on)
    {
        super.setLineDisplayPseudoclass(pseudoclass, on);
        lineDisplay.setPseudoclassOnAllVisibleLines(pseudoclass, on);
    }

    private enum EditType { IME_EDIT, NON_IME_EDIT}

    private void replaceText(int start, int end, String text)
    {
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

    /**
     * Is the user's most recent action to have typed an open curly bracket?
     * Used to decide whether to auto-add a closing curly bracket if they then press Enter.
     */
    public boolean hasJustAddedCurlyBracket()
    {
        return justAddedOpeningCurlyBracket;
    }

    @Override
    public double getWidthOfText(String content)
    {
        return lineDisplay.calculateLineWidth(content);
    }

    @Override
    protected int getLineCount()
    {
        return document.getLineCount();
    }

    @Override
    protected List<List<StyledSegment>> getStyledLines()
    {
        // Use an AbstractList rather than pre-calculate, as that means we don't bother
        // styling lines which will not be displayed:
        return new StyledLines(document, lineStyler);
    }

    @Override
    protected String getLongestLineInWholeDocument()
    {
        return document.getLongestLine();
    }

    public static interface FlowEditorPaneListener extends ScopeColors, BaseEditorPaneListener
    {
        public Set<Integer> getBreakpointLines();

        // Returns -1 if no step line
        int getStepLine();

        public void showErrorPopupForCaretPos(int caretPos, boolean mousePosition);

        public String getErrorAtPosition(int caretPos);

        public void middleClickedAtPosition(double screenX, double screenY);
    }

    // Use an AbstractList rather than pre-calculate, as that means we don't bother
    // styling lines which will not be displayed:
    // Important to implement empty RandomAccess interface for use of Collections.binarySearch()
    @OnThread(value = Tag.FXPlatform ,ignoreParent = true)
    static class StyledLines extends AbstractList<List<StyledSegment>> implements RandomAccess
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
