package bluej.extensions.event;

/**
 * This class encapsulates events at the BlueJ application level.
 * 
 * @version $Id: ApplicationEvent.java 1851 2003-04-14 15:52:26Z iau $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class ApplicationEvent implements BlueJExtensionEvent 
  {
  /**
   * This event is generated when BlueJ becomes ready.
   * Note: An extension loaded with a project (rather than with BlueJ) will 
   * not receive this event since BlueJ has already completed its 
   * initialization before the extension is loaded.
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
   * Returns the eventId, one of the values defined.
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
