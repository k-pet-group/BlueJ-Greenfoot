package greenfoot.gui;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootWorld;
import greenfoot.WorldVisitor;
import greenfoot.util.Location;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * The visual representation of the world
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldCanvas.java 3124 2004-11-18 16:08:48Z polle $
 */
public class WorldCanvas extends JComponent
    implements Observer, DropTarget
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private GreenfootWorld world;
    private Image backgroundImage;

    private DropTarget dropTargetListener;

    public WorldCanvas(GreenfootWorld world)
    {
        setWorld(world);
    }

    /**
     * Sets the world that shoudl be visualied by this canvas
     * 
     * @param world
     */
    public void setWorld(GreenfootWorld world)
    {
        this.world = world;
        this.setSize(getSizeInPixels());
        if (world != null) {
            setBackground(world.getBackgroundColor());
            setBackgroundImage(world.getBackgroundImage());

        }
    }

    public Dimension getSizeInPixels()
    {
        Dimension size = new Dimension();
        if (world != null) {
            size.width = world.getWorldWidth() * world.getCellWidth();
            size.height = world.getWorldHeight() * world.getCellHeight();
        }
        return size;
    }

    /**
     * Convenience method to translate form screen coordinates (pixels) into
     * gridCoordinates
     *  
     */
    public Location translateToGrid(int x, int y)
    {
        Location loc = new Location();
        if (world == null) {
            return loc;
        }
        loc.setX(x / world.getCellWidth());
        loc.setY(y / world.getCellHeight());
        return loc;
    }

    /**
     * Puts an image in the background of the world.
     *  
     */
    public void setBackgroundImage(Image image)
    {
        this.backgroundImage = image;

        MediaTracker m = new MediaTracker(this);
        m.addImage(image, 1);
        try {
            m.waitForID(1);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        repaint();
    }

    /**
     * Paints all the objects
     * 
     * @param g
     */
    private void paintObjects(Graphics g)
    {
        if (world == null) {
            return;
        }
        for (Iterator iter = world.getObjects(); iter.hasNext();) {
            try {
                GreenfootObject thing = (GreenfootObject) iter.next();

                Location loc = new Location(thing.getX(), thing.getY());
                loc.scale(world.getCellWidth(), world.getCellHeight());

                ImageIcon image = thing.getImage();
                if (image != null) {
                    Graphics2D g2 = (Graphics2D) g;

                    double halfWidth = image.getIconWidth() / 2.;
                    double halfHeight = image.getIconHeight() / 2.;
                    double rotateX = halfWidth + loc.getX();
                    double rotateY = halfHeight + loc.getY();
                    AffineTransform oldTx = g2.getTransform();
                    g2.rotate(Math.toRadians(thing.getRotation()), rotateX, rotateY);
                    image.paintIcon(this, g, loc.getX(), loc.getY());
                    g2.setTransform(oldTx);
                }
            }
            catch (NullPointerException e) {
                //Sometimes the world.getObjects has null objects... because of
                // the impl. which uses addAll()
            };
        }

    }

    private void paintGrid(Graphics g)
    {
        if (!world.isGridEnabled()) {
            return;
        }
        g.setColor(world.getGridColor());
        Dimension sizeInPixels = getSizeInPixels();
        for (int x = 0; x < sizeInPixels.width; x += world.getCellWidth()) {
            g.drawLine(x, 0, x, sizeInPixels.height);
        }
        for (int y = 0; y < sizeInPixels.height; y += world.getCellHeight()) {
            g.drawLine(0, y, sizeInPixels.width, y);
        }
    }

    /**
     * TODO optimize performance... double buffering?
     *  
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (world == null) {
            return;
        }
        paintBackground(g);
        paintCanvas(g);
        paintObjects(g);
        paintGrid(g);
    }

    private void paintCanvas(Graphics g)
    {
        Image canvasImage = WorldVisitor.getCanvasImage(world);
        if (canvasImage != null) {
            g.drawImage(canvasImage, 0, 0, this);
        }
    }

    private void paintBackground(Graphics g)
    {
        g.setColor(getBackground());
        g.fillRect(0, 0, (int) getSizeInPixels().getWidth(), (int) getSizeInPixels().getHeight());

        if (world.isTiledBackground()) {
            paintTiledBackground(g);
        }
        else if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, this);
        }

    }

    private void paintTiledBackground(Graphics g)
    {
        if (backgroundImage == null) {
            return;
        }
        int imgWidth = backgroundImage.getWidth(this);
        int imgHeight = backgroundImage.getHeight(this);

        int xTiles = (int) Math.ceil(getSizeInPixels().getWidth() / imgWidth);
        int yTiles = (int) Math.ceil(getSizeInPixels().getHeight() / imgHeight);

        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                g.drawImage(backgroundImage, x * imgWidth, y * imgHeight, this);
            }
        }

    }

    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    public Dimension getMinimumSize()
    {

        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        return getSizeInPixels();
    }

    /**
     * If we get an update, something has changed, and we do a repaint
     * 
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public void update(Observable o, Object arg)
    {
        if (o == world) {
            if (arg instanceof Color) {
                setBackground((Color) arg);
            }
            else if (arg instanceof Image) {
                setBackgroundImage((Image) arg);
            }
            repaint();
        }
    }

    public void setDropTargetListener(DropTarget dropTargetListener)
    {
        this.dropTargetListener = dropTargetListener;
    }

    public boolean drop(Object o, Point p)
    {
        if (dropTargetListener != null) {
            return dropTargetListener.drop(o, p);
        }
        else {
            return false;
        }

    }

    public boolean drag(Object o, Point p)
    {
        if (dropTargetListener != null) {
            return dropTargetListener.drag(o, p);
        }
        else {
            return false;
        }
    }

    public void dragEnded(Object o)
    {
        if (dropTargetListener != null) {
            dropTargetListener.dragEnded(o);
        }
    }

}