package bluej.graph;

import java.awt.*;
import java.util.Iterator;

/**
 * The diagram's marquee (a rectangular drag area for selecting graph elements).
 * 
 * @author fisker
 */
public final class Marquee
{
    private Graph graph;
    private int drag_start_x, drag_start_y;
    private Rectangle currentRect;
    private GraphElementSet elements;  // the graph elements currently in the marguee

    /**
     * Create a marquee for a given graph.
     */
    public Marquee(Graph graph)
    {
        this.graph = graph;
        elements = new GraphElementSet();
    }

    /**
     * Start a marquee selection at point x, y.
     */
    public void start(int x, int y)
    {
        drag_start_x = x;
        drag_start_y = y;
        elements.clear();
    }

    /**
     * Place the marquee from its starting point to the coordinate (drag_x,
     * drag_y). The marquee must have been started before this method is called.
     * 
     * @param drag_x  The x coordinate of the current drag position 
     * @param drag_y  The y coordinate of the current drag position 
     */
    public void move(int drag_x, int drag_y)
    {
        int x = drag_start_x;
        int y = drag_start_y;
        int w = drag_x - drag_start_x;
        int h = drag_y - drag_start_y;
        //Rectangle can't handle negative numbers, modify coordinates
        if (w < 0)
            x = x + w;
        if (h < 0)
            y = y + h;
        w = Math.abs(w);
        h = Math.abs(h);
        Rectangle newRect = new Rectangle(x, y, w, h);
        currentRect = newRect;

        findSelectedVertices(x, y, w, h);
    }

    
    /**
     * Find, and add, all vertices that intersect the specified area.
     */
    private void findSelectedVertices(int x, int y, int w, int h)
    {
        //clear the currently selected
        elements.clear();

        //find the intersecting vertices
        for (Iterator it = graph.getVertices(); it.hasNext();) {
            Vertex v = (Vertex) it.next();
            if (v.getRectangle().intersects(x, y, w, h)) {
                elements.add(v);
            }
        }
    }

    /**
     * Stop a current marquee selection.
     */
    public void stop()
    {
        currentRect = null;
    }

    /**
     * Get the elements selected by the marquee
     */
    public GraphElementSet getElements()
    {
        return elements;
    }

    public Rectangle getRectangle()
    {
        return currentRect;
    }
}