package bluej.graph;

import java.awt.*;
import java.util.Iterator;
import java.awt.geom.*;

/**
 * General graph
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: Graph.java 2465 2004-01-29 13:33:46Z fisker $
 */
public abstract class Graph
{
    public final static String UML="uml";
    public final static String BLUE="blue";

    private static final int RIGHT_PLACEMENT_MIN = 300;
    private static final int SNAP_GRID_SIZE = 10;
    private static final int WHITESPACE_SIZE = 10;

    public abstract Iterator getVertices();
    public abstract Iterator getEdges();

    
    public void setActiveGraphElement(GraphElement ge)
    {
    }

    public Dimension getMinimumSize()
    {
        int minWidth = 1;
        int minHeight = 1;

        for(Iterator it = getVertices(); it.hasNext(); ) {
            Vertex v = (Vertex)it.next();

            if(v.getX() + v.getWidth() > minWidth)
        	minWidth = v.getX() + v.getWidth();
            if(v.getY() + v.getHeight() > minHeight)
        	minHeight = v.getY() + v.getHeight();
        }

        return new Dimension(minWidth, minHeight);
    }

    public void findSpaceForVertex(Vertex t)
    {
        Area a = new Area();

        for(Iterator it = getVertices(); it.hasNext(); ) {
            Vertex vertex = (Vertex)it.next();

            // lets discount the vertex we are adding from the space
            // calculations
            if (vertex != t) {
                Rectangle vr = new Rectangle(vertex.getX(), vertex.getY(),
                                                vertex.getWidth(), vertex.getHeight());
                a.add(new Area(vr));
            }
        }

        Dimension min = getMinimumSize();

        if (RIGHT_PLACEMENT_MIN > min.width)
            min.width = RIGHT_PLACEMENT_MIN;

        Rectangle targetRect = new Rectangle(t.getWidth() + WHITESPACE_SIZE*2,
                                                t.getHeight() + WHITESPACE_SIZE*2);

        for(int y=0; y<(2*min.height); y+=10) {
            for(int x=0; x<(min.width-t.getWidth()-2*WHITESPACE_SIZE); x+=10) {
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
