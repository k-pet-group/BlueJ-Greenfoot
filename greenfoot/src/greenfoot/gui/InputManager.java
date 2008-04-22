package greenfoot.gui;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.TriggeredKeyAdapter;
import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseAdapter;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionAdapter;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.input.states.ConstructorDragState;
import greenfoot.gui.input.states.ConstructorDragWhileRunningState;
import greenfoot.gui.input.states.DisabledState;
import greenfoot.gui.input.states.IdleState;
import greenfoot.gui.input.states.MoveState;
import greenfoot.gui.input.states.QuickAddDragState;
import greenfoot.gui.input.states.RunningState;
import greenfoot.gui.input.states.State;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

/**
 * Manages which listeners gets input events (mouse/keyboard) from components at
 * different times/states.
 * <p>
 * It works by forwarding events to the listeners that should receive events at
 * the current state.
 * 
 * @author Poul Henriksen
 * 
 */
public class InputManager
    implements SimulationListener, KeyListener, MouseListener, MouseMotionListener, WorldListener
{
    /** The current state */
    private State state = null;

    // The active listeners that will receive the events.
    private TriggeredKeyListener activeKeyListener;
    private TriggeredMouseListener activeMouseListener;
    private TriggeredMouseMotionListener activeMouseMotionListener;
    
    /**
     * Create a new input manager. Before using this object, the listeners
     * should be initialized and the {@link #init()} method should be called.
     * 
     * @see #setMoveListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setDragListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setIdleListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setRunningListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #init()
     */
    public InputManager()
    {
        State disabledState = DisabledState.initialize(this,  new TriggeredKeyAdapter(),  new TriggeredMouseAdapter(), new TriggeredMouseMotionAdapter());
        switchAndActivateState(disabledState);
    }

    /**
     * Set the listeners that should be active when the simulation is running.
     * 
     * @see #init()
     */
    public void setRunningListeners(TriggeredKeyListener runningKeyListener, TriggeredMouseListener runningMouseListener,
            TriggeredMouseMotionListener runningMouseMotionListener)
    {      
        RunningState.initialize(this, runningKeyListener, runningMouseListener, runningMouseMotionListener);
    }

    /**
     * Set the listeners that should be active when the application is idle.
     * 
     * @see #init()
     */
    public void setIdleListeners(TriggeredKeyListener idleKeyListener, TriggeredMouseListener idleMouseListener,
            TriggeredMouseMotionListener idleMouseMotionListener)
    {        
        IdleState.initialize(this, idleKeyListener, idleMouseListener, idleMouseMotionListener);
    }

    /**
     * Set the listeners that should be active when dragging a newly created
     * object around.
     * 
     * @see #init()
     */
    public void setDragListeners(TriggeredKeyListener dragKeyListener, TriggeredMouseListener dragMouseListener,
            TriggeredMouseMotionListener dragMouseMotionListener)
    {
        QuickAddDragState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);
        ConstructorDragState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);
        ConstructorDragWhileRunningState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);;
    }

    /**
     * Set the listeners that should be active when moving an actor around that
     * has been added to the world previously.
     * 
     * @see #init()
     */
    public void setMoveListeners(TriggeredKeyListener moveKeyListener, TriggeredMouseListener moveMouseListener,
            TriggeredMouseMotionListener moveMouseMotionListener)
    {
        MoveState.initialize(this, moveKeyListener, moveMouseListener, moveMouseMotionListener);
    }

    /**
     * Should be called after all listeners are correctly setup.
     * 
     * @see #setMoveListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setDragListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setIdleListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setRunningListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @throws IllegalStateException If some of the listeners has not been set up correctly.
     */
    public void init() throws IllegalStateException
    {
        // Make sure we fail now if we can't get all the states.
        DisabledState.getInstance();
        RunningState.getInstance();
        MoveState.getInstance();
        QuickAddDragState.getInstance();
        ConstructorDragState.getInstance();
        ConstructorDragWhileRunningState.getInstance();
        
        switchAndActivateState(IdleState.getInstance());
    }
    
    /**
     * Deactivates the current listeners and enables the new ones. This method
     * is called from the State classes when they are activated
     */
    public void activateListeners(TriggeredKeyListener keyL, TriggeredMouseListener mouseL, TriggeredMouseMotionListener mouseMotionL) 
    {
        if (activeKeyListener != null) {
            activeKeyListener.listeningEnded();
        }
        if (activeMouseListener != null) {
            activeMouseListener.listeningEnded();
        }
        if (activeMouseMotionListener != null) {
            activeMouseMotionListener.listeningEnded();
        }
        
        activeKeyListener = keyL;
        activeMouseListener = mouseL;
        activeMouseMotionListener = mouseMotionL;

        activeKeyListener.listeningStarted();
        activeMouseListener.listeningStarted();
        activeMouseMotionListener.listeningStarted();
    }
    
    /**
     * Switches to the given state and activates it by calling the method
     * {@link #activate()}.
     * 
     * @see #activate()
     */
    public void switchAndActivateState(State newState)
    {
        System.out.println("Switching to new state: " + newState);
        state = newState;
        state.activate();
    }
    
    /**
     * Used for changing between running and stopped state.
     */
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.STARTED) {
            state.switchToNextState(State.Event.SIMULATION_STARTED);
        }
        else if (e.getType() == SimulationEvent.STOPPED) {
            state.switchToNextState(State.Event.SIMULATION_STOPPED);
        }
    }

    /**
     * When an actor is created via constructor the constructor in the context menu.
     * @param object 
     */
    public void objectAdded()
    {
        state.switchToNextState(State.Event.CONSTRUCTOR_INVOKED);
    }

    public void keyPressed(KeyEvent e)
    {
        activeKeyListener.keyPressed(e);
        if (e.isShiftDown()) {
            state.switchToNextState(State.Event.SHIFT_PRESSED);
        }
        activeKeyListener.keyPressed(e);
    }

    public void keyReleased(KeyEvent e)
    {
        activeKeyListener.keyReleased(e);
        //TODO: should this be !e.isShiftDown instead?
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            state.switchToNextState(State.Event.SHIFT_RELEASED);
        }
        activeKeyListener.keyReleased(e);
    }

    public void keyTyped(KeyEvent e)
    {
        activeKeyListener.keyTyped(e);
    }

    public void mouseClicked(MouseEvent e)
    {
		checkShift(e);
        activeMouseListener.mouseClicked(e);
    }

    public void mousePressed(MouseEvent e)
    {
		checkShift(e);
        activeMouseListener.mousePressed(e);
        if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
            state.switchToNextState(State.Event.MOUSE_PRESSED);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
		checkShift(e);
        activeMouseListener.mouseReleased(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
            state.switchToNextState(State.Event.MOUSE_RELEASED);
        }
    }

    public void mouseEntered(MouseEvent e)
    {
		checkShift(e);
        activeMouseListener.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e)
    {
		checkShift(e);
        activeMouseListener.mouseExited(e);
    }

    /**
	 * Method that checks whether shift has been released without us noticing.
	 * It will then simulate that the shift key has been released.
	 * 
	 */
	private void checkShift(MouseEvent e) {
		if (state == QuickAddDragState.getInstance() && !e.isShiftDown()) {
			state.switchToNextState(State.Event.SHIFT_RELEASED);
		}
	}

    public void mouseDragged(MouseEvent e)
    {
		checkShift(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
            activeMouseMotionListener.mouseDragged(e);
        }
    }

    public void mouseMoved(MouseEvent e)
    {
		checkShift(e);
        activeMouseMotionListener.mouseMoved(e);
    }

	public void worldCreated(WorldEvent e) {
		state.switchToNextState(State.Event.WORLD_CREATED);
	}

	public void worldRemoved(WorldEvent e) {
		state.switchToNextState(State.Event.WORLD_REMOVED);
	}
}
