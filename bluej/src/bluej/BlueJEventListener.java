package bluej;

/**
 ** Interface for listeners to BlueJ events (see class BlueJEvent).
 **
 ** @author Michael Kolling
 **/
public interface BlueJEventListener
{
    /**
     ** Called when any BlueJ event is raised.
     **
     ** @arg eventId  A constant identifying the event. One of the event id
     **               constants defined in BlueJEvent.
     ** @ ard param   An event specific parameter. See BlueJEvent for 
     **               definition.
     **/
    public void blueJEvent(int eventId, Object arg);
}
