import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;
import java.util.Iterator;

/**
 * This greep variant is based on PolleGreep2.
 *  
 *  Fixed a lot of bugs. But it now creates too many assassins.
 *   
 * 
 * @author Davin McCall, Poul Henriksen.
 * @version 0.1
 */
public class PolleGreep4 extends Greep
{
    // Remember: you cannot extend the Greep's memory. So:
    // no additional fields (other than final fields) allowed in this class!
    
    private static int ENEMY_SHIP_SLOT = 0;
    
    private static int ASSASSINS = 2;
    
    /**
     * Default constructor for testing purposes.
     */
    public PolleGreep4()
    {
        setFlag(1, randomChance(50));
    }

    private int getEnemyShipX() {
        int mem = getMemory(ENEMY_SHIP_SLOT);
        return mem / 1000;
    }
    
    private int getEnemyShipY() {
        int mem = getMemory(ENEMY_SHIP_SLOT);
        return mem % 1000;        
    }    
    
    private void setEnemyShipX(int x) {
        int y = getEnemyShipY();
        int mem = x * 1000 + y;
        setMemory(ENEMY_SHIP_SLOT, mem);
    }
    
    private void setEnemyShipY(int y) {        
        int x = getEnemyShipX();
        int mem = x * 1000 + y;
        setMemory(ENEMY_SHIP_SLOT, mem); 
    }
    
    private boolean enemyLocationKnown() 
    {
        return getEnemyShipX() != 999;
    }
    
    private boolean isAssassin() 
    {
        if(getFlag(2)) 
           getImage().setTransparency(100);
        return getFlag(2);   
    }
    
    private boolean isAssassin(GreepInfo friend) 
    {
        return friend.getFlags()[1];   
    }
    
    private void makeAssassin() 
    {
        if(getFlag(2)) return;
           
       // System.out.println("ASSASSIN Created");
        setFlag(2, true);
        setEnemyShipX(999);
        setEnemyShipY(999);
        getImage().setTransparency(100);
    }
    
    private void cancelAssassin() 
    {
        if(!getFlag(2)) return;
           
        setFlag(2, false);
        setEnemyShipX(0);
        setEnemyShipY(0);
        getImage().setTransparency(255);
        setMemory(0, 4);
        headHomeward(-1,-1);
        
        System.out.println("ASSASSIN cancelled: " + getMemory(0));
    }
    
    /**
     * An assassins primary objective is to locate the enemy ship and blast all greeps there. 
     * 
     * To do this, it first explores the map and tries to locate the enemy ship. 
     * If it hasn't found it within a certain time, it will remember its last position and
     * head back to the base to see if anyone else found it.
     */
    private void assassinAct() 
    {
        List<GreepInfo> visibleFriends = getVisibleFriends();
        int assassinFriends = 0;
        for(GreepInfo friend : visibleFriends) {
            if(isAssassin(friend)) {
                assassinFriends++;
            }
        }
        
        boolean atShip = (Math.abs(getEnemyShipX() - getX()) < 3) && (Math.abs(getEnemyShipY() - getY()) < 3); 
       
        if(assassinFriends > 1 ) {       
            // There are too many assassins, cancel this one
            
            kablam();
            cancelAssassin();
        }
        else if(enemyLocationKnown() && !atShip)
        {
           // System.out.println("Enemy known: " + getEnemyShipX() +"," + getEnemyShipY());
           /* List<GreepInfo> visibleOpponents = getVisibleOpponents();
            if( visibleOpponents.size() > 3 ) {
                kablam();
            }  */
            
            // if we didn't find this by spying we should move random instead
            headHomeward(getEnemyShipX(), getEnemyShipY());
        }
        else if(atOpponentShip() || atShip) {
            // In the above if, we need to compare the coordinates because atOpponentShip might 
            // report false if we are rotated in a different way from when we found the ship.
            
            if(!enemyLocationKnown()) {
                setEnemyShipX(getX());
                setEnemyShipY(getY());
              //  System.out.println("Found ship: " + getMemory(0));
            }
            
            List<GreepInfo> visibleOpponents = getVisibleOpponents();
            if( visibleOpponents.size() > 2 ) {
         //       System.out.println("ASSASSINATION!");
               // block();
                kablam();
            }
            else {
               // System.out.println("Block");
                block();
            }
        }
        else if(!enemyLocationKnown()){
            if(!tail()) {
             //   System.out.println("Lost track: " + getMemory(0));
                // we lost the opponent carrying a tomato, 
                //that probably means that it dropped the tomato at the ship,
                //so we should just continue in the current direction for a while.
                //MAYBE: We use the otherrwise unused y coordinate of the enemy location as a counter
                if(getEnemyShipY() ==  999) {
                   // System.out.println("start count");
                    //start count
                    setEnemyShipY(10);
                }
                if(getEnemyShipY() == 0) {
                    System.out.println("lost count");
                    // we lost track, cancel assassination mode
                    cancelAssassin();
                    return;
                }
                
                setEnemyShipY(getEnemyShipY() -1);
                move();
            } 
        } 
        else {
            // Since atOpponentShip depends on the greep's rotation it might end here because it is actually at the ship, but the atOpponentShip doesn't report it.
            System.out.println("*************************SHOULD NOT HAPPEN: " + getMemory(0) + "  atOpp: " + atOpponentShip() + "  x,y" + getX() + getY());
        }
    }
    
