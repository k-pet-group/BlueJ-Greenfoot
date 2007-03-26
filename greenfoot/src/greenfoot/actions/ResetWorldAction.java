package greenfoot.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.utility.Debug;

public class ResetWorldAction extends AbstractAction implements SimulationListener
{

    private Simulation simulation;
    private static ResetWorldAction instance = new ResetWorldAction();

    private static final String iconFile = "reset.png";
    
    /**
     * Singleton factory method for action.
     */
    public static ResetWorldAction getInstance()
    {
        return instance;
    }
    
    private ResetWorldAction()
    {
        super("Reset", new ImageIcon(ResetWorldAction.class.getClassLoader().getResource(iconFile)));
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;
        simulation.addSimulationListener(this);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(simulation == null)
            Debug.reportError("attempt to reset a simulation while none exists.");
        else
            simulation.setPaused(true);
        WorldHandler.getInstance().instantiateNewWorld();
    }

    /**
     * Observing for the simulation state so we can dis/en-able us appropiately
     */
    public void simulationChanged(final SimulationEvent e)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                int eventType = e.getType();
                if (eventType == SimulationEvent.STOPPED) {
                    setEnabled(true);
                }
                else if (eventType == SimulationEvent.STARTED) {
                    setEnabled(true);
                }
                else if (eventType == SimulationEvent.DISABLED) {
                    setEnabled(false);
                }
            }
        });
    }

}
