/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.utility;

import bluej.Config;

/**
 * Class to handle debugging messages.
 * 
 * @author Michael Kolling
 * @version $Id: Debug.java 6164 2009-02-19 18:11:32Z polle $
 */

public class Debug
{
    /**
     * Write out a debug message. This may go to a terminal, or to
     * a debug file, depending on external debug settings.
     * 
     * @param msg The message to be written.
     */
    public static final void message(String msg)
    {
        System.out.println(msg);
        System.out.flush();
    }
    
    /**
     * Write out a debug message to the debuglog file only.
     * 
     * @param msg The message to be written.
     */
    public static void log(String msg)
    {
        if (! Config.getPropString("bluej.debug").equals("true"))
            message(msg);
    }

    /**
     * Write out a BlueJ error message for debugging.
     * 
     * @param error The error message.
     */
    public static void reportError(String error)
    {
        message("Internal error: " + error);
    }

    /**
     * Write out a BlueJ error message for debugging.
     * 
     * @param error The error message.
     */
    public static void reportError(String error, Exception exc)
    {
        message("Internal error: " + error);
        message("Exception: " + exc);
    }
}
