import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class Blabla extends GreenfootObject
{
  public Blabla()
  {
    setImage("Robot.gif");  
    }
  public void act()
  {
    moveDown();
  }
  
  public void moveDown() {
      setLocation(getX(), getY() +1);
  }

}