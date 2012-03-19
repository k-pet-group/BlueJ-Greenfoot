import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A variation of an actor that maintains precise location (using doubles for the co-ordinates
 * instead of ints). It also maintains a current movement vector that is independent of
 * the current rotation of the actor.
 * 
 * @author Poul Henriksen
 * @author Michael Kolling
 * @author Neil Brown
 * 
 * @version 3.0
 */
public abstract class SmoothMover extends Actor
{
    private double movementX;
    private double movementY;
    private double exactX;
    private double exactY;
    
    /**
     * Create a SmoothMover with an empty movement vector (standing still).
     */
    public SmoothMover()
    {
        this(0, 0);
    }
    
    /**
     * Create new SmoothMover initialised with given velocity.
     */
    public SmoothMover(double movementX, double movementY)
    {
        this.movementX = movementX;
        this.movementY = movementY;
    }
    
    /**
     * Move in the current movement direction.
     */
    public void moveVector() 
    {
        setLocation(exactX + movementX, exactY + movementY);
    }
    
    /*
     * Move forward by the specified distance.
     * (Overrides the method in Actor).
     */
    public void move(int distance)
    {
        double radians = Math.toRadians(rotation);
        double dx = Math.round(Math.cos(radians) * distance);
        double dy = Math.round(Math.sin(radians) * distance);
        setLocation(exactX + dx, exactY + dy);
    }
    
    /**
     * Set the location using exact coordinates.
     */
    public void setLocation(double x, double y) 
    {
        exactX = x;
        exactY = y;
        super.setLocation((int) (x + 0.5), (int) (y + 0.5));
    }
    
    /*
     * Set the location using integer coordinates.
     * (Overrides the method in Actor.)
     */
    public void setLocation(int x, int y) 
    {
        exactX = x;
        exactY = y;
        super.setLocation(x, y);
    }

    /**
     * Return the exact x-coordinate (as a double).
     */
    public double getExactX() 
    {
        return exactX;
    }

    /**
     * Return the exact y-coordinate (as a double).
     */
    public double getExactY() 
    {
        return exactY;
    }

    /**
     * Increase the speed with the given vector.
     */
    public void addForce(double forceX, double forceY) 
    {
        movementX += forceX;
        movementY += forceY;
    }
    
    /**
     * Accelerate the speed of this mover by the given factor. (Factors &lt; 1 will
     * decelerate.)
     */
    public void accelerate(double factor)
    {
        movementX *= factor;
        movementY *= factor;
        if (getSpeed()  < 0.15) {
            movementX = 0;
            movementY = 0;
        }
    }
    
    /**
     * Return the speed of the current movement vector.
     */
    public double getSpeed()
    {
        return Math.hypot(movementX, movementY);
    }
    
    /**
     * Increase the speed with the given vector.
     */
    public void stop()
    {
        movementX = 0;
        movementY = 0;
    }
    
    /**
     * Return the current speed in the X dimension.
     */
    public double getSpeedX() 
    {
        return movementX;
    }
    
    /**
     * Return the current speed in the Y dimension.
     */
    public double getSpeedY() 
    {
        return movementX;
    }
}
