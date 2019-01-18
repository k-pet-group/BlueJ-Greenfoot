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

import bluej.editor.flow.Document.Bias;
import bluej.utility.Utility;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A FlowEditorPane is a component with (optional) horizontal and vertical scroll bars.
 * 
 * It displays only the lines that are currently visible on screen, in what is known
 * as a virtualised container.  Scrolling re-renders the currently visible line set.
 */
public class FlowEditorPane extends Region
{
    private static final Image UNDERLINE_IMAGE = new Image(
            // Temporary hack hard coding the path (since this isn't running under BlueJ proper, yet)
            "file:///Users/neil/intellij/bjgf/bluej/lib/images/" + 
            "error-underline.png");
    
    private final ArrayList<TextLine> currentlyVisibleLines = new ArrayList<>();
    private int firstLineIndex = 0;
    
    private final Document document;
    
    private final TrackedPosition anchor;
    private final TrackedPosition caret;
    private final Path caretShape;
    
    private final List<IndexRange> errorUnderlines = new ArrayList<>();
    
    public FlowEditorPane(String content)
    {
        document = new HoleDocument();
        document.replaceText(0, 0, content);
        caret = document.trackPosition(0, Bias.FORWARD);
        // Important that the anchor is a different object to the caret, as they will move independently:
        anchor = document.trackPosition(0, Bias.FORWARD);
        caretShape = new Path();
        caretShape.setStroke(Color.RED);
        caretShape.setMouseTransparent(true);
        updateRender();

        Nodes.addInputMap(this, InputMap.sequence(
            InputMap.consume(KeyEvent.KEY_PRESSED, this::keyPressed),
            InputMap.consume(KeyEvent.KEY_TYPED, this::keyTyped),
            InputMap.consume(MouseEvent.MOUSE_PRESSED, this::mousePressed),
            InputMap.consume(MouseEvent.MOUSE_DRAGGED, this::mouseDragged)
        ));
    }
    
    private void keyTyped(KeyEvent e)
    {
        if (e.getCharacter().isEmpty())
        {
            return;
        }
        
        int start = Math.min(caret.position, anchor.position);
        int end = Math.max(caret.position, anchor.position);
        
        document.replaceText(start, end, e.getCharacter());
        anchor.position = caret.position;
        updateRender();
    }

    private void mousePressed(MouseEvent e)
    {
        requestFocus();
        positionCaretAtDestination(e);
        anchor.position = caret.position;
        updateRender();
    }

    private void positionCaretAtDestination(MouseEvent e)
    {
        for (int i = 0; i < currentlyVisibleLines.size(); i++)
        {
            TextFlow currentlyVisibleLine = currentlyVisibleLines.get(i);
            // getLayoutBounds() seems to get out of date, so calculate manually:
            BoundingBox actualBounds = new BoundingBox(currentlyVisibleLine.getLayoutX(), currentlyVisibleLine.getLayoutY(), currentlyVisibleLine.getWidth(), currentlyVisibleLine.getHeight());
            if (actualBounds.contains(e.getX(), e.getY()))
            {
                // Can't use parentToLocal if layout bounds may be out of date:
                HitInfo hitInfo = currentlyVisibleLine.hitTest(new Point2D(e.getX() - currentlyVisibleLine.getLayoutX(), e.getY() - currentlyVisibleLine.getLayoutY()));
                if (hitInfo != null)
                {
                    caret.moveToLineColumn(i, hitInfo.getInsertionIndex());
                    break;
                }
            }
        }
    }

    private void mouseDragged(MouseEvent e)
    {
        positionCaretAtDestination(e);
        // Don't update the anchor, though
        updateRender();
    }

    private void keyPressed(KeyEvent e)
    {
        switch (e.getCode())
        {
            case LEFT:
                caret.moveBy(-1);
                anchor.position = caret.position;
                updateRender();
                break;
            case RIGHT:
                caret.moveBy(1);
                anchor.position = caret.position;
                updateRender();
                break;
            case F1:
                // Temporarily, add error underline:
                if (caret.position != anchor.position)
                {
                    errorUnderlines.add(new IndexRange(Math.min(caret.position, anchor.position), Math.max(caret.position, anchor.position)));
                    updateRender();
                }
                break;
            case F2:
                // Temporarily, remove error underlines:
                errorUnderlines.clear();
                updateRender();
                break;
        }
    }
    
