package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.InputManager;
import greenfoot.gui.input.states.State.Event;

/**
 * This state is active when the constructor of an Actor has been invoked via
 * the context-menu of the Actor. Works only while the simulation is running,
 * but there is another state {@link ConstructorDragState} which handles the
 * same case when the simulation is stopped.
 * 
 * @author Poul Henriksen
 */
public class ConstructorDragWhileRunningState extends State
{
    protected static ConstructorDragWhileRunningState instance;

    public static synchronized ConstructorDragWhileRunningState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private ConstructorDragWhileRunningState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized ConstructorDragWhileRunningState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        if (instance != null) {
            throw new IllegalStateException("Already intialized.");
        }
        instance = new ConstructorDragWhileRunningState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event)
    {
        super.switchToNextState(event);
        switch(event) {
            case SIMULATION_STOPPED :
                switchAndActivateState(ConstructorDragState.getInstance());
                break;
            case MOUSE_RELEASED :
                switchAndActivateState(RunningState.getInstance());
                break;
        }
    }
}
