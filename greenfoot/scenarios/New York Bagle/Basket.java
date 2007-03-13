import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * The Basket Class collects Bagles.
 * When a Bagle is added, it will drawn a miniture bagle over
 * it's own picture, to show it as a new Bagle added to the basket.
 * 
 * @author Joseph Lenton
 * @version 13/03/07
 */
public class Basket extends Actor
{
    // the image of the miniture bagle
    private static final GreenfootImage BAGLE_IMAGE = new GreenfootImage("bagle_small.png");
    
    // the number of bagles in the basket
    private int bagleCount = 0;
    
    /**
     * No action is performed when act is called.
     */
    public void act() 
    {
        // empty
    }
   
    /**
     * Adds a new Bagle to the Basket.
     */
    public void addBagle()
    {
        bagleCount++;
        updateImage();
    }
    
    /**
     * Draws a Bagle image randomly onto the Basket's image,
     * so it looks like it has an extra Bagle inside.
     */
    private void updateImage()
    {
        GreenfootImage image = getImage();
        int bagleX = Greenfoot.getRandomNumber( image.getWidth()-BAGLE_IMAGE.getWidth() );
        int bagleY = Greenfoot.getRandomNumber( image.getHeight()-BAGLE_IMAGE.getHeight() );
        
        image.drawImage(BAGLE_IMAGE, bagleX, bagleY);
    }
}
