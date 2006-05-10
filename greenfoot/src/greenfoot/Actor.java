package greenfoot;

import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.util.Circle;
import greenfoot.util.Version;

import java.net.URL;
import java.util.List;

/**
 * An Actor is an object that exists in the greenfoot world. 
 * Every Actor has a location in the world, and an appearance (that is:
 * an icon).
 * 
 * An Actor is not normally instantiated, but instead used as a superclass
 * to more specific objects in the world. Every object that is intended to appear
 * in the world must extend Actor. Subclasses can then define their own 
 * appearance and behaviour.
 * 
 * One of the most important aspects of this class is the 'act' method. This method
 * is called when the 'Act' or 'Play' buttons are activated in the greenfoot interface.
 * The method here is empty, and subclasses normally provide their own implementations.
 * 
 * @author Poul Henriksen
 * @version 0.9
 * @cvs-version $Id$
 */
public abstract class Actor extends ObjectTransporter
{
    /** Error message to display when trying to use methods that requires a world. */
    private static final String NO_WORLD = "No world has been instantiated.";

    /** Error message to display when trying to use methods that requires the actor be in a world. */
    private static final String ACTOR_NOT_IN_WORLD = "The actor has not been inserted into a world so it has no location yet. You might want to look at the method addedToWorld on the Actor class.";
    
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

    /** Rotation in degrees (0-359) */
    private int rotation = 0;

    /** Reference to the world that this actor is a part of. */
    World world;
    
    /** The image for this actor. */
    private GreenfootImage image;

    /** Bounding circle. Used for collision checking. */
    private Circle boundingCircle;
    
    /** Field used to store some extra data in an object. Used by collision checkers. */
    private Object data;

    private static GreenfootImage greenfootImage = new GreenfootImage("images/greenfoot-logo.png");
    private boolean usingClassImage;

    /**
     * Construct an Actor.
     * The object will have a default image.
     * 
     */
    public Actor()
    {
        // Use the class image, if one is defined, as the default image, or the
        // greenfoot logo image otherwise
        GreenfootImage image = getClassImage();
        if (image == null) {
            image = greenfootImage;
        }
        setImage(image);
        usingClassImage = true;
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

        boundsCheck(x, y);

        this.x = x;
        this.y = y;
        locationChanged(oldX, oldY);
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
    public void addedToWorld(World world)
    {}
    
    /**
     * Returns the image used to represent this Actor. This image can be 
     * modified to change the object's appearance.
     * 
     * @return The object's image.
     */
    public GreenfootImage getImage()
    {
        if (usingClassImage) {
            // If this actor is using the class image, make a copy of it before
            // returning it. Otherwise modifications will affect the class image.
            image = image.copy();
            usingClassImage = false;
        }
        
        return image;
    }

    /**
     * Set an image for this object from an image file. The file may be in
     * jpeg, gif or png format. The file should be located in the project
     * directory.
     * 
     * @param filename The name of the image file.
     */
    public void setImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        if (imageURL == null) {
            imageURL = this.getClass().getClassLoader().getResource("images/" + filename);
        }
        if (imageURL != null) {
            image = new GreenfootImage(imageURL);
            usingClassImage = false;
            sizeChanged();
        }
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

        boundsCheck(xCell, yCell);
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
            boundingCircle = new Circle(x,y,calcBoundingRadius());
        }
    }
    
    /**
     * Get the image to use when displaying this actor. This should be whatever
     * was set using setImage(). The returned image should not be modified as it
     * may be the original class image.
     * 
     * @return The image to use to display the actor
     */
    GreenfootImage getDisplayImage()
    {
        return image;
    }
    
    /**
     * Get the bounding circle of the object. Taking into consideration that the
     * object can rotate.
     * 
     */
    Circle getBoundingCircle() {        
        return boundingCircle;
    }

    void setData(Object o) {
        this.data = o;
    }
    
    Object getData() {
        return data;
    }
    
    /**
     * Check whether the object is using the class image. This is true until
     * getImage() is called, when a copy of the image is made.
     * (package-private method). 
     */
    boolean isUsingClassImage()
    {
        return usingClassImage;
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
            GreenfootImage image = GreenfootMain.getProjectProperties().getImage(clazz.getName());
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
            aWorld = WorldHandler.getInstance().getWorld();
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
        World aWorld = world;
        if (aWorld == null) {
            aWorld = WorldHandler.getInstance().getWorld();
        } 
        if (aWorld == null) {
            // Should never happen
            throw new IllegalStateException(NO_WORLD);
        }
        double cellCenter = aWorld.getCellCenter(x);
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
        World aWorld = world;
        if (aWorld == null) {
            aWorld = WorldHandler.getInstance().getWorld();
        } 
        if (aWorld == null) {
            throw new IllegalStateException(NO_WORLD);
        }
        double cellCenter = aWorld.getCellCenter(y);
        double paintY = cellCenter - image.getHeight() / 2.;
		return (int) Math.floor(paintY);
    }
    
    /**
     * Notify the world that this object's size has changed.
     *
     */
    private void sizeChanged()
    {
        if(boundingCircle != null) {
            boundingCircle.setRadius(calcBoundingRadius());
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
            boundingCircle.setX(x);
            boundingCircle.setY(y);
        }
        if(world != null) {
            world.updateObjectLocation(this, oldX, oldY);
        }
    }
    
    /**
     * Calculate the bounding radius. In grid coordinates.
     */
    private int calcBoundingRadius() {
        if(world == null) return -1;
        // An explanation of why +3 is needed:
        // +1 comes form the fact that Max - Min is not the width of the objects,
        //  but the difference in max an min location which for instance can be 0.
        // +2 we need  to cover the boundary cases in both ends.
        int dy = getYMax() - getYMin() + 3;
        int dx = getXMax() - getXMin() + 3;
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

    /**
     * Checks if the coordinates are within the bounds of the world. Throws an
     * exception if they are not within bounds.
     * 
     * @param x
     * @param y
     */
    private void boundsCheck(int x, int y)
    {
        failIfNotInWorld();
        if (world.getWidth() <= x || x < 0) {
            throw new IndexOutOfBoundsException("x(" + x + ") is out of bounds(" + world.getWidth() + ")");
        }
        if (world.getHeight() <= y || y < 0) {
            throw new IndexOutOfBoundsException("y(" + y + ") is out of bounds(" + world.getHeight() + ")");
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
        return getWorld().getNeighbours(getX(), getY(), distance, diagonal, cls);
    }
    
    /**
     * Return all objects that intersect the given location (relative to this
     * object's location). <br>
     * 
     * 
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
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
     * This method is used by collision checking methods. Therefor, this method
     * can be overridden if, for example, other than rectangular image shapes
     * should be considered. <p>
     * 
     * NOTE: Does not take rotation into consideration. <br>
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
        // TODO this disregards rotations. maybe this should be updated in the
        // getWidth/height methods
        failIfNotInWorld();
        if (image != null) {
            int width = getXMax() - getXMin() + 1;
            int height = getYMax() - getYMin() + 1;
            int left = getXMin() - getX();
            int top = getYMin() - getY();
            return intersects(dx, dy, left, top, width, height);
        }
        else {
            return false;
        }
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
    
}