package bluej.extensions.event;

/**
 * If you want to listen for extensions events you have tom implement this interface
 * and registre it using the addExtListener of the BlueJ Class.<P>
 * Damiano
 */
public interface ExtEventListener
{
    public void eventOccurred (ExtEvent event);
}