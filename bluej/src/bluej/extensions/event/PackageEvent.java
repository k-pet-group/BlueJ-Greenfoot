package bluej.extensions.event;

import bluej.extensions.BPackage;

/**
 * This class represents a package event. It is provided to a <code>BJEventListener</code> 
 * that has registered an interest in receiving such events with 
 * a <CODE>BlueJ</CODE> object by using its <CODE>addBJEventListener</CODE> method.
 * @author Clive Miller
 * @version $Id: PackageEvent.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class PackageEvent extends BJEvent
{
    /**
     * Event id: Occurs when a package has just been opened. Use {@link bluej.extensions.event.PackageEvent#getPackage() getPackage} to
     * get a reference to the package.
     */
    public static final int PACKAGE_OPENED = 1;

    /**
     * Event id: Occurs when a package is just about to be closed so that the extension can take any necessary
     * action before being voided.
     */
    public static final int PACKAGE_CLOSING = 2;

    /**
     * Event id: A bitwise combination of PACKAGE_OPENED and PACKAGE_CLOSING
     */
    public static final int PACKAGE_EVENT = PACKAGE_OPENED | PACKAGE_CLOSING;

    /**
     * Constructs a package event
     */
    public PackageEvent (int event, BPackage pkg)
    {
        super (event, pkg);
    }
}