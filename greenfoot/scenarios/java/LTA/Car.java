import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A car that can be driven with the right/left cursor keys.
 * 
 * @author mik 
 * @version 1.0
 */
public class Car extends Actor
{
    /**
     * Constructor for Car - nothing to do.
     */
    public Car()
    {
    }
    
    /**
     * Drive and allow steering.
     */
    public void act()
    {
        if ( Greenfoot.isKeyDown("left") )
        {
            turn(-5);
        }
        if ( Greenfoot.isKeyDown("right") )
        {
            turn(5);
        }
        move(4);
    }
}