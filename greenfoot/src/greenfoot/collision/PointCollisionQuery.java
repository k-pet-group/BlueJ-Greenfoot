package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;

/**
 * Checks a collision against a point.
 *
 * @author Poul Henriksen
 */
public class PointCollisionQuery implements CollisionQuery{
    private int x;
    private int y;
    
    public void init(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean checkCollision(Actor actor) {
        return ActorVisitor.contains(actor, x - actor.getX(), y - actor.getY());
    }
    
}
