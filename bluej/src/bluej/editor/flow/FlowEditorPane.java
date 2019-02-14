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
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.HitInfo;
import javafx.scene.text.TextFlow;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A FlowEditorPane is a component with (optional) horizontal and vertical scroll bars.
 * 
 * It displays only the lines that are currently visible on screen, in what is known
 * as a virtualised container.  Scrolling re-renders the currently visible line set.
 */
@OnThread(value = Tag.FXPlatform, ignoreParent = true)
public class FlowEditorPane extends Region implements DocumentListener
{
    private static final Image UNDERLINE_IMAGE = new Image(
            // Temporary hack hard coding the path (since this isn't running under BlueJ proper, yet)
            "file:///Users/neil/intellij/bjgf/bluej/lib/images/" + 
            "error-underline.png");
    private final LineDisplay lineDisplay = new LineDisplay();

    double fontSize = 12;
    
    private final Document document;
    
    private final TrackedPosition anchor;
    private final TrackedPosition caret;
    private final Path caretShape;
    
    private final List<IndexRange> errorUnderlines = new ArrayList<>();
    
    public FlowEditorPane(String content)
    {
        document = new HoleDocument();
        document.replaceText(0, 0, content);
        document.addListener(this);
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
        
        document.replaceText(start, end, e.getCharacter().equals("\r") ? "\n" : e.getCharacter());
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
        /*
        for (int i = 0; i < lineDisplay.currentlyVisibleLines.size(); i++)
        {
            TextFlow currentlyVisibleLine = lineDisplay.currentlyVisibleLines.get(i);
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
        */
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
            case F3:
                fontSize += 2;
                updateRender();
                break;
            case F4:
                fontSize -= 2;
                updateRender();
                break;
        }
    }

    @Override
    public void documentChanged()
    {
        updateRender();
    }

    private void updateRender()
    {
        List<Node> prospectiveChildren = new ArrayList<>();
        prospectiveChildren.addAll(lineDisplay.recalculateVisibleLines(document.getLines(), getHeight(), fontSize));
        prospectiveChildren.add(caretShape);
        
        // This will often avoid changing the children, if the window has not been resized:
        boolean needToChangeLinesAndCaret = false;
        for (int i = 0; i < prospectiveChildren.size(); i++)
        {
            // Reference equality is fine here:
            if (i >= getChildren().size() || prospectiveChildren.get(i) != getChildren().get(i))
            {
                needToChangeLinesAndCaret = true;
                break;
            }
        }
        if (needToChangeLinesAndCaret)
        {
            getChildren().setAll(prospectiveChildren);
        }
        else
        {
            // Clear rest after:
            if (getChildren().size() > prospectiveChildren.size())
            {
                getChildren().subList(prospectiveChildren.size(), getChildren().size()).clear();
            }
        }

        if (lineDisplay.isLineVisible(caret.getLine()))
        {
            TextLine caretLine = lineDisplay.getVisibleLine(caret.getLine());
            caretShape.getElements().setAll(caretLine.caretShape(caret.getColumn(), true));
            caretShape.setLayoutX(caretLine.getLayoutX());
            caretShape.setLayoutY(caretLine.getLayoutY());
            caretShape.setVisible(true);
        }
        else
        {
            caretShape.getElements().clear();
            caretShape.setLayoutX(0);
            caretShape.setLayoutY(0);
            caretShape.setVisible(false);
        }
        
        if (caret.position != anchor.position)
        {
            TrackedPosition startPos = caret.position < anchor.position ? caret : anchor;
            TrackedPosition endPos = caret.position < anchor.position ? anchor : caret;
            
            // Simple case; one line selection:
            if (startPos.getLine() == endPos.getLine() && lineDisplay.isLineVisible(startPos.getLine()))
            {
                TextLine caretLine = lineDisplay.getVisibleLine(startPos.getLine());
                caretLine.showSelection(caretLine.rangeShape(startPos.getColumn(), endPos.getColumn()));
            }
            else
            {
                // Need composite of several lines
                // Do all except last line:
                for (int line = startPos.getLine(); line < endPos.getLine(); line++)
                {
                    int startOnThisLine = line == startPos.getLine() ? startPos.getColumn() : 0;
                    TextLine textLine = lineDisplay.getVisibleLine(line);
                    PathElement[] elements = textLine.rangeShape(startOnThisLine, document.getLineStart(line + 1) - document.getLineStart(line));
                    textLine.showSelection(elements);
                }
                // Now do last line:
                lineDisplay.getVisibleLine(endPos.getLine()).showSelection(lineDisplay.getVisibleLine(endPos.getLine()).rangeShape(0, endPos.getColumn()));
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
        
                
        requestLayout();
    }

    private TextLine getVisibleLine(int line)
    {

        return lineDisplay.getVisibleLine(line);
    }

    private boolean isLineVisible(int line)
    {
        return lineDisplay.isLineVisible(line);
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

    @Override
    protected void layoutChildren()
    {
        double y = 0;
        for (Node child : getChildren())
        {
            if (child instanceof TextFlow)
            {
                double height = child.prefHeight(-1.0);
                child.resizeRelocate(0, y, child.prefWidth(height), height);
                y += height;
            }
        }
    }

    public Document getDocument()
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
        updateRender();
    }
}
