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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import java.util.ArrayList;

/**
 * A FlowEditorPane is a component with (optional) horizontal and vertical scroll bars.
 * 
 * It displays only the lines that are currently visible on screen, in what is known
 * as a virtualised container.  Scrolling re-renders the currently visible line set.
 */
public class FlowEditorPane extends Region
{
    private final ArrayList<TextFlow> currentlyVisibleLines = new ArrayList<>();
    private int firstLineIndex = 0;
    
    private final Document document;
    
    private final TrackedPosition caret;
    private final Path caretShape;
    
    public FlowEditorPane(String content)
    {
        document = new HoleDocument();
        document.replaceText(0, 0, content);
        caret = document.trackPosition(0, Bias.FORWARD);
        caretShape = new Path();
        caretShape.setStroke(Color.RED);
        updateRender();

        Nodes.addInputMap(this, InputMap.sequence(
            InputMap.consume(KeyEvent.KEY_PRESSED, this::keyPressed),
            InputMap.consume(MouseEvent.MOUSE_CLICKED, this::mouseClicked)
        ));
    }

    private void mouseClicked(MouseEvent e)
    {
        requestFocus();
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
                    updateRender();
                    break;
                }
            }
        }
    }

    private void keyPressed(KeyEvent e)
    {
        switch (e.getCode())
        {
            case LEFT:
                caret.moveBy(-1);
                updateRender();
                break;
            case RIGHT:
                caret.moveBy(1);
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
                currentlyVisibleLines.add(new TextFlow());
            currentlyVisibleLines.get(i).getChildren().setAll(new Text(lines[i]));
        }

        getChildren().setAll(currentlyVisibleLines);
        getChildren().add(caretShape);

        TextFlow caretLine = currentlyVisibleLines.get(caret.getLine());
        caretShape.getElements().setAll(caretLine.caretShape(caret.getColumn(), true));
        caretShape.setLayoutX(caretLine.getLayoutX());
        caretShape.setLayoutY(caretLine.getLayoutY());
                
        requestLayout();
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
}
