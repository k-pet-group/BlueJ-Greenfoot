package bluej.graph;

/**
 * @author fisker
 * implemented by GraphElements that is selectable
 * @version 
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
