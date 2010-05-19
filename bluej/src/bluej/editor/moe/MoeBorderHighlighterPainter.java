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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

/**
 * Highlighter for the border and fill of the found search instances.<p>
 * 
 * This highlighter also paints in a different colour where it coincides with the
 * selection.
 * 
 * @author Marion Zalk
 */
public class MoeBorderHighlighterPainter extends DefaultHighlightPainter
{
    Color borderColor=Color.BLACK;
    Color innerColor1;
    Color innerColor2;
    Color selectionColor1;
    Color selectionColor2;
    
    public MoeBorderHighlighterPainter(Color bColor, Color fillColor1, Color fillColor2,
            Color selectionColor1, Color selectionColor2)
    {
        super(fillColor1);
        borderColor=bColor;
        innerColor1=fillColor1;
        innerColor2=fillColor2;
        this.selectionColor1 = selectionColor1;
        this.selectionColor2 = selectionColor2;
    }
    
    /**
     * Paint a gradient fill
     * @param g  The graphics object on which to draw
     * @param r  The region to be occupied by the fill
     * @param color1   The first color in the gradient
     * @param color2   The second color in the gradient
     */
    private void paintGradient(Graphics g, Rectangle r, Color color1, Color color2)
    {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D)g;

            // Paint a gradient from top to bottom:
            GradientPaint gp = new GradientPaint(
                r.x,r.y, color1,
                r.x+(r.width/2), r.y+r.height, color2);

            g2d.setPaint(gp);
            g2d.fillRect(r.x-1, r.y, r.width+1, r.height);
        }
    }
    
    /**
     * Overrides the default method in order to draw the border 
     */
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
            JTextComponent c, View view)
    {        
        // Should only render part of View.
        try {
            // --- determine locations ---
            Shape shape = view.modelToView(offs0, Position.Bias.Forward,
                    offs1,Position.Bias.Backward,
                    bounds);
            Rectangle r = shape.getBounds(); 

            paintGradient(g, r, innerColor1, innerColor2);

            g.setColor(borderColor);
            g.drawRoundRect(r.x-2,r.y, r.width+2, r.height-1, 6, 6);
            
            int selStart = c.getSelectionStart();
            int selEnd = c.getSelectionEnd();
            boolean overLaps = selStart != selEnd;
            overLaps &= (selStart < offs1 && selEnd >= offs0);
                   
            if (overLaps) {
                Shape origClip = g.getClip();
                Rectangle clip = (origClip != null) ? origClip.getBounds() : bounds.getBounds();
                if (selEnd < offs1) {
                    int clipR = view.modelToView(selEnd, bounds,
                            Position.Bias.Backward).getBounds().x;
                    clip.width = Math.min(clip.width, clipR - clip.x);
                }
                if (selStart > offs0) {
                    int clipL = view.modelToView(selStart, bounds,
                            Position.Bias.Forward).getBounds().x;
                    int diff = clipL - clip.x;
                    if (diff > 0) {
                        clip.x = clipL;
                        clip.width -= diff;
                    }
                }

                g.setClip(clip);
                paintGradient(g, r, selectionColor1, selectionColor2);
                g.setClip(origClip);
            }
            
            r.x -= 2;
            r.width += 3;
            return r;
        } catch (BadLocationException e) {
            // can't render
        }
        return null;
    }

}
