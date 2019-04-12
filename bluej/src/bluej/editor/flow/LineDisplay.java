/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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

import bluej.editor.flow.TextLine.StyledSegment;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformFunction;
import com.google.common.collect.Multimap;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.HitInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class to handle the display of the set of visible lines in an editor window.
 * The visible lines will always be a contiguous subset of the full list of lines
 * in the document: the viewport shows lines N to N+W, where W is the number of
 * lines that can be fit vertically in the window.
 */
class LineDisplay
{
    // Handler for clicking in a line margin
    private final FXPlatformConsumer<Integer> onLineMarginClick;
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
    
    private final DoubleExpression heightProperty;
    private double averageLineHeight = 1.0;

    LineDisplay(DoubleExpression heightProperty, FXPlatformConsumer<Integer> onLineMarginClick)
    {
        this.heightProperty = heightProperty;
        this.onLineMarginClick = onLineMarginClick;
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
     * @param fontSize The height of the font (in points)
     * @return The ordered list of visible lines
     */
    List<Node> recalculateVisibleLines(List<List<StyledSegment>> allLines, FXPlatformFunction<Double, Double> snapHeight, double height, double fontSize)
    {
        // Start at the first visible line:
        Iterator<List<StyledSegment>> lines = allLines.subList(firstVisibleLineIndex, allLines.size()).iterator();
        double curY = firstVisibleLineOffset;
        int lineIndex = firstVisibleLineIndex;
        ArrayList<Double> lineHeights = new ArrayList<>();
        while (lines.hasNext() && curY <= height)
        {
            MarginAndTextLine line = visibleLines.computeIfAbsent(lineIndex, k -> new MarginAndTextLine(k + 1, new TextLine(), () -> onLineMarginClick.accept(k)));
            line.textLine.setText(lines.next(), fontSize);
            double lineHeight = snapHeight.apply(line.prefHeight(-1.0));
            curY += lineHeight;
            lineHeights.add(lineHeight);
            lineIndex += 1;
        }
        this.averageLineHeight = lineHeights.stream().mapToDouble(d -> d).average().orElse(1.0);
        
        // Remove any excess lines:
        int lastLineIndexIncl = lineIndex - 1;
        visibleLines.entrySet().removeIf(e -> e.getKey() < firstVisibleLineIndex || e.getKey() > lastLineIndexIncl);
        
        // Notify any rendering listeners of new line exposure:
        int[] lineRangeVisible = getLineRangeVisible();
        for (LineDisplayListener lineDisplayListener : lineDisplayListeners)
        {
            lineDisplayListener.renderedLines(lineRangeVisible[0], lineRangeVisible[1]);
        }
        
        return visibleLines.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey())).map(e -> e.getValue()).collect(Collectors.toList());
    }

    /**
     * Scrolls so that the given line index (zero-based) is shown at the top,
     * with the given pixel offset (zero or negative).
     */
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
        
        double overallPos = firstVisibleLineIndex * averageLineHeight - firstVisibleLineOffset;
        double newOverallPos = overallPos - deltaY;
        // Important to clamp in this order, as first clamp
        // may clamp too far, into negative:
        newOverallPos = Math.min(newOverallPos, averageLineHeight * documentLines - heightProperty.get());
        newOverallPos = Math.max(0, newOverallPos);
        int newTopLine = (int)Math.floor(newOverallPos / averageLineHeight);
        double newOffset = (newTopLine * averageLineHeight) - newOverallPos;
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
            double singleLineHeight = averageLineHeight;
            int numLinesCanDisplay = (int)Math.ceil(heightProperty.get() / singleLineHeight);
            firstVisibleLineIndex = line - numLinesCanDisplay + 1;
            if (firstVisibleLineIndex < 0)
            {
                // Just scroll to top:
                firstVisibleLineIndex = 0;
                firstVisibleLineOffset = 0.0;
            }
            else
            {
                double leftOver = (numLinesCanDisplay * singleLineHeight) % heightProperty.get();
                firstVisibleLineOffset = -leftOver;
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
        return averageLineHeight;
    }

    public void applyScopeBackgrounds(Map<Integer, List<Region>> scopeBackgrounds)
    {
        visibleLines.forEach((lineIndex, item) -> {
            item.textLine.setScopeBackgrounds(scopeBackgrounds.get(lineIndex));
        });
    }

    static interface LineDisplayListener
    {
        public void renderedLines(int fromLineIndexIncl, int toLineIndexIncl);
    }
    
    // Pair of ints; line index and column index (both zero based)
    public int[] getCaretPositionForMouseEvent(MouseEvent e)
    {
        for (int i = 0; i < visibleLines.size(); i++)
        {
            MarginAndTextLine currentlyVisibleLine = visibleLines.get(i + firstVisibleLineIndex);
            if (currentlyVisibleLine.getLayoutY() <= e.getY() && e.getY() <= currentlyVisibleLine.getLayoutY() + currentlyVisibleLine.getHeight())
            {
                // Can't use parentToLocal if layout bounds may be out of date:
                Point2D pointInLocal = new Point2D(e.getX() - currentlyVisibleLine.getLayoutX() - MarginAndTextLine.TEXT_LEFT_EDGE, e.getY() - currentlyVisibleLine.getLayoutY());
                HitInfo hitInfo = currentlyVisibleLine.textLine.hitTest(pointInLocal);
                if (hitInfo != null)
                {
                    return new int[] {i + firstVisibleLineIndex, hitInfo.getInsertionIndex()};
                }
            }
        }
        return null;
    }
}
