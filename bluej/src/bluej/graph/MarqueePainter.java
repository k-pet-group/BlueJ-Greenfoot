package bluej.graph;

import java.awt.*;
import java.awt.Graphics2D;


/**
 * Paints a marquee
 * @author fisker
 * @version $Id: MarqueePainter.java 2477 2004-02-11 11:29:19Z fisker $
 */
public class MarqueePainter
{
    private static final Color tc = new Color(100,100,100,20);
    Rectangle oldRect;
    public void paint(Graphics2D g, Marquee marquee){
        oldRect = marquee.getRectangle();
        if(oldRect != null){
            g.setColor(Color.black);
            g.draw(oldRect);
            g.setColor(tc);
            g.fill(oldRect);
        }
    }
}