package greenfoot.event;

import java.util.EventListener;

/**
 * Listener for simulation events
 * 
 * @author Poul Henriksen
 * @version $Id: SimulationListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface SimulationListener
    extends EventListener
{
    public void simulationChanged(SimulationEvent e);
}