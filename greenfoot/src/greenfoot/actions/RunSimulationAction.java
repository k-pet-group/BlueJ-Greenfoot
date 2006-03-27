package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id: RunSimulationAction.java 3890 2006-03-27 16:04:42Z mik $
 */
public class RunSimulationAction extends AbstractAction
{
    private static final String iconFile = "run.gif";
    private static RunSimulationAction instance = new RunSimulationAction();
    
    /**
     * Singleton factory method for action.
     */
    public static RunSimulationAction getInstance()
    {
        return instance;
    }
    

    private Simulation simulation;

    private RunSimulationAction()
    {
        super("Run", new ImageIcon(RunSimulationAction.class.getClassLoader().getResource(iconFile)));
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;        
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(simulation == null) {
            Debug.reportError("attempt to pause a simulation while none exists.");
            return;
        }
        //	We don't want to block!
        new Thread() {
            public void run()
            {
                if (simulation.isAlive()) {
                    simulation.setPaused(false);
                }
                else {
                    simulation.start();
                }
            }
        }.start();
    }
}