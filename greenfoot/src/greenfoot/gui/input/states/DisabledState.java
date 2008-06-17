package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * This state is active when you should not be allowed to initiate a drag.
 * 
 * @author Poul Henriksen
 */
public class DisabledState extends State
{
    protected static DisabledState instance;

    public static synchronized DisabledState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    

    private DisabledState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized DisabledState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        instance = new DisabledState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        switch(event) {
            case WORLD_CREATED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
        }
    }
}