package greenfoot.event;

import java.util.EventObject;

/**
 * Event used when something changes from being valid to invalid or vice versa.
 * 
 * @author Poul Henriksen
 */
public class ValidityEvent extends EventObject
{
    private String reason;

    /**
     * Create a new event.
     * 
     * @param source
     *            The source of the event.
     * @param reason
     *            The reason for the change of validity.
     */
    public ValidityEvent(Object source, String reason)
    {
        super(source);
        this.reason = reason;
    }

    /**
     * Get the reason why this event was created. Typically a message explaining
     * why a it is not valid.
     */
    public String getReason()
    {
        return reason;
    }
}
