package bluej.graph;

import java.awt.Graphics;

/**
 ** @version $Id: TestEdge.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Test graph edge
 **/

public class TestEdge extends Edge
{
	public TestEdge(Vertex from, Vertex to)
	{
		super(from, to);
	}

	public void draw(Graphics g)
	{
		g.drawLine(from.x, from.y, to.x, to.y);
	}
}
