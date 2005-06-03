package greenfoot;

import greenfoot.collision.CollisionChecker;
import greenfoot.collision.GridCollisionChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Observable;

/**
 * This class represents the object world, which is a 2 dimensional grid of
 * cells. The world can be populated with GreenfootObjects. <br>
 * 
 * The most normal use of the world is using a cell size of one, which means
 * that it is using pixel resolution. If another cell size is used you should be
 * aware that all methods that has something to do with location and size in
 * GreenfootObject and GreenfootWorld is using that resolution.
 * 
 * 
 * @see greenfoot.GreenfootObject
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootWorld.java 3405 2005-06-03 15:10:56Z polle $
 */
public class GreenfootWorld extends Observable
{
    // TODO: wrapping

    // TODO: Maybe we want to be able to force the world into "single-cell"
    // mode. With that we avoid accidently going into sprite mode if an object
    // is rotated and therefor get out of cell bounds

    private CollisionChecker collisionChecker = new GridCollisionChecker();

    /** All the objects currently in the world */
    private List objects = new ArrayList();

    /** The size of the cell in pixels. */
    private int cellSize = 1;

    /** Size of the world */
    private int width;
    private int height;

    /** Whether the world should wrap around the edges */
    private boolean wrapWorld;

    /** Image painted in the background. */
    private GreenfootImage backgroundImage;

    /** Whether the background image should be tiled */
    private boolean tiledBackground;

    /**
     * The order in which objects should be painted
     * 
     * @see #setPaintOrder(List)
     */
    private List classPaintOrder;

    /**
     * This constructor should be used if a scenario is created that should use
     * a grid. It creates a new world with the given size.
     * 
     * @see GreenfootWorld
     * @param worldWidth
     *            The width of the world (in cells).
     * @param worldHeight
     *            The height of the world (in cells).
     * @param cellSize
     *            Size of a cell in pixels
     * @param wrap
     *            Whether the world should wrap around the edges
     * 
     */
    public GreenfootWorld(int worldWidth, int worldHeight, int cellSize, boolean wrap)
    {
        initialize(worldWidth, worldHeight, cellSize, wrap);
    }

    /**
     * Sets the size of the world. <br>
     * 
     * This will remove all objects from the world. TODO Maybe it shouldn't!
     * 
     */
    private void initialize(int width, int height, int cellSize, boolean wrap)
    {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.wrapWorld = wrap;
        collisionChecker.initialize(width, height, wrap);
        update();
    }

    /**
     * Sets the backgroundimage of the world.
     * 
     * @see #setTiledBackground(boolean)
     * @see #setBackgroundImage(String)
     * @param image
     *            The image
     */
    final public void setBackground(GreenfootImage image)
    {
        backgroundImage = image;
        update();
    }

    /**
     * Gets the background image.
     * 
     * @return The background image
     */
    public GreenfootImage getBackground()
    {
        if (backgroundImage == null) {
            backgroundImage = new GreenfootImage(getWidthInPixels(), getHeightInPixels());
        }
        return backgroundImage;
    }

    /**
     * Gets the width of the world. This is the number of cells horisontally.
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Gets the height of the world. This is the number of cells vertically.
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Get the cell size. If no cell size has been specified via the
     * constructor, the cell size is 1.
     */
    public int getCellSize()
    {
        return cellSize;
    }

    /**
     * Adds a GreenfootObject to the world.
     * 
     * @param object
     *            The new object to add.
     * @throws IndexOutOfBoundsException
     *             If the coordinates are outside the bounds of the world. Note
     *             that a wrapping world has not bounds.
     */

    public synchronized void addObject(GreenfootObject object)
        throws IndexOutOfBoundsException
    {
        // TODO bad performance when using a List for the objects. But if we
        // want to have paint order, a List is necessary.
        if (objects.contains(object)) {
            return;
        }
        checkAndWrapLocation(object);
        object.setWorld(this);
        collisionChecker.addObject(object);
        objects.add(object);
        update();
    }

    /**
     * Removes the object from the world.
     * 
     * @param object
     *            the object to remove
     */
    public synchronized void removeObject(GreenfootObject object)
    {
        collisionChecker.removeObject(object);
        objects.remove(object);
        update();
    }

