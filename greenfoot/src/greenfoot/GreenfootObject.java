package greenfoot;

import greenfoot.core.LocationTracker;
import greenfoot.core.ObjectDragProxy;
import greenfoot.core.WorldHandler;
import greenfoot.util.Circle;
import greenfoot.util.Location;

import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * A GreenfootObject is an object that exists in the greenfoot world. 
 * Every GreenfootObject has a location in the world, and an appearance (that is:
 * an icon).
 * 
 * A GreenfootObject is not normally instantiated, but instead used as a superclass
 * to more specific objects in the world. Every object that is intended to appear
 * in the world must extend GreenfootObject. Subclasses can then define their own 
 * appearance and behaviour.
 * 
 * One of the most important aspects of this class is the 'act' method. This method
 * is called when the 'Act' or 'Play' buttons are activated in the greenfoot interface.
 * The method here is empty, and subclasses normally provide their own implementations.
 * 
 * @author Poul Henriksen
 * @version 0.3.0
 * @cvs-version $Id: GreenfootObject.java 3875 2006-03-24 12:03:53Z polle $
 */
public class GreenfootObject extends ObjectTransporter
{

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

    private GreenfootWorld world;
    private GreenfootImage image;

    private Circle boundingCircle;
    private Object data;

    private static GreenfootImage greenfootImage = new GreenfootImage("images/greenfoot-logo.png");

    /**
     * Construct a GreenfootObject.
     * The default position is (0,0). Usually the constructor
     * GreenfootObject(int, int) should be used in preference.
     * The object will have a default image.
     * 
     * @see GreenfootObject#GreenfootObject(int, int)
     */
    public GreenfootObject()
    {
        init();
    }

    /**
     * Construct a GreenfootObject with a default image at a specified
     * location. The location is specified as the horizontal and vertical index
     * of the world cell where the object is placed.
     * 
     * @see #setLocation(int, int)
     */
    public GreenfootObject(int x, int y)
    {
        init();
        int oldx = x;
        int oldy = y;
        this.x = x;
        this.y = y;
        locationChanged(x,y);
    }
    
    private void init()
    {
        
        setImage(greenfootImage);
        WorldHandler worldHandler = WorldHandler.instance();
        if( worldHandler == null ||  worldHandler.getWorld() == null) {
            //we are probably doing some unit testing...
            return;
        }
        
        if( ! (this instanceof ObjectDragProxy)) {            
            world = worldHandler.getWorld();
            
            LocationTracker tracker = LocationTracker.instance();
            Location location = tracker.getLocation();
            int x = location.getX();
            int y = location.getY();
            
            // If an object that shouldn't really use the location from the
            // object tracker gets a location which is out of the world bounds,
            // it should just set the location to something else. We make no
            // promises of where the obecjt will be added if no location is
            // specified.
            if(x >= world.getWidthInPixels() || x < 0) {
                x = 0;
            }
            if(y >= world.getHeightInPixels() || y < 0) {
                y = 0;
            }
            
            setLocationInPixels(x, y); 
            
            world.addObject(this);            
        }
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
     */
    public int getX()
    {
        return x;
    }

    /**
     * Return the y-coordinate of the object's current location. The
     * value returned is the vertical index of the object's cell in the world.
     * 
     * @return The y-coordinate of the object's current location
     */
    public int getY()
    {
        return y;
    }

    /**
     * Return the width of the object. The width is the number of cells
     * that an object's image overlaps horizontally.
     * 
     * @return The width of the object, or -1 if it has no image.
     */
    public int getWidth()
    {
        if (image == null || world == null) {
            return -1;
        }
        else {
            return getXMax() - getXMin() + 1; // TODO watch out if wrapping?
        }
    }

    /**
     * Return the height of the object. The height is the number of cells
     * that an object's image overlaps vertically.
     * 
     * @return The height of the object, or -1 if it has no image.
     */
    public int getHeight()
    {
        if (image == null || world == null) {
            return -1;
        }
        else {
            return getYMax() - getYMin() + 1;
        }
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
     */
    private int getXMax()
    {
        return toCellFloor(getPaintX() + image.getWidth() - 1);
    }

    /**
     * Gets the y-coordinate of the top most cell that is occupied by the
     * object.
     * 
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
        int oldWidth = getWidth();
        int oldHeight = getHeight();

        this.rotation = rotation;

        if (oldHeight != getHeight() || oldWidth != getWidth()) {
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
     */
    public void setLocation(int x, int y)
    {
        if (world == null) {
            this.x = x;
            this.y = y;
            return;
        }
        int oldX = this.x;
        int oldY = this.y;

        if (!world.isWrapped()) {
            boundsCheck(x, y);
        }

        this.x = x;
        this.y = y;
        locationChanged(oldX, oldY);
    }


    private void boundsCheck(int x, int y)
    {
        if (world != null) {
            if (world.getWidth() <= x || x < 0) {
                throw new IndexOutOfBoundsException("x(" + x + ") is out of bounds("+world.getWidth() +")");
            }
            if (world.getHeight() <= y || y < 0) {
                throw new IndexOutOfBoundsException("y(" + y + ") is out of bounds("+world.getHeight() +")");
            }
        }
    }

    /**
     * Return the world that this object lives in.
     * 
     * @return The world.
     */
    final public GreenfootWorld getWorld()
    {
        return world; 
    }
 
    /**
     * Returns the image used to represent this GreenfootObject. This image can be 
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
     * jpeg, gif or png format. The file should be located in the project directory.
     * 
     * @see #setImage(ImageIcon)
     * @param filename The name of the image file.
     */
    final public void setImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        if (imageURL != null) {
            image = new GreenfootImage(imageURL);
            sizeChanged();
        }
    }

    /**
     * Set the image for this object to the specified image.
     * 
     * @see #setImage(String)
     * @param image The image.
     */
    final public void setImage(GreenfootImage image)
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
        if (world != null) {
            int xCell = world.toCellFloor(x);
            int yCell = world.toCellFloor(y);
            
            if(x == getX() && y == getY()) {
                return;
            }
            
            boundsCheck(xCell, yCell);
            setLocation(xCell, yCell);
        }
    }

