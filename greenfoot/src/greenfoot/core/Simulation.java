package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;

import java.util.Iterator;
import java.util.List;

import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * obejcts in the world and then paints them.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id$
 */
public class Simulation extends Thread implements WorldListener
{
    private WorldHandler worldHandler;
    private boolean paused;
    
    /** Whether the simulation is enabled (world installed) */
    private boolean enabled;
    
    /** Whether to run one loop when paused */
    private boolean runOnce;

    private EventListenerList listenerList = new EventListenerList();

    private SimulationEvent startedEvent;
    private SimulationEvent stoppedEvent;
    private SimulationEvent disabledEvent;
    private SimulationEvent speedChangeEvent;
    private static Simulation instance;

    /** for timing the animation */
    public static final int MAX_SIMULATION_SPEED = 100;
    private int speed;      // the simulation speed in range (1..100)
    
    private long lastDelayTime;
    private int delay;      // the speed translated into delay (ms) per step
    private boolean sleeping; // true when we are sleeping for delay or pause purposes

    /** for synchronized object dragging */
    private Actor draggedObject;
    private int dragXpos;
    private int dragYpos;
    
    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation()
    {
        speed = 0;
        delay = calculateDelay(speed);
    }

    
    public static void initialize(WorldHandler worldHandler)
    {
        if (instance == null) {
            instance = new Simulation();

            instance.worldHandler = worldHandler;
            
            instance.startedEvent = new SimulationEvent(instance, SimulationEvent.STARTED);
            instance.stoppedEvent = new SimulationEvent(instance, SimulationEvent.STOPPED);
            instance.speedChangeEvent = new SimulationEvent(instance, SimulationEvent.CHANGED_SPEED);
            instance.disabledEvent = new SimulationEvent(instance, SimulationEvent.DISABLED);
            instance.setPriority(Thread.MIN_PRIORITY);
//            instance.setSpeed(50);
            instance.paused = true;
            instance.sleeping = true;
            
            worldHandler.addWorldListener(instance);
            
            instance.start();
        }
    }

    
    /**
     * Returns the simulation if it is initialised. If not, it will return null.
     */
    public static Simulation getInstance()
    {
        return instance;
    }

    // The following methods should run only on the simulation thread itself!
    
    /**
     * Runs the simulation from the current state.
     */
    public void run()
    {
        System.gc();
        while (true) {
            maybePause();
            
            World world = worldHandler.getWorld();
            if (world != null) {
                WorldVisitor.startSequence(world);
            runOneLoop();
            }
            delay();
        }
    }

    /**
     * Block if the simulation is paused. This will block until the simulation is resumed.
     */
    private synchronized void maybePause()
    {
        checkScheduledDrag();
        
        if (paused && enabled) {
            fireSimulationEvent(stoppedEvent);
            System.gc();
        }
        while (paused && ! runOnce) {
            sleeping = true;
            try {
                this.wait();
            }
            catch (InterruptedException e1) { }
            if (!paused) {
                System.gc();
                fireSimulationEvent(startedEvent);
            }

        }
        
        runOnce = false;
        sleeping = false;
    }

    /**
     * Performs one step in the simulation. Calls act() on all actors.
     * 
     */
    private void runOneLoop()
    {
        World world = worldHandler.getWorld();
        if (world == null) {
            return;
        }
        
        try {
            List<? extends Actor> objects = null;

            // We need to sync, so that the collection is not changed while
            // copying it ( to avoid ConcurrentModificationException)
            synchronized (world) {
                // XX We need to copy it, to avoid ConcurrentModificationException
                // No we don't. What we get back is a copy anyway...
                objects = world.getObjects(null);

                for (Iterator i = objects.iterator(); i.hasNext();) {
                    Actor actor = (Actor) i.next();
                    if (actor.getWorld() != null) {
                        actor.act();
                    }
                }
                
                // If an object is being dragged, update its location
                synchronized (this) {
                        if (draggedObject != null && draggedObject.getWorld() != null) {
                            ActorVisitor.setLocationInPixels(draggedObject, dragXpos, dragYpos);
                            draggedObject = null;
                        }
                }
            }
        }
        catch (Throwable t) {
            // If an exception occurs, halt the simulation
            paused = true;
            t.printStackTrace();
        }
        
        
        worldHandler.repaint();
    }
    
