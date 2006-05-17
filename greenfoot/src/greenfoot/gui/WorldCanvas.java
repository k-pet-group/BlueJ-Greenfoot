package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.ImageVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * The visual representation of the world
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldCanvas.java 4285 2006-05-17 10:35:32Z davmac $
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
     * Sets the world that should be visualied by this canvas
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
     * Paints all the objects
     * 
     * @param g
     */
    private void paintObjects(Graphics g)
    {
        if (world == null) {
            return;
        }
        //we need to sync, so that objects are not added and removed when we traverse the list.
        synchronized (world) {
            
            List objects = WorldVisitor.getObjectsList(world);
            
            for (Iterator iter = objects.iterator(); iter.hasNext();) {

                Actor thing = (Actor) iter.next();
                int cellSize = WorldVisitor.getCellSize(world);

                greenfoot.GreenfootImage image = ActorVisitor.getDisplayImage(thing);
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
        
        WorldVisitor.paintDebug(world, g);
    }

    
    /**
     * Paint the world background. This takes tiling into account: the
     * world image is painted either once or tiled onto this component.
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
        // TODO Auto-generated method stub
        return false;
    }

    public boolean getScrollableTracksViewportHeight()
    {
        // TODO Auto-generated method stub
        return false;
    }

}