package bluej.graph;

import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;
import java.awt.geom.*;

/**
 * General graph
 *
 * @author  Michael Cahill
 * @version $Id: Graph.java 650 2000-07-26 00:29:43Z ajp $
 */
public abstract class Graph
{
    public final static String UML="uml";
    public final static String BLUE="blue";

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

            Rectangle vr = new Rectangle(vertex.x, vertex.y,
                                            vertex.width, vertex.height);

            a.add(new Area(vr));
        }

        Dimension min = getMinimumSize();
        Rectangle targetRect = new Rectangle(t.width + 20, t.height + 20);

        for(int y=0; true; y+=20) {
            for(int x=0; x<(min.width-t.width-20); x+=20) {
                targetRect.setLocation(x,y);
                if (!a.intersects(targetRect)) {
                    t.setPos(x+10,y+10);
                    return;
                }
            }
        }
    }
}
