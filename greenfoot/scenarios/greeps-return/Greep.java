import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A Greep is the base class for all alien beings in this scenario. It
 * provides the basic abilities of greeps in this world.
 * 
 * @author Michael Kolling
 * @author Davin McCall
 * @author Poul Henriksen
 * @version 2.0
 */
public abstract class Greep extends Actor
{
    private static final double WALKING_SPEED = 5.0;
    private static final int TIME_TO_SPIT = 10;
    private static final int KNOCK_OUT_TIME = 70;
    private static final int VISION_RANGE = 70;
    
    /** Indicate whether we have a tomato with us */
    private boolean carryingTomato = false; 
    
    /** The greep's home ship */
    private Ship ship;

    /** General state */
    private boolean moved = false;
    private boolean atWater = false;
    private boolean moveWasBlocked = false;
    private int mode = MODE_WALKING;
    private int timeToKablam = 0;
    
    private static final int MODE_WALKING = 0;
    private static final int MODE_BLOCKING = 1;
    private static final int MODE_FLIPPED = 2;

    /** if flipped, will slide for some way */
    int slideSpeed = 0;
    int spinSpeed = 0;
    int slideDirection = 0;
    
    /** General purpose memory */
    private int[] memory = new int[4];
    private boolean[] flags = new boolean[2];
        
    /**
     * Create a greep.
     */
    public Greep(Ship ship)
    {         
        this.ship = ship;
        setRotation(Greenfoot.getRandomNumber(360));
        setImage(getCurrentImage());  
    }      
    
    /**
     * Greenfoot's standard act method, which can be reimplemented in subclasses.
     */
    public void act()
    {        
        moved = false;
            
        if (mode == MODE_FLIPPED) {
            if (slideSpeed != 0 || spinSpeed != 0) {
                if (slideSpeed != 0) {
                    if(slideSpeed < 20) {
                        // Drop the tomato after we have been sliding for a while
                        leaveTomato();
                    }
                    double angle = Math.toRadians(slideDirection);
                    int speed = slideSpeed / 10;
                    int x = (int) Math.round(getX() + Math.cos(angle) * speed);
                    int y = (int) Math.round(getY() + Math.sin(angle) * speed);
                    if (canMoveTo(x,y)) {
                        setLocation(x,y);    
                    }
                    else {
                        slideDirection = slideDirection + 80 + Greenfoot.getRandomNumber(20);   
                    }
                    slideSpeed = Math.max((int)(slideSpeed * .95 - 1), 0);
                }
                if (spinSpeed != 0) {
                    setRotation(getRotation() + spinSpeed);
                    spinSpeed--;
                }
            }
            else
            if (Greenfoot.getRandomNumber(KNOCK_OUT_TIME) == 0) {
                mode = MODE_WALKING;
                setImage(getCurrentImage());
            }
        }
        else if(mode == MODE_BLOCKING) {
            turn(3);
            
        }
        else {
            if (timeToKablam > 0) {
                timeToKablam--;
            }
        }
    }       
    
    /**
     * This method specifies the name of the greeps (for display on the result board).
     * Try to keep the name short so that it displays nicely on the result board.
     */
    abstract public String getName();    
    
    /**
     * Turn 'angle' degrees towards the right (clockwise).
     */
    protected final void turn(int angle)
    {
        if(mode == MODE_FLIPPED) return;
        setRotation(getRotation() + angle);
    }    

    /**
     * Turn in the direction facing the home ship.
     */
    protected final void turnHome()
    {
        if(mode == MODE_FLIPPED) return;
        turnTowards(ship.getX(), ship.getY());
    }
    
    /**
     * Turn to face an arbitrary point on the map.
     */
    protected final void turnTowards(int x, int y)
    {
        if(mode == MODE_FLIPPED) return;
        int deltaX = x - getX();
        int deltaY = y - getY();
        setRotation(getAngleTo(deltaX, deltaY));
    }
    
    /**
     * True if we are at our space ship.
     */
    protected final boolean atShip()
    {
        return ship == getOneIntersectingObject(Ship.class);
    }
    
    /**
     * Get the ship's data bank array (1000 ints).
     * This can only be done if the greep is at the ship.
     * The data inside the array can be freely manipulated.
     */
    protected final int[] getShipData()
    {
        if (atShip()) {
            return ship.getData();
        }
        else {
            return null;
        }
    }
    
    /**
     * Try and move forward in the current direction. If we are blocked (by water, or an
     * opponent greep who is blocking), we won't move; atWater()/moveWasBlocked() can be
     * used to check for this, and in that case, it is allowed to change direction and try
     * move()ing again.
     */
    protected final void move()
    {
        if(moved)   // can move only once per 'act' round
            return;
                    
        if (mode == MODE_FLIPPED)
            return;
        
        if(mode != MODE_WALKING) {
            mode = MODE_WALKING; // can't be blocking if we're moving
            setImage(getCurrentImage());
        }
        double angle = Math.toRadians( getRotation() );
        int x = (int) Math.round(getX() + Math.cos(angle) * WALKING_SPEED);
        int y = (int) Math.round(getY() + Math.sin(angle) * WALKING_SPEED);
        
        if (canMoveTo(x,y)) {
            setLocation(x, y);
            moved = true;
        }
    }
    
