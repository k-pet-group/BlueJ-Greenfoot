package greenfoot;

import java.net.URL;
import java.util.List;

import javax.swing.ImageIcon;

/**
 * A GreenfootObject is a thing that can be in a world. To be in a world means
 * that it has a graphically representation and a location. Futhermore it has an
 * act method which will be called when the simulation is started when using the
 * 'play' and 'act' buttons from the greenfoot user interface..
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootObject.java 3405 2005-06-03 15:10:56Z polle $
 */
public class GreenfootObject
{

    /**
     * x-coordinate of the object's location in the world. The object is
     * centered aroudn this location.
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

    private static GreenfootImage greenfootImage = new GreenfootImage("greenfoot/greenfoot-logo.png");

    /**
     * Constructor that creates an object with a default image. <br>
     * If this method is called from within the code (programmatically) note
     * that the object gets assigned a random position. You would probably want
     * to use the constructor GreenfootObject(int, int).
     * 
     * @see GreenfootObject#GreenfootObject(int, int)
     */
    public GreenfootObject()
    {
        setImage(greenfootImage);
    }

    /**
     * Constructor that creates an object with a default image and a specified
     * location.
     * 
     * @see #setLocation(int, int)
     */
    public GreenfootObject(int x, int y)
    {
        setImage("greenfoot/greenfoot-logo.png");
        this.x = x;
        this.y = y;
    }

    /**
     * This method is called each time the object should do its stuff. The act
     * methods of different objects gets executed in a sequential way, in no
     * guarantied order. <br>
     * 
     * This implementation does nothing, so it should be overridden to create
     * custom behaviour for the objects.
     * 
     */
    public void act()
    {

    }

    /**
     * Get the x-coordinate of the object's current location in the world. The
     * object is centered around this location.
     * 
     * @return The x-coordinate of the object's current location
     */
    public int getX()
    {
        return x;
    }

    /**
     * Get the y-coordinate of the object's current location in the world. The
     * object is centered arounn this location.
     * 
     * @return The y-coordinate of the object's current location
     */
    public int getY()
    {
        return y;
    }

    /**
     * Get the width of the object in cells. The width is the number of cells
     * that an object occupies horisontally, based on the image.
     * 
     * @returns the width, or -1 if no image or world
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
     * Get the height of the object in cells. The height is the number of cells
     * that an object occupies vertically, based on the image.
     * 
     * @returns the height, or -1 if no image or world
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
     * Gets the rotation of the object. <br>
     * 
     * Zero degrees is to the east. The angle is clockwise from this.
     * 
     * @see #setRotation(int)
     * 
     * @return The rotation in degress
     */
    public int getRotation()
    {
        return rotation;
    }

    /**
     * Sets the rotation of the object. <br>
     * 
     * Zero degrees is to the east. The angle is clockwise from this.
     * 
     * @param rotation
     *            The rotation in degress
     */
    public void setRotation(int rotation)
    {
        int oldWidth = getWidth();
        int oldHeight = getHeight();

        this.rotation = rotation;

        if (oldHeight != getHeight() || oldWidth != getWidth()) {
            world.updateObjectSize(this);
        }
    }

    /**
     * Sets a new location for this object. The object is centered around this
     * location. <br>
     * 
     * If this method is overridden it is important to call this method with
     * super.setLocation(x,y) at the end of the overriding method.
     * 
     * @param x
     *            Location on the x-axis
     * @param y
     *            Location on the y-axis
     */
    public void setLocation(int x, int y)
    {
        int oldX = this.x;
        int oldY = this.y;

        this.x = x;
        this.y = y;
        if (world != null) {
            if (!world.isWrapped()) {
                boundsCheck(x, y);
            }
            world.updateObjectLocation(this, oldX, oldY);
        }
    }

    private void boundsCheck(int x, int y)
    {
        if (world != null) {
            if (world.getWidth() <= x || x < 0) {
                throw new IndexOutOfBoundsException("x is out of bounds: " + x);
            }
            if (world.getHeight() <= y || y < 0) {
                throw new IndexOutOfBoundsException("y is out of bounds: " + x);
            }
        }
    }

    /**
     * Gets the world that this object lives in
     * 
     * @return The world
     */
    final public GreenfootWorld getWorld()
    {
        return world;
    }

