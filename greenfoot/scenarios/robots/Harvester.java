import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

/**
 *  A harvester robot moves in a straight line, 
 * and picks up all beepers it passes.
 */ 
public class Harvester extends Robot
{
  /**
   * Creates a new harvester robot.
   */
  public Harvester()
  {    
  }

  /**
   * Picks up all beepers at the current location 
   * and moves on step forward.
   */
  public void act()
  {
     pickBeeper();
     if(canMove()) {
         move();
     }  else {
         turnLeft();
     }
   }

}
