package greenfoot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;


/**
 * This class represents the object world, which is a 2 dimensional grid of
 * cells. The world can be populated with GreenfootObjects. <br>
 * 
 * The most normal use of the world is using a cell size of one, which means
 * that it is using pixel resolution. If another cell size is used you should be
 * aware that all methods that has something to do with location and size in
 * GreenfootObject and GreenfootWorld is using that resolution.
 * 
 * TODO: wrapping
 * 
 * @see greenfoot.GreenfootObject
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootWorld.java 3297 2005-01-20 03:52:20Z davmac $
 */
public class GreenfootWorld extends Observable
{
    private Map[][] world;
    private List objects = new ArrayList();

    /** The size of the cell in pixels.*/
    private int cellSize = 1;

    /**
     * Map from classes to the size of the largest object of that class - used
     * for collision checks. <br>
     * 
     * TODO find a better way to do this. It is not fail safe if the object size
     * is changed after obejcts has been added to the world Map from classes to
     * sizes. Used for collision
     */
    private Map objectMaxSizes = new HashMap();

    private static Collection emptyCollection = new Vector();

    /** Image painted in the background. */
    private Image backgroundImage;
    private boolean tiledBackground;

    private int delay = 500;


    /**
     * Create a new world with the given size.
     * 
     * @param worldWidth
     *            The width of the world. 
     * @param worldHeight
     *            The height of the world.
     */
    public GreenfootWorld(int worldWidth, int worldHeight)
    {
        setSize(worldWidth, worldHeight);
    }

    /**
     * This constructor should be used if a scenario is created that should use a grid.
     * It creates a new world with the given size .
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
        setSize(worldWidth, worldHeight);
        this.cellSize = cellSize;
    }

    /**
     * Sets the backgroundimage of the world.
     * 
     * @see #setTiledBackground(boolean)
     * @see #setBackgroundImage(String)
     * @param image
     *            The image
     */
    final public void setBackground(Image image)
    {
        backgroundImage = image;
        update();
    }   

    /**
     * Gets the background image
     * 
     * @return The background image
     */
    public Image getBackground()
    {
        if(backgroundImage == null) {
            backgroundImage =new Image(getWidthInPixels(), getHeightInPixels());
        }
        return backgroundImage;
    }

    /**
     * Gets the width of the world.     
     */
    public int getWidth()
    {
        return world.length;
    }

    /**
     * Gets the height of the world. 
     */
    public int getHeight()
    {
        return world[0].length;
    }

    /**
     * Get the cell size. If no cell size has been specified via the
     * constructor, it defaults to 1.
     */
    int getCellSize()
    {
        return cellSize;
    }

    /**
     * Sets the size of the world. <br>
     * This will remove all objects from the world. TODO Maybe it shouldn't!
     */
    private void setSize(int width, int height)
    {
        world = new Map[width][height];
        update();
    }

