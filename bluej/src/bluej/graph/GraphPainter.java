package bluej.graph;

import java.awt.Graphics2D;

/**
 * Interface for GraphPainters
 * @author fisker
 * @version $Id: GraphPainter.java 2475 2004-02-10 09:53:59Z fisker $
 */
public interface GraphPainter
{
    void paint(Graphics2D g, Graph graph);
}