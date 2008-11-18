package greenfoot.core;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.platforms.SimulationDelegate;
import greenfoot.util.HDTimer;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * obejcts in the world and then paints them.
 * 
 * @author Poul Henriksen
 */
public class Simulation extends Thread
    implements WorldListener
{
    // We skip repaints if the simulation is running faster than the
    // MAX_FRAME_RATE. This makes the high speeds run faster, since we avoid
    // repaints that can't be seen anyway.
    private static int MAX_FRAME_RATE = 60;
    private static int MIN_FRAME_RATE = 30;
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
    private SimulationEvent newActEvent;
    private static Simulation instance;

    /** for timing the animation */
    public static final int MAX_SIMULATION_SPEED = 100;
    private int speed; // the simulation speed in range (1..100)

    private long lastDelayTime;
    private long delay; // the speed translated into delay (nanoseconds)

    private long updates; // used for debugging to calculate update rate
    private long lastUpdate; // used for debugging to calculate update rate
    
    /**
     * The last few times at which repaints were requested. These are used to
     * calculate the frame rate. Accessed only from the simulation thread.
     */
    private Queue<Long> repaintTimes = new LinkedList<Long>();
    private volatile boolean interruptedForSpeedChange = false;
    private SimulationDelegate delegate;
    private boolean paintPending;

    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation()
    {
        this.setName("SimulationThread");
        speed = 50;
        delay = calculateDelay(speed);
        HDTimer.init();
    }
    
    public static void initialize(WorldHandler worldHandler, SimulationDelegate simulationDelegate)
    {
        instance = new Simulation();
        instance.worldHandler = worldHandler;
        instance.delegate = simulationDelegate;
        instance.startedEvent = new SimulationEvent(instance, SimulationEvent.STARTED);
        instance.stoppedEvent = new SimulationEvent(instance, SimulationEvent.STOPPED);
        instance.speedChangeEvent = new SimulationEvent(instance, SimulationEvent.CHANGED_SPEED);
        instance.disabledEvent = new SimulationEvent(instance, SimulationEvent.DISABLED);
        instance.newActEvent = new SimulationEvent(instance, SimulationEvent.NEW_ACT);
        instance.setPriority(Thread.MIN_PRIORITY);
        instance.paused = true;

        worldHandler.addWorldListener(instance);
        instance.addSimulationListener(worldHandler);
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
            try {
                if(interruptedForSpeedChange) {
                    interruptedForSpeedChange = false;
                    delay();
                }                
                maybePause();

                World world = worldHandler.getWorld();
                if (world != null) {
                    WorldVisitor.startSequence(world);

                    runOneLoop();

                }
                delay();
            }
            catch (ActInterruptedException e) {
                // Someone interrupted the user code. We ignore it and let
                // maybePause() handle whatever needs to be done.
                // Unless it was interrupted because of a speed change, then we
                // delay an amount equal to the new delay:
                
            }
        }
    }

    /**
     * Block if the simulation is paused. This will block until the simulation
     * is resumed.
     */
    private synchronized void maybePause()
    {
        if (paused && enabled) {
            fireSimulationEvent(stoppedEvent);
            System.gc();
        }
        while (paused && !runOnce) {
            // Make sure we repaint before pausing.
            worldHandler.repaint();
            try {
                System.gc();
                this.wait();
            }
            catch (InterruptedException e1) {
                throw new ActInterruptedException(e1);
            }
            if (!paused) {
                repaintTimes.clear();
                fireSimulationEvent(startedEvent);
            }
        }

        runOnce = false;
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

        fireSimulationEvent(newActEvent);

        List<? extends Actor> objects = null;

        // We need to sync to avoid ConcurrentModificationException
        try {
            world.lock.writeLock().lockInterruptibly();
            try {
                world.act();
                // We need to make a copy so that the original collection can be
                // modified by the actors act() methods
                objects = new ArrayList<Actor>(WorldVisitor.getObjectsListInActOrder(world));
                for (Actor actor : objects) {
                    if (actor.getWorld() != null) {
                        actor.act();
                    }
                }
            }
            catch (ActInterruptedException e) {
                throw e;
            }
            catch (Throwable t) {
                // If any other exceptions occur, halt the simulation
                paused = true;
                t.printStackTrace();
            }
            finally {
                // Release lock if we have it (we might not have the lock if
                // interrupted)
                if(world.lock.isWriteLockedByCurrentThread()){
                    world.lock.writeLock().unlock();
                }
            }
        }
        catch (InterruptedException e) {
            // Interrupted while trying to acquire lock
            throw new ActInterruptedException(e);
        }    

        printUpdateRate(System.nanoTime());

        repaintIfNeeded();
    }

    /**
     * Repaints the world if needed to obtain the desired frame rate.
     */
    private void repaintIfNeeded()
    {
        int repaintRate = getRepaintRate();
        if (repaintRate <= MAX_FRAME_RATE) {
            try {
                synchronized(this) {
                    if (repaintRate <= MIN_FRAME_RATE) {
                        // Waiting here makes sure the WorldCanvas gets a chance to
                        // repaint. It also lets the rest of the UI be responsive, even if
                        // we are running at maximum speed, by making sure events on the
                        // event queue are processed.
                        while (paintPending) {
                            wait();
                        }
                    }

                    triggerRepaint();
                }
            }
            catch (InterruptedException ie) {}
        }
    }

    /**
     * Force the world to be repainted (by putting a repaint job on the event queue).
     */
    private void triggerRepaint()
    {
        synchronized (this) {
            if (! paintPending) {
                paintPending = true;
                worldHandler.repaint();

                synchronized (repaintTimes) {
                    repaintTimes.offer(System.currentTimeMillis());
                }

                EventQueue.invokeLater(new Runnable() {
                    public void run()
                    {
                        synchronized (Simulation.this) {
                            paintPending = false;
                            // worldHandler.paintImmediately();
                            Simulation.this.notifyAll();
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Returns the current repaint rate. Calculated from the time since a
     * previous repaint and the current time.
     */
    private int getRepaintRate()
    {
        long currentTime = System.currentTimeMillis();
        long lastRepaintTime = 0;
        int knownRepaintTimes = repaintTimes.size();

        if (knownRepaintTimes >= 100) {
            lastRepaintTime = repaintTimes.poll();
        }
        else if (knownRepaintTimes > 0) {
            lastRepaintTime = repaintTimes.peek();
        }
        
        long timeSinceRepaint = currentTime - lastRepaintTime;
        // Avoid divide by zero
        if (timeSinceRepaint == 0)
            timeSinceRepaint = 1;
        int frameRate = (int) ((knownRepaintTimes * 1000L) / timeSinceRepaint);
        return frameRate;
    }

    /**
     * Debug output to print the rate at which updates are performed
     * (acts/second).
     */
    private void printUpdateRate(long currentTime)
    {
        updates++;

        long timeSinceUpdate = currentTime - lastUpdate;
        if (timeSinceUpdate > 3000000000L) {
            // DAV
            System.out.println("Updates: " + updates);
            // System.out.println("Repaints: " + worldHandler.getAndResetPaintCount());
            lastUpdate = currentTime;
            updates = 0;
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
        paused = b;
        if (enabled) {
            notifyAll();

            // Interrupt thread to make sure it stops.
            if (paused) {
                instance.interrupt();
                // TODO: check if the thread is still running now, we should
                // force
                // it to quit. Maybe using the deprecated stop() and then
                // restarting
                // the thread. Maybe making sim a runnable instead of thread and
                // then creating a new thread and start that.
                // instance.stop();
                // reinit
            }
        }
    }

    /**
     * Enable or disable the simulation.
     */
    public synchronized void setEnabled(boolean b)
    {
        if (b) {
            notifyAll();
        }
        if (enabled != b) {
            enabled = b;
            if (!enabled) {
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
     * @param l
     *            Listener to remove
     */
    public void removeSimulationListener(SimulationListener l)
    {
        listenerList.remove(SimulationListener.class, l);
    }

    /**
     * Set the speed of the simulation.
     * 
     * @param speed
     *            The speed in the range (0..100)
     */
    public void setSpeed(int speed)
    {
        if (speed < 0) {
            speed = 0;
        }
        else if (speed > MAX_SIMULATION_SPEED) {
            speed = MAX_SIMULATION_SPEED;
        }

        if (this.speed != speed) {
            this.speed = speed;
            
            delegate.setSpeed(speed);
            
            this.delay = calculateDelay(speed);

            repaintTimes.clear();
            
            // If simulation is running we should interrupt any waiting or
            // sleeping that is currently happening.
            if(!paused) {
                interruptedForSpeedChange = true;
                interrupt();
            }
            fireSimulationEvent(speedChangeEvent);
        }
    }

    /**
     * Returns the delay as a function of the speed.
     * 
     * @return The delay in nanoseconds.
     */
    private long calculateDelay(int speed)
    {
        // Make the speed into a delay
        long rawDelay = MAX_SIMULATION_SPEED - speed;

        long min = 30 * 1000L; // Delay at MAX_SIMULATION_SPEED - 1
        long max = 10000 * 1000L * 1000L; // Delay at slowest speed

        double a = Math.pow(max / (double) min, 1D / (MAX_SIMULATION_SPEED - 1));
        long delay = 0;
        if (rawDelay > 0) {
            delay = (long) (Math.pow(a, rawDelay - 1) * min);
        }
        return delay;
    }

    /**
     * Get the current simulation speed.
     * 
     * @return The speed in the range (1..100)
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
                // order to do a repaint. So, we use wait() here in order
                // to release the world lock temporarily.
                HDTimer.wait(delay, world.lock);
            }
            else {
                // shouldn't really happen
                HDTimer.sleep(delay);
            }
        }
        catch (InterruptedException e) {
            throw new ActInterruptedException(e);
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
            long currentTime = System.nanoTime();
            long timeElapsed = currentTime - this.lastDelayTime;
            long actualDelay = delay - timeElapsed;

            // It is not possible to go back in time, so don't try it!
            if (actualDelay < 0) {
                actualDelay = 0;
            }

            if (actualDelay > 0) {
                HDTimer.sleep(actualDelay);
            }
            else {
                // If we get to this point, it means that we have trouble
                // keeping up with chosen speed, and that we are probably
                // starving the CPU. Running at full speed means infinite speed
                // and hence we will always end here when running at full speed.
            }
            // This is the time at which the delay should be over. Instead of
            // using the time after sleep we use the time measured at the
            // beginning, so that the bad precision of sleep() will not have too
            // big an impact.
            this.lastDelayTime = currentTime + actualDelay;
        }
        catch (InterruptedException e) {
            throw new ActInterruptedException(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- WorldListener interface -----------

    /**
     * A new world was created - we're ready to go. Enable the simulation
     * functions.
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

}