package bluej.graph;

/**
 * A superclass for all kinds of edges in th graph.
 * 
 * @version $Id: Edge.java 2788 2004-07-12 17:04:24Z mik $ *
 * @author Michael Cahill
 */
public abstract class Edge extends SelectableGraphElement
{
    public Vertex from, to;

    public Edge(Vertex from, Vertex to)
    {
        this.from = from;
        this.to = to;
    }

}