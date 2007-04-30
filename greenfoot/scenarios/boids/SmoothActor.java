import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * An actor that are using doubles to represent it's location. If velocity 
 * and/or accelarartion is specified those will be used to move the actor.
 * 
 * @author Poul Henriksen 
 * @version 2.0
 */
public class SmoothActor extends Actor
{
    private Vector location = new Vector();
    private Vector velocity = new Vector();
    private Vector acc = new Vector();
    
    /** 
     * Optional minimal speed for this actor. 
     * If positive, the speed will never be less than the specified value. 
     */
    private double minSpeed = -1;
    
    /**
     * Optional maximal speed for this actor. 
     * If positive, the speed will never be larger than the specified value. 
     */
    private double maxSpeed = -1; 
    
    /** Before applying the velocity, the speed is divided by this value. */
    private double speedDivider;
    
    public void act() {
        move();
    }        
    
    /**
     * Move the actor by applying current accelaration and velocity.according to the current velocity.
     */
    protected void move() 
    {      
        velocity.add(acc);
        limitSpeed();
        location.add(getVelocity().divide(speedDivider));        
        setRotation((int) velocity.getDirection());
        setLocation(location);   
    }
     
    /**
     * Limits the speed to be within specified boundaries.
     */
    private void limitSpeed() {
        if(minSpeed >= 0 && velocity.getLength() < minSpeed) {
           velocity.setLength(minSpeed);
        }
        if(maxSpeed >= 0 && velocity.getLength() > maxSpeed) {
            velocity.setLength(maxSpeed);
        }
    }
    
    /**
     * Gets the accelaration.
     */
    protected Vector getAccelaration()
    {
        return acc.copy();
    }
    
    /**
     * Sets the accelaration.
     */
    protected void setAccelaration(Vector newAcc) 
    {        
        acc.setX(newAcc.getX());
        acc.setY(newAcc.getY());
    }
       
    /**
     * Gets the velocity.
     */
    protected Vector getVelocity()
    {
        return velocity.copy();
    }
    
    /**
     * Sets the velocity.
     */
    protected void setVelocity(Vector newVel) 
    {
        velocity.setX(newVel.getX());
        velocity.setY(newVel.getY());
        setRotation((int) velocity.getDirection());
    } 
    
    /**
     * Gets the location.
     */
    protected Vector getLocation() 
    {
        return location.copy();
    }
    
    /**
     * Sets the location.
     */
    protected void setLocation(Vector v) 
    {
        location.setX(v.getX());
        location.setY(v.getY());
        super.setLocation((int) v.getX(), (int) v.getY());
    }
    
    /**
     * Sets the location.
     */
    public void setLocation(int x, int y) 
    {        
        location.setX(x);
        location.setY(y);
        super.setLocation(x, y);
    }
    
    /** 
     * Optional minimal speed for this actor. 
     * The speed will never be less than the specified value. 
     */
    protected void setMinimumSpeed(double speed) 
    {
        minSpeed = speed;
    }
   
    /**
     * Optional maximal speed for this actor. 
     * The speed will never be larger than the specified value. 
     */
    protected void setMaximumSpeed(double speed) 
    {
        maxSpeed = speed;
    }
    
    /**
     * Before applying the velocity, the speed is divided by this value. 
     */
    protected void setSpeedDivider(double d) 
    {
        speedDivider = d;
    }
    
}
