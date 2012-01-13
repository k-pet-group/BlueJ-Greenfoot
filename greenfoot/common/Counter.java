import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
import java.awt.Color;

/**
 * A Counter class that allows you to display a numerical value on screen.
 * 
 * The Counter is an actor, so you will need to create it, and then add it to
 * the world in Greenfoot.  If you keep a reference to the Counter then you
 * can adjust its value.  Here's an example of a world class that
 * displays a counter with the number of act cycles that have occurred:
 * 
 * <pre>
 * class CountingWorld
 * {
 *     private Counter actCounter;
 *     
 *     public CountingWorld()
 *     {
 *         super(600, 400, 1);
 *         actCounter = new Counter("Act Cycles: ");
 *         addObject(actCounter, 100, 100);
 *     }
 *     
 *     public void act()
 *     {
 *         actCounter.setValue(actCounter.getValue() + 1);
 *     }
 * }
 * </pre>
 * 
 * @author Neil Brown 
 * @version 1.0
 */
public class Counter extends Actor
{
    private final int HEIGHT = 25;
    private final Color FOREGROUND = Color.RED;
    
    private String prefix;
    private int value;
    
    /**
     * Creates a new Counter.
     */
    public Counter()
    {
        this("");
    }
    
    /**
     * Creates a new Counter that will use the given String as a prefix.
     * 
     * @param prefix What to write before the score, e.g. "Score: " or "Eaten: "
     */
    public Counter(String prefix)
    {
        this.prefix = prefix;
        this.value = 0;
        updateImage();
    }
    
    private void updateImage()
    {
        setImage(new GreenfootImage(prefix + value, HEIGHT, FOREGROUND, new java.awt.Color(0, true)));
    }
    
    /**
     * Sets a new value for the counter.  The display will update to reflect the new value.
     * 
     * If the new value is longer (has more digits), the counter will keep its left edge in the same place.
     */
    public void setValue(int newValue)
    {
        value = newValue;
        
        // Keep the left-edge in the same place after the score changes:
        int left = getX() - getImage().getWidth()/2;
        updateImage();
        if (getWorld() != null)
        {
            setLocation(getImage().getWidth()/2 + left, getY());
        }
    }
    
    /**
     * Gets the current value of the counter 
     */
    public int getValue()
    {
        return value;
    }
}
