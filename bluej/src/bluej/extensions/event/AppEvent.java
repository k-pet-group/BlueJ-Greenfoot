package bluej.extensions.event;


/**
 * These are events that are on the whole application level.
 * Like when the one that happens when the whole system is ready.
 */

public class AppEvent extends ExtEvent 
  {
  public static final int APP_READY_EVENT=1;

  private int eventId;
  
  public AppEvent(int i_eventId)
    {
    eventId = i_eventId;
    }

  /**
   * get the event type, one of the static values defined.
   */
  public int getEvent ()
    {
    return eventId;
    }


  /**
   * returns a meaningful description of this object
   */
  public String toString()
    {
    if ( eventId == APP_READY_EVENT ) return "AppEvent: APP_READY_EVENT";

    return "AppEvent: UNKNOWN eventId="+eventId;
    }
  }