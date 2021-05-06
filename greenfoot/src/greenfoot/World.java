/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2021  Poul Henriksen and Michael Kolling
 
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


import greenfoot.collision.ColManager;
import greenfoot.collision.CollisionChecker;
import greenfoot.collision.ibsp.Rect;
import greenfoot.core.TextLabel;
import greenfoot.core.WorldHandler;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * World is the world that Actors live in. It is a two-dimensional grid of
 * cells.
 * 
 * <p>All Actor are associated with a World and can get access to the world object.
 * The size of cells can be specified at world creation time, and is constant
 * after creation. Simple scenarios may use large cells that entirely contain
 * the representations of objects in a single cell. More elaborate scenarios may
 * use smaller cells (down to single pixel size) to achieve fine-grained
 * placement and smoother animation.
 * 
 * <p>The world background can be decorated with drawings or images.
 * 
 * @see greenfoot.Actor
 * @author Poul Henriksen
 * @author Michael Kolling
 * @version 2.6
 */
public abstract class World
{    
    private static final greenfoot.Color DEFAULT_BACKGROUND_COLOR = greenfoot.Color.WHITE;

    // private CollisionChecker collisionChecker = new GridCollisionChecker();
    // private CollisionChecker collisionChecker = new BVHInsChecker();
    private CollisionChecker collisionChecker = new ColManager();
    
    //{
    //    collisionChecker = new CollisionProfiler(collisionChecker);
    //}

    // One or two sets can be used to store objects in different orders.
    // Initially only the disordered set will be used, if later we need
    // ordering, the disordered set might be used for an ordered set. If two
    // orderings are used at the same time a new set will be created for the
    // second set.
    private TreeActorSet objectsDisordered = new TreeActorSet(); 
    private TreeActorSet objectsInPaintOrder;    
    private TreeActorSet objectsInActOrder;
    
    // List of text labels displayed over the world
    List<TextLabel> textLabels = new ArrayList<TextLabel>(); 

    /** The size of the cell in pixels. */
    final int cellSize;

    /** Size of the world */
    final int width;
    final int height;

    /** Image painted in the background. */
    private GreenfootImage backgroundImage;
    
    /** Whether the backgroundImage is the class image */
    private boolean backgroundIsClassImage = true;
    
    /** Whether actors are bound to stay inside the world */
    private boolean isBounded;

    /**
     * Construct a new world. The size of the world (in number of cells) and the
     * size of each cell (in pixels) must be specified.
     * 
     * @param worldWidth The width of the world (in cells).
     * @param worldHeight The height of the world (in cells).
     * @param cellSize Size of a cell in pixels.
     */
    public World(int worldWidth, int worldHeight, int cellSize)
    {
        this(worldWidth, worldHeight, cellSize, true);
    }

    /**
     * Construct a new world. The size of the world (in number of cells) and the
     * size of each cell (in pixels) must be specified. This constructor allows
     * the option of creating an unbounded world, which actors can move outside
     * the boundaries of.
     * 
     * @param worldWidth  The width of the world (in cells).
     * @param worldHeight The height of the world (in cells).
     * @param cellSize    Size of a cell in pixels.
     * @param bounded     Should actors be restricted to the world boundary?
     */
    public World(int worldWidth, int worldHeight, int cellSize, boolean bounded)
    {
        this.width = worldWidth;
        this.height = worldHeight;
        this.cellSize = cellSize;
        collisionChecker.initialize(worldWidth, worldHeight, cellSize, false);
        this.isBounded = bounded;
        
        backgroundIsClassImage = true;
        setBackground(getClassImage());
        
        // Now, the WorldHandler must be informed of the new world, so it can be
        // used immediately. This is important for actors that are created by
        // the world constructor, if the actors are accessing the world in their
        // constructors (by using getWidth/Height for instance)
        final WorldHandler wHandler = WorldHandler.getInstance();
        if(wHandler != null) { // will be null when running unit tests.
            wHandler.setInitialisingWorld(this);
        }
    }

