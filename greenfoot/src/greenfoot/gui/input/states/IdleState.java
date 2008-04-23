package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;
import greenfoot.gui.input.states.State.Event;

/**
 * This is the default state, which is active when the simulation is stopped and
 * nothing else is happening (no dragging etc.)
 * 
 * @author Poul Henriksen
 */
public class IdleState extends State
{
    protected static IdleState instance;

    public static synchronized IdleState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private IdleState(InputManager inputManager, TriggeredKeyListener keyListener, TriggeredMouseListener mouseListener,
            TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized IdleState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        if (instance != null) {
            throw new IllegalStateException("Already intialized.");
        }
        instance = new IdleState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case CONSTRUCTOR_INVOKED :
                switchAndActivateState(ConstructorDragState.getInstance(), obj);
                break;
            case SIMULATION_STARTED :
                switchAndActivateState(RunningState.getInstance(), obj);
                break;
            case MOUSE_PRESSED :
                switchAndActivateState(MoveState.getInstance(), obj);
                break;
            case SHIFT_PRESSED :
                switchAndActivateState(QuickAddDragState.getInstance(), obj);
                break;
        }
    }

};
