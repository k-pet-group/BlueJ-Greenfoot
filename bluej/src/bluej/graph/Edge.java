package bluej.graph;

import java.awt.Graphics2D;

/**
 ** @version $Id: Edge.java 427 2000-04-18 04:33:04Z ajp $
 ** @author Michael Cahill
 **
 ** General graph edge
 **/
public abstract class Edge
{
	public Vertex from, to;

	public Edge(Vertex from, Vertex to)
	{
		this.from = from;
		this.to = to;
	}

	public abstract void draw(Graphics2D g);
}
