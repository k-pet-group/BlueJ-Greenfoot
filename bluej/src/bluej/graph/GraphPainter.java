package bluej.graph;

import java.awt.Graphics2D;

/**
 * Interface for GraphPainters
 * @author fisker
 * @version $Id: GraphPainter.java 2590 2004-06-11 11:29:14Z fisker $
 */
public interface GraphPainter
{
    void paint(Graphics2D g, GraphEditor graphEditor);
}