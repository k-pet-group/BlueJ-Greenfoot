package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.core.WorldHandler;
import greenfoot.platforms.ActorDelegate;
import greenfoot.util.Circle;
import greenfoot.util.GreenfootUtil;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.List;


/**
 * An Actor is an object that exists in the Greenfoot world. 
 * Every Actor has a location in the world, and an appearance (that is:
 * an icon).
 * 
 * An Actor is not normally instantiated, but instead used as a superclass
 * to more specific objects in the world. Every object that is intended to appear
 * in the world must extend Actor. Subclasses can then define their own 
 * appearance and behaviour.
 * 
 * One of the most important aspects of this class is the 'act' method. This method
 * is called when the 'Act' or 'Play' buttons are activated in the Greenfoot interface.
 * The method here is empty, and subclasses normally provide their own implementations.
 * 
 * @author Poul Henriksen
 * @version 1.5
 */
public abstract class Actor 
{
    
    /** Error message to display when trying to use methods that requires a world. */
    private static final String NO_WORLD = "An actor is trying to access the world, when no world has been instantiated.";

    /** Error message to display when trying to use methods that requires the actor be in a world. */
    private static final String ACTOR_NOT_IN_WORLD = "The actor has not been inserted into a world so it has no location yet. You might want to look at the method addedToWorld on the Actor class.";
    
    /** Counter of number of actors constructed, used as a hash value */
    static int sequenceNumber = 0;
    
    /**
     * x-coordinate of the object's location in the world. The object is
     * centered around this location.
     */
    int x;

    /**
     * y-coordinate of the object's location in the world. The object is
     * centered aroudn this location.
     */
    int y;
    
    /**
     * Sequence number of this actor
     */
    private int mySequenceNumber;
    
    /**
     * The last time objects in the world were painted, where was this object
     * in the sequence?
     */
    private int lastPaintSequenceNumber;

    /** Rotation in degrees (0-359) */
    private int rotation = 0;

    /** Reference to the world that this actor is a part of. */
    World world;
    
    /** The image for this actor. */
    private GreenfootImage image;

    /** Bounding circle. Used for collision checking. In PIXELS, not cells. */
    private Circle boundingCircle;
    
    /** Field used to store some extra data in an object. Used by collision checkers. */
    private Object data;

    private static GreenfootImage greenfootImage;
    
    static {
        //Do this in a 'try' since a failure at this point will crash greenfoot.
        try {            
            greenfootImage = new GreenfootImage(GreenfootUtil.getGreenfootLogoPath().toString());
        }
        catch(Exception e) {
            // Should not happen unless the greenfoot installation is seriously broken.
            e.printStackTrace();
            System.err.println("Greenfoot installation is broken - reinstalling Greenfoot might help.");
        }
    }
    
    /**
     * Construct an Actor.
     * The object will have a default image.
     * 
     */
    public Actor()
    {
        // Use the class image, if one is defined, as the default image, or the
        // greenfoot logo image otherwise
        mySequenceNumber = sequenceNumber++;
        GreenfootImage image = getClassImage();
        if (image == null) {
            image = greenfootImage;
        }
        
        // Make the image a copy of the original to avoid modifications to the
        // original. 
        image = image.getCopyOnWriteClone();
        
        setImage(image);
    }

    /**
     * The act method is called by the greenfoot framework to give objects a
     * chance to perform some action. At each action step in the environment,
     * each object's act method is invoked, in unspecified order.
     * 
     * This method does nothing. It should be overridden in subclasses to
     * implement an object's action.
     */
    public void act()
    {
    }

    /**
     * Return the x-coordinate of the object's current location. The
     * value returned is the horizontal index of the object's cell in the world.
     * 
     * @return The x-coordinate of the object's current location.
     * @throws IllegalStateException If the actor has not been added into a world.
     */
    public int getX() throws IllegalStateException
    {
        failIfNotInWorld();
        return x;
    }

    /**
     * Return the y-coordinate of the object's current location. The
     * value returned is the vertical index of the object's cell in the world.
     * 
     * @return The y-coordinate of the object's current location
     * @throws IllegalStateException If the actor has not been added into a world.
     */
    public int getY()
    {       
        failIfNotInWorld();
        return y;
    }

