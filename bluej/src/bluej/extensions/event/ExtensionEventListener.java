package bluej.extensions.event;

/**
 * This interface allows you to listen for all BlueJ events by using a single listener.
 * Normally, extensions will use the specialised event types and listeners, but 
 * in some cases (e.g.) logging or testing extensions this overarching event type 
 * will be more appropriate.
 *
 * @version $Id: ExtensionEventListener.java 1904 2003-04-27 17:12:42Z iau $
 */
public interface ExtensionEventListener
{
    /**
     * This method will be called when an event occurs.
     * Note that this method is called from a Swing-like dispatcher and therefore you must
     * return as quickly as possible. 
     * If a long operation must be performed you should start a Thread.
     */
    public void eventOccurred (ExtensionEvent event);
}
