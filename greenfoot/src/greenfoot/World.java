package greenfoot;


import greenfoot.collision.CollisionChecker;
import greenfoot.collision.ibsp.IBSPColChecker;
import greenfoot.core.WorldHandler;
import greenfoot.util.Version;

import java.awt.Color;
import java.awt.Graphics;
import java.lang.reflect.Field;
import java.util.*;




/**
 * World is the world that Actors live in. It is a two-dimensional grid of
 * cells. <br>
 * 
 * All Actor are associated with a World and can get access to the world object.
 * The size of cells can be specified at world creation time, and is constant
 * after creation. Simple scenarios may use large cells that entirely contain
 * the representations of objects in a single cell. More elaborate scenarios may
 * use smaller cells (down to single pixel size) to achieve fine-grained
 * placement and smoother animation.
 * 
 * The world background can be decorated with drawings or images.
 * 
 * @see greenfoot.Actor
 * @author Poul Henriksen
 * @author Michael Kolling
 * @version 1.2.0
 * @cvs-version $Id$
 */
public abstract class World
{    

    /** Version number of the Greenfoot API. Must load the class dynamically to have it work with standalone execution*/
    static Version VERSION;
    static {
        try{
            Class bootCls = Class.forName("bluej.Boot");
            Field field = bootCls.getField("GREENFOOT_API_VERSION");
            String versionStr = (String) field.get(null);
            VERSION = new Version(versionStr);
        }
        catch (ClassNotFoundException e) {
            VERSION = new Version("0");
            //It's fine - running in standalone.
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    // private CollisionChecker collisionChecker = new GridCollisionChecker();
    // private CollisionChecker collisionChecker = new BVHInsChecker();
    private CollisionChecker collisionChecker = new IBSPColChecker();
    
    //{
    //    collisionChecker = new CollisionProfiler(collisionChecker);
    //}

    /** All the objects currently in the world */
    //private List objects = Collections.synchronizedList(new ArrayList());
    private TreeActorSet objects = new TreeActorSet();

    /** The size of the cell in pixels. */
    private int cellSize = 1;

    /** Size of the world */
    private int width;
    private int height;

    /** Image painted in the background. */
    private GreenfootImage backgroundImage;
    
    /** Should the image be tiled to fill the entire background */
    private boolean tiled = true;    

    /**
     * Construct a new world. The size of the world (in number of cells) and the
     * size of each cell (in pixels) must be specified.
     * 
     * @param worldWidth The width of the world (in cells).
     * @param worldHeight The height of the world (in cells).
     * @param cellSize Size of a cell in pixels.
     * 
     */
    public World(int worldWidth, int worldHeight, int cellSize)
    {
        initialize(worldWidth, worldHeight, cellSize);
    }

    /**
     * Sets the size of the world. <br>
     * 
     * This will remove all objects from the world. TODO Maybe it shouldn't!
     * 
     */
    private void initialize(int width, int height, int cellSize)
    {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.tiled = true;
        collisionChecker.initialize(width, height, cellSize, false);
    }

    /**
     * Set a background image for the world. If the image size is larger than
     * the world in pixels, it is clipped. If it is smaller than the world, it
     * is tiled unless specifically stated to do otherwise (see setTiled()). A
     * pattern showing the cells can easily be shown by setting a background
     * image with a size equal to the cell size.
     * 
     * @see #setBackground(String)
     * @see #setTiled(boolean)
     * @param image The image to be shown
     */
    final public void setBackground(GreenfootImage image)
    {
        backgroundImage = image;
    }

    /**
     * Set a background image for the world from an image file. Images of type
     * 'jpeg', 'gif' and 'png' are supported. If the image size is larger than
     * the world in pixels, it is clipped. If it is smaller than the world, it
     * is tiled unless specifically stated to do otherwise (see setTiled()). A
     * pattern showing the cells can easily be shown by setting a background
     * image with a size equal to the cell size.
     * 
     * @see #setBackground(GreenfootImage)
     * @see #setTiled(boolean)
     * @param filename The file holding the image to be shown
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    final public void setBackground(String filename) throws IllegalArgumentException
    {
        GreenfootImage bg = new GreenfootImage(filename);
        setBackground(bg);
    }

    /**
     * Return the world's background image. The image may be used to draw onto
     * the world's background.
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
     * Return the color at the center of the cell. To paint a color, you need to
     * get the background image for the world and paint on that.
     * 
     * @see #getBackground()
     * @throws IndexOutOfBoundsException If the location is not within the world
     *             bounds. If there is no background image at the location it
     *             will return Color.WHITE.
     */
    public Color getColorAt(int x, int y) {
        ensureWithinXBounds(x);
        ensureWithinYBounds(y);       
        
        int xPixel = (int) Math.floor(getCellCenter(x));
        int yPixel = (int) Math.floor(getCellCenter(y));        
        
        // Take tiling into account
        if(isTiled()) {
            xPixel = xPixel % backgroundImage.getWidth();
            yPixel = yPixel % backgroundImage.getHeight();
        }
        
        // TODO if it is not tiled, and outside, what should be returned? BGcolor? Null?
        if(xPixel >= backgroundImage.getWidth()) {
            return Color.WHITE;
        }
        if(yPixel >= backgroundImage.getHeight()) {
            return Color.WHITE;
        }        
        
        return backgroundImage.getColorAt(xPixel, yPixel);
    }

    /**
     * If set to true, the background image will be tiled to fill out the entire
     * background of the world.
     * 
     * @param tiled Whether it should tile the image or not.
     */
    public void setTiled(boolean tiled)
    {
        this.tiled = tiled;
    }

    /**
     * Returns true if the world is tiled.
     * 
     * @return Whether the image is tilled.
     * @see #setTiled(boolean)
     */
    public boolean isTiled()
    {
        return tiled;
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
     * Sets the paint order of objects in the world according to their class.
     * Objects of the first class given will be painted last, so they will
     * appear on top of all other objects; objects of the second class given
     * will be painted second last, and so on.
     * <p>
     * Objects not belonging to any of the given classes will appear underneath
     * all other objects.
     * 
     * @param classes  The classes in desired paint order
     */
    public void setPaintOrder(Class ... classes)
    {
        if (classes == null) {
            // Allow null as an argument, to specify no paint order
            classes = new Class[0];
        }
        objects.setClassOrder(classes);
    }
    
    /**
     * Add an Actor to the world (at the object's specified location).
     * 
     * @param object The new object to add.
     * @throws IndexOutOfBoundsException If the coordinates are outside the
     *             bounds of the world.
     */
    public synchronized void addObject(Actor object, int x, int y)
        throws IndexOutOfBoundsException
    {
        if (! objects.add(object)) {
            // Actor is already in the world
            return;
        }

        object.addToWorld(x, y, this);

        collisionChecker.addObject(object);
        objects.add(object);

        object.addedToWorld(this);
    }

    /**
     * Remove an object from the world.
     * 
     * @param object the object to remove
     */
    public synchronized void removeObject(Actor object)
    {
        if (objects.remove(object)) {
            // we only want to remove it once.
            collisionChecker.removeObject(object);
        }
        object.setWorld(null);
    }

    /**
     * Remove a list of objects from the world.
     * 
     * @param objects A list of Actors to remove.
     */
    public synchronized void removeObjects(Collection objects)
    {
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Actor actor = (Actor) iter.next();
            removeObject(actor);
        }
    }

    /**
     * Get all the objects in the world.<br>
     * 
     * If iterating through these objects, you should synchronize on this world
     * to avoid ConcurrentModificationException.
     * <p>
     * 
     * The objects are returned in their paint order. The first object in the
     * List is the one painted first. The last object is the one painted on top
     * of all other objects.
     * <p>
     * 
     * If a class is specified as a parameter, only objects of that class (or
     * its subclasses) will be returned.
     * <p>
     * 
     * 
     * @param cls Class of objects to look for ('null' will find all objects).
     * 
     * @return An unmodifiable list of objects.
     */
    public synchronized List getObjects(Class cls)
    {
        return Collections.unmodifiableList(collisionChecker.getObjects(cls));
    }
    
    /**
     * Repaints the world. 
     *
     */
    public void repaint() 
    {
        WorldHandler.getInstance().repaint();
    }
    

    // =================================================
    //
    // COLLISION STUFF
    //
    // =================================================

    /**
     * Return all objects at a given cell.
     * <p>
     * 
     * An object is defined to be at that cell if its graphical representation
     * overlaps with the cell at any point.
     * 
     * @param x X-coordinate of the cell to be checked.
     * @param y Y-coordinate of the cell to be checked.
     * @param cls Class of objects to look return ('null' will return all
     *            objects).
     */
    public List getObjectsAt(int x, int y, Class cls)
    {
        return collisionChecker.getObjectsAt(x, y, cls);
    }

    /**
     * Return all the objects that intersect the given object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param actor An Actor in the world
     * @param cls Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    List getIntersectingObjects(Actor actor, Class cls)
    {
        return collisionChecker.getIntersectingObjects(actor, cls);
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
     * 
     * @param x Location
     * @param y Location
     * @param distance Distance in which to look for other objects
     * @param diag Is the distance also diagonal?
     * @param cls Class of objects to look for (null or Object.class will find
     *            all classes)
     * @return A collection of all neighbours found
     */
    List getNeighbours(Actor actor, int distance, boolean diag, Class cls)
    {
        return collisionChecker.getNeighbours(actor, distance, diag, cls);
    }

    /**
     * Return all objects that intersect a straight line from the location at a
     * specified angle. The angle is clockwise.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @param angle The angle relative to current rotation of the object.
     *            (0-359)
     * @param length How far we want to look (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    List getObjectsInDirection(int x0, int y0, int angle, int length, Class cls)
    {

        return collisionChecker.getObjectsInDirection(x0, y0, angle, length, cls);
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
     * Returns the center of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell center in pixels.
     */
    double getCellCenter(int l)
    {
        double cellCenter = l * cellSize + cellSize / 2.;
        return cellCenter;
    }
    
    Collection getObjectsAtPixel(int x, int y)
    {
        return collisionChecker.getObjectsAt(toCellFloor(x), toCellFloor(y), null);
    }

    void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        collisionChecker.updateObjectLocation(object, oldX, oldY);
    }

    void updateObjectSize(Actor object)
    {
        collisionChecker.updateObjectSize(object);
    }

    // =================================================
    //
    // PRIVATE MEHTHODS
    //
    // =================================================



    /**
     * Methods that throws an exception if the location is out of bounds.
     * 
     * @throws IndexOutOfBoundsException
     */
    private void ensureWithinXBounds(int x)
        throws IndexOutOfBoundsException
    {
        if (x >= getWidth()) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + x + ". It must be smaller than: "
                    + getWidth());
        }
        if (x < 0) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + x + ". It must be larger than: 0");
        }
    }

    /**
     * Methods that throws an exception if the location is out of bounds.
     * 
     * @throws IndexOutOfBoundsException
     */
    private void ensureWithinYBounds(int y)
        throws IndexOutOfBoundsException
    {
        if (y >= getHeight()) {
            throw new IndexOutOfBoundsException("The y-coordinate is: " + y + ". It must be smaller than: "
                    + getHeight());
        }
        if (y < 0) {
            throw new IndexOutOfBoundsException("The x-coordinate is: " + y + ". It must be larger than: 0");
        }
    }

    /**
     * Used to indicate the start of an animation sequence. For use in the
     * collision checker.
     * 
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    void startSequence()
    {
        collisionChecker.startSequence();
    }

    Actor getOneObjectAt(Actor object, int dx, int dy, Class cls)
    {
        return collisionChecker.getOneObjectAt(object, dx, dy, cls);
    }

    Actor getOneIntersectingObject(Actor object, Class cls)
    {
        return collisionChecker.getOneIntersectingObject(object, cls);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on itself or the World to avoid concurrent
     * modifactions.
     */
    TreeActorSet getObjectsList()
    {
        return objects;
    }

    void paintDebug(Graphics g)
    {
    /*
     * g.setColor(Color.BLACK); g.drawString("# of Objects: " + objects.size(),
     * 50,50);
     */
    /* collisionChecker.paintDebug(g); */
    }
    
    
    
    //============================================================================
    //  
    //  Object Transporting - between the two VMs
    //  
    //  IMPORTANT: This code is duplicated in greenfoot.Actor!
    //============================================================================
    
    /** The object we want to get a remote version of */
    private static Object transportField;
    
    /** Remote version of this class. Will be of type RClass. */
    private static Object remoteObjectTracker;

    /*static Object getTransportField()
    {
        return transportField;
    }*/
    
    static Object getRemoteObjectTracker()
    {
        return remoteObjectTracker;
    }

    static void setTransportField(Object obj)
    {
        transportField = obj;
    }   
    
}