package greenfoot.event;

/**
 * Interface for classes that wants to be able to receive events from the InputManager.
 * <p>
 * You should not use this interface directly, but rather one of the sub interfaces.
 * 
 * @see TriggeredKeyListener 
 * @see TriggeredMouseListener
 * @see TriggeredMouseMotionListener
 * 
 * @author Poul Henriksen
 */
public interface TriggeredListener
{
    /**
     * Fired when this event will start receiving events.
     * @param obj Optional object to be passed to the listener. Can be null.
     * 
     */
    public void listeningStarted(Object obj);

    /**
     * Fired when this event will stop receiving events.
     * 
     */
    public void listeningEnded();
}
