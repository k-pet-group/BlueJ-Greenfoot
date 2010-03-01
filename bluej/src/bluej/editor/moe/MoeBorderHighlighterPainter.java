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

/**
 * Highligher for the border around the search items
 * 
 * @author Marion Zalk
 *
 */
public class MoeBorderHighlighterPainter extends MoeHighlighterPainter {

    Color borderColor=Color.BLACK;
    public MoeBorderHighlighterPainter(Color arg0) {
        super(arg0);
        borderColor=arg0;
    }
    
    /**
     * Overrides the default method in order to draw the border 
     */
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
            JTextComponent c, View view) {
        g.setColor(borderColor);
        if (offs0 == view.getStartOffset() &&
                offs1 == view.getEndOffset()) {
            // Contained in view, can just use bounds.
            Rectangle alloc;
            if (bounds instanceof Rectangle) {
                alloc = (Rectangle)bounds;
            }
            else {
                alloc = bounds.getBounds();
            }
            g.fillRect(alloc.x, alloc.y, alloc.width, alloc.height);
            return alloc;
        }
        else {
            // Should only render part of View.
            try {
                // --- determine locations ---
                Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                        offs1,Position.Bias.Backward,
                        bounds);
                Rectangle r = (shape instanceof Rectangle) ?
                        (Rectangle)shape : shape.getBounds();                                             
                        g.drawRect(r.x,r.y, r.width-1, r.height-1);
                        return r;
            } catch (BadLocationException e) {
                // can't render
            }
        }
        return null;
    }

}
