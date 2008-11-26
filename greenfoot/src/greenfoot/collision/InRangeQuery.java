package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.util.Circle;

/**
 * A collision query to check for actors within a certain range of a certain
 * point
 * 
 * @author Davin McCall
 */
public class InRangeQuery
    implements CollisionQuery
{
    /** x-coordinate of the center of the circle. In pixels. */
    private int x;
    /** y-coordinate of the center of the circle. In pixels. */
    private int y;
    /** radius of the circle. In pixels. */
    private int r;

    /**
     * Initialise with the given circle. Units are in pixels!
     */
    public void init(int x, int y, int r)
    {
        this.x = x;
        this.y = y;
        this.r = r;
    }

    /**
     * Return true if the distance from some point on the actor to the center of
     * the circle, is less than or equal to the radius of the circle.
     */
    public boolean checkCollision(Actor actor)
    {
        int actorX = ActorVisitor.toPixel(actor, actor.getX());
        int actorY = ActorVisitor.toPixel(actor, actor.getY());   
        
		int dx = actorX - x;
        int dy = actorY - y;
        int dist = (int) Math.sqrt(dx * dx + dy * dy);

        return (dist) <= r;
    }

}
