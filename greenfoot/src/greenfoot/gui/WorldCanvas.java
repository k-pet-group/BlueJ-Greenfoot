package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.ImageVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.util.GreenfootUtil;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * The visual representation of the world.
 * 
 * @author Poul Henriksen
 * @version $Id: WorldCanvas.java 5995 2008-12-02 15:30:49Z polle $
 */
public class WorldCanvas extends JPanel
    implements  DropTarget, Scrollable
{
    private World world;
    private DropTarget dropTargetListener;
    /** The actor being dragged. Null if no dragging. */ 
    private Actor dragActor;
    /** The current location where the object is dragged - in pixel coordinates relative to this canvas. */
    private Point dragLocation;
    /** Image used when dragging new actors on the world. Includes the drop shadow.*/
    private BufferedImage dragImage;
    
    public WorldCanvas(World world)
    {
        setWorld(world);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    
    /**
     * Sets the world that should be visualised by this canvas.
     * Call only from the Swing event thread.
     */
    public void setWorld(World world)
    {
        this.world = world;
        if (world != null) {
            this.setSize(getPreferredSize());
            revalidate();
            repaint();
        }
        else {
            this.setSize(0, 0);
        }
    }

    /**
     * Paints all the objects.
     * 
     * Must be synchronized on the World.lock.
     */
    public void paintObjects(Graphics g)
    {
        Set<Actor> objects = WorldVisitor.getObjectsListInPaintOrder(world);
        int paintSeq = 0;
        for (Iterator<Actor> iter = objects.iterator(); iter.hasNext();) {
            Actor thing = iter.next();
            int cellSize = WorldVisitor.getCellSize(world);

            GreenfootImage image = ActorVisitor.getDisplayImage(thing);
            if (image != null) {
                ActorVisitor.setLastPaintSeqNum(thing, paintSeq++);

                double halfWidth = image.getWidth() / 2.;
                double halfHeight = image.getHeight() / 2.;

                double xCenter = thing.getX() * cellSize + cellSize / 2.;
                int paintX = (int) Math.floor(xCenter - halfWidth);
                double yCenter = thing.getY() * cellSize + cellSize / 2.;
                int paintY = (int) Math.floor(yCenter - halfHeight);

                Graphics2D g2 = (Graphics2D) g;                
                
                float opacity = image.getTransparency() / 255f;
                Composite oldComposite = null;
                if(opacity < 1) {
                    // Don't bother with the composite if completely opaque.
                    if(opacity < 0) opacity = 0;
                    oldComposite = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                }
                
                AffineTransform oldTx = null;
                if(thing.getRotation() % 360 != 0) {
                    // don't bother transforming if it is not rotated at all.
                    oldTx = g2.getTransform();
                    g2.rotate(Math.toRadians(thing.getRotation()), xCenter, yCenter);
                }
                
                ImageVisitor.drawImage(image, g, paintX, paintY, this);
                
                // Restore the old state of the graphics
                if(oldTx != null) {
                    g2.setTransform(oldTx);
                }
                if(oldComposite != null) {
                    g2.setComposite(oldComposite);
                }
            }
        }

    }

    public void paintComponent(Graphics g)
    {         
        if (world == null) {
            return;
        }
        // We need to sync, so that objects are not added and removed when we
        // traverse the list.
        // But, we only try to get the lock for a brief period to avoid
        // deadlocks. A deadlock could otherwise happen if a modal dialog is
        // created from the user code in one of the act() methods.
        // We could do the sync only on the paintObjects, but that would mean
        // that the background will be reset and no objects painted, resulting
        // in a slightly broken look, if the user code is sleeping (with
        // Thread.sleep).
        try {
            if (world.lock.readLock().tryLock(World.READ_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    paintBackground(g);
                    paintObjects(g);
                    paintDraggedObject(g);
                    WorldVisitor.paintDebug(world, g);
                }
                finally {
                    world.lock.readLock().unlock();

                    // Wake up any threads waiting. For instance the
                    // World.repaint() call.
                    if (world.lock.writeLock().tryLock()) {
                        try {
                            world.lock.writeLock().newCondition().signalAll();
                        }
                        finally {
                            world.lock.writeLock().unlock();
                        }
                    }
                }
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    /**
     * If an object is being dragged, paint it.
     */
    private void paintDraggedObject(Graphics g)
    {
        if(dragImage != null) {
            int x = (int) dragLocation.getX();
            int y = (int) dragLocation.getY();
            int xCell =  WorldVisitor.toCellFloor(world, x);
            int yCell =  WorldVisitor.toCellFloor(world, y);
            int cellSize = WorldVisitor.getCellSize(world);
            x = (int) ((xCell + 0.5) * cellSize - dragActor.getImage().getWidth()/2);
            y = (int) ((yCell + 0.5) * cellSize - dragActor.getImage().getHeight()/2);
            
            g.drawImage(dragImage, x, y, null);            
        } 
    }

    /**
     * Paint the world background. This takes tiling into account: the
     * world image is painted either once or tiled onto this component.
     */
    public void paintBackground(Graphics g)
    {
        if (world != null) {            
            GreenfootImage backgroundImage = WorldVisitor.getBackgroundImage(world);
            if (backgroundImage != null) {
                if (world.isTiled()) {
                    paintTiledBackground(g, backgroundImage);
                }
                else {
                    ImageVisitor.drawImage(backgroundImage, g, 0, 0, this);
                }
            } else {
                Color oldColor = g.getColor();
                g.setColor(getBackground());
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                g.setColor(oldColor);
            }
        }
    }

    
    /**
     * Paint the world background image onto this component in tiles
     * so that it will fill the whole world size.
     */
    private void paintTiledBackground(Graphics g, GreenfootImage backgroundImage)
    {
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

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getPreferredSize()
    {
        Dimension size = new Dimension();
        if (world != null) {
            size.width = WorldVisitor.getWidthInPixels(world) ;
            size.height = WorldVisitor.getHeightInPixels(world) ;
        }
        return size;
    }
    
    
    public void setDropTargetListener(DropTarget dropTargetListener)
    {
        this.dropTargetListener = dropTargetListener;
    }
    

    public boolean drop(Object o, Point p)
    {
        clearDragInfo();
        if (dropTargetListener != null) {
            return dropTargetListener.drop(o, p);
        }
        else {
            return false;
        }

    }

    /**
     * If it is a new actor, that has not been added to the world yet, the
     * dragging is handled here.
     */
    public boolean drag(Object o, Point p)
    {
        if(o instanceof Actor && ((Actor) o).getWorld() == null) {   
            if(!getVisibleRect().contains(p)) {
                return false;
            }
            if(o != dragActor) {
                // It is the first time we are dragging this actor. Create the drag image.
                dragActor = (Actor) o;          
                dragImage = GreenfootUtil.createDragShadow(dragActor.getImage().getAwtImage());
            }
            dragLocation = p;
            repaint();
            return true;            
        }        
        else if (dropTargetListener != null) {
            return dropTargetListener.drag(o, p);
        }
        else {        
            return false;
        }
    }
    
    public void dragEnded(Object o)
    {
        clearDragInfo();
        if (dropTargetListener != null) {
            dropTargetListener.dragEnded(o);
        }
        
    }

    /** 
     * End the drag by setting all the drag information to null. And request repaint to update the graphics.
     */
    private void clearDragInfo()
    {
        dragLocation = null;
        dragActor = null;
        dragImage = null;
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        int cellSize = world.getCellSize();
        double scrollPos = 0;
        if(orientation == SwingConstants.HORIZONTAL) {
            //scrolling left
            if(direction < 0) {
                scrollPos = visibleRect.getMinX();
               
            }
            //scrolling right
            else if (direction > 0) {
                scrollPos = visibleRect.getMaxX();
            }
        } else {
            //scrolling up
            if(direction < 0) {
                scrollPos = visibleRect.getMinY();
            }
            //scrolling down
            else if (direction > 0) {
                scrollPos = visibleRect.getMaxY();
            }
        }
        int increment = Math.abs((int) Math.IEEEremainder(scrollPos, cellSize));
        if(increment == 0) {
            increment = cellSize;
        }
      
        return  increment;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
         return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    public boolean getScrollableTracksViewportWidth()
    {
        return false;
    }

    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    
}