    /**
     * Adds a GreenfootObject to the world. <br>
     * If the coordinates of the object is outside the worlds bounds, an
     * exception is thrown.
     * 
     * @param thing
     *            The new object to add.
     */
    public synchronized void addObject(GreenfootObject thing)
        throws ArrayIndexOutOfBoundsException
    {
        if (thing.getX() >= getWidth()) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() >= getHeight()) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }
        if (thing.getX() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }

        if (!objects.contains(thing)) {

            HashMap map = (HashMap) world[thing.getX()][thing.getY()];
            if (map == null) {
                map = new HashMap();
                world[thing.getX()][thing.getY()] = map;
            }
            Class clazz = thing.getClass();
            List list = (List) map.get(clazz);
            if (list == null) {
                list = new ArrayList();
                map.put(clazz, list);
            }
            list.add(thing);
            thing.setWorld(this);
            objects.add(thing);

            updateMaxSize(thing);

            update();
        }
    }

    /**
     * Updates the map of maximum object sizes with the given object (if
     * necessary).
     *  
     */
    private void updateMaxSize(GreenfootObject thing)
    {
        Class clazz = thing.getClass();
        Integer maxSize = (Integer) objectMaxSizes.get(clazz);
        int height = thing.getHeight();
        int width = thing.getWidth();
        int diag = (int) Math.sqrt(width * width + height * height);

        int newSizeInCells = toCellCeil(diag);

        if (maxSize == null || maxSize.intValue() < newSizeInCells) {
            objectMaxSizes.put(clazz, new Integer(newSizeInCells));
        }
    }

    /**
     * Returns all the objects with the exact location (x,y)
     */
    private Collection getObjectsWithLocation(int x, int y)
    {
        Map map = (Map) world[x][y];
        if (map != null) {
            Collection values = map.values();
            Collection list = new ArrayList();
            for (Iterator iter = values.iterator(); iter.hasNext();) {
                List element = (List) iter.next();
                list.addAll(element);
            }
            return list;
        }
        else {
            return emptyCollection;
        }
    }

    /**
     * Gets all the objects of class cls (and subclasses) that contains the
     * given location.
     * 
     * @see GreenfootObject#contains(int, int)
     */
    public Collection getObjectsAt(int x, int y, Class cls)
    {
        List objectsThere = new ArrayList();
        Collection objectsAtCell = getObjectsAt(x, y);
        for (Iterator iter = objectsAtCell.iterator(); iter.hasNext();) {
            GreenfootObject go = (GreenfootObject) iter.next();
            if (cls.isInstance(go)) {
                objectsThere.add(go);
            }
        }
        return objectsThere;
    }

    /**
     * Returns all objects at that contains the given location.
     * 
     * @see GreenfootObject#contains(int, int)
     */
    public Collection getObjectsAt(int x, int y)
    {
        int maxSize = getMaxSize();

        List objectsThere = new ArrayList();
        int xStart = (x - maxSize) + 1;
        int yStart = (y - maxSize) + 1;
        if (xStart < 0) {
            xStart = 0;
        }
        if (yStart < 0) {
            yStart = 0;
        }
        if (x >= getWidth()) {
            x = getWidth() - 1;
        }
        if (y >= getHeight()) {
            y = getHeight() - 1;
        }

        for (int xi = xStart; xi <= x; xi++) {
            for (int yi = yStart; yi <= y; yi++) {
                Map map = world[xi][yi];
                if (map != null) {
                    Collection list = getObjectsWithLocation(xi, yi);
                    for (Iterator iter = Collections.unmodifiableCollection(list).iterator(); iter.hasNext();) {
                        GreenfootObject go = (GreenfootObject) iter.next();
                        if (go.contains(x - xi, y - yi)) {
                            objectsThere.add(go);
                        }
                    }
                }
            }
        }
        return objectsThere;
    }
    
    /**
     * @return
     */
    private int getMaxSize()
    {
        int maxSize = 0;
        Collection maxSizes = objectMaxSizes.values();
        for (Iterator iter = maxSizes.iterator(); iter.hasNext();) {
            Integer element = (Integer) iter.next();
            if (element.intValue() > maxSize) {
                maxSize = element.intValue();
            }
        }
        return maxSize;
    }

    /**
     * Gets all objects within the given radius and of the given class (or
     * subclass).
     * 
     * 
     * The center of the circle is considered to be at the center of the cell.
     * Objects which have the center within the circle is considered to be in range.
     * 
     * @param x
     *            The x-coordinate of the center
     * @param y
     *            The y-coordinate of the center
     * @param r
     *            The radius
     * @param cls
     *            Only objects of this class (or subclasses) are returned
     * @return
     */
    public Collection getObjectsInRange(int x, int y, double r, Class cls)
    {
        Iterator objects = getObjects();

        List neighbours = new ArrayList();
        while (objects.hasNext()) {
            Object o = objects.next();
            if (cls.isInstance(o) && o != this) {
                GreenfootObject go = (GreenfootObject) o;
                if (distance(x, y, go) <= r) {
                    neighbours.add(go);
                }
            }
        }
        return neighbours;       
    }

  
    /**
     * Returns the shortest distance from the cell (center of cell ) to the
     * center of the greenfoot object.
     * 
     * @param x
     *            x-coordinate of the cell
     * @param y
     *            y-coordinate of the cell
     * @param go
     * @return
     */
    private double distance(int x, int y, GreenfootObject go)
    {
        double gx = go.getX() + (go.getWidth() / 2.) / cellSize;
        double gy = go.getY() + (go.getHeight() / 2.) / cellSize;
        double dx = gx - (x + 0.5);
        double dy = gy - (y + 0.5);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * When we have a pixel coordinate, and the cell size > 1, we can use this
     * method which translates into cell coordinates. 
     * 
     */
    Collection getObjectsAtPixel(int x, int y)
    {
        return getObjectsAt(toCellFloor(x), toCellFloor(y));
    }

    /**
     * When we have a pixel coordinate, and the cell size > 1, we can use this
     * method which translates into cell coordinates
     * 
     */
    Collection getObjectsAtPixel(int x, int y, Class cls)
    {
        return getObjectsAt(toCellFloor(x), toCellFloor(y), cls);
    }

    /**
     * Removes the object from the world.
     * 
     * @param object
     *            the object to remove
     */
    public synchronized void removeObject(GreenfootObject object)
    {
        Map map = world[object.getX()][object.getY()];
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
                object.setWorld(null);
            }
        }
        objects.remove(object);
        update();
    }

    /**
     * Provides an Iterator to all the things in the world.
     *  
     */
    public synchronized Iterator getObjects()
    {
        //TODO: Make sure that the iterator returns things in the correct
        // paint-order (whatever that is)
        List c = new ArrayList();
        c.addAll(objects);
        return c.iterator();
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

    /**
     * Updates the location of the object in the world.
     * 
     * 
     * @param object
     *            The object which should be updated
     * @param oldX
     *            The old X location of the object
     * @param oldY
     *            The old Y location of the object
     */
    void updateLocation(GreenfootObject object, int oldX, int oldY)
    {
        Map map = world[oldX][oldY];
        Class clazz = object.getClass();
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
                if (list.isEmpty()) {
                    map.remove(object.getClass());
                    if (map.isEmpty())
                        world[oldX][oldY] = null;
                }
            }
        }

        map = world[object.getX()][object.getY()];
        if (map == null) {
            map = new HashMap();
            world[object.getX()][object.getY()] = map;
        }
        List list = (List) map.get(clazz);
        if (list == null) {
            list = new ArrayList();
            map.put(clazz, list);
        }
        list.add(object);
        update();
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

}