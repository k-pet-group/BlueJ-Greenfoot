package bluej.extensions.event;

import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;

/**
 * Package events, like package OPEN/CLOSE are delivered using this event Class
 *
 * @version $Id: PackageEvent.java 1671 2003-03-10 08:58:32Z damiano $
 */
public class PackageEvent extends ExtEvent
{
    //  Occurs when a package has just been opened
    public static final int PACKAGE_OPENED = 1;

    // Occurs when a package is just about to be closed so that the extension can take any necessary action before being voided.
    public static final int PACKAGE_CLOSING = 2;


    private int eventId;
    private Package thisPackage;
    /**
     * Constructs a package event
     */
    public PackageEvent (int i_eventId, Package pkg)
    {
        eventId = i_eventId;
        thisPackage = pkg;
    }

    /**
     * @return the eventId of this class
     */
    public int getEvent ()
      {
      return eventId;
      }

    /**
     * @return the package on which the event happened
     */
    public BPackage getPackage ()
      {
      return new BPackage (thisPackage);
      }
}