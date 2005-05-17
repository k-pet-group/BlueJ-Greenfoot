import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class Brick extends GreenfootObject
{
  public Brick()
  {
    setImage("brick.png");
  }

  public void act()
  {
    //here you can create the behaviour of your object
  }

    public void collide()
    {
        getWorld().removeObject(this);
    }
}