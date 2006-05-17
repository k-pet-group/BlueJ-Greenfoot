package greenfoot;

import greenfoot.util.Version;

import java.awt.Graphics;
import java.util.Collection;
import java.util.List;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldVisitor.java 4283 2006-05-17 10:20:52Z davmac $
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

    public static int toCellFloor(World world, int x)
    {
        return world.toCellFloor(x);
    }
    
    /**
     * Return the version of the Greenfoot API.
     *
     */
    public static Version getApiVersion() {
        return World.VERSION;
    }
    
    /**
     * Get a possibly-live list of all objects in the world.
     */
    public static List<Actor> getObjectsList(World world)
    {
        return world.getObjectsList(); 
    }
}