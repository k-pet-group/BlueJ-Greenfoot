package greenfoot.collision;

import greenfoot.GreenfootObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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

        public void add(GreenfootObject thing)
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

        public void remove(GreenfootObject object)
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
    }

    private Set objects = new HashSet();

    private static List emptyList = new Vector();
    private boolean wrap;

    private GridWorld world;

    public void initialize(int width, int height, boolean wrap)
    {
        this.wrap = wrap;
        if (wrap) {
            world = new WrappingGridWorld(width, height);
        }
        else {
            world = new GridWorld(width, height);
        }
    }

    /**
     * Adds a GreenfootObject to the world. <br>
     * If the coordinates of the object is outside the worlds bounds, an
     * exception is thrown.
     * 
     * @param thing
     *            The new object to add.
     */
    public synchronized void addObject(GreenfootObject thing)
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
    private void testBounds(GreenfootObject thing)
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
     * @see GreenfootObject#contains(int, int)
     */
    public List getObjectsAt(int x, int y, Class cls)
    {
        List objectsThere = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            GreenfootObject go = (GreenfootObject) iter.next();
            if ((cls == null || cls.isInstance(go)) && go.contains(x - go.getX(), y - go.getY())) {
                objectsThere.add(go);
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
            if (cls.isInstance(o)) {
                GreenfootObject g = (GreenfootObject) o;
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
     * @param go
     * @return
     */
    private double distance(int x, int y, GreenfootObject go)
    {
        // TODO should x,y be wrapped?
        double gx = go.getX();
        double gy = go.getY();
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
    public synchronized void removeObject(GreenfootObject object)
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
    public void updateObjectLocation(GreenfootObject object, int oldX, int oldY)
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

    public void updateObjectSize(GreenfootObject object)
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
    public List getIntersectingObjects(GreenfootObject go, Class cls)
    {
        List intersecting = new ArrayList();
        for (Iterator iter = objects.iterator(); iter.hasNext();) {
            GreenfootObject element = (GreenfootObject) iter.next();
            if (element != go && go.intersects(element) && cls.isInstance(element)) {
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
    public List getNeighbours(int x, int y, int distance, boolean diag, Class cls)
    {
        //TODO: consider the class.
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
     * Get all objects that lie on the line between the two points.
     */
    public List getObjectsAtLine(int x1, int y1, int x2, int y2, Class cls)
    {
        // TODO Auto-generated method stub
        return null;
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
}
