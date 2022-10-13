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
package greenfoot.collision;

import greenfoot.Actor;

import java.awt.Graphics;
import java.util.List;

/**
 * Interface for an implementation of a particular collision checker algorithm.
 * 
 * @author Poul Henriksen
 */
public interface CollisionChecker
{
    /**
     * This method is called when the collision checker should initialise.
     * 
     * @param width Width of the world
     * @param height Height of the world
     * @param cellSize size of one cell
     * @param wrap Whether the world wraps around the edges
     */
    public void initialize(int width, int height, int cellSize, boolean wrap);

    /**
     * Called when an object is added into the world
     */
    public void addObject(Actor actor);

    /**
     * Called when an object is removed from the world
     */
    public void removeObject(Actor object);

    /**
     * Called when an object has changed its location in the world.
     * 
     * @param oldX
     *            Old location
     * @param oldY
     *            Old location
     */
    public void updateObjectLocation(Actor object, int oldX, int oldY);

    /**
     * Called when an object has changed its size in the world.
     */
    public void updateObjectSize(Actor object);

    /**
     * Returns all objects that intersects the given location.
     * 
     * @param x   Cell X coordinate
     * @param y   Cell y coordinate
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls);

    /**
     * Returns all the objects that intersects the given object. This takes the
     * graphical extent of objects into consideration.
     * 
     * @param actor
     *            An Actor in the world
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     */
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls);

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
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls);

    /**
     * Returns the neighbours to the given location. This method only looks at
     * the logical location and not the extent of objects. Hence it is most
     * useful in scenarios where objects only span one cell.
     * 
     * @param x
     *            Location
     * @param y
     *            Location
     * @param distance
     *            Distance in which to look for other objects
     * @param diag
     *            Is the distance also diagonal?
     * @param cls
     *            Class of objects to look for (null or Object.class will find
     *            all classes)
     * @return A collection of all neighbours found
     */
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls);

    /**
     * Return all objects that intersect a straight line from this object at
     * a specified angle. The angle is clockwise relative to the current 
     * rotation of the object.  <br>
     * 
     * If the world is wrapped, the line will wrap around the edges.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @param angle The angle relative to current rotation of the object.
     * @param length How far we want to look (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all objects).
     */
    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls);
    
    /**
     * Get all the objects in the world, or all the objects of a particular class.
     * <p>
     * If a class is specified as a parameter, only objects of that class (or
     * its subclasses) will be returned.
     * <p>
     * 
     * @param cls Class of objects to look for ('null' will find all objects).
     * 
     * @return A list of objects.
     */
    public <T extends Actor> List<T> getObjects(Class<T> cls);
    
    /**
     * Returns the list of all objects. The returned list may be live (updated
     * when objects are added/removed from the collision checker) and should not
     * be directly modified.
     */
    public List<Actor> getObjectsList();
    
    /**
     * Methods that marks that a new sequence is started. A sequence in
     * greenfoot is most likely to start when a new act-iteration is begun
     * through all the objects. When calling methods interactively or using the
     * act button, that single invocation should be considered a sequence.
     * 
     * <br>
     * 
     * This method will initially be used only for performance testing, but
     * collision detection algorithms might also take advantage of this
     * information - especially if we will implement an all-at-once algortihm.
     */
    public void startSequence();

    /**
     * Find a single object which intersects the center point of the given cell.
     * 
     * @param <T>  The type of actor to be returned (normally inferred from cls)
     * @param object  The actor performing the query; this actor will not be returned
     * @param dx  The X co-ordinate of the cell to check
     * @param dy  The Y co-ordinate of the cell to check
     * @param cls  The type of object to return. If non-null, the returned object will be
     *             an instance of this class.
     * @return An actor intersecting the cell center, or null if no actor of the specified
     *         type (other than the querying actor) intersects the cell center. 
     */
    public <T extends Actor> T getOneObjectAt(Actor object, int dx, int dy, Class<T> cls);

    public <T extends Actor> T  getOneIntersectingObject(Actor object, Class<T> cls);

    public void paintDebug(Graphics g);


}