    private void updateRender()
    {
        // For now, just render all lines:
        String[] lines = Utility.splitLines(document.getFullContent());
        for (int i = 0; i < lines.length; i++)
        {
            if (currentlyVisibleLines.size() - 1 < i)
            {
                currentlyVisibleLines.add(new TextLine());
            }
            currentlyVisibleLines.get(i).setText(lines[i]);
            currentlyVisibleLines.get(i).selectionShape.getElements().clear();
        }

        getChildren().clear();
        getChildren().addAll(currentlyVisibleLines);
        getChildren().add(caretShape);
        
        TextLine caretLine = currentlyVisibleLines.get(caret.getLine());
        caretShape.getElements().setAll(caretLine.caretShape(caret.getColumn(), true));
        caretShape.setLayoutX(caretLine.getLayoutX());
        caretShape.setLayoutY(caretLine.getLayoutY());
        
        if (caret.position != anchor.position)
        {
            TrackedPosition startPos = caret.position < anchor.position ? caret : anchor;
            TrackedPosition endPos = caret.position < anchor.position ? anchor : caret;
            
            // Simple case; one line selection:
            if (startPos.getLine() == endPos.getLine())
            {
                caretLine.selectionShape.getElements().setAll(caretLine.rangeShape(startPos.getColumn(), endPos.getColumn()));
            }
            else
            {
                // Need composite of several lines
                // Do all except last line:
                for (int line = startPos.getLine(); line < endPos.getLine(); line++)
                {
                    int startOnThisLine = line == startPos.getLine() ? startPos.getColumn() : 0;
                    PathElement[] elements = currentlyVisibleLines.get(line).rangeShape(startOnThisLine, document.getLineStart(line + 1) - document.getLineStart(line));
                    currentlyVisibleLines.get(line).selectionShape.getElements().setAll(elements);
                }
                // Now do last line:
                currentlyVisibleLines.get(endPos.getLine()).selectionShape.getElements().setAll(currentlyVisibleLines.get(endPos.getLine()).rangeShape(0, endPos.getColumn()));
            }
        }

        for (IndexRange errorUnderline : errorUnderlines)
        {
            Path err = new Path();
            err.setFill(null);
            err.setStroke(new ImagePattern(UNDERLINE_IMAGE, 0, 0, 2, 2, false));
            err.setMouseTransparent(true);
            TextLine errTextLine = currentlyVisibleLines.get(document.getLineFromPosition(errorUnderline.getStart()));
            err.getElements().setAll(keepBottom(errTextLine.rangeShape(document.getColumnFromPosition(errorUnderline.getStart()), document.getColumnFromPosition(errorUnderline.getEnd()))));
            err.setLayoutX(errTextLine.getLayoutX());
            err.setLayoutY(errTextLine.getLayoutY());
            getChildren().add(err);
        }
                
        requestLayout();
    }

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

    @Override
    protected void layoutChildren()
    {
        double y = 0;
        for (Node child : getChildren())
        {
            if (child instanceof TextFlow)
            {
                child.resizeRelocate(0, y, getWidth(), 20);
                y += 20;
            }
        }
    }
    
    private class TextLine extends TextFlow
    {
        private final Path selectionShape = new Path();
        
        public TextLine()
        {
            setMouseTransparent(true);
            selectionShape.setStroke(null);
            selectionShape.setFill(Color.CORNFLOWERBLUE);
            selectionShape.setManaged(false);
            getChildren().add(selectionShape);
        }
        
        public void setText(String text)
        {
            getChildren().clear();
            getChildren().add(selectionShape);
            // Temporarily, for investigating syntax highlighting, we alternate colours of the words:
            final Paint[] colors = new Paint[] { Color.RED, Color.GREEN, Color.LIGHTGRAY, Color.NAVY };
            int nextColor = 0;
            for (String s : text.split("((?<= )|(?= ))"))
            {
                Text t = new Text(s);
                if (!s.isBlank())
                {
                    t.setFill(colors[nextColor]);
                    nextColor = (nextColor + 1) % colors.length;
                }
                getChildren().add(t);
            }
            
        }
    }
}
