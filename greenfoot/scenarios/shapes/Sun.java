import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

/**
 * A sun.
 */
public class Sun extends Circle
{
  /**
   * Creates a yellow sun.
   */ 
  public Sun()
  {
    changeColor(java.awt.Color.YELLOW);
  }

  /**
   * Moves the sun down a bit.
   */
  public void act()
  {
    moveDown();
  }
}