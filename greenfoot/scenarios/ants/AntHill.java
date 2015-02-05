import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
 
/**
 * A hill full of ants.
 * 
 * @author Michael Kölling
 * @version 1.1
 */
public class AntHill extends Actor
{
    /** Number of ants that have come out so far. */
    private int ants = 0;
    
    /** Total number of ants in this hill. */
    private int maxAnts = 40;

    /** Counter to show how much food have been collected so far. */
    private Counter foodCounter;
    
    /**
     * Constructor for ant hill with default number of ants (40).
     */
    public AntHill()
    {
    }

    /**
     * Construct an ant hill with a given number of ants.
     */
    public AntHill(int numberOfAnts)
    {
        maxAnts = numberOfAnts;
    }

    /**
     * Act: If there are still ants left inside, see whether one should come out.
     */
    public void act()
    {
        if(ants < maxAnts) 
        {
            if(Greenfoot.getRandomNumber(100) < 10) 
            {
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
        if(foodCounter == null) 
        {
            foodCounter = new Counter("Food: ");
            int x = getX();
            int y = getY() + getImage().getWidth()/2 + 8;

            getWorld().addObject(foodCounter, x, y);
        }        
        foodCounter.increment();
    }
}