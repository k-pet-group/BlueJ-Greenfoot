import greenfoot.World;
import greenfoot.Actor;

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