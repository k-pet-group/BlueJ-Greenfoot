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

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import threadchecker.OnThread;
import threadchecker.Tag;


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
    public void showSelection(PathElement[] rangeShape)
    {
        selectionShape.getElements().setAll(rangeShape);
        selectionShape.setVisible(true);
    }

    /**
     * Sets the text that is shown on this line.  Also hides any existing
     * selection shape for the line.
     * 
     * @param text The text to show.
     * @param size The font size to use.
     */
    public void setText(String text, double size)
    {
        hideSelection();
        getChildren().clear();
        getChildren().add(selectionShape);
        // Temporarily, for investigating syntax highlighting, we alternate colours of the words:
        final Paint[] colors = new Paint[] { Color.RED, Color.GREEN, Color.LIGHTGRAY, Color.NAVY };
        int nextColor = 0;
        for (String s : text.split("((?<= )|(?= ))"))
        {
            Text t = new Text(s);
            t.setFont(new Font(size));
            if (!s.isBlank())
            {
                t.setFill(colors[nextColor]);
                nextColor = (nextColor + 1) % colors.length;
            }
            getChildren().add(t);
        }
        
    }
}