    /**
     * Return the width of the object. The width is the number of cells
     * that an object's image overlaps horizontally.
     * 
     * @return The width of the object, or -1 if it has no image.
     * @throws IllegalStateException If there is no world instantiated.
     */
    public int getWidth()
    {
        if (image == null) {
            return -1;
        }
        else {
            return getXMax() - getXMin() + 1;
        }
    }

    /**
     * Return the height of the object. The height is the number of cells
     * that an object's image overlaps vertically.
     * 
     * @return The height of the object, or -1 if it has no image.
     * @throws IllegalStateException If there is no world instantiated.
     */
    public int getHeight()
    {
        if (image == null) {
            return -1;
        }
        else {
            return getYMax() - getYMin() + 1;
        }
    }

    /**
     * Return the current rotation of the object. Rotation is expressed as a degree
     * value, range (0..359). Zero degrees is to the east. The angle increases 
     * clockwise.
     * 
     * @see #setRotation(int)
     * 
     * @return The rotation in degrees.
     */
    public int getRotation()
    {
        return rotation;
    }

    /**
     * Set the rotation of the object. Rotation is expressed as a degree
     * value, range (0..359). Zero degrees is to the east. The angle increases 
     * clockwise.
     * 
     * @param rotation The rotation in degrees.
     */
    public void setRotation(int rotation)
    {
        
        int oldWidth = 0;
        int oldHeight = 0;
        if(world != null) {
            oldWidth = getWidth();
            oldHeight = getHeight();
        }

        this.rotation = rotation;

        if (world != null && (oldHeight != getHeight() || oldWidth != getWidth())) {
            sizeChanged();
        }
    }

    /**
     * Assign a new location for this object. The location is specified as a cell
     * index in the world.
     * 
     * If this method is overridden it is important to call this method with
     * super.setLocation(x,y) from the overriding method.
     * 
     * @param x Location index on the x-axis
     * @param y Location index on the y-axis
     * @throws IllegalStateException If the actor has not been added into a world.
     */
    public void setLocation(int x, int y)
    {
        failIfNotInWorld();
        int oldX = this.x;
        int oldY = this.y;
        
        this.x = limitValue(x, world.getWidth());
        this.y = limitValue(y, world.getHeight());

        locationChanged(oldX, oldY);
    }

    /**
     * Limits the value v to be less than limit and large or equal to zero.
     * @return
     */
    private int limitValue(int v, int limit)
    {
        if (v < 0) {
            v = 0;
        }
        if (limit <= v) {
            v = limit - 1;
        }
        return v;
    }

    /**
     * Return the world that this object lives in.
     * 
     * @return The world.
     */
    public World getWorld()
    {
        return world; 
    }

    /**
     * This method will be called by the Greenfoot system when the object has
     * been inserted into the world. This method can be overridden to implement
     * custom behavoiur when the actor is inserted into the world.
     * <p>
     * This default implementation is empty.
     * 
     * @param world The world the object was added to.
     */
    protected void addedToWorld(World world)
    {}
    
    /**
     * Returns the image used to represent this Actor. This image can be 
     * modified to change the object's appearance. 
     * 
     * @return The object's image.
     */
    public GreenfootImage getImage()
    {
        return image;
    }

    /**
     * Set an image for this object from an image file. The file may be in
     * jpeg, gif or png format. The file should be located in the project
     * directory.
     * 
     * @param filename The name of the image file.
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    public void setImage(String filename) throws IllegalArgumentException
    {
        image = new GreenfootImage(filename);
        sizeChanged();
    }

    /**
     * Set the image for this object to the specified image.
     * 
     * @see #setImage(String)
     * @param image The image.
     */
    public void setImage(GreenfootImage image)
    {
        this.image = image;
        sizeChanged();
    }
    
