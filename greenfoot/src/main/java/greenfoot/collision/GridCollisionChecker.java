/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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

/**
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
        private HashMap<Class<?>,List<Actor>> classMap = new HashMap<Class<?>,List<Actor>>();
        private List<Actor> objects = new ArrayList<Actor>();

        public void add(Actor thing)
        {
            Class<?> clazz = thing.getClass();
            List<Actor> list = classMap.get(clazz);
            if (list == null) {
                list = new ArrayList<Actor>();
                classMap.put(clazz, list);
            }
            if(!list.contains(thing)) {
                list.add(thing);
            }
            if(!objects.contains(thing)) {
                objects.add(thing);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> List<T> get(Class<T> cls)
        {
            return (List<T>) classMap.get(cls);
        }

        public void remove(Actor object)
        {
            objects.remove(object);
            List<Actor> classes = classMap.get(object.getClass());
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
        public List<Actor> getAll()
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
        
        private static final String format = "%15s%15s%15s%15s%15s%15s";
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
    
    
    private Set<Actor> objects;

    private boolean wrap;

    private GridWorld world;
    private int cellSize;
    
    private Statistics currentStats = new Statistics();
    private List<Statistics> allStats = new ArrayList<Statistics>();
    private static boolean PRINT_STATS = false;  
    

    public void initialize(int width, int height, int cellSize, boolean wrap)
    {
        this.wrap = wrap;
        this.cellSize = cellSize;
        objects = null;
        if (PRINT_STATS) {
            System.out.println(Statistics.headerString());
            objects = new TreeSet<Actor>(new Comparator<Actor>() {
                public int compare(Actor arg0, Actor arg1)
                {
                    return arg0.hashCode() - arg1.hashCode();
                }
            });
        }
        else {
            objects = new HashSet<Actor>();
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
            int xpos = ActorVisitor.getX(thing);
            int ypos = ActorVisitor.getY(thing);
            Cell cell = world.get(xpos, ypos);
            if (cell == null) {
                cell = new Cell();
                world.set(xpos, ypos, cell);
            }
            cell.add(thing);
            objects.add(thing);
        }
    }

    private void testBounds(Actor thing)
    {
        int ax = ActorVisitor.getX(thing);
        int ay = ActorVisitor.getY(thing);
        
        if (ax >= getWidth()) {
            throw new ArrayIndexOutOfBoundsException(ax);
        }
        if (ay >= getHeight()) {
            throw new ArrayIndexOutOfBoundsException(ay);
        }
        if (ax < 0) {
            throw new ArrayIndexOutOfBoundsException(ax);
        }
        if (ay < 0) {
            throw new ArrayIndexOutOfBoundsException(ay);
        }
    }

    /*
     * TODO: Bad performance. Can be improved MUCH if we only handle worlds
     * wehre objects spans a single cell.
     * 
     * @see Actor#contains(int, int)
     */
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsAt(int x, int y, Class<T> cls)
    {
        if(wrap) {
            x = wrap(x, world.getWidth());
            y = wrap(y, world.getWidth());
        }
        List<T> objectsThere = new ArrayList<T>();
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            currentStats.incGetObjectsAt();
            Actor actor = iter.next();
            int ax = x * cellSize + cellSize / 2;
            int ay = y * cellSize + cellSize / 2;
            if ((cls == null || cls.isInstance(actor)) && ActorVisitor.containsPoint(actor, ax, y - ay)) {
                objectsThere.add((T) actor);
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
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjectsInRange(int x, int y, int r, Class<T> cls)
    {
        // TODO Optimise: if it is faster, run through all grid cells in the
        // distance instead. (based on number of objects vs. cells to run
        // through)
        Iterator<Actor> iter = objects.iterator();
        List<T> neighbours = new ArrayList<T>();
        while (iter.hasNext()) {
            Object o = iter.next();
            currentStats.incGetObjectsInRange();
            if (cls == null || cls.isInstance(o)) {
                Actor g = (Actor) o;
                if (distance(x, y, g) <= r) {
                    neighbours.add((T) g);
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
        double gx = ActorVisitor.getX(actor);
        double gy = ActorVisitor.getY(actor);
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
        int ax = ActorVisitor.getX(object);
        int ay = ActorVisitor.getY(object);
        Cell cell = world.get(ax, ay);
        if (cell != null) {
            cell.remove(object);
            if (cell.isEmpty()) {
                world.set(ax, ay, null);
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

    /*
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

        int ax = ActorVisitor.getX(object);
        int ay = ActorVisitor.getY(object);
        cell = world.get(ax, ay);
        if (cell == null) {
            cell = new Cell();
            world.set(ax, ay, cell);
        }
        cell.add(object);
    }

    public void updateObjectSize(Actor object)
    {
        // we don't care, because we do not directly use the object size for
        // anything.
        return;
    }

    /*
     * This is very slow in this implementation as it checks against all objects
     */
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getIntersectingObjects(Actor actor, Class<T> cls)
    {
        List<T> intersecting = new ArrayList<T>();
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor element = iter.next();
            currentStats.incGetIntersectingObjects();
            if (element != actor && ActorVisitor.intersects(actor, element) && (cls == null || cls.isInstance(element))) {
                intersecting.add((T) element);
            }
        }
        return intersecting;
    }

    /*
     * @see greenfoot.collision.CollisionChecker#getNeighbours(int, boolean,
     *      java.lang.Class)
     */
    public <T extends Actor> List<T> getNeighbours(Actor actor, int distance, boolean diag, Class<T> cls)
    {
        int x = ActorVisitor.getX(actor);
        int y = ActorVisitor.getY(actor);
        List<T> c = new ArrayList<T>();
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
                        Collection<T> found = cell.get(cls);
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
                            Collection<T> found = cell.get(cls);
                            if (found != null) {
                                c.addAll(found);
                            }
                        }
                    }
                    if (dx != 0 && withinBounds(xNeg, getWidth())) {
                        Cell cell = world.get(xNeg, yPos);
                        if (cell != null) {
                            Collection<T> found = cell.get(cls);
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
    public <T extends Actor> List<T> getObjectsInDirection(int x, int y, int angle, int length, Class<T> cls)
    {
        // The current implementation is using a Bresenham algorithm which is
        // probably not the one we want to use. The definition should probably
        // be the following:
        // Draw a line from the center of the start cell. EVERY object that is in a cell that this line intersects should be returned.
        
        // using Bresenham algo
        List<T> result = new ArrayList<T>();
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

    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getObjects(Class<T> cls)
    {
        List<T> objectsThere = new ArrayList<T>();
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            currentStats.incGetObjectsAt();
            Actor actor = iter.next();
            if (cls == null || cls.isInstance(actor)) {
                objectsThere.add((T) actor);
            }
        }
        return objectsThere;
    }

    public List<Actor> getObjectsList()
    {
        List<Actor> l = new ArrayList<Actor>(objects);
        return l;
    }
    
    public <T extends Actor> T getOneObjectAt(Actor actor, int dx, int dy, Class<T> cls)
    {
        List<T> neighbours = getObjectsAt(dx, dy, cls);
        neighbours.remove(actor);
        if(!neighbours.isEmpty()) {
            return neighbours.get(0);
        } else {
            return null;
        }
    }

    public <T extends Actor> T getOneIntersectingObject(Actor object, Class<T> cls)
    {
        List<T> intersecting = getIntersectingObjects(object, cls);
        if(!intersecting.isEmpty()) {
            return intersecting.get(0);
        } else {
            return null; 
        }
    }

    public void paintDebug(Graphics g)
    {
        // TODO Auto-generated method stub
        
    }
}
