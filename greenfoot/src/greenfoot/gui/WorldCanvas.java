package greenfoot.gui;

import greenfoot.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * The visual representation of the world
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldCanvas.java 5171 2007-08-28 03:01:03Z davmac $
 */
public class WorldCanvas extends JPanel
    implements  DropTarget, Scrollable
{
    private World world;
    private DropTarget dropTargetListener;

    public WorldCanvas(World world)
    {
        setWorld(world);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    /**
     * Sets the world that should be visualised by this canvas
     * 
     * @param world
     */
    public void setWorld(World world)
    {
        this.world = world;
        if (world != null) {
            int width = WorldVisitor.getWidthInPixels(world);
            int height = WorldVisitor.getHeightInPixels(world);
            this.setSize(width, height);
            revalidate();
            repaint();
        }
        else {
            this.setSize(0, 0);
        }
    }

    /**
     * Paints all the objects; must be called from a synchronized context.
     */
    private void paintObjects(Graphics g)
    {
        if (world == null) {
            return;
        }
        //we need to sync, so that objects are not added and removed when we traverse the list.
        synchronized (world) {
            Set<Actor> objects = WorldVisitor.getObjectsList(world);
            int paintSeq = 0;
            for (Iterator iter = objects.iterator(); iter.hasNext();) {
                Actor thing = (Actor) iter.next();
                int cellSize = WorldVisitor.getCellSize(world);

                greenfoot.GreenfootImage image = ActorVisitor.getDisplayImage(thing);
                if (image != null) {
                    ActorVisitor.setLastPaintSeqNum(thing, paintSeq++);
                    
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
    public synchronized void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (world == null) {
            return;
        }
        paintBackground(g);
        paintObjects(g);
        
        WorldVisitor.paintDebug(world, g);
    }

    
    /**
     * Paint the world background. This takes tiling into account: the
     * world image is painted either once or tiled onto this component.
     * Must be called from a synchronized context.
     */
    private void paintBackground(Graphics g)
    {
        if (world != null) {
            GreenfootImage backgroundImage = world.getBackground();
            if (backgroundImage != null) {
                if (world.isTiled()) {
                    paintTiledBackground(g, backgroundImage);
                }
                else {
                    ImageVisitor.drawImage(backgroundImage, g, 0, 0, this);
                }
            }
        }
    }

    
    /**
     * Print the world background image onto this component in tiles
     * so that it will the whole world size.
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

//    public Dimension getMaximumSize()
//    {
//    }

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