import greenfoot.World;
import greenfoot.Actor;

public class Plane extends Actor
{
    private static final double SPEED = 3.0;

    public void addedToWorld(World world) {
        createControlKnob();
    }

    public void act()
    {
        move();
    }
    
    public void createControlKnob() 
    {
        ControlKnob ctrl = new ControlKnob();
        getWorld().addObject(ctrl, 100, 540);
        ctrl.attachPlane(this);
    }
    
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