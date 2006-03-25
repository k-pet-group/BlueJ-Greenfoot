package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 * @author Poul Henriksen
 * @version $Id: PauseSimulationAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class PauseSimulationAction extends AbstractAction
{
    private static final String iconFile = "pause.gif";

    private Simulation simulation;

    public PauseSimulationAction(Simulation simulation)
    {
        super("Pause", new ImageIcon(PauseSimulationAction.class.getClassLoader().getResource(iconFile)));
        this.simulation = simulation;
    }

    /**
     * This action was fired.
     */
    public void actionPerformed(ActionEvent e)
    {
        simulation.setPaused(true);
    }

}