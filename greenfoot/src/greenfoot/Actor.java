/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.core.WorldHandler;
import greenfoot.platforms.ActorDelegate;
import greenfoot.util.GreenfootUtil;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
 * @version 2.1
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

    /** Field used to store some extra data in an object. Used by collision checkers. */
    private Object data;

    private static GreenfootImage greenfootImage;

    /** Bounding rectangle of the object. In pixels. */
    private Rect boundingRect;

    static {
        //Do this in a 'try' since a failure at this point will crash greenfoot.
        try {
            greenfootImage = new GreenfootImage(GreenfootUtil.getGreenfootLogoPath());
        }
        catch (Exception e) {
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
        if (this.rotation != rotation) {
            this.rotation = rotation;
            // Recalculate the bounding rect.
            boundingRect = null;
            // since the rotation have changed, the size probably has too.
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
        if (boundingRect != null) {
            int dx = (this.x - oldX) * world.getCellSize();
            int dy = (this.y - oldY) * world.getCellSize();

            boundingRect.setX(boundingRect.getX() + dx);
            boundingRect.setY(boundingRect.getY() + dy);
        }
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
     * This method is called by the Greenfoot system when the object has
     * been inserted into the world. This method can be overridden to implement
     * custom behaviour when the actor is inserted into the world.
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
        setImage(new GreenfootImage(filename));
    }

    /**
     * Set the image for this object to the specified image.
     * 
     * @see #setImage(String)
     * @param image The image.
     */
    public void setImage(GreenfootImage image)
    {
        if (image == null && this.image == null) {
            return;
        }

        boolean sizeChanged = true;

        if (image != null && this.image != null) {
            if (image.getWidth() == this.image.getWidth() && image.getHeight() == this.image.getHeight()) {
                sizeChanged = false;
            }
        }

        this.image = image;

        if (sizeChanged) {
            boundingRect = null;
            sizeChanged();
        }
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
    }

    /**
     * Adds this object to a world at the given coordinate.
     */
    void addToWorld(int x, int y, World world)
    {
        this.x = limitValue(x, world.getWidth());
        this.y = limitValue(y, world.getHeight());
        boundingRect = null;
        this.setWorld(world);

        // This call is not necessary, however setLocation may be overriden
        // so it must still be called. (Asteroids scenario relies on setLocation
        // being called when the object is added to the world...)
        this.setLocation(x, y);
    }

    /**
     * Get the bounding rectangle of the object. Taking into consideration that the
     * object can rotate. 
     * 
     * @return A rect specified in pixels!
     */
    Rect getBoundingRect() 
    {
        if (boundingRect == null) {
            calcBounds();
        }
        return boundingRect;
    }

    /**
     * Calculates the bounds.
     */
    private void calcBounds()
    {
        if (image == null) {
            boundingRect = null;
            return;
        }
        
        World w = getActiveWorld();
        if(w == null) {
            return;
        }
        int cellSize = w.getCellSize();
        
        if (getRotation() % 90 == 0) {
            // Special fast calculation when rotated a multiple of 90
            int width = 0;
            int height = 0;
            
            if(getRotation() % 180 == 0) {
                // Rotated by 180 multiple
                width = image.getWidth();
                height = image.getHeight();
            } else {
                // Swaps width and height since image is rotated by 90 (+/- multiple of 180)
                width = image.getHeight();
                height = image.getWidth();                
            }
            double cellCenterX = getCellCenter(this.x);
            double cellCenterY = getCellCenter(this.y);
            int x = (int) Math.floor(cellCenterX - width / 2.);
            int y = (int) Math.floor(cellCenterY - height / 2.);
            
            boundingRect = new Rect(x, y, width, height);
        }
        else {
            Shape rotatedImageBounds = getRotatedShape();
            Rectangle2D bounds2d = rotatedImageBounds.getBounds2D();
            Rectangle bounds = bounds2d.getBounds();
            int x = this.x * cellSize + bounds.x;
            int y = this.y * cellSize + bounds.y;
            int width = bounds.width;
            int height = bounds.height;
            // This rect will be bit big to include all pixels that is covered.
            // We loose a bit of precision by using integers and might get
            // collisions that wouldn't be there if using floating point. But
            // making it a big bigger, we will get all the collision that we
            // would get with floating point.
            // For instance, if something has the width 28.2, it might cover 30
            // pixels.
            boundingRect = new Rect(x, y, width, height);
        }
    }

    
    void setData(Object o) {
        this.data = o;
    }
    
    Object getData() {
        return data;
    }
    
    /**
     * Translate a cell coordinate into a pixel. This will return the coordinate of the centre of he cell.
     */
    int toPixel(int x)
    {        
        World aWorld = getActiveWorld();
        if(aWorld == null) {
            // Should never happen
            throw new IllegalStateException(NO_WORLD);
        }
        return x * aWorld.getCellSize() +  aWorld.getCellSize()/2;
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
        World aWorld  = getActiveWorld();
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
        //todo rotation
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
        //todo rotation
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
        //todo rotation
        return toCellFloor(getPaintY());
    }

    /**
     * Gets the y-coordinate of the bottom most cell that is occupied by the
     * object.
     * 
     */
    private int getYMax()
    {
        // todo rotation
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
        //todo rotation
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
        // todo rotation
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
        World aWorld = getActiveWorld();
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
        if(world != null) {
            world.updateObjectLocation(this, oldX, oldY);
        }
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
     * NOTE: When rotated, it only uses the axis alligned bounds of the rotated image.
     * 
     * @return True if the object's intersect, false otherwise.
     */
    protected boolean intersects(Actor other)
    {
        // check p101 in collision check book. and p156     
        // TODO: Rotation, we could just increase the bounding box, or we could
        // deal with the rotated bounding box.
        Rect thisBounds = getBoundingRect();
        Rect otherBounds = other.getBoundingRect();       

        return thisBounds.intersects(otherBounds);
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
     * Return all objects within range 'radius' around this object. 
     * An object is within range if the distance between its centre and this
     * object's centre is less than or equal to 'radius'.
     * 
     * @param radius Radius of the circle (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    protected List getObjectsInRange(int radius, Class cls)
    {
        failIfNotInWorld();
        List inRange = world.getObjectsInRange(getX(), getY(), radius, cls);
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

        Shape rotatedImageBounds = getRotatedShape();
        Rectangle cellBounds = new Rectangle(dx * cellSize - cellSize / 2, dy * cellSize - cellSize / 2, cellSize,
                cellSize);       
        
        // Rectangle cellBounds = new Rectangle(dx * cellSize - (int) Math.floor(cellSize / 2d), dy * cellSize - (int) Math.floor(cellSize / 2d), cellSize, cellSize);
        // System.out.println("dx, dy: " + dx + ", " + dy);
        // System.out.println("Cell: " + cellBounds);
        return rotatedImageBounds.intersects(cellBounds);
    }

    /**
     * Get the shape of the image after it has been rotated.
     * 
     */
    private Shape getRotatedShape()
    {

        int width = image.getWidth();
        int height = image.getHeight();
        int xMin = - (width / 2);
        int yMin = - (height / 2);        
        
        if(getRotation() != 0) {
            int xMax = xMin + width;
            int yMax = yMin + height;
            // Create polygon representing the bounding box of the unrotated image
            // in pixels.
            int[] xCoords = new int[]{xMin, xMin, xMax, xMax};
            int[] yCoords = new int[]{yMin, yMax, yMax, yMin};
            Polygon imageBounds = new Polygon(xCoords, yCoords, 4);

            // The location around which the object should be rotated
            // This will be either 0.0 or 0.5 for even and odd sizes
            // respectively.
            double xOrigin = xMin + image.getWidth() / 2.;
            double yOrigin = yMin + image.getHeight() / 2.;
            
            AffineTransform transform = AffineTransform.getRotateInstance(Math.toRadians(getRotation()), xOrigin, yOrigin);       
            Shape rotatedImageBounds = transform.createTransformedShape(imageBounds);
          //  System.out.println("Rotated Image: " + rotatedImageBounds);
          
          /*  it = rotatedImageBounds.getPathIterator(null);
            while (!it.isDone()) {
                double[] coords = new double[6];
                it.currentSegment(coords);
                it.next();
                System.out.println(" coords: " + coords[0] + ", " + coords[1]+ ", " + coords[2]+ ", " + coords[3]+ ", " + coords[4] + ", " + coords[5]);
            }*/

            return rotatedImageBounds;
        } else {
            return new Rectangle(xMin, yMin, width, height);
        }
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
        if(world != null) {
            return world;
        }
        WorldHandler handler = WorldHandler.getInstance();
        if (handler != null) {
            return handler.getWorld();
        }
        else {
            return null;
        }
    }

    // ============================================================================
    //  
    // Object Transporting - between the two VMs
    //  
    // IMPORTANT: This code is duplicated in greenfoot.World!
    // ============================================================================

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
