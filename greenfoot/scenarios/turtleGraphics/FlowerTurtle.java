import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
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

  public void drawFlower() {
     
          turn(10);
          delay();
          drawSquare();
     
  }
}