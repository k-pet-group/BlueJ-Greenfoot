package bluej.extensions.event;

import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;

/**
 * Package events, like package OPEN/CLOSE are delivered using this event Class.
 *
 * @version $Id: PackageEvent.java 1848 2003-04-14 10:24:47Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class PackageEvent implements BlueJExtensionEvent
{
    /**
     * Thie event occurs when a package has just been opened
     */
    public static final int PACKAGE_OPENED = 1;

    /**
     * Occurs when a package is just about to be closed so that the extension can take any necessary action before being voided.
     */ 
    public static final int PACKAGE_CLOSING = 2;

    private int eventId;
    private Package thisPackage;

    /**
     * Constructor for the PackageEvent.
     */
    public PackageEvent (int eventId, Package pkg)
    {
        this.eventId = eventId;
        thisPackage = pkg;
    }

    /**
     * Return the eventId of this Event.
     */
    public int getEvent ()
      {
      return eventId;
      }

    /**
     * Return the package associated with this event.
     */
    public BPackage getPackage ()
      {
      return new BPackage (thisPackage);
      }

    /**
     * Return a meaningful description of this event.
     */
    public String toString()
      {
      StringBuffer aRisul = new StringBuffer (500);

      aRisul.append("PackageEvent:");

      if ( eventId == PACKAGE_OPENED ) aRisul.append(" PACKAGE_OPENED");
      if ( eventId == PACKAGE_CLOSING ) aRisul.append(" PACKAGE_CLOSING");
      
      aRisul.append(" packageName="+thisPackage.getQualifiedName());
      
      return aRisul.toString();      
      }
}