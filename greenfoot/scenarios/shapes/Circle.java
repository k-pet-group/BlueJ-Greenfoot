import greenfoot.GreenfootObject;
import greenfoot.Image;
import greenfoot.Utilities;

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
        Image im = new Image(diameter, diameter);
        im.setColor(color);
        im.fillOval(0,0,diameter,diameter);
        setImage(im);
        Utilities.repaint();
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
        Utilities.repaint();
    }
    
    /**
     * Move the circle vertically by 'distance' pixels.
     */
    public void moveVertical(int distance)
    {
        setLocation(getX(), getY()+distance);
        Utilities.repaint();
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
            Utilities.delay();
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
            Utilities.delay();
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
}