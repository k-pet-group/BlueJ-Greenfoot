package greenfoot;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.gui.ControlPanel;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * obejcts in the world and then paints them.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Simulation.java 3124 2004-11-18 16:08:48Z polle $
 */
public class Simulation extends Thread
    implements ChangeListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private WorldHandler worldHandler;
    private boolean paused;

    private EventListenerList listenerList = new EventListenerList();

    private SimulationEvent startedEvent;
    private SimulationEvent stoppedEvent;
    private static Simulation instance;

    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation()
    {}

    public static void initialize(WorldHandler worldHandler)
    {
        if (instance == null) {
            instance = new Simulation();
        }
        instance.worldHandler = worldHandler;
        instance.startedEvent = new SimulationEvent(instance, SimulationEvent.STARTED);
        instance.stoppedEvent = new SimulationEvent(instance, SimulationEvent.STOPPED);
        instance.paused = true;
        instance.start();
    }

    public static Simulation getInstance()
    {
        return instance;
    }

    /**
     * Runs the simulation from the current state.
     *  
     */
    public void run()
    {
        System.gc();
        while (true) {
            maybePause();
            runOnce();
            worldHandler.delay();
        }
    }

    private synchronized void maybePause()
    {
        if (paused) {
            fireSimulationEvent(stoppedEvent);
            System.gc();
            logger.info("Stoppping Simulation");
        }
        while (paused) {
            try {
                this.wait();
            }
            catch (InterruptedException e1) {}
            if (!paused) {
                System.gc();
                fireSimulationEvent(startedEvent);
                logger.info("Starting Simulation");
            }

        }
    }

    /**
     * Performs one step in the simulation. Calls act() on all actors.
     *  
     */
    public void runOnce()
    {
        for (Iterator i = worldHandler.getGreenfootObjects(); i.hasNext();) {
            GreenfootObject actor = (GreenfootObject) i.next();
            actor.act();
        }
        worldHandler.repaint();
    }

    /**
     * Pauses and unpauses the simulation.
     * 
     * @param b
     */
    public synchronized void setPaused(boolean b)
    {
        paused = b;
        notifyAll();
    }

    protected void fireSimulationEvent(SimulationEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SimulationListener.class) {
                ((SimulationListener) listeners[i + 1]).simulationChanged(event);
            }
        }
    }

    /**
     * Add a simulationListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    public void addSimulationListener(SimulationListener l)
    {
        listenerList.add(SimulationListener.class, l);
    }

    /**
     * Probably an update in the speedslider
     *  
     */
    public void stateChanged(ChangeEvent e)
    {
        if (e.getSource() instanceof ControlPanel) {
            int delay = ((ControlPanel) e.getSource()).getDelay();
            worldHandler.setDelay(delay);
        }
    }

}