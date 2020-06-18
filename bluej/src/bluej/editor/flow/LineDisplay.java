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

import bluej.editor.flow.FlowEditorPane.FlowEditorPaneListener;
import bluej.editor.flow.TextLine.StyledSegment;
import bluej.utility.javafx.FXFunction;
import bluej.utility.javafx.FXSupplier;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;
import javafx.scene.text.HitInfo;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class to handle the display of the set of visible lines in an editor window.
 * The visible lines will always be a contiguous subset of the full list of lines
 * in the document: the viewport shows lines N to N+W, where W is the number of
 * lines that can be fit vertically in the window.
 */
@OnThread(Tag.FX)
public class LineDisplay
{
    // Handler for clicking in a line margin
    private final FlowEditorPaneListener flowEditorPaneListener;
    // Zero is the first line in document
    private int firstVisibleLineIndex = 0;
    // The display offset in pixels of the first visible line.
    // Always zero or negative, because if it would be positive, there would
    // be a display gap above the first visible line, which we don't allow to happen.
    private double firstVisibleLineOffset = 0; 
    // The collection of current visible display lines.  This is always a contiguous 
    // block of numbered lines, starting with firstVisibleLineIndex (inclusive).
    private final Map<Integer, MarginAndTextLine> visibleLines = new HashMap<>();
    
    private final ArrayList<LineDisplayListener> lineDisplayListeners = new ArrayList<>();
    
    private final StringExpression fontCSS;
    private final FXSupplier<Double> getHeight;
    private final DoubleExpression horizScrollProperty;
    private double lineHeightEstimate = 1.0;    

    public LineDisplay(FXSupplier<Double> getHeight, DoubleExpression horizScrollProperty, StringExpression fontCSS, FlowEditorPaneListener flowEditorPaneListener)
    {
        this.fontCSS = fontCSS;
        this.getHeight = getHeight;
        this.horizScrollProperty = horizScrollProperty;
        this.flowEditorPaneListener = flowEditorPaneListener;
    }

    /**
     * Gets the visible line object corresponding to the given document line.
     * Throws an exception if that line is not visible (you should check first via isLineVisible).
     */
    MarginAndTextLine getVisibleLine(int line)
    {
        if (!isLineVisible(line))
        {
            throw new IndexOutOfBoundsException("Line " + line + " is not visible.  Visible range is " + firstVisibleLineIndex + " to " + (firstVisibleLineIndex + visibleLines.size()));
        }

        return visibleLines.get(line);
    }

    /**
     * Checks if the given document line is currently visible on screen.
     */
    boolean isLineVisible(int line)
    {
        return line >= firstVisibleLineIndex && line < firstVisibleLineIndex + visibleLines.size();
    }

