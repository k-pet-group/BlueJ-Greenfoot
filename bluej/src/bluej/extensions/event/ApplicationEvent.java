package bluej.extensions.event;

/**
 * This class encapsulates BlueJ application events.
 * 
 * @version $Id: ApplicationEvent.java 1904 2003-04-27 17:12:42Z iau $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class ApplicationEvent implements ExtensionEvent 
  {
  /**
   * Event generated when the BlueJ application is initialised and ready.
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
