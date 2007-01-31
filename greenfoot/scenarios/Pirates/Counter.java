import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * Counter Class
 * 
 * The Counter class is to create instances
 * of counters, for counting down. It also creates
 * a bar for it's image, to represent the
 * current value of the counter.
 * 
 * @author Joseph Lenton
 * @version 16/01/07
 */
public class Counter extends Actor
{
    // the starting count of the counter
    private int startingCount;
    // the actual count value of the counter
    private int count;
    
    /**
     * The Counter's constructor.
     * Requiring the height and width of the counter
     * to determine the count bar's maximum size.
     * 
     * The count is also made equal to the starting count.
     * 
     * @param width the maximum width of the counter's bar
     * @param height the height of the counter's bar
     * @param startingCount the initial count of the counter
     */
    public Counter(int width, int height, int startingCount)
    {
        this.startingCount = startingCount;
        this.count = startingCount;
        
        setImage(new GreenfootImage(width, height));
    }

    /**
     * Be the counter.
     */
    public void act()
    {
        
    }

    /**
     * Reduces the count by one, if it is over 0.
     */
    public void incriment()
    {
        if (count > 0) {
            count--;
        }
        updateImage();
    }
    
    /**
     * Resets the counter to it's initial starting count
     * value.
     */
    public void reset()
    {
        count = startingCount;
    }
    
    /**
     * Changes the count of the counter, to a new value.
     * @param count the new value for the counter.
     */
    public void setCount(int count)
    {
        this.count = count;
    }
    
    /**
     * Returns the current count of the counter.
     * @return the current count of the counter
     */
    public int getCount()
    {
        return count;
    }
    
    /**
     * Update Image updates the image of the counter,
     * so it is the correct length.
     * The image is cleared, and then the bar is re-drawn.
     */
    private void updateImage()
    {
        double startingCount = this.startingCount;
        
        GreenfootImage image = getImage();
        // clear the image
        image.clear();
        if (startingCount != 0) {
            // draw the red count bar
            image.setColor(Color.RED);
            image.fillRect(0,0,(int) (image.getWidth() * (count/startingCount)), image.getHeight());
            
            // draw a black outline
            image.setColor(Color.BLACK);
            image.drawRect(0,0,(int) (image.getWidth() * (count/startingCount)), image.getHeight()-1);
        }
    }
}