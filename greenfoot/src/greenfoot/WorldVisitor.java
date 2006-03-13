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
 * @version $Id: WorldVisitor.java 3816 2006-03-13 15:49:00Z polle $
 */
public class WorldVisitor
{
    public static int getWidthInPixels(GreenfootWorld w)
    {
        return w.getWidthInPixels();
    }

    public static int getHeightInPixels(GreenfootWorld w)
    {
        return w.getHeightInPixels();
    }

    public static int getCellSize(GreenfootWorld w)
    {
        return w.getCellSize();
    }
    
    public static Collection getObjectsAtPixel(GreenfootWorld w, int x, int y)
    {
        return w.getObjectsAtPixel(x, y);
    }

    /**
     * Used to indicate the start of an animation sequence. For use in the collision checker.
     * @see greenfoot.collision.CollisionChecker#startSequence()
     */
    public static void startSequence(GreenfootWorld w)
    {
        w.startSequence();
    }

    public static void paintDebug(GreenfootWorld world, Graphics g)
    {
        world.paintDebug(g);
    }
}