    /**
     * Set a background image for the world. If the image size is larger than
     * the world in pixels, it is clipped. If it is smaller than the world, it
     * is tiled. A pattern showing the cells can easily be shown by setting a
     * background image with a size equal to the cell size.
     * 
     * @see #setBackground(String)
     * @param image The image to be shown
     */
    final public void setBackground(GreenfootImage image)
    {        
        if (image != null) {
            int imgWidth = image.getWidth();
            int imgHeight = image.getHeight();
            int worldWidth = getWidthInPixels();
            int worldHeight = getHeightInPixels();
            boolean tile = imgWidth < worldWidth || imgHeight < worldHeight;

            if (tile) {
                backgroundIsClassImage = false;
                backgroundImage = new GreenfootImage(worldWidth, worldHeight);
                backgroundImage.setColor(DEFAULT_BACKGROUND_COLOR);
                backgroundImage.fill();

                for (int x = 0; x < worldWidth; x += imgWidth) {
                    for (int y = 0; y < worldHeight; y += imgHeight) {
                        backgroundImage.drawImage(image, x, y);
                    }
                }
            }
            else {
                // To make it behave exactly the same way as when tiling we
                // should make a clone here. But it performs better when not
                // cloning.
                // Performance will be an issue for people changing the
                // background image all the time for animated backgrounds
                backgroundImage = image;
            }
        }
        else {
            backgroundIsClassImage = false;
            backgroundImage = null;
        }
    }

    /**
     * Set a background image for the world from an image file. Images of type
     * 'jpeg', 'gif' and 'png' are supported. If the image size is larger than
     * the world in pixels, it is clipped. A pattern showing the cells can
     * easily be shown by setting a background image with a size equal to the
     * cell size.
     * 
     * @see #setBackground(GreenfootImage)
     * @param filename The file holding the image to be shown
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    final public void setBackground(String filename)
        throws IllegalArgumentException
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
            backgroundImage.setColor(DEFAULT_BACKGROUND_COLOR);
            backgroundImage.fill();
            backgroundIsClassImage = false;
        }
        else if (backgroundIsClassImage) {
            // Make the image a copy of the original to avoid modifications
            // to the original.
            backgroundImage = backgroundImage.getCopyOnWriteClone();
            backgroundIsClassImage = false;
        }
        return backgroundImage;
    }
        
    /**
     * Return the color at the centre of the cell. To paint a color, you need to
     * get the background image for the world and paint on that.
     *
     * @param x The x coordinate of the cell.
     * @param y The y coordinate of the cell.
     * @see #getBackground()
     * @throws IndexOutOfBoundsException If the location is not within the world
     *             bounds. If there is no background image at the location it
     *             will return Color.WHITE.
     */
//    Color getAWTColorAt(int x, int y)
//    {
//
//    }

    /**
     * Return the color at the centre of the cell. To paint a color, you need to
     * get the background image for the world and paint on that.
     *
     * @param x The x coordinate of the cell.
     * @param y The y coordinate of the cell.
     * @see #getBackground()
     * @throws IndexOutOfBoundsException If the location is not within the world
     *             bounds. If there is no background image at the location it
     *             will return Color.WHITE.
     * @return The color at the centre of the cell.
     */
    public greenfoot.Color getColorAt(int x, int y)
    {
                ensureWithinXBounds(x);
        ensureWithinYBounds(y);       
        
        int xPixel = (int) Math.floor(getCellCenter(x));
        int yPixel = (int) Math.floor(getCellCenter(y));        
                
        if(xPixel >= backgroundImage.getWidth()) {
            return DEFAULT_BACKGROUND_COLOR;
        }
        if(yPixel >= backgroundImage.getHeight()) {
            return DEFAULT_BACKGROUND_COLOR;
        }        
        
        return backgroundImage.getColorAt(xPixel, yPixel);
    }
    
    /**
     * Return the width of the world (in number of cells).
     *
     * @return Width of the world (in cells)
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Return the height of the world (in number of cells).
     *
     * @return Height of the world (in cells)
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Return the size of a cell (in pixels).
     *
     * @return Size of a cell (in pixels)
     */
    public int getCellSize()
    {
        return cellSize;
    }

    /**
     * Set the paint order of objects in the world. Paint order is specified
     * by class: objects of one class will always be painted on top of objects
     * of some other class. The order of objects of the same class cannot be 
     * specified.
     * Objects of classes listed first in the parameter list will 
     * appear on top of all objects of classes listed later.
     * <p>
     * Objects of a class not explicitly specified effectively inherit the paint
     * order from their superclass.
     * <p>
     * Objects of classes not listed will appear below the objects whose classes
     * have been specified.
     * 
     * @param classes  The classes in desired paint order
     */
    @SuppressWarnings("unchecked")
    public void setPaintOrder(Class ... classes)
    {
        if (classes == null) {
            // Allow null as an argument, to specify no paint order
            if(objectsInPaintOrder == objectsDisordered) {
                if (objectsInActOrder == null) {
                    classes = new Class[0];
                    objectsDisordered.setClassOrder(true, classes);
                }
                else {
                    objectsDisordered = objectsInActOrder;
                }
            }
            objectsInPaintOrder = null;
            return;
        }
        
        if (objectsInPaintOrder != null) {
            // Just reuse existing set
        }
        else if (objectsInActOrder == objectsDisordered) {
            // Use new set because existing disordered set is in use
            // already.
            objectsInPaintOrder = new TreeActorSet();
            objectsInPaintOrder.setClassOrder(true, classes);
            objectsInPaintOrder.addAll(objectsDisordered);
            return;
        }
        else {
            // Reuse disordered set, since it is not already in use by the
            // act ordering
            objectsInPaintOrder = objectsDisordered;
        }
        objectsInPaintOrder.setClassOrder(true, classes);
    }
    
