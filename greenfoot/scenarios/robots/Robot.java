import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import java.util.*;

public class Robot extends GreenfootObject
{

  /** 
   * East direction for the robot.
   * @see #setDirection(int)
   */
  public static final int EAST = 0;
  /** 
   * West direction for the robot.
   * @see #setDirection(int)
   */
  public static final int WEST = 1;
  /** 
   * North direction for the robot.
   * @see #setDirection(int)
   */
  public static final int NORTH = 2;
  /** 
   * South direction for the robot.
   * @see #setDirection(int)
   */
  public static final int SOUTH = 3;

  private int direction;       
  
  Stack beeperBag = new Stack();
  
  /**
   * Creates a robot. 
   */ 
  public Robot()
  {
    setImage("Robot.gif");
    setDirection(SOUTH);
  }  
  
  /**
   * Does nothing.
   */
  public void act()
  {
    //here you can create the behaviour of your object
  }
  
  /**
   * Moves the robot one cell forward in the 
   * current direction 
   */
   public void move() {        
        if(! canMove()) {
            return;
        }
        switch (direction) {
            case SOUTH :
                setLocation(getX(), getY() + 1);
                break;
            case EAST :
                setLocation(getX() + 1, getY());
                break;
            case NORTH :
                setLocation(getX(), getY() - 1);
                break;
            case WEST :
                setLocation(getX() - 1, getY());
                break;
        }
    }
    
   /**
    * Test if the robot can move forward.
    * 
    * @return true if the robot can move.
    */
    public boolean canMove() {   
    GreenfootWorld myWorld = getWorld();
    int x = getX();
    int y = getY();
    switch (direction) {
     case SOUTH :
       y++;
       break;
     case EAST :
       x++;
       break;
     case NORTH :
       y--;
       break;
     case WEST :
       x--;
       break;
    }        
    // test for outside border 
    if(x>=myWorld.getWorldWidth() || y>=myWorld.getWorldHeight()) {
     return false;
    } else if(x<0 || y<0) {
     return false;
    } 
    
    //Run through all objects and see if there is a wall.
    Collection objectsThere = myWorld.getObjectsAtCell(x, y, Wall.class, false);
    if(objectsThere.isEmpty()) {
        return true;
    } else {
        return false;
    }
    }
    
    /**
     * Turns the robot to the left. 
     */
    public void turnLeft() {
        switch (direction) {
            case SOUTH :
                setDirection(EAST);
                break;
            case EAST :
                setDirection(NORTH);
                break;
            case NORTH :
                setDirection(WEST);
                break;
            case WEST :
                setDirection(SOUTH);
                break;           
        }
    }
    
    
    /**
     * Sets the direction of the robot.
     * 
     * @see #SOUTH
     * @see #EAST 
     * @see #NORTH
     * @see #WEST 
     */ 
    public void setDirection(int direction) {
        this.direction = direction;
        switch (direction) {
            case SOUTH :
                setRotation(90);
                break;
            case EAST :
                setRotation(0);
                break;
            case NORTH :
                setRotation(270);
                break;
            case WEST :
                setRotation(180);
                break;
            default :
                break;
        }
        update();
    }
    
    
    /**
     * If there is a beeper at the robots current location, 
     * it picks it up.
     * 
     */
    public void pickBeeper() {
        GreenfootWorld myWorld = getWorld();
        Collection objectsHere = myWorld.getObjectsAtCell(getX(), getY(), Beeper.class, false);
        Iterator iter = objectsHere.iterator();
        if(iter.hasNext()) {
            Object currentObject = iter.next();
            Beeper beeper = (Beeper) currentObject;
            myWorld.removeObject(beeper);
            beeperBag.add(beeper);
            
        }
    }

    /**
     * If the robot has any beepers, the last one that was picked up is put down
     * again.
     *  
     */
    public void putBeeper() {
        GreenfootWorld myWorld = getWorld();
        if (!beeperBag.isEmpty()) {            
            Beeper beeper = (Beeper) beeperBag.pop();    
            beeper.setLocation(getX(), getY());
            myWorld.addObject(beeper);
            setLocation(getX(), getY());
        }
    }    
}
