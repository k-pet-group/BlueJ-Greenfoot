package bluej;

import bluej.utility.Debug;

/**
 ** @author Michael Kolling
 **
 ** Class to handle (throw and deliver) BlueJ events. Event are defined
 ** for things that might be caused by low level parts of the system which
 ** other parts of the system might be interested in. Objects can register
 ** themselves as event listeners. They then get notified of events.
 **/

public class BlueJEvent
{
    public static final String nl = System.getProperty("line.separator");

    private static boolean initialised = false;

    /**
     * Initialisation of BlueJ event handling. Must be called before first
     * use.
     */
    public static void initialise()
    {
	if(initialised)
	    return;

	initialised = true;

	// 

    } // initialise

    /**
     * 
     * 
     */
    public static void raiseEvent(int eventId, Object arg)
    {
    }
	
    /**
     * Add a listener object. The object must implement the
     * BlueJEventListener interface.
     */
    public static void addListener(BlueJEventListener listener)
    {
	listeners.add(listener);
    }
	
}

