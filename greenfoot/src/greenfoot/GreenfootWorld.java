package greenfoot;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;

import javax.swing.ImageIcon;

/**
 * This class represents the object world, which is a 2 dimensional grid of
 * cells. Each cell can hold multiple obejcts. The world can be populated with
 * GreenfootObjects.
 * 
 * @see greenfoot.GreenfootObject
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootWorld.java 3124 2004-11-18 16:08:48Z polle $
 */
public class GreenfootWorld extends Observable
{

    private Map[][] world;
    private List objects = new ArrayList();

    /**
     * Map from classes to the size of the largest object of that class - used
     * for collision checks. TODO find a better way to do this. It is not fail
     * safe if the object size is changed after obejcts has been added to the
     * world Map from classes to sizes. Used for collision
     */
    private Map objectMaxSizes = new HashMap();

    private int cellWidth;
    private int cellHeight;

    private static Collection emptyCollection = new Vector();

    private Color backgroundColor = Color.WHITE;

    /** Image painted in the background. */
    private Image backgroundImage;
    private boolean tiledBackground;

    /** A canvas that can be used for additional drawing. */
    private Image canvasImage;

    private boolean gridEnabled = false;
    private Color gridColor = Color.BLACK;

    private int delay = 500;

    /** for timing the animation */
    private long lastDelay;

    /**
     * Create a new world with the given size.
     * 
     * @param worldWidth
     *            The width of the world. In cells
     * @param worldHeight
     *            The height of the world. In cells
     * @param cellWidth
     *            The width of a cell. In pixels
     * @param cellHeight
     *            The height of a cell. In pixels
     */
    public GreenfootWorld(int worldWidth, int worldHeight, int cellWidth, int cellHeight)
    {
        setWorldSize(worldWidth, worldHeight);
        setCellSize(cellWidth, cellHeight);
    }

    /**
     * Sets a new background color.
     * 
     * @param color
     *            The new background color
     */
    final public void setBackgroundColor(Color color)
    {
        this.backgroundColor = color;
        setChanged();
        notifyObservers(color);
    }

    /**
     * Sets the backgroundimage of the world
     * 
     * @param image
     *            The image
     */
    final public void setBackgroundImage(Image image)
    {
        backgroundImage = image;
        setChanged();
        notifyObservers(image);
    }

    /**
     * Tiles the backgroundimage to fill up the background.
     * 
     * @see #setBackgroundImage(Image)
     * @see #setBackgroundImage(String)
     * @param tiled
     *            Whether it should tile the background or not.
     */
    public void setTiledBackground(boolean tiled)
    {
        tiledBackground = tiled;
        update();
    }

    /**
     * Returns true if the background image is tiled. Otrherwise false is
     * returned.
     * 
     * @return Wherher the background image is tilled.
     */
    public boolean isTiledBackground()
    {
        return tiledBackground;
    }

    /**
     * Gets the background image
     * 
     * @return The background image
     */
    public Image getBackgroundImage()
    {
        return backgroundImage;
    }

    /**
     * Sets the backgroundimage of the world.
     * 
     * @see #setTiledBackground(boolean)
     * @see #setBackgroundImage(Image)
     * @param filename
     *            The file containing the image
     */
    final public void setBackgroundImage(String filename)
    {
        URL imageURL = this.getClass().getClassLoader().getResource(filename);
        ImageIcon imageIcon = new ImageIcon(imageURL);
        setBackgroundImage(imageIcon.getImage());
        update();
    }

    /**
     * Gets the width of the world.
     * 
     * @return Number of cells in the x-direction
     */
    public int getWorldWidth()
    {
        return world.length;
    }

    /**
     * Gets the height of the world.
     * 
     * @return Number of cells in the y-direction
     */
    public int getWorldHeight()
    {
        return world[0].length;
    }

    /**
     * Sets the size of the world. <br>
     * This will remove all objects from the world. TODO Maybe it shouldn't!
     */
    public void setWorldSize(int width, int height)
    {
        world = new Map[width][height];
        canvasImage = null;

        update();
    }

