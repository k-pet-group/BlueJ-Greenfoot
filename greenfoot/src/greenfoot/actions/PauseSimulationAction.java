package greenfoot.actions;

import greenfoot.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

/**
 * @author Poul Henriksen
 * @version $Id: PauseSimulationAction.java 3124 2004-11-18 16:08:48Z polle $
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