    // ==================================
    //
    // PACKAGE PROTECTED METHODS
    //
    // ==================================
    /**
     * 
     * Translates the given location into cell-coordinates before setting the
     * location.
     * 
     * Used by the WorldHandler to drag objects.
     * 
     * @param x x-coordinate in pixels
     * @param y y-coordinate in pixels
     */
    void setLocationInPixels(int x, int y)
    {
        failIfNotInWorld();
        int xCell = world.toCellFloor(x);
        int yCell = world.toCellFloor(y);

        if (x == getX() && y == getY()) {
            return;
        }

        setLocation(xCell, yCell);
    }

    /**
     * Sets the world of this actor.
     * 
     * @param world
     */
    void setWorld(World world)
    {
        this.world = world;
        if(world != null) {
            boundingCircle = new Circle(toPixel(x), toPixel(y), calcBoundingRadius());
        }
    }

    /**
     * Adds this object to a world at the given coordinate.
     */
    void addToWorld(int x, int y, World world)
    {
        this.x = limitValue(x, world.getWidth());
        this.y = limitValue(y, world.getHeight());

        this.setWorld(world);

        // This call is not necessary, however setLocation may be overriden
        // so it must still be called. (Asteroids scenario relies on setLocation
        // being called when the object is added to the world...)
        this.setLocation(x, y);
    }
    
    /**
     * Get the bounding circle of the object. Taking into consideration that the
     * object can rotate. This is in pixels.
     * 
     */
    Circle getBoundingCircle() {        
        return boundingCircle;
    }
    
    /**
     * Get the bounding rectangle of the object. Taking into consideration that the
     * object can rotate. 
     * 
     * @return A new Rect specified in pixels!
     */
    Rect getBoundingRect() {    
        if(world == null) return null;
        int x = getPaintX();
        int y = getPaintY();
        int width = image.getWidth();
        int height = image.getHeight();
        return new Rect(x, y, width, height);
    }

    void setData(Object o) {
        this.data = o;
    }
    
    Object getData() {
        return data;
    }

    
    // ============================
    //
    // Private methods
    //
    // ============================
    
    /**
     * Get the default image for objects of this class. May return null.
     */
    private GreenfootImage getClassImage()
    {
        Class clazz = getClass();
        while (clazz != null) {
            GreenfootImage image = null;
            try {
                image = getImage(clazz);
            }
            catch (Throwable e) {
                // Ignore exception and continue looking for images
            }
            if (image != null) {
                return image;
            }
            clazz = clazz.getSuperclass();
        }

        return greenfootImage;
    }
    
    private int toCellFloor(int i)
    {
        World aWorld = world;
        if(aWorld == null) {
            aWorld = getActiveWorld();
        }
        if(aWorld == null) {
            // Should never happen
            throw new IllegalStateException(NO_WORLD);
        }
        return (int) Math.floor((double) i / aWorld.getCellSize());
    }

    /**
     * Gets the x-coordinate of the left most cell that is occupied by the
     * object.
     * 
     */
    private int getXMin()
    {
        return toCellFloor(getPaintX());
    }

    /**
     * Gets the x-coordinate of the right most cell that is occupied by the
     * object.
     * 
     * @throws IllegalStateException If there is no world instantiated.
     */
    private int getXMax()
    {
        return toCellFloor(getPaintX() + image.getWidth() - 1);
    }

    /**
     * Gets the y-coordinate of the top most cell that is occupied by the
     * object.
     * 
     * @throws IllegalStateException If there is no world instantiated.
     */
    private int getYMin()
    {
        return toCellFloor(getPaintY());
    }

    /**
     * Gets the y-coordinate of the bottom most cell that is occupied by the
     * object.
     * 
     */
    private int getYMax()
    {
        return toCellFloor(getPaintY() + image.getHeight() - 1);
    }
    
    /**
     * Pixel location of the left of the image.
     * 
     * Rounds down if it does not result in an integer.
     * 
     * @throws IllegalStateException If there is no world instantiated.
     */
    private final int getPaintX()
    {
        double cellCenter = getCellCenter(x);
        double paintX = cellCenter - image.getWidth() / 2.;
        return (int) Math.floor(paintX);
    }

