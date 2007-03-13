import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * The BagleFloor Class is to represent Bagle's
 * which are on the floor.
 * It slowly moves down the screen, until it is off-screen.
 * 
 * @author Joseph Lenton
 * @version 13/03/07
 */
public class BagleFloor extends Actor
{
    // the number of steps to wait for, before the BagleFloor moves.
    private static final int MOVE_WAIT = 3;
    // the number of stepd the BagleFloor has waited for.
    private int moveCount;
    
    /**
     * BagleFloor constructor.
     * Creates the BagleFloor with a given starting rotation.
     * 
     * @param angle the angle of the BagleFloor, in degrees.
     */
    public BagleFloor(int angle)
    {
        moveCount = 0;
        setRotation(angle);
    }
    
    /**
     * If the BagleFloor can move, it will.
     * If it goes off-screen it will also remove its self
     * from the world.
     */
    public void act() 
    {
        // if BagleFloor can move
        if (moveCount >= MOVE_WAIT) {
            // check to remove itself
            if (getY() >= getWorld().getHeight()-1) {
                getWorld().removeObject(this);
            }
            // otherwise move one pixel down
            else {
                setLocation(getX(), getY()+1);
                // reset the movement counter
                moveCount = 0;
            }
        }
        
        // increment the movement counter
        moveCount++;
    }
}