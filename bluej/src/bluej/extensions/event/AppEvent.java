package bluej.extensions.event;


/**
 * This class encapsulates events that are at the whole application level.
 * @version $Id: AppEvent.java 1707 2003-03-14 06:37:51Z damiano $
 */

public class AppEvent extends BluejEvent 
  {
  /**
   * This event will be sent to the Extension when BlueJ is ready.
   * NOTE, TODO: If you load the extension with a Project you will not get this event.
   */
  public static final int APP_READY_EVENT=1;

  private int eventId;

  /**
   * Constructor, not for use by the Extensions.
   */
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