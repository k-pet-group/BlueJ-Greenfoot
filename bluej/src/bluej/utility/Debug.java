package bluej.utility;

import bluej.Config;

/**
 * Class to handle debugging messages.
 * 
 * @author Michael Kolling
 * @version $Id: Debug.java 3056 2004-10-21 00:58:44Z davmac $
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
        message("Internal BlueJ error: " + error);
    }
}
