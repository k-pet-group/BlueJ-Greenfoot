package bluej.extensions.event;

/**
 * This interface allows you to listen for BlueJ events.
 * If you want to listen for extensions events you have tom implement this interface
 * and register it using the addBluejListener of the BlueJ Class.
 *
 * @version $Id: BluejEventListener.java 1726 2003-03-24 13:33:06Z damiano $
 */
public interface BluejEventListener
{
    /**
     * This method will be called when one event is available.
     * Note that this is called inside a swing like dispatcher and therefore you must
     * return as quick as possible. 
     * If long operation must be performed they should be a Thread.
     */
    public void eventOccurred (BluejEvent event);
}