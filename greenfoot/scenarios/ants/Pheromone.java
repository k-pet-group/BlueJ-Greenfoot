import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Pheromones are dropped by ants when they wnat to communicate something to other ants.
 */
public class Pheromone extends Actor
{
    private final static int MAX_INTENSITY = 180;
    private int intensity;
    
    public Pheromone()
    {
        intensity = MAX_INTENSITY;
        updateImage();
    } 

    public Pheromone(int x, int y)
    {
        intensity = MAX_INTENSITY;
        setLocation(x, y);
        updateImage();
    }

    public void act()
    {
        intensity -= 1;        
        if(intensity <= 0) {
            getWorld().removeObject(this);
        }
        else {
            if((intensity % 4) == 0) {
                updateImage();
            }
        }
    }

    /**
     * Make the image
     */
    private void updateImage() 
    {        
        int size = intensity / 3 + 5;
        GreenfootImage image = new GreenfootImage(size+1, size+1);
        int alpha = intensity / 3;
        image.setColor(new Color(255, 255, 255, alpha));
        image.fillOval(0, 0, size, size);
        image.setColor(Color.DARK_GRAY);
        image.fillRect(size/2, size/2, 2, 2);        
        setImage(image);
    }
    
}
