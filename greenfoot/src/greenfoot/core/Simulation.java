/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    // We skip repaints if the simulation is running faster than the
    // MAX_FRAME_RATE. This makes the high speeds run faster, since we avoid
    // repaints that can't be seen anyway.
    private static int MAX_FRAME_RATE = 60;
    private static int MIN_FRAME_RATE = 30;
    private WorldHandler worldHandler;
    
    
    private volatile boolean paused;

    /** Whether the simulation is enabled (world installed) */
    private volatile boolean enabled;

    /** Whether to run one loop when paused */
    private volatile boolean runOnce;

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

    /**
     * Track whether a repaint operation on the world canvas is pending.
     * Used in keeping track of the repaint rate.
     */
    private boolean paintPending;

    /**
     * Lock to synchronize access to the two feilds: delaying and interruptDelay
     */
    private Object interruptLock = new Object();
    /** Whether we are currently delaying between act-loops. */
    private boolean delaying;
    /** Whether a delay between act-loops should be interrupted. */
    private boolean interruptDelay;

    
    /** Used to figure out when we are transitioning from running to paused state and vice versa. */
    private boolean isRunning = false;
    
    /** flag to indicate that we want to abort the simulation and never start it again. */
    private volatile boolean abort;
    
    /**
     * Create new simulation. Leaves the simulation in paused state
     * 
     * @param worldHandler
     *            The handler for the world that is simulated
     */
    private Simulation(WorldHandler worldHandler, SimulationDelegate simulationDelegate)
    {
        this.setName("SimulationThread");
        this.worldHandler = worldHandler;
        this.delegate = simulationDelegate;
        startedEvent = new SimulationEvent(this, SimulationEvent.STARTED);
        stoppedEvent = new SimulationEvent(this, SimulationEvent.STOPPED);
        speedChangeEvent = new SimulationEvent(this, SimulationEvent.CHANGED_SPEED);
        disabledEvent = new SimulationEvent(this, SimulationEvent.DISABLED);
        newActEvent = new SimulationEvent(this, SimulationEvent.NEW_ACT);
        setPriority(Thread.MIN_PRIORITY);
        paused = true;
        speed = 50;
        delay = calculateDelay(speed);
        HDTimer.init();
    }
    
    public static void initialize(WorldHandler worldHandler, SimulationDelegate simulationDelegate)
    {
        instance = new Simulation(worldHandler, simulationDelegate);
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
        while (!abort) {
            try {
                if(interruptedForSpeedChange) {
                    // If it was interrupted because of a speed change, then we
                    // delay an amount equal to the new delay.
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
                
            } catch (Throwable t) {
                // If any other exceptions occur, halt the simulation
                paused = true;
                t.printStackTrace();
            }
        }

        // The simulations has been aborted. But, we might still have to notify the world.
        synchronized (this) {
            if(isRunning) {
                World world = worldHandler.getWorld();
                if (world != null) {
                    world.stopped();
                }
                isRunning = false;
            } 
        }
    }   
         
    /**
     * Block if the simulation is paused. This will block until the simulation
     * is resumed.
     * 
     * @throws InterruptedException If it couldn't acquire the world lock when
     *             signalling started() stopped() to the world.
     */
    private synchronized void maybePause()
        throws InterruptedException
    {

        if(runOnce || paused) {
            // This code will be executed when:
            //  runOnce is over
            //  setPaused(true)
            //  setEnabled(false)
            //  abort() (sometimes, depending on timing)
            World world = worldHandler.getWorld();
            if (world != null) {
                // We need to sync to avoid ConcurrentModificationException
                ReentrantReadWriteLock lock = WorldVisitor.getLock(world);
                lock.writeLock().lockInterruptibly();
                try {
                    world.stopped();
                }
                catch (Throwable t) {
                    // If any exceptions occur, halt the simulation
                    paused = true;
                    t.printStackTrace();
                }
                finally {
                    // Release lock if we have it (we might not have the
                    // lock if interrupted)
                    if (lock.isWriteLockedByCurrentThread()) {
                        lock.writeLock().unlock();
                    }
                }
            }
            isRunning = false;
            runOnce = false;
        }
        
        if (paused && enabled) {
            fireSimulationEvent(stoppedEvent);
            System.gc();
        }

        // Wait loop that waits until no longer pause or if we need to run the
        // simulation once because the user pressed 'Act'
        while (paused && !runOnce) {
            if (abort) {
                // if we are about to abort, now is the time. We have notified
                // the world.stopped and there is nothing else to do here.
                return;
            }
            
            // Make sure we repaint before pausing.
            worldHandler.repaint();
            try {
                System.gc();
                this.wait();
            }
            catch (InterruptedException e1) {
                // Swallow the interrupt
            }
                
            if (!paused && enabled && !abort) {
                repaintTimes.clear();
                lastDelayTime = System.nanoTime();
                fireSimulationEvent(startedEvent);
            }
        }
        
        if (!isRunning && enabled && !abort) {
            // No longer paused, get ready to run:
            isRunning = true;
            World world = worldHandler.getWorld();
            if (world != null) {
                // We need to sync to avoid ConcurrentModificationException
                ReentrantReadWriteLock lock = WorldVisitor.getLock(world);
                lock.writeLock().lockInterruptibly();
                try {
                    world.started();
                }
                finally {
                    // Release lock if we have it (we might not have the
                    // lock if
                    // interrupted)
                    if (lock.isWriteLockedByCurrentThread()) {
                        lock.writeLock().unlock();
                    }
                }
            }
        }
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

        // We don't want to be interrupted in the middle of an act-loop
        // so we remember the first interrupted exception and throw it
        // when all the actors have acted.
        ActInterruptedException interruptedException = null;
        
        List<? extends Actor> objects = null;

        // We need to sync to avoid ConcurrentModificationException
        try {
            ReentrantReadWriteLock lock = WorldVisitor.getLock(world);
            lock.writeLock().lockInterruptibly();
            try {
                try {
                    world.act();
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
                    if (actor.getWorld() != null) {
                        try {
                            actor.act();
                        }
                        catch (ActInterruptedException e) {
                            if (interruptedException == null) {
                                interruptedException = e;
                            }
                        }
                    }

                }
            }
            finally {
                // Release lock if we have it (we might not have the lock if
                // interrupted)
                if(lock.isWriteLockedByCurrentThread()){
                    lock.writeLock().unlock();
                }
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
                        repaintTimes.offer(System.currentTimeMillis());

                        // Waiting here makes sure the WorldCanvas gets a chance to
                        // repaint. It also lets the rest of the UI be responsive, even if
                        // we are running at maximum speed, by making sure events on the
                        // event queue are processed.

                        while (paintPending) {
                            wait();
                        }
                        
                        doRepaint();
                    }
                    else {
                        if (! paintPending) {
                            repaintTimes.offer(System.currentTimeMillis());
                            doRepaint();
                        }
                    }
                }
            }
            catch (InterruptedException ie) {}
        }
    }

    /**
     * Issue a repaint of the world canvas.
     */
    private void doRepaint()
    {
        paintPending = true;
        worldHandler.repaint();

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

            // Interrupt thread to make sure it stops.
            if (paused) {
                interruptDelay();                
            }
        }
    }

    /**
     * Interrupt if we are currently delaying between act-loops or the user is
     * using the Greenfoot.delay() method. This will basically jump to the next
     * act-loop as fast as possible while still excuting the rest of actors in
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
        if (b) {
            notifyAll();
        }
        if (enabled != b) {
            enabled = b;
            if (!enabled) {
                paused = true;
                interrupt();
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

            // If simulation is running we should interrupt any waiting or
            // sleeping that is currently happening.
            
            if(!paused) {
                interruptedForSpeedChange = true;
                interruptDelay();
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
                HDTimer.wait(delay, WorldVisitor.getLock(world));
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
     * This method is used for timing the animation.
     */
    private void delay() throws ActInterruptedException
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
                // Signal that we are now in the delay part of the code, by setting delaying=true
                synchronized (interruptLock) {
                    if(interruptDelay) {
                        // interruptDelay was issued before entering this sync, so interrupt now.
                        interruptDelay = false;
                        throw new InterruptedException("Interrupted in Simulation.delay() before sleep.");
                    }
                    delaying = true;
                }                
                
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
            // The reason we put it here (and not before the sleep), is so that
            // it is not set if we are interrupted, because an interruption will
            // get back to this point (if interrupted for speed) or will be
            // reset (if interrupted for pausing).
            this.lastDelayTime = currentTime + actualDelay;
        }
        catch (InterruptedException e) {            
            throw new ActInterruptedException(e);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            synchronized (interruptLock) {
                delaying = false;
            }
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