    /**
     * Set the act order of objects in the world. Act order is specified
     * by class: objects of one class will always act before objects
     * of some other class. The order of objects of the same class cannot be 
     * specified.
     * 
     * <p>Objects of classes listed first in the parameter list will 
     * act before any objects of classes listed later.
     * 
     * <p>Objects of a class not explicitly specified inherit the act
     * order from their superclass.
     * 
     * <p>Objects of classes not listed will act after all objects whose classes
     * have been specified.
     * 
     * @param classes
     *            The classes in desired act order
     */
    @SuppressWarnings("unchecked")
    public void setActOrder(Class ... classes)
    {
        if (classes == null) {
            // Allow null as an argument, to specify no paint order
            if (objectsInActOrder == objectsDisordered) {
                if (objectsInPaintOrder == null) {
                    classes = new Class[0];
                    objectsDisordered.setClassOrder(false, classes);
                }
                else {
                    objectsDisordered = objectsInPaintOrder;
                }
            }
            objectsInActOrder = null;
            return;
        }
        
        if (objectsInActOrder != null) {
            // Just reuse existing set
        }
        else if (objectsInPaintOrder == objectsDisordered) {
            // Use new set because existing disordered set is in use
            // already.
            objectsInActOrder = new TreeActorSet();
            objectsInActOrder.setClassOrder(false, classes);
            objectsInActOrder.addAll(objectsDisordered);
            return;
        }
        else {
            // Reuse disordered set, since it is not already in use by the
            // paint ordering
            objectsInActOrder = objectsDisordered;
        }
        objectsInActOrder.setClassOrder(false, classes);
    }
    
    /**
     * Add an Actor to the world.
     * 
     * @param object The new object to add.
     * @param x The x coordinate of the location where the object is added.
     * @param y The y coordinate of the location where the object is added.
     */
    public void addObject(Actor object, int x, int y)
    {
        if (object.world != null) {
            if (object.world == this) {
                return;  // Actor is already in the world
            }
            object.world.removeObject(object);
        }
        
        objectsDisordered.add(object);
        addInPaintOrder(object);
        addInActOrder(object);

        // Note we must call this before adding the object to the collision checker,
        // so that the cached bounds are cleared:
        object.addToWorld(x, y, this);
        
        collisionChecker.addObject(object);
        object.addedToWorld(this);
        
        WorldHandler whInstance = WorldHandler.getInstance();
        if (whInstance != null) {
            WorldHandler.getInstance().objectAddedToWorld(object);
        }
    }

    /**
     * Remove an object from the world.
     * 
     * @param object the object to remove
     */
    public void removeObject(Actor object)
    {
        if (object == null || object.world != this) {
            return;
        }
        
        objectsDisordered.remove(object);
        collisionChecker.removeObject(object);
        if (objectsDisordered != objectsInActOrder && objectsInActOrder != null) {
            objectsInActOrder.remove(object);
        }
        else if (objectsDisordered != objectsInPaintOrder && objectsInPaintOrder != null) {
            objectsInPaintOrder.remove(object);
        }
        object.setWorld(null, new ActorRemovedFromWorld());
    }

    /**
     * Remove a list of objects from the world.
     * 
     * @param objects A list of Actors to remove.
     */
    @SuppressWarnings("unchecked")
    public void removeObjects(Collection<? extends Actor> objects)
    {
        for (Iterator<? extends Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor actor = iter.next();
            removeObject(actor);
        }
    }

