import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;

/**
 * A Greep is an alien creature that likes to collect tomatoes.
 * 
 * This is a simple greep that makes use of most of the functionality in Greep,
 * and it is the default opponent that you will have when competing in the 
 * Greeps competition. 
 * 
 * This greep uses the memory to remember where it has seen tomatoes, and will 
 * go back to get more. Also, if they hit water, they turn immediately and try 
 * a different direction.
 * 
 * If the greep is waiting alone at a pile of tomatoes it will block so opponents 
 * can't get to the pile.
 * 
 * Finally, if the greep can see multiple opponent greeps, it will let
 * off a stink bomb.
 * 
 * This greep does not communicate with other greeps or the ship.
 * 
 * @author Davin McCall
 * @author Poul Henriksen
 * @version 1.0
 */
public class SimpleGreep extends Greep
{
    // Remember: you cannot extend the Greep's memory. So:
    // no additional fields (other than final fields) allowed in this class!
    
    private static final int TOMATO_LOCATION_KNOWN = 1;
    
    /**
     * Default constructor. Do not remove.
     */
    public SimpleGreep(Ship ship)
    {
        super(ship);
    }
    
    /**
     * Do what a greep's gotta do.
     */
    public void act()
    {
        super.act();   // do not delete! leave as first statement in act().        

        // Before moving, lets check for food.
        checkFood();
            
        if (carryingTomato()) {
            bringTomatoHome();
        }
        else if(getTomatoes() != null) {            
            TomatoPile tomatoes = getTomatoes(); 
            if(!blockAtPile(tomatoes)) {
                // Not blocking so lets go towards the centre of the pile
                turnTowards(tomatoes.getX(), tomatoes.getY());
                move();
            }
        }
        else if (getMemory(0) == TOMATO_LOCATION_KNOWN) {
            // Hmm. We know where there are some tomatoes...
            turnTowards(getMemory(1), getMemory(2));
            move();
        }
        else if (numberOfOpponents(false) > 3) {
            // Can we see four or more opponents?
            kablam();            
        } 
        else {
            randomWalk();
        }        
        
        // Avoid obstacles
        if (atWater() || moveWasBlocked()) {
            // If we were blocked, try to move somewhere else
            int r = getRotation();
            setRotation (r + Greenfoot.getRandomNumber(2) * 180 - 90);
            move();
        }        
    }

    /**
     * Is there any food here where we are? If so, try to load some!
     */
    public void checkFood()
    {
        TomatoPile tomatoes = getTomatoes();
        if(tomatoes != null) {
            loadTomato();
            // Note: this attempts to load a tomato onto *another* Greep. It won't
            // do anything if we are alone here.
            
            setMemory(0, TOMATO_LOCATION_KNOWN);
            setMemory(1, tomatoes.getX());
            setMemory(2, tomatoes.getY());
        }
    }
    
    /** 
     * Move forward, with a slight chance of turning randomly
     */
    private void randomWalk()
    {
        // there's a 3% chance that we randomly turn a little off course
        if (randomChance(3)) {
            turn((Greenfoot.getRandomNumber(3) - 1) * 100);
        }
        
        move();
    }
    
    /**
     * Bring a tomato to our ship. Drop it if we are at the ship.
     */
    private void bringTomatoHome() 
    {
        if(atShip()) {
            dropTomato();
        }
        else {
            turnHome();
            move();
        }
    }
    
    /**
     * If we are at a tomato pile and none of our friends are blocking, we will block.
     * 
     * @return True if we are blocking, false if not.
     */
    private boolean blockAtPile(TomatoPile tomatoes) 
    {
        // Are we at the centre of the pile of tomatoes?  
        boolean atPileCentre = tomatoes != null && distanceTo(tomatoes.getX(), tomatoes.getY()) < 4;
        if(atPileCentre && getFriend() == null ) {
            // No friends at this pile, so we might as well block
            block(); 
            return true;
        }
        else {
            return false;
        }
    }
    
    private int distanceTo(int x, int y)
    {
        int deltaX = getX() - x;
        int deltaY = getY() - y;
        return (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    
    /**
     * This method specifies the name of the author (for display on the result board).
     */
    public String getName()
    {
        return "Simple";
    }    
}
