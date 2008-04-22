package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.InputManager;
import greenfoot.gui.input.states.State.Event;

/**
 * This state is active when an Actor that has previously been added to the
 * world is dragged around. Works only while the simulation is stopped.
 * 
 * @author Poul Henriksen
 */
public class MoveState extends State
{
    protected static MoveState instance;

    public static synchronized MoveState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    

    private MoveState(InputManager inputManager, TriggeredKeyListener keyListener, TriggeredMouseListener mouseListener,
            TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized MoveState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        if (instance != null) {
            throw new IllegalStateException("Already intialized.");
        }
        instance = new MoveState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event)
    {
        super.switchToNextState(event);
        switch(event) {
            case MOUSE_RELEASED :
                switchAndActivateState(IdleState.getInstance());
                break;
        }
    }

};
