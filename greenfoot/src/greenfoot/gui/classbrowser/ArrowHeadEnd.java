package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;

import javax.swing.JComponent;

/**
 * Graphics for the head of an arrow. With a connector to the right.
 *  ^ | |__
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowHeadEnd.java 3124 2004-11-18 16:08:48Z polle $
 */
public class ArrowHeadEnd extends JComponent
{
    private int arrowWidth;
    private int arrowHeight;
    public Dimension minimumSize;

    public ArrowHeadEnd(int width, int height)
    {
        this.arrowWidth = width;
        this.arrowHeight = height;
        minimumSize = new Dimension(arrowWidth + 2, arrowHeight);
    }

    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        Polygon arrow = new Polygon();
        arrow.addPoint(size.width / 2, 0);
        arrow.addPoint((size.width / 2) - arrowWidth / 2, arrowHeight);
        arrow.addPoint((size.width / 2) + arrowWidth / 2, arrowHeight);

        g.drawLine(size.width / 2, 0 + arrowHeight, size.width / 2, size.height / 2);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
        g.fillPolygon(arrow);
    }

    public Dimension getMinimumSize()
    {
        return minimumSize;
    }

    public Dimension getPreferredSize()
    {
        return minimumSize;
    }
}