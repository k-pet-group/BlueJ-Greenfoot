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
import bluej.utility.javafx.FXPlatformFunction;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.HitInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A class to handle the display of the set of visible lines in an editor window.
 * The visible lines will always be a contiguous subset of the full list of lines
 * in the document: the viewport shows lines N to N+W, where W is the number of
 * lines that can be fit vertically in the window.
 */
class LineDisplay
{
    // Zero is the first line in document
    private int firstVisibleLineIndex = 0;
    // The display offset in pixels of the first visible line.
    // Always zero or negative, because if it would be positive, there would
    // be a display gap above the first visible line, which we don't allow to happen.
    private double firstVisibleLineOffset = 0; 
    // The ordered list of the current visible display lines.
    // The first line in the list corresponds to line firstVisibleLineIndex
    // in the actual document.
    private final ArrayList<TextLine> currentlyVisibleLines = new ArrayList<>();
    
    private final ArrayList<LineDisplayListener> lineDisplayListeners = new ArrayList<>();
    
    private final DoubleExpression heightProperty;
    private double averageLineHeight = 1.0;

    LineDisplay(DoubleExpression heightProperty)
    {
        this.heightProperty = heightProperty;
    }

    /**
     * Gets the visible line object corresponding to the given document line.
     * Throws an exception if that line is not visible (you should check first via isLineVisible).
     */
    TextLine getVisibleLine(int line)
    {
        if (!isLineVisible(line))
        {
            throw new IndexOutOfBoundsException("Line " + line + " is not visible.  Visible range is " + firstVisibleLineIndex + " to " + (firstVisibleLineIndex + currentlyVisibleLines.size()));
        }

        return currentlyVisibleLines.get(line - firstVisibleLineIndex);
    }

    /**
     * Checks if the given document line is currently visible on screen.
     */
    boolean isLineVisible(int line)
    {
        return line >= firstVisibleLineIndex && line < firstVisibleLineIndex + currentlyVisibleLines.size();
    }

    /**
     * Recalculates the set of visible lines, and returns them, ready to be used as children of 
     * an editor pane.
     * @param allLines The ordered stream of all lines in the document.
     * @param height The height of the graphical pane to render into, in pixels
     * @param fontSize The height of the font (in points)
     * @return The ordered list of visible lines
     */
    List<Node> recalculateVisibleLines(Stream<List<StyledSegment>> allLines, FXPlatformFunction<Double, Double> snapHeight, double height, double fontSize)
    {
        // Start at the first visible line:
        Iterator<List<StyledSegment>> lines = allLines.skip(firstVisibleLineIndex).iterator();
        double curY = firstVisibleLineOffset;
        int visLineSubIndex = 0;
        ArrayList<Double> lineHeights = new ArrayList<>();
        while (lines.hasNext() && curY <= height)
        {
            if (currentlyVisibleLines.size() - 1 < visLineSubIndex)
            {
                currentlyVisibleLines.add(new TextLine());
            }
            currentlyVisibleLines.get(visLineSubIndex).setText(lines.next(), fontSize);
            double lineHeight = snapHeight.apply(currentlyVisibleLines.get(visLineSubIndex).prefHeight(-1.0));
            curY += lineHeight;
            lineHeights.add(lineHeight);
            visLineSubIndex += 1;
        }
        this.averageLineHeight = lineHeights.stream().mapToDouble(d -> d).average().orElse(1.0);
        
        // Remove any excess lines:
        if (visLineSubIndex < currentlyVisibleLines.size())
        {
            currentlyVisibleLines.subList(visLineSubIndex, currentlyVisibleLines.size()).clear();
        }
        
        // Notify any rendering listeners of new line exposure:
        int[] lineRangeVisible = getLineRangeVisible();
        for (LineDisplayListener lineDisplayListener : lineDisplayListeners)
        {
            lineDisplayListener.lineVisibilityChanged(lineRangeVisible[0], lineRangeVisible[1]);
        }
        
        return Collections.unmodifiableList(currentlyVisibleLines);
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
        return new int[] {firstVisibleLineIndex, firstVisibleLineIndex + currentlyVisibleLines.size() - 1};
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
        else if (line >= firstVisibleLineIndex + currentlyVisibleLines.size() - 1)
        {
            //Debug:
            double[] ys = currentlyVisibleLines.stream().mapToDouble(l -> l.getLayoutY()).toArray();
            
            // Scroll down:
            double singleLineHeight = currentlyVisibleLines.get(0).getHeight();
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
        return currentlyVisibleLines.size();
    }

    public void addLineDisplayListener(LineDisplayListener lineDisplayListener)
    {
        lineDisplayListeners.add(lineDisplayListener);
    }

    public double getLineHeight()
    {
        return averageLineHeight;
    }

    static interface LineDisplayListener
    {
        public void lineVisibilityChanged(int fromLineIndexIncl, int toLineIndexIncl);
    }
    
    // Pair of ints; line index and column index (both zero based)
    public int[] getCaretPositionForMouseEvent(MouseEvent e)
    {
        for (int i = 0; i < currentlyVisibleLines.size(); i++)
        {
            TextLine currentlyVisibleLine = currentlyVisibleLines.get(i);
            // getLayoutBounds() seems to get out of date, so calculate manually:
            BoundingBox actualBounds = new BoundingBox(currentlyVisibleLine.getLayoutX(), currentlyVisibleLine.getLayoutY(), currentlyVisibleLine.getWidth(), currentlyVisibleLine.getHeight());
            if (currentlyVisibleLine.getLayoutY() <= e.getY() && e.getY() <= currentlyVisibleLine.getLayoutY() + currentlyVisibleLine.getHeight())
            {
                // Can't use parentToLocal if layout bounds may be out of date:
                Point2D pointInLocal = new Point2D(e.getX() - currentlyVisibleLine.getLayoutX(), e.getY() - currentlyVisibleLine.getLayoutY());
                HitInfo hitInfo = currentlyVisibleLine.hitTest(pointInLocal);
                if (hitInfo != null)
                {
                    return new int[] {i + firstVisibleLineIndex, hitInfo.getInsertionIndex()};
                }
            }
        }
        return null;
    }
}
