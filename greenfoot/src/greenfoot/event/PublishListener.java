package greenfoot.event;

import java.util.EventListener;

/**
 * Listener for recieving events when publishing a scenario to a website.
 * 
 * @author Poul Henriksen
 *
 */
public interface PublishListener extends EventListener
{
    public void statusRecieved(PublishEvent event);
    public void errorRecieved(PublishEvent event);
    public void progressMade(PublishEvent event);
}
