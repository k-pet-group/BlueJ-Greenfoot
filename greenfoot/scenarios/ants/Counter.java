import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Counter extends GreenfootObject
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
        updateImage();
    }

    public void act()
    {
        //here you can create the behaviour of your object
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
        BufferedImage image = new BufferedImage(stringLength, 16,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        g.setColor(Color.BLACK);
        g.drawString(text + value, 1, 12);
        setImage(new GreenfootImage(image));
    }
}