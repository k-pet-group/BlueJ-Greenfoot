package greenfoot;

import java.awt.Image;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to world methods that are package protected. We need some
 * package-protected methods in the world, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: WorldVisitor.java 3211 2004-12-02 13:12:54Z polle $
 */
public class WorldVisitor
{
    public static Image getCanvasImage(GreenfootWorld w)
    {
        return w.getCanvasImage();
    }
    
    public static int getWidthInPixels(GreenfootWorld w) {
        return w.getWidthInPixels();
    }
    public static int getHeightInPixels(GreenfootWorld w) {
        return w.getHeightInPixels();
    }
}