    /**
     * Return true if we have just seen water in front of us. 
     * The edge of the map is also water.
     */
    protected final boolean atWater()
    {
        return atWater;
    }
    
    /**
     * Return true if we have been blocked by an opponent greep.
     */
    protected final boolean moveWasBlocked()
    {
        return moveWasBlocked;
    }
    
    /**
     * Load a tomato onto *another* greep. This works only if there is another greep
     * and a tomato pile present, otherwise this method does nothing.
     */
    protected final void loadTomato()
    {
        if(mode == MODE_FLIPPED) return;
        // check whether there's a tomato pile here
        TomatoPile tomatoes = getTomatoes();
        
        // check whether there's another friendly greep here
        List friendlies = getObjectsInRange(10, this.getClass());

        if(! friendlies.isEmpty() && tomatoes != null) {
            Greep greep = (Greep) friendlies.iterator().next();
            if(greep.ship == this.ship && !greep.carryingTomato()) {
                tomatoes.takeOne();
                greep.carryTomato();
            }
        }
    }    

    /**
     * Check whether we are carrying a tomato.
     */
    protected final boolean carryingTomato()
    {
        return carryingTomato;
    }
    
    /**
     * Drop the tomato we are carrying. If we are at the ship, it is counted.
     * If not, it's just gone...
     */
    protected final void dropTomato()
    {
        if(!carryingTomato)
            return;
            
        if(atShip()) {
            ship.storeTomato();
        }
        carryingTomato = false;
        setImage(getCurrentImage());
    }   
    
    /**
     * If we can see tomatoes, this will return them. Otherwise it returnes null.
     * <p>
     * You are only allowed to call getX() and getY() on the returned tomato pile. 
     */
    protected final TomatoPile getTomatoes()
    {
        TomatoPile tomatoes = (TomatoPile) getOneIntersectingObject(TomatoPile.class);
        if (tomatoes != null) {
            if (distanceTo(tomatoes.getX(), tomatoes.getY()) < 25 && !tomatoes.isEmpty()) {
                return tomatoes;
            }
        }
        return null;
    }        
    
