
import greenfoot.World;
import greenfoot.Actor;

import java.util.List;

/**
 * A harvester robot moves in a straight line, and picks up all beepers it
 * passes.
 */
public class Harvester extends Robot
{
    /** 
     * Creates a new harvester robot.
     */
    public Harvester()
    {}

    /**
     * Picks up all beepers at the current location and moves on step forward.
     */
    public void act()
    {
        List here = getObjectsAtOffset(0, 0, Beeper.class); // take beepers, if any
        getWorld().removeObjects(here);
         
        if (canMove()) {
            move();
        }
        else {
            turnLeft();
        }
        
    }

}