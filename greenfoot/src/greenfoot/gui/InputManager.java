package greenfoot.gui;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;


import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

/**
 * Manages which listeners gets input events (mouse/keyboard) from components at different times/states.
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

    // Components that can generate mouse and key events
    private Component defaultComp;
    private Component dragComp;
    
    // Key listeners
    private  KeyListener runningKeyListener;
    private  KeyListener idleKeyListener;
    private  KeyListener dragKeyListener;
    
    // Mouse listeners
    private  MouseListener runningMouseListener;
    private  MouseListener idleMouseListener;
    private  MouseListener dragMouseListener;  
    
    // Mouse motion listeners
    private  MouseMotionListener runningMouseMotionListener;
    private  MouseMotionListener idleMouseMotionListener;
    private  MouseMotionListener dragMouseMotionListener;
        
    private Component activeComponent;
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
            System.out.println("SWITCH from state: " + state + " to " + newState);
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
            activateIdleListeners();  
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
    

    // STATES
    private final State RUNNING_STATE = new RunningState();
    private final State IDLE_STATE = new IdleState();
    private final State MOVE_STATE = new MoveState();
    private final State QUICKADD_DRAG_STATE = new QuickAddDragState();
    private final State CONSTRUCTOR_DRAG_STATE = new ConstructorDragState();
    
    private State state;
    
    public InputManager(Component defaultComp, Component dragComp, KeyListener runningKeyListener,
            KeyListener idleKeyListener, KeyListener dragKeyListener, MouseListener runningMouseListener,
            MouseListener idleMouseListener, MouseListener dragMouseListener, MouseMotionListener runningMouseMotionListener,
            MouseMotionListener idleMouseMotionListener, MouseMotionListener dragMouseMotionListener)
    {
        super();
        this.defaultComp = defaultComp;
        this.dragComp = dragComp;
        this.runningKeyListener = runningKeyListener;
        this.idleKeyListener = idleKeyListener;
        this.dragKeyListener = dragKeyListener;
        this.runningMouseListener = runningMouseListener;
        this.idleMouseListener = idleMouseListener;
        this.dragMouseListener = dragMouseListener;
        this.runningMouseMotionListener = runningMouseMotionListener;
        this.idleMouseMotionListener = idleMouseMotionListener;
        this.dragMouseMotionListener = dragMouseMotionListener;
                

        removeSelfAsListener();
        defaultComp.addKeyListener(this);
        defaultComp.addMouseListener(this);
        defaultComp.addMouseMotionListener(this);
        dragComp.addKeyListener(this);
        dragComp.addMouseListener(this);
        dragComp.addMouseMotionListener(this);
        
        state = IDLE_STATE;
        state.activate();
    }

    private void removeSelfAsListener()
    {
        defaultComp.removeKeyListener(InputManager.this);
        defaultComp.removeMouseListener(InputManager.this);
        defaultComp.removeMouseMotionListener(InputManager.this);
        dragComp.removeKeyListener(InputManager.this);
        dragComp.removeMouseListener(InputManager.this);
        dragComp.removeMouseMotionListener(InputManager.this);
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
        System.out.println("keyPressed: " + e);
        activeKeyListener.keyPressed(e);
        if(e.isShiftDown()) {
            state.switchToNextState(Event.SHIFT_PRESSED);
        }
    }

    public void keyReleased(KeyEvent e)
    {
        System.out.println("keyReleased: " + e);
        activeKeyListener.keyReleased(e);
        if(! e.isShiftDown()) {
            state.switchToNextState(Event.SHIFT_RELEASED);
        }
    }

    public void keyTyped(KeyEvent e)
    {
        System.out.println("keyTyped: " + e);
        activeKeyListener.keyTyped(e);
    }

    public void mouseClicked(MouseEvent e)
    {

        System.out.println("mouseClicked: " + e);
        activeMouseListener.mouseClicked(e);
    }

    public void mousePressed(MouseEvent e)
    {
        System.out.println("mousePressed: " + e);
        activeMouseListener.mousePressed(e);
        if(SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown() ) {
            state.switchToNextState(Event.MOUSE_PRESSED);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        
        System.out.println("mouseReleased: " + e);
        activeMouseListener.mouseReleased(e);
        state.switchToNextState(Event.MOUSE_RELEASED);
    }
    
    public void mouseEntered(MouseEvent e)
    {
        System.out.println("mouseEntered: " + e);
        // TODO take care of state changes that has happened outside this component. Like mouse pressed/released and SHIFT.
        /*if(! e.isShiftDown()) {
            activeMouseListener.mouseEntered(e);
            state.switchToNextState(Event.SHIFT_RELEASED);
        }*/
        activeMouseListener.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e)
    {
        System.out.println("mouseExited: " + e);
        activeMouseListener.mouseExited(e);
    }

    public void mouseDragged(MouseEvent e)
    {
      //  System.out.println("mouseDragged: " + e);
        activeMouseMotionListener.mouseDragged(e);
    }

    public void mouseMoved(MouseEvent e)
    {
     //   System.out.println("mouseMoved: " + e);
        activeMouseMotionListener.mouseMoved(e);
    }

}
