package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.graph.Marquee;

/**
 * Paints a marquee
 * @author fisker
 * @version $Id: MarqueePainter.java 2475 2004-02-10 09:53:59Z fisker $
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