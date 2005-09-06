package greenfoot;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to GreenfootObject methods that are package protected. We need some
 * package-protected methods, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen 
 * @version $Id$
 */
public class GreenfootObjectVisitor
{
    public static void setLocationInPixels(GreenfootObject go, int dragBeginX, int dragBeginY) {
        go.setLocationInPixels(dragBeginX, dragBeginY);
    }
}
