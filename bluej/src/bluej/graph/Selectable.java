package bluej.graph;

/**
 * Implemented by GraphElements that is selectable
 * @author fisker
 * @version $Id: Selectable.java 2488 2004-04-06 09:42:07Z fisker $
 */
public interface Selectable {
    //selection
    public void setSelected(boolean selected);
    public boolean isSelected();
    
    //resizing
    public boolean isHandle(int x, int y);
    public boolean isResizing();
    public void setResizing(boolean resizeing);
    public boolean isResizable();
}
