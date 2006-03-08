package greenfoot.collision;

import greenfoot.GreenfootObject;

import java.util.List;

/**
 * Interface for an implementation of a particular collision checker algorithm.
 * 
 * @author Poul Henriksen
 */
public interface CollisionChecker
{
    /**
     * This method is called when the collision checker should initialise.
     * 
     * @param width
     *            Width of the world
     * @param height
     *            Height of the world
     * @param wrap
     *            Whether the world wraps around the edges
     */
    public void initialize(int width, int height, boolean wrap);

    /**
     * Called when an object is added into the world
     */
    public void addObject(GreenfootObject go);

    /**
     * Called when an object is removed from the world
     */
    public void removeObject(GreenfootObject object);

    /**
     * Called when an object has changed its location in the world.
     * 
     * @param oldX
     *            Old location
     * @param oldY
     *            Old location
     */
    public void updateObjectLocation(GreenfootObject object, int oldX, int oldY);

    /**
     * Called when an object has changed its size in the world.
     */
    public void updateObjectSize(GreenfootObject object);

    /**
     * Returns all objects that intersects the given location.
     * 
     * @param x
     *            Location
     * @param y
     *            Location
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getObjectsAt(int x, int y, Class cls);

    /**
     * Returns all the objects that intersects the given object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param go
     *            A GreenfootObject in the world
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getIntersectingObjects(GreenfootObject go, Class cls);

    /**
     * Returns all objects with the logical location within the specified
     * circle. In other words an object A is within the range of an object B if
     * the distance between the center of the two objects is less thatn r.
     * 
     * @param x
     *            Center of the cirle
     * @param y
     *            Center of the cirle
     * @param r
     *            Radius of the cirle
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getObjectsInRange(int x, int y, int r, Class cls);

    /**
     * Returns the neighbours to the given location. This method only looks at
     * the logical location and not the extent of objects. Hence it is most
     * useful in scenarios where objects only span one cell.
     * 
     * @param x
     *            Location
     * @param y
     *            Location
     * @param distance
     *            Distance in which to look for other objects
     * @param diag
     *            Is the distance also diagonal?
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     * @return A collection of all neighbours found
     */
    public List getNeighbours(int x, int y, int distance, boolean diag, Class cls);

    /**
     * Return all objects that intersect a straight line from this object at
     * a specified angle. The angle is clockwise relative to the current 
     * rotation of the object.  <br>
     * 
     * If the world is wrapped, the line will wrap around the edges.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @param angle The angle relative to current rotation of the object.
     * @param length How far we want to look (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    public List getObjectsInDirection(int x, int y, int angle, int length, Class cls);
}