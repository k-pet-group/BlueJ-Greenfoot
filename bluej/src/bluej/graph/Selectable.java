package bluej.graph;

import java.awt.Rectangle;

/**
 * Implemented by GraphElements that is selectable
 * @author fisker
 * @version $Id: Selectable.java 2755 2004-07-07 15:52:12Z mik $
 */
public interface Selectable {
    //selection
    void setSelected(boolean selected);
    boolean isSelected();
    Rectangle getBoundingBox();
    
    //resizing
    boolean isHandle(int x, int y);
    boolean isResizing();
    void setResizing(boolean resizeing);
    boolean isResizable();
}
