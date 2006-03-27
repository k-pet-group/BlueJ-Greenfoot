package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id: PauseSimulationAction.java 3890 2006-03-27 16:04:42Z mik $
 */
public class PauseSimulationAction extends AbstractAction
{
    private static final String iconFile = "pause.gif";
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
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;        
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

}