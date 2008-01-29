package greenfoot.event;

import java.util.EventListener;

/**
 * Listener to be notified when something changes from a valid state to invalid or vice versa.
 * 
 * @author Poul Henriksen
 */
public interface ValidityListener extends EventListener
{
    /**
     * Change from invalid to valid.
     */
    public void changedToValid(ValidityEvent e);
    
    /**
     * Change from valid to invalid.
     */
    public void changedToInvalid(ValidityEvent e);
}
