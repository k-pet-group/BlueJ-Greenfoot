// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import java.awt.*;

import javax.swing.text.*;

import bluej.utility.Debug;

/**
 * Specialised highlight painter for painting matching bracket in text component as
 * a rectangle.
 */
public class BracketMatchPainter extends DefaultHighlighter.DefaultHighlightPainter
{
     
    public BracketMatchPainter(Color colour)
    {
        super(colour);
    }
        
    /**
     * Paints a rectangle around a matching bracket
     * This seems to be the only method we need to over-ride.
     *
     * @return area highlighted 
     */
    public Shape paintLayer(Graphics g, int begin, int end, Shape bounds, JTextComponent comp, View view) 
    {
        g.setColor(getColor());
        Rectangle rect = null;
        try {
            Shape shape = view.modelToView(begin, Position.Bias.Forward,
                                           end,Position.Bias.Backward,
                                           bounds);
            rect = shape.getBounds();                
            g.drawRect(rect.x, rect.y, rect.width -1, rect.height - 1);
        } catch (BadLocationException ble) {
            Debug.reportError("bad location exception thrown");
            ble.printStackTrace();
        }    
        return rect;
    }
    
}