package bluej.graph;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Enumeration;
import java.util.Vector;

/**
 * General graph
 *
 * @author  Michael Cahill
 * @version $Id: Graph.java 505 2000-05-24 05:44:24Z ajp $
 */
public abstract class Graph
{
    protected GraphEditor editor;

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

/*    public void setEditor(GraphEditor editor)
    {
        this.editor = editor;
    }

    public GraphEditor getEditor()
    {
        return editor;
    } */

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
}
