import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A turtle that can draw a square
 * 
 * @author Poul Henriksen
 * @version 1.0.1
 */
public class SquareTurtle extends Turtle
{
    public SquareTurtle()
    {
       penDown();
    }

    public void act()
    {
        goAndTurn();
    }
  
    public void goAndTurn() {
        move(50);  
        turn(90);
    }
}