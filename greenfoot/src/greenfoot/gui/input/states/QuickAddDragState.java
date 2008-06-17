package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * This state is active when "quick adding" a new Actor by holding down the
 * SHIFT-key. Works only while the simulation is stopped.
 * 
 * @author Poul Henriksen
 */
public class QuickAddDragState extends State
{
    protected static QuickAddDragState instance;

    public static synchronized QuickAddDragState getInstance() throws IllegalStateException
    {
        if (instance == null) {
            throw new IllegalStateException("Not initialized.");
        }
        return instance;
    }
        
    private QuickAddDragState(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        super(inputManager, keyListener, mouseListener, mouseMotionListener);
    }
    
    public static synchronized QuickAddDragState initialize(InputManager inputManager, TriggeredKeyListener keyListener,
            TriggeredMouseListener mouseListener, TriggeredMouseMotionListener mouseMotionListener)
    {
        instance = new QuickAddDragState(inputManager, keyListener, mouseListener, mouseMotionListener);
        return instance;
    }

    @Override
    public void switchToNextState(State.Event event, Object obj)
    {
        super.switchToNextState(event, obj);
        switch(event) {
            case SHIFT_RELEASED :
                switchAndActivateState(IdleState.getInstance(), obj);
                break;
        }
    }
}