import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

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