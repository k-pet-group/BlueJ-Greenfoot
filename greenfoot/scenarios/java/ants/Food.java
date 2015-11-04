import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;
import java.util.Random;

/**
 * A pile of food. The pile consists initially of 100 crumbs of food.
 * 
 * @author Michael Kölling
 * @version 1.1
 */
public class Food extends Actor
{
    private static final int SIZE = 30;
    private static final int HALFSIZE = SIZE / 2;
    private static final Color color1 = new Color(160, 200, 60);
    private static final Color color2 = new Color(80, 100, 30);
    private static final Color color3 = new Color(10, 50, 0);

    private static final Random randomizer = new Random();
    
    private int crumbs = 100;  // number of bits of food in this pile

    /**
     * Create a pile of food with an image depicting the amount.
     */
    public Food()
    {
        updateImage();
    }

    /**
     * Remove some food from this pile of food.
     */
    public void takeSome()
    {
        crumbs = crumbs - 3;
        if (crumbs <= 0) {
            getWorld().removeObject(this);
        }
        else {
            updateImage();
        }
    }

    /**
     * Update the image
     */
    private void updateImage()
    {
        GreenfootImage image = new GreenfootImage(SIZE, SIZE);

        for (int i = 0; i < crumbs; i++) {
            int x = randomCoord();
            int y = randomCoord();

            image.setColorAt(x, y, color1);
            image.setColorAt(x + 1, y, color2);
            image.setColorAt(x, y + 1, color2);
            image.setColorAt(x + 1, y + 1, color3);
        }
        setImage(image);
    }

    /**
     * Returns a random number relative to the size of the food pile.
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