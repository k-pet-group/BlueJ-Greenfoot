package greenfoot;

import java.awt.Graphics;
import java.util.Collection;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldVisitor.java 5960 2008-11-19 15:15:47Z polle $
 */
public class WorldVisitor
{
    public static int getWidthInPixels(World w)
    {
        return w.getWidthInPixels();
    }

    public static int getHeightInPixels(World w)
    {
        return w.getHeightInPixels();
    }

    public static int getCellSize(World w)
    {
        return w.getCellSize();
    }
    
    public static Collection getObjectsAtPixel(World w, int x, int y)
    {
        return w.getObjectsAtPixel(x, y);
    }

    /**
     * Used to indicate the start of an animation sequence. For use in the collision checker.
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    public static void startSequence(World w)
    {
        w.startSequence();
    }

    public static void paintDebug(World world, Graphics g)
    {
        world.paintDebug(g);
    }
    
    /**
     * Convert a location in pixels into a cell location
     */
    public static int toCellFloor(World world, int x)
    {
        return world.toCellFloor(x);
    }
    
    /**
     * Returns the center of the cell. It should be rounded down with Math.floor() if the integer version is needed.
     * @param l Cell location.
     * @return Absolute location of the cell center in pixels.
     */
    public static  double getCellCenter(World w, int c)
    {
        return w.getCellCenter(c);
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on itself or the World to avoid concurrent
     * modifications.
     */
    public static TreeActorSet getObjectsListInPaintOrder(World world)
    {
        return world.getObjectsListInPaintOrder(); 
    }
    
    /**
     * Get the list of all objects in the world. This returns a live list which
     * should not be modified by the caller. If iterating over this list, it
     * should be synchronized on itself or the World to avoid concurrent
     * modifications.
     */
    public static TreeActorSet getObjectsListInActOrder(World world)
    {
        return world.getObjectsListInActOrder(); 
    }

    /**
     * Get the background image for the world, but without initialising it if it is not yet created.
     * 
     * @return Background of the world or null if not create yet.
     */
    public static GreenfootImage getBackgroundImage(World world)
    {
        return world.getBackgroundNoInit();
    }

}