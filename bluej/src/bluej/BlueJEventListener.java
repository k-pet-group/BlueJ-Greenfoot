package bluej;

/**
 ** Interface for listeners to BlueJ events (see class BlueJEvent).
 **
 ** @author Michael Kolling
 **/
public interface BlueJEventListener
{
    /**
     * Called when a BlueJ event is raised. The event can be any BlueJEvent
     * type. The implementation of this method should check first whether
     * the event type is of interest an return immediately if it isn't.
     *
     * @param eventId  A constant identifying the event. One of the event id
     *                 constants defined in BlueJEvent.
     * @param arg      An event specific parameter. See BlueJEvent for 
     *                 definition.
     */
    void blueJEvent(int eventId, Object arg);
}
