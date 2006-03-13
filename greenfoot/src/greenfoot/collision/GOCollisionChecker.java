package greenfoot.collision;

import greenfoot.GreenfootObject;
import greenfoot.GreenfootObjectVisitor;

/**
 * Comparator used to check or collisions between GreenfooObjects.
 * 
 * @author Poul Henriksen
 *
 */
public class GOCollisionChecker{
    
    private Class cls;
    private GreenfootObject compareObject;

    /**
     * Initialise.
     * 
     * @param cls The compared object must be of this class. If null, it is accepted.
     * @param go Object to compare against other objects.
     */
    public void init(Class cls, GreenfootObject go) {
        this.cls = cls;
        this.compareObject = go;
    }        
    
    /**
     * Checks if the other object collides with this object and if it is of the given class.
     * 
     */
    public boolean checkCollision(GreenfootObject other) {
       
        if(compareObject == null && cls == null ) {
            return true;
        }       
        else if(cls == null && GreenfootObjectVisitor.intersects(compareObject, other)) {
            return true;
        }
        else if(compareObject == null && cls.isInstance(other)) {
            return true;
        }
        else if(cls.isInstance(other) && cls.isInstance(other)) {
            return true;
        }
        return false;
    }
     
}