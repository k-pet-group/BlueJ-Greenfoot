package bluej.extensions.event;

import bluej.extensions.BPackage;

/**
 * @author Clive Miller
 * @version $Id: BJEvent.java 1459 2002-10-23 12:13:12Z jckm $
 */
public abstract class BJEvent
{
    private final BPackage pkg;

    private int event;
    
    protected BJEvent (int event, BPackage pkg)
    {
        this.event = event;
        this.pkg = pkg;
    }
    
    /**
     * Gets the id of the event raised
     * @return an event id
     */
    public int getEvent()
    {
        return event;
    }
    
    /**
     * Gets the package in which the event occurred (if any)
     * @return the package
     */
    public BPackage getPackage()
    {
        return pkg;
    }
}