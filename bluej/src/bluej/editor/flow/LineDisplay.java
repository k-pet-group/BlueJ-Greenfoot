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

import bluej.utility.javafx.FXPlatformFunction;
import javafx.beans.binding.DoubleExpression;
import javafx.scene.Node;

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
    
    private final DoubleExpression heightProperty;

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
    List<Node> recalculateVisibleLines(Stream<CharSequence> allLines, FXPlatformFunction<Double, Double> snapHeight, double height, double fontSize)
    {
        // Start at the first visible line:
        Iterator<String> lines = allLines.skip(firstVisibleLineIndex).map(l -> l.toString()).iterator();
        double curY = firstVisibleLineOffset;
        int visLineSubIndex = 0;
        while (lines.hasNext() && curY <= height)
        {
            if (currentlyVisibleLines.size() - 1 < visLineSubIndex)
            {
                currentlyVisibleLines.add(new TextLine());
            }
            currentlyVisibleLines.get(visLineSubIndex).setText(lines.next(), fontSize);
            curY += snapHeight.apply(currentlyVisibleLines.get(visLineSubIndex).prefHeight(-1.0));
            visLineSubIndex += 1;

        }
        // Remove any excess lines:
        if (visLineSubIndex < currentlyVisibleLines.size())
        {
            currentlyVisibleLines.subList(visLineSubIndex, currentlyVisibleLines.size()).clear();
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

    public double getFirstVisibleLineOffset()
    {
        return firstVisibleLineOffset;
    }

    public int[] getLineRangeVisible()
    {
        return new int[] {firstVisibleLineIndex, firstVisibleLineIndex + currentlyVisibleLines.size() - 1};
    }

    /**
     * Scrolls the visible lines so that the given zero-based line index is in view.
     */
    public void ensureLineVisible(int line)
    {
        if (line < firstVisibleLineIndex)
        {
            // Scroll up:
            firstVisibleLineIndex = line;
            firstVisibleLineOffset = 0;
        }
        else if (line >= firstVisibleLineIndex + currentlyVisibleLines.size())
        {
            //Debug:
            double[] ys = currentlyVisibleLines.stream().mapToDouble(l -> l.getLayoutY()).toArray();
            
            // Scroll down:
            double singleLineHeight = currentlyVisibleLines.get(0).getHeight();
            int numLinesCanDisplay = (int)Math.ceil(heightProperty.get() / singleLineHeight);
            firstVisibleLineIndex = line - numLinesCanDisplay;
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
}
