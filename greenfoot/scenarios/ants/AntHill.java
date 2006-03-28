import greenfoot.World;
import greenfoot.Actor; 
 
import java.util.Random;
  
public class AntHill extends Actor
{
    private final static Random randomizer = AntWorld.getRandomizer();
    private final static int DEFAULT_ANTS = 40;
    
    // number of ants created so far
    private int ants = 0;
    private int maxAnts;
    private Counter foodCounter;
    
    public AntHill()
    {
        maxAnts = DEFAULT_ANTS;
        setImage("images/anthill.gif");
    }

    public AntHill(int numberOfAnts)
    {
        maxAnts = numberOfAnts;
        setImage("images/anthill.gif");
    }

    public void act()
    {
        if(ants < maxAnts) {
            if(randomizer.nextInt(100) < 10) {            
                getWorld().addObject(new Ant(getX(), getY(), this));
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
            int y = getY() +getWidth()/2 + 8;
            if(y >= getWorld().getHeight()) {
                y = getWorld().getHeight();    
            }

            foodCounter.setLocation(x, y);
            getWorld().addObject(foodCounter);
        }        
        foodCounter.increment();
    }

}