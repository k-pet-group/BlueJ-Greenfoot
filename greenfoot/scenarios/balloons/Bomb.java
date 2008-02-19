import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
import java.util.List;
/**
 *  A bomb can be used to explode balloons. It will make an explosion that pops
 *  all the balloons that it touches.
 *  <p>
 *  
 *  The bombs are used by dragging them from their initial location and onto a 
 *  balloon. This will trigger the explosion. If the balloon is dragged to a 
 *  location with out a balloon it will return the bomb to its original 
 *  location.
 *  
 * @author Poul Henriksen
 */
public class Bomb extends Actor
{
    // The initial location of the bomb
    private int originalX;
    private int originalY;
    
    /**
     * Will store the initial location of the bomb.
     */
    public void addedToWorld(World world) {
        originalX = getX();
        originalY = getY();
    }

    /**
     * Check the mouse and make the bomb draggable.  
     *
     */  
    public void act() 
    { 
        // Drag the bomb
        if(Greenfoot.mouseDragged(this)) {
            MouseInfo mouse = Greenfoot.getMouseInfo();
            setLocation(mouse.getX(), mouse.getY());             
        }
         
        // Check if the drag has ended.
        if(Greenfoot.mouseDragEnded(this)) {
            if(getOneIntersectingObject(Balloon.class) != null) {
                explode();
            } 
            else { 
                reset();
            }
        }        
    }    
    
    /**
     * Make an explosion.
     */
    private void explode() {
        BalloonWorld w;
        List balloons = getWorld().getObjects(Balloon.class);
        
        getWorld().addObject(new Explosion(), getX(), getY());
        getWorld().removeObject(this);
    }
    
    /**
     * Reset the bomb to its original location.
     */
    private void reset() 
    {
        setLocation(originalX, originalY);
    }
}
