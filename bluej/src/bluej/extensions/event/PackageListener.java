package bluej.extensions.event;

/**
 * This interface allows you to listen for when a package is being opened.
 *
 * @version $Id: PackageListener.java 1894 2003-04-25 09:53:08Z damiano $
 */
public interface PackageListener
{
    /**
     * This method will be called when a package is being opened.
     * If a long operation must be performed you should start a Thread.
     */
    public void packageOpened (PackageEvent event);

    /**
     * This method will be called when a package is being closed.
     * If a long operation must be performed you should start a Thread.
     */
    public void packageClosing (PackageEvent event);

}
