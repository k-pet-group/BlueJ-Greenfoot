package greenfoot.collision;

import greenfoot.GreenfootObject;

/**
 * This is an interface for doing low level collision checks with a GreenfootObject. 
 * 
 * @author Poul Henriksen
 */
public interface CollisionQuery {
     /**
      * Does the GreenfootObject collide with this collision checker?
      */
     public boolean checkCollision(GreenfootObject go);
}
