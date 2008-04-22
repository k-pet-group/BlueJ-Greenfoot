package greenfoot.event;

import java.awt.event.KeyListener;

/**
 * Interface for classes that wants to be able to receive KeyEvents from the InputManager.
 * 
 * @author Poul Henriksen
 */
public interface TriggeredKeyListener
    extends KeyListener, TriggeredListener
{

}
