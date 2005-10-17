import greenfoot.GreenfootObject;
import greenfoot.GreenfootImage;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * A circle
 */
public class Circle extends GreenfootObject
{
    
    private Color color;
    private int diameter;
    
    /**
     * Creates a black circle.
     */
    public Circle()
    {
        diameter=32;
        color=Color.BLACK;
        draw();
        
    }
    
    /**
     * Draws the circle.
     */
    public void draw() {
        GreenfootImage im = new GreenfootImage(diameter, diameter);
        im.setColor(color);
        im.fillOval(0,0,diameter-1,diameter-1);
        setImage(im);

    }
    
    
    /**
     * Does nothing.
     */
    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    
    /**
     * Move the circle a few pixels to the right.
     */
    public void moveRight()
    {
        moveHorizontal(20);
    }
    
    /**
     * Move the circle a few pixels to the left.
     */
    public void moveLeft()
    {
        moveHorizontal(-20);
    }
    
    /**
     * Move the circle a few pixels up.
     */
    public void moveUp()
    {
        moveVertical(-20);
    }
    
    /**
     * Move the circle a few pixels down.
     */
    public void moveDown()
    {
        moveVertical(20);
    }
    
    /**
     * Move the circle horizontally by 'distance' pixels.
     */
    public void moveHorizontal(int distance)
    {
        setLocation(getX()+distance, getY());

    }
    
    /**
     * Move the circle vertically by 'distance' pixels.
     */
    public void moveVertical(int distance)
    {
        setLocation(getX(), getY()+distance);

    }
    
    /**
     * Slowly move the circle horizontally by 'distance' pixels.
     */
    public void slowMoveHorizontal(int distance)
    {
        int delta;
        
        if(distance < 0) 
        {
            delta = -1;
            distance = -distance;
        }
        else 
        {
            delta = 1;
        }
        
        for(int i = 0; i < distance; i++)
        {
            setLocation(getX()+delta, getY());

        }
    }
    
    /**
     * Slowly move the circle vertically by 'distance' pixels.
     */
    public void slowMoveVertical(int distance)
    {
        int delta;
        
        if(distance < 0) 
        {
            delta = -1;
            distance = -distance;
        }
        else 
        {
            delta = 1;
        }
        
        for(int i = 0; i < distance; i++)
        {
            setLocation(getX(), getY()+delta);

        }
    }
    
    /**
     * Change the size to the new size (in pixels). Size must be >= 0.
     */
    public void changeSize(int newDiameter)
    {   
        diameter=newDiameter;
        draw();
    }
    
    /**
     * Change the color.
     */
    public void changeColor(Color newColor)
    {
        color = newColor;
        draw();
    }
    
    public boolean contains(int dx, int dy) {
        int dist = dx * dx + dy * dy;
        int radius = (int) (diameter/2.);
        if(dist < radius*radius) {
            return true;
        }
        else {
            return false;
        }
    } 
}