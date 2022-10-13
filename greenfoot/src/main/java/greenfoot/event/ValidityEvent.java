/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.event;

import java.util.EventObject;

/**
 * Event used when something changes from being valid to invalid or vice versa.
 * 
 * @author Poul Henriksen
 */
public class ValidityEvent extends EventObject
{
    private String reason;

    /**
     * Create a new event.
     * 
     * @param source
     *            The source of the event.
     * @param reason
     *            The reason for the change of validity.
     */
    public ValidityEvent(Object source, String reason)
    {
        super(source);
        this.reason = reason;
    }

    /**
     * Get the reason why this event was created. Typically a message explaining
     * why a it is not valid.
     */
    public String getReason()
    {
        return reason;
    }
}
