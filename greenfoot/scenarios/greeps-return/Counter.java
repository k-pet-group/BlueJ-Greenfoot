import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Counter that displays an (optional) text and a number.
 * 
 * @author Michael Kolling
 * @version 1.0
 */
public class Counter extends Actor
{
    public static final float FONT_SIZE = 18.0f;
    
    private int value;
    private String text;
    private int stringLength;
    private Font font;

    /**
     * Create a counter without text. 
     */
    public Counter()
    {
        this("");
    }

    /**
     * Create a counter with prefix text, and 0 value.
     */
    public Counter(String prefix)
    {
        this(prefix, 0);
    }
    
    /**
     * Create a counter with prefix text and value.
     */
    public Counter(String prefix, int value)
    {
        this.value = value;
        text = prefix;
        stringLength = (text.length() + 4) * 10;

        GreenfootImage image = new GreenfootImage(stringLength, 22);
        setImage(image);
        font = image.getFont();
        font = font.deriveFont(FONT_SIZE);
        updateImage();
    }

    /**
     * Increment the counter by one.
     */
    public void increment()
    {
        value++;
        updateImage();
    }

    /**
     * Return the current counter value.
     */
    public int getValue()
    {
        return value;
    }
    
    /**
     * Make the image
     */
    private void updateImage()
    {
        GreenfootImage image = getImage();
        image.clear();
        image.setFont(font);
        image.setColor(Color.BLACK);
        image.drawString(text + value, 6, (int)FONT_SIZE);
    }
}