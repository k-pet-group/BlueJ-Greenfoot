package bluej.graph;

import java.awt.Rectangle;

/**
 * Implemented by GraphElements that is selectable
 * @author fisker
 * @version $Id: Selectable.java 2787 2004-07-12 14:12:42Z mik $
 */
public interface Selectable 
{
    //selection
    void setSelected(boolean selected);
    boolean isSelected();
    Rectangle getBoundingBox();
    
    //resizing
    boolean isHandle(int x, int y);
    boolean isResizable();
}
