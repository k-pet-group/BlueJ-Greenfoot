import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
 
import java.util.Random;
  
/**
 * A hill full of ants.
 * 
 * @author Michael Kolling
 * @version 1.0.1
 */
public class AntHill extends Actor
{
    
    /** Number of ants created so far. */
    private int ants = 0;
    /** Maximum number of ants the hill can produce. */
    private int maxAnts = 40;
    /** Counter to show how much food have been collected so far. */
    private Counter foodCounter;
    
    public AntHill()
    {
    }

    public AntHill(int numberOfAnts)
    {
        maxAnts = numberOfAnts;
    }

    public void act()
    {
        if(ants < maxAnts) {
            if(Greenfoot.getRandomNumber(100) < 10) {            
                getWorld().addObject(new Ant(this), getX(), getY());
                ants++;
            }
        }
    }

    /**
     * Record that we have collected another bit of food.
     */
    public void countFood()
    {
        if(foodCounter == null) {
            foodCounter = new Counter("Food: ");
            int x = getX();
            int y = getY() + getImage().getWidth()/2 + 8;
            if(y >= getWorld().getHeight()) {
                y = getWorld().getHeight();    
            }

            getWorld().addObject(foodCounter, x, y);
        }        
        foodCounter.increment();
    }
}