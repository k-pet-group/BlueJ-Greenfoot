package greenfoot.platforms;

/**
 * Interface for handling the simulation differently for the IDE and standalone 
 * versions of Greenfoot.
 * 
 * @author Poul Henriksen
 */
public interface SimulationDelegate
{
    /** 
     * This method will be called at speed changes.
     * 
     * @param speed The new speed.
     */
    public void setSpeed(int speed);
}
