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
 * @version $Id: ImageVisitor.java 3238 2004-12-14 18:43:54Z polle $
 */
public class ImageVisitor
{
    public static  void drawImage(Image image, Graphics g, int x, int y, ImageObserver observer)
    {
        image.drawImage(g, x, y, observer);
    }
}
