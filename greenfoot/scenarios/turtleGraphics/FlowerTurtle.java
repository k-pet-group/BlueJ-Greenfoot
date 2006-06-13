import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A turtle that draw something that looks like a flower (might require 
 * some imagination to see this though)
 * 
 * @author Poul Henriksen
 * @version 1.0.1
 */
public class FlowerTurtle extends SquareTurtle
{
    public FlowerTurtle()
    {
        setColor("red");
    }

    public void act()
    {
        turn(10);
        goAndTurn();
    }
}