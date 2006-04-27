/*
 * Created on Oct 7, 2003
 */
package greenfoot.event;

import java.util.EventObject;

/**
 * A simulation event
 * 
 * @author Poul Henriksen
 * @version $Id: SimulationEvent.java 4043 2006-04-27 15:30:40Z davmac $
 */
public class SimulationEvent extends EventObject
{
    public final static int STARTED = 0;
    public final static int STOPPED = 1;
    public final static int CHANGED_SPEED = 2;

    private int type;

    public SimulationEvent(Object source, int type)
    {
        super(source);
        this.type = type;
    }

    public int getType()
    {
        return type;
    }
}