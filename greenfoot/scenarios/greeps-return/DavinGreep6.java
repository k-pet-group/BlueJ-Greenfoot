import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;
import java.util.Iterator;

/**
 * This greep variant is based on Greep5.
 * 
 * - if we find a tomato patch and enemy greeps are present, hold position. This allows kablaming
 *   at appropriate times, thus sabotaging enemy efforts
 * - be slightly less aggressive in kablaming. Wait until we can see an extra opponent greep.
 * 
 * @author Davin McCall
 * @version 0.1
 */
public class DavinGreep6 extends Greep
{
    // Remember: you cannot extend the Greep's memory. So:
    // no additional fields (other than final fields) allowed in this class!
    
    /**
     * Default constructor for testing purposes.
     */
    public DavinGreep6(Ship ship)
    {
        super(ship);
        setFlag(1, randomChance(50)); // controls which direction to turn when an obstacle is hit
    }

    /**
     * Do what a greep's gotta do.
     */
    public void act()
    {
        super.act();   // do not delete! leave as first statement in act().
        if (carryingTomato()) {
            if(atShip()) {
                dropTomato();
                checkForKnownPile();
            }
            else { 
                headHomeward(-1, -1);
            }
        }
        else {
            // Not carrying tomatoes.
            
            if (getMemory(0) == 1) {
                // We have found some tomatoes. Sit tight and guard the pile.
                Greep friend = getFriend();
                if (friend != null && friend.getMemory(0) == 1) {
                    // The other greep is doing the same thing as me.
                    // I'll go home and store info about the pile in the
                    // ship's databank:
                    setMemory(0, 4);
                    headHomeward(-1, -1);
                    return;
                }
                
                block();
                // headHomeward(getMemory(1), getMemory(2));
                // checkKablam();
            }
            else if (getMemory(0) == 2) {
                // Collect tomatoes from a known location
                if (distanceTo(getMemory(1), getMemory(2)) < 5) {
                    if (getTomatoes() == null) {
                        // No more tomatoes in this pile - inform the ship
                        setMemory(0, 3);
                        moveNormally();
                    }
                    else {
                        if (checkKablam()) {
                            return;
                        }
                    }
                }
                else {
                    headHomeward(getMemory(1), getMemory(2));
                    loadTomato();
                }
            }
            else if (getMemory(0) == 3) {
                // A known pile is empty. We need to get back to the ship and
                // update the databank.
                if (atShip()) {
                    removePile();
                    setMemory(0, 0);
                    checkForKnownPile();
                }
                else {
                    headHomeward(-1, -1);
                }
            }
            else if (getMemory(0) == 4) {
                // Taking information about a pile location back to the ship.    
                // We know where there are some tomatoes... we're heading back to
                // the ship, to update the data bank.
                if (atShip()) {
                    addKnownPile();
                    setMemory(0, 0);
                    checkForKnownPile();
                    act();
                }
                else {
                    headHomeward(-1, -1);    
                }
            }
            else {
                moveNormally();    
            }
        }
    }
    
    private static int PILE_BASE_INDEX = 2;
    private static int PILE_INFO_SIZE = 2;
    
