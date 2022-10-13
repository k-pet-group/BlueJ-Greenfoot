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

import bluej.Config;

/**
 * Event from publishing a scenario.
 * 
 * @author Poul Henriksen
 * 
 */
public class PublishEvent
{
    /** The publish returned an error */
    public final static int ERROR = 0;

    /**
     * A status message has been returned. For now, receiving a status message
     * means that it was a successful submit.
     */
    public final static int STATUS = 1;

    /**
     * Some upload progress has been made. Use getBytes() to find out how
     * much.
     */
    public final static int PROGRESS = 2;
    
    private String msg;
    private int bytes;

    private int type;

    /**
     * Construct a PublishEvent for an "upload complete" event.
     * @param type The event type.
     */
    public PublishEvent(int type)
    {
        this.type = type;
    }
    
    public PublishEvent(String msg, int type)
    {
        this.msg = msg;
        this.type = type;
    }

    public PublishEvent(int progress, int type)
    {
        this.type = type;
        this.bytes = progress;
    }
    
    public String getMessage()
    {
        return msg;
    }
    
    public int getBytes()
    {
        return bytes;
    }

    public int getType()
    {
        return type;
    }

    public String toString()
    {
        String s = super.toString() + " [";
        if (type == ERROR)
            s += Config.getString("publish.event.error");
        else if (type == STATUS)
            s += Config.getString("publish.event.status");
        s += msg + "]";
        return s;
    }
}
