import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.Random;

public class AntHill extends GreenfootObject
{
    private final static Random randomizer = AntWorld.getRandomizer();
    private final static int DEFAULT_MAX_ANTS = 50;
    
    // number of ants created so far
    private int ants = 0;
    private int maxAnts;
    private Counter foodCounter;
    
    public AntHill()
    {
        maxAnts = DEFAULT_MAX_ANTS;
        setImage("anthill.gif");
    }

    public AntHill(int numberOfAnts)
    {
        maxAnts = numberOfAnts;
        setImage("anthill.gif");
    }

    public void act()
    {
        if(ants < maxAnts) {
            if(randomizer.nextInt(100) < 10) {            
                getWorld().addObject(new Ant(getX() + 2, getY() + 2, this));
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
            int y = getY() + 8;
            if(y >= getWorld().getWorldHeight()) {
                y = getWorld().getWorldHeight();    
            }

            foodCounter.setLocation(getX(), getY() + 8);
            getWorld().addObject(foodCounter);
        }        
        foodCounter.increment();
    }

}