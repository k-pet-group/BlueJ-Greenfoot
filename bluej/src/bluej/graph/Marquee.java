/*
 * Created on Sep 17, 2003
 *
 */
package bluej.graph;

import java.awt.*;
import java.util.Iterator;

/**
 * @author fisker
 *  
 */
public class Marquee
{
    private Graph graph;
    private int drag_start_x, drag_start_y;
    private Rectangle oldRect;
    private GraphElementManager graphElementManger;

    /**
     * Create a Marquee
     * 
     * @param graph
     * @param graphEditor
     */
    public Marquee(Graph graph)
    {
        this.graph = graph;
        this.graphElementManger = new GraphElementManager();
    }

    /**
     * start the marquee at point x, y
     * 
     * @param x
     * @param y
     */
    public void start(int x, int y)
    {
        drag_start_x = x;
        drag_start_y = y;
        graphElementManger.clear();
    }

    /**
     * Place the marquee from its starting point to the coordinate (drag_x,
     * drag_y). The marquee must have been started before this method is called.
     * 
     * @param drag_x
     * @param drag_y
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
        oldRect = newRect;

        findSelectedVertices(x, y, w, h);
    }

    private void findSelectedVertices(int x, int y, int w, int h)
    {
        //clear the currently selected
        graphElementManger.clear();
        //find the intersecting vertices
        Vertex v;
        for (Iterator it = graph.getVertices(); it.hasNext();) {
            v = (Vertex) it.next();
            if (v.getRectangle().intersects(x, y, w, h)) {
                graphElementManger.add(v);
            }
        }
    }

    public void stop()
    {
        oldRect = null;
    }

    /**
     * Get the GraphElementManger
     */
    public GraphElementManager getGraphElementManger()
    {
        return graphElementManger;
    }

    public Rectangle getRectangle()
    {
        return oldRect;
    }
}