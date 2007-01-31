import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * Native class
 * 
 * For creating Native instances. They will move around finding
 * Pirate instances, and when one is found they will pick them up.
 * They will then drop them later, and start to wait
 * before they go to pick up another Pirate.
 * 
 * @author Joseph Lenton
 * @version 4/12/06
 */
public class Native extends Unit
{
    // The Natives Speed
    private static final int SPEED = 4;
    private Pirate pirate = null;
    private int waitCount = 0;
    
    public Native()
    {
    }

    public void act()
    {
        if (pirate == null) {
            findPirate();
        }
        else {
            dropPirate();
        }
        
        move();
        walkingIntoEdge();
    }
    
    private void findPirate()
    {
        if (waitCount <= 0) {
            Pirate pirate = (Pirate) getOneIntersectingObject(Pirate.class);
            
            if (pirate != null) {
                this.pirate = pirate;
                getWorld().removeObject(pirate);
                waitCount = Greenfoot.getRandomNumber(40)+80;
            }
        }
        else {
            waitCount--;
        }
    }
    
    private void dropPirate()
    {
        if (waitCount <= 0) {
            getWorld().addObject(pirate, getX(), getY());
            waitCount = Greenfoot.getRandomNumber(20)+40;
            pirate = null;
        }
        else {
            waitCount--;
        }
    }
}