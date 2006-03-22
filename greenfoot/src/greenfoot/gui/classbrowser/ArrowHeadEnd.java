package greenfoot.gui.classbrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;

/**
 * Graphics for the head of an arrow. With a connector to the right.
 *  ^ | |__
 * 
 * @author Poul Henriksen
 * @version $Id: ArrowHeadEnd.java 3857 2006-03-22 00:08:17Z mik $
 */
public class ArrowHeadEnd extends ArrowElement
{
    public void paintComponent(Graphics g)
    {
        Dimension size = getSize();
        Polygon arrow = new Polygon();
        arrow.addPoint(size.width / 2, 0);
        arrow.addPoint((size.width / 2) - ARROW_WIDTH / 2, ARROW_HEIGHT);
        arrow.addPoint((size.width / 2) + ARROW_WIDTH / 2, ARROW_HEIGHT);

        g.drawLine(size.width / 2, 0 + ARROW_HEIGHT, size.width / 2, size.height / 2);
        g.drawLine(size.width / 2, size.height / 2, size.width, size.height / 2);
        g.fillPolygon(arrow);
    }
}