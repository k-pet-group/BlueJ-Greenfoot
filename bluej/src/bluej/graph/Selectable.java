package bluej.graph;

/**
 * @author fisker
 * Implemented by GraphElements that is selectable
 * @version $Id: Selectable.java 2474 2004-02-10 09:40:47Z fisker $
 */
public interface Selectable {
    //selection
    public void setSelected(boolean selected);
    public boolean isSelected();
    
    //resizing
    public boolean isHandle(int x, int y);
    public boolean isResizing();
    public void setResizing(boolean resizeing);
}
