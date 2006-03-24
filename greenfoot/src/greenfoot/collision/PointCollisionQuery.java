package greenfoot.collision;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootObjectVisitor;

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

    public boolean checkCollision(GreenfootObject go) {
        return GreenfootObjectVisitor.contains(go, x - go.getX(), y - go.getY());
    }
    
}
