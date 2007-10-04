package greenfoot.core;

import greenfoot.*;
import greenfoot.event.*;

import java.util.ArrayList;
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
    
    /** X position of a dragged object in world coordinates (cells) */
    private int dragXpos;

    /** Y position of a dragged object in world coordinates (cells) */
    private int dragYpos;
    
    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation()
    {
        this.setName("SimulationThread");
        speed = 0;
        delay = calculateDelay(speed);
    }

    
    public static void initialize(WorldHandler worldHandler)
    {
        instance = new Simulation();
        instance.worldHandler = worldHandler;

        instance.startedEvent = new SimulationEvent(instance, SimulationEvent.STARTED);
        instance.stoppedEvent = new SimulationEvent(instance, SimulationEvent.STOPPED);
        instance.speedChangeEvent = new SimulationEvent(instance, SimulationEvent.CHANGED_SPEED);
        instance.disabledEvent = new SimulationEvent(instance, SimulationEvent.DISABLED);
        instance.setPriority(Thread.MIN_PRIORITY);
        // instance.setSpeed(50);
        instance.paused = true;
        instance.sleeping = true;

        worldHandler.addWorldListener(instance);
        instance.start();
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
            // copying it (to avoid ConcurrentModificationException)
            synchronized (world) {
                world.act();
                
                // We need to copy it, to avoid ConcurrentModificationException
                objects = new ArrayList<Actor>(WorldVisitor.getObjectsList(world));

                for (Actor actor : objects) {
                    if (actor.getWorld() != null) {
                        actor.act();
                    }
                }
                
                // If an object is being dragged, update its location
                synchronized (this) {
                        if (draggedObject != null && draggedObject.getWorld() != null) {
                            draggedObject.setLocation(dragXpos, dragYpos);
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
                    draggedObject.setLocation(dragXpos, dragYpos);
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
     * Remove a simulationListener to listen for changes.
     * 
     * @param l  Listener to remove
     */
    public void removeSimulationListener(SimulationListener l)
    {
        listenerList.remove(SimulationListener.class, l);
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
     * Sleep an amount of time according to the current speed setting for this
     * simulation. This will wait without considering previous waits, as opposed
     * to delay().
     */
    public void sleep()
    {
        World world = WorldHandler.getInstance().getWorld();
        
        try {
            if (world != null) {
                // The WorldCanvas may be trying to synchronize on the world in
                // order to do a repaint. So, we use world.wait() here in order to
                // release the world lock temporarily.
                
                long beginTime = System.currentTimeMillis();
                long currentTime = System.currentTimeMillis();
                while (currentTime - beginTime < delay) {
                    synchronized (world) {
                        world.wait(beginTime + delay - currentTime);
                    }
                    currentTime = System.currentTimeMillis();
                }
            }
            else {
                // shouldn't really happen
                Thread.sleep(delay);
            }
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Cause a delay (wait) according to the current speed setting for this
     * simulation. It will take the time spend in this simulation loop into
     * consideration and only pause the remaining time.
     * 
     * This method is used for timing the animation.
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
     * 
     * @param xpos Drag location in world coordinates (cells)
     * @param ypos Drag location in world coordinates (cells)
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