import greenfoot.World;
import greenfoot.Actor;

/**
 * A Plane which flies above the field. The direction of the plane
 * is controlled via a knob which is visible in the world. The knob
 * will be created automatically when the plane is added to the
 * world.
 * 
 * @author Michael Kolling
 * @version $Id$
 */
public class Plane extends Actor
{
    private static final double SPEED = 3.0;

    /**
     * This method will be called automatically when the plane
     * is added to the world.
     */
    public void addedToWorld(World world)
    {
        createControlKnob();
    }

    /**
     * Act is called repeatedly when the simulation is running.
     * All a plane does is move forward a little.
     */
    public void act()
    {
        move();
    }
    
    /**
     * Create the control knob which is used to control the
     * direction of the plane.
     */
    public void createControlKnob() 
    {
        ControlKnob ctrl = new ControlKnob();
        getWorld().addObject(ctrl, 100, 540);
        ctrl.attachPlane(this);
    }
    
    /**
     * Move the plane forward in its current direction.
     */
    public void move() {
        double angle = Math.toRadians(getRotation());
        int x = (int) Math.round(getX() + Math.cos(angle) * SPEED);
        int y = (int) Math.round(getY() + Math.sin(angle) * SPEED);
        if(x >= getWorld().getWidth()) {
            x = getWorld().getWidth() - 1;
        }
        if(x < 0) {
            x = 0;
        }
        if(y >= getWorld().getHeight()) {
            y = getWorld().getHeight() - 1;
        }
        if(y < 0) {
            y = 0;
        }
        setLocation(x, y);
    }
                      

}