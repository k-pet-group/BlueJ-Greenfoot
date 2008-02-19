import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A dart is used to pop balloons.
 * 
 * @author Poul Henriksen
 */
public class Dart extends Actor
{
    /**
     * Make the dart follow the mouse and check for mouseclicks.
     */
    public void act() 
    {
        // Follow the mouse
        if(Greenfoot.mouseMoved(null)) {
            MouseInfo mouse = Greenfoot.getMouseInfo();
            setLocation(mouse.getX(), mouse.getY());
        }
        
        // Pop
        if(Greenfoot.mouseClicked(null)) {
            Balloon balloon = (Balloon) getOneObjectAtOffset(-getWidth()/2, getHeight()/2, Balloon.class);
            if(balloon != null) {
                balloon.pop();
            }
        }
        
    }    
}
