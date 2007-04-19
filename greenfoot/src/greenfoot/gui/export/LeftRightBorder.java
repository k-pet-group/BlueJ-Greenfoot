package greenfoot.gui.export;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.border.LineBorder;

/*
 * Custom Border class to draw just the left and right sides of a lin border.
 *
 * @author Michael Kolling
 * @version $Id: LeftRightBorder.java 4977 2007-04-19 17:13:25Z mik $
 */

public class LeftRightBorder extends LineBorder
{
    
    /**
     * Create a left-right border with width 1 and the given color.
     */
    public LeftRightBorder(Color col)
    {
        super(col);
    }

    /**
     * Paints the border only on the left and right sides.
     */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Color oldColor = g.getColor();

        g.setColor(getLineColor());
        g.drawLine(x, y, x, height-1);
        g.drawLine(width-1, y, width-1, height-1);
        g.setColor(oldColor);
    }
}
