package bluej.extensions.event;

/**
 * This interface allows you to listen for BlueJ events.
 * If you want to listen for BlueJ events you have to implement this interface
 * and register it using the <code>addBlueJExtensionEventListener()</code> 
 * method of the <code>BlueJ</code> class.
 *
 * @version $Id: BlueJExtensionEventListener.java 1851 2003-04-14 15:52:26Z iau $
 */
public interface BlueJExtensionEventListener
{
    /**
     * This method will be called when an event occurs.
     * Note that this method is called from a Swing-like dispatcher and therefore you must
     * return as quickly as possible. 
     * If a long operation must be performed you should start a Thread.
     */
    public void eventOccurred (BlueJExtensionEvent event);
}
