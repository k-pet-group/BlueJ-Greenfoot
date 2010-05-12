/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.moe;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

/**
 * Highligher for the border and fill of the select and search items
 * 
 * @author Marion Zalk
 *
 */
public class MoeBorderHighlighterPainter extends DefaultHighlightPainter {

    Color borderColor=Color.BLACK;
    Color innerColor;
    public MoeBorderHighlighterPainter(Color bColor, Color fillColor) {
        super(fillColor);
        borderColor=bColor;
        innerColor=fillColor;
    }
    
    /**
     * Overrides the default method in order to draw the border 
     */
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
            JTextComponent c, View view) {
        if (offs0 != view.getStartOffset() &&
                offs1 != view.getEndOffset()) {
            // Should only render part of View.
            try {
                // --- determine locations ---
                Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                        offs1,Position.Bias.Backward,
                        bounds);
                Rectangle r = (shape instanceof Rectangle) ?
                        (Rectangle)shape : shape.getBounds(); 
                        //fill in the rectangle and then draw the border
                        g.setColor(innerColor);
                        g.fillRect(r.x,r.y, r.width, r.height);
                        g.setColor(borderColor);
                        g.drawRect(r.x,r.y, r.width-1, r.height-1);
                        return r;
            } catch (BadLocationException e) {
                // can't render
            }
        }
        return null;
    }

}
