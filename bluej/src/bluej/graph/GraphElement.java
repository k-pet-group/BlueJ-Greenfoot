package bluej.graph;

import java.awt.event.MouseEvent;

/**
 * An element in a Graph
 * @author fisker
 * 
 */
public abstract class GraphElement {
    
    /**
     * Remove this element from the graph.
     */
    abstract public void remove();
    
    /**
     * Subtypes of Graph elements must override this if it want GraphEditor
     * to be able to locate them. Only classes that can not be selected and that
     * doesn't have a popupmenu can made due with the default behavior.
     * @return
     */
    abstract public boolean contains(int x, int y);
    
    /**
     * A double click was done on this element.
     */
    public void doubleClick(MouseEvent evt) {}

    /**
     * Post the context menu for this target.
     */
    abstract public void popupMenu(int x, int y);
}
