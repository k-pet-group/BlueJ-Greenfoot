import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
public class Collector extends Robot
{
  public Collector()
  {
    //setImage("name of the image file");
  }

  /**
   * Inverts and moves one step forward.
   */
  public void act()
  {
     pickOrDrop();
     if(canMove()) {
         move();
     }  else {
         turnLeft();
         turnLeft();
         turnLeft();
     }
  } 
  
  /**
   * Invert the current cell. This makes the robot pick up 
   * a beeper if there is a beeper here, and puts a beeper 
   * down if there is no beeper
   */
  public void pickOrDrop() {
      int beepersInBag = beeperBag.size();          
      pickBeeper();
      
      if(beepersInBag == beeperBag.size()) {
          if(beepersInBag>0) {
              putBeeper();
          }
      }
  }
}