package bluej.extensions.event;

/**
 * This interface allows you to listen for BlueJReady events.
 *
 * @version $Id: BlueJReadyEventListener.java 1870 2003-04-22 11:41:27Z damiano $
 */
public interface BlueJReadyEventListener
{
    /**
     * This method will be called when an event occurs.
     * Note that this method is called from a Swing-like dispatcher and therefore you must
     * return as quickly as possible. 
     * If a long operation must be performed you should start a Thread.
     */
    public void BlueJReady (BlueJReadyEvent event);
}
