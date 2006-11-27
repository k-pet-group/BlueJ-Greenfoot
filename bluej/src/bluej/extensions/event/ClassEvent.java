package bluej.extensions.event;

import bluej.extensions.BClass;

/**
 * This class encapsulates events which occur on BlueJ classes.<p>
 * 
 * The following events can occur:<p>
 * 
 * STATE_CHANGED: The compile state changed (either from uncompiled to compiled,
 *                or from compiled to uncompiled)<p>
 * CHANGED_NAME:  The class has changed name.
 * 
 * 
 * @author Davin McCall
 * @version $Id: ClassEvent.java 4708 2006-11-27 00:47:57Z bquig $
 */
public class ClassEvent implements ExtensionEvent
{
    public static final int STATE_CHANGED = 0;
    public static final int CHANGED_NAME = 1;
    
    private int eventId;
    private BClass bClass;
    private boolean isCompiled;
    private String oldName;
    
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
     * Construct a new ClassEvent object for a CHANGED_NAME event.
     * @param eventId  The event identifier (CHANGED_NAME)
     * @param bClass   The class which was renamed (refers to the new name)
     */
    public ClassEvent(int eventId, BClass bClass, String oldName)
    {
        this.eventId = eventId;
        this.bClass = bClass;
        this.oldName = oldName;
    }
    
    /**
     * Get the event Id (one of STATE_CHANGED, CHANGED_NAME).
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
     * Get the new class name. Valid for CHANGED_NAME event.
     */
    public String getOldName()
    {
        return oldName;
    }
}
