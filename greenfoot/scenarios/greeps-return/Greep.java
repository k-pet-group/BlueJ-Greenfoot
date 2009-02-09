import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * A Creature is the base class for all alien beings in this scenario. It
 * provides the basic abilities of creatures in this world.
 * 
 * @author Michael Kolling
 * @version 1.0
 */
public abstract class Greep extends Actor
{
    private static final double WALKING_SPEED = 5.0;
    private static final int TIME_TO_SPIT = 10;
    private static final int KNOCK_OUT_TIME = 70;
    
    /** Indicate whether we have a tomato with us */
    private boolean carryingTomato = false; 
    
    /** The creature's home ship */
    private Ship ship;
    private boolean canSeeShip; // can this greep currently see the ship? (cache value for atShip())

    /** General state */
    private boolean moved = false;
    private boolean atWater = false;
    private boolean moveWasBlocked = false;
    private int timeToSpit = 0;
    private int mode = MODE_WALKING;
    private int timeToKablam = 0;
    
    public static final int MODE_WALKING = 0;
    public static final int MODE_BLOCKING = 1;
    public static final int MODE_FLIPPED = 2; // Flipped over

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
    public Greep()
    { 
    }      
    
    /**
     * Set which ship this greep belongs to.
     */
    final public void setShip(Ship ship)
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
        canSeeShip = false;

