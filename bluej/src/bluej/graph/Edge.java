package bluej.graph;

import java.awt.Graphics;

/**
 ** @version $Id: Edge.java 36 1999-04-27 04:04:54Z mik $
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

	public abstract void draw(Graphics g);
}
