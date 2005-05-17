import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

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