    /**
     * Get all the objects in the world, or all the objects of a particular class.
     * <p>
     * If a class is specified as a parameter, only objects of that class (or
     * its subclasses) will be returned.
     *
     * @param <A> The type of objects to look for
     * @param cls Class of objects to look for ('null' will find all objects).
     * 
     * @return A list of objects.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <A> List<A> getObjects(Class<A> cls)
    {
        List result = new ArrayList();
        
        Iterator<Actor> i = objectsDisordered.iterator();
        while (i.hasNext()) {
            Actor actor = i.next();
            if (cls == null || cls.isInstance(actor)) {
                result.add(actor);
            }
        }
        
        return result;
    }
    
    /**
     * Get the number of actors currently in the world.
     * 
     * @return The number of actors
     */
    public int numberOfObjects()
    {
        return objectsDisordered.size();
    }
    
    /**
     * Repaints the world. 
     */
    public void repaint() 
    {
        WorldHandler instance = WorldHandler.getInstance();
        if (instance != null)
        {
            instance.repaintAndWait();
        }
    }
        
    /**
     * Act method for world. The act method is called by the greenfoot framework
     * at each action step in the environment. The world's act method is called
     * before the act method of any objects in the world.<p>
     * 
     * This method does nothing. It should be overridden in subclasses to
     * implement an world's action.
     */
    public void act()
    {
        // by default, do nothing
    }

    /**
     * This method is called by the Greenfoot system when the execution has
     * started. This method can be overridden to implement custom behaviour when
     * the execution is started.
     * <p>
     * This default implementation is empty.
     */
    public void started()
    {
        // by default, do nothing
    }   
    