    /**
     * Pixel location of the top of the image.
     * 
     * Rounds down if it does not result in an integer.
     * 
     * @throws IllegalStateException If there is no world instantiated.
     */
    private final int getPaintY()
    {
        double cellCenter = getCellCenter(y);
        double paintY = cellCenter - image.getHeight() / 2.;
		return (int) Math.floor(paintY);
    }


    /**
     * Gets the center location of this cell (in pixels).
     * 
     * @param X or Y coordinate of a cell to be translated into pixels.
     * @returns location in pixels of the center of the cell.
     * @throws IllegalStateException If there is no world instantiated.
     */
    private double getCellCenter(int cell)
        throws IllegalStateException
    {
        World aWorld = world;
        if (aWorld == null) {
            aWorld = getActiveWorld();
        } 
        if (aWorld == null) {
            // Should never happen.
            throw new IllegalStateException(NO_WORLD);
        }
        return aWorld.getCellCenter(cell);
    }
    
    /**
     * Notify the world that this object's size has changed, if it in fact has changed.
     *
     */
    private void sizeChanged()
    {
        int newSize = calcBoundingRadius();
        if(boundingCircle != null && newSize == boundingCircle.getRadius()) {
            //If the size have not changed we just return
            return;
        }
        if(boundingCircle != null) {
            boundingCircle.setRadius(newSize);
        }
        if(world != null) {
            world.updateObjectSize(this);
        }
    }   

    /**
     * Notify the world that this object's location has changed.
     *
     */
    private void locationChanged(int oldX, int oldY)
    {
        if(boundingCircle != null) {
            boundingCircle.setX(toPixel(x));
            boundingCircle.setY(toPixel(y));
        }
        if(world != null) {
            world.updateObjectLocation(this, oldX, oldY);
        }
    }
    
    /**
     * Translate a cell coordinate into a pixel. This will return the coordinate of the center of he cell.
     */
    private int toPixel(int x)
    {        
        World aWorld = world;
        if(aWorld == null) {
            aWorld = getActiveWorld();
        }
        if(aWorld == null) {
            // Should never happen
            throw new IllegalStateException(NO_WORLD);
        }
        return x * aWorld.getCellSize() +  aWorld.getCellSize()/2;
    }

    /**
     * Calculate the bounding radius. In pixels coordinates.
     */
    private int calcBoundingRadius() {
        if(world == null) return -1;

        int dx = image.getHeight();
        int dy = image.getWidth();
        return (int) (Math.sqrt(dx*dx + dy*dy) / 2 );
    }
    
    /**
     * Throws an exception if the actor is not in a world.
     * 
     * @throws IllegalStateException If not in world.
     */
    private void failIfNotInWorld()
    {
        if(world == null) {
            throw new IllegalStateException(ACTOR_NOT_IN_WORLD);
        }
    }

   

    // ============================
    //
    // Collision stuff
    //
    // ============================

    /**
     * Check whether this object intersects with another given object.
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @return True if the object's intersect, false otherwise.
     */
    protected boolean intersects(Actor other)
    {
        // TODO: Rotation, we could just increase the bounding box, or we could
        // deal with the rotated bounding box.
        int thisX = getXMin();
        int otherX = other.getXMin();
        int thisW = getWidth();
        int otherW = other.getWidth();
        if (!intersects(thisX, otherX, thisW, otherW)) {
            return false;
        }

        int thisY = getYMin();
        int otherY = other.getYMin();
        int thisH = getHeight();
        int otherH = other.getHeight();
        if (!intersects(thisY, otherY, thisH, otherH)) {
            return false;
        }

        return true;
    }

    /**
     * Return the neighbours to this object within a given distance. This
     * method considers only logical location, ignoring extent of the image.
     * Thus, it is most useful in scenarios where objects are contained in a
     * single cell.
     * <p>
     * 
     * All cells that can be reached in the number of steps given in 'distance'
     * from this object are considered. Steps may be only in the four main
     * directions, or may include diagonal steps, depending on the 'diagonal'
     * parameter. Thus, a distance/diagonal specification of (1,false) will
     * inspect four cells, (1,true) will inspect eight cells.
     * <p>
     * 
     * @param distance Distance (in cells) in which to look for other objects.
     * @param diagonal If true, include diagonal steps.
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     * @return A list of all neighbours found.
     */
    protected List getNeighbours(int distance, boolean diagonal, Class cls)
    {
        failIfNotInWorld();
        return getWorld().getNeighbours(this, distance, diagonal, cls);
    }
    
