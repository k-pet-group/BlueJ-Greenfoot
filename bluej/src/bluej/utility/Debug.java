package bluej.utility;

/**
 * Class to handle debugging messages.
 * 
 * @author Michael Kolling
 * @version $Id: Debug.java 2273 2003-11-05 13:27:08Z mik $
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
     * Write out a BlueJ error message for debugging.
     * 
     * @param error The error message.
     */
    public static void reportError(String error)
    {
        message("Internal BlueJ error: " + error);
    }
}
