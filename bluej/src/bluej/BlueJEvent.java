package bluej;

import java.util.ArrayList;
import java.util.List;

/**
  * Class to handle (throw and deliver) BlueJ events. Event are defined
  * for things that might be caused by low level parts of the system which
  * other parts of the system might be interested in. Objects can register
  * themselves as event listeners. They then get notified of events.
  *
  * A BlueJEvent has one argument. The argument passed differs for every 
  * event type.
  *
  * Event types and their arguments:<PRE>
  *
  *  type       argument        sent when...
  *  -----------------------------------------------------------------------
  *  CREATE_VM      (unused)        creation of VM has started
  *
  *  CREATE_VM_DONE (unused)        creation of VM completed
  *
  *  METHOD_CALL    a String representing   an interactive method call
  *                      the call                was started
  *
  *  EXECUTION_STARTED  (unused)        VM execution started
  *
  *  EXECUTION_FINISHED (unused)        VM execution finished
  *
  *  GENERATING_DOCU    (unused)        documentation generation started
  *
  *  DOCU_GENERATED (unused)        documentation generation finished
  *
  *  DOCU_ABORTED   (unused)        documentation generation aborted
  *
  * </PRE>
  * @author Michael Kolling
  * @version $Id: BlueJEvent.java 2039 2003-06-19 06:03:24Z ajp $
  *
  */

public class BlueJEvent
{
    // BlueJ event types

    public static final int CREATE_VM           = 0;
    public static final int CREATE_VM_DONE      = CREATE_VM + 1;
    public static final int METHOD_CALL         = CREATE_VM_DONE + 1;
    public static final int EXECUTION_RESULT    = METHOD_CALL + 1;
    public static final int GENERATING_DOCU     = EXECUTION_RESULT + 1;
    public static final int DOCU_GENERATED      = GENERATING_DOCU + 1;
    public static final int DOCU_ABORTED        = DOCU_GENERATED + 1;


    // other variables

    private static List listeners = new ArrayList();

    /**
     * Raise a BlueJ event with an argument. All registered listeners
     * will be informed of this event.
     */
    public static void raiseEvent(int eventId, Object arg)
    {
        for(int i = listeners.size() - 1; i >= 0; i--) {
            BlueJEventListener listener = 
            (BlueJEventListener)listeners.get(i);
            listener.blueJEvent(eventId, arg);
        }
    }
    
    /**
     * Add a listener object. The object must implement the
     * BlueJEventListener interface.
     */
    public static void addListener(BlueJEventListener listener)
    {
        listeners.add(listener);
    }
    
    /**
     * Remove a listener object from the known listener set.
     */
    public static void removeListener(BlueJEventListener listener)
    {
        listeners.remove(listener);
    }
    
}

