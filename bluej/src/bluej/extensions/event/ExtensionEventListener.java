package bluej.extensions.event;

/**
 * This interface allows you to listen for BlueJ events.
 * If you want to listen for BlueJ events you have to implement this interface
 * and register it using the <code>addExtensionEventListener()</code> 
 * method of the <code>BlueJ</code> class.
 *
 * @version $Id: ExtensionEventListener.java 1885 2003-04-25 08:53:48Z damiano $
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