    /**
     * Sets the world of this object
     * 
     * @param world
     */
    void setWorld(GreenfootWorld world)
    {
        // TODO Possible error if its location is out of bounds
        this.world = world;
        boundingCircle = new Circle();
        boundingCircle.setRadius(calcBoundingRadius());
        boundingCircle.setX(x);
        boundingCircle.setY(y);  
    }
    
    Circle getBoundingCircle() {        
        return boundingCircle;
    }

    void setData(Object o) {
        this.data = o;
    }
    
    Object getData() {
        return data;
    }
    
    private int toCellFloor(int i)
    {
        return (int) Math.floor((double) i / getWorld().getCellSize());
    }

    /**
     * Pixel location of the left of the image.
     * 
     * Rounds down if it does not result in an integer.
     * 
     * @return
     */
    private final int getPaintX()
    {
        if (world == null) {
            return -1;
        }
        int cellSize = world.getCellSize();
        double cellCenter = getX() * cellSize + cellSize / 2.;
        double paintX = cellCenter - image.getWidth() / 2.;

        return (int) Math.floor(paintX);
    }

    /**
     * Pixel location of the top of the image.
     * 
     * Rounds down if it does not result in an integer.
     * 
     * @return
     */
    private final int getPaintY()
    {
        if (world == null) {
            return -1;
        }
        int cellSize = world.getCellSize();
        double cellCenter = getY() * cellSize + cellSize / 2.;
        double paintY = cellCenter - image.getHeight() / 2.;

        return (int) Math.floor(paintY);
    }
    

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
     *Calculate the bounding radius. In grid coordinates.
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

    // ============================
    //
    // Collision stuff
    //
    // ============================

