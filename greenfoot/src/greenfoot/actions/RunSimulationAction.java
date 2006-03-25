package greenfoot.actions;

import greenfoot.core.Simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 * @author Poul Henriksen
 * @version $Id: RunSimulationAction.java 3879 2006-03-25 20:40:14Z mik $
 */
public class RunSimulationAction extends AbstractAction
{
    private static final String iconFile = "run.gif";

    private Simulation simulation;

    public RunSimulationAction(Simulation simulation)
    {
        super("Run", new ImageIcon(RunSimulationAction.class.getClassLoader().getResource(iconFile)));
        this.simulation = simulation;
    }

    public void actionPerformed(ActionEvent e)
    {
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