    /**
     * Returns an image representing this GreenfootObject.
     * 
     * @return The image
     */
    public GreenfootImage getImage()
    {
        return image;
    }

    /**
     * Sets the image of this object to the one specified by the filename. <br>
     * The file should be located in the project directory.
     * 
     * 
     * @see #setImage(ImageIcon)
     * @param filename
     *            The filename of the image.
     */
    final public void setImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        if (imageURL != null) {
            image = new GreenfootImage(imageURL);
        }
    }

    /**
     * Sets the image of this object <br>
     * 
     * @see #setImage(String)
     * @param image
     *            The image.
     */
    final public void setImage(GreenfootImage image)
    {
        this.image = image;
    }

    // ==================================
    //
    // PROTECTED METHODS
    //
    // ==================================
    /**
     * 
     * Translates the given location into cell-coordinates before setting the
     * location.
     * 
     * Used by the WorldHandler to drag objects.
     * 
     * @param x
     *            x-coordinate in pixels
     * @param y
     *            y-coordinate in pixels
     */
    void setLocationInPixels(int x, int y)
    {
        if (world != null) {
            int xCell = world.toCellFloor(x);
            int yCell = world.toCellFloor(y);
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
    }

    int toCellFloor(int i)
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
        double paintX = cellCenter - image.getWidth() / 2;

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
        double paintY = cellCenter - image.getHeight() / 2;

        return (int) Math.floor(paintY);
    }

    // ============================
    //
    // Collision stuff
    //
    // ============================

    /**
     * Whether this object intersect another object <br>
     * 
     */
    public boolean intersects(GreenfootObject other)
    {

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
     * Determines whether the given relative cell-location is considered to be
     * inside this object. <br>
     * 
     * This implementation uses the size of the image to determine which cells
     * it spans. <br>
     * 
     * This method is used by several of the methods internally in greenfoot
     * that has to do with collision checks. Therefor, this method can be
     * overridden if we want to use other shapes than the bounding box.
     * 
     * 
     * @param x
     *            The x-position relative to the location of the object
     * @param y
     *            The y-position relative to the location of the object
     * @return True if the image contains the point. If it has no image it will
     *         return false.
     */
    public boolean contains(int x, int y)
    {
        // TODO this disregards rotations. maybe this should be updated in the
        // getWidth/height methods
        if (image != null && world != null) {
            int width = getXMax() - getXMin() + 1;
            int height = getYMax() - getYMin() + 1;
            if (world.isWrapped()) {
                x = world.wrap(x, world.getWidth());
                y = world.wrap(y, world.getHeight());
            }
            return intersects(x, y, 0, 0, width, height);
        }
        else {
            return false;
        }
    }

    /**
     * Returns the neighbours to the given this object. This method only looks
     * at the logical location and not the extent of objects. Hence it is most
     * useful in scenarios where objects only span one cell.
     * 
     * @param distance
     *            Distance in which to look for other objects
     * @param diag
     *            Is the distance also diagonal?
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     * @return A collection of all neighbours found
     */
    public List getNeighbours(int distance, boolean diag, Class cls)
    {
        return getWorld().getNeighbours(getX(), getY(), distance, diag, cls);
    }

    /**
     * Returns all objects that intersects the given location relative to this
     * objects location.
     * 
     * @param dx
     *            x-coordinate relative to this objects location
     * @param dy
     *            y-coordinate relative to this objects location
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getObjectsAt(int dx, int dy, Class cls)
    {
        return world.getObjectsAt(getX() + dx, getY() + dy, cls);
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
    public List getObjectsInRange(int r, Class cls)
    {
        List inRange = world.getObjectsInRange(getX(), getY(), r, cls);
        inRange.remove(this);
        return inRange;
    }

    /**
     * Returns all the objects that intersects this object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getIntersectingObjects(Class cls)
    {
        return world.getIntersectingObjects(this, cls);
    }

    /**
     * Returns all the objects that intersects the line going out from this
     * object at the specified angle. The angle is clockwise relative to the
     * current rotation of the object.
     * 
     * @param angle
     *            The angle relative to current rotation of the object.
     *            Clockwise.
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public List getObjectsInDirection(int angle, Class cls)
    {
        return null;
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