    /**
     * Check whether this object intersects with another given object.
     * 
     * NOTE: Does not take rotation into consideration, and has not been tested
     * when the world is wrapped.
     * 
     * @return True if the object's intersect, false otherwise.
     */
    protected boolean intersects(GreenfootObject other)
    {
        // TODO: Rotation, we could just increase the bounding box, or we could
        // deal with the rotated bounding box.
        // TODO: Take wrapping of the world into consideration.
        if (world == null)
            return false;

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
     * Checks whether the specified relative cell-location is considered to be
     * inside this object.<p>
     * 
     * A location is considered to be inside an object, if the object's image
     * overlaps at least partially with that cell.<p>
     * 
     * This method is used by collision checking methods. Therefor, this method 
     * can be overridden if, for example, other than rectangular image shapes
     * should be considered.
     * <br>
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @param dx The x-position relative to the location of the object
     * @param dy The y-position relative to the location of the object
     * @return True if the image contains the cell. If the object has no image,
     *         false will be returned.
     */
    protected boolean contains(int dx, int dy)
    {
        // TODO wrapping when the object actually lies on the edge.
        // TODO this disregards rotations. maybe this should be updated in the
        // getWidth/height methods
        if (image != null && world != null) {
            int width = getXMax() - getXMin() + 1;
            int height = getYMax() - getYMin() + 1;
            int left = getXMin() - getX();
            int top = getYMin() - getY();
            if (world.isWrapped()) {
                //TODO dx, dy is relative and should not be wrapped in this way. If at all...
          //      dx = world.wrap(dx, world.getWidth());
            //    dy = world.wrap(dy, world.getHeight());
            }
           // System.out.println("dx,dy: " + dx +"," + dy +"  left: " + left + "  top: " + top + "  width:" + width + "  height:" + height);
            
            return intersects(dx, dy, left, top, width, height);
        }
        else {
            return false;
        }
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
     * NOTE: It does not return subclasses of the given class, but only the actual class.
     * 
     * @param distance Distance (in cells) in which to look for other objects.
     * @param diagonal If true, include diagonal steps.
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     * @return A list of all neighbours found.
     */
    protected List getNeighbours(int distance, boolean diagonal, Class cls)
    {
        return getWorld().getNeighbours(getX(), getY(), distance, diagonal, cls);
    }
    
    /**
     * Return all objects that intersect the given location (relative to this
     * object's location). <br>
     * 
     * NOTE: has not been tested when the world is wrapped.<br>
     * NOTE: It does not return subclasses of the given class, but only the actual class.
     * 
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getObjectsAt(int dx, int dy, Class cls)
    {
        return world.getObjectsAt(getX() + dx, getY() + dy, cls);
    }

    /**
     * Return one object that is located at the specified cell (relative to this
     * objects location). Objects found can be restricted to a specific class
     * (and its subclasses) by supplying the 'cls' parameter. If more than one
     * object of the specified class resides at that location, one of them will
     * be chosen and returned.
     * 
     * NOTE: has not been tested when the world is wrapped.<br>
     * NOTE: It does not return subclasses of the given class, but only the actual class.
     * 
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     * @return An object at the given location, or null if none found.
     */
    protected GreenfootObject getOneObjectAt(int dx, int dy, Class cls)
    {
        return world.getOneObjectAt(getX() + dx, getY() + dy, cls);
        
    }
    
    /**
     * Return all objects within range 'r' around this object. 
     * An object is within range if the distance between its centre and this
     * object's centre is less than or equal to r.
     * 
     * 
     * @param r Radius of the cirle (in pixels)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getObjectsInRange(int r, Class cls)
    {
        List inRange = world.getObjectsInRange(getX(), getY(), r, cls);
        inRange.remove(this);
        return inRange;
    }

    /**
     * Return all the objects that intersect this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration, and has not been tested
     * when the world is wrapped.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getIntersectingObjects(Class cls)
    {
        return world.getIntersectingObjects(this, cls);
    }
    
    /**
     * Return an object that intersects this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration, and has not been tested
     * when the world is wrapped.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected GreenfootObject getOneIntersectingObject(Class cls)
    {
        return world.getOneIntersectingObject(this, cls);

    }

    /**
     * Return all objects that intersect a straight line from this object at
     * a specified angle. The angle is clockwise relative to the current 
     * rotation of the object.   <br>
     * It will never include the object itself.
     * 
     * 
     * @param angle The angle relative to current rotation of the object.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getObjectsInDirection(int angle, int length, Class cls)
    {
       List l = world.getObjectsInDirection(getX(), getY(), angle + getRotation(), length, cls);
       l.remove(this);
       return l;
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
     * TODO: wrap
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