
import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

/**
 * An inverter moves forward in a straight line. <br>
 * It inverts the beepers: if sees a beeper, 
 * it picks it up, else it puts down a beeper.
 */ 
public class Inverter extends Robot
{
    public Inverter()
    {
    }

    /**
     * Inverts and moves one step forward.
     */
    public void act()
    {         
        if (canMove()) {
            invert();
            move();
        }
        else {
            turnLeft();
        }
    }

    /**
     * Invert the current cell. This makes the robot pick up a beeper if there
     * is a beeper here, and puts a beeper down if there is no beeper
     */
    public void invert()
    {
        int beepersInBag = beeperBag.size();
        pickBeeper();

        if (beepersInBag == beeperBag.size()) {
            if (beepersInBag > 0) {
                putBeeper();
            }
            else {
                Beeper newBeeper = new Beeper();
                newBeeper.setLocation(getX(), getY());
                getWorld().addObject(newBeeper);
            }
        }
    }
}