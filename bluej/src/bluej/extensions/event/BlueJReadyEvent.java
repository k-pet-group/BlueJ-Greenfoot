package bluej.extensions.event;

/**
 * This class reports BlueJReady event.
 * This event is generated when BlueJ becomes ready.
 * Note: An extension loaded with a project (rather than with BlueJ) will 
 * not receive this event since BlueJ has already completed its 
 * initialization before the extension is loaded.
 * 
 * @version $Id: BlueJReadyEvent.java 1885 2003-04-25 08:53:48Z damiano $
 */

 /*
  * Author Damiano Bolla, University of Kent at Canterbury, January 2003
  */

public class BlueJReadyEvent implements ExtensionEvent 
  {


  /**
   * Returns a meaningful description of this event.
   */
  public String toString()
    {
    return "BlueJReadyEvent: ";
    }
  }
