package bluej.graph;

/**
 * @author fisker
 *
  */
public interface Moveable
{
    /**
     * @return Returns the ghostX.
     */
    public int getGhostX();
    
    /**
     * @return Returns the ghostX.
     */
    public int getGhostY();
    
    /**
     * Set the position of the ghost image given a delta to the real size.
     */
    public void setGhostPosition(int deltaX, int deltaY);

    /**
     * Set the size of the ghost image.
     */
    public void setGhostSize(int ghostWidth, int ghostHeight);

    /**
     * Set the target's position to its ghost position.
     */
    public void setPositionToGhost();
    
    /** 
     * Ask whether we are currently dragging. 
     */
    public boolean isDragging();
    
    /**
     * Set whether or not we are currently dragging this class
     * (either moving or resizing).
     */
    public void setDragging(boolean isDragging);
    
    /**
     * Tell whether this element is indeed currently moveable.
     */
    public boolean isMoveable();
    
    /**
     * Specify whether this element is indeed currently moveable.
     */
    public void setIsMoveable(boolean isMoveable);

    /**
     * Tell whether this element is resizable.
     */
    public boolean isResizable();

}
