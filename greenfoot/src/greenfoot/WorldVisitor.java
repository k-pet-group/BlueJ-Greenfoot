package greenfoot;

import java.awt.Image;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldVisitor.java 3124 2004-11-18 16:08:48Z polle $
 */
public class WorldVisitor
{
    public static Image getCanvasImage(GreenfootWorld w)
    {
        return w.getCanvasImage();
    }
}