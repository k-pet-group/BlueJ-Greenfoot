/*
 * Created on May 12, 2004
 *
 */
package bluej.graph;

/**
 * @author fisker
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface TraverseStragegy
{
    public Vertex findNextVertex(Graph graph, Vertex currentVertex, int key);
}