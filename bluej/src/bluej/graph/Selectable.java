package bluej.graph;

/**
 * Implemented by GraphElements that is selectable
 * @author fisker
 * @version $Id: Selectable.java 2475 2004-02-10 09:53:59Z fisker $
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
