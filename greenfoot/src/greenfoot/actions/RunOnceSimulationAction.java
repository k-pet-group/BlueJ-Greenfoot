package greenfoot.actions;

import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id: RunOnceSimulationAction.java,v 1.10 2004/11/18 09:43:46 polle
 *          Exp $
 */
public class RunOnceSimulationAction extends AbstractAction
    implements SimulationListener
{
    private static final String iconFile = "step.png";
    private static RunOnceSimulationAction instance = new RunOnceSimulationAction();
    
    /**
     * Singleton factory method for action.
     */
    public static RunOnceSimulationAction getInstance()
    {
        return instance;
    }
    

    private Simulation simulation;

    private RunOnceSimulationAction()
    {
        super("Act", new ImageIcon(RunOnceSimulationAction.class.getClassLoader().getResource(iconFile)));
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
        if(simulation == null) {
            Debug.reportError("attempt to pause a simulation while none exists.");
            return;
        }
        //We don't want to block!
        new Thread() {
            public void run()
            {
                simulation.runOnce();
            }

        }.start();
    }

    /**
     * Observing for the simulation state so we can dis/en-able us appropiately
     */
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.STOPPED) {
            setEnabled(true);
        }
        if (e.getType() == SimulationEvent.STARTED) {
            setEnabled(false);
        }
    }
}