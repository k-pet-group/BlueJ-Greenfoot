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
    
    /** A status message has been returned */
    public final static int STATUS = 1;

    private String msg;

    private int type;
    
    public PublishEvent(String msg, int type) {
        this.msg = msg;
        this.type = type;
    }
    
    public String getMessage() {
        return msg;
    }
    
    public int getType() {
        return type;
    }
}
