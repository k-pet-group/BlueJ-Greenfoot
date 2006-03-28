import greenfoot.World;
import greenfoot.Actor;

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