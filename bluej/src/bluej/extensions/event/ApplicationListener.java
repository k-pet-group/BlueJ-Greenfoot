package bluej.extensions.event;

/**
 * This interface allows you to listen for application events.
 *
 * @version $Id: ApplicationListener.java 1904 2003-04-27 17:12:42Z iau $
 */
public interface ApplicationListener
{
    /**
     * This method will be called when the BlueJ application is initialised.
     * Warning: If you load an extension with a Project you will not get this event since
     * BlueJ has already completed its initialisation when the project is loaded.
     * Note that this method is called from a Swing-like dispatcher and therefore you must
     * return as quickly as possible. 
     * If a long operation must be performed you should start a Thread.
     */
    public void blueJReady (ApplicationEvent event);
}
