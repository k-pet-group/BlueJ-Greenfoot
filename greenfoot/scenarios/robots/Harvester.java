
import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

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
        //pickBeeper();
          
        List here = getObjectsAt(0,0,Beeper.class);
        getWorld().removeObjects(here);
         
        if (canMove()) {
            move();
        }
        else {
            turnLeft();
        }
        
    }

}