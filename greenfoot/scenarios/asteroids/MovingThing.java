import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

/**
 * A thing that can move around with a certain speed.
 * 
 * @author Poul Henriksen
 */
public abstract class MovingThing extends Actor
{
    private Vector speed = new Vector();
    
    private double x = 0;
    private double y = 0;
    
    public MovingThing()
    {
    }
    
    /**
     * Create new thing initialised with given speed.
     */
    public MovingThing(Vector speed)
    {
        this.speed = speed;
    }
    
    /**
     * Move forward one step.
     */
    public void move() {
        x = x + speed.getX();
        y = y + speed.getY();
        if(x >= getWorld().getWidth()) {
            x = 0;
        }
        if(x < 0) {
            x = getWorld().getWidth() - 1;
        }
        if(y >= getWorld().getHeight()) {
            y = 0;
        }
        if(y < 0) {
            y = getWorld().getHeight() - 1;
        }
        setLocation(x, y);
    }
    
    public void setLocation(double x, double y) {
        this.x = x;
        this.y = y;
        super.setLocation((int) x, (int) y);
    }
    
    public void setLocation(int x, int y) {
        setLocation((double) x, (double) y);
    }

    /**
     * Increase the speed with the given vector.
     */
    public void increaseSpeed(Vector s) {
        speed.add(s);
    }
    /**
     * Return the current speed.
     */
    public Vector getSpeed() {
        return speed;
    }
}