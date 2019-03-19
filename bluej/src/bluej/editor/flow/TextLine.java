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

import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A graphical line of text on the screen.  Extends TextFlow to add the ability
 * to display a selection shape on the line.
 */
@OnThread(Tag.FXPlatform)
class TextLine extends TextFlow
{
    // The selection shape (may be empty and invisible when not in use)
    private final Path selectionShape = new Path();
    
    public TextLine()
    {
        getStyleClass().add("text-line");
        setMouseTransparent(true);
        selectionShape.setStroke(null);
        selectionShape.setFill(Color.CORNFLOWERBLUE);
        selectionShape.setManaged(false);
        getChildren().add(selectionShape);
    }

    /**
     * Hides the selection so that no selection is shown on this line
     */
    public void hideSelection()
    {
        selectionShape.getElements().clear();
        selectionShape.setVisible(false);
    }

    /**
     * Shows the given selection shape on this line.
     * The items should have coordinates relative to this line.
     */
    public void showSelection(int start, int end, boolean extendToRight)
    {
        selectionShape.getElements().setAll(extendToRight(extendToRight, rangeShape(start, end)));
        selectionShape.setVisible(true);
        if (getScene() != null)
        {
            JavaFXUtil.runAfterNextLayout(getScene(), () -> {
                selectionShape.getElements().setAll(extendToRight(extendToRight, rangeShape(start, end)));
            });
        }
    }

    /**
     * If extendToRight is true, extend the shape all the way to the right of the line,
     * which helps make the selection look neater, and clears up confusion when the selection
     * involves empty lines.  If it is false, leave the shape untouched.
     */
    private PathElement[] extendToRight(boolean extendToRight, PathElement[] rangeShape)
    {
        if (extendToRight)
        {
            if (rangeShape.length == 5 && rangeShape[1] instanceof LineTo && rangeShape[2] instanceof LineTo)
            {
                ((LineTo)rangeShape[1]).setX(getWidth() - 1.0);
                ((LineTo)rangeShape[2]).setX(getWidth() - 1.0);
            }
            else if (rangeShape.length == 0)
            {
                // Blank line; make the selection ourselves:
                rangeShape = new PathElement[] {
                    new MoveTo(0, 0),
                    new LineTo(getWidth() - 1.0, 0),
                    new LineTo(getWidth() - 1.0, getHeight()),
                    new LineTo(0, getHeight()),
                    new LineTo(0, 0)
                };
            }
        }

        // Snap all the positions so that they meet up nicely without vertical gaps:
        for (PathElement pathElement : rangeShape)
        {
            if (pathElement instanceof LineTo)
            {
                LineTo lineTo = (LineTo) pathElement;
                lineTo.setX(snapPositionX(lineTo.getX()));
                lineTo.setY(snapPositionY(lineTo.getY()));
            }
            else if (pathElement instanceof MoveTo)
            {
                MoveTo moveTo = (MoveTo) pathElement;
                moveTo.setX(snapPositionX(moveTo.getX()));
                moveTo.setY(snapPositionY(moveTo.getY()));
            }
        }

        return rangeShape;
    }

    /**
     * Sets the text that is shown on this line.  Also hides any existing
     * selection shape for the line.
     * 
     * @param text The text to show.
     * @param size The font size to use.
     */
    public void setText(List<StyledSegment> text, double size)
    {
        hideSelection();
        getChildren().clear();
        getChildren().add(selectionShape);
        for (StyledSegment styledSegment : StyledSegment.mergeAdjacentIdentical(text))
        {
            Text t = new Text(styledSegment.text);
            t.setFont(new Font("Roboto Mono", size));
            t.getStyleClass().addAll(styledSegment.cssClasses);
            getChildren().add(t);
        }
    }

    /**
     * A piece of text content, plus a collection of CSS style classes to apply to the content.
     */
    public static class StyledSegment
    {
        private final List<String> cssClasses;
        private final String text;

        public StyledSegment(List<String> cssClasses, String text)
        {
            this.cssClasses = cssClasses;
            this.text = text;
        }

        /**
         * Gives back an iterator that returns the contents of the given list, but merging together any
         * adjacent run of segments that have identical styles.
         */
        private static Iterable<StyledSegment> mergeAdjacentIdentical(List<StyledSegment> segments)
        {
            return () -> new Iterator<StyledSegment>()
            {
                int nextToExamine = 0;
                
                @Override
                public boolean hasNext()
                {
                    return nextToExamine < segments.size();
                }

                @Override
                public StyledSegment next()
                {
                    StyledSegment next = segments.get(nextToExamine);
                    nextToExamine += 1;
                    // Merge in all following items that have exactly the same CSS classes:
                    while (nextToExamine < segments.size() && segments.get(nextToExamine).cssClasses.equals(next.cssClasses))
                    {
                        // Combine:
                        next = new StyledSegment(next.cssClasses, next.text + segments.get(nextToExamine).text);
                        nextToExamine += 1;
                    }
                    
                    return next;
                }
            };
        }
    }
}
