import greenfoot.World;
import greenfoot.Actor;

/**
 * A turtle that draw something that looks like a flower (might requrie 
 * some imagination to see this though)
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