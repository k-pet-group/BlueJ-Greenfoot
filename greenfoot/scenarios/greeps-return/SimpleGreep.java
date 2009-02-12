import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A Greep is an alien creature that likes to collect tomatoes.
 * 
 * Rules:
 * 1. You can call any method defined in the "Greep" superclass, except act().
 * 2. You cannot call any method defined in any other class, including Actor, except:
 *    2a.  getX() and getY()  [greeps carry a small GPS device which tells 
 *                             them their location at all times]
 *    2b.  getRotation() and setRotation()
 *    2c.  getX() and getY() on a TomatoPile object.
 * 
 * X. You may not redefine/override any methods from Actor, including getX(), getY()
 *    etc.
 * 
 * X. Your ship will deploy 20 greeps.
 * 
 * X: If you change the name of this class, you should also change it in Ship.createGreep()
 * 
 * @author (your name here)
 * @version 0.1
 */
public class SimpleGreep extends Greep
{
    // Remember: you cannot extend the Greep's memory. So:
    // no additional fields (other than final fields) allowed in this class!
    
    /**
     * Default constructor. Do not remove.
     */
    public SimpleGreep(Ship ship)
    {
        super(ship);
    }
    
    /**
     * Do what a greep's gotta do.
     */
    public void act()
    {
        super.act();   // do not delete! leave as first statement in act().
        if (carryingTomato()) {
            if(atShip()) {
                dropTomato();
            }
            else {
                turnHome();
                move();
            }
        }
        else {
            randomWalk();
            checkFood();
        }
    }
    
    /** 
     * Move forward, with a slight chance of turning randomly
     */
    public void randomWalk()
    {
        // there's a 3% chance that we randomly turn a little off course
        if (randomChance(3)) {
            turn((Greenfoot.getRandomNumber(3) - 1) * 100);
        }
        
        move();
    }

    /**
     * Is there any food here where we are? If so, try to load some!
     */
    public void checkFood()
    {
        // check whether there's a tomato pile here
        TomatoPile tomatoes = getTomatoes();
        if(tomatoes != null) {
            loadTomato();
            // Note: this attempts to load a tomato onto *another* Greep. It won't
            // do anything if we are alone here.
        }
    }


    /**
     * This method specifies the name of the greeps (for display on the result board).
     */
    public String getName()
    {
        return "Anonymous";  // write your name here! And keep it short.
    }
}