    /**
     * Check if there is a scheduled object drag, and perform it if there is.
     * Should be called from a synchronized context on the simulation thread. 
     */
    private void checkScheduledDrag()
    {
        if (draggedObject == null) {
            return;
        }
        
        World world = draggedObject.getWorld();
        if (world != null) {
            synchronized (world) {
                if (draggedObject != null && draggedObject.getWorld() != null) {
                    ActorVisitor.setLocationInPixels(draggedObject, dragXpos, dragYpos);
                    draggedObject = null;
                }
            }
        }
    }

    // Public methods etc.
    
    /**
     * Run one step of the simulation. Each actor in the world acts once.
     */
    public synchronized void runOnce()
    {
        // Don't call runOneLoop directly as that executes user code
        // and might hang.
        runOnce = true;
        notifyAll();
    }
    
    /**
     * Pauses and unpauses the simulation.
     */
    public synchronized void setPaused(boolean b)
    {
        if (enabled) {
            paused = b;
            notifyAll();
        }
    }
    
    /**
     * Enable or disable the simulation.
     */
    public synchronized void setEnabled(boolean b)
    {
        if (enabled != b) {
            enabled = b;
            if (! enabled) {
                paused = true;
                fireSimulationEvent(disabledEvent);
            }
            else {
                // fire a paused event to let listeners know we are
                // enabled again
                fireSimulationEvent(stoppedEvent);
            }
        }
    }

    private void fireSimulationEvent(SimulationEvent event)
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
     * Set the speed of the simulation.
     *
     * @param speed  The speed in the range (0..100)
     */
    public void setSpeed(int speed)
    {
        if (speed < 0) {
            speed = 0;
        }
        else if (speed > MAX_SIMULATION_SPEED) {
            speed = MAX_SIMULATION_SPEED;
        }

        if(this.speed != speed) {
            this.speed = speed;
            this.delay = calculateDelay(speed);
            fireSimulationEvent(speedChangeEvent);
        }
    }

    /**
     * Returns the delay as a function of the speed.
     * 
     */
    private int calculateDelay(int speed)
    {
        return (MAX_SIMULATION_SPEED - speed) * 4;
    }
    
    
    /**
     * Get the current simulation speed.
     * @return  The speed in the range (1..100)
     */
    public int getSpeed()
    {
        return speed;
    }

    
    /**
     * Cause a delay (wait) according to the current speed setting for this
     * simulation.
     */
    private void delay()
    {
        try {
            long timeElapsed = System.currentTimeMillis() - this.lastDelayTime;
            long actualDelay = delay - timeElapsed;
            if (actualDelay > 0) {
                synchronized (this) {
                    sleeping = true;
                }
                Thread.sleep(delay - timeElapsed);
                synchronized (this) {
                    sleeping = false;
                }
            } else {
               // Thread.yield();
            }
            this.lastDelayTime = System.currentTimeMillis();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // ---------- WorldListener interface -----------
    
    /**
     * A new world was created - we're ready to go.
     * Enable the simulation functions.
     */
    public void worldCreated(WorldEvent e)
    {
        setEnabled(true);
    }

    
    /**
     * The world was removed - disable the simulation functions.
     */
    public void worldRemoved(WorldEvent e)
    {
        setEnabled(false);
    }
    
    // ----------- End of WorldListener interface -------------
    
    /**
     * Drag an object in the world, in a synchronized manner with the rest
     * of the simulation. This will return immediately even if the actual
     * actor movement is slightly delayed.
     */
    public void dragObject(Actor object, int xpos, int ypos)
    {
        synchronized (this) {
            if (sleeping && ! hasSleepTimeExpired()) {
				object.setLocation(xpos,ypos);
                worldHandler.repaint();
            }
            else {
                // schedule a drag
                draggedObject = object;
                dragXpos = xpos;
                dragYpos = ypos;
            }
        }
    }
    
    /**
     * Check whether the simulation is running and the delay time has expired.
     * This is an indication that the simulation thread is being starved for
     * CPU time.
     */
    private boolean hasSleepTimeExpired()
    {
        if (paused || ! enabled) {
            return false;
        }
            
        return (lastDelayTime + delay < System.currentTimeMillis());
    }

}