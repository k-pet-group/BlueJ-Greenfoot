package bluej.extensions.event;

/**
 * This class encapsulates events that are at the whole application level.
 * 
 * @version $Id: AppEvent.java 1726 2003-03-24 13:33:06Z damiano $
 */

public class AppEvent extends BluejEvent 
  {
  /**
   * This event will be sent to the Extension when BlueJ is ready.
   * WARNING: If you load the extension with a Project you will not get this event since
   * BlueJ has already completed its initialization when the project is loaded.
   */
  public static final int APP_READY_EVENT=1;

  private int eventId;

  /**
   * NOT to be used by Extension writer.
   */
  public AppEvent(int i_eventId)
    {
    eventId = i_eventId;
    }

  /**
   * Returns the event type, one of the values defined.
   */
  public int getEvent ()
    {
    return eventId;
    }


  /**
   * Returns a meaningful description of this event.
   */
  public String toString()
    {
    if ( eventId == APP_READY_EVENT ) return "AppEvent: APP_READY_EVENT";

    return "AppEvent: UNKNOWN eventId="+eventId;
    }
  }