package greenfoot.core;

import greenfoot.util.Location;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * This singleton keeps track of where to add an object. This location is used
 * to initialise the object with a location in the constructor. <br>
 * 
 * 
 * A location will always be returned. 
 * 
 * @author Poul Henriksen
 * 
 */
public class LocationTracker implements MouseMotionListener
{
    private static LocationTracker instance;
    private Location location = new Location();

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
    }

    /**
     * 
     * @return null if no location is available
     */
    public Location getLocation()
    {
        return location;
      
    }

    public void mouseDragged(MouseEvent e)
    {
        move(e);
    }

    public void mouseMoved(MouseEvent e)
    {
        move(e);
    }

    private void move(MouseEvent e)
    {
        Point p = e.getPoint(); 
        int x = (int) p.getX();
        int y = (int) p.getY();
        LocationTracker.instance().setLocation(x, y);
    }

   

}
