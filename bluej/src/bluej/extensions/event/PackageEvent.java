package bluej.extensions.event;

import bluej.extensions.*;
import bluej.pkgmgr.Package;

/**
 * This class encapsulates events on BlueJ packages.
 *
 * @version $Id: PackageEvent.java 1869 2003-04-21 11:04:44Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class PackageEvent implements BlueJExtensionEvent
{
    /**
     * This event occurs when a package has just been opened.
     */
    public static final int PACKAGE_OPENED = 1;

    /**
     * This event occurs when a package is just about to be closed.
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
     * Returns the eventId of this event.
     */
    public int getEvent ()
      {
      return eventId;
      }

    /**
     * Returns the package associated with this event.
     */
    public BPackage getPackage ()
      {
      return ExtensionBridge.newBPackage (thisPackage);
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
