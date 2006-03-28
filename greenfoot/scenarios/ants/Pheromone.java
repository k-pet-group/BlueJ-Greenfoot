import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

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
        BufferedImage image = new BufferedImage(size+1, size+1,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        int alpha = intensity / 3;
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillOval(0, 0, size, size);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(size/2, size/2, 2, 2);
        
        setImage(new GreenfootImage(image));
    }
    
}
