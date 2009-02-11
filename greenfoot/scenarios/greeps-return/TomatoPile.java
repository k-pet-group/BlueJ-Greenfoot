import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * A pile of tomatoes.
 * 
 * @author Michael Kolling
 * @version 2.0
 */
public class TomatoPile extends Actor
{
    private static Random randomizer = new Random();

    private static final int SIZE = 30;
    private static final int HALFSIZE = SIZE / 2;
    private static final Color color1 = new Color(255, 100, 100);
    private static final Color color2 = new Color(227, 49, 49);
    private static final Color color3 = new Color(100, 20, 20);

    private int tomatoes;

    /**
     * Create a pile of a given number of tomatoes.
     */
    public TomatoPile(int tomatoes)
    {
        this.tomatoes = tomatoes;
        updateImage();
    }
    
    public void act() 
    {        
        if (isEmpty()) {
            getWorld().removeObject(this);
        }
    }
    
    /**
     * Return the x-coordinate of the tomato pile's location.
     * 
     * You are allowed to call this method from the greep.
     * 
     * @see #Greep.getTomatoPile
     */
    public int getX() 
    {
        return super.getX();
    }
    
    /**
     * Return the x-coordinate of the tomato pile's location.
     * 
     * You are allowed to call this method from the greep.
     * 
     * @see #Greep.getTomatoPile
     */
    public int getY() 
    {
        return super.getY();
    }

    /**
     * Remove a tomato from this pile. (If it was the last one, this pile will
     * disappear from the world.)
     * 
     * You are not allowed to call this method.
     */
    protected void takeOne()
    {
        tomatoes = tomatoes - 1;
        updateImage();
        
    }
    
    /**
     * Is the pile empty?
     * 
     * You are not allowed to call this method.
     */
    protected boolean isEmpty() 
    {
        return tomatoes <= 0; 
    }

    /**
     * Update the image to show the current number of tomatoes.
     */
    private void updateImage()
    {
        GreenfootImage image = new GreenfootImage(SIZE+3, SIZE+3);

        for (int i = 0; i < tomatoes; i++) {
            drawTomato(image, randomCoord(), randomCoord());
        }
        setImage(image);
    }

    /**
     * Draw a single tomato onthe the given image at the position specified.
     */
    private void drawTomato(GreenfootImage image, int x, int y)
    {
        image.setColorAt(x + 1, y, color1);
        image.setColorAt(x, y + 1, color1);
        image.setColorAt(x, y + 2, color2);
        image.setColorAt(x + 1, y + 1, color2);
        image.setColorAt(x + 1, y + 2, color2);
        image.setColorAt(x + 2, y, color2);
        image.setColorAt(x + 2, y + 1, color2);
        image.setColorAt(x + 1, y + 3, color3);
        image.setColorAt(x + 2, y + 2, color3);
        image.setColorAt(x + 2, y + 3, color3);
        image.setColorAt(x + 3, y + 1, color3);
        image.setColorAt(x + 3, y + 2, color3);
    }
    
    /**
     * Generate a random number relative to the size of the food pile.
     */
    private int randomCoord()
    {
        int val = HALFSIZE + (int) (randomizer.nextGaussian() * (HALFSIZE / 2));
        if (val < 0)
            return 0;
        if (val > SIZE - 2)
            return SIZE - 2;
        else
            return val;
    }
}