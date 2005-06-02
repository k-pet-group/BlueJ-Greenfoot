package greenfoot.gui;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootWorld;
import greenfoot.ImageVisitor;
import greenfoot.WorldVisitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * The visual representation of the world
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldCanvas.java 3397 2005-06-02 11:11:08Z polle $
 */
public class WorldCanvas extends JComponent
    implements Observer, DropTarget
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private GreenfootWorld world;
    private DropTarget dropTargetListener;

    public WorldCanvas(GreenfootWorld world)
    {
        setWorld(world);
    }

    /**
     * Sets the world that should be visualied by this canvas
     * 
     * @param world
     */
    public void setWorld(GreenfootWorld world)
    {
        this.world = world;
        this.setSize(0, 0);
        if (world != null) {
            int width = WorldVisitor.getWidthInPixels(world);
            int height = WorldVisitor.getHeightInPixels(world);
            this.setSize(width, height);
        }
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
        List objects = world.getObjects();
        
        //we need to sync, so that objects are not added and removed when we traverse the list.
        synchronized (world) {
            for (Iterator iter = objects.iterator(); iter.hasNext();) {

                GreenfootObject thing = (GreenfootObject) iter.next();
                int cellSize = WorldVisitor.getCellSize(world);

                greenfoot.Image image = thing.getImage();
                if (image != null) {
                    double halfWidth = image.getWidth() / 2.;
                    double halfHeight = image.getHeight() / 2.;

                    double xCenter = thing.getX() * cellSize + cellSize / 2.;
                    int paintX = (int) Math.floor(xCenter - halfWidth);
                    double yCenter = thing.getY() * cellSize + cellSize / 2.;
                    int paintY = (int) Math.floor(yCenter - halfHeight);

                    Graphics2D g2 = (Graphics2D) g;
                    AffineTransform oldTx = g2.getTransform();
                    g2.rotate(Math.toRadians(thing.getRotation()), xCenter, yCenter);
                    ImageVisitor.drawImage(image, g, paintX, paintY, this);
                    g2.setTransform(oldTx);
                }
            }
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
        paintObjects(g);
    }

    private void paintBackground(Graphics g)
    {
        if (world != null) {
            g.setColor(getBackground());

            int width = WorldVisitor.getWidthInPixels(world);
            int height = WorldVisitor.getHeightInPixels(world);
            g.fillRect(0, 0, width, height);

            greenfoot.Image backgroundImage = world.getBackground();
            if (backgroundImage.isTiled()) {
                paintTiledBackground(g);
            }
            else if (backgroundImage != null) {
                ImageVisitor.drawImage(backgroundImage, g, 0, 0, this);
            }
        }

    }

    private void paintTiledBackground(Graphics g)
    {
        greenfoot.Image backgroundImage = world.getBackground();
        if (backgroundImage == null || world == null) {
            return;
        }
        int imgWidth = backgroundImage.getWidth();
        int imgHeight = backgroundImage.getHeight();

        int width = WorldVisitor.getWidthInPixels(world);
        int height = WorldVisitor.getHeightInPixels(world);

        int xTiles = (int) Math.ceil((double) width / imgWidth);
        int yTiles = (int) Math.ceil((double) height / imgHeight);

        for (int x = 0; x < xTiles; x++) {
            for (int y = 0; y < yTiles; y++) {
                ImageVisitor.drawImage(backgroundImage, g, x * imgWidth, y * imgHeight, this);
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
        Dimension size = new Dimension();
        if (world != null) {
            size.width = WorldVisitor.getWidthInPixels(world);
            size.height = WorldVisitor.getHeightInPixels(world);
        }
        return size;
    }

    /**
     * If we get an update, something has changed, and we do a repaint
     * 
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public void update(Observable o, Object arg)
    {
        repaint();
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