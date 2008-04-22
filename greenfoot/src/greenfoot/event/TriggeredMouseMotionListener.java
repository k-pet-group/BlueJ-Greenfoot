package greenfoot.event;

import java.awt.event.MouseMotionListener;

/**
 * Interface for classes that wants to be able to receive MouseMotionEvents from the InputManager.
 * 
 * @author Poul Henriksen
 */
public interface TriggeredMouseMotionListener
    extends MouseMotionListener, TriggeredListener
{

}
