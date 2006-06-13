import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Counter that displays a number.
 * 
 * @author Michael Kolling
 * @version 1.0.1
 */
public class Counter extends Actor
{
    private int value = 0;
    private String text;
    private int stringLength;

    public Counter()
    {
        text = "";
        stringLength = (text.length() + 2) * 10;
        updateImage();
    }

    public Counter(String prefix)
    {
        text = prefix;
        stringLength = (text.length() + 2) * 10;

        setImage(new GreenfootImage(stringLength, 16));
        updateImage();
    }

    public void increment()
    {
        value++;
        updateImage();
    }

    /**
     * Make the image
     */
    private void updateImage()
    {
        GreenfootImage image = getImage();
        image.clear();
        image.setColor(Color.BLACK);
        image.drawString(text + value, 1, 12);
    }
}