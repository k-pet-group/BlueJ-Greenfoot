import greenfoot.World;
import greenfoot.Actor;
/**
 * A producer robot moves forward in a straight line 
 * and puts down a beeper in each cell it passes.
 */ 
public class Producer extends Robot
{
    public Producer() 
    {
    }

    /**
     * Create a beeper and put it down. Then moves one step forward
     */
    public void act() 
    {
        Beeper beeper = new Beeper(); 
        getWorld().addObject(beeper, getX(), getY());

        move();
    }

}