  /*  private void determineRole() 
    {
        if(isAssassin()) {
            //already have a role
            return;
        }
        if(atShip()) {            
            int [] shipData = getShipData();
            int assassinCount = shipData[ASSASSIN_COUNT_SLOT];
            if(assassinCount < ASSASSINS) {
                makeAssassin();
                shipData[ASSASSIN_COUNT_SLOT] = assassinCount + 1;
            }
        }
    }*/
    
    private boolean tail() {
            List<GreepInfo> visibleOpponents = getVisibleOpponents();
            for(GreepInfo opponent : visibleOpponents) {
                if(opponent.hasTomato()) {
                    //System.out.println("saw one");
                    int dir = getAngleTo(opponent.getX(), opponent.getY()) % 360;
                    int oppDir = opponent.getDirection() % 360;
                    //Make sure it is not negative
                    if(dir < 0) dir+=360;
                    if(oppDir < 0) dir+=360;
                    int diff = oppDir - dir;
                    
                    //Make sure it is not negative
                    if( diff < 0 ) diff*=-1;
                    
                    // Make sure the diff is the in the smaller half 
                    if(diff>180) diff = 360 - diff;
                    if(diff < 100) {                        
                        //System.out.println("following: " + opponent.getX() + " " + opponent.getY());
                        //lets follow it
                        headHomeward(opponent.getX(), opponent.getY());
                        makeAssassin();
                        //Indicate that we are tailing an enemy
                        setEnemyShipY(999);
                        return true;
                    }
                }                    
            }   
            return false;
    }
    
    /**
     * Do what a greep's gotta do.
     */
    public void act()
    {
        super.act();   // do not delete! leave as first statement in act().
       
//        System.out.println("mac: " + Integer.MAX_VALUE);
     //   determineRole();
        // We found the ship, make sure we are an assasin
       // if(atOpponentShip()) makeAssassin();
        
        if(isAssassin()) {
            assassinAct();
            return;
        }
        
        
        if (getMemory(0) == 4) {  
                if(atShip()) {
                    setMemory(0, 0);                  
                    checkForKnownPile();
                }
                else {
                    headHomeward(-1, -1);  
                    return;
                }
            }
        
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
                // We know where there are some tomatoes... we're heading back to
                // the ship, to update the data bank.
                if (atShip()) {
                    addKnownPile();
                    setMemory(0, 0);
                    checkForKnownPile();
                   // greepAct();
                }
                else {
                    headHomeward(-1, -1);    
                }
            }
            
