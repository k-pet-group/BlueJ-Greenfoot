package bluej.extensions.event;

/**
 * This interface allows you to listen for BlueJ events.
 * If you want to listen for extensions events you have tom implement this interface
 * and register it using the addBluejListener of the BlueJ Class.
 * @version $Id: BluejEventListener.java 1707 2003-03-14 06:37:51Z damiano $
 */
public interface BluejEventListener
{
    public void eventOccurred (BluejEvent event);
}