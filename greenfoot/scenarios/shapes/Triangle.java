import greenfoot.GreenfootObject;
import greenfoot.Image;
import greenfoot.Utilities;

import java.awt.Color;


/**
 * A triangle
 */
public class Triangle extends GreenfootObject
{
    private Color color;
    private int width;
    private int height;
    
    /**
     * Creates a new triangle.
     */
    public Triangle()
    {
        width=32;
        height=32;
        color=Color.BLACK;
        draw();
    } 
    
    /**
     * Draws the triangle
     */
    public void draw() {
        Image im = new Image(width, height);
        im.setColor(color);
        int[] xpoints = {0, 0 + (width / 2), width};
        int[] ypoints = {height, 0, height};
        im.fillPolygon(xpoints, ypoints, 3);
        setImage(im);
        Utilities.repaint();
    }
    
    /**
     * Does nothing
     */
    public void act()
    {
        //here you can create the behaviour of your object
    }
    
    
    /**
     * Move the triangle a few pixels to the right.
     */
    public void moveRight()
    {
        moveHorizontal(20);
    }
    
    /**
     * Move the triangle a few pixels to the left.
     */
    public void moveLeft()
    {
        moveHorizontal(-20);
    }
    
    /**
     * Move the triangle a few pixels up.
     */
    public void moveUp()
    {
        moveVertical(-20);
    }
    
    /**
     * Move the triangle a few pixels down.
     */
    public void moveDown()
    {
        moveVertical(20);
    }
    
    /**
     * Move the triangle horizontally by 'distance' pixels.
     */
    public void moveHorizontal(int distance)
    {
        setLocation(getX()+distance, getY());
        Utilities.repaint();
    }
    
    /**
     * Move the triangle vertically by 'distance' pixels.
     */
    public void moveVertical(int distance)
    {
        setLocation(getX(), getY()+distance);
        Utilities.repaint();
    }
    
    /**
     * Slowly move the triangle horizontally by 'distance' pixels.
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
     * Slowly move the triangle vertically by 'distance' pixels.
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
    public void changeSize(int newWidth, int newHeight)
    {
        height = newHeight;
        width = newWidth;
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