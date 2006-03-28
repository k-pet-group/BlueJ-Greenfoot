package greenfoot.collision;

import greenfoot.Actor;

/**
 * This is an interface for doing low level collision checks with a Actor. 
 * 
 * @author Poul Henriksen
 */
public interface CollisionQuery {
     /**
      * Does the Actor collide with this collision checker?
      */
     public boolean checkCollision(Actor go);
}
