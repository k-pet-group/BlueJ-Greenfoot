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
     * @param ghostX The ghostX to set.
     */
    public void setGhostX(int ghostX);
    
    /**
     * @param ghostY The ghostY to set.
     */
    public void setGhostY(int ghostY);
    
    /** returns whether */
    public boolean isMoving();
    
    public void setIsMoving(boolean isMoving);
    
    public boolean isMoveable();
    
    public void setIsMoveable(boolean isMoveable);

}
