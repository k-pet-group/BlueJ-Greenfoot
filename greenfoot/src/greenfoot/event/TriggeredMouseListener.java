
package greenfoot.event;

import java.awt.event.MouseListener;

/**
 * Interface for classes that wants to be able to receive MouseEvents from the InputManager.
 * 
 * @author Poul Henriksen
 */
public interface TriggeredMouseListener
    extends MouseListener, TriggeredListener
{

}
