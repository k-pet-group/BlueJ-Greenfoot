package bluej.graph;

/**
 ** @version $Id: Edge.java 2045 2003-06-23 11:53:23Z fisker $
 ** @author Michael Cahill
 **
 ** General graph edge
 **/
public abstract class Edge extends GraphElement
{
	public Vertex from, to;

	public Edge(Vertex from, Vertex to)
	{
		this.from = from;
		this.to = to;
	}

}
