import greenfoot.World;
import greenfoot.Actor;

public class ControlKnob extends Actor
{
    private Plane plane;

    public void attachPlane(Plane plane) {
        this.plane = plane;
    }
    
    public void setLocation(int x, int y) {
        if(plane != null) {
            plane.setRotation(x);
        }
        //ignore y location
        super.setLocation(x, 540);
    }

}