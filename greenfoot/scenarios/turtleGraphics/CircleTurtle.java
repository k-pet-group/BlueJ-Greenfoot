import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A turtle that runs in circles.
 * 
 * @author Poul Henriksen
 * @version 1.0.1
 */
public class CircleTurtle extends Turtle
{
    public CircleTurtle()
    {
        penDown();
    }

    public void act()
    {
        move(5);
        turn(2);
    }
}