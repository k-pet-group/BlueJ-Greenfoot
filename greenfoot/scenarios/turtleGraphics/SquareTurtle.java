import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import greenfoot.Utilities;
public class SquareTurtle extends Turtle
{
  public SquareTurtle()
  {
    penDown();
  }

  public void act()
  {
    drawSquare();
  }
  
  public void drawSquare() {
    for(int i=0; i<4; i++) {
        move(50);  
        turn(90);
        Utilities.delay();
    }
  }

}