import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
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
        beeper.setLocation(getX(), getY());
        getWorld().addObject(beeper);

        move();
    }

}