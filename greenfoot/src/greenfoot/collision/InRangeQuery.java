package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.util.Circle;

/**
 * A collision query to check for actors within a certain range of a certain point
 * 
 * @author Davin McCall
 */
public class InRangeQuery implements CollisionQuery
{
    private int x;
    private int y;
    private int r;
    
    public void init(int x, int y, int r)
    {
        this.x = x;
        this.y = y;
        this.r = r;
    }
    
    public boolean checkCollision(Actor actor)
    {
        Circle c = ActorVisitor.getBoundingCircle(actor);
        int dx = c.getX() - x;
        int dy = c.getY() - y;
        int dist = (int) Math.sqrt(dx*dx + dy*dy);
        return (dist - c.getRadius()) <= r;
    }

}
