/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021,2022,2023,2024,2025  Michael Kolling and John Rosenberg

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
package bluej.terminal;

import bluej.Config;
import bluej.editor.base.BackgroundItem;
import bluej.editor.base.BaseEditorPane;
import bluej.editor.base.EditorPosition;
import bluej.editor.base.TextLine.StyledSegment;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.fxmisc.wellbehaved.event.EventPattern;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import javax.tools.Tool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A non-editable text pane that is used for the terminal text panes (stdout and stderr).  The requirements
 * for these panes are:
 *  - They must be styleable for things like method call recording and stack traces
 *  - They need to support clickable hyperlinks for stack traces
 *  - They must be read-only, because only the program should be able to output to these panes, not the user editing them
 *  - They must be focusable and navigable and accessibility-supported, for partially-sighted users to be able to read the output via screenreader
 */
public abstract class TerminalTextPane extends BaseEditorPane
{
    // One list entry per line of content.  Will always be at least one entry:
    private final ArrayList<ContentLine> content = new ArrayList<>();
    // Listeners to call when the content of the pane changes
    private final ArrayList<FXPlatformRunnable> contentListeners = new ArrayList<>();

    // The position of the caret and the anchor.  The Pos class is immutable so the instance
    // will be swapped out as a whole if it changes.
    private Pos caretPos = new Pos(0, 0, 0);
    private Pos anchorPos = new Pos(0, 0, 0);
    
    // A record holding a location in the terminal window.
    // The line may be negative in some circumstances (see Section, below).
    // In this case, column should be ignored.  If the column is Integer.MAX_VALUE
    // it means to take the whole line as included, no matter how long.
    // Both column and line are zero-based.
    record TerminalPos(int line, int column)
    {
        public TerminalPos subtractLines(int linesToSubtract)
        {
            return new TerminalPos(line - linesToSubtract, column);
        }
    } 
    
    // endLine is negative if ongoing.  startLine is negative if the start has scrolled off the top
    // It is possible for them to both be negative if the section is very long and ongoing.
    // All values of the pos are inclusive.  The columns should be ignored if the line is negative.
    record Section(TerminalPos start, TerminalPos end) {}
    
    private final ArrayList<Section> currentSections = new ArrayList<>();

    // Get the current end position of the content as a start position
    // This is different to getCurEnd() because it does not do any extra
    // calculation about trailing newlines.
    private TerminalPos getCurStart()
    {
        if (content.isEmpty())
        {
            return new TerminalPos(0, 0);
        }
        else
        {
            return new TerminalPos(content.size() - 1, content.get(content.size() - 1).getText().length());
        }
    }
    
    // Get the current end position of the content as an end position
    // This is different to getCurStart() because if the content ends in a newline
    // (the last content is a blank line), we take the end position as being the end
    // of the previous line, not the start of the new line (if there's no content on that line)
    private TerminalPos getCurEnd()
    {
        // If the final line is empty, we count the current end as the whole of the line before
        if (content.isEmpty())
        {
            return new TerminalPos(0, 0);
        }
        else
        {
            String lastLine = content.get(content.size() - 1).getText();
            if (lastLine.isEmpty())
            {
                return new TerminalPos(content.size() - 2, Integer.MAX_VALUE);
            }
            else
            {
                return new TerminalPos(content.size() - 1, lastLine.length());
            }
        }
    }
    
    public void markNewSection(String sectionTitle)
    {
        if (!currentSections.isEmpty())
        {
            int lastLineIndex = currentSections.size() - 1;
            // If current last section was marked as ongoing, finish it:
            Section last = currentSections.get(lastLineIndex);
            if (last.end.line < 0)
            {
                currentSections.set(lastLineIndex, new Section(last.start, getCurEnd()));
            }
        }
        currentSections.add(new Section(getCurStart(), new TerminalPos(-1, 0)));
    }

