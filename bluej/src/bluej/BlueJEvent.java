package bluej;

import bluej.utility.Debug;

import java.util.Vector;

/**
 ** @author Michael Kolling
 **
 ** Class to handle (throw and deliver) BlueJ events. Event are defined
 ** for things that might be caused by low level parts of the system which
 ** other parts of the system might be interested in. Objects can register
 ** themselves as event listeners. They then get notified of events.
 **
 ** A BlueJEvent has one argument. The argument passed differs for every 
 ** event type.
 **
 ** Event types and their arguments:
 **
 **  type             argument
 **  ------------------------------------------------------------
 **  CREATE_VM        (unused)
 **  CREATE_VM_DONE   (unused)
 **  BREAKPOINT	      the JdiThread object that hit the breakpoint
 **
 **
 **
 **/

public class BlueJEvent
{
    // BlueJ event types

    public static final int CREATE_VM = 0;
    public static final int CREATE_VM_DONE = CREATE_VM + 1;
    public static final int BREAKPOINT = CREATE_VM_DONE + 1;


    // other variables

    private static Vector listeners = new Vector();

    /**
     * Raise a BlueJ event with an argument. All registered listeners
     * will be informed of this event.
     */
    public static void raiseEvent(int eventId, Object arg)
    {
	for(int i = listeners.size() - 1; i >= 0; i--) {
	    BlueJEventListener listener = 
		(BlueJEventListener)listeners.elementAt(i);
	    listener.blueJEvent(eventId, arg);
	}
    }
	
    /**
     * Add a listener object. The object must implement the
     * BlueJEventListener interface.
     */
    public static void addListener(BlueJEventListener listener)
    {
	listeners.addElement(listener);
    }
	
    /**
     * Remove a listener object from the known listener set.
     */
    public static void removeListener(BlueJEventListener listener)
    {
	listeners.removeElement(listener);
    }
	
}

