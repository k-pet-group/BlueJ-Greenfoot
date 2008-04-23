package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;
import greenfoot.gui.input.states.State.Event;

/**
 * This state is active when the simulation is running.
 * 
 * @author Poul Henriksen
 */
public class RunningState extends State
{
    protected static RunningState instance;

    public static synchronized RunningState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private RunningState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized RunningState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        if (instance != null) {
            throw new IllegalStateException("Already intialized.");
        }
        instance = new RunningState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case SIMULATION_STOPPED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
            case CONSTRUCTOR_INVOKED :
                switchAndActivateState(ConstructorDragWhileRunningState.getInstance(), obj);
                break;
        }
    }
}