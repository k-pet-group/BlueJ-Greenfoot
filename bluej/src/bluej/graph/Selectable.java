package bluej.graph;


/**
 * Implemented by GraphElements that is selectable
 * @author fisker
 * @version $Id: Selectable.java 2991 2004-09-06 08:29:16Z polle $
 */
public interface Selectable 
{
    //selection
    void setSelected(boolean selected);
    boolean isSelected();
    
    //resizing
    boolean isHandle(int x, int y);
    boolean isResizable();
}
