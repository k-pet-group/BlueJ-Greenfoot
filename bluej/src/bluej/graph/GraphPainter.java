package bluej.graph;

import java.awt.Graphics2D;

/**
 * Interface for GraphPainters
 * @author fisker
 * @version $Id: GraphPainter.java 2789 2004-07-12 18:08:11Z mik $
 */
public interface GraphPainter
{
    /**
     * Paint the given graph editor on screen.
     * @param g  The graphics contect to paint on.
     * @param graphEditor  The editor to be painted.
     */
    void paint(Graphics2D g, GraphEditor graphEditor);
}