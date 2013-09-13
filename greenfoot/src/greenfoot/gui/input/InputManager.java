/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.gui.input;

import greenfoot.Actor;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.TriggeredKeyAdapter;
import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseAdapter;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionAdapter;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.input.states.ConstructorDragState;
import greenfoot.gui.input.states.ConstructorDragWhileRunningState;
import greenfoot.gui.input.states.DisabledState;
import greenfoot.gui.input.states.IdleState;
import greenfoot.gui.input.states.MoveState;
import greenfoot.gui.input.states.QuickAddDragState;
import greenfoot.gui.input.states.RunningState;
import greenfoot.gui.input.states.State;

import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

/**
 * Manages which listeners gets input events (mouse/keyboard) from components at
 * different times/states.
 * <p>
 * It works by forwarding events to the listeners that should receive events at
 * the current state.
 * <p>
 * Not thread safe. Make sure to call methods from the event thread.
 * @author Poul Henriksen
 * 
 */
public class InputManager
    implements SimulationListener, KeyListener, MouseListener, MouseMotionListener, WorldListener
{
    /** The current state */
    private State state = null;

    // The active listeners that will receive the events.
    private TriggeredKeyListener activeKeyListener;
    private TriggeredMouseListener activeMouseListener;
    private TriggeredMouseMotionListener activeMouseMotionListener;
    
    /**
     * Create a new input manager. Before using this object, the listeners
     * should be initialized and the {@link #init()} method should be called.
     * 
     * @see #setMoveListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setDragListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setIdleListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #setRunningListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @see #init()
     */
    public InputManager()
    {
        State disabledState = DisabledState.initialize(this,  new TriggeredKeyAdapter(),  new TriggeredMouseAdapter(), new TriggeredMouseMotionAdapter());
        switchAndActivateState(disabledState, null);
    }

    /**
     * Set the listeners that should be active when the simulation is running.
     * 
     * @see #init()
     */
    public void setRunningListeners(TriggeredKeyListener runningKeyListener, TriggeredMouseListener runningMouseListener,
            TriggeredMouseMotionListener runningMouseMotionListener)
    {      
        RunningState.initialize(this, runningKeyListener, runningMouseListener, runningMouseMotionListener);
    }

    /**
     * Set the listeners that should be active when the application is idle.
     * 
     * @see #init()
     */
    public void setIdleListeners(TriggeredKeyListener idleKeyListener, TriggeredMouseListener idleMouseListener,
            TriggeredMouseMotionListener idleMouseMotionListener)
    {        
        IdleState.initialize(this, idleKeyListener, idleMouseListener, idleMouseMotionListener);
    }

    /**
     * Set the listeners that should be active when dragging a newly created
     * object around.
     * 
     * @see #init()
     */
    public void setDragListeners(TriggeredKeyListener dragKeyListener, TriggeredMouseListener dragMouseListener,
            TriggeredMouseMotionListener dragMouseMotionListener)
    {
        QuickAddDragState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);
        ConstructorDragState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);
        ConstructorDragWhileRunningState.initialize(this, dragKeyListener, dragMouseListener, dragMouseMotionListener);;
    }

    /**
     * Set the listeners that should be active when moving an actor around that
     * has been added to the world previously.
     * 
     * @see #init()
     */
    public void setMoveListeners(TriggeredKeyListener moveKeyListener, TriggeredMouseListener moveMouseListener,
            TriggeredMouseMotionListener moveMouseMotionListener)
    {
        MoveState.initialize(this, moveKeyListener, moveMouseListener, moveMouseMotionListener);
    }

    /**
     * Should be called after all listeners are correctly setup.
     * 
     * @see #setMoveListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setDragListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setIdleListeners(TriggeredKeyListener, TriggeredMouseListener, TriggeredMouseMotionListener)
     * @see #setRunningListeners(TriggeredKeyListener, TriggeredMouseListener,
     *      TriggeredMouseMotionListener)
     * @throws IllegalStateException If some of the listeners has not been set up correctly.
     */
    public void init() throws IllegalStateException
    {
        // Make sure we fail now if we can't get all the states.
        DisabledState.getInstance();
        RunningState.getInstance();
        MoveState.getInstance();
        QuickAddDragState.getInstance();
        ConstructorDragState.getInstance();
        ConstructorDragWhileRunningState.getInstance();
        
        switchAndActivateState(IdleState.getInstance(), null);
    }
    
    /**
     * Deactivates the current listeners and enables the new ones. This method
     * is called from the State classes when they are activated
     */
    public void activateListeners(TriggeredKeyListener keyL, TriggeredMouseListener mouseL, TriggeredMouseMotionListener mouseMotionL, Object obj) 
    {
        if (activeKeyListener != null) {
            activeKeyListener.listeningEnded();
        }
        if (activeMouseListener != null) {
            activeMouseListener.listeningEnded();
        }
        if (activeMouseMotionListener != null) {
            activeMouseMotionListener.listeningEnded();
        }
        
        activeKeyListener = keyL;
        activeMouseListener = mouseL;
        activeMouseMotionListener = mouseMotionL;

        activeKeyListener.listeningStarted(obj);
        activeMouseListener.listeningStarted(obj);
        activeMouseMotionListener.listeningStarted(obj);
    }
    
    /**
     * Switches to the given state and activates it by calling the method
     * {@link #activate()}.
     * 
     * @see #activate()
     */
    public void switchAndActivateState(State newState, Object obj)
    {
        state = newState;
        state.activate(obj);
    }
    
    /**
     * Used for changing between running and stopped state.
     */
    public void simulationChanged(final SimulationEvent e)
    {
        // Simulation events occur on the simulation thread.
        EventQueue.invokeLater(new Runnable() {
           @Override
            public void run()
            {
               if (e.getType() == SimulationEvent.STARTED) {
                   state.switchToNextState(State.Event.SIMULATION_STARTED, null);
               }
               else if (e.getType() == SimulationEvent.STOPPED) {
                   state.switchToNextState(State.Event.SIMULATION_STOPPED, null);
               }
            } 
        });
    }

    /**
     * When an actor is created via constructor the constructor in the context menu.
     * @param object Object that has been added
     */
    public void objectCreated(Actor object)
    {
        state.switchToNextState(State.Event.CONSTRUCTOR_INVOKED, object);
    }
    

    /**
     * When an actor is created via constructor the constructor in the context menu.
     * @param object Object that has been added
     */
    public void objectMoved(Actor object)
    {
        state.switchToNextState(State.Event.OBJECT_MOVED, object);
    }

    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            state.switchToNextState(State.Event.SHIFT_PRESSED, null);
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            state.switchToNextState(State.Event.ESC_PRESSED, null); 
        }
        activeKeyListener.keyPressed(e);
    }

    public void keyReleased(KeyEvent e)
    {
        activeKeyListener.keyReleased(e);
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            state.switchToNextState(State.Event.SHIFT_RELEASED, null);
        }
    }

    public void keyTyped(KeyEvent e)
    {
        activeKeyListener.keyTyped(e);
    }

    public void mouseClicked(MouseEvent e)
    {
        checkShift(e);
        activeMouseListener.mouseClicked(e);
    }

    public void mousePressed(MouseEvent e)
    {
        checkShift(e);
        activeMouseListener.mousePressed(e);
        if (SwingUtilities.isLeftMouseButton(e) && !e.isShiftDown()) {
            state.switchToNextState(State.Event.MOUSE_PRESSED, null);
        }
    }

    public void mouseReleased(MouseEvent e)
    {
        checkShift(e);
        activeMouseListener.mouseReleased(e);
        state.switchToNextState(State.Event.MOUSE_RELEASED, null);
    }

    public void mouseEntered(MouseEvent e)
    {
        checkShift(e);
        activeMouseListener.mouseEntered(e);
    }

    public void mouseExited(MouseEvent e)
    {
        checkShift(e);
        activeMouseListener.mouseExited(e);
    }

    /**
     * Method that checks whether shift has been released without us noticing.
     * It will then simulate that the shift key has been released.
     */
    private void checkShift(MouseEvent e)
    {
        if (state == QuickAddDragState.getInstance() && !e.isShiftDown()) {
            state.switchToNextState(State.Event.SHIFT_RELEASED, null);
        }
    }

    public void mouseDragged(MouseEvent e)
    {
        checkShift(e);
        activeMouseMotionListener.mouseDragged(e);
    }

    public void mouseMoved(MouseEvent e)
    {
        checkShift(e);
        activeMouseMotionListener.mouseMoved(e);
    }

    public void worldCreated(WorldEvent e)
    {
        state.switchToNextState(State.Event.WORLD_CREATED, null);
    }

    public void worldRemoved(WorldEvent e)
    {
        state.switchToNextState(State.Event.WORLD_REMOVED, null);
    }
}
