import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;
import java.awt.*;

/**
 * A world for the robots
 */
public class RobotWorld extends GreenfootWorld
{
    /**
     * Creates a new world with 20x20 cells and 
     * with a cell size of 50x50 pixels
     */ 
    public RobotWorld() {
        super(10,10,50,50);
        setBackgroundColor(Color.BLACK);
        setBackgroundImage("road.gif");
        setTiledBackground(true);
    }
    
    public void populate() {
        Robot robot = new Harvester();
        robot.setLocation(3,1);
        addObject(robot);
        
        Beeper beeper1 = new Beeper();
        beeper1.setLocation(3,6);
        addObject(beeper1);
        
        Beeper beeper2 = new Beeper();
        beeper2.setLocation(3,4);
        addObject(beeper2);   
    
        Beeper beeper3 = new Beeper();
        beeper3.setLocation(7,7);
        addObject(beeper3);   
        
        Beeper beeper4 = new Beeper();
        beeper4.setLocation(5,7);
        addObject(beeper4);  
    
    }  
    
}
