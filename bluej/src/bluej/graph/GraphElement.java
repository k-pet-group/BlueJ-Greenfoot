package bluej.graph;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

/**
 * An element in a Graph
 * @author fisker
 * 
 */
public abstract class GraphElement {
    
    //abstract public void remove();
    
    /**
     * Subtypes of Graph elements must override this if it want GraphEditor
     * to be able to locate them. Only classes that can not be selected and that
     * doesn't have a popupmenu can made due with the default behavior.
     * @return
     */
    public boolean contains(int x, int y){
        return false;
    }
    
    public abstract void draw(Graphics2D g);

    public void mousePressed(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseReleased(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseDragged(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void mouseMoved(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void doubleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}
    public void singleClick(MouseEvent evt, int x, int y, GraphEditor editor) {}

    public void popupMenu(int x, int y, GraphEditor editor) {}
    
}