    /**
     * Get all the objects in the world.<br>
     * 
     * If iterating through these objects, you should synchronize on this world
     * to avoid ConcurrentModificationException. <br>
     * 
     * The order in which they are returned, is the paint order. The first
     * object in the List should be painted first. This means that the lasat
     * object will always be painted on top of all the other objects.<br>
     * 
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     * 
     * @return An unmodifiable list of objects.
     */
    public synchronized List getObjects(Class cls)
    {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Whether the world is wrapped around the edges.
     * 
     */
    public boolean isWrapped()
    {
        return wrapWorld;
    }

    /**
     * Sets the paint order of objects based on their class. <br>
     * 
     * Objects of the classes that are first in the list will be painted on top
     * of objects of the classes later in the list. <br>
     * 
     * If there are objcts in the world that is not specified in this paint
     * order, the top object will be the one that was updated last (change of
     * location)
     * 
     * @param classOrder
     *            List of classes.
     * 
     */
    public void setPaintOrder(List classPaintOrder)
    {
        this.classPaintOrder = classPaintOrder;
    }

    // =================================================
    //
    // COLLISION STUFF
    //
    // =================================================

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
    public List getObjectsAt(int x, int y, Class cls)
    {
        return collisionChecker.getObjectsAt(x, y, cls);
    }

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
    List getIntersectingObjects(GreenfootObject go, Class cls)
    {
        return collisionChecker.getIntersectingObjects(go, cls);
    }

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
    List getObjectsInRange(int x, int y, int r, Class cls)
    {
        return collisionChecker.getObjectsInRange(x, y, r, cls);
    }

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
    List getNeighbours(int x, int y, int distance, boolean diag, Class cls)
    {
        return collisionChecker.getNeighbours(x, y, distance, diag, cls);
    }

    /**
     * Get all objects that lie on the line between the two points.
     * 
     * @param x1
     *            Location of point 1
     * @param y1
     *            Location of point 1
     * @param x2
     *            Location of point 2
     * @param y2
     *            Location of point 2
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    List getObjectsAtLine(int x1, int y1, int x2, int y2, Class cls)
    {
        return collisionChecker.getObjectsAtLine(x1, y1, x2, y2, cls);
    }

    // =================================================
    //
    // PROTECTED MEHTHODS
    //
    // used by other classes internally in greenfoot
    // =================================================

    /**
     * Get the height of the world in pixels.
     */
    int getHeightInPixels()
    {
        return getHeight() * getCellSize();
    }

    /**
     * Get the width of the world in pixels.
     */
    int getWidthInPixels()
    {
        return getWidth() * getCellSize();
    }

    int toCellCeil(int i)
    {
        return (int) Math.ceil((double) i / cellSize);
    }

    int toCellFloor(int i)
    {
        return (int) Math.floor((double) i / cellSize);
    }

    /**
     * Refreshes the world. <br>
     * Should be called to see the changes after painting on the graphics
     * 
     * @see #getCanvas()
     * @see #getCanvas(int, int)
     */
    final void update()
    {
        setChanged();
        notifyObservers();
    }

    Collection getObjectsAtPixel(int x, int y)
    {
        return collisionChecker.getObjectsAt(toCellFloor(x), toCellFloor(y), null);
    }

    void updateObjectLocation(GreenfootObject object, int oldX, int oldY)
    {
        checkAndWrapLocation(object);
        collisionChecker.updateObjectLocation(object, oldX, oldY);
    }

    void updateObjectSize(GreenfootObject object)
    {
        collisionChecker.updateObjectSize(object);
    }

    // =================================================
    //
    // PRIVATE MEHTHODS
    //
    // =================================================

    /**
     * Throws an exception if the object's location is out of the bounds of the
     * world. <br>
     * If the world is wrapping around the edges, it will convert the location
     * to be within the actual size of the world. <br>
     * This method only checks the logical location.
     * 
     */
    private void checkAndWrapLocation(GreenfootObject object)
        throws IndexOutOfBoundsException
    {
        if (!wrapWorld) {
            ensureWithinBounds(object);
        }
        else {
            wrapLocation(object);
        }
    }

    /**
     * Makes sure that the location of the object is wrapped into a location
     * within the worlds bounds.
     * 
     * @param object
     */
    private void wrapLocation(GreenfootObject object)
    {
        int x = object.getX();
        int y = object.getY();

        if (x >= getWidth()) {
            x = wrap(x, getWidth());
        }
        if (y >= getHeight()) {
            y = wrap(y, getHeight());
        }
        if (x < 0) {
            x = wrap(x, getWidth());
        }
        if (object.getY() < 0) {
            y = wrap(y, getHeight());
        }
        object.x = x;
        object.y = y;
    }

    /**
     * wraps the number x with the width
     */
    int wrap(int x, int width)
    {
        int remainder = x % width;
        if (remainder < 0) {
            return width + remainder;
        }
        else {
            return remainder;
        }
    }

    /**
     * Methods that throws an exception if the location of the world is out of
     * bounds.
     * 
     * @param object
     * @return
     * @throws IndexOutOfBoundsException
     */
    private void ensureWithinBounds(GreenfootObject object)
        throws IndexOutOfBoundsException
    {
        if (object.getX() >= getWidth()) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + object.getX() + ". It must be smaller than: "
                    + getWidth());
        }
        if (object.getY() >= getHeight()) {
            throw new IndexOutOfBoundsException("The y-coordinate is: " + +object.getY()
                    + ". It must be smaller than: " + getHeight());
        }
        if (object.getX() < 0) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + +object.getX()
                    + ". It must be larger than: 0");
        }
        if (object.getY() < 0) {
            throw new IndexOutOfBoundsException("The y-coordinate is: " + +object.getY()
                    + ". It must be larger than: 0");
        }
    }

}