    /**
     * We're at the ship and we know about a pile of tomatoes.
     * Store that info in the ship's databank
     */
    private void addKnownPile()
    {
        int [] shipData = getShipData();
        if (shipData != null) {
            int numberKnown = shipData[PILE_BASE_INDEX];
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*i];
                int ky = shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*i];
                if (kx == getMemory(1) && ky == getMemory(2)) {
                    return; // already known    
                }
                else if (kx == -1) {
                    // An empty slot
                    shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*i] = getMemory(1);
                    shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*i] = getMemory(2);
                    // We may have created a duplicate, but that's no
                    // big deal.
                    return;
                }
            }
        
            shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*numberKnown] = getMemory(1);
            shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*numberKnown] = getMemory(2);
            numberKnown++;
            shipData[PILE_BASE_INDEX] = numberKnown;
        }
    }
    
    /**
     * We're at the ship, and we know that a pile of tomatoes has become
     * empty. Update the databank.
     */
    private void removePile()
    {
        int [] shipData = getShipData();
        if (shipData != null) {
            int numberKnown = shipData[PILE_BASE_INDEX];
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*i];
                int ky = shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*i];
                if (kx == getMemory(1) && ky == getMemory(2)) {
                    shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*i] = -1;
                    shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*i] = -1;
                }
            }
        }
    }
    
    /**
     * We're at the ship. Consult its databank for information on
     * tomato locations.
     */
    private void checkForKnownPile()
    {
        int [] shipData = getShipData();
        if (shipData != null) {
            int numberKnown = shipData[PILE_BASE_INDEX];
            int cx = -1;
            int cy = -1;
            int closestDist = 100000;
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[PILE_BASE_INDEX + 1 + PILE_INFO_SIZE*i];
                int ky = shipData[PILE_BASE_INDEX + 2 + PILE_INFO_SIZE*i];
                if (kx != -1) {
                    int kdist = distanceTo(kx, ky);
                    if (kdist < closestDist) {
                        closestDist = kdist;
                        cx = kx;
                        cy = ky;
                    }
                }
            }
            if (cx != -1) {
                setMemory(0, 2);
                setMemory(1, cx);
                setMemory(2, cy);
            }
        }
    }
    
    
    /**
     * Head back towards the ship (x == -1) or specified location. Avoid obstacles.
     */
    private void headHomeward(int x, int y)
    {
        boolean dir = getFlag(1);
        int r = getRotation();
        if (atWater() || moveWasBlocked()) {
            setRotation (r + (dir ? 45 : -45));
            setMemory(3, 10);
        }
        else {
            int turnCounter = getMemory(3);
            if (turnCounter == 0) {
                if (x == -1) {
                    turnHome();
                }
                else {
                    turnTowards(x,y);    
                }
            }
            else {
                turnCounter--;
                setRotation(r + (dir ? -4 : 4));
                setMemory(3, turnCounter);
                if (turnCounter == 0) {
                    setFlag(1, randomChance(10) ^ dir); // reverse turning direction    
                }
            }
        }
        move();
        if (moveWasBlocked()) {
            kablam();
        }
    }
    
    /**
     * Turn if we've hit water.
     * 
     * Kablam if there are a reasonable number of opponent greeps.
     * 
     * Otherwise, generally move around in a random manner. 
     */
    private void moveNormally()
    {
        randomWalk();
        if (atWater() || moveWasBlocked()) {
            int r = getRotation();
            setRotation (r + Greenfoot.getRandomNumber(2) * 180 - 90);
            move();
        }
        
        if (! checkKablam()) { 
            randomWalk();
            checkFood();
            checkForKnownPile(); // if at ship
        }
    }

    /**
     * Can we see more opponents than friendlies? If so, kablam!
     */
    private boolean checkKablam()
    {
        if (getNumberOfOpponents(false) > (getNumberOfFriends(false) + 2)) {
            kablam();
            return true;
        }
        return false;
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
            
            if (getNumberOfOpponents(false) > 0) {
                setMemory(0, 1);    
            }
            else {
                setMemory(0, 4);
            }
            
            // Seeing as we found tomatoes, head back to the ship and store the location
            // in the ship's databank:
            setMemory(1, tomatoes.getX());
            setMemory(2, tomatoes.getY());
        }
    }


    /**
     * This method specifies the name of the greep (for display on the result board).
     */
    public String getName()
    {
        return "Davin 6";  // write your name here!
    }
    
    /** Override method from Greep */
    public void randomWalk()
    {
        // there's a 3% chance that we randomly turn a little off course
        if (randomChance(3)) {
            turn((Greenfoot.getRandomNumber(3) - 1) * 100);
        }
        
        move();
    }
    
    private int distanceTo(int x, int y)
    {
        int deltaX = getX() - x;
        int deltaY = getY() - y;
        return (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

}
