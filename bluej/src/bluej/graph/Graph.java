package bluej.graph;

import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import java.awt.geom.*;

/**
 * General graph
 *
 * @author  Michael Cahill
 * @version $Id: Graph.java 653 2000-07-26 01:46:35Z ajp $
 */
public abstract class Graph
{
    public final static String UML="uml";
    public final static String BLUE="blue";

    private static final int RIGHT_PLACEMENT_MIN = 300;
    private static final int SNAP_GRID_SIZE = 10;
    private static final int WHITESPACE_SIZE = 10;

    public abstract Enumeration getVertices();
    public abstract Enumeration getEdges();

    public void draw(Graphics g)
    {
        for(Enumeration e = getEdges(); e.hasMoreElements(); ) {
            Edge edge = (Edge)e.nextElement();
            edge.draw((Graphics2D) g.create());
        }

        for(Enumeration e = getVertices(); e.hasMoreElements(); ) {
            Vertex vertex = (Vertex)e.nextElement();
            vertex.drawUntranslated((Graphics2D) g.create());
        }
    }

    public void setActiveVertex(Vertex v)
    {
    }

    public Dimension getMinimumSize()
    {
        int minWidth = 1;
        int minHeight = 1;

        for(Enumeration e = getVertices(); e.hasMoreElements(); ) {
            Vertex v = (Vertex)e.nextElement();

            if(v.x + v.width > minWidth)
        	minWidth = v.x + v.width;
            if(v.y + v.height > minHeight)
        	minHeight = v.y + v.height;
        }

        return new Dimension(minWidth, minHeight);
    }

    public void findSpaceForVertex(Vertex t)
    {
        Area a = new Area();

        for(Enumeration e = getVertices(); e.hasMoreElements(); ) {
            Vertex vertex = (Vertex)e.nextElement();

            // lets discount the vertex we are adding from the space
            // calculations
            if (vertex != t) {
                Rectangle vr = new Rectangle(vertex.x, vertex.y,
                                                vertex.width, vertex.height);
                a.add(new Area(vr));
            }
        }

        Dimension min = getMinimumSize();

        if (RIGHT_PLACEMENT_MIN > min.width)
            min.width = RIGHT_PLACEMENT_MIN;

        Rectangle targetRect = new Rectangle(t.width + WHITESPACE_SIZE*2,
                                                t.height + WHITESPACE_SIZE*2);

        for(int y=0; y<(2*min.height); y+=10) {
            for(int x=0; x<(min.width-t.width-2*WHITESPACE_SIZE); x+=10) {
                targetRect.setLocation(x,y);
                if (!a.intersects(targetRect)) {
                    t.setPos(x+10,y+10);
                    return;
                }
            }
        }

        t.setPos(10,min.height+10);
    }
}
