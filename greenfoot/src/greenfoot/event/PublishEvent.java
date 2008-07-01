package greenfoot.event;

import bluej.Config;

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
     * A status message has been returned. For now, receiving a status message
     * means that it was a successful submit.
     */
    public final static int STATUS = 1;

    /**
     * Some upload progress has been made. Use getBytes() to find out how
     * much.
     */
    public final static int PROGRESS = 2;
    
    private String msg;
    private int bytes;

    private int type;

    public PublishEvent(String msg, int type)
    {
        this.msg = msg;
        this.type = type;
    }

    public PublishEvent(int progress, int type)
    {
        this.type = type;
        this.bytes = progress;
    }
    
    public String getMessage()
    {
        return msg;
    }
    
    public int getBytes()
    {
        return bytes;
    }

    public int getType()
    {
        return type;
    }

    public String toString()
    {
        String s = super.toString() + " [";
        if (type == ERROR)
            s += Config.getString("publish.event.error");
        else if (type == STATUS)
            s += Config.getString("publish.event.status");
        s += msg + "]";
        return s;
    }
}