    /**
     * Return all objects that intersect the given location (relative to this
     * object's location). <br>
     * 
     * @return List of objects at the given offset. The list will include this
     *         object, if the offset is zero.
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    protected List getObjectsAtOffset(int dx, int dy, Class cls)
    {
        failIfNotInWorld();
        return world.getObjectsAt(getX() + dx, getY() + dy, cls);
    }

    /**
     * Return one object that is located at the specified cell (relative to this
     * objects location). Objects found can be restricted to a specific class
     * (and its subclasses) by supplying the 'cls' parameter. If more than one
     * object of the specified class resides at that location, one of them will
     * be chosen and returned.
     * 
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     * @return An object at the given location, or null if none found.
     */
    protected Actor getOneObjectAtOffset(int dx, int dy, Class cls)
    {
        failIfNotInWorld();
        return world.getOneObjectAt(this, getX() + dx, getY() + dy, cls);        
    }
    
    /**
     * Return all objects within range 'r' around this object. 
     * An object is within range if the distance between its centre and this
     * object's centre is less than or equal to r.
     * 
     * @param r Radius of the cirle (in pixels)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getObjectsInRange(int r, Class cls)
    {
        failIfNotInWorld();
        List inRange = world.getObjectsInRange(getX(), getY(), r, cls);
        inRange.remove(this);
        return inRange;
    }

    /**
     * Return all the objects that intersect this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getIntersectingObjects(Class cls)
    {
        failIfNotInWorld();
        List l = world.getIntersectingObjects(this, cls);
        l.remove(this);
        return l;
    }
    
    /**
     * Return an object that intersects this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected Actor getOneIntersectingObject(Class cls)
    {
        failIfNotInWorld();
        return world.getOneIntersectingObject(this, cls);
    }

//    /**
//     * Return all objects that intersect a straight line from this object at
//     * a specified angle. The angle is clockwise relative to the current 
//     * rotation of the object.   <br>
//     * It will never include the object itself.
//     * 
//     * NOTE: not implemented yet!
//     * 
//     * @param angle The angle relative to current rotation of the object.
//     * @param cls Class of objects to look for (passing 'null' will find all objects).
//     */
//    protected List getObjectsInDirection(int angle, int length, Class cls)
//    {
//        failIfNotInWorld();
//        List l = world.getObjectsInDirection(getX(), getY(), angle + getRotation(), length, cls);
//        l.remove(this);
//        return l;
//    }


