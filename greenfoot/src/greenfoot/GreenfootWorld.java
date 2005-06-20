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
 * GreenfootWorld is the world that GreenfootObjects live in. It is a two-dimensional 
 * grid of cells. <br>
 * 
 * All GreenfootObject are associated with a GreenfootWorld and can get access to the 
 * world object. The size of cells can be specified at world creation time, and is 
 * constant after creation. Simple scenarios may use large cells that entirely contain
 * the representations of objects in a single cell. More elaborate scenarios may use
 * smaller cells (down to single pixel size) to achieve fine-grained placement and 
 * smoother animation.
 * 
 * The world background can be decorated with drawings or images.
 * 
 * @see greenfoot.GreenfootObject
 * @author Poul Henriksen
 * @author Michael Kolling
 * @version 0.2
 * @cvs-version $Id: GreenfootWorld.java 3461 2005-06-20 11:33:14Z mik $
 */
public class GreenfootWorld extends Observable
{
    // TODO: wrapping

    // TODO: Maybe we want to be able to force the world into "single-cell"
    // mode. With that we avoid accidently going into sprite mode if an object
    // is rotated and therefore get out of cell bounds

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

    /**
     * The order in which objects should be painted
     * 
     * @see #setPaintOrder(List)
     */
    private List classPaintOrder;

    /**
     * Construct a new world. The size of the world (in number of cells) and the size 
     * of each cell (in pixels) must be specified. The world may be specified to 'wrap'.
     * In a wrapping world, an object moving out of the world at one edge automatically
     * enters the world at the opposite edge.
     * 
     * @param worldWidth The width of the world (in cells).
     * @param worldHeight The height of the world (in cells).
     * @param cellSize Size of a cell in pixels.
     * @param wrap If true, the world wraps around at its edges.
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
     * Set a background image for the world. If the image size is larger than the 
     * world in pixels, it is clipped. If it is smaller than the world, it is tiled.
     * A pattern showing the cells can easily be shown by setting a background image
     * with a size equal to the cell size.
     * 
     * NOTE: Incomplete implementation. Tiling is currently not automatic and must be
     * specified in the GreenfootImage.
     * 
     * @see #setBackground(String)
     * @param image The image to be shown
     */
    final public void setBackground(GreenfootImage image)
    {
        backgroundImage = image;
        update();
    }

    /**
     * Set a background image for the world from an image file. Images of type 'jpeg',
     * 'gif' and 'png' are supported. If the image size is larger than the 
     * world in pixels, it is clipped. If it is smaller than the world, it is tiled.
     * A pattern showing the cells can easily be shown by setting a background image
     * with a size equal to the cell size.
     * 
     * NOTE: Incomplete implementation. NOT IMPLEMENTED.
     * 
     * @see #setBackground(GreenfootImage)
     * @param image The image to be shown
     */
    final public void setBackground(String filename)
    {
    }

    /**
     * Return the world's background image. The image may be used to draw onto the
     * world's background.
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
     * Return the width of the world (in number of cells).
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Return the height of the world (in number of cells).
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Return the size of a cell (in pixels).
     */
    public int getCellSize()
    {
        return cellSize;
    }

    /**
     * Add a GreenfootObject to the world (at the object's specified location).
     * 
     * @param object The new object to add.
     * @throws IndexOutOfBoundsException If the coordinates are outside the
     *             bounds of the world. Note that a wrapping world has not
     *             bounds.
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
     * Remove an object from the world.
     * 
     * @param object the object to remove
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
     * to avoid ConcurrentModificationException. <p>
     * 
     * The objects are returned in their paint order. The first object in the
     * List is the one painted first. The last object is the one painted on top 
     * of all other objects.<p>
     * 
     * If a class is specified as a parameter, only objects of that class (or its
     * subclasses) will be returned. <p>
     * 
     * NOTE: Incomplete implementation. the cls parameter is currently ignored.
     * 
     * @param cls Class of objects to look for ('null' will find all objects).
     * 
     * @return An unmodifiable list of objects.
     */
    public synchronized List getObjects(Class cls)
    {
        //TODO: implement cls parameter
        return Collections.unmodifiableList(objects);
    }

    /**
     * Check whether the world is wrapped around the edges.
     * 
     * @return True if we have a wrapped world, false otherwise.
     */
    public boolean isWrapped()
    {
        return wrapWorld;
    }

    /**
     * Sets the paint order of objects based on their class. <p>
     * 
     * Objects of the classes that are first in the list will be painted on top
     * of objects of the classes later in the list. Objects of classes not mentioned
     * in the list will be painted below objects explicitly named here.
     * 
     * Within the same class, or within classes that have unspecified paint order,
     * the last object that has moved into a cell is painted on top of others.
     * 
     * NOTE: NOT IMPLEMENTED.
     * 
     * @param classOrder List of class objects.
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
     * Return all objects at a given cell. <p>
     * 
     * An object is defined to be at that cell if its graphical representation overlaps
     * with the cell at any point.
     * 
     * NOTE: Incomplete implementation. May be faulty at the edges when the world is wrapped.
     * 
     * @param x  X-coordinate of the cell to be checked.
     * @param y  Y-coordinate of the cell to be checked.
     * @param cls Class of objects to look return ('null' will return all objects).
     */
    public List getObjectsAt(int x, int y, Class cls)
    {
        return collisionChecker.getObjectsAt(x, y, cls);
    }

    /**
     * Return all the objects that intersect the given object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param go A GreenfootObject in the world
     * @param cls Class of objects to look for (null or Object.class will find
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
     * @param x Center of the cirle
     * @param y Center of the cirle
     * @param r Radius of the cirle
     * @param cls Class of objects to look for (null or Object.class will find
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
     * NOTE: class argument does not work. It returns all types of objects.
     * 
     * @param x Location
     * @param y Location
     * @param distance Distance in which to look for other objects
     * @param diag Is the distance also diagonal?
     * @param cls Class of objects to look for (null or Object.class will find
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
     * @param x1 Location of point 1
     * @param y1 Location of point 1
     * @param x2 Location of point 2
     * @param y2 Location of point 2
     * @param cls Class of objects to look for (null or Object.class will find
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