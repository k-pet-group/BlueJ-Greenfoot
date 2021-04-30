/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2018,2019,2021  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.platforms.ActorDelegate;
import greenfoot.util.GreenfootUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * An Actor is an object that exists in the Greenfoot world. 
 * Every Actor has a location in the world, and an appearance (that is:
 * an icon).
 * 
 * <p>An Actor is not normally instantiated, but instead used as a superclass
 * to more specific objects in the world. Every object that is intended to appear
 * in the world must extend Actor. Subclasses can then define their own
 * appearance and behaviour.
 * 
 * <p>One of the most important aspects of this class is the 'act' method. This method
 * is called when the 'Act' or 'Run' buttons are activated in the Greenfoot interface.
 * The method here is empty, and subclasses normally provide their own implementations.
 * 
 * @author Poul Henriksen
 * @version 2.5
 */
public abstract class Actor
{
    /** Error message to display when trying to use methods that requires a world. */
    private static final String NO_WORLD = "An actor is trying to access the world, when no world has been instantiated.";

    /** Error message to display when trying to use methods that requires the actor be in a world. */
    private static final String ACTOR_NEVER_IN_WORLD = "Actor not in world. You must add it to a world before you can call this method.";
    private static final String ACTOR_LEFT_WORLD = "Actor has been removed from the world.";

    /** Counter of number of actors constructed, used as a hash value */
    private static int sequenceNumber = 0;

    /**
     * x-coordinate of the object's location in the world. The object is
     * centered around this location.
     */
    int x;

    /**
     * y-coordinate of the object's location in the world. The object is
     * centered around this location.
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
    int rotation = 0;

    /** Reference to the world that this actor is a part of. */
    World world;
    
    // The stack trace for when this actor was last removed from a world (null if it has never been in a world).
    private Throwable lastWorldRemovalTrace = null;

    /** The image for this actor. */
    private GreenfootImage image;

    /** Field used to store some extra data in an object. Used by collision checkers. */
    private Object data;

    static GreenfootImage greenfootImage;

    // The following variables cache various aspects of an actor's size, including
    // its bounding box after rotation.
    
    /** Axis-aligned bounding rectangle of the object, in pixels. */
    private Rect boundingRect;
    /** X-coordinates of the rotated bounding rectangle's corners */
    private int[] boundingXs = new int[4];
    /** Y-coordinates of the rotated bounding rectangle's corners */
    private int[] boundingYs = new int[4];
    /** Cached image width */
    private int imageWidth;
    /** Cached image hieght */
    private int imageHeight;

    static {
        //Do this in a 'try' since a failure at this point will crash Greenfoot.
        try {
            greenfootImage = new GreenfootImage(GreenfootUtil.getGreenfootLogoPath());
        }
        catch (Exception e) {
            // Should not happen unless the Greenfoot installation is seriously broken.
            e.printStackTrace();
            System.err.println("Greenfoot installation is broken - reinstalling Greenfoot might help.");
        }
    }

