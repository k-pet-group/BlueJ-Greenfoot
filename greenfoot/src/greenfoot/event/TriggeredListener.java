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
     * 
     * @param e The event that was received last, and triggered the change to
     *            this listener. Can be null if it wasn't triggered by an event.
     */
    public void listeningStarted();

    /**
     * Fired when this event will stop receiving events.
     * 
     * @param e The event that was just received, and triggered the change away
     *            from listener. Can be null if it wasn't triggered by an event.
     */
    public void listeningEnded();
}
