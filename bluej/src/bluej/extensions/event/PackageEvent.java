/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extensions.event;

import bluej.extensions.*;
import bluej.pkgmgr.Package;

/**
 * This class encapsulates events on BlueJ packages.
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class PackageEvent implements ExtensionEvent
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
        return thisPackage.getBPackage();
    }

    /**
     * Returns a meaningful description of this event.
     */
    @Override
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
