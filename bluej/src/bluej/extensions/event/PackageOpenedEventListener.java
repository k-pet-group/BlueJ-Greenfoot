package bluej.extensions.event;

/**
 * This interface allows you to listen for when a package is being opened.
 *
 * @version $Id: PackageOpenedEventListener.java 1873 2003-04-22 12:50:07Z damiano $
 */
public interface PackageOpenedEventListener
{
    /**
     * This method will be called when a package is being opened.
     * If a long operation must be performed you should start a Thread.
     */
    public void packageOpened (PackageEvent event);
}
