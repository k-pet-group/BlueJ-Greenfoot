/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
import greenfoot.ActorVisitor;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**<
 * Very good when objects only span one cell. <br>
 * Good when most of the cells are occupied by objects. It has a store for each
 * cell location, so it could potentially take up a lot of memory if the world
 * is big. <br>
 * Very poor performance when objects span multiple cells (noOfObjects^2) <br>
 * TODO: check performance with objects that spans multiple cells.
 * 
 * @author Poul Henriksen
 */
public class GridCollisionChecker
    implements CollisionChecker
{
    class Cell
    {
        private HashMap classMap = new HashMap();
        private List objects = new ArrayList();

        public void add(Actor thing)
        {
            Class clazz = thing.getClass();
            List list = (List) classMap.get(clazz);
            if (list == null) {
                list = new ArrayList();
                classMap.put(clazz, list);
            }
            if(!list.contains(thing)) {
                list.add(thing);
            }
            if(!objects.contains(thing)) {
                objects.add(thing);
            }
        }

        public List get(Class cls)
        {
            return (List) classMap.get(cls);
        }

        public void remove(Actor object)
        {
            objects.remove(object);
            List classes = (List) classMap.get(object.getClass());
            if(classes != null) {
                classes.remove(object);
            }
        }

        public boolean isEmpty()
        {
            return objects.isEmpty();
        }

        /**
         * Returns all objects in this cell. Be carefull not to modify this
         * collection!!!
         * 
         * TODO For performace testing it has not been made imuttable...
         * 
         * @return
         */
        public List getAll()
        {
            return objects;
        }
    }

    /**
     * A grid world is made up of cells. Each location in the grid is
     * represented by a cell.
     * 
     * @author Poul Henriksen
     */
    private class GridWorld
    {
        protected Cell[][] world;

        public GridWorld(int width, int height)
        {
            world = new Cell[width][height];
        }

        public Cell get(int x, int y)
        {
            return world[x][y];
        }

        public void set(int x, int y, Cell cell)
        {
            world[x][y] = cell;
        }

        public int getWidth()
        {
            return world.length;
        }

        public int getHeight()
        {
            return world[0].length;
        }
    }

    private class WrappingGridWorld extends GridWorld
    {
        public WrappingGridWorld(int width, int height)
        {
            super(width, height);
        }

        public Cell get(int x, int y)
        {
            x = wrap(x, getWidth());
            y = wrap(y, getHeight());
            return world[x][y];
        }

        public void set(int x, int y, Cell cell)
        {
            x = wrap(x, getWidth());
            y = wrap(y, getHeight());
            world[x][y] = cell;
        }

        
    }

    public static class Statistics {
        
        private static final String format = "%15s%15s%15s%15s%15s";
        private long objectsAt;
        private long intersectionObjects;
        private long objectsInRange;
        private long neighbours;
        private long objectsInDirection;
        private long startTime = -1;

        public void incGetObjectsAt() {
            initStartTime();
            objectsAt++;
        }

        public void incGetIntersectingObjects() {
            initStartTime();
            intersectionObjects++;
        }

        public void incGetObjectsInRange() {
            initStartTime();
            objectsInRange++;
        }

        public void incGetNeighbours() {
            initStartTime();
            neighbours++;
        }

        public void incGetObjectsInDirection() {
            initStartTime();
            objectsInDirection++;
        }

        private void initStartTime()
        {
            if(startTime == -1) {
                startTime = System.currentTimeMillis();
            }
        }
        
        public String toString() {
            return String.format(format, new Object[] {
                    Long.valueOf(startTime),
                    Long.valueOf(objectsAt),
                    Long.valueOf(intersectionObjects),
                    Long.valueOf(objectsInRange),
                    Long.valueOf(neighbours),
                    Long.valueOf(objectsInDirection)
                    });
        }
        
        public static String headerString() {
            return String.format(format, new Object[] {
                    "startTime",
                    "objectsAt",
                    "intersection",
                    "oinRange",
                    "neighbours",
                    "inDirection"
                    });
        }
    }
    
    
    private Set objects;

    private static List emptyList = new Vector();
    private boolean wrap;

    private GridWorld world;
    
    private Statistics currentStats = new Statistics();
    private List allStats = new ArrayList();
    private static boolean PRINT_STATS = false;  
    

    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        this.wrap = wrap;
        objects = null;
        if (PRINT_STATS) {
            System.out.println(Statistics.headerString());
            objects = new TreeSet(new Comparator() {
                public int compare(Object arg0, Object arg1)
                {
                    int compare = arg0.hashCode() - arg1.hashCode();
                    if (compare == 0 && !arg0.equals(arg1)) {
                        System.err.println("Write the developers that they should fix the GridCollisionChecker!");
                    }
                    return compare;
                }
            });
        }
        else {
            objects = new HashSet();
        }
        
        if (wrap) {
            world = new WrappingGridWorld(width, height);
        }
        else {
            world = new GridWorld(width, height);
        }
    }

    /**
     * Adds a Actor to the world. <br>
     * If the coordinates of the object is outside the worlds bounds, an
     * exception is thrown.
     * 
     * @param thing
     *            The new object to add.
     */
    public synchronized void addObject(Actor thing)
        throws ArrayIndexOutOfBoundsException
    {
        testBounds(thing);

        if (!objects.contains(thing)) {
            Cell cell = world.get(thing.getX(), thing.getY());
            if (cell == null) {
                cell = new Cell();
                world.set(thing.getX(), thing.getY(), cell);
            }
            cell.add(thing);
            objects.add(thing);
        }
    }

    /**
     * @param thing
     */
    private void testBounds(Actor thing)
    {
        if (thing.getX() >= getWidth()) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() >= getHeight()) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }
        if (thing.getX() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }
    }

    /**
     * Returns all objects that contains the given location.
     * 
     * TODO: Bad performance. Can be improved MUCH if we only handle worlds
     * wehre objects spans a single cell.
     * 
     * @see Actor#contains(int, int)
     */
    public List getObjectsAt(int x, int y, Class cls)
    {
        if(wrap) {
            x = wrap(x, world.getWidth());
            y = wrap(y, world.getWidth());
        }
        List objectsThere = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            currentStats.incGetObjectsAt();
            Actor actor = (Actor) iter.next();
            if ((cls == null || cls.isInstance(actor)) && ActorVisitor.contains(actor,x - actor.getX(), y - actor.getY())) {
                objectsThere.add(actor);
            }
        }
        return objectsThere;
    }

    /**
     * Gets all objects within the given radius and of the given class (or
     * subclass).
     * 
     * 
     * The center of the circle is considered to be at the center of the cell.
     * Objects which have the center within the circle is considered to be in
     * range.
     * 
     * @param x
     *            The x-coordinate of the center
     * @param y
     *            The y-coordinate of the center
     * @param r
     *            The radius
     * @param cls
     *            Only objects of this class (or subclasses) are returned
     * @return
     */
    public List getObjectsInRange(int x, int y, int r, Class cls)
    {
        // TODO Optimise: if it is faster, run through all grid cells in the
        // distance instead. (based on number of objects vs. cells to run
        // through)
        Iterator iter = objects.iterator();
        List neighbours = new ArrayList();
        while (iter.hasNext()) {
            Object o = iter.next();
            currentStats.incGetObjectsInRange();
            if (cls == null || cls.isInstance(o)) {
                Actor g = (Actor) o;
                if (distance(x, y, g) <= r) {
                    neighbours.add(g);
                }
            }
        }
        return neighbours;
    }

    /**
     * Returns the shortest distance from the cell (center of cell ) to the
     * center of the greenfoot object.
     * 
     * @param x
     *            x-coordinate of the cell
     * @param y
     *            y-coordinate of the cell
     * @param actor
     * @return
     */
    private double distance(int x, int y, Actor actor)
    {
        // TODO should x,y be wrapped?
        double gx = actor.getX();
        double gy = actor.getY();
        double dx = Math.abs(gx - x);
        double dy = Math.abs(gy - y);

        if (wrap) {
            double dxWrap = getWidth() - dx;
            double dyWrap = getWidth() - dy;
            if (dx >= dxWrap) {
                dx = dxWrap;
            }
            if (dy >= dyWrap) {
                dy = dyWrap;
            }
        }

        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Removes the object
     * 
     * @param object
     *            the object to remove
     */
    public synchronized void removeObject(Actor object)
    {
        Cell cell = world.get(object.getX(), object.getY());
        if (cell != null) {
            cell.remove(object);
            if (cell.isEmpty()) {
                // Do we really want to do this?
                world.set(object.getX(), object.getY(), null);
            }
        } 
        objects.remove(object);
    }

    /**
     * Gets the width of the world.
     */
    public int getWidth()
    {
        return world.getWidth();
    }

    /**
     * Gets the height of the world.
     */
    public int getHeight()
    {
        return world.getHeight();
    }

    /**
     * Updates the location of the object in the world.
     * 
     * 
     * @param object
     *            The object which should be updated
     * @param oldX
     *            The old X location of the object
     * @param oldY
     *            The old Y location of the object
     */
    public void updateObjectLocation(Actor object, int oldX, int oldY)
    {
        Cell cell = world.get(oldX, oldY);
        if (cell != null) {
            cell.remove(object);
            if (cell.isEmpty()) {
                // Do we really want to do this?
                world.set(oldX, oldY, null);
            }

        }

        cell = world.get(object.getX(), object.getY());
        if (cell == null) {
            cell = new Cell();
            world.set(object.getX(), object.getY(), cell);
        }
        cell.add(object);

    }

    public void updateObjectSize(Actor object)
    {
        // we don't care, because we do not directly use the object size for
        // anything.
        return;
    }

    /**
     * 
     * This is very slow in this implementation as it checks against all objects
     * 
     */
    public List getIntersectingObjects(Actor actor, Class cls)
    {
        List intersecting = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            Actor element = (Actor) iter.next();
            currentStats.incGetIntersectingObjects();
            if (element != actor && ActorVisitor.intersects(actor, element) && (cls == null || cls.isInstance(element))) {
                intersecting.add(element);
            }
        }
        return intersecting;
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.collision.CollisionChecker#getNeighbours(int, boolean,
     *      java.lang.Class)
     */
    public List getNeighbours(Actor actor, int distance, boolean diag, Class cls)
    {
        int x = actor.getX();
        int y = actor.getY();
        List c = new ArrayList();
        if (diag) {
            for (int dx = x - distance; dx <= x + distance; dx++) {
                if (!wrap) {
                    if (dx < 0)
                        continue;
                    if (dx >= world.getWidth())
                        break;
                }
                for (int dy = y - distance; dy <= y + distance; dy++) {
                    if (!wrap) {
                        if (dy < 0)
                            continue;
                        if (dy >= world.getHeight())
                            break;
                    }
                    if (dx == x && dy == y)
                        continue;
                    Cell cell = world.get(dx, dy);
                    currentStats.incGetNeighbours();
                    if (cell != null) {
                        Collection found = cell.get(cls);
                        if (found != null) {
                            c.addAll(found);
                        }
                    }
                }
            }
        }
        else {
            int d = distance;
            int xStart = x;
            int yStart = y;

            int dyEnd = d;
            for (int dx = 0; dx <= d; dx++) {
                for (int dy = dx - d; dy <= dyEnd; dy++) {
                    int xPos = xStart + dx;
                    int xNeg = xStart - dx;
                    int yPos = yStart + dy;
                    if (!wrap) {
                        if (yPos >= world.getHeight()) {
                            break;
                        }
                        if (yPos < 0) {
                            continue;
                        }
                    }
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    currentStats.incGetNeighbours();
                    if (withinBounds(xPos, getWidth())) {
                        Cell cell = world.get(xPos, yPos);
                        if (cell != null) {
                            Collection found = cell.get(cls);
                            if (found != null) {
                                c.addAll(found);
                            }
                        }
                    }
                    if (dx != 0 && withinBounds(xNeg, getWidth())) {
                        Cell cell = world.get(xNeg, yPos);
                        if (cell != null) {
                            Collection found = cell.get(cls);
                            if (found != null) {
                                c.addAll(found);
                            }
                        }
                    }
                }
                dyEnd--;
            }
        }
        return c;
    }
    

    /**
     * Get all objects that lie on the line between the two points.<br>
     * 
     * Current implementation is using Bresenham. Don't know if that is the way
     * we want to do it, since it does NOT include every pixel that the line
     * intersects.
     * 
     * @param x1 Location of point 1
     * @param y1 Location of point 1
     * @param x2 Location of point 2
     * @param y2 Location of point 2
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
 /*   public List getObjectsAtLine(int x0, int y0, int x1, int y1, Class cls)
    {
        // TODO would we want to add them in order, so that the closest objects
        // are first in the list?

        // using Bresenham algo
        List result = new ArrayList();
        int dy = y1 - y0;
        int dx = x1 - x0;
        int stepx, stepy;

        if (dy < 0) {
            dy = -dy;
            stepy = -1;
        }
        else {
            stepy = 1;
        }
        if (dx < 0) {
            dx = -dx;
            stepx = -1;
        }
        else {
            stepx = 1;
        }
        dy <<= 1; // dy is now 2*dy
        dx <<= 1; // dx is now 2*dx

        result.addAll(getObjectsAt(x0, y0, cls));
        if (dx > dy) {
            int fraction = dy - (dx >> 1); // same as 2*dy - dx
            while (x0 != x1) {
                if (fraction >= 0) {
                    y0 += stepy;
                    fraction -= dx; // same as fraction -= 2*dx
                }
                x0 += stepx;
                fraction += dy; // same as fraction -= 2*dy

                result.addAll(getObjectsAt(x0, y0, cls));
            }
        }
        else {
            int fraction = dx - (dy >> 1);
            while (y0 != y1) {
                if (fraction >= 0) {
                    x0 += stepx;
                    fraction -= dy;
                }
                y0 += stepy;
                fraction += dx;
                result.addAll(getObjectsAt(x0, y0, cls));
            }
        }

        return result;
    }*/


    /**
     * Return all objects that intersect a straight line from this object at a
     * specified angle. The angle is clockwise relative to the current rotation
     * of the object. <br>
     * 
     * This implementation is likely to change. Currently it uses a Bresenham algorithm. 
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param angle The angle relative to current rotation of the object.
     * @param length How far we want to look (in cells)
     * @param cls Class of objects to look for (passing 'null' will find all
     *            objects).
     */
    public List getObjectsInDirection(int x, int y, int angle, int length, Class cls)
    {
        // The current implementation is using a Bresenham algorithm which is
        // probably not the one we want to use. The definition should probably
        // be the following:
        // Draw a line from the center of the start cell. EVERY object that is in a cell that this line intersects should be returned.
        
        
        //calculate end point
     /*   int x1 = x + (int) Math.round(length * Math.cos(Math.toRadians(angle)));
        int y1 = y + (int) Math.round(length * Math.sin(Math.toRadians(angle)));
        return getObjectsAtLine(x, y, x1, y1, cls);*/
        // using Bresenham algo
        List result = new ArrayList();
        double dy = (2 * Math.sin(Math.toRadians(angle)));
        double dx =  (2 * Math.cos(Math.toRadians(angle)));
        int lxMax = (int) Math.abs(Math.round(length * Math.cos(Math.toRadians(angle))));
        int lyMax = (int) Math.abs(Math.round(length * Math.sin(Math.toRadians(angle))));
        
        int stepx, stepy;

        if (dy < 0) {
            dy = -dy;
            stepy = -1;
        }
        else {
            stepy = 1;
        }
        if (dx < 0) {
            dx = -dx;
            stepx = -1;
        }
        else {
            stepx = 1;
        }

        result.addAll(getObjectsAt(x, y, cls));
        if (dx > dy) {
            double fraction = dy - (dx / 2); // same as 2*dy - dx
            for(int l=0; l< lxMax; l++) {
                currentStats.incGetObjectsInDirection();
                if (fraction >= 0) {
                    y += stepy;
                    fraction -= dx; // same as fraction -= 2*dx
                }
                x += stepx;
                fraction += dy; // same as fraction -= 2*dy

                result.addAll(getObjectsAt(x, y, cls));
            }
        }
        else {
            double fraction = dx - (dy / 2);
            for(int l=0; l< lyMax; l++) {
                currentStats.incGetObjectsInDirection();
                if (fraction >= 0) {
                    x += stepx;
                    fraction -= dy;
                }
                y += stepy;
                fraction += dx;
                result.addAll(getObjectsAt(x, y, cls));
            }
        }

        return result;
    }

    /**
     * 
     * Determines if x lies between 0 and width. If wrapping is on this will
     * always return true.
     * 
     * @param xPos
     * @return
     */
    private boolean withinBounds(int x, int width)
    {
        return wrap || (!wrap && x >= 0 && x < width);
    }
    
    /**
     * wraps the number x with the width
     */
    private int wrap(int x, int width)
    {
        int remainder = x % width;
        if (remainder < 0) {
            return width + remainder;
        }
        else {
            return remainder;
        }
    }


    public void startSequence()
    {
        if (PRINT_STATS) {
            System.out.println(currentStats);
        }
        allStats.add(currentStats);
        currentStats = new Statistics();
    }

    public List getObjects(Class cls)
    {
        List objectsThere = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            currentStats.incGetObjectsAt();
            Actor actor = (Actor) iter.next();
            if (cls == null || cls.isInstance(actor)) {
                objectsThere.add(actor);
            }
        }
        return objectsThere;
    }

    public List<Actor> getObjectsList()
    {
        List<Actor> l = new ArrayList<Actor>(objects);
        return l;
    }
    
    public Actor getOneObjectAt(Actor actor, int dx, int dy, Class cls)
    {
        List neighbours = getObjectsAt(dx, dy, cls);
        neighbours.remove(actor);
        if(!neighbours.isEmpty()) {
            return (Actor) neighbours.get(0);
        } else {
            return null;
        }
    }

    public Actor getOneIntersectingObject(Actor object, Class cls)
    {
        List intersecting = getIntersectingObjects(object, cls);
        if(!intersecting.isEmpty()) {
            return (Actor) intersecting.get(0);
        } else {
            return null; 
        }
    }

    public void paintDebug(Graphics g)
    {
        // TODO Auto-generated method stub
        
    }
}
