package bluej.graph;

/**
 ** @version $Id: Edge.java 2775 2004-07-09 15:07:12Z mik $
 ** @author Michael Cahill
 **
 ** General graph edge
 **/
public abstract class Edge extends SelectableGraphElement
{
	public Vertex from, to;

	public Edge(Vertex from, Vertex to)
	{
		this.from = from;
		this.to = to;
	}

}