    /**
     * Return the number of visible opponent greeps which are not knocked out by a stink bomb.
     * 
     * @param withTomatoes If true, only count the greeps that are carrying a tomato.
     */
    protected final int getNumberOfOpponents(boolean withTomatoes)
    {
        int count = 0;
        List l = getObjectsInRange(VISION_RANGE, Greep.class);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Greep greep = (Greep) i.next();
            if (greep.ship != ship) {
                // It's an enemy greep
                if (greep.mode != MODE_FLIPPED && (!withTomatoes || greep.carryingTomato()))                    
                    count++;
            }
        }        
        return count;
    }
    
    /**
     * Return the number of visible friendly greeps which are not knocked out by a stink bomb.
     * 
     * @param withTomatoes If true, only count the greeps that are carrying a tomato.
     */
    protected final int getNumberOfFriends(boolean withTomatoes)
    {
        int count = 0;
        List l = getObjectsInRange(VISION_RANGE, this.getClass());
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Greep greep = (Greep) i.next();
            if (greep.ship == ship) {
                // It's a friendly greep
                if( greep.mode != MODE_FLIPPED && (!withTomatoes || greep.carryingTomato()))                    
                    count++;
            }
        }        
        return count;
    }
    
    /**
     * Returns a friendly greep, if there is one at our current location.
     * Returns null otherwise.
     * <p>
     * You are only allowed to access the memory and flags of the friend.
     */
    protected final Greep getFriend()
    {
        List greeps =  getObjectsInRange(30, this.getClass());
        for(Object obj : greeps) {
            Greep greep = (Greep) obj;
            if(greep.ship == this.ship) {
                return greep;
            }
        }       
        return null;
    }    
    /**
     * Return 'true' in exactly 'percent' number of calls. That is: a call
     * randomChance(25) has a 25% chance to return true.
     */
    protected final boolean randomChance(int percent)
    {
        return Greenfoot.getRandomNumber(100) < percent;
    }       
    
    /**
     * Store something in the greep's memory. There are four memory slots, numbered
     * 0 to 3, each can hold an int value.
     */
    protected final void setMemory(int slot, int val)
    {
        memory[slot] = val;
    }    
    
    /**
     * Retrieve a previously stored value.
     * 
     * Other friendly greeps are allowed to call this mehtod.
     */
    public final int getMemory(int slot)
    {
        return memory[slot];
    }

    /**
     * Store a user defined boolean value (a "flag"). Two flags are available, 
     * i.e. 'flagNo' may be 1 or 2.
     */
    protected final void setFlag(int flagNo, boolean val)
    {
        if(flagNo < 1 || flagNo > 2)
            throw new IllegalArgumentException("flag number must be either 1 or 2");
        else 
            flags[flagNo-1] = val;
    }    
    
    /**
     * Retrieve the value of a flag. 'flagNo' can be 1 or 2.
     * 
     * Other friendly greeps are allowed to call this mehtod.
     */
    public final boolean getFlag(int flagNo)
    {
        if(flagNo < 1 || flagNo > 2)
            throw new IllegalArgumentException("flag number must be either 1 or 2");
        else 
            return flags[flagNo-1];
    }
    
    /**
     * Return true if this greep is in "blocking" mode - it has hunkered down to
     * prevent opponent greeps from passing (while allowing friendly greeps through).
     */
    protected final boolean isBlocking()
    {
        return mode == MODE_BLOCKING;
    }
    
    /**
     * Block opponent greeps from passing our current location. This is only effective if
     * we haven't moved (can't move and block in the same turn).
     */
    protected final void block()
    {
        if (moved)
            return;
        
        if (mode == MODE_FLIPPED)
            return;
        
        mode = MODE_BLOCKING;            
        setImage(getCurrentImage());
    }
    
    /**
     * Release a stink bomb. All greeps within a small radius will be knocked out for
     * a small period of time.
     */
    protected final void kablam()
    {
        if (mode == MODE_FLIPPED) {
            return;   
        }
        
        if (timeToKablam > 0) {
            return;
        }
        
        timeToKablam = 20; // prevent total carnage
        
        List l = getObjectsInRange(100, Greep.class);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Greep greep = (Greep) i.next();
            greep.keelOver();
            greep.slideSpeed = 20 + Greenfoot.getRandomNumber(120);
            greep.spinSpeed = Greenfoot.getRandomNumber(70) + 10;
            greep.slideDirection = Greenfoot.getRandomNumber(360);
        }
        
        keelOver();
        
        slideSpeed = 20 + Greenfoot.getRandomNumber(120);
        spinSpeed = Greenfoot.getRandomNumber(70) + 10;
        slideDirection = Greenfoot.getRandomNumber(360);
        getWorld().addObject(new Smoke(5, isTeamTwo()), getX(), getY());
    }    

    private boolean canMoveTo(int x, int y)
    {
        atWater = false;
        moveWasBlocked = false;
        
        if(x >= getWorld().getWidth()) {
            atWater = true;
        }
        if(x < 0) {
            atWater = true;
        }
        if(y >= getWorld().getHeight()) {
            atWater = true;
        }
        if(y < 0) {
            atWater = true;
        }
        
        if(! atWater && ((Earth)getWorld()).isWater(x, y)) {
            atWater = true;
        }
        
        moveWasBlocked = false;
        if (! atWater) {
            List greepsAtOldLoc = getWorld().getObjectsAt(getX(), getY(), Greep.class);
            List otherGreeps = getWorld().getObjectsAt(x,y,Greep.class);
            for (Iterator i = otherGreeps.iterator(); i.hasNext(); ) {
                Greep otherGreep = (Greep) i.next();
                if (otherGreep.ship != this.ship) {
                    if (otherGreep.isBlocking() && ! greepsAtOldLoc.contains(otherGreep)) {
                        moveWasBlocked = true;
                        break;
                    }
                }
            }
        }
        
        return !(atWater || moveWasBlocked);
    }
    
    /**
     * Receive a tomato and carry it.
     */
    private void carryTomato()
    {
        carryingTomato = true;
        setImage(getCurrentImage());
    }
        
    /**
     * Leave the tomato we are carrying. 
     * It will put the tomato on the ground - forming a pile of one tomato.
     */
    private void leaveTomato()
    {
        if(!carryingTomato)
            return;
            
        getWorld().addObject(new TomatoPile(1), getX(), getY());
        carryingTomato = false;
        setImage(getCurrentImage());
    }    
    
    /**
     * Make this greep flip.
     */
    private void keelOver()
    {
        mode = MODE_FLIPPED;
        setImage(getCurrentImage());
    }
    
     /**
     * This method specifies the image we want displayed at any time.
     */
    private String getCurrentImage()
    {
        String base;
        
        if(isTeamTwo()) {
            base = "greep-purple";
        }
        else {
            base = "greep-green";
        }
        
        if (mode == MODE_FLIPPED) {
            return base + "-flipped.png";   
        }
        else if(mode == MODE_BLOCKING) {
            return base + "-blocking.png";
        }
        else if(carryingTomato()) {
            return base + "-with-food.png";
        }
        else {
            return base + ".png";
        }
    }
    
    /**
     * Return true if this is team 2, false if it is team 1.
     */
    private boolean isTeamTwo()
    {
        if(ship == null) {
            return false;
        }
        else {
            return ship.isTeamTwo();
        }
    }    
        
    /**
     * Return the angle from the origin (0,0) to some point (x,y), in degrees
     */
    private int getAngleTo(int x, int y)
    {
        return (int)(180 * Math.atan2(y, x) / Math.PI);
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
}
