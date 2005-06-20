package greenfoot;

import greenfoot.util.Location;

/**
 * This singleton keeps track of where to add an object. This location is used
 * to initialise the object with a location in the constructor. <br>
 * 
 * A location can only be retrieved one time. This is done so it is possible to
 * create objects that did not have a location tracked for it, and then make
 * some desicion on where to place it, instead of just placing it where the last
 * obejct was tracked to.
 * 
 * @author Poul Henriksen
 * 
 */
public class LocationTracker
{
    private static LocationTracker instance;
    private Location location = new Location();
    private boolean hasLocation = false;

    private LocationTracker()
    {

    }

    public synchronized static LocationTracker instance()
    {
        if (instance == null) {
            instance = new LocationTracker();
        }
        return instance;
    }

    public void setLocation(int x, int y)
    {
        location.setX(x);
        location.setY(y);
        hasLocation = true;
    }

    /**
     * 
     * @return null if no location is available
     */
    public Location getLocation()
    {
        if (hasLocation) {
            reset();
            return location;
        }
        else {
            return null;
        }
    }

    public boolean hasLocation()
    {
        return hasLocation;
    }

    public void reset()
    {
        hasLocation = false;
    }

}
