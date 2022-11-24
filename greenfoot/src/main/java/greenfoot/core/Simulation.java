/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010,2011,2012,2013,2014,2016,2019,2021  Poul Henriksen and Michael Kolling 
 
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
import greenfoot.event.SimulationListener;
import greenfoot.event.SimulationListener.AsyncEvent;
import greenfoot.event.SimulationListener.SyncEvent;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.util.HDTimer;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.event.EventListenerList;

/**
 * The main class of the simulation. It drives the simulation and calls act()
 * on the objects in the world and then paints them.
 * 
 * @author Poul Henriksen
 */
@OnThread(Tag.Simulation)
public class Simulation extends Thread
    implements WorldListener
{
    // Most of the fields require synchronized access. Some of them do not because they are only
    // accessed from the simulation thread itself.
    
    // All user code should generally be run on the simulation thread. The simulation monitor
    // should not be held while executing user code (though the world lock should be held).
    
    // The following two constants control repainting of the world while the simulation is
    // running. We skip repaints if the simulation is running faster than the MAX_FRAME_RATE.
    // This makes the high speeds run faster, since we avoid repaints that can't be seen
    // anyway. Once requested, the repaint may take some time to occur; if the effective
    // repaint rate falls below MIN_FRAME_RATE, then we temporarily suspend the simulation
    // and wait for the repaint to occur.
    
    @OnThread(Tag.Any)
    private WorldHandler worldHandler;
    
    /** Whether the simulation is (to be) paused */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean paused;

    /** Whether the simulation is enabled (world installed) */
    private volatile boolean enabled;

    /** Whether to run one loop when paused */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private boolean runOnce;
    
    /** Tasks that are queued to run on the simulation thread */
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private Queue<SimulationRunnable> queuedTasks = new LinkedList<>();

    @OnThread(Tag.Any)
    private final List<SimulationListener> listenerList = new ArrayList<>();

    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static Simulation instance;

    /** for timing the animation */
    public static final int MAX_SIMULATION_SPEED = 100;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private int speed; // the simulation speed in range (1..100)

    private long lastDelayTime;
    private long delay; // the speed translated into delay (nanoseconds)

    /**
     * Lock to synchronize access to the two fields: delaying and interruptDelay
     */
    @OnThread(Tag.Any)
    private Object interruptLock = new Object();
    /** Whether we are currently delaying between act-loops. */
    @OnThread(Tag.Any)
    private boolean delaying;
    /** Whether a delay between act-loops should be interrupted. */
    @OnThread(Tag.Any)
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
     */
    @OnThread(Tag.Any)
    private Simulation()
    {
        super("SimulationThread");
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
    @OnThread(Tag.Any)
    public static synchronized void initialize()
    {
        instance = new Simulation();
    }

    /**
     * Returns the simulation if it is initialised. If not, it will return null.
     */
    @OnThread(Tag.Any)
    public static synchronized Simulation getInstance()
    {
        return instance;
    }

    /**
     * Attach this simulation to the world handler (and vice versa).
     */
    @OnThread(Tag.Any)
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
    @OnThread(value = Tag.Simulation, ignoreParent = true)
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

                // Don't delay if doing a single cycle:
                boolean currentlyPaused;
                synchronized (this)
                {
                    currentlyPaused = paused;
                }
                
                if (!currentlyPaused)
                {
                    delay();
                }
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
                // Send word to the server VM that we stopped with an error (and thus they need to show terminal):
                WorldHandler.getInstance().notifyStoppedWithError();
                paintRemote(true);
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
    
    public static interface SimulationRunnable
    {
        @OnThread(Tag.Simulation)
        public void run();
    }

    /**
     * Schedule some task to run on the simulation thread. The task will be run with the
     * world write lock held.
     */
    @OnThread(Tag.Any)
    public synchronized void runLater(SimulationRunnable r)
    {
        queuedTasks.add(r);
        // If the simulation is paused we must notify so that the wait is triggered and the
        // queued task will run. We check 'paused' as well as 'enabled' since a world may
        // be instantiated via this mechanism.
        if (paused || ! enabled) {
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
        paintRemote(true);
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
                    synchronized (interruptLock) {
                        interruptDelay = false;
                    }
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
                        fireSimulationEventAsync(AsyncEvent.STOPPED);
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
        fireSimulationEventSync(SyncEvent.STARTED);
        World world = worldHandler.getWorld();
        if (world != null) {
            try {
                worldStarted(world); // may cause us to pause
            }
            catch (Throwable t) {
                isRunning = false;
                synchronized (interruptLock) {
                    // Clear interrupted status
                    Thread.interrupted();
                    interruptDelay = false;
                }
                setPaused(true);
                t.printStackTrace();
                return;
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
        SimulationRunnable r;
        synchronized (this) {
            r = queuedTasks.poll();
        }
        
        while (r != null) {
            try {
                fireSimulationEventSync(SyncEvent.QUEUED_TASK_BEGIN);
                
                try {
                    // This may run user code, which might throw an exception.
                    r.run();
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
                
                fireSimulationEventSync(SyncEvent.QUEUED_TASK_END);
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
     * May propagate a runtime exception or error from user code.
     * 
     * @throws ActInterruptedException  if an act() call was interrupted.
     */
    private void runOneLoop(World world)
    {
        fireSimulationEventSync(SyncEvent.NEW_ACT_ROUND);
        
        // We don't want to be interrupted in the middle of an act-loop
        // so we remember the first interrupted exception and throw it
        // when all the actors have acted.
        ActInterruptedException interruptedException = null;
        
        List<? extends Actor> objects = null;

        try
        {
            actWorld(world);
            if (world != worldHandler.getWorld())
            {
                paintRemote(false);
                return; // New world was set
            }
        }
        catch (ActInterruptedException e)
        {
            interruptedException = e;
        }
        // We need to make a copy so that the original collection can be
        // modified by the actors' act() methods.
        objects = new ArrayList<Actor>(WorldVisitor.getObjectsListInActOrder(world));
        for (Actor actor : objects)
        {
            if (!enabled)
            {
                return;
            }
            if (ActorVisitor.getWorld(actor) != null)
            {
                try
                {
                    actActor(actor);
                    if (world != worldHandler.getWorld())
                    {
                        return; // New world was set
                    }
                }
                catch (ActInterruptedException e)
                {
                    if (interruptedException == null)
                    {
                        interruptedException = e;
                    }
                }
            }
        }
        
        worldHandler.getKeyboardManager().clearLatchedKeys();

        // We were interrupted while running through the act-loop. Throw now.
        if(interruptedException != null) {
            throw interruptedException;
        }
        
        // printUpdateRate(System.nanoTime());

        repaintIfNeeded();

        fireSimulationEventSync(SyncEvent.END_ACT_ROUND);
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
        paintRemote(false);
    }
    
    protected void paintRemote(boolean forcePaint)
    {
        WorldHandler.getInstance().paint(forcePaint);
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
    @OnThread(Tag.Any)
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
     * Toggles the running/paused state of the simulation.
     */
    @OnThread(Tag.Any)
    public synchronized void togglePaused()
    {
        setPaused(!paused);
    }
    
    /**
     * Pauses and unpauses the simulation.
     */
    @OnThread(Tag.Any)
    public synchronized void setPaused(boolean b)
    {
        if (paused == b)
        {
            //Nothing to do for us.
            return;
        }
        paused = b;
        if (enabled)
        {
            if(!paused) 
            {
                synchronized (interruptLock)
                {
                    interruptDelay = false;
                }
            }
            
            notifyAll();

            // If we are currently in the delay loop, interrupt it so that
            // the pause takes effect immediately.
            if (paused)
            {
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
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Any)
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
                fireSimulationEventAsync(AsyncEvent.STOPPED);
            }
        }
        else {
            // Note that a user method might be executing even if paused (i.e. an
            // interactive method invocation by right-clicking an object in the
            // world). We need to interrupt any delay that is currently running
            // and to prevent any future delay, until the simulation is re-enabled.
            interruptDelay();
            if (! paused) {
                paused = true;
            }
            else {
                // We are paused, or at least should be.
                // We don't want interruptDelay set; we're not running, so the
                // only delay we can get is a call to Greenfoot.delay(...), which
                // goes through sleep(...). That will exit early if the simulation
                // is not enabled. If we leave interruptDelay set now, it may affect
                // a future delay.
                synchronized (interruptLock) {
                    interruptDelay = false;
                }
            }
            fireSimulationEventAsync(AsyncEvent.DISABLED);
        }
    }

    @OnThread(Tag.Simulation)
    private void fireSimulationEventSync(SyncEvent event)
    {
        synchronized (listenerList) {
            for (SimulationListener listener : listenerList)
            {
                listener.simulationChangedSync(event);
            }
        }
    }

    @OnThread(Tag.Any)
    private void fireSimulationEventAsync(AsyncEvent event)
    {
        synchronized (listenerList) {
            for (SimulationListener listener : listenerList)
            {
                listener.simulationChangedAsync(event);
            }
        }
    }

    /**
     * Add a simulationListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    @OnThread(Tag.Any)
    public void addSimulationListener(SimulationListener l)
    {
        synchronized (listenerList) {
            listenerList.add(0, l);
        }
    }

    /**
     * Set the speed of the simulation.
     * 
     * @param newSpeed
     *            The speed in the range (0..100)
     */
    @OnThread(Tag.Any)
    public void setSpeed(int newSpeed)
    {
        if (newSpeed < 0)
        {
            newSpeed = 0;
        }
        else if (newSpeed > MAX_SIMULATION_SPEED)
        {
            newSpeed = MAX_SIMULATION_SPEED;
        }
        
        boolean speedChanged;
        synchronized (this)
        {
            speedChanged = this.speed != newSpeed;
            if (speedChanged)
            {
                this.speed = newSpeed;
                this.delay = calculateDelay(newSpeed);

                // If simulation is running we should interrupt any waiting or
                // sleeping that is currently happening.
                
                if(!paused)
                {
                    synchronized (interruptLock)
                    {
                        if (delaying)
                        {
                            interrupt();
                        }
                    }
                }    
            }
        }
        
        if (speedChanged)
        {
            fireSimulationEventAsync(AsyncEvent.CHANGED_SPEED);
        }
    }

    /**
     * Returns the delay as a function of the speed.
     * 
     * @return The delay in nanoseconds.
     */
    @OnThread(Tag.Any)
    private static long calculateDelay(int curSpeed)
    {
        // Make the speed into a delay
        long rawDelay = MAX_SIMULATION_SPEED - curSpeed;

        long min = 30 * 1000L; // Delay at MAX_SIMULATION_SPEED - 1
        long max = 10000 * 1000L * 1000L; // Delay at slowest speed

        double a = Math.pow(max / (double) min, 1D / (MAX_SIMULATION_SPEED - 1));
        long calcDelay = 0;
        if (rawDelay > 0) {
            calcDelay = (long) (Math.pow(a, rawDelay - 1) * min);
        }
        return calcDelay;
    }

    /**
     * Get the current simulation speed.
     * 
     * @return The speed in the range (1..100)
     */
    @OnThread(Tag.Any)
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
    @OnThread(Tag.Simulation)
    public void sleep(int numCycles)
    {
        synchronized (this)
        {
            if (paused && isRunning && !runOnce)
            {
                // If it should be paused but is still running, it means that we
                // should try to end as quickly as possible and hence should NOT
                // delay.
                // If the user is interactively invoking a method that calls this
                // method, it will not be caught here, which is the correct
                // behaviour. Otherwise the call to sleep() will have no visible
                // effect at all.
                return;
            }
            if (! enabled)
            {
                // It's possible an interactive method invocation was fired which
                // calls Greenfoot.delay(), but the simulation was disabled (reset)
                // between the invocation and call to Greenfoot.delay(). In this
                // case, we don't want to delay.
                return;
            }
            
            // We need to check the interruptDelay while the simulation lock is
            // still held, in case setEnabled(false) is called just after we release
            // the simulation lock.
            synchronized (interruptLock)
            {
                if (interruptDelay)
                {
                    // If interrupted, we just want to return now. We do not
                    // want to abort by throwing an exception, because that will
                    // leave the user code execution in an inconsistent state.
                    return;
                }
                delaying = true;
            }
        }

        fireSimulationEventSync(SyncEvent.DELAY_LOOP_ENTERED);

        try
        {
            // If we will be asleep for more than 1/100th of a second, force repaint, otherwise rely on usual if-due mechanism.
            worldHandler.paint(numCycles * delay > 100_000_000L);
            for (int i = 0; i < numCycles; i++)
            {
                HDTimer.sleep(delay);
            }
        }
        catch (InterruptedException e)
        {
            // If interrupted, we just want to return now. We do not
            // want to abort by throwing an exception, because that will
            // leave the user code execution in an inconsistent state.            
        }
        finally
        {
            synchronized (interruptLock)
            {
                Thread.interrupted(); // clear interrupt, in case we were interrupted just after the delay
                interruptDelay = false;
                delaying = false;
            }
            fireSimulationEventSync(SyncEvent.DELAY_LOOP_COMPLETED);
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
        
        synchronized (this)
        {
            synchronized (interruptLock)
            {
                if(interruptDelay)
                {
                    // interruptDelay was issued before entering this sync, so interrupt now.
                    interruptDelay = false;
                    if (paused || abort)
                    {
                        lastDelayTime = currentTime;
                        return; // return... without delay
                    }
                }
                delaying = true;
            }
        }

        fireSimulationEventSync(SyncEvent.DELAY_LOOP_ENTERED);

        while (actualDelay > 0)
        {
            try
            {
                HDTimer.sleep(actualDelay);
            }
            catch (InterruptedException ie)
            {
                // We get interrupted either due to a pause, abort, being disabled or
                // a speed change. If it's a speed change, we can continue to delay, up
                // to the new time; otherwise we should finish up now.
                synchronized (this)
                {
                    if (!enabled || paused || abort)
                    {
                        break;
                    }
                }
            }

            currentTime = System.nanoTime();
            timeElapsed = currentTime - lastDelayTime;
            actualDelay = delay - timeElapsed;
        }

        lastDelayTime = currentTime;
        synchronized (interruptLock)
        {
            Thread.interrupted(); // clear interrupt, in case we were interrupted just after the delay
            interruptDelay = false;
            delaying = false;
        }
        fireSimulationEventSync(SyncEvent.DELAY_LOOP_COMPLETED);
    }

    /**
     * Abort the simulation. It abruptly stops what is running and ends the
     * simulation thread, and it is not possible to start it again.
     */
    @OnThread(Tag.Any)
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
