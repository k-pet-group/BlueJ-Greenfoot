package greenfoot.gui;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

/**
 * Manages which listeners gets input events (mouse/keyboard) from components at different times/states. Also handles whether dragComponent is visible.
 * <p>
 * It works by forwarding events to the listeners that should receive events at the current state.
 * <p>
 * States:
 *  a) Running: Listener: everything goes to mouse manager and keyboard manager (rename them to polling) comp: WorldCanvas
 *  Stopped:
 *    b)  Initial/nothing/idle: List: WorldHandler gets all keys and mouse Comp: WorldCanvas
 *    c)  Move: List: worldhandler Comp: WorldCanvas  
 *    d)  SHIFT/quickadd: List, Comp: DragGlassPane activated when shift pressed. Deactivated when Shift released (OBS: shift release outside comp)
 *    e)  Constructor invoc: List, Comp: DragGlassPane is activated when new object is created via menu. until mouse clicked. Or shift (which cancels and starts quickadd)?
 *      
 *  Triggers of state change:    
 *     1. Object created via constructor menu ( b->e)
 *      1a. Mouse clicked (e->b)
 *      1b. SHIFT pressed (e->d)
 *     2. SHIFT pressed (b->d)
 *      2a. SHIFT released (d->b)
 *     4. Sim started (b->a)
 *      4a. Sim ended (a->b)
 *     5. drag on actor (b-c)
 *      5a. mouse released (c-b)
 *     
 * Two components can be enabled: WorldCanvas or DragGlassPane
 * Listeners: (MouseManager, KeyboardManager), WorldHandler and DragGlassPane
 * 
 * What about standalone!?
 * RightClicks!
 * What about keyboard shortcuts while running? Space for pause?
 * 
 * @author Poul Henriksen
 *
 */
public class InputManager implements SimulationListener, KeyListener, MouseListener, MouseMotionListener
{
    
    // Key listeners
    private KeyListener runningKeyListener;
    private KeyListener idleKeyListener;
    private KeyListener dragKeyListener;
    private KeyListener moveKeyListener;

    // Mouse listeners
    private MouseListener runningMouseListener;
    private MouseListener idleMouseListener;
    private MouseListener dragMouseListener;
    private MouseListener moveMouseListener;

    // Mouse motion listeners
    private MouseMotionListener runningMouseMotionListener;
    private MouseMotionListener idleMouseMotionListener;
    private MouseMotionListener dragMouseMotionListener;
    private MouseMotionListener moveMouseMotionListener;

    private KeyListener activeKeyListener;
    private MouseListener activeMouseListener;
    private MouseMotionListener activeMouseMotionListener;
    /**
     * Events represents the different events that can tricker state changes.
     */
    private enum Event{CONSTRUCTOR_INVOKED, MOUSE_RELEASED, SHIFT_PRESSED, SHIFT_RELEASED, MOUSE_PRESSED, SIMULATION_STARTED, SIMULATION_STOPPED};
       
    /**
     * Superclass for all states. Each state is also responsible for determining
     * the next state given a specific event. The states also set up the
     * listeners and components that should be active in that state.
     * 
     * @author Poul Henriksen
     * 
     */
    private abstract class State{
        /**
         * The rules for switching states.
         */        
        abstract void switchToNextState(Event event);
        
        /**
         * Switches to the given state and activates it.
         */
        protected void switchAndActivateState(State newState)
        {
            state = newState;
            state.activate();
        }      
        
        /**
         * This method should set up the correct listeners and components.
         */
        protected abstract void activate();
    };    
    
    
    private class RunningState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case SIMULATION_STOPPED :
                    switchAndActivateState(IDLE_STATE);
                    break;
                case CONSTRUCTOR_INVOKED :
                    switchAndActivateState(CONSTRUCTOR_DRAG_WHILE_RUNNING_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {
            activateRunningListeners();
        }        
    }

    private class IdleState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case CONSTRUCTOR_INVOKED:
                    switchAndActivateState(CONSTRUCTOR_DRAG_STATE);
                    break;
                case SIMULATION_STARTED:
                    switchAndActivateState(RUNNING_STATE);
                    break;
                case MOUSE_PRESSED:
                    switchAndActivateState(MOVE_STATE);
                    break;
                case SHIFT_PRESSED:
                    switchAndActivateState(QUICKADD_DRAG_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {
            activateIdleListeners();            
        }
    };

    private class MoveState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case MOUSE_RELEASED:
                    switchAndActivateState(IDLE_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {        
            activateMoveListeners();  
        }
    };

    private class QuickAddDragState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case SHIFT_RELEASED:
                    switchAndActivateState(IDLE_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {
            activateDragListeners();
        }
    };

    private class ConstructorDragState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case SHIFT_PRESSED:
                    switchAndActivateState(QUICKADD_DRAG_STATE);
                    break;
                case MOUSE_RELEASED:
                    switchAndActivateState(IDLE_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {
            activateDragListeners();
        }
    }; 
    
    private class ConstructorDragWhileRunningState extends State
    {
        @Override
        void switchToNextState(Event event)
        {
            switch(event) {
                case SIMULATION_STOPPED:
                    switchAndActivateState(CONSTRUCTOR_DRAG_STATE);
                    break;
                case MOUSE_RELEASED:
                    switchAndActivateState(RUNNING_STATE);
                    break;
            }
        }

        @Override
        protected void activate()
        {
            activateDragListeners();
        }
    }; 
    

