/*
 * Created on Oct 7, 2003
 */
package greenfoot.event;

import java.util.EventObject;

/**
 * A simulation event
 * 
 * @author Poul Henriksen
 * @version $Id: SimulationEvent.java 3124 2004-11-18 16:08:48Z polle $
 */
public class SimulationEvent extends EventObject
{
    public final static int STARTED = 0;
    public final static int STOPPED = 1;

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