    /**
     * Checks whether the specified relative cell-location is considered to be
     * inside this object.
     * <p>
     * 
     * A location is considered to be inside an object, if the object's image
     * overlaps at least partially with that cell.
     * <p>
     * 
     * This method is used by collision checking methods. Therefore, this method
     * can be overridden if, for example, other than rectangular image shapes
     * should be considered. <p>
     * 
     * NOTE: No longer public,
     * since no scenarios have used it so far, and we might want to do it
     * sligthly different if we want collision checkers to only do most of the
     * computation once pr. act.
     * 
     * @param dx The x-position relative to the location of the object
     * @param dy The y-position relative to the location of the object
     * @return True if the image contains the cell. If the object has no image,
     *         false will be returned.
     */
    boolean contains(int dx, int dy)
    {
        failIfNotInWorld();
        if (image == null) {
            return false;
        }
        int cellSize = getWorld().getCellSize();

        //System.out.println("Imagesize: " + image.getWidth() + "," + image.getHeight());
        int xMin = (int) Math.ceil(-image.getWidth() / 2d);
        int yMin = (int) Math.ceil(-image.getHeight() / 2d);
        int xMax = xMin + image.getWidth();
        int yMax = yMin + image.getHeight();

        // Create polygon representing the bounding box of the unrotated image
        // in pixels.
        int[] xCoords = new int[]{xMin, xMin, xMax, xMax};
        int[] yCoords = new int[]{yMin, yMax, yMax, yMin};
        Polygon imageBounds = new Polygon(xCoords, yCoords, 4);

        Shape rotatedImageBounds = null;
        if(getRotation() != 0) {
            AffineTransform transform = AffineTransform.getRotateInstance(Math.toRadians(getRotation()));       
            rotatedImageBounds = transform.createTransformedShape(imageBounds);
            /*   System.out.println("Rotated Image: " + rotatedImageBounds);
            it = rotatedImageBounds.getPathIterator(null);
            while (!it.isDone()) {
                double[] coords = new double[6];
                it.currentSegment(coords);
                it.next();
                System.out.println(" coords: " + coords[0] + "," + coords[1]);
            }*/
        } else {
            rotatedImageBounds = imageBounds;
        }
        Rectangle cellBounds = new Rectangle(dx * cellSize - cellSize / 2, dy * cellSize - cellSize / 2, cellSize,
                cellSize);
        
        
    //    Rectangle cellBounds = new Rectangle(dx * cellSize - (int) Math.floor(cellSize / 2d), dy * cellSize - (int) Math.floor(cellSize / 2d), cellSize, cellSize);
        // System.out.println("dx, dy: " + dx + ", " + dy);
        // System.out.println("Cell: " + cellBounds);
        return rotatedImageBounds.intersects(cellBounds);
    }
    
    /**
     * Determines if the given position intersects with the rectangle.<br>
     * 
     */
    private boolean intersects(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        if (x >= rectX && x < (rectX + rectWidth) && y >= rectY && y < (rectY + rectHeight)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Determines if two lines intersects.
     * 
     */
    private boolean intersects(int x1, int x2, int w1, int w2)
    {
        if (x1 <= x2 && x2 < x1 + w1)
            return true;
        if (x1 < x2 + w2 && x2 + w2 <= x1 + w1)
            return true;
        if (x2 <= x1 && x1 < x2 + w2)
            return true;
        if (x2 < x1 + w1 && x1 + w1 <= x2 + w2)
            return true;
        return false;
    } 
    
    /**
     * Get the sequence number of this actor. This can be used as a
     * hash value, which is not overridable by the user.
     */
    final int getSequenceNumber()
    {
        return mySequenceNumber;
    }

    /**
     * Get the sequence number of this actor from the last paint operation.
     * (Returns whatever was set using the setLastPaintSeqNum method).
     */
    final int getLastPaintSeqNum()
    {
        return lastPaintSequenceNumber;
    }
    
    /**
     * Set the sequence number of this actor from the last paint operation.
     */
    final void setLastPaintSeqNum(int num)
    {
        lastPaintSequenceNumber = num;
    }
    
    // ============================================================================
    //  
    // Methods below here are delegated to different objects depending on how
    // the project is run.
    // (From Greenfoot IDE or StandAlone)
    //  
    // ============================================================================

    private static ActorDelegate delegate;
    
    /**
     * Set the object that this actor should delegate method calls to.
     *
     */
    static void setDelegate(ActorDelegate d) {
        delegate = d;
    }
    
    static ActorDelegate getDelegate()
    {
        return delegate;
    }
    
    /**
     * Get the default image for objects of this class. May return null.
     */
    GreenfootImage getImage(Class clazz)
    {
        return delegate.getImage(clazz.getName());
    }

    /**
     * Get the active world. This method will return the instantiated world,
     * even if the object is not yet added to a world.
     */
    World getActiveWorld()
    {
        return WorldHandler.getInstance().getWorld();
    }
       
    //============================================================================
    //  
    //  Object Transporting - between the two VMs
    //  
    //  IMPORTANT: This code is duplicated in greenfoot.World!
    //============================================================================    
    
    /** The object we want to get a remote version of */
    private static Object transportField;
    
    /** Remote version of this class. Will be of type RClass. */
    private static Object remoteObjectTracker;
    
    static Object getRemoteObjectTracker()
    {
        return remoteObjectTracker;
    }

    static void setTransportField(Object obj)
    {
        transportField = obj;
    }       
}
