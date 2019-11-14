/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2.event;

import bluej.extensions2.BPackage;
import bluej.pkgmgr.Package;
import javafx.event.Event;

/**
 * This class encapsulates events on BlueJ packages.
 *
 * @author Clive Miller, University of Kent at Canterbury, 2002
 * @author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class PackageEvent implements ExtensionEvent
{
    /**
     * Types of package events.
     */
    public static enum EventType
    {
        /**
         * Event generated when a package has just been opened.
         */
        PACKAGE_OPENED,

        /**
         * Event generated when a package is just about to be closed.
         */
        PACKAGE_CLOSING
    }

    private EventType eventType;
    private Package thisPackage;

    /**
     * @param eventType one of the {@link EventType} values for this PackageEvent.
     * @param pkg a {@link Package} object that raised this PackageEvent.
     */
    public PackageEvent (EventType eventType, Package pkg)
    {
        this.eventType = eventType;
        thisPackage = pkg;
    }

    /**
     * Gets the event type.
     *
     * @return The {@link EventType} value associated with this PackageEvent.
     */
    public EventType getEventType ()
    {
        return eventType;
    }

    /**
     * Gets the package on which the event occurred.
     *
     * @return A {@link BPackage} object wrapping the package on which this event occurred.
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

        switch (eventType)
        {
            case PACKAGE_OPENED:
                aRisul.append(" PACKAGE_OPENED");
                break;
            case PACKAGE_CLOSING:
                aRisul.append(" PACKAGE_CLOSING");
                break;
        }

        aRisul.append(" packageName="+thisPackage.getQualifiedName());

        return aRisul.toString();      
    }
}
