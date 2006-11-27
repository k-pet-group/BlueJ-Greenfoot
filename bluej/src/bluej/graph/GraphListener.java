package bluej.graph;

/**
 * An interfacing for receiving events from a Graph.
 * 
 * @author davmac
 * @version $Id: GraphListener.java 4708 2006-11-27 00:47:57Z bquig $
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
