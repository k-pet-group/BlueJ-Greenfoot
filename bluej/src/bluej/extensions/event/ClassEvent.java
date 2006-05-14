package bluej.extensions.event;

/**
 * This class encapsulates events which occur on BlueJ classes.
 * 
 * @author Davin McCall
 * @version $Id: ClassEvent.java 4257 2006-05-14 16:38:01Z davmac $
 */
public class ClassEvent implements ExtensionEvent
{
    public static final int STATE_CHANGED = 0;
    
    private boolean isCompiled;
    private int eventId;
    
    /**
     * Construct a new ClassEvent object.
     * @param eventId    The event identifier (currently STATE_CHANGED)
     * @param isCompiled  Whether the class is compiled or not
     */
    public ClassEvent(int eventId, boolean isCompiled)
    {
        this.eventId = eventId;
        this.isCompiled = isCompiled;
    }
    
    /**
     * Get the event Id. This can presently only be STATE_CHANGED.
     */
    public int getEventId()
    {
        return eventId;
    }
    
    /**
     * Check whether the class for which the event occurred is compiled.
     */
    public boolean isClassCompiled()
    {
        return isCompiled;
    }
    
}
