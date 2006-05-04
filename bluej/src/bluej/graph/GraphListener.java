package bluej.graph;

/**
 * An interfacing for receiving events from a Graph.
 * 
 * @author davmac
 * @version $Id: GraphListener.java 4082 2006-05-04 13:37:44Z davmac $
 */
public interface GraphListener
{
    /**
     * A vertex was removed from the graph.
     */
    public void selectableElementRemoved(SelectableGraphElement element);
    
    /**
     * General notification that the graph has changed.
     */
    public void graphChanged();
}
