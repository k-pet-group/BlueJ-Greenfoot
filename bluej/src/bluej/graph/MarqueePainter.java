package bluej.graph;

import java.awt.*;
import java.awt.Graphics2D;

/**
 * Paints a marquee
 * 
 * @author fisker
 * @version $Id: MarqueePainter.java 2787 2004-07-12 14:12:42Z mik $
 */
public final class MarqueePainter
{
    private static final Color tc = new Color(100, 100, 100, 20);
    Rectangle marqRect;

    public void paint(Graphics2D g, Marquee marquee)
    {
        marqRect = marquee.getRectangle();
        if (marqRect != null) {
            g.setColor(Color.black);
            g.draw(marqRect);
            g.setColor(tc);
            g.fill(marqRect);
        }
    }
}