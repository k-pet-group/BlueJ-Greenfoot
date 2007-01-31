import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Home Class
 * 
 * The Home class is for creating Home instances
 * where the Pirates originate from. Their is a class
 * variable for depicting the maximum amount of Pirates
 * to be created.
 * 
 * When it creates the Pirates they are given a reference
 * to the Home instance, so they can always find their way home
 * even if it moves.
 * 
 * @author Joseph Lenton
 * @version 16/01/07
 */
public class Home extends Actor
{
    private static final int MaximumPirates = 20;
    private int pirateCount = 0;
    private int treasure = 0;
    
    /**
     * Be the Home.
     */
    public Home()
    {
        
    }
    
    /**
     * Checks how many Pirates there are,
     * if their arn't enough,
     * it'll make some more.
     */
    public void act()
    {
        if (pirateCount < MaximumPirates) {
            getWorld().addObject(new Pirate(this), getX(), getY());
            pirateCount++;
        }
    }
    
    /**
     * Incriments the amount of treasure by 1
     */
    public void addTreasure()
    {
        treasure++;
    }
    
    /**
     * For getting the amount of treasure the home is holding.
     * @return the amount of treasure the home currently holds.
     */
    public int getTreasure()
    {
        return treasure;
    }
}