    /**
     * Recalculates the set of visible lines, and returns them, ready to be used as children of 
     * an editor pane.
     * @param allLines The ordered stream of all lines in the document.
     * @param height The height of the graphical pane to render into, in pixels
     * @return The ordered list of visible lines
     */
    @OnThread(Tag.FX)
    List<MarginAndTextLine> recalculateVisibleLines(List<List<StyledSegment>> allLines, FXFunction<Double, Double> snapHeight, double xTranslate, double width, double height, boolean lineWrapping)
    {
        if (firstVisibleLineIndex >= allLines.size())
        {
            // Content must have been deleted, so for safety, reset to showing the last line:
            firstVisibleLineIndex = allLines.size() - 1;
            firstVisibleLineOffset = 0;
        }
        
        final int lastLineIndexIncl;
        if (!lineWrapping)
        {
            double lineHeight = snapHeight.apply(calculateLineHeight());
            //Debug.message("Line height: " + lineHeight);

            // Here's how we work out how many lines we need to draw.  We'll always need to draw
            // firstVisibleLineIndex, which we know takes up (lineHeight + firstVisibleLineOffset) pixels
            // (since the offset is always between -lineHeight and zero, this comes out positive
            //   in the range zero to lineHeight)
            // So we subtract that from the height, then divide by lineHeight and take the ceil to
            // work out how many more lines we need, and add one for the top line.
            int linesToDraw = 1 + (int)Math.ceil((height - (lineHeight + firstVisibleLineOffset)) / lineHeight);
            
            // Start at the first visible line:
            Iterator<List<StyledSegment>> lines = allLines.subList(firstVisibleLineIndex, Math.min(linesToDraw + firstVisibleLineIndex, allLines.size())).iterator();
            int lineIndex = firstVisibleLineIndex;
            while (lines.hasNext())
            {
                MarginAndTextLine line = visibleLines.computeIfAbsent(lineIndex, k -> new MarginAndTextLine(k + 1, new TextLine(lineWrapping), () -> flowEditorPaneListener.marginClickedForLine(k), () -> flowEditorPaneListener.getContextMenuToShow()));
                line.textLine.setText(lines.next(), xTranslate, false, fontCSS);
                lineIndex += 1;
            }
            //Debug.message("Lines: " + firstVisibleLineIndex + " to " + lineIndex + " giving " + (lineIndex - firstVisibleLineIndex));
            // Line heights can vary slightly so max is more reliable than average:
            this.lineHeightEstimate = lineHeight;
            lastLineIndexIncl = lineIndex - 1;
        }
        else
        {
            // Line wrapping also means we're printing.  We must calculate each line's height individually:
            double totalHeightSoFar = 0;
            int lineIndex;
            for (lineIndex = firstVisibleLineIndex; lineIndex < allLines.size() && totalHeightSoFar < height; lineIndex += 1)
            {
                MarginAndTextLine line = visibleLines.computeIfAbsent(lineIndex, k -> new MarginAndTextLine(k + 1, new TextLine(lineWrapping), () -> flowEditorPaneListener.marginClickedForLine(k), () -> flowEditorPaneListener.getContextMenuToShow()));
                line.textLine.setText(allLines.get(lineIndex), xTranslate, true, fontCSS);
                double lineHeight = calculateLineHeight(allLines.get(lineIndex), width);
                totalHeightSoFar += snapHeight.apply(lineHeight);
            }
            lastLineIndexIncl = lineIndex - 1;
        }
        
        // Remove any excess lines:
        visibleLines.entrySet().removeIf(e -> e.getKey() < firstVisibleLineIndex || e.getKey() > lastLineIndexIncl);
        
        // Notify any rendering listeners of new line exposure:
        int[] lineRangeVisible = getLineRangeVisible();
        for (LineDisplayListener lineDisplayListener : lineDisplayListeners)
        {
            if (lineRangeVisible[1] >= lineRangeVisible[0])
            {
                lineDisplayListener.renderedLines(lineRangeVisible[0], lineRangeVisible[1]);
            }
        }
        
        return visibleLines.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey())).map(e -> e.getValue()).collect(Collectors.toList());
    }

    /**
     * Scrolls so that the given line index (zero-based) is shown at the top,
     * with the given pixel offset (zero or negative).
     */
    @OnThread(Tag.FX)
    void scrollTo(int lineIndex, double lineOffset)
    {
        firstVisibleLineIndex = lineIndex;
        firstVisibleLineOffset = lineOffset;
    }
    
    void scrollBy(double deltaY, int documentLines)
    {
        // Negative deltaY tries to move down the document, i.e.
        // tries to increase firstVisibleLineIndex
        if (deltaY == 0)
            return;
        
        double overallPos = firstVisibleLineIndex * lineHeightEstimate - firstVisibleLineOffset;
        double newOverallPos = overallPos - deltaY;
        // Important to clamp in this order, as first clamp
        // may clamp too far, into negative:
        newOverallPos = Math.min(newOverallPos, lineHeightEstimate * documentLines - getHeight.get());
        newOverallPos = Math.max(0, newOverallPos);
        int newTopLine = (int)Math.floor(newOverallPos / lineHeightEstimate);
        double newOffset = (newTopLine * lineHeightEstimate) - newOverallPos;
        scrollTo(newTopLine, newOffset);
        /*
        // How many lines have we moved the top visible line by?
        // Sign is opposite to deltaY. 
        int movedBy = 0;
        
        // We get offset to zero, then scroll whole lines, then
        // finally adjust offset again:
        if (firstVisibleLineOffset != 0.0)
        {
            if (deltaY < 0)
            {
                // Scrolling down document, so moving lines upwards,
                double distToNextTop = averageLineHeight + firstVisibleLineOffset;
                if (-deltaY < distToNextTop)
                {
                    // Can do it by offset alone
                    firstVisibleLineOffset += deltaY;
                    return;
                }
                else
                {
                    deltaY += distToNextTop;
                    firstVisibleLineOffset = 0;
                    movedBy += 1;
                }
            }
            else
            {
                // Scrolling up document, so moving lines downwards
                double distToNextTop = -firstVisibleLineOffset;
                if (deltaY < distToNextTop)
                {
                    // Can do it by offset alone
                    firstVisibleLineOffset += deltaY;
                    return;
                }
                else
                {
                    deltaY -= distToNextTop;
                    firstVisibleLineOffset = 0;
                }
            }
        }
        // Now scroll entire lines:
        // TODO watch for hitting document end!
        while (Math.abs(deltaY) > averageLineHeight)
        {
            deltaY -= Math.signum(deltaY) * averageLineHeight;
            movedBy -= (int)Math.signum(deltaY);
        }
        // Now scroll last part by offset:
        if (deltaY != )
            */
        
    }

    public double getFirstVisibleLineOffset()
    {
        return firstVisibleLineOffset;
    }

    /**
     * First element is the first line index (zero-based) that is visible, inclusive.
     * Second element is the last line index (zero-based) that is visible, also inclusive.
     */
    @OnThread(Tag.FX)
    public int[] getLineRangeVisible()
    {
        return new int[] {firstVisibleLineIndex, firstVisibleLineIndex + visibleLines.size() - 1};
    }

    /**
     * Scrolls the visible lines so that the given zero-based line index is in view.
     */
    public void ensureLineVisible(int line)
    {
        // Note: if the line is the first/last visible, it may be only partially visible, so we still 
        // scroll because we may need to move slightly to bring the whole line into view.
        
        if (line <= firstVisibleLineIndex)
        {
            // Scroll up:
            firstVisibleLineIndex = line;
            firstVisibleLineOffset = 0;
        }
        else if (line >= firstVisibleLineIndex + visibleLines.size() - 1)
        {            
            // Scroll down:
            double singleLineHeight = lineHeightEstimate;
            // As an example, imagine each line is 10 pixels high, and the whole pane is 84 pixels high.
            // You will need to draw nine lines (so ceil(84 / 10) ) because if you only drew 8, there would be 4 pixels undrawn: 
            int numLinesCanDisplay = (int)Math.ceil(getHeight.get() / singleLineHeight);
            // Imagine that you're drawing 9 lines, and you want to show line #14, which will be the last one.  You need to render lines 6,7,8,9,10,11,12,13,14
            // So the top line is 14 - 9 + 1 = 6
            firstVisibleLineIndex = line - numLinesCanDisplay + 1;
            if (firstVisibleLineIndex < 0)
            {
                // Just scroll to top:
                firstVisibleLineIndex = 0;
                firstVisibleLineOffset = 0.0;
            }
            else
            {
                // If we are drawing 9 lines with #14 at the bottom, the top line (#6) will be only partially visible.  So
                // if we have 84 pixels high, we want to slide the top line up 6 pixels, which is 84 - 9*10 = -6.
                firstVisibleLineOffset = getHeight.get() - (numLinesCanDisplay * singleLineHeight);
            }
        }
        // Otherwise, it is visible -- nothing to do.
    }

    public int getVisibleLineCount()
    {
        return visibleLines.size();
    }

    public void addLineDisplayListener(LineDisplayListener lineDisplayListener)
    {
        lineDisplayListeners.add(lineDisplayListener);
    }

    public double getLineHeight()
    {
        return lineHeightEstimate;
    }

    public void applyScopeBackgrounds(Map<Integer, List<BackgroundItem>> scopeBackgrounds)
    {
        visibleLines.forEach((lineIndex, item) -> {
            item.textLine.setScopeBackgrounds(scopeBackgrounds.get(lineIndex));
        });
    }

    /**
     * The font size has changed; change the size of the text on all visible lines.
     */
    public void fontSizeChanged()
    {
        for (MarginAndTextLine line : visibleLines.values())
        {
            line.fontSizeChanged(fontCSS);
        }
    }

    public void hideAllErrorUnderlines()
    {
        for (MarginAndTextLine marginAndTextLine : visibleLines.values())
        {
            marginAndTextLine.textLine.hideErrorUnderline();
        }
    }

    @OnThread(Tag.FXPlatform)
    public Bounds[] getBoundsForRange(Document document, int startOffset, int endOffset)
    {
        int firstLine = document.getLineFromPosition(startOffset);
        int lastLine = document.getLineFromPosition(endOffset);

        return IntStream.rangeClosed(firstLine, lastLine)
            .mapToObj(line -> {
                if (!isLineVisible(line))
                    return null;
                MarginAndTextLine node = getVisibleLine(line);
                Path path = new Path(node.textLine.rangeShape(Math.max(startOffset, document.getLineStart(line)), Math.min(endOffset, document.getLineEnd(line))));
                return node.localToParent(node.textLine.localToParent(path.getBoundsInLocal()));
            }).filter(b -> b != null).toArray(Bounds[]::new);
    }

    static interface LineDisplayListener
    {
        @OnThread(Tag.FX)
        public void renderedLines(int fromLineIndexIncl, int toLineIndexIncl);
    }
    
    // Pair of ints; line index and column index (both zero based)
    public int[] getCaretPositionForLocalPoint(Point2D localPoint)
    {
        for (int i = 0; i < visibleLines.size(); i++)
        {
            MarginAndTextLine currentlyVisibleLine = visibleLines.get(i + firstVisibleLineIndex);
            if (currentlyVisibleLine != null && currentlyVisibleLine.getLayoutY() <= localPoint.getY() && localPoint.getY() <= currentlyVisibleLine.getLayoutY() + currentlyVisibleLine.getHeight())
            {
                // Can't use parentToLocal if layout bounds may be out of date:
                Point2D pointInLocal = new Point2D(localPoint.getX() - currentlyVisibleLine.getLayoutX() - MarginAndTextLine.TEXT_LEFT_EDGE + horizScrollProperty.get(), localPoint.getY() - currentlyVisibleLine.getLayoutY());
                HitInfo hitInfo = currentlyVisibleLine.textLine.hitTest(pointInLocal);
                if (hitInfo != null)
                {
                    return new int[] {i + firstVisibleLineIndex, hitInfo.getInsertionIndex()};
                }
            }
        }
        return null;
    }

    /**
     * Calculates the anticipated width of the given line of text.
     * @return The line width, in pixels
     */
    public double calculateLineWidth(String line)
    {
        TextLine textLine = new TextLine(false);
        // Must be in a scene for CSS (for font family/size) to get applied correctly:
        Scene s = new Scene(textLine);
        textLine.setText(List.of(new StyledSegment(List.of(), line)), 0, true, fontCSS);
        textLine.applyCss();
        textLine.layout();
        return textLine.prefWidth(-1);
    }

    /**
     * Calculates the anticipated height of a single line of text.
     */
    public double calculateLineHeight()
    {
        return calculateLineHeight(List.of(new StyledSegment(List.of(), "Xy")), -1);
    }

    /**
     * Calculates the anticipated height of a given line of text, wrapping at maxWidth.
     */
    public double calculateLineHeight(List<StyledSegment> content, double maxWidth)
    {
        TextLine textLine = new TextLine(false);
        // Must be in a scene for CSS (for font family/size) to get applied correctly:
        Scene s = new Scene(textLine);
        textLine.setText(content, 0, true, fontCSS);
        textLine.applyCss();
        textLine.layout();
        return textLine.prefHeight(maxWidth);
    }
}
