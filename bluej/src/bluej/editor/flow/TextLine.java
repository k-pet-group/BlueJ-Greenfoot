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

import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.ResizableRectangle;
import com.google.common.collect.Lists;
import javafx.beans.binding.StringExpression;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * A graphical line of text on the screen.  Extends TextFlow to add the ability
 * to display a selection shape on the line.
 */
@OnThread(Tag.FX)
public class TextLine extends TextFlow
{
    static enum HighlightType
    {
        FIND_RESULT, BRACKET_MATCH;
    }

    private final boolean printing;
    // The selection shape (may be empty and invisible when not in use)
    private final Path selectionShape = new Path();
    private final Path findResultShape = new Path();
    private final Path bracketMatchShape = new Path();
    private final Path errorUnderlineShape = new Path();
    // Ranges are column locations relative to start of line, 0 is beginning.
    private final ArrayList<IndexRange> errorLocations = new ArrayList<>();
    
    private List<BackgroundItem> backgroundNodes = Collections.emptyList();
    private List<StyledSegment> latestContent = Collections.emptyList();
    private final Rectangle clip;

    public TextLine(boolean printing)
    {
        this.printing = printing;
        getStyleClass().add("text-line");
        setMouseTransparent(true);
        selectionShape.getStyleClass().add("flow-selection");
        selectionShape.setManaged(false);
        findResultShape.getStyleClass().add("flow-find-result");
        findResultShape.setManaged(false);
        bracketMatchShape.getStyleClass().add("flow-bracket-match");
        bracketMatchShape.setManaged(false);
        errorUnderlineShape.setStroke(Color.RED);
        errorUnderlineShape.setFill(null);
        errorUnderlineShape.setManaged(false);
        getChildren().setAll(bracketMatchShape, findResultShape, selectionShape, errorUnderlineShape);
        clip = new ResizableRectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
    }

    @Override
    @OnThread(value = Tag.FX, ignoreParent = true)
    protected void layoutChildren()
    {
        super.layoutChildren();
        // Also do unmanaged BackgroundItem:
        for (Node child : getChildren())
        {
            if (child instanceof BackgroundItem)
            {
                BackgroundItem backgroundItem = (BackgroundItem)child;
                // For reasons unknown, JavaFX doesn't align consecutive lines' backgrounds properly when printing
                // so we need to add an extra pixel at the top and bottom to avoid white gaps between lines:
                backgroundItem.resizeRelocate(backgroundItem.x, printing ? -1 : 0, backgroundItem.width, getHeight() + (printing ? 2 : 0));
            }
        }
    }

    /**
     * Hides the selection so that no selection is shown on this line
     */
    public void hideSelection()
    {
        selectionShape.getElements().clear();
        selectionShape.setVisible(false);
    }
    
    private void runOnceLaidOut(FXRunnable action)
    {
        if (isNeedsLayout())
        {
            // We run ourselves, not the action, in case multiple layout passes are needed and we need to defer multiple times:
            JavaFXUtil.runAfterNextLayout(getScene(), () -> runOnceLaidOut(action));
        }
        else
        {
            action.run();
        }
    }

    /**
     * Shows the given selection shape on this line.
     * The items should have coordinates relative to this line.
     */
    public void showSelection(int start, int end, boolean extendToRight)
    {
        runOnceLaidOut(() -> {
            selectionShape.getElements().setAll(extendShape(extendToRight, rangeShape(start, end)));
            selectionShape.setVisible(true);
        });
    }

