package bluej.extensions.event;

public class AppEvent extends BJEvent 
  {
  // I really think that all event ID should go into one place.
  // Will do it ASAP
  public static final int APP_READY_EVENT=16;

  public AppEvent(int i_eventId)
    {
    super ( i_eventId, null );
    }
  }