package bluej.graph;

/**
 * A strategy to move graph selections with keyboard input.
 * 
 * @author fisker
 */
public interface TraverseStragegy
{
    /**
     * Given a currently selected vertex and a key press, decide which vertex 
     * should be selected next.
     * 
     * @param graph  The graph we're looking at.
     * @param currentVertex  The currently selected vertex.
     * @param key  The key that was pressed.
     * @return     A vertex that should be selected now.
     */
    public Vertex findNextVertex(Graph graph, Vertex currentVertex, int key);
}