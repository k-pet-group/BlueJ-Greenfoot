import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.awt.*;
import javax.swing.*;
import java.awt.image.*;

public class Pheromone extends GreenfootObject
{
    private final static int MAX_INTENSITY = 60;
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
        intensity--;
        if(intensity <= 0) {
            getWorld().removeObject(this);
        }
        else {
            updateImage();
        }
    }

    /**
     * Make the image
     */
    private void updateImage() 
    {
        int size = (intensity / 2) + 10;
        BufferedImage image = new BufferedImage(size+1, size+1,
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();
        int alpha = (150 - MAX_INTENSITY) +  intensity;
        g.setColor(new Color(255, 255, 255, alpha));
        g.fillOval(0, 0, size, size);
        g.setColor(Color.DARK_GRAY);
        g.fillRect(size/2, size/2, 1, 1);
        setImage(new ImageIcon(image));
    }
    
    public int getWidth() {        
        return getImage().getIconWidth();
    }
    
    public int getHeight() {        
        return getImage().getIconWidth();
    }
    
    public int getCenterX() {
        return getX() + getWidth()/2;
    }
    
    public int getCenterY() {
        return getY() + getHeight()/2;
    }
}