            else if(tail()) {
                //tail takes care of it
            }
            else if (getMemory(0) == 2) {
                // Collect tomatoes from a known location
                if (distanceTo(getMemory(1), getMemory(2)) < 5 && visibleTomatoes() == null) {
                    // No more tomatoes in this pile
                    setMemory(0, 3);
                    moveNormally();
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
            else {
                moveNormally();    
            }
        }
    }
    
    /**
     * We're at the ship and we know about a pile of tomatoes.
     * Store that info in the ship's databank
     */
    private void addKnownPile()
    {
        int [] shipData = getShipData();
        if (shipData != null) {
            int numberKnown = shipData[0];
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[1 + 2*i];
                int ky = shipData[2 + 2*i];
                if (kx == getMemory(1) && ky == getMemory(2)) {
                    return; // already known    
                }
                else if (kx == -1) {
                    // An empty slot
                    shipData[1 + 2*i] = getMemory(1);
                    shipData[2 + 2*i] = getMemory(2);
                    // We may have created a duplicate, but that's no
                    // big deal.
                    return;
                }
            }
        
            shipData[1 + 2*numberKnown] = getMemory(1);
            shipData[2 + 2*numberKnown] = getMemory(2);
            numberKnown++;
            shipData[0] = numberKnown;
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
            int numberKnown = shipData[0];
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[1 + 2*i];
                int ky = shipData[2 + 2*i];
                if (kx == getMemory(1) && ky == getMemory(2)) {
                    shipData[1 + 2*i] = -1;
                    shipData[2 + 2*i] = -1;
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
            int numberKnown = shipData[0];
            int cx = -1;
            int cy = -1;
            int closestDist = 100000;
            for (int i = 0; i < numberKnown; i++) {
                int kx = shipData[1 + 2*i];
                int ky = shipData[2 + 2*i];
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
        if (atWater()) {
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
        move();
        if (atWater()) {
            int r = getRotation();
            setRotation (r + Greenfoot.getRandomNumber(2) * 180 - 90);
            move();
        }
        
        List<GreepInfo> visibleOpponents = getVisibleOpponents();
        List<GreepInfo> visibleFriends = getVisibleFriends();
        int activeOpponents = countActiveGreeps(visibleOpponents);
        int activeFriends = countActiveGreeps(visibleFriends) + 1;
        if (activeOpponents > activeFriends) {
            kablam();
        }
        
        move();
        checkFood();
        checkForKnownPile(); // if at ship
    }

    private int countActiveGreeps(List<GreepInfo> greeps)
    {
        int count= 0;
        for (Iterator<GreepInfo> i = greeps.iterator(); i.hasNext(); ) {
            GreepInfo info = i.next();
            if (info.getState() != MODE_FLIPPED) {
                count++;   
            }
        }
        return count;
    }
    
    /**
     * Is there any food here where we are? If so, try to load some!
     */
    public void checkFood()
    {
        int [] tomatoes = visibleTomatoes();
        if(tomatoes != null) {
            loadTomato();
            // Note: this attempts to load a tomato onto *another* Greep. It won't
            // do anything if we are alone here.
            
            setMemory(0, 1);
            setMemory(1, tomatoes[0]);
            setMemory(2, tomatoes[1]);
        }
    }


    /**
     * This method specifies the name of the author (for display on the result board).
     */
    public String getAuthorName()
    {
        return "Assassin";  // write your name here!
    }
    
    /** Override method from Greep */
    public void move()
    {
        // there's a 3% chance that we randomly turn a little off course
        if (randomChance(3)) {
            turn((Greenfoot.getRandomNumber(3) - 1) * 100);
        }
        
        super.move();
    }
    /**
     * Return the distance between this greep and an arbitrary point.
     */
    private int distanceTo(int x, int y)
    {
        int deltaX = getX() - x;
        int deltaY = getY() - y;
        return (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
    
    /**
     * Return the angle from the origin (0,0) to some point (x,y), in degrees
     */
    private int getAngleTo(int x, int y)
    {
        return (int)(180 * Math.atan2(y, x) / Math.PI);
    }
}