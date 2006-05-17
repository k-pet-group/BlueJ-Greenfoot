import greenfoot.World;
import greenfoot.Actor;

/**
 * A turtle that can draw a square
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