        if(timeToSpit > 0)
            timeToSpit--;
            
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
        else {
            if (timeToKablam > 0) {
                timeToKablam--;
            }
        }
    }
   
    
    
    /**
     * Turn 'angle' degrees towards the right (clockwise).
     */
    public void turn(int angle)
    {
        if(mode == MODE_FLIPPED) return;
        setRotation(getRotation() + angle);
    }
    

    /**
     * Turn in the direction facing the home ship.
     */
    public void turnHome()
    {
        if(mode == MODE_FLIPPED) return;
        turnTowards(ship.getX(), ship.getY());
    }
    
    /**
     * Turn to face an arbitrary point on the map.
     */
    public void turnTowards(int x, int y)
    {
        if(mode == MODE_FLIPPED) return;
        int deltaX = x - getX();
        int deltaY = y - getY();
        setRotation(getAngleTo(deltaX, deltaY));
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
    
     /**
     * True if we are at the opponent's space ship.
     */
    public final boolean atOpponentShip()
    {
        boolean canSeeOpponentShip = false;
        Ship ship = (Ship) getOneIntersectingObject(Ship.class);
        canSeeOpponentShip = (ship != null && ship != this.ship);
        return canSeeOpponentShip;
    }
    
    /**
     * True if we are at our space ship.
     */
    public final boolean atShip()
    {
        if (! canSeeShip) {
            Ship ship = (Ship) getOneIntersectingObject(Ship.class);
            canSeeShip = (ship == this.ship);
        }
        return canSeeShip;
    }
    
    /**
     * Get the ship's data bank array (1000 ints).
     * This can only be done if the greep is at the ship.
     * The data inside the array can be freely manipulated.
     */
    public int[] getShipData()
    {
        if (atShip()) {
            return ship.getData();
        }
        else {
            return null;
        }
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
                if (otherGreep.getClass() != this.getClass()) {
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
     * Try and move forward in the current direction. If we are blocked (by water, or an
     * opponent greep who is blocking), we won't move; atWater()/moveWasBlocked() can be
     * used to check for this, and in that case, it is allowed to change direction and try
     * move()ing again.
     */
    public void move()
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
     */
    public boolean atWater()
    {
        return atWater;
    }
    
    /**
     * Return true if we have seen water or anything else that blocks our path
     * in front of us.
     */
    public boolean moveWasBlocked()
    {
        return atWater || moveWasBlocked;
    }
    
    /**
     * Load a tomato onto *another* creature. This works only if there is another creature
     * and a tomato pile present, otherwise this method does nothing.
     */
    public final void loadTomato()
    {
        if(mode == MODE_FLIPPED) return;
        // check whether there's a tomato pile here
        TomatoPile tomatoes = (TomatoPile) getOneIntersectingObject(TomatoPile.class);
        
        // check whether there's another friendly greep here
        List friendlies = getObjectsInRange(10, this.getClass());

        if(! friendlies.isEmpty() && tomatoes != null) {
            Greep greep = (Greep) friendlies.iterator().next();
            if(!greep.carryingTomato()) {
                tomatoes.takeOne();
                greep.carryTomato();
            }
        }
    }
    

    /**
     * Check whether we are carrying a tomato.
     */
    public final boolean carryingTomato()
    {
        return carryingTomato;
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
     * Leave the tomato we are carrying. 
     * It will put the tomato on the ground - forming a pile of one tomato.
     */
    protected final void leaveTomato()
    {
        if(!carryingTomato)
            return;
            
        getWorld().addObject(new TomatoPile(1), getX(), getY());
        carryingTomato = false;
        setImage(getCurrentImage());
    }
    
    
    /**
     * Test if we are close to one of the edges of the world. Return true if we are.
     */
    public boolean atWorldEdge()
    {
        if(getX() < 3 || getX() > getWorld().getWidth() - 3)
            return true;
        if(getY() < 3 || getY() > getWorld().getHeight() - 3)
            return true;
        else
            return false;
    }

    
    /**
     * Return 'true' in exactly 'percent' number of calls. That is: a call
     * randomChance(25) has a 25% chance to return true.
     */
    protected boolean randomChance(int percent)
    {
        return Greenfoot.getRandomNumber(100) < percent;
    }
       
    
    /**
     * Store something in the greep's memory. There are four memory slots, numbered
     * 0 to 3, each can hold an int value.
     */
    public void setMemory(int slot, int val)
    {
        memory[slot] = val;
    }
    
    
    /**
     * Retrieve a previously stored value.
     */
    public int getMemory(int slot)
    {
        return memory[slot];
    }


    /**
     * Store a user defined boolean value (a "flag"). Two flags are available, 
     * i.e. 'flagNo' may be 1 or 2.
     */
    public void setFlag(int flagNo, boolean val)
    {
        if(flagNo < 1 || flagNo > 2)
            throw new IllegalArgumentException("flag number must be either 1 or 2");
        else 
            flags[flagNo-1] = val;
    }
    
    
    /**
     * Retrieve the value of a flag. 'flagNo' can be 1 or 2.
     */
    public boolean getFlag(int flagNo)
    {
        if(flagNo < 1 || flagNo > 2)
            throw new IllegalArgumentException("flag number must be either 1 or 2");
        else 
            return flags[flagNo-1];
    }
    
     /**
     * This method specifies the image we want displayed at any time.
     */
    private final String getCurrentImage()
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
    private final boolean isTeamTwo()
    {
        if(ship == null) {
            return false;
        }
        else {
            return ship.isTeamTwo();
        }
    }    
    
    /**
     * Return true if this greep is in "blocking" mode - it has hunkered down to
     * prevent opponent greeps from passing (while allowing friendly greeps through).
     */
    public final boolean isBlocking()
    {
        return mode == MODE_BLOCKING;
    }
    
    /**
     * If we can see tomatoes, this will return their co-ordinates as 2-dimensional array
     * [x,y]; otherwise it returnes null.
     */
    public int[] visibleTomatoes()
    {
        TomatoPile tomatoes = (TomatoPile) getOneIntersectingObject(TomatoPile.class);
        if (tomatoes != null) {
            if (distanceTo(tomatoes.getX(), tomatoes.getY()) < 25) {
                int[] rval = {tomatoes.getX(), tomatoes.getY()};
                return rval;
            }
        }
        return null;
    }
    
    /**
     * Get a list of visible opponent greeps.
     */
    public List<GreepInfo> getVisibleOpponents()
    {
        List<GreepInfo> rlist = new ArrayList<GreepInfo>(); 
        
        List l = getIntersectingObjects(Greep.class);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Greep greep = (Greep) i.next();
            if (greep.ship != ship) {
                // It's an enemy greep
                GreepInfo details = new GreepInfo(greep, null, null, greep.mode);
                rlist.add(details);
            }
        }
        
        return rlist;
    }
    
    /**
     * Get a list of visible friendly greeps.
     */
    public List<GreepInfo> getVisibleFriends()
    {
        List<GreepInfo> rlist = new ArrayList<GreepInfo>(); 
        
        List l = getIntersectingObjects(Greep.class);
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Greep greep = (Greep) i.next();
            if (greep.ship == ship) {
                int [] memory = (int[])greep.memory.clone();
                boolean [] flags = (boolean[])greep.flags.clone();
                GreepInfo details = new GreepInfo(greep, memory, flags, greep.mode);
                rlist.add(details);
            }
        }
        
        return rlist;
    }
    
    /**
     * Block opponent greeps from passing our current location. This is only effective if
     * we haven't moved (can't move and block in the same turn).
     */
    public void block()
    {
        if (moved)
            return;
        
        if (mode == MODE_FLIPPED)
            return;
        
        mode = MODE_BLOCKING;            
        setImage(getCurrentImage());
    }
    
    final private void keelOver()
    {
        mode = MODE_FLIPPED;
        setImage(getCurrentImage());
    }
    
    /**
     * Release a stink bomb. All greeps within a small radius will be knocked out for
     * a small period of time.
     */
    public void kablam()
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

    abstract public String getAuthorName();    
    
}
