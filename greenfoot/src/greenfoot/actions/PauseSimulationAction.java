package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.utility.Debug;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

/**
 * @author Poul Henriksen
 * @version $Id: PauseSimulationAction.java 4115 2006-05-08 12:56:59Z davmac $
 */
public class PauseSimulationAction extends AbstractAction
    implements SimulationListener
{
    private static final String iconFile = "pause.png";
    private static PauseSimulationAction instance = new PauseSimulationAction();
    
    /**
     * Singleton factory method for action.
     */
    public static PauseSimulationAction getInstance()
    {
        return instance;
    }
    
    private Simulation simulation;

    private PauseSimulationAction()
    {
        super("Pause", new ImageIcon(PauseSimulationAction.class.getClassLoader().getResource(iconFile)));
        setEnabled(false);
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;
        simulation.addSimulationListener(this);
    }
    
    /**
     * This action was fired.
     */
    public void actionPerformed(ActionEvent e)
    {
        if(simulation == null)
            Debug.reportError("attempt to pause a simulation while none exists.");
        else
            simulation.setPaused(true);
    }

    /**
     * Observing for the simulation state so we can dis/en-able us appropiately
     */
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.STOPPED) {
            setEnabled(false);
        }
        if (e.getType() == SimulationEvent.STARTED) {
            setEnabled(true);
        }
    }
}