package greenfoot.collision;

import greenfoot.Actor;
import greenfoot.ActorVisitor;

/**
 * Checks collisions between GreenfooObjects.
 * 
 * @author Poul Henriksen
 *
 */
public class GOCollisionQuery implements CollisionQuery {
    
    private Class cls;
    private Actor compareObject;

    /**
     * Initialise.
     * 
     * @param cls The compared object must be of this class. If null, it is accepted.
     * @param actor Object to compare against other objects.
     */
    public void init(Class cls, Actor actor) {
        this.cls = cls;
        this.compareObject = actor;
    }        
    
    /**
     * Checks if the other object collides with this object and if it is of the given class.
     * 
     */
    public boolean checkCollision(Actor other) {   
        if(cls != null && !cls.isInstance(other)) {
            return false;
        }
        
        if(compareObject == null) {
            return true;
        }
        else if(ActorVisitor.intersects(compareObject, other)) {
            return true;
        } 
        return false;
    }     
}