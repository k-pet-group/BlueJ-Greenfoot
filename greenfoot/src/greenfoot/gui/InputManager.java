package greenfoot.gui;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

/**
 * Manages which listeners gets input events (mouse/keyboard) from components at different times/states.
 * <p>
 * It works by forwarding.
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
 * @author Poul Henriksen
 *
 */
public class InputManager implements SimulationListener, KeyListener, MouseListener, MouseMotionListener
{

    // Components that can generate eventse
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
    
    private enum States{NO_STATE, RUNNING, IDLE, CONSTUCTOR_DRAG, QUICKADD_DRAG};
    private States state = States.NO_STATE;
    
    private Component activeComponent;
    private KeyListener activeKeyListener;
    private MouseListener activeMouseListener;
    private MouseMotionListener activeMouseMotionListener;
    
    
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
    }

    /**
     * Used for changing between running and stopped state.
     */
    public void simulationChanged(SimulationEvent e)
    {        
        //maybe add state change to states? so you just send "event" to a state, and that then transitions to next state
        if(e.getType() == SimulationEvent.STARTED)
        {
            activeKeyListener = runningKeyListener;
            activeMouseListener = runningMouseListener;
            activeMouseMotionListener = runningMouseMotionListener;
            state = States.RUNNING;
        }
        else if(e.getType() == SimulationEvent.STOPPED)
        {
            activeKeyListener = idleKeyListener;
            activeMouseListener = idleMouseListener;
            activeMouseMotionListener = idleMouseMotionListener;
            state = States.IDLE;
        }
    }

    /**
     * When object created via constructor
     */
    public void objectAdded() {
        
    }
    
    public void keyPressed(KeyEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void keyReleased(KeyEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void keyTyped(KeyEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseClicked(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseEntered(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseExited(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mousePressed(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseReleased(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseDragged(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

    public void mouseMoved(MouseEvent e)
    {
        // TODO Auto-generated method stub
        
    }

}
