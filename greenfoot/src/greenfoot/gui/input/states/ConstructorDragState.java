package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * This state is active when the constructor of an Actor has been invoked via
 * the context-menu of the Actor. Works only while the simulation is stopped,
 * but there is another state {@link ConstructorDragWhileRunningState} which
 * handles the same case when the simulation is running.
 * 
 * @author Poul Henriksen
 */
public class ConstructorDragState extends State
{
    protected static ConstructorDragState instance;

    public static synchronized ConstructorDragState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
    
    private ConstructorDragState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }

    public static synchronized ConstructorDragState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        instance = new ConstructorDragState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case SHIFT_PRESSED :
                switchAndActivateState(QuickAddDragState.getInstance(), obj);
                break;
            case MOUSE_RELEASED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
        }
    }
}