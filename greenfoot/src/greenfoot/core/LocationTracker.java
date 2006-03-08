package greenfoot.core;

import greenfoot.util.Location;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

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
public class LocationTracker
{
    private static LocationTracker instance;
    private Location location = new Location();
    private Component component;
    
    static {
        instance();
    }

    private LocationTracker()
    {
        AWTEventListener listener = new AWTEventListener() {

            public void eventDispatched(AWTEvent event)
            {
                MouseEvent me = (MouseEvent) event;
                Component source = me.getComponent();
                me = SwingUtilities.convertMouseEvent(source, me, component);
                LocationTracker.this.move(me);
            } 
            
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    
    }

    public synchronized static LocationTracker instance()
    {
        if (instance == null) {
            instance = new LocationTracker();
        }
        return instance;
    }

    private void setLocation(int x, int y)
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

    private void move(MouseEvent e)
    {
        Point p = e.getPoint(); 
        int x = p.x;
        int y = p.y;
		LocationTracker.instance().setLocation(x, y);
    }

    public void setComponent(Component worldCanvas)
    {
        this.component = worldCanvas;
    }

}
