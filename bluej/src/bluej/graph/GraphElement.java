package bluej.graph;

import java.awt.event.MouseEvent;

/**
 * An element in a Graph
 * @author fisker
 * 
 */
public abstract class GraphElement {
    
    abstract public void remove();
    
    /**
     * Subtypes of Graph elements must override this if it want GraphEditor
     * to be able to locate them. Only classes that can not be selected and that
     * doesn't have a popupmenu can made due with the default behavior.
     * @return
     */
    abstract public boolean contains(int x, int y);
    

    public void mousePressed(MouseEvent evt, GraphEditor editor) {}
    public void mouseReleased(MouseEvent evt, GraphEditor editor) {}
    public void mouseDragged(MouseEvent evt, GraphEditor editor) {}
    public void mouseMoved(MouseEvent evt, GraphEditor editor) {}
    public void doubleClick(MouseEvent evt, GraphEditor editor) {}
    public void singleClick(MouseEvent evt, GraphEditor editor) {}
    public void popupMenu(int x, int y, GraphEditor editor) {}   
}
