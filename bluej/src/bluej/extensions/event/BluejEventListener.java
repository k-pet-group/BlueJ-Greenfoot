package bluej.extensions.event;

/**
 * If you want to listen for extensions events you have tom implement this interface
 * and register it using the addBluejListener of the BlueJ Class.<P>
 * Damiano
 */
public interface BluejEventListener
{
    public void eventOccurred (BluejEvent event);
}