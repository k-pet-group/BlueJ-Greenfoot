package bluej.graph;

import bluej.utility.Utility;
import java.awt.Frame;

/**
 ** @version $Id: Main.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Test for the bluej.graph package
 **/

public class Main
{
	static int numVertices = 10;
	static int numEdges = 5;

	public static void main(String[] args)
	{
		int i;
		TestGraph g = new TestGraph();
		
		TestVertex vertices[] = new TestVertex[numVertices];

		for(i = 0; i < numVertices; i++)
		{
			vertices[i] = new TestVertex();

			g.addVertex(vertices[i]);
		}

		for(i = 0; i < numEdges; i++)
		{
			TestVertex from = vertices[Utility.getRandom(0, numVertices)];
			TestVertex to = vertices[Utility.getRandom(0, numVertices)];

			g.addEdge(new TestEdge(from, to));
		}

		Frame f = new Frame();
		GraphEditor editor = new GraphEditor(g, null);
		f.add("Center", editor);
		f.pack();
		f.setVisible(true);
	}
}
