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
    private Class cls;
    
    public void init(int x, int y, Class cls) {
        this.x = x;
        this.y = y;
        this.cls = cls;
    }

    public boolean checkCollision(Actor actor) {
        if(cls == null) {
            return ActorVisitor.contains(actor, x - actor.getX(), y - actor.getY());
        } else {
            return cls.isInstance(actor) && ActorVisitor.contains(actor, x - actor.getX(), y - actor.getY());
        }
    }
    
}
