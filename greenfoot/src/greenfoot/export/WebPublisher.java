package greenfoot.export;

import javax.swing.event.EventListenerList;

import greenfoot.event.PublishEvent;
import greenfoot.event.PublishListener;
import greenfoot.export.gameserver.GameServer;



/**
 * Class to publish scenarios to a website. 
 * 
 * @author Poul Henriksen
 *
 */
public class WebPublisher extends GameServer
{
    private EventListenerList listenerList = new EventListenerList();
        
    /**
     * Create a new webpublisher. 
     * @see greenfoot.export.gameserver.GameServer#submit(String, String, String, String, String)
     */
    public WebPublisher() {

    }
       
    /**
     * Do not call this method. Use the listener interface instead.
     * 
     * @see #addPublishListener(PublishListener)
     */
    public void error(String s) {
        firePublishEvent(new PublishEvent(s, PublishEvent.ERROR));
    }

    /**
     * Do not call this method. Use the listener interface instead.
     * 
     * @see #addPublishListener(PublishListener)
     */
    public void status(String s) {
        firePublishEvent(new PublishEvent(s, PublishEvent.STATUS));
    }

    private void firePublishEvent(PublishEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == PublishListener.class) {
                if(event.getType() == PublishEvent.ERROR) {
                    ((PublishListener) listeners[i + 1]).errorRecieved(event);
                } 
                else if(event.getType() == PublishEvent.STATUS) {
                    ((PublishListener) listeners[i + 1]).statusRecieved(event);
                }
            }
        }
    }

    /**
     * Add a publishListener to listen for status and error messages.
     * 
     * @param l Listener to add
     */
    public void addPublishListener(PublishListener l)
    {
        listenerList.add(PublishListener.class, l);
    }
    
    /**
     * Remove a publishListener.
     * 
     * @param l  Listener to remove
     */
    public void removePublishListener(PublishListener l)
    {
        listenerList.remove(PublishListener.class, l);
    }
}