    // STATES
    private final State RUNNING_STATE = new RunningState();
    private final State IDLE_STATE = new IdleState();
    private final State MOVE_STATE = new MoveState();
    private final State QUICKADD_DRAG_STATE = new QuickAddDragState();
    private final State CONSTRUCTOR_DRAG_STATE = new ConstructorDragState();
    private final State CONSTRUCTOR_DRAG_WHILE_RUNNING_STATE = new ConstructorDragWhileRunningState();
    
    private State state;
    private boolean moveInitialized;
    private boolean dragInitialized;
    private boolean idleInitialized;
    private boolean runningInitialized;

    public void setRunningListeners(KeyListener runningKeyListener, MouseListener runningMouseListener,
            MouseMotionListener runningMouseMotionListener)
    {
        runningInitialized = true;
        this.runningKeyListener = runningKeyListener;
        this.runningMouseListener = runningMouseListener;
        this.runningMouseMotionListener = runningMouseMotionListener;
    } 
    
    public void setIdleListeners(KeyListener idleKeyListener, MouseListener idleMouseListener,
            MouseMotionListener idleMouseMotionListener)
    {
        idleInitialized = true;
        this.idleKeyListener = idleKeyListener;
        this.idleMouseListener = idleMouseListener;
        this.idleMouseMotionListener = idleMouseMotionListener;
    }
    
    public void setDragListeners(KeyListener dragKeyListener, MouseListener dragMouseListener,
            MouseMotionListener dragMouseMotionListener)
    {
        dragInitialized = true;
        this.dragMouseListener = dragMouseListener;
        this.dragKeyListener = dragKeyListener;
        this.dragMouseMotionListener = dragMouseMotionListener;
    }
    
    public void setMoveListeners(KeyListener moveKeyListener, MouseListener moveMouseListener,
            MouseMotionListener moveMouseMotionListener)
    {        
        moveInitialized = true;
        this.moveMouseListener = moveMouseListener;
        this.moveKeyListener = moveKeyListener;
        this.moveMouseMotionListener = moveMouseMotionListener;
    }
    
    /**
     * Should be called after all listeners are correctly setup.
     */
    public void init() 
    {
        if( !(idleInitialized && runningInitialized && moveInitialized && dragInitialized)) {
            throw new IllegalStateException("Listeners not set up correctly.");
        }
        state = IDLE_STATE;
        state.activate();
    }
    
    public InputManager()
    {
        super();
    }
    
    private void activateRunningListeners() {
        activeKeyListener = runningKeyListener;
        activeMouseListener = runningMouseListener;
        activeMouseMotionListener = runningMouseMotionListener;
    }
    private void activateIdleListeners() {
        activeKeyListener = idleKeyListener;
        activeMouseListener = idleMouseListener;
        activeMouseMotionListener = idleMouseMotionListener;
    }
    private void activateMoveListeners() {
        activeKeyListener = moveKeyListener;
        activeMouseListener = moveMouseListener;
        activeMouseMotionListener = moveMouseMotionListener;
    }
    private void activateDragListeners() {
        activeKeyListener = dragKeyListener;
        activeMouseListener = dragMouseListener;
        activeMouseMotionListener = dragMouseMotionListener;
    }
    
    /**
     * Used for changing between running and stopped state.
     */
    public void simulationChanged(SimulationEvent e)
    {        
        //maybe add state change to states? so you just send "event" to a state, and that then transitions to next state
        if(e.getType() == SimulationEvent.STARTED)
        {
            state.switchToNextState(Event.SIMULATION_STARTED);
        }
        else if(e.getType() == SimulationEvent.STOPPED)
        {
            state.switchToNextState(Event.SIMULATION_STOPPED);
        }
    }

    /**
     * When object created via constructor
     */
    public void objectAdded() {
        state.switchToNextState(Event.CONSTRUCTOR_INVOKED);
        //TODO who needs to be told about this? And how do we do that? Is it done somewhere else?
    }
    
    public void keyPressed(KeyEvent e)
    {
        activeKeyListener.keyPressed(e);
        if(e.isShiftDown()) {
            state.switchToNextState(Event.SHIFT_PRESSED);
        }
        activeKeyListener.keyPressed(e);
    }

    public void keyReleased(KeyEvent e)
    {
        activeKeyListener.keyReleased(e);
        if(! e.isShiftDown()) {
            state.switchToNextState(Event.SHIFT_RELEASED);
        }
        activeKeyListener.keyReleased(e);
    }

    public void keyTyped(KeyEvent e)
    {
        activeKeyListener.keyTyped(e);
    }

    public void mouseClicked(MouseEvent e)
    {
        activeMouseListener.mouseClicked(e);
    }

    public void mousePressed(MouseEvent e)
    {
        activeMouseListener.mousePressed(e);
        if(SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() ) {
            state.switchToNextState(Event.MOUSE_PRESSED);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        activeMouseListener.mouseReleased(e);
        if (SwingUtilities.isLeftMouseButton(e)) {
            state.switchToNextState(Event.MOUSE_RELEASED);
        }
    }
    
    public void mouseEntered(MouseEvent e)
    {
        activeMouseListener.mouseEntered(e);
        
        // Somehow during a drag the button was released without us noticing;
        // make sure we are in the right state. (I think this can happen when
        // some other window steals focus during a drag).
        if (!e.isShiftDown()) {
            state.switchToNextState(Event.SHIFT_RELEASED);
        }
    //    if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
     //       state.switchToNextState(Event.MOUSE_RELEASED);
      //  }
    }

    public void mouseExited(MouseEvent e)
    {
        activeMouseListener.mouseExited(e);
    }

    public void mouseDragged(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            activeMouseMotionListener.mouseDragged(e);
        }
    }

    public void mouseMoved(MouseEvent e)
    {
        activeMouseMotionListener.mouseMoved(e);
    }

}
