package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.util.Circle;

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.List;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RObject;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * An Actor is an object that exists in the greenfoot world. Every Actor has a
 * location in the world, and an appearance (that is: an icon).
 * 
 * An Actor is not normally instantiated, but instead used as a superclass to
 * more specific objects in the world. Every object that is intended to appear
 * in the world must extend Actor. Subclasses can then define their own
 * appearance and behaviour.
 * 
 * One of the most important aspects of this class is the 'act' method. This
 * method is called when the 'Act' or 'Play' buttons are activated in the
 * greenfoot interface. The method here is empty, and subclasses normally
 * provide their own implementations.
 * 
 * @author Poul Henriksen
 * @version 1.1.0
 * @cvs-version $Id$
 */
public abstract class Actor
{

    /**
     * Most method calls are delegated to the delegate.
     */
    private ActorDelegate delegate;
    
    /**
     * Construct an Actor.
     * The object will have a default image.
     * 
     */
    public Actor() {
        delegate = new ActorDelegate(this);
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
    {}
   
    /**
     * Return the world that this object lives in.
     * 
     * @return The world.
     */
    public World getWorld()
    {
        return delegate.getWorld();
    }

    /**
     * Returns the image used to represent this Actor. This image can be
     * modified to change the object's appearance.
     * <p>
     * 
     * If you override this method, you should call super.getImage() before
     * doing anything else.
     * 
     * @return The object's image.
     */
    public GreenfootImage getImage()
    {
        return delegate.getImage();
    }

    /**
     * Set an image for this object from an image file. The file may be in jpeg,
     * gif or png format. The file should be located in the project directory.
     * 
     * @param filename The name of the image file.
     * @throws IllegalArgumentException If the image can not be loaded.
     */
    public void setImage(String filename)
        throws IllegalArgumentException
    {
        delegate.setImage(filename);
    }
    
    /**
     * Set the image for this object to the specified image.
     * 
     * @see #setImage(String)
     * @param image The image.
     */
    public void setImage(GreenfootImage image)
    {
        delegate.setImage(image);
    }
    

    /**
     * Set the rotation of the object. Rotation is expressed as a degree value,
     * range (0..359). Zero degrees is to the east. The angle increases
     * clockwise.
     * 
     * @param rotation The rotation in degrees.
     */
    public void setRotation(int rotation)
    {
        delegate.setRotation(rotation);
    }

    
    /**
     * Return the current rotation of the object. Rotation is expressed as a
     * degree value, range (0..359). Zero degrees is to the east. The angle
     * increases clockwise.
     * 
     * @see #setRotation(int)
     * 
     * @return The rotation in degrees.
     */
    public int getRotation()
    {
        return delegate.getRotation();
    }

    /**
     * Return the width of the object. The width is the number of cells that an
     * object's image overlaps horizontally.
     * 
     * @return The width of the object, or -1 if it has no image.
     * @throws IllegalStateException If there is no world instantiated.
     */
    public int getWidth()
    {
        return delegate.getWidth();
    }

    /**
     * Return the height of the object. The height is the number of cells that
     * an object's image overlaps vertically.
     * 
     * @return The height of the object, or -1 if it has no image.
     * @throws IllegalStateException If there is no world instantiated.
     */
    public int getHeight()
    {
        return delegate.getHeight();
    }
    
    /**
     * Return the x-coordinate of the object's current location. The value
     * returned is the horizontal index of the object's cell in the world.
     * 
     * @return The x-coordinate of the object's current location.
     * @throws IllegalStateException If the actor has not been added into a
     *             world.
     */
    public int getX()
        throws IllegalStateException
    {
        return delegate.getX();
    }

    /**
     * Return the y-coordinate of the object's current location. The value
     * returned is the vertical index of the object's cell in the world.
     * 
     * @return The y-coordinate of the object's current location
     * @throws IllegalStateException If the actor has not been added into a
     *             world.
     */
    public int getY()
    {
        return delegate.getY();
    }


    /**
     * Assign a new location for this object. The location is specified as a
     * cell index in the world.
     * 
     * If this method is overridden it is important to call this method with
     * super.setLocation(x,y) from the overriding method.
     * 
     * @param x Location index on the x-axis
     * @param y Location index on the y-axis
     * @throws IllegalStateException If the actor has not been added into a
     *             world.
     */
    public void setLocation(int x, int y)
    {
        delegate.setLocation(x, y);
    }

    /**
     * Check whether this object intersects with another given object.
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @return True if the object's intersect, false otherwise.
     */
    protected boolean intersects(Actor other)
    {
        return delegate.intersects(other);
    }

    /**
     * Return the neighbours to this object within a given distance. This method
     * considers only logical location, ignoring extent of the image. Thus, it
     * is most useful in scenarios where objects are contained in a single cell.
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
        return delegate.getNeighbours(distance, diagonal, cls);
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
        return delegate.getObjectsAtOffset(dx, dy, cls);
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
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     * @return An object at the given location, or null if none found.
     */
    protected Actor getOneObjectAtOffset(int dx, int dy, Class cls)
    {
        return delegate.getOneObjectAtOffset(dx, dy, cls);
    }

    /**
     * Return all objects within range 'r' around this object. An object is
     * within range if the distance between its centre and this object's centre
     * is less than or equal to r.
     * 
     * @param r Radius of the cirle (in pixels)
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    protected List getObjectsInRange(int r, Class cls)
    {

        return delegate.getObjectsInRange(r, cls);
    }

    /**
     * Return all the objects that intersect this object. This takes the
     * graphical extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    protected List getIntersectingObjects(Class cls)
    {
        return delegate.getIntersectingObjects(cls);
    }

    /**
     * Return an object that intersects this object. This takes the graphical
     * extent of objects into consideration. <br>
     * 
     * NOTE: Does not take rotation into consideration.
     * 
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    protected Actor getOneIntersectingObject(Class cls)
    {
        return delegate.getOneIntersectingObject(cls);
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
    void setLocationInPixels(int dragBeginX, int dragBeginY)
    {
        delegate.setLocationInPixels(dragBeginX, dragBeginY);
    }

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
     * should be considered.
     * <p>
     * 
     * NOTE: Does not take rotation into consideration. <br>
     * NOTE: No longer public, since no scenarios have used it so far, and we
     * might want to do it sligthly different if we want collision checkers to
     * only do most of the computation once pr. act.
     * 
     * @param dx The x-position relative to the location of the object
     * @param dy The y-position relative to the location of the object
     * @return True if the image contains the cell. If the object has no image,
     *         false will be returned.
     */
    boolean contains(int dx, int dy)
    {
        return delegate.contains(dx, dy);
    }

    /**
     * Get the bounding circle of the object. Taking into consideration that the
     * object can rotate.
     * 
     */
    Circle getBoundingCircle()
    {
        return delegate.getBoundingCircle();
    }

    /**
     * Get the bounding rectangle of the object. Taking into consideration that
     * the object can rotate.
     * 
     * @return A new Rect specified in pixels!
     */
    Rect getBoundingRect()
    {
        return delegate.getBoundingRect();
    }

    void setData(Object n)
    {
        delegate.setData(n);
    }

    Object getData()
    {
        // TODO Auto-generated method stub
        return delegate.getData();
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
        return delegate.getDisplayImage();
    }

    /**
     * Adds this object to a world at the given coordinate.
     */
    void addToWorld(int x, int y, World world)
    {
        delegate.addToWorld(x, y, world);
    }

    /**
     * Sets the world of this actor.
     * 
     * @param world
     */
    void setWorld(World world)
    {
        delegate.setWorld(world);
    }

    /**
     * Gets the x-coordinate of the left most cell that is occupied by the
     * object.
     * 
     */
    int getXMin()
    {
        return delegate.getXMin();
    }

    /**
     * Gets the y-coordinate of the top most cell that is occupied by the
     * object.
     * 
     * @throws IllegalStateException If there is no world instantiated.
     */
    int getYMin()
    {
        return delegate.getYMin();
    }

    // ============================================================================
    //  
    // Methods below here are different depending on how the project is run
    // (From Greenfoot IDE or StandAlone)
    //  
    // ============================================================================

    /**
     * Get the default image for objects of this class. May return null.
     */
    GreenfootImage getImage(Class clazz)
    {
        return GreenfootMain.getProjectProperties().getImage(clazz.getName());
    }

    /**
     * Get the active world.
     */
    World getActiveWorld()
    {
        return WorldHandler.getInstance().getWorld();
    }

    // ============================================================================
    //  
    // Object Transporting - between the two VMs
    //  
    // IMPORTANT: This code is duplicated in greenfoot.World!
    // ============================================================================

    /** Remote version of this class */
    private static RClass remoteObjectTracker;

    /** The object we want to get a remote version of */
    private static Object transportField;

    /** Lock to ensure that we only have one remoteObjectTracker */
    private static Object lock = new Object();
    // TODO The cached objects should be cleared at recompile.
    private static Hashtable cachedObjects = new Hashtable();

    /**
     * Gets the remote reference to the obj.
     * <p>
     * 
     * IMPORTANT: This code is duplicated in greenfoot.World!
     * 
     * @throws ClassNotFoundException
     * @throws RemoteException
     * @throws PackageNotFoundException
     * @throws ProjectNotOpenException
     * 
     */
    static RObject getRObject(Object obj)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, ClassNotFoundException
    {
        synchronized (lock) {
            RObject rObject = (RObject) cachedObjects.get(obj);
            if (rObject != null) {
                return rObject;
            }
            transportField = obj;
            rObject = getRemoteClass(obj).getField("transportField").getValue(null);
            cachedObjects.put(obj, rObject);
            return rObject;
        }
    }

    /**
     * "Forget" about a remote object reference. This is needed to avoid memory
     * leaks (worlds are otherwise never forgotten).
     * 
     * @param obj The object to forget
     */
    static void forgetRObject(Object obj)
    {
        synchronized (lock) {
            RObject rObject = (RObject) cachedObjects.remove(obj);
            if (rObject != null) {
                try {
                    rObject.removeFromBench();
                }
                catch (RemoteException re) {
                    throw new Error(re);
                }
                catch (ProjectNotOpenException pnoe) {
                    // shouldn't happen
                }
                catch (PackageNotFoundException pnfe) {
                    // shouldn't happen
                }
            }
        }
    }

    /**
     * Remove all objects from the remote object cache. This should be called
     * after a compilation.
     */
    static void clearObjectCache()
    {
        cachedObjects.clear();
    }

    /**
     * This method ensures that we have the remote (RClass) representation of
     * this class.
     * <p>
     * 
     * IMPORTANT: This code is duplicated in greenfoot.World!
     * 
     * @param obj
     * 
     */
    static private RClass getRemoteClass(Object obj)
    {
        if (remoteObjectTracker == null) {
            String rclassName = obj.getClass().getName();
            remoteObjectTracker = GreenfootMain.getInstance().getProject().getRClass(rclassName);
        }
        return remoteObjectTracker;
    }

}