    /**
     * Sets the size of the cells.
     * 
     * @param width
     *            Width in pixels
     * @param height
     *            Height in pixels
     */
    public void setCellSize(int width, int height)
    {
        cellWidth = width;
        cellHeight = height;
        update();
    }

    /**
     * Adds a GreenfootObject to the world.
     * 
     * If the coordinates of the objects is outside the worlds bounds, an
     * exception is thrown.
     * 
     * @param thing
     *            The new object to add.
     */
    public synchronized void addObject(GreenfootObject thing)
        throws ArrayIndexOutOfBoundsException
    {
        if (thing.getX() >= getWorldWidth()) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() >= getWorldHeight()) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }
        if (thing.getX() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getX());
        }
        if (thing.getY() < 0) {
            throw new ArrayIndexOutOfBoundsException(thing.getY());
        }

        if (!objects.contains(thing)) {

            HashMap map = (HashMap) world[thing.getX()][thing.getY()];
            if (map == null) {
                map = new HashMap();
                world[thing.getX()][thing.getY()] = map;
            }
            Class clazz = thing.getClass();
            List list = (List) map.get(clazz);
            if (list == null) {
                list = new ArrayList();
                map.put(clazz, list);
            }
            list.add(thing);
            thing.setWorld(this);
            objects.add(thing);

            updateMaxSize(thing);

            update();
        }
    }

    /**
     * Updates the map of maximum object sizes with the given object (if
     * necessary).
     *  
     */
    private void updateMaxSize(GreenfootObject thing)
    {
        Class clazz = thing.getClass();
        Integer maxSize = (Integer) objectMaxSizes.get(clazz);
        int height = thing.getImage().getIconHeight();
        int width = thing.getImage().getIconWidth();
        int diag = (int) Math.sqrt(width * width + height * height);
        if (maxSize == null || maxSize.intValue() < diag) {
            objectMaxSizes.put(clazz, new Integer(diag));
        }
    }

    /**
     * Returns all objects of class cls that is at cell (x,y).
     * <p>
     * If checkImage is true, it will also use the contains() method of
     * GreenfootObejct to check if it actually intersects. <br>
     * If checkImage is false, it will only check the "anchor-point" of the
     * object.
     * 
     * 
     * @param x
     *            The cell x-coordinate
     * @param y
     *            The cell y-coordinate
     * @param cls
     *            The class
     * @param checkImage
     *            Whether to check if the image intersect
     * @return
     */
    public Collection getObjectsAtCell(int x, int y, Class cls, boolean checkImage)
    {

        // TODO and subclasses??
        if (!checkImage) {
            Map map = (Map) world[x][y];
            if (map != null) {
                Collection list = (Collection) map.get(cls);
                if (list == null) {
                    return emptyCollection;
                }
                else {
                    return list;
                }
            }
            else {
                return emptyCollection;
            }
        }
        else {
            Integer maxSizeI = (Integer) objectMaxSizes.get(cls);
            if (maxSizeI == null) {
                return emptyCollection;
            }
            List objectsThere = new ArrayList();
            int maxSize = maxSizeI.intValue();
            int xStart = (int) (x - Math.ceil((double) maxSize / (double) getCellWidth()) + 1);
            int yStart = (int) (y - Math.ceil((double) maxSize / (double) getCellHeight()) + 1);
            if (xStart < 0) {
                xStart = 0;
            }
            if (yStart < 0) {
                yStart = 0;
            }
            if (x >= getWorldWidth()) {
                x = getWorldWidth() - 1;
            }
            if (y >= getWorldHeight()) {
                y = getWorldHeight() - 1;
            }

            for (int xi = xStart; xi <= x; xi++) {
                for (int yi = yStart; yi <= y; yi++) {
                    Map map = world[xi][yi];
                    if (map != null) {
                        List list = (List) map.get(cls);
                        if (list != null) {
                            list = Collections.unmodifiableList(list);
                            for (Iterator iter = list.iterator(); iter.hasNext();) {
                                GreenfootObject go = (GreenfootObject) iter.next();
                                int xPixel = (x - xi) * cellWidth;
                                int yPixel = (y - yi) * cellHeight;
                                if (go.contains(xPixel, yPixel)) {
                                    objectsThere.add(go);
                                }
                            }
                        }
                    }
                }
            }
            return objectsThere;
        }
    }

    /**
     * Gets all the object of class cls at the given pixel location
     */
    public Collection getObjectsAtPixel(int x, int y, Class cls)
    {
        List objectsThere = new ArrayList();
        int xCell = (int) Math.floor((double) x / (double) getCellWidth());
        int yCell = (int) Math.floor((double) y / (double) getCellHeight());
        Collection objectsAtCell = getObjectsAtCell(xCell, yCell, cls, true);
        for (Iterator iter = objectsAtCell.iterator(); iter.hasNext();) {
            GreenfootObject go = (GreenfootObject) iter.next();
            if (go.contains(x - xCell, y - yCell)) {
                objectsThere.add(go);
            }
        }
        return objectsThere;
    }

    public Collection getObjectsAtPixel(int x, int y, boolean checkImage)
    {
        int xCell = (int) Math.floor((double) x / (double) getCellWidth());
        int yCell = (int) Math.floor((double) y / (double) getCellHeight());

        if (!checkImage) {
            //Collection list = (Collection) world[x][y].get(cls);

            Collection list = getObjectsAtCell(xCell, yCell);

            if (list == null) {
                return emptyCollection;
            }
            else {
                return list;
            }
        }
        else {
            Collection maxSizes = objectMaxSizes.values();
            int maxSize = 0;
            for (Iterator iter = maxSizes.iterator(); iter.hasNext();) {
                Integer element = (Integer) iter.next();
                if (element.intValue() > maxSize) {
                    maxSize = element.intValue();
                }
            }

            List objectsThere = new ArrayList();
            int xStart = (int) (xCell - Math.ceil((double) maxSize / (double) getCellWidth()) + 1);
            int yStart = (int) (yCell - Math.ceil((double) maxSize / (double) getCellHeight()) + 1);
            if (xStart < 0) {
                xStart = 0;
            }
            if (yStart < 0) {
                yStart = 0;
            }
            if (xCell >= getWorldWidth()) {
                xCell = getWorldWidth() - 1;
            }
            if (yCell >= getWorldHeight()) {
                yCell = getWorldHeight() - 1;
            }

            for (int xi = xStart; xi <= xCell; xi++) {
                for (int yi = yStart; yi <= yCell; yi++) {
                    Map map = world[xi][yi];
                    if (map != null) {
                        Collection list = getObjectsAtCell(xi, yi);
                        for (Iterator iter = Collections.unmodifiableCollection(list).iterator(); iter.hasNext();) {
                            GreenfootObject go = (GreenfootObject) iter.next();
                            int xiPixel = xi * cellWidth;
                            int yiPixel = yi * cellHeight;
                            if (go.contains(x - xiPixel, y - yiPixel)) {
                                objectsThere.add(go);
                            }
                        }
                    }
                }
            }
            return objectsThere;
        }

    }

    /**
     * @param x
     * @param y
     * @return
     */
    private Collection getObjectsAtCell(int x, int y)
    {
        Map map = (Map) world[x][y];
        if (map != null) {
            Collection values = map.values();
            Collection list = new ArrayList();
            for (Iterator iter = values.iterator(); iter.hasNext();) {
                List element = (List) iter.next();
                list.addAll(element);
            }

            return list;
        }
        else {
            return emptyCollection;
        }
    }

    /**
     * Removes the object from the world.
     * 
     * @param object
     *            the object to remove
     */
    public synchronized void removeObject(GreenfootObject object)
    {
        Map map = world[object.getX()][object.getY()];
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
                object.setWorld(null);
            }
        }
        objects.remove(object);
        update();
    }

    /**
     * Provides an Iterator to all the things in the world.
     *  
     */
    public synchronized Iterator getObjects()
    {
        //TODO: Make sure that the iterator returns things in the correct
        // paint-order (whatever that is)
        List c = new ArrayList();
        c.addAll(objects);
        return c.iterator();
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
    protected void updateLocation(GreenfootObject object, int oldX, int oldY)
    {
        Map map = world[oldX][oldY];
        Class clazz = object.getClass();
        if (map != null) {
            List list = (List) map.get(object.getClass());
            if (list != null) {
                list.remove(object);
            }
        }

        map = world[object.getX()][object.getY()];
        if (map == null) {
            map = new HashMap();
            world[object.getX()][object.getY()] = map;
        }
        List list = (List) map.get(clazz);
        if (list == null) {
            list = new ArrayList();
            map.put(clazz, list);
        }
        list.add(object);
        update();
    }

    /**
     * Returns the height of a cell
     * 
     * @return The height in pixels
     */
    public int getCellHeight()
    {
        return cellHeight;
    }

    /**
     * Returns the width of a cell
     * 
     * @return The width in pixels
     */
    public int getCellWidth()
    {
        return cellWidth;
    }

    /**
     * Sets the delay that is used in the animation loop
     * 
     * @param millis
     *            The delay in ms
     */
    public void setDelay(int millis)
    {
        this.delay = millis;
        update();
    }

    /**
     * Returns the delay.
     * 
     * @return The delay in ms
     */
    public int getDelay()
    {
        return delay;
    }

    /**
     * Pauses for a while
     */
    public void delay()
    {
        //TODO this functionality shouldn't really be here. Should it be
        // available to the user at all?
        try {
            long timeElapsed = System.currentTimeMillis() - this.lastDelay;
            long actualDelay = delay - timeElapsed;
            if (actualDelay > 0) {
                Thread.sleep(delay - timeElapsed);
            }
            this.lastDelay = System.currentTimeMillis();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the background color
     * 
     * @return The current background color
     */
    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    /**
     * Is the grid enabled?
     * 
     * @return True if grid is enabled
     */
    public boolean isGridEnabled()
    {
        return gridEnabled;
    }

    /**
     * Whether the grid should be enabled.
     * 
     * @param gridEnabled
     *            True if grid shouldbe enabled.
     */
    public void setGridEnabled(boolean gridEnabled)
    {
        this.gridEnabled = gridEnabled;
        setChanged();
        notifyObservers();
    }

    /**
     * Gets the color of the grid
     * 
     * @return the gridColor.
     */
    public Color getGridColor()
    {
        return gridColor;
    }

    /**
     * Sets a new color for the grid.
     * 
     * @param gridColor
     *            The gridColor
     */
    public void setGridColor(Color gridColor)
    {
        this.gridColor = gridColor;
        update();
    }

    /**
     * Returns a canvas that can be used to paint custom stuff. <br>
     * This will be painted on top of the background and below the
     * GreenfootObjects. <br>
     * update() must be called for changes to take effect.
     * 
     * @see #update()
     * @return A graphics2D that can be used for painting.
     */
    public Graphics2D getCanvas()
    {
        if (canvasImage == null) {
            canvasImage = new BufferedImage(getWorldWidth() * getCellWidth(), getWorldHeight() * getCellHeight(),
                    BufferedImage.TYPE_INT_ARGB);
        }
        return (Graphics2D) canvasImage.getGraphics();
    }

    /**
     * Used by the WorldCanvas.
     * 
     * @return
     */
    Image getCanvasImage()
    {
        return canvasImage;
    }

    /**
     * Refreshes the world. <br>
     * Should be called to see the changes after painting on the graphics
     * 
     * @see #getCanvas()
     * @see #getCanvas(int, int)
     */
    public void update()
    {
        setChanged();
        notifyObservers();
    }

    /**
     * Gets a canvas that can be used to draw on the world. The origo of the
     * canvas will be the center of the cell at the given coordinates.
     * 
     * @see #update()
     * @param x
     *            The cell at the x-location
     * @param y
     *            The cell at the y-location
     * @return A graphics2D that can be used for painting.
     */
    public Graphics2D getCanvas(int x, int y)
    {
        int centerX = (int) Math.floor(getCellWidth() * (x + .5));
        int centerY = (int) Math.floor(getCellHeight() * (y + .5));
        Graphics2D g = getCanvas();
        g.translate(centerX, centerY);
        return g;
    }
}