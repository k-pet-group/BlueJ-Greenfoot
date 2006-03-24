package greenfoot.collision;

import greenfoot.GreenfootObject;

/**
 *  Checks if a greenfoot object is within a specific neighbourhood.
 *
 * @author Poul Henriksen
 */
public class NeighbourCollisionQuery implements CollisionQuery{

    private int x;
    private int y;
    private int distance;
    private boolean diag;
    private Class cls;
    
    public void init(int x, int y, int distance, boolean diag, Class cls) 
    {
        if(distance < 0) {
            throw new IllegalArgumentException("Distance must not be less than 0. It was: " + distance);
        }
        this.x = x;
        this.y = y;
        this.distance = distance;
        this.diag = diag;
        this.cls = cls;
    }

    public boolean checkCollision(GreenfootObject go) {
        if(cls != null && !cls.isInstance(go)) {
            return false;
        }
        if(go.getX() == x && go.getY() == y) {
            return false;
        }       
        if(diag) {
            int x1 = x - distance;            
            int y1 = y - distance;            
            int x2 = x + distance;            
            int y2 = y + distance;
            return (go.getX() >= x1 && go.getY() >=y1 && go.getX() <= x2 && go.getY() <=y2);
        } else {
            int dx = Math.abs(go.getX() - x);
            int dy = Math.abs(go.getY() - y);
            return ((dx+dy) <= distance);            
        }
    }

}
