package bluej.extensions.event;

import bluej.extensions.BPackage;
import bluej.pkgmgr.Package;

/**
 * Package events, like package OPEN/CLOSE are delivered using this event Class.
 *
 * @version $Id: PackageEvent.java 1807 2003-04-10 10:28:21Z damiano $
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
     * NOT to be used by Extension writer.
     */
    public PackageEvent (int i_eventId, Package pkg)
    {
        eventId = i_eventId;
        thisPackage = pkg;
    }

    /**
     * Returns the eventOd of this Event.
     */
    public int getEvent ()
      {
      return eventId;
      }

    /**
     * Returns the package being closed.
     * From a BPackage you can then get the BProject, if you need to.
     */
    public BPackage getPackage ()
      {
      return new BPackage (thisPackage);
      }

    /**
     * Returns a meaningful description of this event.
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