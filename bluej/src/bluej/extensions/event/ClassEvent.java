package bluej.extensions.event;

import bluej.extensions.BClass;

/**
 * This class encapsulates events which occur on BlueJ classes.
 * 
 * @author Davin McCall
 * @version $Id: ClassEvent.java 4354 2006-06-13 04:27:35Z davmac $
 */
public class ClassEvent implements ExtensionEvent
{
    public static final int STATE_CHANGED = 0;
    public static final int CHANGING_NAME = 1;
    
    private int eventId;
    private BClass bClass;
    private boolean isCompiled;
    private String newName;
    
    /**
     * Construct a new ClassEvent object for a STATE_CHANGED event.
     * @param eventId    The event identifier (STATE_CHANGED)
     * @param isCompiled  Whether the class is compiled or not
     */
    public ClassEvent(int eventId, BClass bClass, boolean isCompiled)
    {
        this.eventId = eventId;
        this.isCompiled = isCompiled;
        this.bClass = bClass;
    }
    
    /**
     * Construct a new ClassEvent object for a CHANGING_NAME event.
     * @param eventId  The event identifier (CHANGING_NAME)
     * @param bClass   The class which is being renamed
     * @param newName  The new name of the class
     */
    public ClassEvent(int eventId, BClass bClass, String newName)
    {
        this.eventId = eventId;
        this.newName = newName;
        this.bClass = bClass;
    }
    
    /**
     * Get the event Id (one of STATE_CHANGED, CHANGING_NAME).
     */
    public int getEventId()
    {
        return eventId;
    }
    
    /**
     * Check whether the class for which the event occurred is compiled.
     * Valid for STATE_CHANGED event.
     */
    public boolean isClassCompiled()
    {
        return isCompiled;
    }
    
    /**
     * Get the BClass object identifying the class on which the event
     * occurred.
     */
    public BClass getBClass()
    {
        return bClass;
    }
    
    /**
     * Get the new class name. Valid for CHANGING_NAME event.
     */
    public String getNewName()
    {
        return newName;
    }
}
