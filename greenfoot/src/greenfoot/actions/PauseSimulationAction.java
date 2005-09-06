package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * @author Poul Henriksen
 * @version $Id: PauseSimulationAction.java 3551 2005-09-06 09:31:41Z polle $
 */
public class PauseSimulationAction extends AbstractAction
{

    private Simulation simulation;

    public PauseSimulationAction(String name, Icon icon, Simulation simulation)
    {
        super(name, icon);
        this.simulation = simulation;
    }

    /**
     * 
     *  
     */
    public void actionPerformed(ActionEvent e)
    {
        simulation.setPaused(true);
    }

}