package greenfoot;

import greenfoot.util.Circle;

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
    
   
    public static boolean contains(GreenfootObject go, int dx, int dy)
    {
        return go.contains(dx, dy);
    }

    public static boolean intersects(GreenfootObject go, GreenfootObject other)
    {
        return go.intersects(other);
    }
    
    public static Circle getBoundingCircle(GreenfootObject go) 
    {
        return go.getBoundingCircle();
    }
    
    public static void setData(GreenfootObject go, Object n)
    {
        go.setData(n);
    }
    
    public static Object getData(GreenfootObject go)
    {
        return go.getData();
    }
}
