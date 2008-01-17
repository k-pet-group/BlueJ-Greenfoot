/*
 * Created on Oct 7, 2003
 */
package greenfoot.event;

import java.util.EventObject;

/**
 * A simulation event
 * 
 * @author Poul Henriksen
 * @version $Id: SimulationEvent.java 5457 2008-01-17 12:22:42Z polle $
 */
public class SimulationEvent extends EventObject
{
    /** The simulation started running */
    public final static int STARTED = 0;
    
    /** The simulation was paused */
    public final static int STOPPED = 1;
    
    /** The simulation speed changed */
    public final static int CHANGED_SPEED = 2;   
    
    /** 
     * The simulation was stopped and cannot be restarted
     * until a STOPPED event is received.
     */
    public final static int DISABLED = 3; 

    /** The simulation is about to start a new act round */
    public final static int NEW_ACT = 4;

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
    
    public String toString() {
        switch (type) {
            case STARTED:
                return "STARTED";
            case STOPPED:
                return "STOPPED";
            case CHANGED_SPEED:
                return "CHANGED_SPEED";
            case DISABLED:
                return "DISABLED";
            case NEW_ACT:
                return "NEW_ACT";                
        }
        return super.toString();
        
    }
}