package greenfoot;

import greenfoot.collision.ibsp.Rect;
import greenfoot.platforms.ActorDelegate;
import greenfoot.util.Circle;

/**
 * Class that makes it possible for classes outside the greenfoot package to get
 * access to Actor methods that are package protected. We need some
 * package-protected methods, because we don't want them to show up
 * in the public interface visible to users.
 * 
 * @author Poul Henriksen 
 * @version $Id$
 */
public class ActorVisitor
{
    public static void setLocationInPixels(Actor actor, int dragBeginX, int dragBeginY) {
        actor.setLocationInPixels(dragBeginX, dragBeginY);
    }
    
   
    public static boolean contains(Actor actor, int dx, int dy)
    {
        return actor.contains(dx, dy);
    }

    public static boolean intersects(Actor actor, Actor other)
    {
        return actor.intersects(other);
    }
    
    public static Circle getBoundingCircle(Actor actor) 
    {
        return actor.getBoundingCircle();
    }
    
    public static Rect getBoundingRect(Actor actor) 
    {
        return actor.getBoundingRect();
    }
    
    public static void setData(Actor actor, Object n)
    {
        actor.setData(n);
    }
    
    public static Object getData(Actor actor)
    {
        return actor.getData();
    }
    
    /**
     * Get the display image for an actor. This is the last image that was
     * set using setImage(). The returned image should not be modified.
     * 
     * @param actor  The actor whose display image to retrieve
     */
    public static GreenfootImage getDisplayImage(Actor actor)
    {
        return actor.getDisplayImage();
    }

    
    public static void setDelegate(ActorDelegate instance)
    {
        Actor.setDelegate(instance);
    }
}