    /**
     * Construct an Actor.
     * The object will have a default image.
     */
    public Actor()
    {
        // Use the class image, if one is defined, as the default image, or the
        // Greenfoot logo image otherwise
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
     * The act method is called by the greenfoot framework to give actors a
     * chance to perform some action. At each action step in the environment,
     * each object's act method is invoked, in unspecified order.
     * 
     * <p>The default implementation does nothing. This method should be overridden in
     * subclasses to implement an actor's action.
     */
    public void act()
    {
    }

    /**
     * Return the x-coordinate of the actor's current location. The
     * value returned is the horizontal index of the actor's cell in the world.
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
     * value returned is the vertical index of the actor's cell in the world.
     * 
     * @return The y-coordinate of the actor's current location
     * @throws IllegalStateException If the actor has not been added into a world.
     */
    public int getY()
    {
        failIfNotInWorld();
        return y;
    }

    /**
     * Return the current rotation of this actor. Rotation is expressed as a degree
     * value, range (0..359). Zero degrees is towards the east (right-hand side of
     * the world), and the angle increases clockwise.
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
     * Set the rotation of this actor. Rotation is expressed as a degree
     * value, range (0..359). Zero degrees is to the east (right-hand side of the
     * world), and the angle increases clockwise.
     * 
     * @param rotation The rotation in degrees.
     * 
     * @see #turn(int)
     */
    public void setRotation(int rotation)
    {
        // First normalize
        if (rotation >= 360) {
            // Optimize the usual case: rotation has adjusted to a value greater than
            // 360, but is still within the 360 - 720 bound.
            if (rotation < 720) {
                rotation -= 360;
            }
            else {
                rotation = rotation % 360;
            }
        }
        else if (rotation < 0) {
            // Likwise, if less than 0, it's likely that the rotation was reduced by
            // a small amount and so will be >= -360.
            if (rotation >= -360) {
                rotation += 360;
            }
            else {
                rotation = 360 + (rotation % 360);
            }
        }
        
        if (this.rotation != rotation) {
            this.rotation = rotation;
            // Recalculate the bounding rect.
            boundingRect = null;
            // since the rotation have changed, the size probably has too.
            sizeChanged();
        }
    }
    
    /**
     * Turn this actor to face towards a certain location.
     * 
     * @param x  The x-coordinate of the cell to turn towards
     * @param y  The y-coordinate of the cell to turn towards
     */
    public void turnTowards(int x, int y)
    {
        double a = Math.atan2(y - this.y, x - this.x);
        setRotation((int) Math.toDegrees(a));
    }
    
    /**
     * Detect whether the actor has reached the edge of the world.
     * 
     * The actor is at the edge of the world if their position is
     * at, or beyond, the cells at the very edge of the world.  For example,
     * if your world is 640 by 480 pixels, an actor is at the edge if its
     * X position is &lt;= 0 or &gt;= 639, or its Y position is &lt;= 0 or &gt;= 479.
     * 
     * @return True if the actor is at or beyond the edge cell of the world, and false otherwise.
     */
    public boolean isAtEdge()
    {
        failIfNotInWorld();
        // We use <=,>= not == because actors can be outside the world bounds, and 
        // the method should still return true in this case
        return (x <= 0 || y <= 0 || x >= world.getWidth() - 1 || y >= world.getHeight() - 1);
    }

    /**
     * Assign a new location for this actor. This moves the actor to the specified
     * location. The location is specified as the coordinates of a cell in the world.
     * 
     * <p>If this method is overridden it is important to call this method as
     * "super.setLocation(x,y)" from the overriding method, to avoid infinite recursion.
     * 
     * @param x Location index on the x-axis
     * @param y Location index on the y-axis
     * 
     * @see #move(int)
     */
    public void setLocation(int x, int y)
    {
        setLocationDrag(x, y);
    }
    
    /**
     * Move this actor the specified distance in the direction it is
     * currently facing.
     * 
     * <p>The direction can be set using the {@link #setRotation(int)} method.
     * 
     * @param distance  The distance to move (in cell-size units); a negative value
     *                  will move backwards
     * 
     * @see #setLocation(int, int)
     */
    public void move(int distance)
    {
        double radians = Math.toRadians(rotation);

        // We round to the nearest integer, to allow moving one unit at an angle
        // to actually move.
        int dx = (int) Math.round(Math.cos(radians) * distance);
        int dy = (int) Math.round(Math.sin(radians) * distance);
        setLocation(x + dx, y + dy);
    }
    
    /**
     * Turn this actor by the specified amount (in degrees).
     * 
     * @param amount  the number of degrees to turn; positive values turn clockwise
     * 
     * @see #setRotation(int)
     */
    public void turn(int amount)
    {
        setRotation(rotation + amount);
    }
    
    /**
     * The implementation of setLocation.  The main reason for the existence of this method
     * (rather than inlining it into setLocation) is that setLocation can
     * be overridden.  We make sure that setLocationInPixels (used during dragging)
     * always calls this method (setLocationDrag) so that it never calls the 
     * potentially-overridden setLocation method.
     * 
     * <p>setLocation is then called once after the drag, by WorldHandler, so that actors
     * that do override setLocation only see the method called once at the end of the drag
     * (even though the stored location is changing during the drag). 
     */
    private void setLocationDrag(int x, int y)
    {
        // Note this should not call user code - because it is called off the
        // simulation thread. We must access world fields (width, height, cellSize) directly.
        
        if (world != null) {
            int oldX = this.x;
            int oldY = this.y;

            if (world.isBounded()) {
                this.x = limitValue(x, world.width);
                this.y = limitValue(y, world.height);
            }
            else {
                this.x = x;
                this.y = y;
            }

            if (this.x != oldX || this.y != oldY) {
                if (boundingRect != null) {
                    int dx = (this.x - oldX) * world.cellSize;
                    int dy = (this.y - oldY) * world.cellSize;

                    boundingRect.setX(boundingRect.getX() + dx);
                    boundingRect.setY(boundingRect.getY() + dy);

                    for (int i = 0; i < 4; i++) {
                        boundingXs[i] += dx;
                        boundingYs[i] += dy;
                    }
                }
                locationChanged(oldX, oldY);
            }
        }
    }

    /**
     * Limits the value v to be less than limit and large or equal to zero.
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
     * Return the world that this actor lives in.
     * 
     * @return The world, or null if this actor is not in a world.
     */
    public World getWorld()
    {
        return world;
    }
    
    /**
     * Return the world that this actor lives in, provided that it is
     * an instance of the given "worldClass" class (i.e. that it is an instance
     * of worldClass or one of its subclasses).
     *
     * @param <W> The type of the world.
     * @param worldClass The class of the world type.
     * @return The world this actor is in, or null if either this actor is not in a world
     * @throws java.lang.ClassCastException If the actor is in a world, but not one that is an instance of worldClass or one of its subclasses
     */
    public <W> W getWorldOfType(Class<W> worldClass)
    {
        // If null, returns null.  If not of right type, already throws ClassCastException for us:
        return worldClass.cast(world);
    }

    /**
     * This method is called by the Greenfoot system when this actor has
     * been inserted into the world. This method can be overridden to implement
     * custom behaviour when the actor is inserted into the world.
     * <p>
     * The default implementation does nothing.
     * 
     * @param world The world the object was added to.
     */
    protected void addedToWorld(@SuppressWarnings("unused") World world)
    {}

    /**
     * Returns the image used to represent this actor. This image can be
     * modified to change the actor's appearance.
     * 
     * @return The object's image.
     */
    public GreenfootImage getImage()
    {
        return image;
    }

    /**
     * Set an image for this actor from an image file. The file may be in
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
     * Set the image for this actor to the specified image.
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

        if (image != null) {
            if (image.getWidth() == imageWidth && image.getHeight() == imageHeight) {
                sizeChanged = false;
            }
            else {
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
            }
        }
        else {
            sizeChanged = imageHeight != 0 || imageWidth != 0;
            imageWidth = 0;
            imageHeight = 0;
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
        int xCell = world.toCellFloor(x);
        int yCell = world.toCellFloor(y);

        if (xCell == this.x && yCell == this.y) {
            return;
        }

        setLocationDrag(xCell, yCell);
    }

    /**
     * Sets the world of this actor.
     * 
     * @param world
     */
    void setWorld(World world, Throwable stackTrace)
    {
        this.world = world;
        if (world == null)
            lastWorldRemovalTrace = stackTrace;
    }

    /**
     * Sets the world, and the initial location. The location is adjusted according to the world's bounding
     * rules. The cached collision checking bounds, if any, are cleared.
     */
    void addToWorld(int x, int y, World world)
    {
        if (world.isBounded()) {
            x = limitValue(x, world.getWidth());
            y = limitValue(y, world.getHeight());
        }
        
        this.x = x;
        this.y = y;
        boundingRect = null;

        this.setWorld(world, null);
        
        // This call is not necessary, however setLocation may be overridden
        // so it must still be called. (Asteroids scenario relies on setLocation
        // being called when the object is added to the world...)
        this.setLocation(x, y);
    }

    /**
     * Get the axis-aligned bounding rectangle of the object, taking rotation into account.
     * This returns a rectangle which completely covers the rotated actor's area.
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
        World w = world;
        if(w == null) {
            return;
        }
        int cellSize = w.getCellSize();
        
        if (image == null) {
            int wx = x * cellSize + cellSize / 2;
            int wy = y * cellSize + cellSize / 2;
            boundingRect = new Rect(wx, wy, 0, 0);
            for (int i = 0; i < 4; i++) {
                boundingXs[i] = wx;
                boundingYs[i] = wy;
            }
            return;
        }
        
        if (rotation % 90 == 0) {
            // Special fast calculation when rotated a multiple of 90
            int width = 0;
            int height = 0;
            
            if(rotation % 180 == 0) {
                // Rotated by 180 multiple
                width = image.getWidth();
                height = image.getHeight();
            } else {
                // Swaps width and height since image is rotated by 90 (+/- multiple of 180)
                width = image.getHeight();
                height = image.getWidth();                
            }
            
            int x = cellSize * this.x + (cellSize - width - 1) / 2;
            int y = cellSize * this.y + (cellSize - height - 1) / 2;
            boundingRect = new Rect(x, y, width, height);
            boundingXs[0] = x; boundingYs[0] = y;
            boundingXs[1] = x + width - 1; boundingYs[1] = y;
            boundingXs[2] = boundingXs[1]; boundingYs[2] = y + height - 1;
            boundingXs[3] = x; boundingYs[3] = boundingYs[2];
        }
        else {
            getRotatedCorners(boundingXs, boundingYs, cellSize);
            
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            
            for (int i = 0; i < 4; i++) {
                minX = Math.min(boundingXs[i] - 1, minX);
                maxX = Math.max(boundingXs[i] + 1, maxX);
                minY = Math.min(boundingYs[i] - 1, minY);
                maxY = Math.max(boundingYs[i] + 1, maxY);
            }
            
            // This rect will be bit big to include all pixels that are covered.
            // We lose a bit of precision by using integers and might get
            // collisions that wouldn't be there if using floating point. But
            // making it a big bigger, we will get all the collision that we
            // would get with floating point.
            // For instance, if something has the width 28.2, it might cover 30
            // pixels.
            boundingRect = new Rect(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }
    }

    /**
     * Set collision-checker-private data for this actor.
     */
    void setData(Object o)
    {
        this.data = o;
    }
    
    /**
     * Get the collision-checker-private data for this actor.
     * @return An Object contains the info about the collision.
     */
    Object getData()
    {
        return data;
    }
    
    /**
     * Translate a cell coordinate into a pixel. This will return the coordinate of the centre of he cell.
     */
    int toPixel(int x)
    {
        if(world == null) {
            // Should never happen
            throw new IllegalStateException(NO_WORLD);
        }
        return x * world.getCellSize() +  world.getCellSize()/2;
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
        Class<?> clazz = getClass();
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
    

    /**
     * Notify the world that this object's size has changed, if it in fact has changed.
     */
    private void sizeChanged()
    {
        if(world != null) {
            world.updateObjectSize(this);
        }
    }   

    /**
     * Notify the world that this object's location has changed.
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
            if (lastWorldRemovalTrace == null)
                throw new IllegalStateException(ACTOR_NEVER_IN_WORLD);
            else
                throw new IllegalStateException(ACTOR_LEFT_WORLD, lastWorldRemovalTrace);
        }
    }
    
    /**
     * Calculated the co-ordinates of the bounding rectangle after it is rotated
     * and translated for the actor position, in pixels.
     * 
     * @param xs  The array to hold the four X coordinates
     * @param ys  The array to hold the four Y coordinates
     * @param cellSize  The world cell size
     */
    private void getRotatedCorners(int [] xs, int [] ys, int cellSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        
        xs[0] = -width / 2;
        xs[1] = xs[0] + width - 1;
        xs[2] = xs[1];
        xs[3] = xs[0];
        
        ys[0] = -height / 2;
        ys[1] = ys[0];
        ys[2] = ys[1] + height - 1;
        ys[3] = ys[2];
        
        double rotR = Math.toRadians(rotation);
        double sinR = Math.sin(rotR);
        double cosR = Math.cos(rotR);
        
        double xc = cellSize * x + cellSize / 2.;
        double yc = cellSize * y + cellSize / 2.;
        
        // Do the actual rotation
        for (int i = 0; i < 4; i++) {
            int nx = (int)(xs[i] * cosR - ys[i] * sinR + xc);
            int ny = (int)(ys[i] * cosR + xs[i] * sinR + yc);
            xs[i] = nx;
            ys[i] = ny;
        }
    }

    /**
     * Check whether all of the vertexes in the "other" rotated rectangle are on the
     * outside of any one of the edges in "my" rotated rectangle.
     *  
     * @param myX   The x-coordinates of the corners of "my" rotated rectangle
     * @param myY    The y-coordinates of the corners "my" rotated rectangle
     * @param otherX  The x-coordinates of the corners of the "other" rotated rectangle
     * @param otherY  The y-coordinates of the corners of the "other" rotated rectangle
     * 
     * @return  true if all corners of the "other" rectangle are on the outside of any of
     *          the edges of "my" rectangle.
     */
    private static boolean checkOutside(int [] myX, int [] myY, int [] otherX, int [] otherY)
    {
        vloop:
        for (int v = 0; v < 4; v++) {
            int v1 = (v + 1) & 3; // wrap at 4 back to 0
            int edgeX = myX[v] - myX[v1];
            int edgeY = myY[v] - myY[v1];
            int reX = -edgeY;
            int reY = edgeX;
            
            if (reX == 0 && reY == 0) {
                continue vloop;
            }

            for (int e = 0; e < 4; e++) {
                int scalar = reX * (otherX[e] - myX[v1]) + reY * (otherY[e] - myY[v1]);
                if (scalar <= 0) {
                    continue vloop;
                }
            }

            // If we got here, we have an edge with all vertexes from the other rect
            // on the outside:
            return true;
        }

        return false;
    }
    
    // ============================
    //
    // Collision stuff
    //
    // ============================

    /**
     * Check whether this object intersects with another given object.
     * 
     * @param other  The second object to detect the existing of intersection with it.
     * @return True if the object's intersect, false otherwise.
     */
    protected boolean intersects(Actor other)
    {
        if (image == null) {
            if (other.image == null) {
                // No images; the actors can be considered to represent points,
                // and we'll say they intersect if they match exactly.
                return x == other.x && y == other.y;
            }
            
            int cellSize = world.getCellSize();
            
            // We are a point, the other actor is a rect. Rotate our relative
            return other.containsPoint(x * cellSize + cellSize / 2, y * cellSize + cellSize / 2);
        }
        else if (other.image == null) {
            // We are a rectangle, the other is a point
            int cellSize = world.getCellSize();
            return containsPoint(other.x * cellSize + cellSize / 2, other.y * cellSize + cellSize / 2);
        }
        else {
            Rect thisBounds = getBoundingRect();
            Rect otherBounds = other.getBoundingRect();
            if (rotation == 0 && other.rotation == 0) {
                return thisBounds.intersects(otherBounds);
            }
            else {
                // First do a check based only on axis-aligned bounding boxes.
                if (! thisBounds.intersects(otherBounds)) {
                    return false;
                }
                
                int [] myX = boundingXs;
                int [] myY = boundingYs;
                int [] otherX = other.boundingXs;
                int [] otherY = other.boundingYs;
                
                if (checkOutside(myX, myY, otherX, otherY)) {
                    return false;
                }
                if (checkOutside(otherX, otherY, myX, myY)) {
                    return false;
                }
            }
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
     * @param <A> The class of the object to look for.
     * @param distance Distance (in cells) in which to look for other objects.
     * @param diagonal If true, include diagonal steps.
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     * @return A list of all neighbours found.
     */
    protected <A> List<A> getNeighbours(int distance, boolean diagonal, Class<A> cls)
    {
        failIfNotInWorld();
        // Don't use getWorld() here, as it is overridable
        return world.getNeighbours(this, distance, diagonal, cls);
    }
    
    /**
     * Return all objects that intersect the center of the given location (relative to
     * this object's location). <br>
     *
     * @param <A> The class of the object to look for.
     * @return List of objects at the given offset. The list will include this
     *         object, if the offset is zero.
     * @param dx X-coordinate relative to this objects location.
     * @param dy y-coordinate relative to this objects location.
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    protected <A> List<A> getObjectsAtOffset(int dx, int dy, Class<A> cls)
    {
        failIfNotInWorld();
        return world.getObjectsAt(x + dx, y + dy, cls);
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
    protected Actor getOneObjectAtOffset(int dx, int dy, Class<?> cls)
    {
        failIfNotInWorld();
        return world.getOneObjectAt(this, x + dx, y + dy, cls);        
    }
    
    /**
     * Return all objects within range 'radius' around this object. 
     * An object is within range if the distance between its centre and this
     * object's centre is less than or equal to 'radius'.
     *
     * @param <A> The class of the object to look for.
     * @param radius Radius of the circle (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     * @return List of objects of the given class type within the given radius.
     */
    protected <A> List<A> getObjectsInRange(int radius, Class<A> cls)
    {
        failIfNotInWorld();
        List<A> inRange = world.getObjectsInRange(x, y, radius, cls);
        inRange.remove(this);
        return inRange;
    }

    /**
     * Return all the objects that intersect this object. This takes the
     * graphical extent of objects into consideration. <br>
     *
     * @param <A> The class of the object to look for.
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     * @return List of objects of the given class type that intersect with the current object.
     */
    protected <A> List<A> getIntersectingObjects(Class<A> cls)
    {
        failIfNotInWorld();
        List<A> l = world.getIntersectingObjects(this, cls);
        l.remove(this);
        return l;
    }
    
    /**
     * Return an object that intersects this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     * @return An object of the given class type that intersects with the current object.
     */
    protected Actor getOneIntersectingObject(Class<?> cls)
    {
        failIfNotInWorld();
        return world.getOneIntersectingObject(this, cls);
    }
    
    /**
     * Checks whether this actor is touching any other objects
     * of the given class.
     * 
     * @param cls Class of objects to look for (passing 'null' will check for all actors).
     * @return True if there is an object of the given class type that intersects with the 
     *            current object, false otherwise.
     */
    protected boolean isTouching(Class<?> cls)
    {
        failIfNotInWorld();
        return getOneIntersectingObject(cls) != null;
    }
    
    /**
     * Removes one object of the given class that this actor is
     * currently touching (if any exist). 
     * 
     * @param cls Class of objects to remove (passing 'null' will remove any actor).
     */
    protected void removeTouching(Class<?> cls)
    {
        failIfNotInWorld();
        // This cast should never fail, because getOneIntersectingObject will only
        // be non-null if cls extends Actor.
        Actor a = (Actor)getOneIntersectingObject(cls);
        if (a != null)
        {
            world.removeObject(a);
        }
    }
    
    /**
     * Checks whether the specified point (specified in pixel co-ordinates) is within the area
     * covered by the (rotated) graphical representation of this actor.
     * 
     * @param px  The (world relative) x pixel co-ordinate
     * @param py  The (world relative) y pixel co-ordinate
     * @return  true if the pixel is within the actor's bounds; false otherwise
     */
    boolean containsPoint(int px, int py)
    {
        failIfNotInWorld();
        if (image == null) {
            return false;
        }

        if (boundingRect == null) {
            calcBounds(); // Make sure bounds are up-to-date
        }
        
        if (rotation == 0 || rotation == 90 || rotation == 270) {
            // We can just check the bounding rectangle
            return (px >= boundingRect.getX() && px < boundingRect.getRight()
                    && py >= boundingRect.getY() && py < boundingRect.getTop());
        }
        
        vloop: for (int v = 0; v < 4; v++) {
            int v1 = (v + 1) & 3; // wrap at 4 back to 0
            int edgeX = boundingXs[v] - boundingXs[v1];
            int edgeY = boundingYs[v] - boundingYs[v1];
            int reX = -edgeY;
            int reY = edgeX;

            if (reX == 0 && reY == 0) {
                continue vloop;
            }

            int scalar = reX * (px - boundingXs[v1]) + reY * (py - boundingYs[v1]);
            if (scalar < 0) {
                continue vloop;
            }

            // If we got here, we have an edge with all vertexes from the other rect
            // on the outside:
            return false;
        }
        
        return true;
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

    @OnThread(Tag.Any)
    private static ActorDelegate delegate;
    
    /**
     * Set the object that this actor should delegate method calls to.
     *
     */
    @OnThread(Tag.Any)
    static void setDelegate(ActorDelegate d)
    {
        delegate = d;
    }
    
    static ActorDelegate getDelegate()
    {
        return delegate;
    }
    
    /**
     * Get the default image for objects of this class. May return null.
     */
    GreenfootImage getImage(Class<?> clazz)
    {
        return delegate.getImage(clazz.getName());
    }

}