    /**
     * Extends the shape to fit the vertical height of the line.
     * Also, if extendToRight is true, extend the shape all the way to the right of the line,
     * which helps make the selection look neater, and clears up confusion when the selection
     * involves empty lines.  If it is false, leave the shape untouched.
     */
    private PathElement[] extendShape(boolean extendToRight, PathElement[] rangeShape)
    {
        // Extend down and left lines to reach bottom of full line height:
        double height = getHeight();
        if (rangeShape.length == 5 && rangeShape[2] instanceof LineTo && rangeShape[3] instanceof LineTo)
        {
            ((LineTo)rangeShape[2]).setY(height);
            ((LineTo)rangeShape[3]).setY(height);
        }
        
        if (extendToRight)
        {
            double width = getWidth();
            if (rangeShape.length == 5 && rangeShape[1] instanceof LineTo && rangeShape[2] instanceof LineTo)
            {
                ((LineTo)rangeShape[1]).setX(width - 1.0);
                ((LineTo)rangeShape[2]).setX(width - 1.0);
            }
            else if (rangeShape.length == 0)
            {
                // Selection begins at the end of the line (including case where line is blank); make the selection ourselves from right edge of text:
                double rhs = Utility.findLast(getChildren().stream().filter(t -> t instanceof Text).map(n -> n.getBoundsInParent().getMaxX())).orElse(0.0);
                
                
                rangeShape = new PathElement[] {
                    new MoveTo(rhs, 0),
                    new LineTo(width - 1.0, 0),
                    new LineTo(width - 1.0, getHeight()),
                    new LineTo(rhs, getHeight()),
                    new LineTo(rhs, 0)
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
     * @param fontChanged Has the font size changed since last call?
     */
    public void setText(List<StyledSegment> text, double xTranslate, boolean fontChanged, StringExpression fontCSS)
    {
        setTranslateX(xTranslate);
        clip.setX(-xTranslate);
        text = Lists.newArrayList(StyledSegment.mergeAdjacentIdentical(text));
        if (!fontChanged && latestContent.equals(text))
        {
            return;
        }
        
        hideSelection();
        hideErrorUnderline();
        getChildren().clear();
        getChildren().addAll(backgroundNodes);
        getChildren().addAll(bracketMatchShape, findResultShape, selectionShape);
        for (StyledSegment styledSegment : text)
        {
            Text t = new Text(styledSegment.text);
            t.setStyle(fontCSS.getValue());
            t.getStyleClass().add("editor-text");
            t.getStyleClass().addAll(styledSegment.cssClasses);
            getChildren().add(t);
        }
        getChildren().add(errorUnderlineShape);
        latestContent = new ArrayList<>(text);
    }

    public void showError(int startColumn, int endColumn)
    {
        errorLocations.add(new IndexRange(startColumn, endColumn));
        runOnceLaidOut(() -> {
            // Note: it is possible between the call and the lay out that the errorLocations
            // change.  That's fine, we just use the latest one (which may be empty):
            errorUnderlineShape.getElements().setAll(errorLocations.stream().flatMap(r -> makeSquiggle(rangeShape(r.getStart(), r.getEnd())).stream()).collect(Collectors.toList()));
            errorUnderlineShape.setVisible(!errorUnderlineShape.getElements().isEmpty());
        });
    }

    // Each item is size 2, start pos incl and end pos excl.  No other highlights of this kind will be shown on the line,
    // so call this method with the complete set you want to show on this line.
    // To turn off, call with an empty list.
    public void showHighlight(HighlightType highlightType, List<int[]> positions)
    {
        runOnceLaidOut(() -> {
            Path shape = highlightType == HighlightType.FIND_RESULT ? this.findResultShape : this.bracketMatchShape;
            shape.getElements().setAll(positions.stream().flatMap(p -> Arrays.stream(rangeShape(p[0], p[1]))).toArray(PathElement[]::new));
            shape.setVisible(!shape.getElements().isEmpty());
        });
    }

    private List<PathElement> makeSquiggle(PathElement[] rectShape)
    {
        ArrayList<PathElement> squiggle = new ArrayList<>(); 
        if (rectShape.length == 5
            && rectShape[2] instanceof LineTo && rectShape[3] instanceof LineTo)
        {
            double leftHandX = ((LineTo)rectShape[3]).getX();
            double rightHandX = ((LineTo)rectShape[2]).getX();
            double y = ((LineTo)rectShape[2]).getY() - 1;
            
            // Minimum size for underline:
            double width = Math.max(9, rightHandX - leftHandX);
            boolean downStroke = true;
            double x = snapPositionX(leftHandX);
            squiggle.add(new MoveTo(x, snapPositionY(y - 2)));
            do
            {
                x += 3;
                squiggle.add(new LineTo(snapPositionX(x), snapPositionY(downStroke ? y + 1 : y - 2)));
                downStroke = !downStroke;
            }
            while (x < snapPositionX(leftHandX + width));
        }
        else
        {
            // Backup case, use full path:
            squiggle.addAll(Arrays.asList(rectShape));
        }
        return squiggle;
    }

    public void hideErrorUnderline()
    {
        errorLocations.clear();
        errorUnderlineShape.getElements().clear();
        errorUnderlineShape.setVisible(false);
    }

    public void setScopeBackgrounds(Collection<BackgroundItem> nodes)
    {
        if (nodes == null)
            nodes = Collections.emptyList();
        
        this.backgroundNodes = new ArrayList<>(nodes);
        int selectionIndex = getChildren().indexOf(bracketMatchShape);
        getChildren().remove(0, selectionIndex);
        getChildren().addAll(0, backgroundNodes);
        requestLayout();
    }

    /**
     * Changes the font size of the text on the line, without altering the content.
     * @param fontCSS
     */
    public void fontSizeChanged(StringExpression fontCSS)
    {
        List<StyledSegment> content = this.latestContent;
        // Avoid check for identical content:
        latestContent = Collections.emptyList();
        setText(content, getTranslateX(), true, fontCSS);
    }

    /**
     * A piece of text content, plus a collection of CSS style classes to apply to the content.
     */
    @OnThread(Tag.FX)
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

        public String getText()
        {
            return text;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StyledSegment that = (StyledSegment) o;
            return cssClasses.equals(that.cssClasses) &&
                text.equals(that.text);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(cssClasses, text);
        }
    }
}
