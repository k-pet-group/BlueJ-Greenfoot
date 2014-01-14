/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010,2011,2012,2013,2014  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.core;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.platforms.SimulationDelegate;
import greenfoot.util.HDTimer;

import java.awt.EventQueue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JComponent;
import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * on the objects in the world and then paints them.
 * 
 * @author Poul Henriksen
 */
public class Simulation extends Thread
    implements WorldListener
{
    // Most of the fields require synchronized access. Some of them do not because they are only
    // accessed from the simulation thread itself. "repaintLock" protects paintPending and
    // lastRepaintTime.
    
    // All user code should generally be run on the simulation thread. The simulation monitor
    // should not be held while executing user code (though the world lock should be held).
    
    // The following two constants control repainting of the world while the simulation is
    // running. We skip repaints if the simulation is running faster than the MAX_FRAME_RATE.
    // This makes the high speeds run faster, since we avoid repaints that can't be seen
    // anyway. Once requested, the repaint may take some time to occur; if the effective
    // repaint rate falls below MIN_FRAME_RATE, then we temporarily suspend the simulation
    // and wait for the repaint to occur.
    
    /** Repaints will be requested at this rate (at most) */
    private static int MAX_FRAME_RATE = 65;
    /** Simulation will wait for repaints if the repaint rate falls below this */
    private static int MIN_FRAME_RATE = 35;
    
    private WorldHandler worldHandler;
    
    /** Whether the simulation is (to be) paused */
    private boolean paused;

    /** Whether the simulation is enabled (world installed) */
    private volatile boolean enabled;

    /** Whether to run one loop when paused */
    private boolean runOnce;
    
    /** Tasks that are queued to run on the simulation thread */
    private Queue<Runnable> queuedTasks = new LinkedList<Runnable>();

    private EventListenerList listenerList = new EventListenerList();

    /* Various simulation events */
    private SimulationEvent startedEvent;
    private SimulationEvent stoppedEvent;
    private SimulationEvent disabledEvent;
    private SimulationEvent speedChangeEvent;
    private SimulationEvent debuggerPausedEvent;
    private SimulationEvent debuggerResumedEvent;
    
    private static Simulation instance;

    /** for timing the animation */
    public static final int MAX_SIMULATION_SPEED = 100;
    private int speed; // the simulation speed in range (1..100)

    private long lastDelayTime;
    private long delay; // the speed translated into delay (nanoseconds)

    // private long updates; // used for debugging to calculate update rate
    //private long lastUpdate; // used for debugging to calculate update rate
    
    /** Protects "paintPending" and "lastRepaintTime" */
    private Object repaintLock = new Object();
    /** The last time that a repaint of the World was issued. */
    private long lastRepaintTime;
    /** true if a repaint has been issued and not yet processed. */
    private boolean paintPending;
    
    private SimulationDelegate delegate;

    /**
     * Lock to synchronize access to the two fields: delaying and interruptDelay
     */
    private Object interruptLock = new Object();
    /** Whether we are currently delaying between act-loops. */
    private boolean delaying;
    /** Whether a delay between act-loops should be interrupted. */
    private boolean interruptDelay;

    
    /**
     * Used to figure out when we are transitioning from running to paused state and vice versa.
     * Only modify this from the simulation thread.
     */
    private boolean isRunning = false;
    
    /** flag to indicate that we want to abort the simulation and never start it again. */
    private volatile boolean abort;
    
    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation(SimulationDelegate simulationDelegate)
    {
        this.setName("SimulationThread");
        this.delegate = simulationDelegate;
        startedEvent = new SimulationEvent(this, SimulationEvent.STARTED);
        stoppedEvent = new SimulationEvent(this, SimulationEvent.STOPPED);
        speedChangeEvent = new SimulationEvent(this, SimulationEvent.CHANGED_SPEED);
        disabledEvent = new SimulationEvent(this, SimulationEvent.DISABLED);
        debuggerPausedEvent = new SimulationEvent(this, SimulationEvent.DEBUGGER_PAUSED);
        debuggerResumedEvent = new SimulationEvent(this, SimulationEvent.DEBUGGER_RESUMED);
        setPriority(Thread.MIN_PRIORITY);
        paused = true;
        speed = 50;
        delay = calculateDelay(speed);
        HDTimer.init();
    }
    
    /**
     * Initialize the (singleton) simulation instance.
     * The simulation thread will not actually be started until the WorldHandler
     * is attached.
     */
    public static void initialize(SimulationDelegate simulationDelegate)
    {
        instance = new Simulation(simulationDelegate);
    }

    /**
     * Returns the simulation if it is initialised. If not, it will return null.
     */
    public static Simulation getInstance()
    {
        return instance;
    }

    /**
     * Attach this simulation to the world handler (and vice versa).
     */
    public void attachWorldHandler(WorldHandler worldHandler)
    {
        this.worldHandler = worldHandler;
        worldHandler.addWorldListener(this);
        addSimulationListener(worldHandler);
        start();
    }
    
    // The following methods should run only on the simulation thread itself!

    /**
     * Runs the simulation from the current state.
     */
    @Override
    public void run()
    {
        /* It is important this redirects to another method.
         * The debugger sets a breakpoint on the first line of this method, and if
         * that is a loop (as is the case for the first line of runContent at the time of writing)
         * then it hits the breakpoint every time.  By putting it all in a separate
         * method, we avoid that happening:
         */
        runContent();
    }
    
    private void runContent()
    {
        while (!abort) {
            try {
                maybePause();
                                
                if (worldHandler.hasWorld()) {
                    runOneLoop(worldHandler.getWorld());
                }

                delay();
            }
            catch (ActInterruptedException e) {
                // Someone interrupted the user code. We ignore it and let
                // maybePause() handle whatever needs to be done.
            }
            catch (InterruptedException e) {
                // maybePause was interrupted. Do nothing, will be handled the next time we get to maybePause.
            }
            catch (Throwable t) {
                // If any other exceptions occur, halt the simulation
                synchronized (this) {
                    paused = true;
                }
                t.printStackTrace();
            }
        }

        // The simulations has been aborted. But, we might still have to notify the world.
        synchronized (this) {
            if(isRunning) {
                World world = worldHandler.getWorld();
                if (world != null) {
                    worldStopped(world);
                }
                isRunning = false;
            } 
        }
    }   

    /**
     * Schedule some task to run on the simulation thread. The task will be run with the
     * world write lock held.
     */
    public synchronized void runLater(Runnable r)
    {
        queuedTasks.add(r);
        if (paused) {
            notify();
        }
    }
    
    public final static String PAUSED = "simulationWait";
    /**
     * A special method recognised by the debugger as indicating that the simulation
     * is pausing.
     */
    private void simulationWait() throws InterruptedException
    {
        this.wait();
    }
    
    public final static String WORLD_STARTED = "worldStarted";
    
    private static void worldStarted(World world)
    {
        world.started();
    }
    
    public final static String WORLD_STOPPED = "worldStopped";
    
    private static void worldStopped(World world)
    {
        world.stopped();
    }
    
    /**
     * Block if the simulation is paused. This will block until the simulation
     * is resumed (is both enabled and unpaused). It should only be called on the
     * simulation thread.
     * 
     * @throws InterruptedException If it couldn't acquire the world lock when
     *             signalling started()/stopped() to the world.
     */
    private void maybePause()
        throws InterruptedException
    {
        while (!abort) {
            runQueuedTasks();

            // Wait loop that waits until such time that at least one simulation
            // loop can be run.

            World world;
            boolean checkStop;
            
            synchronized (this) {
                checkStop = (paused || !enabled) && isRunning;
                world = worldHandler.getWorld();
                
                if (checkStop) {
                    isRunning = false; // if we start again, we'll need to signal it.
                }
                else if (isRunning) {
                    return; // We're running and don't need to stop
                }
            }
            
            // We are either not running, or running and need to stop.
            
            if (checkStop) {
                try {
                    signalStopping(world);
                }
                catch (InterruptedException ie) {
                    continue;
                }
                    
                synchronized (this) {
                    runOnce = false;

                    if (! paused) {
                        isRunning = enabled; // Never signalled a stop, so don't signal a start
                    }
                }
            }
            
            // We're not running; we may need to resume running.
            
            boolean doResumeRunning;
            
            synchronized (this) {
                doResumeRunning = !paused && enabled && !abort && !isRunning;
                if (! isRunning && ! doResumeRunning && ! runOnce) {
                    // Still paused, so notify listeners, and actually pause
                    if (enabled) {
                        fireSimulationEvent(stoppedEvent);
                    }
                    if (worldHandler != null) {
                        worldHandler.repaint();
                    }
                    
                    if (! queuedTasks.isEmpty()) {
                        continue; // Must run queued tasks before wait
                    }
                    
                    System.gc();
                    try {
                        simulationWait();
                        lastDelayTime = System.nanoTime();
                    }
                    catch (InterruptedException e1) {
                        // Swallow the interrupt
                    }
                    
                    continue; // take it from the top
                }
            }
            
            if (doResumeRunning) {
                resumeRunning();
            }
            
            synchronized (this) {
                if (runOnce || isRunning) {
                    // Run the simulation
                    runOnce = false;
                    return;
                }
            }
        }
    
        runQueuedTasks();
    }
    
    /**
     * Send a started event and notify the world that it is now running.
     * 
     * @throws InterruptedException
     */
    private void resumeRunning() throws InterruptedException
    {
        isRunning = true;
        lastDelayTime = System.nanoTime();
        fireSimulationEvent(startedEvent);
        World world = worldHandler.getWorld();
        if (world != null) {
            // We need to sync to avoid ConcurrentModificationException
            ReentrantReadWriteLock lock = worldHandler.getWorldLock();
            try {
                lock.writeLock().lockInterruptibly();
            }
            catch (InterruptedException ie) {
                isRunning = false; // need to notify again
                throw ie;
            }
                
            try {
                worldStarted(world); // may cause us to pause
            }
            catch (Throwable t) {
                isRunning = false;
                setPaused(true);
                t.printStackTrace();
                return;
            }
            finally {
                lock.writeLock().unlock();
            }
        }
    }
    
    /**
     * Tell the world that the simulation is stopping. The world might resume
     * the simulation when this happens.
     */
    private void signalStopping(World world) throws InterruptedException
    {
        // This code will be executed when:
        //  runOnce is over  or
        //  setPaused(true)   or
        //  setEnabled(false)  or
        //  abort() (sometimes, depending on timing)
        if (world != null) {
            // We need to sync to avoid ConcurrentModificationException
            ReentrantReadWriteLock lock = worldHandler.getWorldLock();
            lock.writeLock().lockInterruptibly();
            try {
                worldStopped(world); // may un-pause
            }
            catch (ActInterruptedException aie) {
                synchronized (this) {
                    paused = true;
                }
                throw aie;
            }
            catch (Throwable t) {
                // If any exceptions occur, halt the simulation
                synchronized (this) {
                    paused = true;
                }
                t.printStackTrace();
            }
            finally {
                lock.writeLock().unlock();
            }
        }
    }

    /** This must match the method name below! */
    public static String RUN_QUEUED_TASKS = "runQueuedTasks";
    
    /**
     * Run all tasks that have been schedule to run on the simulation thread.
     * Of course, this should only be called from the simulation thread...
     * (and from an unsynchronized context).
     */
    private void runQueuedTasks()
    {
        Runnable r;
        synchronized (this) {
            r = queuedTasks.poll();
        }
        
        while (r != null) {
            World world = WorldHandler.getInstance().getWorld();
            try {
                ReentrantReadWriteLock lock  = null;
                if (world != null) {
                    lock = worldHandler.getWorldLock();
                    lock.writeLock().lock();
                }
                
                try {
                    // This may run user code, which might throw an exception.
                    r.run();
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
                
                if (world != null) {
                    lock.writeLock().unlock();
                }
            }
            finally {
                
            }
            synchronized (this) {
                r = queuedTasks.poll();
            }
        }
    }
    
    /**
     * Performs one step in the simulation. Calls act() on all actors.
     * 
     * @throws ActInterruptedException  if an act() call was interrupted.
     */
    private void runOneLoop(World world)
    {
        worldHandler.startSequence();

        // We don't want to be interrupted in the middle of an act-loop
        // so we remember the first interrupted exception and throw it
        // when all the actors have acted.
        ActInterruptedException interruptedException = null;
        
        List<? extends Actor> objects = null;

        // We need to sync to avoid ConcurrentModificationException
        try {
            ReentrantReadWriteLock lock = worldHandler.getWorldLock();
            lock.writeLock().lockInterruptibly();
            try {
                try {
                    actWorld(world);
                    if (world != worldHandler.getWorld()) {
                        return; // New world was set
                    }
                }
                catch (ActInterruptedException e) {
                    interruptedException = e;
                }
                // We need to make a copy so that the original collection can be
                // modified by the actors' act() methods.
                objects = new ArrayList<Actor>(WorldVisitor.getObjectsListInActOrder(world));
                for (Actor actor : objects) {
                    if (!enabled) {
                        return;
                    }
                    if (ActorVisitor.getWorld(actor) != null) {
                        try {
                            actActor(actor);
                            if (world != worldHandler.getWorld()) {
                                return; // New world was set
                            }
                        }
                        catch (ActInterruptedException e) {
                            if (interruptedException == null) {
                                interruptedException = e;
                            }
                        }
                    }

                }
                
                worldHandler.getKeyboardManager().clearLatchedKeys();
            }
            finally {
                lock.writeLock().unlock();
            }
        }
        catch (InterruptedException e) {
            // Interrupted while trying to acquire lock
            throw new ActInterruptedException(e);
        }

        // We were interrupted while running through the act-loop. Throw now.
        if(interruptedException != null) {
            throw interruptedException;
        }
        
        // printUpdateRate(System.nanoTime());

        repaintIfNeeded();
    }
    
    // The actActor, actWorld and newInstance methods exist as a tagging mechanism
    // that allows them to be found easily in the debugger when we
    // are attempting to reach the next call to user code
    
    public static final String ACT_ACTOR = "actActor";
    private static void actActor(Actor actor)
    {
        actor.act();
    }
    
    public static final String ACT_WORLD = "actWorld";
    private static void actWorld(World world)
    {
        world.act();
    }
    
    public static final String NEW_INSTANCE = "newInstance";
    public static Object newInstance(Constructor<?> constructor)
        throws InvocationTargetException, IllegalArgumentException, InstantiationException, IllegalAccessException
    {
        return constructor.newInstance((Object[])null);
    }
    
    /**
     * Repaints the world if needed to obtain the desired frame rate.
     */
    private void repaintIfNeeded()
    {
        long currentTime = System.currentTimeMillis();
        long timeSinceLast = Math.max(1, currentTime - lastRepaintTime);
        
        if ((1000 / timeSinceLast) <= MAX_FRAME_RATE) {
            try {
                synchronized(repaintLock) {
                    
                    // Current frame rate is less than maximum, so we'll at least request
                    // a repaint at this time.
                    if (! paintPending) {
                        lastRepaintTime = currentTime;
                        worldHandler.repaint();
                        paintPending = true;
                    }
                    
                    if ((1000 / timeSinceLast) <= MIN_FRAME_RATE) {
                        // Waiting here makes sure the WorldCanvas gets a chance to
                        // repaint. It also lets the rest of the UI be responsive, even if
                        // we are running at maximum speed, by making sure events on the
                        // event queue are processed.

                        // Schedule a forced repaint, so that we don't deadlock while
                        // waiting for a repaint if something stops the repaint from
                        // occurring (no world for instance).
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run()
                            {
                                forcedRepaint();
                            }
                        });
                        
                        while (paintPending) {
                            repaintLock.wait();
                        }
                    }
                }
            }
            catch (InterruptedException ie) {}
        }
    }
    
    private void forcedRepaint()
    {
        JComponent wcanvas = WorldHandler.getInstance().getWorldCanvas();
        synchronized (repaintLock) {
            if (WorldHandler.getInstance().hasWorld()) {
                wcanvas.paintImmediately(wcanvas.getBounds());
            }
            
            if (paintPending) {
                paintPending = false;
                repaintLock.notify();
            }
        }
    }

    /**
     * Inform the simulation that the world has been repainted successfully.
     */
    public void worldRepainted()
    {
        synchronized (repaintLock) {
            paintPending = false;
            //long response = System.currentTimeMillis() - lastRepaintTime;
            //if (response > 250) {
            //    System.out.println("Repaint response time: " + response);
            //}
            repaintLock.notify();
        }
    }

    /**
     * Debug output to print the rate at which updates are performed
     * (acts/second).
     */
    /*
    private void printUpdateRate(long currentTime)
    {
        //updates++;

        long timeSinceUpdate = currentTime - lastUpdate;
        if (timeSinceUpdate > 3000000000L) {
            lastUpdate = currentTime;
            //updates = 0;
        }
    }
    */

    // Public methods etc.

    /**
     * Run one step of the simulation. Each actor in the world acts once.
     */
    public synchronized void runOnce()
    {
        // Don't call runOneLoop directly as that executes user code
        // and might hang.
        if (enabled) {
            synchronized (interruptLock) {
                interruptDelay = false;
            }
        }
        runOnce = true;
        notifyAll();
    }

    /**
     * Pauses and unpauses the simulation.
     */
    public synchronized void setPaused(boolean b)
    {
        if(paused == b) {
            //Nothing to do for us.
            return;
        }
        paused = b;
        if (enabled) {
            if(!paused) 
            {
                synchronized (interruptLock) {
                    interruptDelay = false;
                }
            }
            
            notifyAll();

            // If we are currently in the delay loop, interrupt it so that
            // the pause takes effect immediately.
            if (paused) {
                interruptDelay();                
            }
        }
    }

    /**
     * Interrupt if we are currently delaying between act-loops or the user is
     * using the Greenfoot.delay() method. This will basically jump to the next
     * act-loop as fast as possible while still executing the rest of actors in
     * the current loop. Used by setPaused() and setSpeed() to interrupt current
     * delays.
     */
    private void interruptDelay()
    {
        synchronized (interruptLock) {
            if (delaying) {
                interrupt();
            }
            else {
                // Called outside the delaying, so make sure it doesn't go into
                // the delay by signalling with this flag
                interruptDelay = true;
            }
        }
    }

    /**
     * Enable or disable the simulation.
     */
    public synchronized void setEnabled(boolean b)
    {
        if (b == enabled) {
            return;
        }

        enabled = b;
        
        if (b) {
            notifyAll();
            // fire a paused event to let listeners know we are
            // enabled again
            if (paused) {
                fireSimulationEvent(stoppedEvent);
            }
        }
        else {
            paused = true;
            // isRunning = false; // cause a started event if necessary, when the simulation is enabled again
            interruptDelay = true;
            interruptDelay();
            fireSimulationEvent(disabledEvent);
        }
    }

    private void fireSimulationEvent(SimulationEvent event)
    {
        // Guaranteed to return a non-null array
        Object[] listeners;
        synchronized (listenerList) {
            listeners = listenerList.getListenerList();

            // Process the listeners last to first, notifying
            // those that are interested in this event
            for (int i = listeners.length - 2; i >= 0; i -= 2) {
                if (listeners[i] == SimulationListener.class) {
                    ((SimulationListener) listeners[i + 1]).simulationChanged(event);
                }
            }
        }
    }
    
    /**
     * Notify that the simulation thread has been halted or resumed by the debugger.
     */
    public void notifyThreadStatus(boolean halted)
    {
        if (halted) {
            fireSimulationEvent(debuggerPausedEvent);
        }
        else {
            // resumed
            fireSimulationEvent(debuggerResumedEvent);
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
        synchronized (listenerList) {
            listenerList.add(SimulationListener.class, l);
        }
    }

    /**
     * Remove a simulationListener to listen for changes.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeSimulationListener(SimulationListener l)
    {
        synchronized (listenerList) {
            listenerList.remove(SimulationListener.class, l);
        }
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

        boolean speedChanged;
        synchronized (this) {
            speedChanged = this.speed != speed;
            if (speedChanged) {
                this.speed = speed;
                
                delegate.setSpeed(speed);
                
                this.delay = calculateDelay(speed);

                // If simulation is running we should interrupt any waiting or
                // sleeping that is currently happening.
                
                if(!paused) {
                    synchronized (interruptLock) {
                        if (delaying) {
                            interrupt();
                        }
                    }
                }    
            }
        }
        
        if (speedChanged) {
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
    public synchronized int getSpeed()
    {
        return speed;
    }

    /**
     * Sleep an amount of time according to the current speed setting for this
     * simulation. This will wait without considering previous waits, as opposed
     * to delay(). It should be called only from the simulation thread, in an
     * unsynchronized context.
     */
    public void sleep()
    {
        World world = worldHandler.getWorld();

        synchronized (this) {
            if (paused && isRunning && !runOnce) {
                // If it should be paused but is still running, it means that we
                // should try to end as quickly as possible and hence should NOT
                // delay.
                // If the user is interactively invoking a method that calls this
                // method, it will not be caught here, which is the correct
                // behaviour. Otherwise the call to sleep() will have no visible
                // effect at all.
                return;
            }
        }
        
        try {
            synchronized (interruptLock) {
                if (interruptDelay) {
                    // If interrupted, we just want to return now. We do not
                    // want to abort by throwing an exception, because that will
                    // leave the user code execution in an inconsistent state.
                    return;
                }
                delaying = true;
            }
            if (world != null) {
                // The WorldCanvas may be trying to synchronize on the world in
                // order to do a repaint. So, we use wait() here in order
                // to release the world lock temporarily.
                HDTimer.wait(delay, worldHandler.getWorldLock());
            }
            else {
                // shouldn't really happen
                HDTimer.sleep(delay);
            }
        }
        catch (InterruptedException e) {
            // If interrupted, we just want to return now. We do not
            // want to abort by throwing an exception, because that will
            // leave the user code execution in an inconsistent state.            
        }
        finally {
            synchronized (interruptLock) {
                delaying = false;
            }
        }
    }

    /**
     * Cause a delay (wait) according to the current speed setting for this
     * simulation. It will take the time spend in this simulation loop into
     * consideration and only pause the remaining time.
     * 
     * <p>This method is used for controlling the speed of the animation.
     * 
     * <p>The world lock should not be held when this method is called, so
     * that repaints can occur.
     */
    private void delay()
    {
        long currentTime = System.nanoTime();
        long timeElapsed = currentTime - lastDelayTime;
        long actualDelay = Math.max(delay - timeElapsed, 0L);
        
        synchronized (this) {
            synchronized (interruptLock) {
                if(interruptDelay) {
                    // interruptDelay was issued before entering this sync, so interrupt now.
                    interruptDelay = false;
                    if (paused || abort) {
                        lastDelayTime = currentTime;
                        return; // return... without delay
                    }
                }
                delaying = true;
            }
        }

        while (actualDelay > 0) {

            try {
                HDTimer.sleep(actualDelay);
            }
            catch (InterruptedException ie) {
                // We get interrupted either due to a pause, abort, being disabled or
                // a speed change. If it's a speed change, we can continue to delay, up
                // to the new time; otherwise we should finish up now.
                synchronized (this) {
                    if (!enabled || paused || abort) {
                        break;
                    }
                }
            }

            currentTime = System.nanoTime();
            timeElapsed = currentTime - lastDelayTime;
            actualDelay = delay - timeElapsed;
        }

        lastDelayTime = currentTime;
        synchronized (interruptLock) {
            Thread.interrupted(); // clear interrupt, in case we were interrupted just after the delay
            delaying = false;
        }
    }

    /**
     * Abort the simulation. It abruptly stops what is running and ends the
     * simulation thread, and it is not possible to start it again.
     */
    public void abort()
    {
        abort = true;
        setEnabled(false);
    }


    // ---------- WorldListener interface -----------

    /**
     * A new world was created - we're ready to go. Enable the simulation
     * functions.
     */
    @Override
    public void worldCreated(WorldEvent e)
    {
        setEnabled(true);
    }

    /**
     * The world was removed - disable the simulation functions.
     */
    @Override
    public void worldRemoved(WorldEvent e)
    {
        setEnabled(false);
    }

    // ----------- End of WorldListener interface -------------

}
