package bluej.extensions.event;

/**
 * This class encapsulates events that are at the whole application level.
 * 
 * @version $Id: ApplicationEvent.java 1807 2003-04-10 10:28:21Z damiano $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class ApplicationEvent implements BlueJExtensionEvent 
  {
  /**
   * This event will be sent to the Extension when BlueJ is ready.
   * warning: If you load the extension with a Project you will not get this event since
   * BlueJ has already completed its initialization when the project is loaded.
   */
  public static final int APP_READY_EVENT=1;

  private int eventId;

  /**
   * Constructs an ApplicationEvent
   */
  public ApplicationEvent(int anEventId)
    {
    eventId = anEventId;
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