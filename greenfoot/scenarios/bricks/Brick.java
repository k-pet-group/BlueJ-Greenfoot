import greenfoot.World;
import greenfoot.Actor;
public class Brick extends Actor
{
  public Brick()
  {
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