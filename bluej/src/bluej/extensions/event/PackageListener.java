package bluej.extensions.event;

/**
 * This interface allows you to listen for events on BlueJ packages.
 *
 * @version $Id: PackageListener.java 1904 2003-04-27 17:12:42Z iau $
 */
public interface PackageListener
{
    /**
     * This method will be called when a package has been opened.
     * If a long operation must be performed you should start a Thread.
     */
    public void packageOpened (PackageEvent event);

    /**
     * This method will be called when a package is about to be closed.
     * If a long operation must be performed you should start a Thread.
     */
    public void packageClosing (PackageEvent event);

}
