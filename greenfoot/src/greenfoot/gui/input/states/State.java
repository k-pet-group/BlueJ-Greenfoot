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
package greenfoot.gui.input.states;

import greenfoot.event.TriggeredKeyAdapter;
import greenfoot.event.TriggeredKeyListener;
import greenfoot.event.TriggeredMouseAdapter;
import greenfoot.event.TriggeredMouseListener;
import greenfoot.event.TriggeredMouseMotionAdapter;
import greenfoot.event.TriggeredMouseMotionListener;
import greenfoot.gui.input.InputManager;

/**
 * Superclass for all states. Each state is also responsible for determining the
 * next state given a specific event. The states also set up the listeners that
 * should be active in that state.
 * <p>
 * Sub classes should make sure to initialize the singleton instance.
 * 
 * @author Poul Henriksen
 * 
 */
public abstract class State
{
    /**
     * Represents the different events that can tricker state changes.
     */
    public static enum Event {
         CONSTRUCTOR_INVOKED,  MOUSE_RELEASED,  SHIFT_PRESSED,  SHIFT_RELEASED,  MOUSE_PRESSED,  SIMULATION_STARTED,  SIMULATION_STOPPED,  WORLD_CREATED,  WORLD_REMOVED, OBJECT_MOVED,  ESC_PRESSED;        
    }

    protected InputManager inputManager;
    private TriggeredKeyListener keyListener;
    private TriggeredMouseListener mouseListener;
    private TriggeredMouseMotionListener mouseMotionListener;

    State(InputManager inputManager, TriggeredKeyListener keyListener, TriggeredMouseListener mouseListener,
            TriggeredMouseMotionListener mouseMotionListener)
    {
        super();
        this.inputManager = inputManager;

        if (keyListener != null) {
            this.keyListener = keyListener;
        }
        else {
            this.keyListener = new TriggeredKeyAdapter();
        }

        if (mouseListener != null) {
            this.mouseListener = mouseListener;
        }
        else {
            this.mouseListener = new TriggeredMouseAdapter();
        }

        if (mouseMotionListener != null) {
            this.mouseMotionListener = mouseMotionListener;
        }
        else {
            this.mouseMotionListener = new TriggeredMouseMotionAdapter();
        }
    }
    
    /**
     * The rules for switching states. Implementations should respond to events
     * by using the method
     * {@link #switchAndActivateState(greenfoot.gui.InputManager.State)} to
     * switch to other states based on the event.
     * 
     * @see #switchAndActivateState(greenfoot.gui.InputManager.State)
     */
    public void switchToNextState(State.Event event, Object obj)
    {
        switch(event) {
            case WORLD_REMOVED :
                inputManager.switchAndActivateState(DisabledState.getInstance(), obj);
                break;
        }
    }

    /**
     * This method should sets up the listeners. Activating a state means
     * setting up the listeners to receive events and notifying them that
     * listening has started via the TriggeredListener interface.
     * 
     * @see InputManager.#activeKeyListener
     * @see InputManager.#activeMouseListener
     * @see InputManager.#activeMouseMotionListener
     */
    public void activate(Object obj)
    {
        inputManager.activateListeners(keyListener, mouseListener, mouseMotionListener, obj);
    }

    void switchAndActivateState(State state, Object obj)
    {
        inputManager.switchAndActivateState(state, obj);
    }
}