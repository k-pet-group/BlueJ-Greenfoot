package bluej.graph;

import java.util.Vector;
import java.util.Enumeration;
import java.awt.Graphics;

/**
 ** @version $Id: TestGraph.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Test graph
 **/

public class TestGraph extends Graph
{
	Vector vertices;
	Vector edges;

	public TestGraph()
	{
		vertices = new Vector();
		edges = new Vector();
	}

	public void addVertex(Vertex v)
	{
		vertices.addElement(v);
	}

	public void removeVertex(Vertex v)
	{
		vertices.removeElement(v);
	}

	public void addEdge(Edge e)
	{
		edges.addElement(e);
	}

	public void removeEdge(Edge e)
	{
		edges.removeElement(e);
	}

	public Enumeration getVertices()
	{
		return vertices.elements();
	}

	public Enumeration getEdges()
	{
		return edges.elements();
	}
}
