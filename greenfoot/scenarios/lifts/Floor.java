import greenfoot.World;
import greenfoot.Actor;

import java.util.Random;
import java.util.List;
import java.util.Iterator;

public class Floor extends Actor
{
    private static final Random random = Building.getRandomizer();

    private int floorNumber;
    private Button button;
    
    public Floor()
    {
        this(0, null);
    }

    public Floor(int floorNumber, Button liftButton)
    {
        this.floorNumber = floorNumber;
        setImage("floor.jpg");
        button = liftButton;
    }

    public void setLocation(int x, int y)
    {
        super.setLocation(x, y);
        button.setLocation(x+78, y+8);
    }
    
    /**
     * Do the regular simulation action. For a floor, that is: produce a new
     * person every now and then.
     */
    public void act()
    {
        if(random.nextFloat() < 0.005) {
            Person p = new Person(getX() + random.nextInt(68),
                                  getY() + 8,
                                  this,
                                  (Building)getWorld());
            getWorld().addObject(p);
        }
    }
    
    /**
     * Return this floor's number.
     */
    public int getFloorNumber()
    {
        return floorNumber;
    }
    
    /**
     * Press a button to call a lift to this floor.
     */
    public void pressButton(int direction)
    {
        button.press(direction);
    }

    /**
     * Press a button to call a lift to this floor.
     */
    public void clearButton(int direction)
    {
        button.clear(direction);
    }

}