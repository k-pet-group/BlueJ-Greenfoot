package greenfoot.actions;

import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @author Poul Henriksen
 * @version $Id: RunOnceSimulationAction.java,v 1.10 2004/11/18 09:43:46 polle
 *          Exp $
 */
public class RunOnceSimulationAction extends AbstractAction
    implements SimulationListener
{
    private static final String iconFile = "step.gif";

    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private Simulation simulation;

    public RunOnceSimulationAction(Simulation simulation)
    {
        super("Act", new ImageIcon(RunOnceSimulationAction.class.getClassLoader().getResource(iconFile)));
        this.simulation = simulation;
        simulation.addSimulationListener(this);
    }

    public void actionPerformed(ActionEvent e)
    {
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
     * 
     * @see greenfoot.event.SimulationListener#simulationChanged(greenfoot.simulation.SimulationEvent)
     */
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.STOPPED) {
            logger.info("enabling");
            setEnabled(true);
        }
        if (e.getType() == SimulationEvent.STARTED) {
            setEnabled(false);
            logger.info("disabling");
        }
    }
}