    /**
     * This method is called by the Greenfoot system when the execution has
     * stopped. This method can be overridden to implement custom behaviour when
     * the execution is stopped.
     * <p>
     * This default implementation is empty.
     */
    public void stopped()
    {
        // by default, do nothing
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
     * overlaps the center of the cell.
     *
     * @param <A> The type of objects to look for
     * @param x X-coordinate of the cell to be checked.
     * @param y Y-coordinate of the cell to be checked.
     * @param cls Class of objects to look return ('null' will return all
     *            objects).
     * @return The list of objects whose graphical representation overlaps the centre of the cell.
     */
    public <A> List<A> getObjectsAt(int x, int y, Class<A> cls)
    {
        return collisionChecker.getObjectsAt(x, y, (Class)cls);
    }

    /**
     * Show some text centred at the given position in the world. The text will be
     * displayed in front of any actors. Any previous text shown at the same location will
     * first be removed.
     *  
     * @param text   The text to display; can be null to show no text
     * @param x      X-coordinate of the text
     * @param y      Y-coordinate of the text
     */
    public void showText(String text, int x, int y)
    {
        for (Iterator<TextLabel> i = textLabels.iterator(); i.hasNext(); ) {
            TextLabel label = i.next();
            if (label.getX() == x && label.getY() == y) {
                if (label.getText().equals(text)) {
                    // Already have that text at that location
                    return;
                }
                // Have different text at that location
                i.remove();
                break;
            }
        }
        
        if (text != null && text.length() != 0) {
            textLabels.add(new TextLabel(text, x, y));
        }
    }

    // =================================================
    // PACKAGE-PROTECTED METHODS
    //
    // used by other classes internally in Greenfoot
    // =================================================

    /**
     * Return the world's background image but without initialising it first
     * 
     * @return The background image or null if not initialised yet.
     */
    GreenfootImage getBackgroundNoInit()
    {
        return backgroundImage;
    }
    
    /**
     * Test whether this world is bounded. 
     */
    boolean isBounded()
    {
        return isBounded;
    }

    /**
     * Return all the objects that intersect the given object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param actor An Actor in the world
     * @param cls Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    <A> List<A> getIntersectingObjects(Actor actor, Class<A> cls)
    {
        return collisionChecker.getIntersectingObjects(actor, (Class)cls);
    }

    /**
     * Returns all objects with the logical location within the specified
     * circle. In other words an object A is within the range of an object B if
     * the distance between the centre of the two objects is less than r.
     * 
     * @param x Centre of the cirle
     * @param y Centre of the cirle
     * @param r Radius of the cirle
     * @param cls Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    <A> List<A> getObjectsInRange(int x, int y, int r, Class<A> cls)
    {
        return collisionChecker.getObjectsInRange(x, y, r, (Class)cls);
    }

    /**
     * Returns the neighbours to the given location. This method only looks at
     * the logical location and not the extent of objects. Hence it is most
     * useful in scenarios where objects only span one cell.
     *
     * @param actor  The actor whose neighbours to locate
     * @param distance Distance in which to look for other objects
     * @param diag Is the distance also diagonal?
     * @param cls Class of objects to look for (null or Object.class will find
     *            all classes)
     * @return A collection of all neighbours found
     */
    <A> List<A> getNeighbours(Actor actor, int distance, boolean diag, Class<A> cls)
    {
        if(distance < 0) {
            throw new IllegalArgumentException("Distance must not be less than 0. It was: " + distance);
        }
        return collisionChecker.getNeighbours(actor, distance, diag, (Class)cls);
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
    <A> List<A> getObjectsInDirection(int x0, int y0, int angle, int length, Class<A> cls)
    {
        return collisionChecker.getObjectsInDirection(x0, y0, angle, length, (Class)cls);
    }

    /**
     * Get the height of the world in pixels.
     */
    int getHeightInPixels()
    {
        return height * cellSize;
    }

    /**
     * Get the width of the world in pixels.
     */
    int getWidthInPixels()
    {
        return width * cellSize;
    }

    /**
     * Converts the pixel location into a cell, rounding up.
     */    
    int toCellCeil(int pixel)
    {
        return (int) Math.ceil((double) pixel / cellSize);
    }

    /**
     * Converts the pixel location into a cell, rounding down.
     */
    @OnThread(Tag.Any)
    int toCellFloor(int pixel)
    {
        return (int) Math.floor((double) pixel / cellSize);
    }        

    /**
     * Returns the centre of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell centre in pixels.
     */
    double getCellCenter(int l)
    {
        double cellCenter = l * cellSize + cellSize / 2.;
        return cellCenter;
    }
    
    Collection<Actor> getObjectsAtPixel(int x, int y)
    {
        // This is a very naive and slow way of getting the objects at a given
        // pixel.
        // However, it makes sure that it doesn't use the collision checker
        // which we want to keep optimised.
        // It will be very slow with a lot of rotated objects. It is only used
        // when using the mouse to select objects, which is not a time-critical
        // task.
        
        List<Actor> result = new LinkedList<Actor>();
        TreeActorSet objects = getObjectsListInPaintOrder();
        for (Actor actor : objects) {
            Rect bounds = actor.getBoundingRect();
            if (x >= bounds.getX()  && x <= bounds.getRight() && y>=bounds.getY() && y<= bounds.getTop()) {
                if (actor.containsPoint(x, y)) {
                   result.add(actor);
                }
            }
        } 
      
        return result;
    }

    void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        collisionChecker.updateObjectLocation(object, oldX, oldY);
    }

    void updateObjectSize(Actor object)
    {
        collisionChecker.updateObjectSize(object);
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

    Actor getOneObjectAt(Actor object, int dx, int dy, Class<?> cls)
    {
        return collisionChecker.getOneObjectAt(object, dx, dy, (Class)cls);
    }

    Actor getOneIntersectingObject(Actor object, Class<?> cls)
    {
        return collisionChecker.getOneIntersectingObject(object, (Class) cls);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on itself or the World to avoid concurrent
     * modifications.
     */
    TreeActorSet getObjectsListInPaintOrder()
    {
        if (objectsInPaintOrder != null) {
            return objectsInPaintOrder;
        }
        else {
            return objectsDisordered;
        }
    }

    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. The world lock must be held while iterating
     * over this list.
     */
    TreeActorSet getObjectsListInActOrder()
    {
        if (objectsInActOrder != null) {
            return objectsInActOrder;
        }
        else {
            return objectsDisordered;
        }
    }
    
    void paintDebug(@SuppressWarnings("unused") Graphics g)
    {
        /*
         * g.setColor(Color.BLACK); g.drawString("# of Objects: " + objects.size(),
         * 50,50);
         */
        //collisionChecker.paintDebug(g); 
    }
    
    // =================================================
    //
    // PRIVATE MEHTHODS
    //
    // =================================================

    /**
     * Get the default image for objects of this class. May return null.
     */
    private GreenfootImage getClassImage()
    {
        Class<?> clazz = getClass();
        while (clazz != null) {
            GreenfootImage image = null;
            try {
                image = Actor.getDelegate().getImage(clazz.getName());
            }
            catch (Throwable e) {
                // Ignore exception and continue looking for images
            }
            if (image != null) {
                return image;
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

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
    
    private void addInActOrder(Actor object)
    {
        if(objectsInActOrder != null) {
            objectsInActOrder.add(object);
        }
    }

    private void addInPaintOrder(Actor object)
    {
        if(objectsInPaintOrder != null) {
            objectsInPaintOrder.add(object);
        }
    }
}
