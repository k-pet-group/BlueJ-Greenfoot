
import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.Utilities;
public class FlowerTurtle extends SquareTurtle
{
    public FlowerTurtle()
    {
        setColor("red");
    }

    public void act()
    {
        drawFlower();
    }

    public void drawFlower()
    {
        turn(10);
        Utilities.delay();
        drawSquare();
    }
}