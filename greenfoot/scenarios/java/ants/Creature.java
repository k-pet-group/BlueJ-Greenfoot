import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

/**
 * A creature in a simulation. The creature has a known home. It can move and head towards or away from home.
 * 
 * Movement of the creature is arranged by storing a deltaX/deltaY pair: the offsets in the x/y direction
 * that the creature will move in the next step. The value for these is capped by the SPEED constant: 
 * the delta values will always be in the range [-SPEED..SPEED].
 * 
 * @author Michael Kölling
 * @version 1.0
 */
public class Creature  extends Actor
{
    /** The maximum movement speed of the ant. */
    private static final int SPEED = 3;

    /** Current movement. Defined as the offset in x and y direction moved in each step. */
    private int deltaX;
    private int deltaY;

    /** The home ant hill. */
    private AntHill home;

    /**
     * Crtae a new creature with neutral movement (movement speed is zero).
     */
    public Creature()
    {
        deltaX = 0;
        deltaY = 0;
    }
    
    /**
     * Set the home hill of this creature.
     */
    public void setHomeHill(AntHill homeHill)
    {
        home = homeHill;
    }
    
    /**
     * Get the home hill of this creature.
     */
    public AntHill getHomeHill()
    {
        return home;
    }
    
    /**
     * Walk around randomly (random direction and speed).
     */
    public void randomWalk()
    {
        if (randomChance(50)) {
            deltaX = adjustSpeed(deltaX);
            deltaY = adjustSpeed(deltaY);
        }
        walk();
    }

    /**
     * Try to walk home. Sometimes creatures get distracted or encounter small obstacles, so
     * they occasionally head in a different direction for a moment.
     */
    public void walkTowardsHome()
    {
        if(home == null) {
            //if we do not have a home, we can not go there.
            return;
        }
        if (randomChance(2)) {
            randomWalk();  // cannot always walk straight. 2% chance to turn off course.
        }
        else {
            headRoughlyTowards(home);
            walk();
        }
    }
    
    /**
     * Try to walk away from home. (Goes occasionally off course a little.)
     */
    public void walkAwayFromHome()
    {
        if(home == null) {
            //if we do not have a home, we can not head away from it.
            return;
        }
        if (randomChance(2)) {
            randomWalk();  // cannot always walk straight. 2% chance to turn off course.
        }
        else {
            headRoughlyTowards(home);   // first head towards home...
            deltaX = -deltaX;           // ...then turn 180 degrees
            deltaY = -deltaY;
            walk();
        }
    }

    /**
     * Adjust the walking direction to head towards the given co-ordinates.
     */
    public void headTowards(Actor target)
    {
        deltaX = capSpeed(target.getX() - getX());
        deltaY = capSpeed(target.getY() - getY());
    }
    
    /**
     * Walk forward in the current direction with the current speed. 
     * (Does not change direction or speed.)
     */
    public void walk()
    {
        setLocation(getX() + deltaX, getY() + deltaY);
        setRotation((int) (180 * Math.atan2(deltaY, deltaX) / Math.PI));
    }

    /**
     * Adjust the walking direction to head somewhat towards the given co-ordinates. This does not 
     * always head in the same direction. The heading is slightly random (but likely to be somewhat
     * towards the target) to make it look more natural.
     */
    private void headRoughlyTowards(Actor target)
    {
        int distanceX = Math.abs(getX() - target.getX());
        int distanceY = Math.abs(getY() - target.getY());
        boolean moveX = (distanceX > 0) && (Greenfoot.getRandomNumber(distanceX + distanceY) < distanceX);
        boolean moveY = (distanceY > 0) && (Greenfoot.getRandomNumber(distanceX + distanceY) < distanceY);

        deltaX = computeHomeDelta(moveX, getX(), target.getX());
        deltaY = computeHomeDelta(moveY, getY(), target.getY());
    }
    
    /**
     * Compute and return the direction (delta) that we should steer in when
     * we're on our way home.
     */
    private int computeHomeDelta(boolean move, int current, int home)
    {
        if (move) {
            if (current > home)
                return -SPEED;
            else
                return SPEED;
        }
        else
            return 0;
    }

    /**
     * Adjust the speed randomly (start moving, continue or slow down). The
     * speed returned is in the range [-SPEED .. SPEED].
     */
    private int adjustSpeed(int speed)
    {
        speed = speed + Greenfoot.getRandomNumber(2 * SPEED - 1) - SPEED + 1;
        return capSpeed(speed);
    }

    /**
     * Make sure the speed returned is in the range [-SPEED .. SPEED].
     */
    private int capSpeed(int speed)
    {
        if (speed < -SPEED)
            return -SPEED;
        else if (speed > SPEED)
            return SPEED;
        else
            return speed;
    }

    /**
     * Return 'true' in exactly 'percent' number of calls. That is: a call
     * randomChance(25) has a 25% chance to return true.
     */
    private boolean randomChance(int percent)
    {
        return Greenfoot.getRandomNumber(100) < percent;
    }

}
