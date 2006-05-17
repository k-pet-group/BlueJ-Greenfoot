import greenfoot.World;
import greenfoot.Actor;

/**
 * A control knob for a plane. A ControlKnob can be dragged around
 * the world, and this will turn the associated plane.
 */
public class ControlKnob extends Actor
{
    private Plane plane;

    /**
     * Associate a plane with this control knob.
     */
    public void attachPlane(Plane plane)
    {
        this.plane = plane;
    }
    
    /**
     * This method is called when the knob is dragged through
     * the world.
     */
    public void setLocation(int x, int y)
    {
        if(plane != null) {
            plane.setRotation(x);
        }

        // ignore y location - keep the knob at the same vertical
        // position
        super.setLocation(x, 540);
    }

}