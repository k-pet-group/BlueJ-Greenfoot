package greenfoot;

import java.awt.Graphics;
import java.awt.image.ImageObserver;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to Image methods that are package protected. We need some
 * package-protected methods in the Image, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ImageVisitor.java 3405 2005-06-03 15:10:56Z polle $
 */
public class ImageVisitor
{
    public static  void drawImage(GreenfootImage image, Graphics g, int x, int y, ImageObserver observer)
    {
        image.drawImage(g, x, y, observer);
    }
}
