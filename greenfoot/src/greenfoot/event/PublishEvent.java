package greenfoot.event;

/**
 * Event from publishing a scenario.
 * 
 * @author Poul Henriksen
 * 
 */
public class PublishEvent
{
    /** The publish returned an error */
    public final static int ERROR = 0;

    /**
     * A status message has been returned. For now, recieving a status message
     * means that it was a successful submit.
     */
    public final static int STATUS = 1;

    private String msg;

    private int type;

    public PublishEvent(String msg, int type)
    {
        this.msg = msg;
        this.type = type;
    }

    public String getMessage()
    {
        return msg;
    }

    public int getType()
    {
        return type;
    }

    public String toString()
    {
        String s = super.toString() + " [";
        if (type == ERROR)
            s += "Error: ";
        else if (type == STATUS)
            s += "Status: ";
        s += msg + "]";
        return s;
    }
}