    // End the current section of content.
    public void endSection()
    {        
        if (!currentSections.isEmpty())
        {
            // If the content is empty we get rid of all sections: 
            if (content.isEmpty())
            {
                currentSections.clear();
            }
            else
            {
                int lastSection = currentSections.size() - 1;
                Section last = currentSections.get(lastSection);
                currentSections.set(lastSection, new Section(last.start, getCurEnd()));
                updateRender(false);
            }
        }
    }    
    
    
    public TerminalTextPane(Stage terminalWindow)
    {
        super(false, new BaseEditorPaneListener()
        {
            // There is not a special margin in the terminal to click:
            @Override
            public boolean marginClickedForLine(int lineIndex)
            {
                return false;
            }

            @Override
            public ContextMenu getContextMenuToShow(BaseEditorPane editorPane, Point2D screenCoords)
            {
                return new ContextMenu(JavaFXUtil.makeMenuItem(Config.getString("editor.copyLabel"), () -> ((TerminalTextPane)editorPane).copy(), null)); // TODO
            }

            @Override
            public void scrollEventOnTextLine(ScrollEvent e, BaseEditorPane editorPane)
            {
                editorPane.scrollEventOnTextLine(e);
            }
        });
        // Set the content to be empty on construction:
        clear();

        // Add shortcuts to allow changing font size:
        Nodes.addInputMap(this, InputMap.sequence(
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN)), e -> Utility.increaseFontSize(PrefMgr.getEditorFontSize())),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN)), e -> Utility.increaseFontSize(PrefMgr.getEditorFontSize())),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN)), e -> Utility.decreaseFontSize(PrefMgr.getEditorFontSize())),
                InputMap.consume(EventPattern.keyPressed(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN)), e -> PrefMgr.getEditorFontSize().set(PrefMgr.DEFAULT_JAVA_FONT_SIZE))
        ));

        JavaFXUtil.addChangeListenerPlatform(PrefMgr.getEditorFontSize(), s -> {
            lineDisplay.fontSizeChanged();
        });
    }

    @Override
    protected void keyPressed(KeyEvent event)
    {
        final int caretLine = caretPos.getLine();
        switch (event.getCode())
        {
            case CONTEXT_MENU:            
            {
                showContextMenuAtCaret();
                event.consume();
                return;
            }
            case ENTER:
            case SPACE:
            {
                Object customData = content.get(caretPos.getLine()).getCustomStyleDataAtColumn(caretPos.getColumn());
                if (customData != null && customData instanceof ExceptionSourceLocation)
                {
                    ((ExceptionSourceLocation)customData).showInEditor();
                    event.consume();
                }
                return;
            }
            case C:
            {
                // Ctrl/Cmd-C shortcut:
                if (event.isShortcutDown())
                {
                    copy();
                }
                return;
            }
            case TAB:
            {
                if (event.isShiftDown())
                    focusPrevious();
                else
                    focusNext();
                event.consume();
                return;
            }
            
            case UP:
            {
                int destLine = Math.max(0, caretLine - 1);
                caretPos = makePosition(destLine, Math.min(caretPos.getColumn(), getLineLength(destLine)));
                break;
            }
            case DOWN:
            {
                int destLine = Math.min(getLineCount() - 1, caretLine + 1);
                caretPos = makePosition(destLine, Math.min(caretPos.getColumn(), getLineLength(destLine)));
                break;
            }
            case LEFT:
            {
                int prevColumn = caretPos.getColumn() - 1;
                int destLine = caretLine;
                int destColumn;
                if (prevColumn < 0)
                {
                    if (caretLine > 0)
                    {
                        destLine = caretLine - 1;
                        destColumn = getLineLength(destLine);
                    }
                    else
                    {
                        destColumn = 0;
                    }
                }
                else
                {
                    destColumn = prevColumn;
                }
                caretPos = makePosition(destLine, destColumn);
                break;
            }
            case RIGHT:
            {
                int destLine = caretLine;
                int lineEnd = getLineLength(caretLine);
                int nextColumn = caretPos.getColumn() + 1;
                int destColumn;
                if (nextColumn > lineEnd)
                {
                    if (caretLine < getLineCount() - 1)
                    {
                        destLine = caretLine + 1;
                        destColumn = 0;
                    }
                    else
                    {
                        destColumn = lineEnd;
                    }
                }
                else
                {
                    destColumn = nextColumn;
                }
                caretPos = makePosition(destLine, destColumn);
                break;
            }
            default:
                // Not a key we care about -- skip everything below the switch:
                return;
        }
        // We'll only get here if we actually handled a key above, and didn't hit the default case:
        
        // Move the anchor if they're not holding shift:
        if (!event.isShiftDown())
            anchorPos = new Pos(caretPos.getPosition(), caretPos.getLine(), caretPos.getColumn());
        updateRender(true);
        event.consume();
    }
    
    public abstract void focusPrevious();
    public abstract void focusNext();

    @Override
    protected void updateRender(boolean ensureCaretVisible)
    {
        super.updateRender(ensureCaretVisible);
        // Recalculate all sections in the terminal:
        HashMap<Integer, List<BackgroundItem>> map = new HashMap<>();
        boolean reschedule = false;
        if (PrefMgr.getFlag(PrefMgr.SHOW_TERMINAL_SCOPES))
        {
            for (int i = 0; i < content.size(); i++)
            {
                // Can't work it out for non visible lines:
                if (!lineDisplay.isLineVisible(i))
                    continue;
                for (Section s : currentSections)
                {
                    final double singleRadius = 5;
                    // All are specific to this section, on this line:
                    double topRadius = 0, bottomRadius = 0;
                    // Top or bottom inset of 0 basically means "don't draw the grey line":
                    double topInset = 0, bottomInset = 0;
                    // Default is whole width:
                    double leftInset = 0, rightInset = getTextDisplayWidth() - 1.0;

                    // Each section could begin and/or end on the current line
                    // If neither, it may be ongoing through this line, or just not overlapping at all.
                    // So there's quite a few circumstances to consider.  We start with beginning:
                    if (s.start.line == i)
                    {
                        topRadius = singleRadius;
                        topInset = 1;
                        if (s.start.column >= 0 && s.start.column < content.get(i).getText().length())
                        {
                            Optional<Double> edge = lineDisplay.calculateLeftEdgeX(i, s.start.column);
                            reschedule |= edge.isEmpty();
                            leftInset = edge.orElse(leftInset);
                        }
                        if (s.end.line == i)
                        {
                            bottomRadius = singleRadius;
                            bottomInset = 1;
                            if (s.end.column >= 0 && s.end.column <= content.get(i).getText().length())
                            {
                                Optional<Double> edge = lineDisplay.calculateLeftEdgeX(i, s.end.column);
                                reschedule |= edge.isEmpty();
                                rightInset = edge.orElse(rightInset);
                            }
                        }
                    } else if (s.end.line == i)
                    {
                        bottomRadius = singleRadius;
                        bottomInset = 1;
                        if (s.end.column >= 0 && s.end.column <= content.get(i).getText().length())
                        {
                            Optional<Double> edge = lineDisplay.calculateLeftEdgeX(i, s.end.column);
                            reschedule |= edge.isEmpty();
                            rightInset = edge.orElse(rightInset);
                        }
                    } else if (!(s.start.line < i && (s.end.line == -1 || s.end.line > i)))
                    {
                        // Does not overlap this line at all:
                        continue;
                    }

                    CornerRadii radii = new CornerRadii(topRadius, topRadius, bottomRadius, bottomRadius, false);
                    Insets bodyInsets = new Insets(topInset, 1, bottomInset, 1);
                    map.computeIfAbsent(i, _i -> new ArrayList<>()).add(new BackgroundItem(leftInset, rightInset - leftInset,
                            new BackgroundFill(Color.LIGHTGRAY, radii, null),
                            new BackgroundFill(Color.WHITE, radii, bodyInsets)));
                }
            }
        }
        lineDisplay.applyScopeBackgrounds(map);
        if (reschedule)
        {
            JavaFXUtil.runAfterNextLayout(getScene(), () -> updateRender(false));
        }
    }

    public final void requestFocusAndShowCaret()
    {
        requestFocus();
        updateRender(true);
    }

    @Override
    protected void keyTyped(KeyEvent event)
    {
        // Pane is not editable, so typing does nothing.
    }

    @Override
    protected void mouseMoved(MouseEvent e)
    {
        // The text lines themselves are mouse-transparent.  So rather than use CSS styles,
        // we must change the cursor ourselves for the whole panel based on where the mouse is:
        getCaretPositionForMouseEvent(e).ifPresent(p -> {
            Object styleData = content.get(p.getLine()).getCustomStyleDataAtColumn(p.getColumn());
            // Checks non-null and sanity check for the type too:
            if (styleData != null && styleData instanceof ExceptionSourceLocation)
            {
                setCursor(Cursor.HAND);
            }
            else
            {
                setCursor(null);
            }
        });
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
                {
                    caretPos = new Pos(p.getPosition(), p.getLine(), p.getColumn());
                    anchorPos = new Pos(p.getPosition(), p.getLine(), p.getColumn());
                }
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
        if (e.isStillSincePress())
        {
            // If it was a click, and on a hyperlinked stack trace, follow the link:
            getCaretPositionForMouseEvent(e).ifPresent(p -> {
                Object styleData = content.get(p.getLine()).getCustomStyleDataAtColumn(p.getColumn());
                // Checks non-null and sanity check for the type too:
                if (styleData != null && styleData instanceof ExceptionSourceLocation)
                {
                    ((ExceptionSourceLocation)styleData).showInEditor();
                }
            });
        }
    }

    @Override
    protected Pos makePosition(int line, int column)
    {
        return new Pos(content.stream().limit(line).mapToInt(l -> l.getText().length()).sum() + column, line, column);
    }

    @Override
    protected void moveCaret(EditorPosition position, boolean ensureCaretVisible)
    {
        caretPos = new Pos(position.getPosition(), position.getLine(), position.getColumn());
        updateRender(ensureCaretVisible);
        callSelectionListeners();
    }

    /**
     * Trims to the most recent N lines of content.  Used when unlimited buffering is disabled to
     * keep the buffer to a specific number of lines.
     * @param numLines The maximum number of lines to allow in the content.  If there are more
     *                 lines in this, trim to this number of lines by removing excess lines from
     *                 the beginning (not from the end).
     */
    public void trimToMostRecentNLines(int numLines)
    {
        if (content.size() > numLines)
        {
            // Important to take a copy, as content will be blanked during the set:
            int linesToSubtract = content.size() - numLines;
            setContent(new ArrayList<>(content.subList(linesToSubtract, content.size())));
            // Adjust caret and anchor positions upwards by the trimmed lines:
            int newCaretLine = Math.max(0, caretPos.getLine() - linesToSubtract);
            caretPos = makePosition(
                newCaretLine, Math.min(caretPos.getColumn(), getLineLength(newCaretLine))
            );
            int newAnchorLine = Math.max(0, anchorPos.getLine() - linesToSubtract);
            anchorPos = makePosition(
                newAnchorLine, Math.min(anchorPos.getColumn(), getLineLength(newAnchorLine))
            );
            // Adjust line offset of any current lines in the display to match what we've just changed:
            for (ListIterator<Section> iterator = currentSections.listIterator(); iterator.hasNext(); )
            {
                Section s = iterator.next();
                // Check for scrolling off the top entirely, if it's not currently ongoing:
                if (s.end.line > 0 && s.end.line < linesToSubtract)
                {
                    iterator.remove();
                }
                else
                {
                    iterator.set(new Section(s.start.subtractLines(linesToSubtract), s.end.subtractLines(linesToSubtract)));
                }
            }
            updateRender(false);
        }
    }

    // Helper to set content and call listeners:
    private void setContent(List<ContentLine> lines)
    {
        content.clear();
        content.addAll(lines);
        refreshDisplay();
        contentChanged();
    }

    // Helper to call all the content listeners:
    private void contentChanged()
    {
        contentListeners.forEach(FXPlatformRunnable::run);
    }

    // Refresh the display according to the latest content
    public void refreshDisplay()
    {
        updateRender(false);
    }

    /**
     * Clear the content of the pane.
     */
    public void clear()
    {
        // Reset cursor and anchor to only remaining valid position:
        // Important to do this before setContent because that may use the caret position
        // while updating the display:
        caretPos = new Pos(0, 0, 0);
        anchorPos = new Pos(0, 0, 0);
        setContent(Collections.singletonList(new ContentLine(new ArrayList<>())));
        currentSections.clear();
        lineDisplay.applyScopeBackgrounds(Map.of());
    }

    /**
     * Get the text content of the pane as a list of lines.
     * @return The list of lines, without any newline characters.
     */
    public List<String> getLines()
    {
        return content.stream().map(line -> line.getText()).collect(Collectors.toList());
    }

    @Override
    protected int getLineLength(int lineIndex)
    {
        return content.get(lineIndex).getText().length();
    }

    @Override
    protected String getLineContentAtCaret()
    {
        return content.get(caretPos.line).getText();
    }

    @Override
    protected String getLongestLineInWholeDocument()
    {
        return content.stream().map(l -> l.getText()).max(Comparator.comparing(String::length)).orElse("");
    }

    @Override
    protected int getLineCount()
    {
        return content.size();
    }

    // Returns a copy, to avoid sharing.
    public List<List<StyledSegment>> getStyledLines()
    {
        return content.stream().map(line -> ImmutableList.copyOf(line)).collect(Collectors.toList());
    }

    @Override
    protected EditorPosition getCaretEditorPosition()
    {
        return caretPos;
    }

    @Override
    protected EditorPosition getAnchorEditorPosition()
    {
        return anchorPos;
    }

    /**
     * Appends a new styled segment to the end of the buffer.  If the text content has newlines,
     * they will be handled accordingly, splitting the content up into multiple lines.
     */
    public void append(StyledSegment styledSegment)
    {
        // Append, accounting for newlines:
        String remainder = styledSegment.getText().replaceAll("\r", "");
        while (!remainder.isEmpty())
        {
            int newlineIndex = remainder.indexOf('\n');
            if (newlineIndex == -1)
            {
                // No newline, just append it:
                content.get(content.size() - 1).append(new StyledSegment(styledSegment.getStyleClasses(), remainder));
                remainder = "";
            }
            else
            {
                String beforeNewline = remainder.substring(0, newlineIndex);
                content.get(content.size() - 1).append(new StyledSegment(styledSegment.getStyleClasses(), beforeNewline));
                content.add(new ContentLine(new ArrayList<>()));
                remainder = remainder.substring(newlineIndex + 1);
            }
        }
        refreshDisplay();
        contentChanged();
    }

    /**
     * Scrolls the pane all the way to the end
     */
    public void scrollToEnd()
    {
        lineDisplay.ensureLineVisible(content.size() - 1, getLineContainerHeight(), getLineCount());
        updateRender(false);
    }

    /**
     * Sets the style for a particular part of the content.  You should call refreshDisplay() afterwards to
     * make the changes actually take effect.
     * 
     * @param lineIndex The index of the line (zero-based, so zero is the first line)
     * @param start The start character index, within the line (zero-based, so zero is before the first character on the line)
     * @param end The end character index, within the line (zero-based, so zero is before the first character on the line)
     * @param cssClasses The classes to use for the style (replaces existing style)
     * @param customData The custom data to use for the style (replaces existing style)
     */
    public void setStyleForLineSegment(int lineIndex, int start, int end, List<String> cssClasses, Object customData)
    {
        Iterable<StyledSegment> origLine = content.get(lineIndex);
        ArrayList<StyledSegment> result = new ArrayList<>();
        // We need to find the right segment and replace it:
        int charsToSkip = start;
        int charsToConsume = end - start;
        for (StyledSegment segment : origLine)
        {
            int segmentLength = segment.getText().length();
            if (charsToConsume > 0 && charsToSkip < segmentLength)
            {
                // We need to split up this segment.
                
                // If there's a start bit unaffected, copy that over:
                if (charsToSkip > 0)
                {
                    result.add(new StyledSegment(segment.getStyleClasses(), segment.getText().substring(0, charsToSkip), segment.getCustomData()));
                }
                // Copy the affected part and replace style:
                int consumable = Math.min(charsToConsume, segmentLength - charsToSkip);
                result.add(new StyledSegment(cssClasses, segment.getText().substring(charsToSkip, charsToSkip + consumable), customData));
                charsToConsume -= consumable;
                // Copy any part after our part:
                if (consumable < segmentLength - charsToSkip)
                {
                    result.add(new StyledSegment(segment.getStyleClasses(), segment.getText().substring(charsToSkip + consumable), segment.getCustomData()));
                }
                
                // No more to skip:
                charsToSkip = 0;
            }
            else
            {
                result.add(segment);
                if (charsToConsume > 0 && charsToSkip > 0)
                    charsToSkip -= segmentLength;
            }
        }
        content.set(lineIndex, new ContentLine(result));
        // We don't call contentChanged here, because although the styles have changed, the text content has not
    }

    /**
     * Copy the current selection (if any) to the clipboard. 
     */
    public void copy()
    {
        StringBuilder copied = new StringBuilder();
        Pos startPos = anchorPos.getPosition() < caretPos.getPosition() ? anchorPos : caretPos;
        Pos endPos = anchorPos.getPosition() < caretPos.getPosition() ? caretPos : anchorPos;
        List<String> lines = getLines();
        if (startPos.getLine() == endPos.getLine())
        {
            copied.append(lines.get(startPos.getLine()).substring(startPos.getColumn(), endPos.getColumn()));
        }
        else
        {
            // First line:
            copied.append(lines.get(startPos.getLine()).substring(startPos.getColumn())).append("\n");
            // Inbetween lines:
            for (int line = startPos.getLine() + 1; line < endPos.getLine(); line++)
            {
                copied.append(lines.get(line)).append("\n");
            }
            // Last line (no newline):
            copied.append(lines.get(endPos.getLine()).substring(0, endPos.getColumn()));
        }
        if (copied.length() > 0)
            Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, copied.toString()));
    }

    /**
     * Add a listener to be called back when the text content of this pane may have changed.
     */
    public void addTextChangeListener(FXPlatformRunnable listener)
    {
        contentListeners.add(listener);
    }

    /**
     * Clear any selection by moving the anchor to be where the caret is.
     */
    public void deselect()
    {
        anchorPos = new Pos(caretPos.getPosition(), caretPos.getLine(), caretPos.getColumn());
        updateRender(false);
        callSelectionListeners();
    }

    // A helper class to track positions.  Tracks position in the whole document, but for convenience
    // also tracks line and column.  Will be invalidated if any content is changed in the document before
    // this point.
    private static class Pos implements EditorPosition
    {
        private final int line;
        private final int column;
        private final int position;

        public Pos(int position, int line, int column)
        {
            this.line = line;
            this.column = column;
            this.position = position;
        }

        @Override
        public int getLine()
        {
            return line;
        }

        @Override
        public int getColumn()
        {
            return column;
        }

        @Override
        public int getPosition()
        {
            return position;
        }
    }

}
