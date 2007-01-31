import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Treasure Class
 * 
 * It holds a treasure value, and when the treasure is being depleted it will
 * display a counter showing how much of the treasure is left.
 * When it has no more treasure it will remove itself from the world.
 * 
 * @author Joseph Lenton
 * @version 16/01/07
 */
public class Treasure extends Actor
{
    // the default macimum amount of treasure
    private static final int MAXIMUM_TREASURE = 25;
    private int treasure;
    private Counter treasureCounter;
    
    /**
     * Constructor
     */
    public Treasure()
    {
        treasure = MAXIMUM_TREASURE;
    }

    /**
     * Checks if it's self should be removed
     * if it no longer has any treasure.
     * If so it also removes the counter, first.
     */
    public void act()
    {
        if (treasure == 0) {
            getWorld().removeObject(treasureCounter);
            getWorld().removeObject(this);
        }
    }
    
    /**
     * Returns the amount of treasure currently held by the instance.
     * @return the amount of treasure currently held.
     */
    public int getTreasure() {
        return treasure;
    }

    /**
     * Lowers the amount of treasure by one,
     * and then updates the counter.
     */
    public void takeTreasure()
    {
        treasure--;
        updateBar();
    }
    
    /**
     * Either creates the counter if it doesn't exist
     * and adds it to the current world,
     * or has it lower it's count by one.
     */
    private void updateBar()
    {
        if (treasureCounter == null) {
            treasureCounter = new Counter(getImage().getWidth(), 12, MAXIMUM_TREASURE);
            getWorld().addObject(treasureCounter, getX(), getY() + getImage().getHeight()/2);
        }
        
        treasureCounter.incriment();
    }
}